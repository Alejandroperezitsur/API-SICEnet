package com.example.sicedroid.db

import com.example.sicedroid.model.*

class WebLocalDataSource : LocalDataSource {
    override fun getSession(): SessionEntity? = null
    override fun sessionExists(): Boolean = false
    override fun saveSession(matricula: String, password: String) {}
    override fun clearSession() {}

    override fun getProfile(matricula: String): ProfileStudent? = null
    override fun saveProfile(matricula: String, profile: ProfileStudent) {}
    override fun deleteProfile(matricula: String) {}

    override fun getKardex(matricula: String): List<MateriaKardex> = emptyList()
    override fun saveKardex(matricula: String, kardex: List<MateriaKardex>) {}

    override fun getCarga(matricula: String): List<MateriaCarga> = emptyList()
    override fun saveCarga(matricula: String, carga: List<MateriaCarga>) {}

    override fun getCalifUnidad(matricula: String): List<MateriaParcial> = emptyList()
    override fun saveCalifUnidad(matricula: String, parciales: List<MateriaParcial>) {}

    override fun getCalifFinal(matricula: String): List<MateriaFinal> = emptyList()
    override fun saveCalifFinal(matricula: String, finales: List<MateriaFinal>) {}

    override fun clearAll(matricula: String) {}
}
