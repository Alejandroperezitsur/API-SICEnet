package com.example.marsphotos.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.marsphotos.MarsPhotosApplication
import com.example.marsphotos.data.SNRepository
import com.example.marsphotos.model.ProfileStudent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/**
 * UI state para la pantalla de Perfil
 */
sealed interface ProfileUiState {
    object Loading : ProfileUiState
    data class Success(val profile: ProfileStudent) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

/**
 * ViewModel para mostrar el perfil académico del estudiante
 */
class ProfileViewModel(private val snRepository: SNRepository) : ViewModel() {
    
    var profileUiState: ProfileUiState by mutableStateOf(ProfileUiState.Loading)
        private set
    
    /**
     * Carga el perfil académico del estudiante
     */
    fun loadProfile(matricula: String) {
        viewModelScope.launch {
            android.util.Log.d("ProfileVM", "Iniciando carga de perfil para $matricula")
            profileUiState = ProfileUiState.Loading
            profileUiState = try {
                val profile = withContext(Dispatchers.IO) {
                    android.util.Log.d("ProfileVM", "Llamando a snRepository.profile...")
                    val result = snRepository.profile(matricula)
                    android.util.Log.d("ProfileVM", "snRepository.profile retornó: $result")
                    result
                }
                android.util.Log.d("ProfileVM", "Carga exitosa, actualizando estado a Success")
                ProfileUiState.Success(profile)
            } catch (e: IOException) {
                android.util.Log.e("ProfileVM", "Error de IO", e)
                ProfileUiState.Error("Error de conexión: ${e.message}")
            } catch (e: HttpException) {
                android.util.Log.e("ProfileVM", "Error HTTP", e)
                ProfileUiState.Error("Error del servidor: ${e.message}")
            } catch (e: Throwable) {
                android.util.Log.e("ProfileVM", "Error inesperado (Throwable)", e)
                ProfileUiState.Error("Error inesperado: ${e.message}")
            }
        }
    }
    
    /**
     * Factory para crear instancias de ProfileViewModel
     */
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as MarsPhotosApplication)
                val snRepository = application.container.snRepository
                ProfileViewModel(snRepository = snRepository)
            }
        }
    }
}
