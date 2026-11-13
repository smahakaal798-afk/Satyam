package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AdminCommissionRecord
import com.example.data.SPINMINT_ADMIN_EMAIL
import com.example.ui.theme.BorderViolet
import com.example.ui.theme.BrightEmerald
import com.example.ui.theme.CardSlate
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidObsidian
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminDashboardModal(
    adminEmail: String,
    totalCommissionCoins: Long,
    unclaimedCoins: Long,
    commissions: List<AdminCommissionRecord>,
    onDismiss: () -> Unit,
    onClaimCommissions: () -> Unit
) {
    val nf = NumberFormat.getNumberInstance(Locale.US)
    val totalInr = totalCommissionCoins * 0.01
    val unclaimedInr = unclaimedCoins * 0.01

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
            border = BorderStroke(1.4.dp, NeonGold),
            tonalElevation = 14.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AdminPanelSettings,
                            contentDescription = null,
                            tint = NeonGold,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Admin Revenue Vault",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = adminEmail.ifBlank { SPINMINT_ADMIN_EMAIL },
                                style = MaterialTheme.typography.labelSmall,
                                color = BrightEmerald
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close admin console",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 1% Commission Treasury Summary Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CardSlate,
                    border = BorderStroke(1.2.dp, NeonGold.copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "1% WINNING COMMISSION EARNED (ALL PLAYERS)",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGold,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "${nf.format(totalCommissionCoins)} COINS",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = NeonGold,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = String.format(Locale.US, "Total Value ≈ ₹%.2f", totalInr),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BrightEmerald,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Ready to Claim",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${nf.format(unclaimedCoins)} (₹${String.format(Locale.US, "%.2f", unclaimedInr)})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onClaimCommissions,
                            enabled = unclaimedCoins > 0L,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("claim_admin_commission_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldMint,
                                contentColor = VoidObsidian,
                                disabledContainerColor = DeepIndigo,
                                disabledContentColor = TextMuted
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MonetizationOn,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (unclaimedCoins > 0L) {
                                    "CLAIM +${nf.format(unclaimedCoins)} COINS TO MY WALLET"
                                } else {
                                    "ALL 1% COMMISSIONS CLAIMED"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // APK Export & Download Info Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = NeonCyan.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "APK Download & Player Distribution",
                                style = MaterialTheme.typography.labelLarge,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "In AI Studio: open Settings (gear/menu) -> Generate APK / Export ZIP to download and share the APK. Every player's 1% winning cut automatically routes here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BorderViolet)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "RECENT 1% COMMISSION FEED (${commissions.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (commissions.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = CardSlate,
                        border = BorderStroke(1.dp, BorderViolet)
                    ) {
                        Text(
                            text = "No winning payouts recorded yet. When any player wins coins in Spin, Tap, Ludo, Slots, or Cards, 1% automatically appears here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    val dateFmt = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(commissions, key = { it.id.ifBlank { "${it.playerUid}_${it.grossWinCoins}_${it.createdAt}" } }) { item ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = CardSlate,
                                border = BorderStroke(1.dp, BorderViolet)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${item.playerName} • ${item.sourceGame}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val timeLabel = item.createdAt?.toDate()?.let { dateFmt.format(it) }
                                            ?: dateFmt.format(Date())
                                        Text(
                                            text = "Win: ${nf.format(item.grossWinCoins)} • Net: ${nf.format(item.netPlayerCoins)} • $timeLabel",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "+${nf.format(item.commissionCoins)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = BrightEmerald,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.ExtraBold
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
