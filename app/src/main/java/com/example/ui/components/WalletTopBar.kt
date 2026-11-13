package com.example.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.BorderViolet
import com.example.ui.theme.BrightEmerald
import com.example.ui.theme.CardSlate
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidObsidian
import java.text.NumberFormat
import java.util.Locale

@Composable
fun WalletTopBar(
    coins: Long,
    inrBalance: Double,
    usdBalance: Double,
    isMuted: Boolean,
    isSecurityPinActive: Boolean,
    isAdmin: Boolean = false,
    userDisplayName: String = "Player",
    onToggleMute: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenAdmin: () -> Unit = {},
    onOpenAccount: () -> Unit = {},
    onOpenDeposit: () -> Unit,
    onOpenWithdraw: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedCoins by animateIntAsState(
        targetValue = coins.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        animationSpec = tween(durationMillis = 420),
        label = "wallet_coins"
    )
    val formattedCoins = NumberFormat.getNumberInstance(Locale.US).format(animatedCoins)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CardSlate,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(NeonGold.copy(alpha = 0.55f), BorderViolet, EmeraldMint.copy(alpha = 0.55f))
            )
        ),
        tonalElevation = 6.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_vault_banner),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.32f)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                DeepIndigo.copy(alpha = 0.88f),
                                CardSlate.copy(alpha = 0.72f),
                                DeepIndigo.copy(alpha = 0.90f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(NeonGold, Color(0xFFB4690E))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MonetizationOn,
                                contentDescription = stringResource(R.string.wallet_balance_label),
                                tint = VoidObsidian,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "SPINMINT • ${userDisplayName.substringBefore(" ").uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "1,000 Coins = ₹10 • $0.10",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                maxLines = 1
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isAdmin) {
                            IconButton(
                                onClick = onOpenAdmin,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("admin_panel_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AdminPanelSettings,
                                    contentDescription = "Admin 1% Revenue Console",
                                    tint = NeonGold
                                )
                            }
                        }
                        IconButton(
                            onClick = onOpenSecurity,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("security_vault_button")
                        ) {
                            Icon(
                                imageVector = if (isSecurityPinActive) Icons.Rounded.VerifiedUser else Icons.Rounded.Shield,
                                contentDescription = "Security Vault",
                                tint = if (isSecurityPinActive) BrightEmerald else TextSecondary
                            )
                        }
                        IconButton(
                            onClick = onOpenHistory,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("history_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                contentDescription = stringResource(R.string.history_cta),
                                tint = TextSecondary
                            )
                        }
                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("mute_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = stringResource(
                                    if (isMuted) R.string.unmute_sound_desc else R.string.mute_sound_desc
                                ),
                                tint = if (isMuted) TextMuted else NeonGold
                            )
                        }
                        IconButton(
                            onClick = onOpenAccount,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = stringResource(R.string.account_logout_desc),
                                tint = NeonGold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = formattedCoins,
                                style = MaterialTheme.typography.headlineMedium,
                                color = NeonGold,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "COINS",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonGold.copy(alpha = 0.85f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = EmeraldMint.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, EmeraldMint.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "≈ ₹%.2f • $%.2f",
                                    inrBalance,
                                    usdBalance
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = BrightEmerald,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onOpenDeposit,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("open_deposit_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.2.dp, NeonGold),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGold),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Add",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onOpenWithdraw,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("open_withdraw_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldMint,
                                contentColor = VoidObsidian
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.withdraw_cta),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
