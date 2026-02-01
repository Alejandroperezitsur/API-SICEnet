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
            val soapBody = bodyacceso.format(safeMatricula, safeContrasenia)
            
            Log.d("SNRepository", "Enviando SOAP Body (truncado): ${soapBody.take(100)}...")
            
            // Usamos text/xml; charset=utf-8 explícitamente
            val response = snApiService.acceso(soapBody.toRequestBody("text/xml;charset=utf-8".toMediaType()))
            val xmlString = response.string()
            
            Log.d("SNRepository", "Respuesta XML recibida: $xmlString")
            
            // La respuesta suele venir como un JSON string dentro del XML o directamente XML
            // Intentamos extraer el JSON si existe
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
        Log.d("SNRepository", "===== OBTENIENDO PERFIL =====")
        return try {
            val soapBody = bodyperfil.format(matricula)
            val response = snApiService.perfil(soapBody.toRequestBody("text/xml;charset=utf-8".toMediaType()))
            
            val xmlString = response.string()
            Log.d("SNRepository", "Respuesta Perfil XML: $xmlString")
            
            val persister = Persister()
            var alumno: AlumnoInfo? = null
            
            try {
                // Buscamos el contenido dentro de <consultaPerfilResult>
                val resultTagStart = "Result>"
                val startIdx = xmlString.indexOf(resultTagStart)
                val endIdx = xmlString.lastIndexOf("</")
                
                if (startIdx != -1 && endIdx != -1) {
                    var innerXml = xmlString.substring(startIdx + resultTagStart.length, endIdx)
                    // Decodificar entidades XML si es necesario
                    innerXml = innerXml.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
                    
                    if (innerXml.contains("<Alumno>")) {
                        // El XML interno suele empezar con <DataSet> o similar
                        val xmlToParse = if (innerXml.contains("<DataSet")) {
                            innerXml.substring(innerXml.indexOf("<DataSet"))
                        } else {
                            innerXml
                        }
                        alumno = persister.read(PerfilDataSet::class.java, xmlToParse).alumno
                    }
                }
            } catch (e: Exception) {
                Log.w("SNRepository", "Error parseando XML interno", e)
            }
            
            var fotoUrl = ""
            var especialidad = ""
            var cdtsReunidos = ""
            var cdtsActuales = ""
            var inscrito = ""
            var reinscripcion = ""
            var sinAdeudos = ""
            var semActual = ""
            var estatusAcademico = ""
            var estatusAlumno = ""
            val operaciones = mutableListOf<String>()
            
            try {
                Log.d("SNRepository", "Cargando plataforma HTML...")
                val resp = snApiService.plataforma()
                val html = resp.string()
                val doc = Jsoup.parse(html, "https://sicenet.itsur.edu.mx")

                // Selectores específicos basados en la estructura recurrente de SICEnet
                fotoUrl = doc.selectFirst("#imgAlumno, [src*=foto], [src*=Foto]")?.absUrl("src") ?: ""
                
                especialidad = doc.selectFirst("td:contains(Especialidad) + td, #lblEspecialidad")?.text()?.trim() ?: ""
                cdtsReunidos = doc.selectFirst("td:contains(Cdts. Reunidos) + td, #lblCdtsReunidos")?.text()?.trim() ?: "0"
                cdtsActuales = doc.selectFirst("td:contains(Cdts. Actuales) + td, #lblCdtsActuales")?.text()?.trim() ?: "0"
                semActual = doc.selectFirst("td:contains(Sem. Actual) + td, #lblSemActual")?.text()?.trim() ?: "0"
                inscrito = doc.selectFirst("td:contains(Inscrito) + td, #lblInscrito")?.text()?.trim() ?: "NO"
                reinscripcion = doc.selectFirst("td:contains(Fecha) + td, #lblFechaReinscripcion")?.text()?.trim() ?: "PENDIENTE"
                
                estatusAcademico = doc.selectFirst("td:contains(Estatus Académico) + td, #lblEstatusAcademico")?.text()?.trim() ?: ""
                estatusAlumno = doc.selectFirst("td:contains(Estatus) + td, #lblEstatus")?.text()?.trim() ?: ""
                
                sinAdeudos = doc.select("td, span").find { it.text().contains("ADEUDOS") }?.text()?.trim() ?: ""

                // Operaciones Académicas (Enlaces del menú)
                doc.select("a").forEach { a ->
                    val txt = a.text().uppercase()
                    if (txt.contains("CALIFICACIONES") || txt.contains("KARDEX") || 
                        txt.contains("MONITOREO") || txt.contains("REINSCRIPCION") || 
                        txt.contains("CARGA") || txt.contains("CERRAR SESION")) {
                        operaciones.add(txt)
                    }
                }
            } catch (e: Exception) {
                Log.e("SNRepository", "Error Jsoup", e)
            }

            val finalAlumno = alumno ?: AlumnoInfo(matricula = matricula, nombre = "Alumno", apellidos = "Desconocido")
            
            ProfileStudent(
                matricula = finalAlumno.matricula ?: matricula,
                nombre = finalAlumno.nombre ?: "",
                apellidos = finalAlumno.apellidos ?: "",
                carrera = finalAlumno.carrera ?: "",
                semestre = if (semActual != "0") semActual else (finalAlumno.semestre ?: ""),
                promedio = finalAlumno.promedio ?: "",
                estado = finalAlumno.estado ?: "",
                statusMatricula = finalAlumno.statusMatricula ?: "",
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
            Log.e("SNRepository", "Error perfil", e)
            throw e
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
