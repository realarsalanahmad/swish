package io.github.aedev.flow.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.R
import io.github.aedev.flow.data.backup.BackupOperation
import io.github.aedev.flow.ui.components.shared.FlowProgressBanner
import io.github.aedev.flow.ui.components.shared.TakeoutGuide

@Composable
internal fun WelcomeTakeoutCard(
    operation: BackupOperation,
    imported: Boolean,
    onImport: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.swish_takeout_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.swish_takeout_description), style = MaterialTheme.typography.bodyMedium)
            TakeoutGuide()
            if (operation is BackupOperation.Running) {
                FlowProgressBanner(
                    operation.headline,
                    operation.current,
                    operation.total,
                    note = stringResource(R.string.import_running_background),
                )
            }
            if (imported) Text(stringResource(R.string.swish_takeout_complete))
            Button(onClick = onImport, enabled = operation !is BackupOperation.Running, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (imported) R.string.swish_takeout_more else R.string.swish_takeout_upload))
            }
        }
    }
}
