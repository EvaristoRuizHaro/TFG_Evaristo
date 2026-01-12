package com.example.diaaia.model

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "DiaAIA.db", null, 3) {

    override fun onCreate(db: SQLiteDatabase?) {
        // Tabla de Usuarios
        db?.execSQL("CREATE TABLE usuarios (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT, " +
                "password TEXT)")

        // Tabla de Nutrición
        db?.execSQL("CREATE TABLE nutricion (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "alimento TEXT, " +
                "calorias INTEGER, " +
                "fecha DATE DEFAULT CURRENT_DATE)")

        // Tabla de Entrenamiento
        db?.execSQL("CREATE TABLE entrenamiento (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ejercicio TEXT, " +
                "series INTEGER, " +
                "reps INTEGER, " +
                "peso REAL, " +
                "fecha DATE DEFAULT CURRENT_DATE)")

        // Credenciales iniciales
        db?.execSQL("INSERT INTO usuarios (nombre, password) VALUES ('admin', '1234')")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS usuarios")
        db?.execSQL("DROP TABLE IF EXISTS nutricion")
        db?.execSQL("DROP TABLE IF EXISTS entrenamiento")
        onCreate(db)
    }
}