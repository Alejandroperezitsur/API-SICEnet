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
import com.example.marsphotos.model.BodyAccesoResponse
import com.example.marsphotos.model.EnvelopeSobreAcceso
import com.example.marsphotos.model.ProfileStudent
import com.example.marsphotos.model.Usuario
import com.example.marsphotos.network.SICENETWService
import com.example.marsphotos.network.bodyacceso
import com.example.marsphotos.network.bodyperfil
import okhttp3.RequestBody.Companion.toRequestBody
import org.simpleframework.xml.core.Persister
import java.io.IOException

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
    override suspend fun acceso(matricula: String, contrasenia: String): String {
        return ""
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
            val response = snApiService.acceso(soapBody.toRequestBody("text/xml; charset=utf-8".toMediaType()))
            
            val xmlString = response.string()
            Log.d("SNRepository", "Respuesta SOAP: $xmlString")
            
            // Parsear la respuesta XML
            val persister = Persister()
            val envelope = persister.read(EnvelopeSobreAcceso::class.java, xmlString)
            
            val result = envelope.body?.accesoLoginResponse?.accesoLoginResult
            Log.d("SNRepository", "Resultado: $result")
            
            // Guardar la matrícula si la autenticación fue exitosa
            if (result?.lowercase()?.contains("true") == true || result?.lowercase()?.contains("ok") == true) {
                userMatricula = matricula
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("SNRepository", "Error en autenticación", e)
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
            
            // Por ahora retornamos un perfil con la matrícula
            // En una implementación completa, parseamos el XML con la estructura del DataSet
            ProfileStudent(
                matricula = matricula,
                nombre = "Nombre",
                apellidos = "Apellidos",
                carrera = "Carrera",
                semestre = "Semestre",
                promedio = "0.0",
                estado = "Activo",
                statusMatricula = "Vigente"
            )
        } catch (e: Exception) {
            Log.e("SNRepository", "Error al obtener perfil", e)
            ProfileStudent()
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
private fun String.toMediaType(): okhttp3.MediaType {
    return okhttp3.MediaType.parse(this) ?: okhttp3.MediaType.parse("application/octet-stream")!!
}
