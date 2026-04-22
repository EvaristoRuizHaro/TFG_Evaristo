package com.example.diaaia.repository

import com.example.diaaia.model.DatabaseHelper
import com.example.diaaia.model.Ejercicio

/**
 * Repositorio para el catálogo de ejercicios.
 */
class EjercicioRepository(private val dbHelper: DatabaseHelper) {

    fun listarTodos(): List<Ejercicio> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Ejercicio>()
        db.rawQuery(
            "SELECT id, nombre, musculo_primario, musculo_secundario FROM ejercicios ORDER BY musculo_primario, nombre",
            null
        ).use { c ->
            while (c.moveToNext()) {
                lista.add(
                    Ejercicio(
                        id = c.getInt(0),
                        nombre = c.getString(1),
                        musculoPrimario = c.getString(2),
                        musculoSecundario = if (c.isNull(3)) null else c.getString(3)
                    )
                )
            }
        }
        return lista
    }

    fun buscarPorId(id: Int): Ejercicio? {
        val db = dbHelper.readableDatabase
        db.rawQuery(
            "SELECT id, nombre, musculo_primario, musculo_secundario FROM ejercicios WHERE id = ?",
            arrayOf(id.toString())
        ).use { c ->
            if (!c.moveToFirst()) return null
            return Ejercicio(
                id = c.getInt(0),
                nombre = c.getString(1),
                musculoPrimario = c.getString(2),
                musculoSecundario = if (c.isNull(3)) null else c.getString(3)
            )
        }
    }

    fun buscarPorNombre(nombre: String): Ejercicio? {
        val db = dbHelper.readableDatabase
        db.rawQuery(
            "SELECT id, nombre, musculo_primario, musculo_secundario FROM ejercicios WHERE nombre = ?",
            arrayOf(nombre)
        ).use { c ->
            if (!c.moveToFirst()) return null
            return Ejercicio(
                id = c.getInt(0),
                nombre = c.getString(1),
                musculoPrimario = c.getString(2),
                musculoSecundario = if (c.isNull(3)) null else c.getString(3)
            )
        }
    }
}
