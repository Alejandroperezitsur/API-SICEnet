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
        return try {
            val soapBody = bodyacceso.format(matricula, contrasenia)
            Log.d("SNRepository", "===== INICIANDO AUTENTICACIÓN =====")
            Log.d("SNRepository", "Matrícula: $matricula")
            Log.d("SNRepository", "URL: https://sicenet.itsur.edu.mx/ws/wsalumnos.asmx")
            
            val response = snApiService.acceso(soapBody.toRequestBody("text/xml; charset=utf-8".toMediaType()))
            
            val xmlString = response.string()
            Log.i("SNRepository", "Respuesta SOAP recibida")
            
            // Extraer el JSON de la respuesta SOAP usando regex
            val jsonPattern = Regex("\\{.*\\}")
            val jsonMatch = jsonPattern.find(xmlString)
            
            if (jsonMatch == null) {
                Log.w("SNRepository", "❌ FALLO: No se encontró JSON en la respuesta")
                Log.d("SNRepository", "Respuesta completa:\n$xmlString")
                false
            } else {
                val jsonString = jsonMatch.value.trim()
                Log.d("SNRepository", "JSON extraído: $jsonString")
                
                // Parsear el JSON
                val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
                val accesoValue = jsonObject["acceso"]?.jsonPrimitive?.content
                
                Log.d("SNRepository", "Valor de 'acceso': $accesoValue")
                
                val success = when {
                    accesoValue == null -> {
                        Log.w("SNRepository", "❌ FALLO: Campo 'acceso' no encontrado en JSON")
                        false
                    }
                    accesoValue.lowercase() == "true" -> {
                        Log.d("SNRepository", "✅ ÉXITO: acceso es true")
                        userMatricula = matricula
                        true
                    }
                    accesoValue.lowercase() == "1" -> {
                        Log.d("SNRepository", "✅ ÉXITO: acceso es 1")
                        userMatricula = matricula
                        true
                    }
                    else -> {
                        Log.w("SNRepository", "❌ FALLO: acceso es $accesoValue")
                        false
                    }
                }
                
                Log.d("SNRepository", if (success) "✅ Autenticación exitosa" else "❌ Autenticación fallida")
                success
            }
        } catch (e: Exception) {
            Log.e("SNRepository", "❌ EXCEPCIÓN en autenticación: ${e.message}", e)
            e.printStackTrace()
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
                ProfileStudent(
                    matricula = alumno.matricula ?: matricula,
                    nombre = alumno.nombre ?: "",
                    apellidos = alumno.apellidos ?: "",
                    carrera = alumno.carrera ?: "",
                    semestre = alumno.semestre ?: "",
                    promedio = alumno.promedio ?: "0.0",
                    estado = alumno.estado ?: "Activo",
                    statusMatricula = alumno.statusMatricula ?: "Vigente"
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
