package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DAILY_REWARD_DAYS
import com.example.ui.theme.BorderViolet
import com.example.ui.theme.BrightEmerald
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CardSlateElevated
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidObsidian

@Composable
fun DailyStreakScreen(
    streakDay: Int,
    canClaimToday: Boolean,
    onClaimBonus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(16.dp),
            color = CardSlate,
            border = BorderStroke(1.dp, BorderViolet)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = NeonGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.daily_streak_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Day ${streakDay.coerceAtMost(7)} / 7",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrightEmerald,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Days 1..6 in 2-column grid, Day 7 wide jackpot card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val firstSix = DAILY_REWARD_DAYS.take(6)
            for (rowIndex in 0 until 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val leftDay = firstSix[rowIndex * 2]
                    val rightDay = firstSix[rowIndex * 2 + 1]
                    StreakDayCard(
                        dayNumber = leftDay.dayNumber,
                        coins = leftDay.coins,
                        subtitle = leftDay.title,
                        isClaimed = leftDay.dayNumber <= streakDay,
                        isCurrent = canClaimToday && leftDay.dayNumber == (streakDay + 1).coerceAtMost(7),
                        modifier = Modifier.weight(1f)
                    )
                    StreakDayCard(
                        dayNumber = rightDay.dayNumber,
                        coins = rightDay.coins,
                        subtitle = rightDay.title,
                        isClaimed = rightDay.dayNumber <= streakDay,
                        isCurrent = canClaimToday && rightDay.dayNumber == (streakDay + 1).coerceAtMost(7),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Day 7 Mega Jackpot Card
            val day7 = DAILY_REWARD_DAYS.last()
            val day7Claimed = day7.dayNumber <= streakDay
            val day7Current = canClaimToday && streakDay == 6
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = if (day7Current) CardSlateElevated else CardSlate,
                border = BorderStroke(
                    width = if (day7Current) 1.8.dp else 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = if (day7Claimed) {
                            listOf(EmeraldMint, BrightEmerald)
                        } else {
                            listOf(NeonGold, EmeraldMint)
                        }
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(NeonGold.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (day7Claimed) Icons.Rounded.CheckCircle else Icons.Rounded.EmojiEvents,
                                contentDescription = null,
                                tint = if (day7Claimed) BrightEmerald else NeonGold,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "DAY 7 • MEGA JACKPOT",
                                style = MaterialTheme.typography.labelLarge,
                                color = NeonGold,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = day7.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                    Text(
                        text = "+${day7.coins}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (day7Claimed) BrightEmerald else NeonGold,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onClaimBonus,
            enabled = canClaimToday,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .height(54.dp)
                .testTag("claim_daily_bonus_button"),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonGold,
                contentColor = VoidObsidian,
                disabledContainerColor = CardSlateElevated,
                disabledContentColor = TextSecondary
            )
        ) {
            Text(
                text = if (canClaimToday) {
                    stringResource(R.string.claim_bonus)
                } else {
                    stringResource(R.string.claimed_today)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun StreakDayCard(
    dayNumber: Int,
    coins: Long,
    subtitle: String,
    isClaimed: Boolean,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isCurrent -> NeonGold
        isClaimed -> EmeraldMint.copy(alpha = 0.7f)
        else -> BorderViolet
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (isCurrent) CardSlateElevated else CardSlate,
        border = BorderStroke(if (isCurrent) 1.6.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DAY $dayNumber",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCurrent) NeonGold else TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = when {
                        isClaimed -> Icons.Rounded.CheckCircle
                        isCurrent -> Icons.Rounded.MonetizationOn
                        else -> Icons.Rounded.Lock
                    },
                    contentDescription = null,
                    tint = when {
                        isClaimed -> BrightEmerald
                        isCurrent -> NeonGold
                        else -> TextMuted
                    },
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = "+$coins",
                style = MaterialTheme.typography.titleLarge,
                color = if (isClaimed) BrightEmerald else TextPrimary,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
    }
}
