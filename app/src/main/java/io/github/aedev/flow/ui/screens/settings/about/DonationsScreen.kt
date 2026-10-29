package io.github.aedev.flow.ui.screens.settings.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.R
import io.github.aedev.flow.ui.components.settings.SettingsPage

@Composable
fun DonationsScreen(onNavigateBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val channel = stringResource(R.string.swish_creator_url)
    SettingsPage(title = stringResource(R.string.swish_support_title), onBack = onNavigateBack) {
        item("support.creator") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.swish_support_description), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { runCatching { uriHandler.openUri(channel) } }) {
                    Icon(Icons.Outlined.OpenInNew, contentDescription = null)
                    Text(stringResource(R.string.swish_creator_channel))
                }
            }
        }
    }
}
