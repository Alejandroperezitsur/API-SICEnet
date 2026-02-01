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
import com.example.marsphotos.model.MateriaKardex
import com.example.marsphotos.model.MateriaCarga
import com.example.marsphotos.model.MateriaParcial
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
        var estadoScraped = ""
        var statusMatriculaScraped = ""
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
        var kardexList = mutableListOf<com.example.marsphotos.model.MateriaKardex>()
        var cargaList = mutableListOf<com.example.marsphotos.model.MateriaCarga>()
        var parcialesList = mutableListOf<com.example.marsphotos.model.MateriaParcial>()

        try {
            Log.e("SNRepository", ">>> Scraping HTML complementario <<<")
            val html = snApiService.plataforma().string()
            val doc = Jsoup.parse(html)
            
            if (nombre.isEmpty()) nombre = doc.selectFirst("#lblNombre, .nombre, td:contains(Alumno) + td")?.text()?.trim() ?: ""
            if (fotoUrl.isEmpty()) fotoUrl = doc.selectFirst("#imgAlumno, [src*=foto], [src*=Foto], .foto")?.absUrl("src") ?: ""
            if (especialidad.isEmpty()) especialidad = doc.selectFirst("td:contains(Especialidad) + td, #lblEspecialidad")?.text()?.trim() ?: ""
            if (semestre.isEmpty()) semestre = doc.selectFirst("td:contains(Sem. Actual) + td, #lblSemActual")?.text()?.trim() ?: ""
            
            estadoScraped = doc.selectFirst("td:contains(Estado) + td, td:contains(Situación) + td, #lblEstado")?.text()?.trim() ?: ""
            statusMatriculaScraped = doc.selectFirst("td:contains(Status Matrícula) + td, td:contains(Estatus Matrícula) + td, #lblStatusMatricula")?.text()?.trim() ?: ""
            
            val estatusAcadScraped = doc.selectFirst("td:contains(Estatus Académico) + td, #lblEstatusAcademico")?.text()?.trim() ?: ""
            val estatusAluScraped = doc.selectFirst("td:contains(Estatus Alumno) + td, td:contains(Estatus:) + td, #lblEstatus")?.text()?.trim() ?: ""

            if (estatusAcad.isEmpty()) estatusAcad = estatusAcadScraped
            if (estatusAlu.isEmpty()) estatusAlu = estatusAluScraped
            
            sinAdeudos = doc.select("td, span").find { it.text().contains("ADEUDOS") }?.text()?.trim() ?: ""
            
            doc.select("a").forEach { a ->
                val txt = a.text().uppercase()
                if (txt.contains("CALIFICACIONES") || txt.contains("KARDEX") || 
                    txt.contains("MONITOREO") || txt.contains("REINSCRIPCION") || 
                    txt.contains("CARGA") || txt.contains("CERRAR SESION")) {
                    operaciones.add(txt)
                }
            }
            Log.e("SNRepository", "✅ Scraping Perfil completado")

            // --- KARDEX ---
            try {
                Log.e("SNRepository", ">>> Scraping KARDEX <<<")
                val kHtml = snApiService.kardex().string()
                val kDoc = Jsoup.parse(kHtml)
                promedio = kDoc.selectFirst("td:contains(Promedio general) + td, #lblPromedioGeneral")?.text()?.trim() ?: ""
                
                kDoc.select("tr").forEach { tr ->
                    val tds = tr.select("td")
                    if (tds.size >= 7 && tds[0].text().firstOrNull()?.isDigit() == true) {
                        kardexList.add(com.example.marsphotos.model.MateriaKardex(
                            clave = tds[1].text().trim(),
                            nombre = tds[2].text().trim(),
                            calificacion = tds[5].text().trim(),
                            acreditacion = tds[6].text().trim(),
                            periodo = tds[7].text().trim() + " " + tds[8].text().trim()
                        ))
                    }
                }
            } catch (e: Exception) { Log.e("SNRepository", "Error Kardex: ${e.message}") }

            // --- CARGA ---
            try {
                Log.e("SNRepository", ">>> Scraping CARGA <<<")
                val cHtml = snApiService.carga().string()
                val cDoc = Jsoup.parse(cHtml)
                cDoc.select("tr").forEach { tr ->
                    val tds = tr.select("td")
                    if (tds.size >= 12 && tds[4].text().trim() == "O") { // Fila de materia
                        cargaList.add(com.example.marsphotos.model.MateriaCarga(
                            nombre = tds[1].text().split("\n").firstOrNull()?.trim() ?: "",
                            docente = tds[1].text().split("\n").getOrNull(1)?.trim() ?: "",
                            grupo = tds[3].text().trim(),
                            creditos = tds[5].text().trim(),
                            lunes = tds[6].text().trim(),
                            martes = tds[7].text().trim(),
                            miercoles = tds[8].text().trim(),
                            jueves = tds[9].text().trim(),
                            viernes = tds[10].text().trim()
                        ))
                    }
                }
            } catch (e: Exception) { Log.e("SNRepository", "Error Carga: ${e.message}") }

            // --- CALIFICACIONES ---
            try {
                Log.e("SNRepository", ">>> Scraping CALIFICACIONES <<<")
                val pHtml = snApiService.calificaciones().string()
                val pDoc = Jsoup.parse(pHtml)
                pDoc.select("tr").forEach { tr ->
                    val tds = tr.select("td")
                    if (tds.size >= 8 && tds[0].text().length >= 4) { // Fila de calificaciones
                        val pars = mutableListOf<String>()
                        for (i in 2..7) pars.add(tds[i].text().trim())
                        parcialesList.add(com.example.marsphotos.model.MateriaParcial(
                            materia = tds[1].text().trim(),
                            parciales = pars
                        ))
                    }
                }
            } catch (e: Exception) { Log.e("SNRepository", "Error Parciales: ${e.message}") }

        } catch (e: Exception) {
            Log.e("SNRepository", "⚠️ Error en Scraping: ${e.message}")
        }

        // 3. Mapeo y Limpieza
        fun mapStatus(s: String): String = when(s.uppercase().trim()) {
            "VI" -> "VIGENTE"
            "BA" -> "BAJA"
            "EG" -> "EGRESADO"
            "TI" -> "TITULADO"
            "IN" -> "INSCRITO"
            else -> s
        }

        fun clean(s: String): String = s
            .replace("?", "Í")
            .replace("í?", "í")
            .replace("A?", "Á")
            .replace("O?", "Ó")
            .replace("&nbsp;", " ")
            .trim()

        return ProfileStudent(
            matricula = matricula,
            nombre = clean(nombre),
            carrera = clean(carrera),
            especialidad = clean(especialidad),
            semestre = semestre,
            promedio = promedio,
            estado = if (estadoScraped.isEmpty()) "INSCRITO" else clean(estadoScraped),
            statusMatricula = if (statusMatriculaScraped.isEmpty()) clean(sinAdeudos) else clean(statusMatriculaScraped),
            cdtsReunidos = cdtAc,
            cdtsActuales = cdtAct,
            inscrito = inscritoStr,
            reinscripcionFecha = fReins,
            estatusAlumno = mapStatus(estatusAlu),
            estatusAcademico = clean(estatusAcad),
            fotoUrl = fotoUrl,
            sinAdeudos = clean(sinAdeudos),
            operaciones = operaciones.distinct(),
            kardex = kardexList,
            cargaAcademica = cargaList,
            calificacionesParciales = parcialesList
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
