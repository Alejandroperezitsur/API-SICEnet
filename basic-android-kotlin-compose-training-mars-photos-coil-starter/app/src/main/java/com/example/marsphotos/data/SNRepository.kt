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
        Log.e("SNRepository", "===== INICIANDO OBTENCIÓN DE PERFIL =====")
        
        var nombre = ""
        var carrera = ""
        var especialidad = ""
        var semestre = ""
        var promedio = ""
        var cdtAc = "0"
        var cdtAct = "0"
        var inscritoStr = "NO"
        var fReins = ""
        var estatusAlu = ""
        var estatusAcad = ""
        var fotoUrl = ""
        var sinAdeudos = ""
        var operaciones = mutableListOf<String>()

        // 1. Obtener datos via SOAP (getAlumnoAcademico)
        try {
            val soapBody = bodyperfil
            Log.e("SNRepository", ">>> Pidiendo Perfil SOAP (getAlumnoAcademico) <<<")
            
            val response = try {
                snApiService.perfil(soapBody.toRequestBody("text/xml; charset=utf-8".toMediaType()))
            } catch (e: retrofit2.HttpException) {
                Log.e("SNRepository", "❌ PERFIL SOAP ERROR ${e.code()}")
                null
            }

            if (response != null) {
                val xmlString = response.string()
                // Intento 1: Regex para extraer el JSON o XML del resultado
                var resultText = Regex("<getAlumnoAcademicoResult>(.*?)</getAlumnoAcademicoResult>").find(xmlString)?.groupValues?.get(1)
                
                if (resultText == null) {
                    try {
                        val envelope = Persister().read(com.example.marsphotos.model.EnvelopeSobreAlumno::class.java, xmlString)
                        resultText = envelope.body?.getAlumnoAcademicoResponse?.getAlumnoAcademicoResult
                    } catch (e: Exception) {}
                }
                
                if (!resultText.isNullOrBlank()) {
                    var processed: String = resultText
                    // Desencapsular si es XML
                    while (processed.contains("&lt;")) {
                        processed = processed.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
                    }

                    // CASO JSON (Detectado en logs)
                    if (processed.trim().startsWith("{")) {
                        Log.e("SNRepository", ">>> Parseando JSON de Perfil <<<")
                        try {
                            val json = Json.parseToJsonElement(processed.trim()).jsonObject
                            nombre = json["nombre"]?.jsonPrimitive?.content ?: ""
                            carrera = json["carrera"]?.jsonPrimitive?.content ?: ""
                            especialidad = json["especialidad"]?.jsonPrimitive?.content ?: ""
                            semestre = json["semActual"]?.jsonPrimitive?.content ?: ""
                            cdtAc = json["cdtosAcumulados"]?.jsonPrimitive?.content ?: "0"
                            cdtAct = json["cdtosActuales"]?.jsonPrimitive?.content ?: "0"
                            inscritoStr = if (json["inscrito"]?.jsonPrimitive?.content == "true") "SI" else "NO"
                            fReins = json["fechaReins"]?.jsonPrimitive?.content ?: ""
                            estatusAlu = json["estatus"]?.jsonPrimitive?.content ?: ""
                            val foto = json["urlFoto"]?.jsonPrimitive?.content ?: ""
                            if (foto.isNotEmpty()) fotoUrl = "https://sicenet.itsur.edu.mx/fotos/$foto"
                            
                            Log.e("SNRepository", "✅ Datos JSON extraídos: $nombre")
                        } catch (e: Exception) {
                            Log.e("SNRepository", "❌ Error en JSON: ${e.message}")
                        }
                    } else if (processed.contains("<Alumno>")) {
                        // CASO XML
                        try {
                            val xmlToParse = if (processed.contains("<DataSet")) processed.substring(processed.indexOf("<DataSet")) else processed
                            val alu = Persister().read(com.example.marsphotos.model.PerfilDataSet::class.java, xmlToParse).alumno
                            if (alu != null) {
                                nombre = "${alu.nombre ?: ""} ${alu.apellidos ?: ""}".trim()
                                carrera = alu.carrera ?: ""
                                semestre = alu.semestre ?: ""
                                promedio = alu.promedio ?: ""
                                estatusAlu = alu.estado ?: ""
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SNRepository", "❌ Error en SOAP: ${e.message}")
        }

        // 2. Scraping HTML para complementar (Foto si falta, adeudos, operaciones)
        try {
            Log.e("SNRepository", ">>> Scraping HTML complementario <<<")
            val html = snApiService.plataforma().string()
            val doc = Jsoup.parse(html)
            
            // Solo actualizamos si no lo tenemos del SOAP/JSON
            if (nombre.isEmpty()) nombre = doc.selectFirst("#lblNombre, .nombre")?.text()?.trim() ?: ""
            if (fotoUrl.isEmpty()) fotoUrl = doc.selectFirst("#imgAlumno, [src*=foto]")?.absUrl("src") ?: ""
            if (especialidad.isEmpty()) especialidad = doc.selectFirst("td:contains(Especialidad) + td")?.text()?.trim() ?: ""
            if (semestre.isEmpty()) semestre = doc.selectFirst("td:contains(Sem. Actual) + td")?.text()?.trim() ?: ""
            
            sinAdeudos = doc.select("td, span").find { it.text().contains("ADEUDOS") }?.text()?.trim() ?: ""
            
            doc.select("a").forEach { a ->
                val txt = a.text().uppercase()
                if (txt.contains("CALIFICACIONES") || txt.contains("KARDEX") || 
                    txt.contains("MONITOREO") || txt.contains("CARGA")) {
                    operaciones.add(txt)
                }
            }
            Log.e("SNRepository", "✅ Scraping completado")
        } catch (e: Exception) {
            Log.e("SNRepository", "⚠️ Error en Scraping: ${e.message}")
        }

        // 3. Limpieza de caracteres (Corregir encoding si es necesario)
        fun clean(s: String): String = s.replace("?", "Í").replace("í?", "í").replace("A?", "Á").replace("O?", "Ó")

        return ProfileStudent(
            matricula = matricula,
            nombre = clean(nombre),
            carrera = clean(carrera),
            especialidad = clean(especialidad),
            semestre = semestre,
            promedio = promedio,
            cdtsReunidos = cdtAc,
            cdtsActuales = cdtAct,
            inscrito = inscritoStr,
            reinscripcionFecha = fReins,
            estatusAlumno = estatusAlu,
            fotoUrl = fotoUrl,
            sinAdeudos = sinAdeudos,
            operaciones = operaciones.distinct()
        )
    }

    /**
     * Obtiene la matrícula del usuario autenticado
     */
    override suspend fun getMatricula(): String {
        return userMatricula
    }
}

// Importar MediaType para usar toMediaType()
