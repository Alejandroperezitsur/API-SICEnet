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
import retrofit2.HttpException
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

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    /**
     * Realiza la autenticación en SICENET
     */
    override suspend fun acceso(matricula: String, contrasenia: String): Boolean {
        Log.d("SNRepository", "===== INICIANDO AUTENTICACIÓN =====")
        Log.d("SNRepository", "Matrícula: $matricula")
        
        return try {
            val safeMatricula = escapeXml(matricula)
            val safeContrasenia = escapeXml(contrasenia)
            val soapBody = bodyacceso.format(safeMatricula.uppercase(), safeContrasenia)
            
            Log.d("SNRepository", "Enviando SOAP Body (truncado): ${soapBody.take(100)}...")
            
            // Usamos text/xml; charset=utf-8 explícitamente
            val response = try {
                snApiService.acceso(soapBody.toRequestBody("text/xml;charset=utf-8".toMediaType()))
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("SNRepository", "❌ HTTP Error ${e.code()}: $errorBody")
                throw e
            }
            
            val xmlString = response.string()
            Log.d("SNRepository", "Respuesta XML recibida: $xmlString")
            
            // Verificación robusta de éxito
            if (xmlString.contains("true", ignoreCase = true) || xmlString.contains(">1<")) {
                userMatricula = matricula
                return true
            }
            val startIdx = xmlString.indexOf('{')
            val endIdx = xmlString.lastIndexOf('}')
            
            if (startIdx != -1 && endIdx != -1) {
                val jsonString = xmlString.substring(startIdx, endIdx + 1).trim()
                Log.d("SNRepository", "JSON extraído: $jsonString")
                
                try {
                    val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
                    val accesoValue = jsonObject["acceso"]?.jsonPrimitive?.content
                    
                    Log.d("SNRepository", "Valor de 'acceso': $accesoValue")
                    
                    if (accesoValue?.lowercase() == "true" || accesoValue == "1") {
                        userMatricula = matricula
                        return true
                    }
                } catch (e: Exception) {
                    Log.e("SNRepository", "Error parseando JSON interno", e)
                }
            } else {
                // Si no es JSON, intentar parsear XML estándar usando SimpleXML
                // Aquí podríamos usar el persister si fuera necesario, pero por ahora
                // verificamos si contiene indicadores de éxito simples
                if (xmlString.contains("true") || xmlString.contains(">true<")) {
                    userMatricula = matricula
                    return true
                }
            }
            
            false
        } catch (e: Exception) {
            Log.e("SNRepository", "❌ Error en autenticación: ${e.message}", e)
            throw e
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
        Log.d("SNRepository", "===== OBTENIENDO PERFIL (getAlumnoAcademico) =====")
        
        var profile = ProfileStudent(matricula = matricula)
        var alumnoInfo: com.example.marsphotos.model.AlumnoInfo? = null
        
        // 1. Obtener datos via SOAP (getAlumnoAcademico) - No toma parámetros, usa sesión
        try {
            val soapBody = bodyperfil
            Log.d("SNRepository", "Pidiendo Perfil SOAP...")
            
            val response = try {
                snApiService.perfil(soapBody.toRequestBody("text/xml; charset=utf-8".toMediaType()))
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("SNRepository", "❌ HTTP Error ${e.code()} en Perfil: $errorBody")
                null
            }

            if (response != null) {
                val xmlString = response.string()
                Log.d("SNRepository", "Respuesta Perfil XML (truncada): ${xmlString.take(200)}")
                
                try {
                    val persister = Persister()
                    // Usamos el nuevo modelo EnvelopeSobreAlumno
                    val envelope = persister.read(com.example.marsphotos.model.EnvelopeSobreAlumno::class.java, xmlString)
                    val resultXml = envelope.body?.getAlumnoAcademicoResponse?.getAlumnoAcademicoResult
                    
                    if (!resultXml.isNullOrBlank()) {
                        Log.d("SNRepository", "Contenido de getAlumnoAcademicoResult extraído, intentando parsear DataSet...")
                        // El resultado suele ser otro XML (DataSet) codificado - lo intentamos parsear si contiene etiquetas
                        if (resultXml.contains("<Alumno>")) {
                            val innerXml = if (resultXml.contains("<DataSet")) {
                                resultXml.substring(resultXml.indexOf("<DataSet"))
                            } else {
                                resultXml
                            }
                            alumnoInfo = persister.read(com.example.marsphotos.model.PerfilDataSet::class.java, innerXml).alumno
                            Log.d("SNRepository", "✅ Datos de AlumnoInfo parseados: ${alumnoInfo?.nombre}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SNRepository", "Error parseando XML de Respuesta Perfil", e)
                }
            }
        } catch (e: Exception) {
            Log.e("SNRepository", "Excepción en flujo SOAP Perfil", e)
        }

        // 2. Enriquecer con Scraping (JSoup) para lo que SOAP no da o como fallback
        try {
            Log.d("SNRepository", "Iniciando scraping HTML (frmPlataformaAlumno.aspx)...")
            val resp = snApiService.plataforma()
            val html = resp.string()
            val doc = Jsoup.parse(html, "https://sicenet.itsur.edu.mx")

            // Actualizamos los campos si los encontramos en el HTML
            val fotoUrl = doc.selectFirst("#imgAlumno, [src*=foto], [src*=Foto]")?.absUrl("src") ?: ""
            val especialidad = doc.selectFirst("td:contains(Especialidad) + td, #lblEspecialidad")?.text()?.trim() ?: ""
            val cdtsReunidos = doc.selectFirst("td:contains(Cdts. Reunidos) + td, #lblCdtsReunidos")?.text()?.trim() ?: "0"
            val cdtsActuales = doc.selectFirst("td:contains(Cdts. Actuales) + td, #lblCdtsActuales")?.text()?.trim() ?: "0"
            val semActual = doc.selectFirst("td:contains(Sem. Actual) + td, #lblSemActual")?.text()?.trim() ?: "0"
            val inscrito = doc.selectFirst("td:contains(Inscrito) + td, #lblInscrito")?.text()?.trim() ?: "NO"
            val reinscripcion = doc.selectFirst("td:contains(Fecha) + td, #lblFechaReinscripcion")?.text()?.trim() ?: "PENDIENTE"
            val estatusAcademico = doc.selectFirst("td:contains(Estatus Académico) + td, #lblEstatusAcademico")?.text()?.trim() ?: ""
            val estatusAlumno = doc.selectFirst("td:contains(Estatus) + td, #lblEstatus")?.text()?.trim() ?: ""
            val sinAdeudos = doc.select("td, span").find { it.text().contains("ADEUDOS") }?.text()?.trim() ?: ""

            val operaciones = mutableListOf<String>()
            doc.select("a").forEach { a ->
                val txt = a.text().uppercase()
                if (txt.contains("CALIFICACIONES") || txt.contains("KARDEX") || 
                    txt.contains("MONITOREO") || txt.contains("REINSCRIPCION") || 
                    txt.contains("CARGA") || txt.contains("CERRAR SESION")) {
                    operaciones.add(txt)
                }
            }

            // Construimos el perfil final combinando SOAP y HTML
            profile = ProfileStudent(
                matricula = alumnoInfo?.matricula ?: matricula,
                nombre = alumnoInfo?.nombre ?: "",
                apellidos = alumnoInfo?.apellidos ?: "",
                carrera = alumnoInfo?.carrera ?: "",
                semestre = if (semActual != "0") semActual else (alumnoInfo?.semestre ?: ""),
                promedio = alumnoInfo?.promedio ?: "",
                estado = alumnoInfo?.estado ?: "",
                statusMatricula = alumnoInfo?.statusMatricula ?: "",
                fotoUrl = fotoUrl,
                especialidad = especialidad,
                cdtsReunidos = cdtsReunidos,
                cdtsActuales = cdtsActuales,
                semActual = semActual,
                inscrito = inscrito,
                estatusAcademico = estatusAcademico,
                estatusAlumno = estatusAlumno,
                reinscripcionFecha = reinscripcion,
                sinAdeudos = sinAdeudos,
                operaciones = operaciones.distinct()
            )
        } catch (e: Exception) {
            Log.e("SNRepository", "Error durante el scraping HTML", e)
            // Si el scraping falla pero tenemos algo de SOAP (aunque sea el nombre genérico), lo retornamos
            if (alumnoInfo != null) {
                profile = ProfileStudent(
                    matricula = matricula,
                    nombre = alumnoInfo.nombre ?: "Alumno",
                    apellidos = alumnoInfo.apellidos ?: ""
                )
            }
        }
        
        return profile
    }

    /**
     * Obtiene la matrícula del usuario autenticado
     */
    override suspend fun getMatricula(): String {
        return userMatricula
    }
}

// Importar MediaType para usar toMediaType()
