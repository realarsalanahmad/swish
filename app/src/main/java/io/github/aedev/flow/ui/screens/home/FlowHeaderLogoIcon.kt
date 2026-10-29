package io.github.aedev.flow.ui.screens.home

import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.aedev.flow.ui.components.shared.FlowLogo

internal fun Modifier.homeLogoClicks(
    clickLabel: String,
    longClickLabel: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
): Modifier = combinedClickable(onClickLabel = clickLabel, onLongClickLabel = longClickLabel, onClick = onClick, onLongClick = onLongClick)

@Composable
internal fun FlowHeaderLogoIcon(
    isDeepFlowActive: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isDeepFlowActive) {
        Icon(Icons.Outlined.VisibilityOff, contentDescription = null, modifier = modifier)
    } else {
        FlowLogo(modifier)
    }
}
