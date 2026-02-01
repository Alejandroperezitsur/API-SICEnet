package com.example.marsphotos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import com.example.marsphotos.model.ProfileStudent
import com.example.marsphotos.ui.theme.SICENETTheme
import coil.compose.AsyncImage

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailScreen(
    profile: ProfileStudent,
    modifier: Modifier = Modifier
) {
    var selectedOp by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ... (Información Personal Card - unchanged logic but keep it for context)
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Información Personal", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Divider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (profile.fotoUrl.isNotEmpty()) {
                        AsyncImage(
                            model = profile.fotoUrl,
                            contentDescription = "Foto",
                            modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.LightGray)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column {
                        ProfileInfoRow(label = "Matrícula", value = profile.matricula)
                        val fullName = if (profile.apellidos.isNotEmpty()) "${profile.nombre} ${profile.apellidos}" else profile.nombre
                        ProfileInfoRow(label = "Nombre", value = fullName)
                    }
                }
            }
        }

        // Información Académica
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Información Académica", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Divider()
                ProfileInfoRow(label = "Carrera", value = profile.carrera)
                ProfileInfoRow(label = "Estatus Académico", value = profile.estatusAcademico)
                ProfileInfoRow(label = "Especialidad", value = profile.especialidad)
                ProfileInfoRow(label = "Semestre", value = profile.semestre)
                ProfileInfoRow(label = "Promedio", value = profile.promedio)
                ProfileInfoRow(label = "Estado", value = profile.estado)
                ProfileInfoRow(label = "Status Matrícula", value = profile.statusMatricula)
                ProfileInfoRow(label = "Estatus Alumno", value = profile.estatusAlumno)
                Divider()
                ProfileInfoRow(label = "Cdts. Reunidos", value = profile.cdtsReunidos)
                ProfileInfoRow(label = "Cdts. Actuales", value = profile.cdtsActuales)
                ProfileInfoRow(label = "Inscrito", value = profile.inscrito)
                ProfileInfoRow(label = "Reinscripción", value = profile.reinscripcionFecha)
                
                if (profile.sinAdeudos.isNotEmpty()) {
                    Text(text = profile.sinAdeudos, color = Color(0xFF006400), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        // Operaciones Académicas Interactiva
        if (profile.operaciones.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Operaciones Académicas (Clic para ver)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Divider()
                    profile.operaciones.forEach { op ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                            shape = RoundedCornerShape(8.dp),
                            onClick = { selectedOp = op }
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = op, modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Icon(imageVector = Icons.Filled.Info, contentDescription = "Ver", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs para Detalles
    selectedOp?.let { op ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { selectedOp = null }) {
            Card(
                modifier = Modifier.fillMaxWidth(0.95f).height(600.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = op, style = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold))
                        IconButton(onClick = { selectedOp = null }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Cerrar")
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Column(modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f)) {
                        when {
                            op.contains("KARDEX") -> KardexView(profile.kardex)
                            op.contains("CARGA") -> CargaView(profile.cargaAcademica)
                            op.contains("CALIFICACIONES") -> CalificacionesView(profile.calificacionesParciales)
                            else -> Text("Contenido para $op no disponible aún o es solo informativo.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KardexView(kardex: List<com.example.marsphotos.model.MateriaKardex>) {
    if (kardex.isEmpty()) { Text("No hay datos en el Kardex."); return }
    kardex.forEach { mat ->
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(text = mat.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Calif: ${mat.calificacion}", fontSize = 12.sp)
                Text(text = mat.acreditacion, fontSize = 12.sp, color = Color.Gray)
            }
            Text(text = "Periodo: ${mat.periodo}", fontSize = 11.sp, color = Color.LightGray)
            Divider(modifier = Modifier.padding(top = 4.dp), thickness = 0.5.dp)
        }
    }
}

@Composable
fun CargaView(carga: List<com.example.marsphotos.model.MateriaCarga>) {
    if (carga.isEmpty()) { Text("No hay carga académica."); return }
    carga.forEach { mat ->
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(text = mat.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = mat.docente, fontSize = 12.sp, color = Color.Gray)
            Text(text = "Horarios: L:${mat.lunes} M:${mat.martes} Mi:${mat.miercoles} J:${mat.jueves} V:${mat.viernes}", fontSize = 11.sp)
            Divider(modifier = Modifier.padding(top = 4.dp), thickness = 0.5.dp)
        }
    }
}

@Composable
fun CalificacionesView(parciales: List<com.example.marsphotos.model.MateriaParcial>) {
    if (parciales.isEmpty()) { Text("No hay calificaciones parciales."); return }
    parciales.forEach { mat ->
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(text = mat.materia, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                mat.parciales.forEachIndexed { index, score ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("U${index+1}", fontSize = 10.sp, color = Color.Gray)
                        Text(score, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Divider(modifier = Modifier.padding(top = 4.dp), thickness = 0.5.dp)
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
    SICENETTheme {
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
    SICENETTheme {
        LoadingProfileScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileErrorScreenPreview() {
    SICENETTheme {
        ProfileErrorScreen(
            error = "Error al cargar perfil",
            onRetryClick = {}
        )
    }
}
