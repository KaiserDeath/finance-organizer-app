package pe.moneyflow.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.moneyflow.core.designsystem.theme.Spacing

/**
 * The app's standard loading skeleton: an optional hero block followed by a stack of card blocks.
 *
 * One shared helper rather than a hand-rolled skeleton per screen, because a skeleton is only useful
 * if it *approximates the real layout* — an arbitrary set of grey boxes is just a different kind of
 * blank screen. Pass [heroHeight] on screens that lead with a summary card (budgets, savings,
 * accounts) and omit it on plain card lists.
 */
@Composable
fun SkeletonBlocks(
    modifier: Modifier = Modifier,
    heroHeight: Dp? = null,
    count: Int = 4,
    blockHeight: Dp = 88.dp,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        if (heroHeight != null) {
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(heroHeight))
        }
        repeat(count) {
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(blockHeight))
        }
    }
}

/**
 * A skeleton for avatar-plus-two-lines list rows, mirroring `TransactionRow`'s anatomy (44dp circular
 * avatar, a title line, a shorter meta line). Used where the loading content is a dense list rather
 * than a stack of cards.
 */
@Composable
fun SkeletonRows(
    modifier: Modifier = Modifier,
    count: Int = 6,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        repeat(count) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShimmerBox(modifier = Modifier.size(44.dp), shape = CircleShape)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.55f).height(14.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.32f).height(12.dp))
                }
            }
        }
    }
}
