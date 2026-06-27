package com.example.sicedroid.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class NetworkException(message: String) : Exception(message)

class SiceApiService {
    val client = HttpClient {
        install(HttpCookies)
        defaultRequest {
            header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
            header(HttpHeaders.AcceptLanguage, "es-MX,es;q=0.9,en;q=0.8")
            header(HttpHeaders.CacheControl, "no-cache")
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 15000
        }
    }

    private suspend fun soapRequest(soapBody: String, soapAction: String): String {
        val primaryUrl = getBaseUrl()
        val urlsToTry = if (primaryUrl.contains("sicenet.itsur.edu.mx") && !primaryUrl.contains("?")) {
            // Direct connection for Android/Desktop (no proxy needed)
            listOf(primaryUrl)
        } else {
            // WASM/JS browser environments require a proxy.
            // We try the primary (user's Cloudflare Worker) first, then fallback proxies.
            listOf(
                primaryUrl,
                "https://proxy.corsfix.com/?https://sicenet.itsur.edu.mx",
                "https://api.allorigins.win/raw?url=https://sicenet.itsur.edu.mx"
            )
        }

        var lastException: Exception? = null
        for (url in urlsToTry) {
            try {
                val response: HttpResponse = client.post("$url/ws/wsalumnos.asmx") {
                    header(HttpHeaders.ContentType, "text/xml; charset=utf-8")
                    header("SOAPAction", "\"$soapAction\"")
                    setBody(soapBody)
                }
                if (response.status.value != 200) {
                    lastException = Exception("HTTP ${response.status.value}: Error del servidor proxy")
                    continue
                }
                val body = response.bodyAsText()
                if (body.isBlank() || body.contains("<html", ignoreCase = true)) {
                    lastException = Exception("Respuesta no válida del servidor")
                    continue
                }
                return body
            } catch (e: Exception) {
                lastException = e
            }
        }
        throw NetworkException("No se pudo conectar al servidor. Verifica tu conexión a internet.")
    }

    suspend fun acceso(soapBody: String): String =
        soapRequest(soapBody, "http://tempuri.org/accesoLogin")

    suspend fun perfil(soapBody: String): String =
        soapRequest(soapBody, "http://tempuri.org/getAlumnoAcademicoWithLineamiento")

    suspend fun kardex(soapBody: String): String =
        soapRequest(soapBody, "http://tempuri.org/getAllKardexConPromedioByAlumno")

    suspend fun carga(soapBody: String): String =
        soapRequest(soapBody, "http://tempuri.org/getCargaAcademicaByAlumno")

    suspend fun parciales(soapBody: String): String =
        soapRequest(soapBody, "http://tempuri.org/getCalifUnidadesByAlumno")

    suspend fun finales(soapBody: String): String =
        soapRequest(soapBody, "http://tempuri.org/getAllCalifFinalByAlumnos")
}
