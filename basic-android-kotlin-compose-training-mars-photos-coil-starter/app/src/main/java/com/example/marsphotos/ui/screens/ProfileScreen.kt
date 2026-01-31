package com.example.marsphotos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marsphotos.model.ProfileStudent
import com.example.marsphotos.ui.theme.MarsPhotosTheme

/**
 * Pantalla que muestra el perfil académico del estudiante
 */
@Composable
fun ProfileScreen(
    profileUiState: ProfileUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top AppBar
        @OptIn(ExperimentalMaterial3Api::class)
        TopAppBar(
            title = { Text("Perfil Académico") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Atrás"
                    )
                }
            }
        )

        // Contenido principal
        when (profileUiState) {
            is ProfileUiState.Loading -> {
                LoadingProfileScreen(modifier = Modifier.fillMaxSize())
            }
            is ProfileUiState.Success -> {
                ProfileDetailScreen(
                    profile = profileUiState.profile,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
            is ProfileUiState.Error -> {
                ProfileErrorScreen(
                    error = profileUiState.message,
                    onRetryClick = onBackClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Pantalla con los detalles del perfil
 */
@Composable
fun ProfileDetailScreen(
    profile: ProfileStudent,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Información Personal",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Divider()

                ProfileInfoRow(label = "Matrícula", value = profile.matricula)
                ProfileInfoRow(label = "Nombre", value = profile.nombre)
                ProfileInfoRow(label = "Apellidos", value = profile.apellidos)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Información Académica",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Divider()

                ProfileInfoRow(label = "Carrera", value = profile.carrera)
                ProfileInfoRow(label = "Semestre", value = profile.semestre)
                ProfileInfoRow(label = "Promedio", value = profile.promedio)
                ProfileInfoRow(label = "Estado", value = profile.estado)
                ProfileInfoRow(label = "Status Matrícula", value = profile.statusMatricula)
            }
        }
    }
}

/**
 * Fila para mostrar información del perfil
 */
@Composable
fun ProfileInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = value,
            fontSize = 14.sp
        )
    }
}

/**
 * Pantalla de carga para el perfil
 */
@Composable
fun LoadingProfileScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(50.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Cargando perfil...")
    }
}

/**
 * Pantalla de error para el perfil
 */
@Composable
fun ProfileErrorScreen(
    error: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Error",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp)
                )

                Button(
                    onClick = onRetryClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Volver atrás")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileDetailScreenPreview() {
    MarsPhotosTheme {
        ProfileDetailScreen(
            profile = ProfileStudent(
                matricula = "S19120153",
                nombre = "Juan",
                apellidos = "Pérez García",
                carrera = "Ingeniería en Sistemas Computacionales",
                semestre = "6",
                promedio = "8.5",
                estado = "Activo",
                statusMatricula = "Vigente"
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingProfileScreenPreview() {
    MarsPhotosTheme {
        LoadingProfileScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileErrorScreenPreview() {
    MarsPhotosTheme {
        ProfileErrorScreen(
            error = "Error al cargar el perfil",
            onRetryClick = {}
        )
    }
}
