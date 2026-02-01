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
package com.example.marsphotos.data

import android.util.Log
import com.example.marsphotos.model.AccesoLoginResponse
import com.example.marsphotos.model.AlumnoInfo
import com.example.marsphotos.model.BodyAccesoResponse
import com.example.marsphotos.model.EnvelopeSobreAcceso
import com.example.marsphotos.model.PerfilDataSet
import com.example.marsphotos.model.ProfileStudent
import com.example.marsphotos.model.Usuario
import com.example.marsphotos.network.SICENETWService
import com.example.marsphotos.network.bodyacceso
import com.example.marsphotos.network.bodyperfil
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.simpleframework.xml.core.Persister
import org.jsoup.Jsoup
import java.net.URL

/**
 * Interface para acceder a los servicios SICENET
 */
interface SNRepository {
    /** Autenticación en SICENET */
    suspend fun acceso(matricula: String, contrasenia: String): Boolean
    
    /** Obtiene el objeto Usuario autenticado */
    suspend fun accesoObjeto(matricula: String, contrasenia: String): Usuario
    
    /** Obtiene el perfil académico del estudiante */
    suspend fun profile(matricula: String): ProfileStudent
    
    /** Obtiene la matrícula del usuario autenticado */
    suspend fun getMatricula(): String
}

/**
 * Implementación local usando base de datos
 */
class DBLocalSNRepository(val apiDB: Any) : SNRepository {
    override suspend fun acceso(matricula: String, contrasenia: String): Boolean {
        return false
    }

    override suspend fun accesoObjeto(matricula: String, contrasenia: String): Usuario {
        return Usuario(matricula = "")
    }

    override suspend fun profile(matricula: String): ProfileStudent {
        return ProfileStudent()
    }

    override suspend fun getMatricula(): String {
        return ""
    }
}

/**
 * Implementación de red que conecta con el servicio SICENET SOAP
 */
class NetworSNRepository(
    private val snApiService: SICENETWService
) : SNRepository {
    
    private var userMatricula: String = ""

    /**
     * Realiza la autenticación en SICENET
     */
    override suspend fun acceso(matricula: String, contrasenia: String): Boolean {
        Log.d("SNRepository", "===== INICIANDO AUTENTICACIÓN =====")
        Log.d("SNRepository", "Matrícula: $matricula")
        
        return try {
            val soapBody = bodyacceso.format(matricula, contrasenia)
            val response = snApiService.acceso(soapBody.toRequestBody("text/xml; charset=utf-8".toMediaType()))
            val xmlString = response.string()
            
            Log.d("SNRepository", "Respuesta recibida, intentando extraer JSON...")
            
            // Extraer el JSON de la respuesta SOAP
            val startIdx = xmlString.indexOf('{')
            val endIdx = xmlString.lastIndexOf('}')
            
            when {
                startIdx == -1 || endIdx == -1 -> {
                    Log.w("SNRepository", "❌ No se encontró JSON en la respuesta")
                    Log.d("SNRepository", "Respuesta: $xmlString")
                    false
                }
                else -> {
                    val jsonString = xmlString.substring(startIdx, endIdx + 1).trim()
                    Log.d("SNRepository", "JSON extraído: $jsonString")
                    
                    try {
                        val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
                        val accesoValue = jsonObject["acceso"]?.jsonPrimitive?.content
                        
                        Log.d("SNRepository", "Valor de 'acceso': $accesoValue")
                        
                        when {
                            accesoValue?.lowercase() == "true" -> {
                                Log.d("SNRepository", "✅ ÉXITO: acceso es true")
                                userMatricula = matricula
                                true
                            }
                            accesoValue?.lowercase() == "1" -> {
                                Log.d("SNRepository", "✅ ÉXITO: acceso es 1")
                                userMatricula = matricula
                                true
                            }
                            else -> {
                                Log.w("SNRepository", "❌ Autenticación fallida: acceso=$accesoValue")
                                false
                            }
                        }
                    } catch (parseE: Exception) {
                        Log.e("SNRepository", "❌ Error parseando JSON: ${parseE.message}", parseE)
                        false
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SNRepository", "❌ Error en autenticación: ${e.message}", e)
            false
        }
    }

    /**
     * Obtiene el usuario autenticado como objeto
     */
    override suspend fun accesoObjeto(matricula: String, contrasenia: String): Usuario {
        return if (acceso(matricula, contrasenia)) {
            Usuario(matricula = matricula)
        } else {
            Usuario(matricula = "")
        }
    }

    /**
     * Obtiene el perfil académico del estudiante
     */
    override suspend fun profile(matricula: String): ProfileStudent {
        return try {
            val soapBody = bodyperfil.format(matricula)
            val response = snApiService.perfil(soapBody.toRequestBody("text/xml; charset=utf-8".toMediaType()))
            
            val xmlString = response.string()
            Log.d("SNRepository", "Respuesta Perfil XML: $xmlString")
            
            // Parsear la respuesta SOAP
            val persister = Persister()
            
            // Intenta parsear como PerfilDataSet si contiene datos
            val profileData = try {
                if (xmlString.contains("<Alumno>") || xmlString.contains("<Alumno ")) {
                    persister.read(PerfilDataSet::class.java, xmlString)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.w("SNRepository", "No se pudo parsear como PerfilDataSet", e)
                null
            }
            
            // Si logramos parsear los datos, usarlos; si no, retornar con valores vacíos
            if (profileData?.alumno != null) {
                val alumno = profileData.alumno
                // Intentar también obtener información adicional desde la página HTML
                var fotoUrl = ""
                var especialidad = ""
                var cdtsReunidos = ""
                var cdtsActuales = ""
                var inscrito = ""
                var reinscripcion = ""
                var sinAdeudos = ""
                val operaciones = mutableListOf<String>()

                try {
                    val resp = snApiService.plataforma()
                    val html = resp.string()
                    val base = URL("https://sicenet.itsur.edu.mx")
                    val doc = Jsoup.parse(html, base.toString())

                    // Foto: buscar la primera imagen dentro de la zona de perfil
                    val img = doc.selectFirst("img#imgAlumno, img[src*=Foto], img[src*=foto], .foto img, img.alumno")
                    fotoUrl = img?.absUrl("src") ?: img?.attr("src") ?: ""

                    // Campos por label: buscar celdas que contengan los textos solicitados
                    fun nextText(label: String): String {
                        return doc.selectFirst("td:contains($label)")?.nextElementSibling()?.text()?.trim() ?: ""
                    }

                    especialidad = nextText("Especialidad").ifEmpty { nextText("TECNOLOGÍAS") }
                    cdtsReunidos = nextText("Cdts. Reunidos").ifEmpty { nextText("Créditos Reunidos") }
                    cdtsActuales = nextText("Cdts. Actuales").ifEmpty { nextText("Créditos Actuales") }
                    inscrito = nextText("Inscrito").ifEmpty { doc.selectFirst("span:contains(Inscrito)")?.text() ?: "" }
                    reinscripcion = nextText("Fecha").ifEmpty { nextText("Reinscripción") }
                    sinAdeudos = doc.selectFirst("td:contains(SIN ADEUDOS)")?.text()?.trim() ?: doc.selectFirst("span:contains(SIN ADEUDOS)")?.text() ?: ""

                    // Operaciones: buscar enlaces del menú de la plataforma
                    val ops = doc.select("a[href]")
                    for (op in ops) {
                        val t = op.text()?.trim() ?: ""
                        if (t.isNotEmpty() && (t.contains("CALIFICACIONES") || t.contains("KARDEX") || t.contains("REINSCRIPCION") || t.contains("CARGA ACADEMICA") || t.contains("MONITOREO") || t.contains("Cerrar"))) {
                            operaciones.add(t)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SNRepository", "No se pudo obtener la página HTML: ${e.message}")
                }

                ProfileStudent(
                    matricula = alumno.matricula ?: matricula,
                    nombre = alumno.nombre ?: "",
                    apellidos = alumno.apellidos ?: "",
                    carrera = alumno.carrera ?: "",
                    semestre = alumno.semestre ?: "",
                    promedio = alumno.promedio ?: "0.0",
                    estado = alumno.estado ?: "Activo",
                    statusMatricula = alumno.statusMatricula ?: "Vigente",
                    fotoUrl = fotoUrl,
                    especialidad = especialidad,
                    cdtsReunidos = cdtsReunidos,
                    cdtsActuales = cdtsActuales,
                    inscrito = inscrito,
                    reinscripcionFecha = reinscripcion,
                    sinAdeudos = sinAdeudos,
                    operaciones = operaciones
                )
            } else {
                // Retornar perfil con datos básicos si no se puede parsear
                ProfileStudent(
                    matricula = matricula,
                    nombre = "Alumno",
                    apellidos = "",
                    carrera = "No disponible",
                    semestre = "N/A",
                    promedio = "N/A",
                    estado = "Activo",
                    statusMatricula = "Vigente"
                )
            }
        } catch (e: Exception) {
            Log.e("SNRepository", "Error al obtener perfil", e)
            ProfileStudent(
                matricula = matricula,
                nombre = "Error",
                apellidos = "No se pudo cargar el perfil",
                carrera = "",
                semestre = "",
                promedio = "0.0",
                estado = "Error",
                statusMatricula = ""
            )
        }
    }

    /**
     * Obtiene la matrícula del usuario autenticado
     */
    override suspend fun getMatricula(): String {
        return userMatricula
    }
}

// Importar MediaType para usar toMediaType()
