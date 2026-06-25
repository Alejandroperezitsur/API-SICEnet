package com.example.sicedroid.db

import com.example.sicedroid.currentTimeMillis
import com.example.sicedroid.model.*
import kotlinx.browser.window
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.w3c.dom.get
import org.w3c.dom.set

class WebLocalDataSource : LocalDataSource {

    private val json = Json { ignoreUnknownKeys = true }
    private val stringListSerializer = ListSerializer(String.serializer())

    private val storage get() = window.localStorage

    // ── Session ────────────────────────────────────────────────────────

    override fun getSession(): SessionEntity? {
        val matricula = storage["session_matricula"] ?: return null
        val password = storage["session_password"] ?: return null
        return SessionEntity(
            matricula = matricula,
            password = password,
            is_logged_in = 1,
            last_login = storage["session_last_login"]?.toLongOrNull() ?: 0L
        )
    }

    override fun sessionExists(): Boolean {
        return storage["session_matricula"] != null
    }

    override fun saveSession(matricula: String, password: String) {
        storage["session_matricula"] = matricula
        storage["session_password"] = password
        storage["session_last_login"] = currentTimeMillis().toString()
    }

    override fun clearSession() {
        storage.removeItem("session_matricula")
        storage.removeItem("session_password")
        storage.removeItem("session_last_login")
    }

    // ── Profile ────────────────────────────────────────────────────────

    override fun getProfile(matricula: String): ProfileStudent? {
        val data = storage["profile_$matricula"] ?: return null
        return try {
            json.decodeFromString(ProfileStudent.serializer(), data)
        } catch (_: Exception) {
            null
        }
    }

    override fun saveProfile(matricula: String, profile: ProfileStudent) {
        storage["profile_$matricula"] = json.encodeToString(ProfileStudent.serializer(), profile)
    }

    override fun deleteProfile(matricula: String) {
        storage.removeItem("profile_$matricula")
    }

    // ── Kardex ─────────────────────────────────────────────────────────

    override fun getKardex(matricula: String): List<MateriaKardex> {
        val data = storage["kardex_$matricula"] ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(MateriaKardex.serializer()), data)
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun saveKardex(matricula: String, kardex: List<MateriaKardex>) {
        storage["kardex_$matricula"] = json.encodeToString(ListSerializer(MateriaKardex.serializer()), kardex)
    }

    // ── Carga ──────────────────────────────────────────────────────────

    override fun getCarga(matricula: String): List<MateriaCarga> {
        val data = storage["carga_$matricula"] ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(MateriaCarga.serializer()), data)
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun saveCarga(matricula: String, carga: List<MateriaCarga>) {
        storage["carga_$matricula"] = json.encodeToString(ListSerializer(MateriaCarga.serializer()), carga)
    }

    // ── CalifUnidad ────────────────────────────────────────────────────

    override fun getCalifUnidad(matricula: String): List<MateriaParcial> {
        val data = storage["parciales_$matricula"] ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(MateriaParcial.serializer()), data)
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun saveCalifUnidad(matricula: String, parciales: List<MateriaParcial>) {
        storage["parciales_$matricula"] = json.encodeToString(ListSerializer(MateriaParcial.serializer()), parciales)
    }

    // ── CalifFinal ─────────────────────────────────────────────────────

    override fun getCalifFinal(matricula: String): List<MateriaFinal> {
        val data = storage["finales_$matricula"] ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(MateriaFinal.serializer()), data)
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun saveCalifFinal(matricula: String, finales: List<MateriaFinal>) {
        storage["finales_$matricula"] = json.encodeToString(ListSerializer(MateriaFinal.serializer()), finales)
    }

    // ── Utilities ──────────────────────────────────────────────────────

    override fun clearAll(matricula: String) {
        storage.removeItem("profile_$matricula")
        storage.removeItem("kardex_$matricula")
        storage.removeItem("carga_$matricula")
        storage.removeItem("parciales_$matricula")
        storage.removeItem("finales_$matricula")
    }
}
