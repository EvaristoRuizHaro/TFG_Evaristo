package com.example.diaaia.model

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestiona la sesión del usuario logueado usando SharedPreferences.
 *
 * No guarda la contraseña, solo el ID y el nombre del usuario para que el resto
 * de la app pueda consultar rápidamente quién está logueado sin volver a pasar
 * por la BD.
 */
class SessionManager(context: Context) {

    companion object {
        private const val PREFS = "diaaia_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ROL = "user_rol"
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun guardarSesion(id: Int, nombre: String, rol: String) {
        prefs.edit()
            .putInt(KEY_USER_ID, id)
            .putString(KEY_USER_NAME, nombre)
            .putString(KEY_USER_ROL, rol)
            .apply()
    }

    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }

    fun estaLogueado(): Boolean = prefs.contains(KEY_USER_ID)

    fun usuarioId(): Int = prefs.getInt(KEY_USER_ID, -1)

    fun usuarioNombre(): String = prefs.getString(KEY_USER_NAME, "") ?: ""

    fun usuarioRol(): String = prefs.getString(KEY_USER_ROL, "cliente") ?: "cliente"

    fun esEntrenador(): Boolean = usuarioRol() == "entrenador"
}
