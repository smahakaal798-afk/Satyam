package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.GameUiState
import com.example.model.PaymentMethod
import com.example.model.SecurityState
import com.example.model.WithdrawalTransaction
import com.example.ui.theme.BorderViolet
import com.example.ui.theme.BrightEmerald
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CardSlateElevated
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonGold
import com.example.ui.theme.RoseAlert
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidObsidian
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WithdrawModal(
    coins: Long,
    inrBalance: Double,
    usdBalance: Double,
    securityState: SecurityState,
    onDismiss: () -> Unit,
    onSubmit: (PaymentMethod, String, String, String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf(PaymentMethod.UPI) }
    var accountInput by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var coinsInput by remember {
        mutableStateOf(
            if (coins >= GameUiState.MIN_WITHDRAW_COINS) coins.toString() else "10000"
        )
    }

    val enteredCoins = coinsInput.toLongOrNull() ?: 0L
    val meetsMinBalance = coins >= GameUiState.MIN_WITHDRAW_COINS

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
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = EmeraldMint,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.withdraw_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close withdraw modal",
                            tint = TextSecondary
                        )
                    }
                }

                // Balance Summary
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CardSlate,
                    border = BorderStroke(1.dp, BorderViolet)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Available Balance: ${NumberFormat.getNumberInstance(Locale.US).format(coins)} Coins",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format(Locale.US, "Cash Value: ₹%.2f  •  $%.2f", inrBalance, usdBalance),
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrightEmerald
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.min_withdraw_rule),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (meetsMinBalance) TextSecondary else RoseAlert
                        )
                    }
                }

                if (!meetsMinBalance) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = RoseAlert.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, RoseAlert.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = RoseAlert,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Need ${NumberFormat.getNumberInstance(Locale.US).format(GameUiState.MIN_WITHDRAW_COINS - coins)} more Coins to unlock withdrawal.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = RoseAlert
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select Payout Rail",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 2x2 Payment Method Selector
                val methods = PaymentMethod.entries
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0..1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0..1) {
                                val method = methods[row * 2 + col]
                                val selected = selectedMethod == method
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("payment_method_${method.name.lowercase()}")
                                        .clickable {
                                            selectedMethod = method
                                            accountInput = ""
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) method.accentColor.copy(alpha = 0.22f) else CardSlate,
                                    border = BorderStroke(
                                        width = if (selected) 1.6.dp else 1.dp,
                                        color = if (selected) method.accentColor else BorderViolet
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = method.displayName,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) method.accentColor else TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = accountInput,
                    onValueChange = { accountInput = it },
                    label = { Text(selectedMethod.accountLabel) },
                    placeholder = { Text(selectedMethod.placeholder, color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_account_input"),
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

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = coinsInput,
                    onValueChange = { raw -> coinsInput = raw.filter { it.isDigit() } },
                    label = { Text("Coins to Redeem") },
                    supportingText = {
                        Text(
                            text = String.format(
                                Locale.US,
                                "Payout: ₹%.2f  •  $%.2f",
                                enteredCoins * 0.01,
                                enteredCoins * 0.0001
                            ),
                            color = BrightEmerald
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_amount_input"),
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

                // Quick Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10_000L to "10K", 25_000L to "25K", 50_000L to "50K").forEach { (preset, label) ->
                        OutlinedButton(
                            onClick = { coinsInput = preset.toString() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderViolet),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                        }
                    }
                    OutlinedButton(
                        onClick = { coinsInput = coins.coerceAtLeast(10_000L).toString() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NeonGold.copy(alpha = 0.7f)),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("MAX", style = MaterialTheme.typography.labelLarge, color = NeonGold)
                    }
                }

                if (securityState.hasPin && securityState.requirePinForWallet) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { raw -> pinInput = raw.filter { it.isDigit() }.take(4) },
                        label = { Text("4-Digit Vault Security PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("withdraw_pin_input"),
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

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onSubmit(selectedMethod, accountInput, coinsInput, pinInput) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_withdraw_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldMint,
                        contentColor = VoidObsidian
                    )
                ) {
                    Text(
                        text = stringResource(R.string.submit_withdraw),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.simulated_disclaimer),
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
fun ReceiptDialog(
    transaction: WithdrawalTransaction,
    onDismiss: () -> Unit
) {
    val dateStr = remember(transaction.timestampMillis) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(transaction.timestampMillis))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = CardSlateElevated,
            border = BorderStroke(1.5.dp, EmeraldMint)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = BrightEmerald,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                val isDeposit = transaction.txType == "DEPOSIT"
                Text(
                    text = if (isDeposit) "Deposit Credited" else stringResource(R.string.receipt_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ref: ${transaction.referenceCode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonGold,
                    fontFamily = JetBrainsMonoFontFamily
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderViolet)
                Spacer(modifier = Modifier.height(12.dp))

                ReceiptRow("Type", if (isDeposit) "Coin Top-Up" else "Payout Request")
                ReceiptRow("Payment Method", transaction.methodName)
                ReceiptRow("Account", transaction.accountIdentifier)
                ReceiptRow(
                    if (isDeposit) "Coins Credited" else "Coins Redeemed",
                    "${if (isDeposit) "+" else "-"}${NumberFormat.getNumberInstance(Locale.US).format(transaction.coinsSpent)} Coins"
                )
                ReceiptRow("Cash Equivalent", String.format(Locale.US, "₹%.2f ($%.2f)", transaction.inrEquivalent, transaction.usdEquivalent))
                ReceiptRow("Date", dateStr)
                ReceiptRow("Status", transaction.status)

                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("close_receipt_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldMint,
                        contentColor = VoidObsidian
                    )
                ) {
                    Text("Done", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun TransactionHistoryDialog(
    transactions: List<WithdrawalTransaction>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = DeepIndigo,
            border = BorderStroke(1.dp, BorderViolet)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                            contentDescription = null,
                            tint = NeonGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.transaction_history_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close history",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (transactions.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.empty_history_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.empty_history_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(transactions, key = { it.id }) { tx ->
                            val isDeposit = tx.txType == "DEPOSIT"
                            val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(tx.timestampMillis))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = CardSlate,
                                border = BorderStroke(1.dp, BorderViolet)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${if (isDeposit) "DEPOSIT" else "WITHDRAW"} • ${tx.methodName} • ${tx.referenceCode}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isDeposit) BrightEmerald else NeonGold
                                        )
                                        Text(
                                            text = tx.status,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrightEmerald
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = tx.accountIdentifier,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%s₹%.2f", if (isDeposit) "+" else "-", tx.inrEquivalent),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isDeposit) BrightEmerald else NeonGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${if (isDeposit) "+" else "-"}${NumberFormat.getNumberInstance(Locale.US).format(tx.coinsSpent)} Coins • $dateStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
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
