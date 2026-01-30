package com.example.marsphotos.model

import kotlinx.serialization.Serializable
import org.simpleframework.xml.Element
import org.simpleframework.xml.Namespace
import org.simpleframework.xml.NamespaceList
import org.simpleframework.xml.Root

@Serializable
@Root(name = "soap:Envelope")
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
@Root(name = "soap:Body")
@NamespaceList(
    Namespace(prefix = "soap", reference = "http://schemas.xmlsoap.org/soap/envelope/"),
            Namespace(reference = "http://tempuri.org/")
)
data class BodyAccesoResponse(

    @Element(name = "accesoLoginResponse")
    @Namespace(reference = "http://tempuri.org/")
    val accesoLoginResponse: AccesoLoginResponse? = null
)

@Serializable
@Root(name = "accesoLoginResponse")
@NamespaceList(
    Namespace(reference = "http://tempuri.org/")
)
data class AccesoLoginResponse (

    @Element(name = "accesoLoginResult")
    @Namespace(reference = "http://tempuri.org/")
    val accesoLoginResult: String? = null
)
