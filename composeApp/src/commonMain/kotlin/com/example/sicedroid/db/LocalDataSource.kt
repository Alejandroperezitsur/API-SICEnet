package com.example.sicedroid.db

import com.example.sicedroid.model.*

interface LocalDataSource {
    fun getSession(): SessionEntity?
    fun sessionExists(): Boolean
    fun saveSession(matricula: String, password: String)
    fun clearSession()

    fun getProfile(matricula: String): ProfileStudent?
    fun saveProfile(matricula: String, profile: ProfileStudent)
    fun deleteProfile(matricula: String)

    fun getKardex(matricula: String): List<MateriaKardex>
    fun saveKardex(matricula: String, kardex: List<MateriaKardex>)

    fun getCarga(matricula: String): List<MateriaCarga>
    fun saveCarga(matricula: String, carga: List<MateriaCarga>)

    fun getCalifUnidad(matricula: String): List<MateriaParcial>
    fun saveCalifUnidad(matricula: String, parciales: List<MateriaParcial>)

    fun getCalifFinal(matricula: String): List<MateriaFinal>
    fun saveCalifFinal(matricula: String, finales: List<MateriaFinal>)

    fun clearAll(matricula: String)
}
