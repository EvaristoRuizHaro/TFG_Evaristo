package com.example.diaaia.repository

import android.content.ContentValues
import com.example.diaaia.model.DatabaseHelper
import com.example.diaaia.model.PasswordHasher
import com.example.diaaia.model.Usuario

/**
 * Repositorio para la entidad Usuario. Encapsula todo el acceso a SQLite para
 * operaciones de autenticación, registro y gestión de perfil.
 */
class UsuarioRepository(private val dbHelper: DatabaseHelper) {

    /**
     * Intenta autenticar al usuario con nombre/contraseña. Devuelve el Usuario si
     * las credenciales son válidas, o null si no lo son.
     */
    fun autenticar(nombre: String, password: String): Usuario? {
        val db = dbHelper.readableDatabase
        db.rawQuery(
            "SELECT id, nombre, password_hash, peso_corporal, calorias_objetivo, proteinas_objetivo, carbs_objetivo, grasas_objetivo, meta, rol FROM usuarios WHERE nombre = ?",
            arrayOf(nombre)
        ).use { c ->
            if (!c.moveToFirst()) return null
            val hashAlmacenado = c.getString(2)
            if (!PasswordHasher.verify(password, hashAlmacenado)) return null
            return cursorAUsuario(c)
        }
    }

    /**
     * Registra un nuevo usuario con la contraseña hasheada. Devuelve el ID
     * generado o -1 si el nombre ya existe u ocurre un error.
     */
    fun registrar(nombre: String, password: String, rol: String = "cliente"): Long {
        val db = dbHelper.writableDatabase
        val hash = PasswordHasher.hash(password)
        val values = ContentValues().apply {
            put("nombre", nombre)
            put("password_hash", hash)
            put("rol", rol)
        }
        return try {
            db.insertOrThrow("usuarios", null, values)
        } catch (e: Exception) {
            -1L
        }
    }

    fun buscarPorId(id: Int): Usuario? {
        val db = dbHelper.readableDatabase
        db.rawQuery(
            "SELECT id, nombre, password_hash, peso_corporal, calorias_objetivo, proteinas_objetivo, carbs_objetivo, grasas_objetivo, meta, rol FROM usuarios WHERE id = ?",
            arrayOf(id.toString())
        ).use { c ->
            if (!c.moveToFirst()) return null
            return cursorAUsuario(c)
        }
    }

    fun actualizarPerfil(usuario: Usuario): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("peso_corporal", usuario.pesoCorporal)
            put("calorias_objetivo", usuario.caloriasObjetivo)
            put("proteinas_objetivo", usuario.proteinasObjetivo)
            put("carbs_objetivo", usuario.carbsObjetivo)
            put("grasas_objetivo", usuario.grasasObjetivo)
            put("meta", usuario.meta)
            put("rol", usuario.rol)
        }
        val filas = db.update("usuarios", values, "id = ?", arrayOf(usuario.id.toString()))

        // Registrar la evolución del peso corporal si ha cambiado
        if (filas > 0) {
            val pesoValues = ContentValues().apply {
                put("usuario_id", usuario.id)
                put("peso", usuario.pesoCorporal)
            }
            db.insert("peso_corporal_historico", null, pesoValues)
        }
        return filas > 0
    }

    fun cambiarContrasena(id: Int, nuevaPassword: String): Boolean {
        val db = dbHelper.writableDatabase
        val hash = PasswordHasher.hash(nuevaPassword)
        val values = ContentValues().apply { put("password_hash", hash) }
        return db.update("usuarios", values, "id = ?", arrayOf(id.toString())) > 0
    }

    fun listarTodos(): List<Usuario> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Usuario>()
        db.rawQuery(
            "SELECT id, nombre, password_hash, peso_corporal, calorias_objetivo, proteinas_objetivo, carbs_objetivo, grasas_objetivo, meta, rol FROM usuarios ORDER BY nombre",
            null
        ).use { c ->
            while (c.moveToNext()) lista.add(cursorAUsuario(c))
        }
        return lista
    }

    /** Lista el histórico de peso corporal de un usuario. Devuelve pares (fecha, peso). */
    fun historicoPeso(usuarioId: Int): List<Pair<String, Double>> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Pair<String, Double>>()
        db.rawQuery(
            "SELECT fecha, peso FROM peso_corporal_historico WHERE usuario_id = ? ORDER BY fecha ASC",
            arrayOf(usuarioId.toString())
        ).use { c ->
            while (c.moveToNext()) {
                lista.add(c.getString(0) to c.getDouble(1))
            }
        }
        return lista
    }

    private fun cursorAUsuario(c: android.database.Cursor): Usuario = Usuario(
        id = c.getInt(0),
        nombre = c.getString(1),
        pesoCorporal = c.getDouble(3),
        caloriasObjetivo = c.getDouble(4),
        proteinasObjetivo = c.getDouble(5),
        carbsObjetivo = c.getDouble(6),
        grasasObjetivo = c.getDouble(7),
        meta = c.getString(8) ?: "hipertrofia",
        rol = c.getString(9) ?: "cliente"
    )
}
