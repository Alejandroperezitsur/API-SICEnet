package com.example.marsphotos.model

import kotlinx.serialization.Serializable
import org.simpleframework.xml.Element
import org.simpleframework.xml.Root

@Serializable
data class ProfileStudent(
    val matricula: String = "",
    val nombre: String = "",
    val apellidos: String = "",
    val carrera: String = "",
    val semestre: String = "",
    val promedio: String = "",
    val estado: String = "",
    val statusMatricula: String = ""
)

// Clase para parsear la respuesta XML del perfil
@Serializable
@Root(name = "DataSet", strict = false)
data class ProfileDataSet(
    @field:Element(name = "xs:schema", required = false)
    @param:Element(name = "xs:schema", required = false)
    val schema: String? = null,
    
    @field:Element(name = "diffgr:diffgram", required = false)
    @param:Element(name = "diffgr:diffgram", required = false)
    val diffgram: String? = null
)
