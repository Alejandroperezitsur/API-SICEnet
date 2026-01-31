/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.marsphotos.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.marsphotos.ui.screens.LoginScreen
import com.example.marsphotos.ui.screens.LoginViewModel
import com.example.marsphotos.ui.screens.ProfileScreen
import com.example.marsphotos.ui.screens.ProfileViewModel

/**
 * Aplicación principal con navegación entre Login y Profile
 */
@Composable
fun MarsPhotosApp() {
    // Estado para controlar la navegación
    var currentScreen by remember { mutableStateOf(AppScreen.LOGIN) }
    var userMatricula by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (currentScreen) {
            AppScreen.LOGIN -> {
                val loginViewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory)
                
                LoginScreen(
                    loginUiState = loginViewModel.loginUiState,
                    matricula = loginViewModel.matricula,
                    contrasenia = loginViewModel.contrasenia,
                    onMatriculaChange = loginViewModel::updateMatricula,
                    onContraseniaChange = loginViewModel::updateContrasenia,
                    onLoginClick = loginViewModel::login,
                    onLoginSuccess = { matricula ->
                        userMatricula = matricula
                        currentScreen = AppScreen.PROFILE
                        loginViewModel.resetState()
                    },
                    onResetForm = loginViewModel::resetForm
                )
            }

            AppScreen.PROFILE -> {
                val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
                
                // Cargar perfil cuando se abre la pantalla
                LaunchedEffect(userMatricula) {
                    if (userMatricula.isNotEmpty()) {
                        profileViewModel.loadProfile(userMatricula)
                    }
                }
                
                ProfileScreen(
                    profileUiState = profileViewModel.profileUiState,
                    onBackClick = {
                        currentScreen = AppScreen.LOGIN
                        userMatricula = ""
                    }
                )
            }
        }
    }
}

/**
 * Enum para controlar las pantallas de la aplicación
 */
enum class AppScreen {
    LOGIN,
    PROFILE
}
