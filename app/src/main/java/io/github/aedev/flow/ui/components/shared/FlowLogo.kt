package io.github.aedev.flow.ui.components.shared

import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import io.github.aedev.flow.R

@Composable
fun FlowLogo(modifier: Modifier = Modifier) {
    Image(
        painterResource(R.drawable.ic_flow_logo),
        contentDescription = null,
        modifier = modifier,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
    )
}
