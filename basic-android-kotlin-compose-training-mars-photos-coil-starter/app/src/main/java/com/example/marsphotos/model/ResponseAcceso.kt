package com.example.marsphotos.model

import kotlinx.serialization.Serializable
import org.simpleframework.xml.Element
import org.simpleframework.xml.Namespace
import org.simpleframework.xml.NamespaceList
import org.simpleframework.xml.Root

// ============ ACCESO LOGIN RESPONSE ============

@Serializable
@Root(name = "soap:Envelope", strict = false)
@NamespaceList(
    Namespace(reference = "http://www.w3.org/2001/XMLSchema-instance", prefix = "xsi"),
    Namespace(reference = "http://www.w3.org/2001/XMLSchema", prefix = "xsd"),
    Namespace(prefix = "soap", reference = "http://schemas.xmlsoap.org/soap/envelope/")
)
data class EnvelopeSobreAcceso(
    @field:Element(name = "soap:Body", required = false)
    @param:Element(name = "soap:Body", required = false)
    val body: BodyAccesoResponse? = null
)

@Serializable
@Root(name = "soap:Body", strict = false)
@NamespaceList(
    Namespace(prefix = "soap", reference = "http://schemas.xmlsoap.org/soap/envelope/"),
    Namespace(reference = "http://tempuri.org/")
)
data class BodyAccesoResponse(
    @Element(name = "accesoLoginResponse", required = false)
    @Namespace(reference = "http://tempuri.org/")
    val accesoLoginResponse: AccesoLoginResponse? = null
)

@Serializable
@Root(name = "accesoLoginResponse", strict = false)
@NamespaceList(
    Namespace(reference = "http://tempuri.org/")
)
data class AccesoLoginResponse(
    @Element(name = "accesoLoginResult", required = false)
    @Namespace(reference = "http://tempuri.org/")
    val accesoLoginResult: String? = null
)

// ============ CONSULTA PERFIL RESPONSE ============

@Serializable
@Root(name = "soap:Envelope", strict = false)
data class EnvelopeSobrePerfil(
    @field:Element(name = "soap:Body", required = false)
    @param:Element(name = "soap:Body", required = false)
    val body: BodyPerfilResponse? = null
)

@Serializable
@Root(name = "soap:Body", strict = false)
data class BodyPerfilResponse(
    @Element(name = "consultaPerfilResponse", required = false)
    val consultaPerfilResponse: ConsultaPerfilResponse? = null
)

@Serializable
@Root(name = "consultaPerfilResponse", strict = false)
data class ConsultaPerfilResponse(
    @Element(name = "consultaPerfilResult", required = false)
    val consultaPerfilResult: String? = null
)
