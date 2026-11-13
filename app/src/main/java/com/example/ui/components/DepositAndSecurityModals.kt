package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.DEPOSIT_PACKAGES
import com.example.model.DepositPackage
import com.example.model.PaymentMethod
import com.example.model.SecurityState
import com.example.ui.theme.BorderViolet
import com.example.ui.theme.BrightEmerald
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CardSlateElevated
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.RoseAlert
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidObsidian
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DepositModal(
    securityState: SecurityState,
    onDismiss: () -> Unit,
    onSubmitDeposit: (PaymentMethod, DepositPackage?, String, String, String) -> Unit
) {
    var selectedPkg by remember { mutableStateOf<DepositPackage?>(DEPOSIT_PACKAGES[1]) }
    var customInr by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.UPI) }
    var accountInput by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }

    val activeInr = selectedPkg?.inrAmount ?: (customInr.toIntOrNull() ?: 0)
    val activeCoins = selectedPkg?.totalCoins ?: (activeInr * 100L)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(26.dp),
            color = DeepIndigo,
            border = BorderStroke(1.2.dp, BorderViolet),
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AddCircle,
                            contentDescription = null,
                            tint = NeonGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Coins / Deposit",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close deposit modal",
                            tint = TextSecondary
                        )
                    }
                }

                Text(
                    text = "Select a Top-Up Package (₹10 = 1,000 Coins)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                // 2x2 Deposit Package Cards
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0..1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0..1) {
                                val pkg = DEPOSIT_PACKAGES[row * 2 + col]
                                val selected = selectedPkg?.id == pkg.id
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("deposit_pkg_${pkg.inrAmount}")
                                        .clickable {
                                            selectedPkg = pkg
                                            customInr = ""
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (selected) NeonGold.copy(alpha = 0.16f) else CardSlate,
                                    border = BorderStroke(
                                        width = if (selected) 1.6.dp else 1.dp,
                                        color = if (selected) NeonGold else BorderViolet
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "₹${pkg.inrAmount}",
                                                style = MaterialTheme.typography.titleLarge,
                                                color = if (selected) NeonGold else TextPrimary,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            pkg.tag?.let { tag ->
                                                Text(
                                                    text = tag,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = BrightEmerald,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "+${NumberFormat.getNumberInstance(Locale.US).format(pkg.totalCoins)} Coins",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = BrightEmerald,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customInr,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }
                        customInr = digits
                        if (digits.isNotEmpty()) selectedPkg = null
                    },
                    label = { Text("Or Custom Deposit Amount (₹)") },
                    placeholder = { Text("e.g. 150", color = TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGold,
                        unfocusedBorderColor = BorderViolet,
                        focusedLabelColor = NeonGold,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Payment Rail",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentMethod.entries.forEach { method ->
                        val selected = selectedMethod == method
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clickable { selectedMethod = method },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) method.accentColor.copy(alpha = 0.2f) else CardSlate,
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) method.accentColor else BorderViolet
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (method == PaymentMethod.USDT) "USDT" else method.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) method.accentColor else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = accountInput,
                    onValueChange = { accountInput = it },
                    label = { Text("${selectedMethod.accountLabel} (Optional)") },
                    placeholder = { Text(selectedMethod.placeholder, color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = selectedMethod.accentColor,
                        unfocusedBorderColor = BorderViolet,
                        focusedLabelColor = selectedMethod.accentColor,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                if (securityState.hasPin && securityState.requirePinForWallet) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { raw -> pinInput = raw.filter { it.isDigit() }.take(4) },
                        label = { Text("4-Digit Vault Security PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("deposit_pin_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrightEmerald,
                            unfocusedBorderColor = BorderViolet,
                            focusedLabelColor = BrightEmerald,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { onSubmitDeposit(selectedMethod, selectedPkg, customInr, accountInput, pinInput) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_deposit_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGold,
                        contentColor = VoidObsidian
                    )
                ) {
                    Text(
                        text = "ADD +${NumberFormat.getNumberInstance(Locale.US).format(activeCoins)} COINS (₹$activeInr)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Simulated top-up • Credits virtual Mint Coins locally for gameplay",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SecurityVaultModal(
    securityState: SecurityState,
    onDismiss: () -> Unit,
    onSetPin: (String) -> Unit,
    onDisablePin: (String) -> Unit,
    onToggleLaunchLock: (Boolean) -> Unit,
    onToggleWalletGuard: (Boolean) -> Unit,
    onLockNow: () -> Unit
) {
    var pinField by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(26.dp),
            color = DeepIndigo,
            border = BorderStroke(1.2.dp, if (securityState.hasPin) BrightEmerald else BorderViolet),
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            tint = if (securityState.hasPin) BrightEmerald else NeonGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Security Vault",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close security modal",
                            tint = TextSecondary
                        )
                    }
                }

                // Security Shield Status Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (securityState.hasPin) EmeraldMint.copy(alpha = 0.14f) else CardSlate,
                    border = BorderStroke(
                        1.dp,
                        if (securityState.hasPin) BrightEmerald.copy(alpha = 0.6f) else BorderViolet
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (securityState.hasPin) "VAULT SHIELD ACTIVE" else "PIN PROTECTION OFF",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (securityState.hasPin) BrightEmerald else NeonGold,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (securityState.hasPin) {
                                "4-digit Vault PIN protects your wallet actions and local Mint storage."
                            } else {
                                "Create a 4-digit PIN below to lock Deposits, Withdrawals, and App Launch."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = pinField,
                    onValueChange = { raw -> pinField = raw.filter { it.isDigit() }.take(4) },
                    label = {
                        Text(if (securityState.hasPin) "Current PIN to Disable (or New 4-Digit PIN)" else "Create 4-Digit Vault PIN")
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("security_pin_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrightEmerald,
                        unfocusedBorderColor = BorderViolet,
                        focusedLabelColor = BrightEmerald,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onSetPin(pinField)
                            if (pinField.length == 4) pinField = ""
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_vault_pin_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldMint,
                            contentColor = VoidObsidian
                        )
                    ) {
                        Text(
                            text = if (securityState.hasPin) "Update PIN" else "Enable PIN",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (securityState.hasPin) {
                        OutlinedButton(
                            onClick = {
                                onDisablePin(pinField)
                                pinField = ""
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, RoseAlert)
                        ) {
                            Text("Disable PIN", color = RoseAlert, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = BorderViolet)
                Spacer(modifier = Modifier.height(10.dp))

                // Security Guard Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Protect Deposit & Withdraw",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Require 4-digit PIN before any wallet transaction",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = securityState.requirePinForWallet,
                        onCheckedChange = onToggleWalletGuard,
                        colors = SwitchDefaults.colors(checkedTrackColor = EmeraldMint)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lock App on Launch",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Require PIN screen when opening SpinMint",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = securityState.lockAppOnLaunch,
                        onCheckedChange = onToggleLaunchLock,
                        colors = SwitchDefaults.colors(checkedTrackColor = EmeraldMint)
                    )
                }

                if (securityState.hasPin) {
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = onLockNow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("lock_vault_now_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, NeonGold)
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = NeonGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lock App Screen Now", color = NeonGold, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CardSlate,
                    border = BorderStroke(1.dp, BorderViolet)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "DataStore Local Vault • Fair-Play RNG Active • Anti-Negative Balance Guard",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PinLockOverlay(
    onUnlockAttempt: (String) -> Boolean
) {
    var entered by remember { mutableStateOf("") }
    var wrongPinError by remember { mutableStateOf(false) }

    fun appendDigit(d: String) {
        if (entered.length >= 4) return
        wrongPinError = false
        val next = entered + d
        entered = next
        if (next.length == 4) {
            val ok = onUnlockAttempt(next)
            if (!ok) {
                wrongPinError = true
                entered = ""
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidObsidian.copy(alpha = 0.97f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = "Vault Locked",
                tint = NeonGold,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "SpinMint Vault Locked",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (wrongPinError) "Incorrect PIN. Try again." else "Enter your 4-digit Vault PIN",
                style = MaterialTheme.typography.bodyMedium,
                color = if (wrongPinError) RoseAlert else TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 4 PIN Dots
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { idx ->
                    val filled = idx < entered.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (filled) NeonGold else CardSlateElevated)
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("CLR", "0", "DEL")
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { key ->
                            Surface(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clickable {
                                        when (key) {
                                            "CLR" -> {
                                                entered = ""
                                                wrongPinError = false
                                            }
                                            "DEL" -> if (entered.isNotEmpty()) entered = entered.dropLast(1)
                                            else -> appendDigit(key)
                                        }
                                    },
                                shape = CircleShape,
                                color = CardSlate,
                                border = BorderStroke(1.dp, BorderViolet)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (key == "DEL") {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                            contentDescription = "Delete digit",
                                            tint = TextSecondary
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            style = if (key == "CLR") MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleLarge,
                                            color = TextPrimary,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
