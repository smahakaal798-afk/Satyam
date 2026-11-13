const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_smahakaal";
const ADMIN_EMAIL = "smahakaal798@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfile(uid, overrides = {}) {
  const pastTime = new Date(Date.now() - 1000);
  return {
    userId: uid,
    displayName: "Player One",
    email: "player@example.com",
    coins: 500,
    freeSpinsLeft: 5,
    lastSpinResetEpochDay: 20400,
    nextAdAvailableEpochMs: 0,
    energy: 100,
    lastEnergyUpdateMs: 1700000000000,
    streakDay: 1,
    lastCheckinEpochDay: 20400,
    totalTaps: 12,
    totalSpins: 3,
    createdAt: pastTime,
    updatedAt: pastTime,
    ...overrides,
  };
}

function validTransaction(uid, overrides = {}) {
  const pastTime = new Date(Date.now() - 1000);
  return {
    userId: uid,
    referenceCode: "SM-904211",
    methodName: "UPI",
    accountIdentifier: "player@okaxis",
    coinsSpent: 10000,
    inrEquivalent: 100.0,
    usdEquivalent: 1.0,
    status: "Submitted",
    txType: "WITHDRAWAL",
    createdAt: pastTime,
    ...overrides,
  };
}

function validCommission(uid, overrides = {}) {
  const pastTime = new Date(Date.now() - 1000);
  return {
    playerUid: uid,
    playerName: "Alice",
    sourceGame: "Lucky Spin",
    grossWinCoins: 1000,
    commissionCoins: 10,
    netPlayerCoins: 990,
    inrValue: 0.1,
    createdAt: pastTime,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write user profiles or admin collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID))
  );
  await assertFails(unauthDb.collection("admin_commissions").get());
  await assertFails(unauthDb.collection("admin_treasury").doc("summary").get());
});

test("Owner: can create, read, and update own SpinMint profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(aliceRef.set(validProfile(ALICE_UID)));
  await assertSucceeds(aliceRef.get());
  await assertSucceeds(
    aliceRef.update({
      coins: 1250,
      totalTaps: 25,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation: Bob cannot read or write Alice's profile or transactions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_1")
      .set(validTransaction(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .update({ coins: 999999, updatedAt: new Date(Date.now() - 500) })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );
});

test("Shadow update & negative coin guard: rejects ghost fields and negative coins", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertFails(
    aliceRef.set(validProfile(ALICE_UID, { isVerified: true }))
  );
  await assertFails(aliceRef.set(validProfile(ALICE_UID, { coins: -50 })));

  await assertSucceeds(aliceRef.set(validProfile(ALICE_UID)));
  await assertFails(
    aliceRef.update({
      coins: -1,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceRef.update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Transactions subcollection: owner can append and list, cannot mutate or delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID))
  );

  const txRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("transactions")
    .doc("tx_dep_1");

  await assertSucceeds(
    txRef.set(
      validTransaction(ALICE_UID, {
        referenceCode: "DEP-551100",
        status: "Credited",
        txType: "DEPOSIT",
      })
    )
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );

  await assertFails(txRef.update({ coinsSpent: 999999 }));
  await assertFails(txRef.delete());
});

test("Admin 1% commission ledger: players can log own 1% fee, cannot read/edit/forge; verified admin can read", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const commRef = aliceDb.collection("admin_commissions").doc("comm_1");

  // Player logs their own 1% commission
  await assertSucceeds(commRef.set(validCommission(ALICE_UID)));

  // Player cannot forge another UID, read commissions, update, or delete
  await assertFails(
    aliceDb
      .collection("admin_commissions")
      .doc("comm_spoof")
      .set(validCommission(BOB_UID))
  );
  await assertFails(aliceDb.collection("admin_commissions").get());
  await assertFails(commRef.update({ commissionCoins: 1 }));
  await assertFails(commRef.delete());

  // Verified Admin (smahakaal798@gmail.com with email_verified: true) can list commissions and update treasury
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();

  await assertSucceeds(adminDb.collection("admin_commissions").get());
  await assertSucceeds(
    adminDb
      .collection("admin_treasury")
      .doc("summary")
      .set({
        adminEmail: ADMIN_EMAIL,
        claimedCoins: 10,
        updatedAt: new Date(Date.now() - 500),
      })
  );
});

test("Email Spoofing Test: unverified admin email token is strictly rejected", async () => {
  const spoofAdminDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: ADMIN_EMAIL,
      email_verified: false,
    })
    .firestore();

  await assertFails(spoofAdminDb.collection("admin_commissions").get());
  await assertFails(
    spoofAdminDb.collection("admin_treasury").doc("summary").get()
  );
  await assertFails(
    spoofAdminDb
      .collection("admin_treasury")
      .doc("summary")
      .set({
        adminEmail: ADMIN_EMAIL,
        claimedCoins: 999,
        updatedAt: new Date(Date.now() - 500),
      })
  );
});
