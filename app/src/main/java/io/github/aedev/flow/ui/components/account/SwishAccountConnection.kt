package io.github.aedev.flow.ui.components.account

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aedev.flow.R
import io.github.aedev.flow.data.account.SwishAccountFailure
import io.github.aedev.flow.data.account.SwishAccountNotice
import io.github.aedev.flow.data.account.SwishAccountState
import io.github.aedev.flow.ui.screens.account.SwishAccountViewModel
import io.github.aedev.flow.ui.screens.account.sharedSwishAccountViewModel

@Composable
internal fun SwishAccountConnection(
    asIconButton: Boolean = false,
    asSignInButton: Boolean = false,
) {
    val viewModel = sharedSwishAccountViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var open by rememberSaveable { mutableStateOf(false) }
    if (asIconButton) {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Outlined.AccountCircle, stringResource(R.string.swish_account_title))
        }
    } else {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(state.user?.name ?: stringResource(if (asSignInButton) R.string.swish_account_start else R.string.swish_account_title))
        }
    }
    if (open) SwishAccountDialog(state, viewModel) { open = false }
}

@Composable
private fun SwishAccountDialog(
    state: SwishAccountState,
    viewModel: SwishAccountViewModel,
    onDismiss: () -> Unit,
) {
    var register by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var reset by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.user?.id) { password = "" }
    val enabled = state.initialized && !state.busy && viewModel.configured
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOff),
        title = { Text(stringResource(R.string.swish_account_title)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (!viewModel.configured) {
                    Text(stringResource(R.string.swish_account_not_configured), color = MaterialTheme.colorScheme.error)
                }
                val user = state.user
                if (user != null) {
                    Text(user.name, style = MaterialTheme.typography.titleMedium)
                    Text(user.email)
                    Text(stringResource(R.string.swish_account_profile_explanation))
                    Button(onClick = viewModel::saveProfile, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.swish_account_save))
                    }
                    OutlinedButton(onClick = viewModel::restoreProfile, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.swish_account_restore))
                    }
                    Text(stringResource(R.string.swish_account_restore_warning), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { reset = true }, enabled = enabled) { Text(stringResource(R.string.swish_account_reset)) }
                    TextButton(onClick = viewModel::logout, enabled = enabled) { Text(stringResource(R.string.swish_account_logout)) }
                } else {
                    Text(stringResource(R.string.swish_account_explanation))
                    if (register) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it.take(80) },
                            singleLine = true,
                            label = { Text(stringResource(R.string.swish_account_name)) },
                            enabled = enabled,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.take(254) },
                        singleLine = true,
                        label = { Text(stringResource(R.string.swish_account_email)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it.take(128) },
                        singleLine = true,
                        label = { Text(stringResource(R.string.swish_account_password)) },
                        supportingText = { Text(stringResource(R.string.swish_account_password_hint)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            if (register) viewModel.register(name, email, password) else viewModel.login(email, password)
                            password = ""
                        },
                        enabled =
                            enabled && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() &&
                                password.length in 12..128 && (!register || name.isNotBlank()),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(if (register) R.string.swish_account_register else R.string.swish_account_login)) }
                    TextButton(onClick = {
                        register = !register
                        password = ""
                    }, enabled = !state.busy) {
                        Text(stringResource(if (register) R.string.swish_account_have_account else R.string.swish_account_create_account))
                    }
                }
                state.failure?.let { Text(stringResource(accountFailureText(it)), color = MaterialTheme.colorScheme.error) }
                state.notice?.let {
                    Text(
                        stringResource(
                            when (it) {
                                SwishAccountNotice.PROFILE_SAVED -> R.string.swish_account_saved
                                SwishAccountNotice.PROFILE_RESTORED -> R.string.swish_account_restored
                                SwishAccountNotice.PROFILE_RESET -> R.string.swish_account_reset_done
                                SwishAccountNotice.SIGNED_OUT -> R.string.swish_account_signed_out
                            },
                        ),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.swish_account_close)) } },
    )
    if (reset && state.user != null) {
        AlertDialog(
            onDismissRequest = { reset = false },
            title = { Text(stringResource(R.string.swish_account_reset)) },
            text = { Text(stringResource(R.string.swish_account_reset_body)) },
            confirmButton = {
                TextButton(onClick = {
                    reset = false
                    viewModel.resetProfile()
                }, enabled = enabled) {
                    Text(stringResource(R.string.reset_profile_confirm))
                }
            },
            dismissButton = { TextButton(onClick = { reset = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private fun accountFailureText(failure: SwishAccountFailure): Int =
    when (failure) {
        SwishAccountFailure.INVALID_INPUT -> R.string.swish_account_invalid_input
        SwishAccountFailure.INVALID_CREDENTIALS -> R.string.swish_account_invalid_credentials
        SwishAccountFailure.EMAIL_IN_USE -> R.string.swish_account_email_in_use
        SwishAccountFailure.SESSION_EXPIRED -> R.string.swish_account_session_expired
        SwishAccountFailure.PROFILE_MISSING -> R.string.swish_account_profile_missing
        SwishAccountFailure.PROFILE_TOO_LARGE -> R.string.swish_account_profile_too_large
        SwishAccountFailure.PROFILE_INVALID -> R.string.swish_account_profile_invalid
        SwishAccountFailure.RATE_LIMITED -> R.string.swish_account_rate_limited
        SwishAccountFailure.UNAVAILABLE -> R.string.swish_account_unavailable
    }
