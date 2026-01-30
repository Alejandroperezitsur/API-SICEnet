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
import com.example.marsphotos.model.ProfileStudent
import com.example.marsphotos.model.Usuario
import com.example.marsphotos.network.SICENETWService
import com.example.marsphotos.network.bodyacceso
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Repository that fetch mars photos list from marsApi.
 */
interface SNRepository {
    /** Fetches list of MarsPhoto from marsApi */
    suspend fun acceso(m: String, p: String): String
    suspend fun accesoObjeto(m: String, p: String): Usuario
    suspend fun profile(m: String, p: String): ProfileStudent


}


class DBLocalSNRepository(val apiDB : Any):SNRepository {
    override suspend fun acceso(m: String, p: String): String {
        //TODO("Not yet implemented")
        //Reviso en base de datos
        //Preparar Room

        //apiDB.acceso( Usuario(matricula = m) )

        return ""

    }

    override suspend fun accesoObjeto(m: String, p: String): Usuario {

        //Tengo  que ir a Room
        return Usuario(matricula = "")
    }

    override suspend fun profile(m: String, p: String): ProfileStudent {
        //TODO("Not yet implemented")
        return ProfileStudent("S")
    }
}

/**
 * Network Implementation of Repository that fetch mars photos list from marsApi.
 */
class NetworSNRepository(
    private val snApiService: SICENETWService
) : SNRepository {
    /** Fetches list of MarsPhoto from marsApi*/
    override suspend fun acceso(m: String, p: String): String {

        //callHTTPS()
        val res = snApiService.acceso(bodyacceso.format(m,p).toRequestBody() )

        Log.d("RXML", res.string() )
       /* Log.d("RXML", res.body?.accesoLoginResponse?.accesoLoginResult.toString() )

        return res.body?.accesoLoginResponse?.accesoLoginResult.toString()*/
        /*Log.d("RXML", res.message() )
        return res.message()*/
        return ""
    }

    override suspend fun accesoObjeto(m: String, p: String): Usuario {
        //TODO("Not yet implemented")
        return Usuario(matricula = "")


    }

    override suspend fun profile(m: String, p: String): ProfileStudent {
        TODO("Not yet implemented")

    }

    suspend fun callHTTPS(){
        // Datos para la petición
        val matricula = "s20120999"
        val contrasenia = "MIPASS"
        val tipoUsuario = "ALUMNO" // o "DOCENTE", según corresponda

        // URL del servicio web SOAP
        val urlString = "https://sicenet.surguanajuato.tecnm.mx/ws/wsalumnos.asmx"

        // Cuerpo del mensaje SOAP
        val soapEnvelope = """
        <soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
          <soap:Body>
            <accesoLogin xmlns="http://tempuri.org/">
              <strMatricula>$matricula</strMatricula>
              <strContrasenia>$contrasenia</strContrasenia>
              <tipoUsuario>$tipoUsuario</tipoUsuario>
            </accesoLogin>
          </soap:Body>
        </soap:Envelope>
    """.trimIndent()

        try {
            // Establecer la conexión HTTPS
            val url = URL(urlString)
            val connection = url.openConnection() as HttpsURLConnection

            // Configurar la conexión
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Host", "sicenet.surguanajuato.tecnm.mx")
            connection.setRequestProperty("Content-Type", "text/xml; charset=\"UTF-8\"")
            //connection.setRequestProperty("Sec-Fetch-Mode", "cors")
            connection.setRequestProperty("Cookie", ".ASPXANONYMOUS=MaWJCZ-X2gEkAAAAODU2ZjkyM2EtNWE3ZC00NTdlLWFhYTAtYjk5ZTE5MDlkODIzeI1pCwvskL6aqtre4eT8Atfq2Po1;")
            connection.setRequestProperty("Content-Length", soapEnvelope.length.toString())
            connection.setRequestProperty("SOAPAction", "\"http://tempuri.org/accesoLogin\"")

            // Enviar el cuerpo del mensaje SOAP
            val outputStream: OutputStream = connection.outputStream
            outputStream.write(soapEnvelope.toByteArray(Charsets.UTF_8))
            outputStream.close()

            // Leer la respuesta del servicio
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val cookies = connection.getHeaderField("Set-Cookie")
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                var line: String?
                val response = StringBuilder()

                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                // Aquí puedes procesar la respuesta del servicio
                println("Respuesta del servicio: $response")
                Log.d("SXML","Respuesta del servicio: $response")
            } else {
                // Manejar errores de conexión
                println("Error en la conexión: $responseCode")
            }

            // Cerrar la conexión
            connection.disconnect()
        } catch (e: IOException) {
            // Manejar excepciones de conexión
            e.printStackTrace()
        }
    }

}
