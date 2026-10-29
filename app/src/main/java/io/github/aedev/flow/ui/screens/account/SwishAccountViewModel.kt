package io.github.aedev.flow.ui.screens.account

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aedev.flow.data.account.SwishAccountRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SwishAccountViewModel
    @Inject
    constructor(
        private val accounts: SwishAccountRepository,
    ) : ViewModel() {
        val state = accounts.state
        val configured: Boolean get() = accounts.configured

        init {
            viewModelScope.launch { accounts.initialize() }
        }

        fun login(
            email: String,
            password: String,
        ) {
            viewModelScope.launch { accounts.login(email, password) }
        }

        fun register(
            name: String,
            email: String,
            password: String,
        ) {
            viewModelScope.launch { accounts.register(name, email, password) }
        }

        fun logout() {
            viewModelScope.launch { accounts.logout() }
        }

        fun saveProfile() {
            viewModelScope.launch { accounts.saveProfile() }
        }

        fun restoreProfile() {
            viewModelScope.launch { accounts.restoreProfile() }
        }

        fun resetProfile() {
            viewModelScope.launch { accounts.resetProfile() }
        }
    }

@Composable
internal fun sharedSwishAccountViewModel(): SwishAccountViewModel =
    hiltViewModel(checkNotNull(LocalActivity.current as? ViewModelStoreOwner))
