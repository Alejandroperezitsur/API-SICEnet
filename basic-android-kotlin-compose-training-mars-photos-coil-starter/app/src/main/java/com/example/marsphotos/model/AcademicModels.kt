package com.example.marsphotos.model

import kotlinx.serialization.Serializable

@Serializable
data class MateriaKardex(
    val clave: String = "",
    val nombre: String = "",
    val calificacion: String = "",
    val acreditacion: String = "",
    val periodo: String = ""
)

@Serializable
data class MateriaCarga(
    val nombre: String = "",
    val docente: String = "",
    val grupo: String = "",
    val creditos: String = "",
    val lunes: String = "",
    val martes: String = "",
    val miercoles: String = "",
    val jueves: String = "",
    val viernes: String = "",
    val sabado: String = ""
)

@Serializable
data class MateriaParcial(
    val materia: String = "",
    val parciales: List<String> = emptyList()
)
