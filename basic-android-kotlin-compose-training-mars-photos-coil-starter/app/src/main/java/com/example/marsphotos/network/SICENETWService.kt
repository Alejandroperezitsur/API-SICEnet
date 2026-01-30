package com.example.marsphotos.network

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST


val bodyacceso =
    """
        <?xml version="1.0" encoding="utf-8"?>
        <soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
          <soap:Body>
            <accesoLogin xmlns="http://tempuri.org/">
              <strMatricula>%S</strMatricula>
              <strContrasenia>%S</strContrasenia>   
              <tipoUsuario>ALUMNO</tipoUsuario>
            </accesoLogin>
          </soap:Body>
        </soap:Envelope>
    """.trimIndent()

/*
val bodyacceso = "<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
        "    <Body>\n" +
        "        <accesoLogin xmlns=\"http://tempuri.org/\">\n" +
        "            <strMatricula>%S</strMatricula>\n" +
        "            <strContrasenia>%S</strContrasenia>\n" +
        "            <tipoUsuario>DOCENTE</tipoUsuario>\n" +
        "        </accesoLogin>\n" +
        "    </Body>\n" +
        "</Envelope>"
*/

interface SICENETWService {

    // sicenet.surguanajuato.tecnm.mx
    @Headers(
        "Content-Type: text/xml;charset=utf-8",
        "SOAPAction: http://tempuri.org/accesoLogin",
        //"Cookie: .ASPXANONYMOUS=MaWJCZ-X2gEkAAAAODU2ZjkyM2EtNWE3ZC00NTdlLWFhYTAtYjk5ZTE5MDlkODIzeI1pCwvskL6aqtre4eT8Atfq2Po1;.ASPXANONYMOUS=MaWJCZ-X2gEkAAAAODU2ZjkyM2EtNWE3ZC00NTdlLWFhYTAtYjk5ZTE5MDlkODIzeI1pCwvskL6aqtre4eT8Atfq2Po1;"
    )
    @POST("/ws/wsalumnos.asmx")
    suspend fun acceso(@Body soap: RequestBody): ResponseBody

    @GET("/")
    suspend fun con (): ResponseBody
}