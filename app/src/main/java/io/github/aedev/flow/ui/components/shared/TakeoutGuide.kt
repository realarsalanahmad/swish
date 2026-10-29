package io.github.aedev.flow.ui.components.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import io.github.aedev.flow.R

@Composable
internal fun TakeoutGuide() {
    val links = LocalUriHandler.current
    Column {
        Text(stringResource(R.string.swish_takeout_guide), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { links.openUri("https://takeout.google.com/") }) {
            Text(stringResource(R.string.swish_takeout_get))
        }
    }
}
