package com.kotla.anifloat.ui.screens

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kotla.anifloat.data.AnilistRepository
import com.kotla.anifloat.data.AuthRepository
import com.kotla.anifloat.data.UserMediaListResult
import com.kotla.anifloat.data.api.NetworkModule
import com.kotla.anifloat.data.model.MediaListEntry
import com.kotla.anifloat.data.model.Viewer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val animeList: List<MediaListEntry>, val viewer: Viewer?) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(
    private val repository: AnilistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        fetchAnimeList()
    }

    fun fetchAnimeList() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val result = repository.getCurrentUserAndList()
                val sorted = result.entries.sortedByDescending { it.updatedAt ?: 0L }
                _uiState.value = HomeUiState.Success(sorted, result.viewer)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val result = repository.getCurrentUserAndList()
                val sorted = result.entries.sortedByDescending { it.updatedAt ?: 0L }
                _uiState.value = HomeUiState.Success(sorted, result.viewer)
            } catch (_: Exception) {
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            onLoggedOut()
        }
    }

    companion object {
        fun Factory(application: Application): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val authRepo = AuthRepository(application)
                val repo = AnilistRepository(NetworkModule.api, authRepo)
                HomeViewModel(repo)
            }
        }
    }
}
