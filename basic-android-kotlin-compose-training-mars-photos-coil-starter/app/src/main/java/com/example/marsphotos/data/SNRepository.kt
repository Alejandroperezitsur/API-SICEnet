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
            
            // Usamos text/xml simple y dejamos que OkHttp maneje el charset por defecto (utf-8)
            val response = snApiService.acceso(soapBody.toRequestBody("text/xml".toMediaType()))
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
        } catch (e: HttpException) {
             Log.e("SNRepository", "Error HTTP: ${e.code()} - ${e.message()}")
             try {
                 Log.e("SNRepository", "Error Body: ${e.response()?.errorBody()?.string()}")
             } catch (_: Exception) {}
             false
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
        Log.d("SNRepository", "===== OBTENIENDO PERFIL =====")
        return try {
            val soapBody = bodyperfil.format(matricula)
            val response = snApiService.perfil(soapBody.toRequestBody("text/xml".toMediaType()))
            
            val xmlString = response.string()
            Log.d("SNRepository", "Respuesta Perfil XML: $xmlString")
            
            // Parsear la respuesta SOAP
            val persister = Persister()
            var alumno: AlumnoInfo? = null
            
            try {
                // SimpleXML puede fallar si el XML tiene namespaces complejos no mapeados
                // Buscamos directamente el tag <Alumno> para ver si hay datos
                if (xmlString.contains("<Alumno>") || xmlString.contains("<Alumno ")) {
                    // Intentamos parsear todo el dataset
                    // A veces el XML viene "sucio" o con prefijos, intentamos limpiar o parsear selectivamente
                    val dataSet = persister.read(PerfilDataSet::class.java, xmlString)
                    alumno = dataSet.alumno
                }
            } catch (e: Exception) {
                Log.w("SNRepository", "Error parseando XML con SimpleXML", e)
            }
            
            // Datos complementarios (HTML)
            var fotoUrl = ""
            var especialidad = ""
            var cdtsReunidos = ""
            var cdtsActuales = ""
            var inscrito = ""
            var reinscripcion = ""
            var sinAdeudos = ""
            val operaciones = mutableListOf<String>()
            
            try {
                Log.d("SNRepository", "Solicitando plataforma HTML para datos extra...")
                val resp = snApiService.plataforma()
                val html = resp.string()
                val base = URL("https://sicenet.itsur.edu.mx")
                val doc = Jsoup.parse(html, base.toString())

                // Foto
                val img = doc.selectFirst("img#imgAlumno, img[src*=Foto], img[src*=foto], .foto img, img.alumno")
                fotoUrl = img?.absUrl("src") ?: img?.attr("src") ?: ""
                Log.d("SNRepository", "Foto URL encontrada: $fotoUrl")

                // Campos por label
                fun nextText(label: String): String {
                    // Busca td con el label y toma el siguiente td
                    // O busca span/div que contenga el texto
                    val td = doc.selectFirst("td:contains($label)")
                    return td?.nextElementSibling()?.text()?.trim() ?: ""
                }
                
                // Helper para buscar texto en celdas cercanas o por ID si fuera necesario
                // La estructura de SICENET suele ser tablas
                
                especialidad = nextText("Especialidad").ifEmpty { nextText("Carrera") } // A veces comparten celda
                if (especialidad.isEmpty()) especialidad = doc.select("span:contains(TECNOLOGÍAS)").text()
                
                cdtsReunidos = nextText("Cdts. Reunidos").ifEmpty { nextText("Créditos Acumulados") }
                cdtsActuales = nextText("Cdts. Actuales").ifEmpty { nextText("Créditos Inscritos") }
                
                inscrito = nextText("Inscrito")
                if (inscrito.isEmpty()) inscrito = if (html.contains("Inscrito: SI")) "SI" else "NO"
                
                reinscripcion = nextText("Fecha").ifEmpty { nextText("Reinscripción") }
                    
                    sinAdeudos = doc.selectFirst("td:contains(SIN ADEUDOS)")?.text()?.trim() 
                                 ?: doc.selectFirst("span:contains(SIN ADEUDOS)")?.text()?.trim()
                                 ?: ""
        
                    // Operaciones
                val ops = doc.select("a[href]")
                for (op in ops) {
                    val t = op.text()?.trim() ?: ""
                    // Filtramos enlaces comunes del menú
                    if (t.isNotEmpty() && (t.contains("CALIFICACIONES") || t.contains("KARDEX") || 
                        t.contains("REINSCRIPCION") || t.contains("CARGA") || t.contains("MONITOREO") || t.contains("Cerrar"))) {
                        operaciones.add(t)
                    }
                }
            } catch (e: Exception) {
                Log.e("SNRepository", "Error parseando HTML", e)
            }

            // Construir objeto final
            // Si el XML falló, usamos datos vacíos o lo que hayamos podido rescatar del HTML (aunque el HTML es la fuente secundaria)
            // Si alumno es null, creamos uno vacío con la matrícula
            val finalAlumno = alumno ?: AlumnoInfo(matricula = matricula, nombre = "Alumno", apellidos = "Desconocido")
            
            ProfileStudent(
                matricula = finalAlumno.matricula ?: matricula,
                nombre = finalAlumno.nombre ?: "",
                apellidos = finalAlumno.apellidos ?: "",
                carrera = finalAlumno.carrera ?: "",
                semestre = finalAlumno.semestre ?: "",
                promedio = finalAlumno.promedio ?: "",
                estado = finalAlumno.estado ?: "",
                statusMatricula = finalAlumno.statusMatricula ?: "",
                fotoUrl = fotoUrl,
                especialidad = especialidad,
                cdtsReunidos = cdtsReunidos,
                cdtsActuales = cdtsActuales,
                inscrito = inscrito,
                reinscripcionFecha = reinscripcion,
                sinAdeudos = sinAdeudos,
                operaciones = operaciones
            )
            
        } catch (e: Exception) {
            Log.e("SNRepository", "Error general obteniendo perfil", e)
            throw e // Re-lanzar para que el ViewModel lo maneje
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
