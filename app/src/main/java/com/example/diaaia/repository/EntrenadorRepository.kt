package com.example.diaaia.repository

import android.content.ContentValues
import com.example.diaaia.model.DatabaseHelper
import com.example.diaaia.model.Usuario

/**
 * Repositorio para el sistema dual entrenador/cliente (requisito deseable).
 * Un entrenador puede vincularse con uno o varios clientes para crear rutinas
 * en su nombre y consultar su progreso.
 */
class EntrenadorRepository(private val dbHelper: DatabaseHelper) {

    fun vincular(entrenadorId: Int, clienteId: Int): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("entrenador_id", entrenadorId)
            put("cliente_id", clienteId)
        }
        return try {
            db.insertOrThrow("entrenador_cliente", null, values)
        } catch (e: Exception) {
            -1L
        }
    }

    fun desvincular(entrenadorId: Int, clienteId: Int): Boolean {
        val db = dbHelper.writableDatabase
        return db.delete(
            "entrenador_cliente",
            "entrenador_id = ? AND cliente_id = ?",
            arrayOf(entrenadorId.toString(), clienteId.toString())
        ) > 0
    }

    fun clientesDeEntrenador(entrenadorId: Int): List<Usuario> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Usuario>()
        db.rawQuery(
            """
            SELECT u.id, u.nombre, u.password_hash, u.peso_corporal, u.calorias_objetivo,
                   u.proteinas_objetivo, u.carbs_objetivo, u.grasas_objetivo, u.meta, u.rol
            FROM usuarios u
            INNER JOIN entrenador_cliente ec ON ec.cliente_id = u.id
            WHERE ec.entrenador_id = ?
            ORDER BY u.nombre
            """.trimIndent(),
            arrayOf(entrenadorId.toString())
        ).use { c ->
            while (c.moveToNext()) {
                lista.add(
                    Usuario(
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
                )
            }
        }
        return lista
    }

    /** Usuarios 'cliente' NO vinculados al entrenador, para poder añadirlos. */
    fun clientesDisponibles(entrenadorId: Int): List<Usuario> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Usuario>()
        db.rawQuery(
            """
            SELECT u.id, u.nombre, u.password_hash, u.peso_corporal, u.calorias_objetivo,
                   u.proteinas_objetivo, u.carbs_objetivo, u.grasas_objetivo, u.meta, u.rol
            FROM usuarios u
            WHERE u.rol = 'cliente'
              AND u.id NOT IN (SELECT cliente_id FROM entrenador_cliente WHERE entrenador_id = ?)
              AND u.id != ?
            ORDER BY u.nombre
            """.trimIndent(),
            arrayOf(entrenadorId.toString(), entrenadorId.toString())
        ).use { c ->
            while (c.moveToNext()) {
                lista.add(
                    Usuario(
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
                )
            }
        }
        return lista
    }
}
