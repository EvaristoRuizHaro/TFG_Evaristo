package com.example.diaaia.model

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Gestor de la base de datos SQLite de la aplicación Día a IA.
 *
 * Mantiene el esquema completo del TFG:
 *  - usuarios (con peso corporal y macros objetivo)
 *  - ejercicios (catálogo maestro)
 *  - alimentos (catálogo maestro con macros por 100g)
 *  - rutinas + rutina_ejercicios (definición de rutinas por usuario)
 *  - sesion_entrenamiento + registro_set (historial de entrenamientos)
 *  - registro_ingesta (historial nutricional)
 *  - entrenador_cliente (sistema deseable)
 *
 * Versión 10: esquema completo TFG.
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "DiaAIA.db", null, 10) {

    override fun onCreate(db: SQLiteDatabase?) {
        if (db == null) return

        // Habilitar claves foráneas para mantener integridad referencial
        db.execSQL("PRAGMA foreign_keys = ON;")

        crearTablaUsuarios(db)
        crearTablaEjercicios(db)
        crearTablaAlimentos(db)
        crearTablaRutinas(db)
        crearTablaRutinaEjercicios(db)
        crearTablaSesionEntrenamiento(db)
        crearTablaRegistroSet(db)
        crearTablaRegistroIngesta(db)
        crearTablaPesoCorporalHistorico(db)
        crearTablaEntrenadorCliente(db)

        // Mantener tablas antiguas durante la transición (historial compatibilidad)
        crearTablaLegacy(db)

        // Datos semilla
        poblarEjercicios(db)
        poblarAlimentos(db)
        poblarUsuarioAdminInicial(db)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (db == null) return
        // Estrategia simple: si cambia el esquema, borrar y recrear.
        db.execSQL("DROP TABLE IF EXISTS entrenador_cliente")
        db.execSQL("DROP TABLE IF EXISTS peso_corporal_historico")
        db.execSQL("DROP TABLE IF EXISTS registro_ingesta")
        db.execSQL("DROP TABLE IF EXISTS registro_set")
        db.execSQL("DROP TABLE IF EXISTS sesion_entrenamiento")
        db.execSQL("DROP TABLE IF EXISTS rutina_ejercicios")
        db.execSQL("DROP TABLE IF EXISTS rutinas")
        db.execSQL("DROP TABLE IF EXISTS alimentos")
        db.execSQL("DROP TABLE IF EXISTS ejercicios")
        db.execSQL("DROP TABLE IF EXISTS usuarios")
        db.execSQL("DROP TABLE IF EXISTS nutricion")
        db.execSQL("DROP TABLE IF EXISTS entrenamiento")
        onCreate(db)
    }

    override fun onOpen(db: SQLiteDatabase?) {
        super.onOpen(db)
        if (db != null && !db.isReadOnly) {
            db.execSQL("PRAGMA foreign_keys = ON;")
        }
    }

    // ========== CREACIÓN DE TABLAS ==========

    private fun crearTablaUsuarios(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                peso_corporal REAL DEFAULT 70.0,
                calorias_objetivo REAL DEFAULT 2500.0,
                proteinas_objetivo REAL DEFAULT 150.0,
                carbs_objetivo REAL DEFAULT 250.0,
                grasas_objetivo REAL DEFAULT 70.0,
                meta TEXT DEFAULT 'hipertrofia',
                rol TEXT DEFAULT 'cliente',
                fecha_registro DATE DEFAULT CURRENT_DATE
            )
            """.trimIndent()
        )
    }

    private fun crearTablaEjercicios(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE ejercicios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL UNIQUE,
                musculo_primario TEXT NOT NULL,
                musculo_secundario TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_ejercicio_musculo ON ejercicios(musculo_primario)")
    }

    private fun crearTablaAlimentos(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE alimentos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL UNIQUE,
                calorias_100g REAL NOT NULL,
                proteinas_100g REAL NOT NULL,
                carbs_100g REAL NOT NULL,
                grasas_100g REAL NOT NULL,
                categoria TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_alimento_categoria ON alimentos(categoria)")
    }

    private fun crearTablaRutinas(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE rutinas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario_id INTEGER NOT NULL,
                nombre TEXT NOT NULL,
                descripcion TEXT,
                fecha_creacion DATE DEFAULT CURRENT_DATE,
                FOREIGN KEY(usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_rutina_usuario ON rutinas(usuario_id)")
    }

    private fun crearTablaRutinaEjercicios(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE rutina_ejercicios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                rutina_id INTEGER NOT NULL,
                ejercicio_id INTEGER NOT NULL,
                orden INTEGER NOT NULL DEFAULT 0,
                series_planeadas INTEGER NOT NULL DEFAULT 3,
                reps_planeadas INTEGER NOT NULL DEFAULT 10,
                FOREIGN KEY(rutina_id) REFERENCES rutinas(id) ON DELETE CASCADE,
                FOREIGN KEY(ejercicio_id) REFERENCES ejercicios(id)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_rutina_ejercicios_rutina ON rutina_ejercicios(rutina_id)")
    }

    private fun crearTablaSesionEntrenamiento(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE sesion_entrenamiento (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario_id INTEGER NOT NULL,
                rutina_id INTEGER,
                nombre_rutina TEXT,
                fecha DATE DEFAULT CURRENT_DATE,
                notas TEXT,
                FOREIGN KEY(usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
                FOREIGN KEY(rutina_id) REFERENCES rutinas(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_sesion_usuario_fecha ON sesion_entrenamiento(usuario_id, fecha)")
    }

    private fun crearTablaRegistroSet(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE registro_set (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sesion_id INTEGER NOT NULL,
                ejercicio_id INTEGER NOT NULL,
                numero_set INTEGER NOT NULL,
                peso REAL NOT NULL,
                reps_planeadas INTEGER NOT NULL,
                reps_reales INTEGER NOT NULL,
                fecha DATE DEFAULT CURRENT_DATE,
                FOREIGN KEY(sesion_id) REFERENCES sesion_entrenamiento(id) ON DELETE CASCADE,
                FOREIGN KEY(ejercicio_id) REFERENCES ejercicios(id)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_set_ejercicio_fecha ON registro_set(ejercicio_id, fecha)")
    }

    private fun crearTablaRegistroIngesta(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE registro_ingesta (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario_id INTEGER NOT NULL,
                alimento_id INTEGER NOT NULL,
                cantidad_g REAL NOT NULL,
                fecha DATE DEFAULT CURRENT_DATE,
                FOREIGN KEY(usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
                FOREIGN KEY(alimento_id) REFERENCES alimentos(id)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_ingesta_usuario_fecha ON registro_ingesta(usuario_id, fecha)")
    }

    private fun crearTablaPesoCorporalHistorico(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE peso_corporal_historico (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario_id INTEGER NOT NULL,
                peso REAL NOT NULL,
                fecha DATE DEFAULT CURRENT_DATE,
                FOREIGN KEY(usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    private fun crearTablaEntrenadorCliente(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE entrenador_cliente (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                entrenador_id INTEGER NOT NULL,
                cliente_id INTEGER NOT NULL,
                fecha_vinculo DATE DEFAULT CURRENT_DATE,
                UNIQUE(entrenador_id, cliente_id),
                FOREIGN KEY(entrenador_id) REFERENCES usuarios(id) ON DELETE CASCADE,
                FOREIGN KEY(cliente_id) REFERENCES usuarios(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    /**
     * Tablas antiguas preservadas por compatibilidad con historial anterior.
     * Ahora redirigimos las lecturas/escrituras al nuevo esquema, pero las mantenemos
     * vacías para que no rompa si algún código antiguo las referencia.
     */
    private fun crearTablaLegacy(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS entrenamiento (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ejercicio TEXT,
                series INTEGER,
                reps INTEGER,
                peso REAL,
                fecha DATE DEFAULT CURRENT_DATE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS nutricion (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                alimento TEXT,
                calorias INTEGER,
                fecha DATE DEFAULT CURRENT_DATE
            )
            """.trimIndent()
        )
    }

    // ========== POBLACIÓN DE DATOS MAESTROS ==========

    private fun poblarEjercicios(db: SQLiteDatabase) {
        val ejercicios = listOf(
            // Pecho
            arrayOf("Press de Banca Plano", "Pecho", "Tríceps"),
            arrayOf("Press de Banca Inclinado", "Pecho", "Hombro"),
            arrayOf("Press de Banca Declinado", "Pecho", "Tríceps"),
            arrayOf("Aperturas con Mancuernas", "Pecho", null),
            arrayOf("Cruces de Poleas", "Pecho", null),
            arrayOf("Fondos en Paralelas", "Pecho", "Tríceps"),

            // Piernas
            arrayOf("Sentadilla Libre", "Cuádriceps", "Glúteos"),
            arrayOf("Sentadilla Búlgara", "Cuádriceps", "Glúteos"),
            arrayOf("Prensa de Piernas", "Cuádriceps", "Glúteos"),
            arrayOf("Extensión de Cuádriceps", "Cuádriceps", null),
            arrayOf("Curl Femoral", "Isquiotibiales", null),
            arrayOf("Peso Muerto Rumano", "Isquiotibiales", "Glúteos"),
            arrayOf("Peso Muerto Convencional", "Espalda", "Isquiotibiales"),
            arrayOf("Zancadas", "Cuádriceps", "Glúteos"),
            arrayOf("Hip Thrust", "Glúteos", "Isquiotibiales"),
            arrayOf("Elevación de Talones", "Gemelo", null),

            // Espalda
            arrayOf("Dominadas", "Espalda", "Bíceps"),
            arrayOf("Jalón al Pecho", "Espalda", "Bíceps"),
            arrayOf("Remo con Barra", "Espalda", "Bíceps"),
            arrayOf("Remo en Polea Baja", "Espalda", "Bíceps"),
            arrayOf("Remo con Mancuerna", "Espalda", "Bíceps"),
            arrayOf("Pull-over", "Espalda", "Pecho"),

            // Hombro
            arrayOf("Press Militar", "Hombro", "Tríceps"),
            arrayOf("Press Arnold", "Hombro", "Tríceps"),
            arrayOf("Elevaciones Laterales", "Hombro", null),
            arrayOf("Pájaros", "Hombro", null),
            arrayOf("Facepull", "Hombro", "Trapecio"),

            // Brazos
            arrayOf("Curl de Bíceps con Barra", "Bíceps", null),
            arrayOf("Curl de Bíceps Martillo", "Bíceps", "Antebrazo"),
            arrayOf("Curl Predicador", "Bíceps", null),
            arrayOf("Press Francés", "Tríceps", null),
            arrayOf("Extensión de Tríceps en Polea", "Tríceps", null),

            // Core
            arrayOf("Plancha Abdominal", "Abdomen", null),
            arrayOf("Rueda Abdominal", "Abdomen", null),
            arrayOf("Crunch", "Abdomen", null)
        )

        val stmt = db.compileStatement(
            "INSERT INTO ejercicios (nombre, musculo_primario, musculo_secundario) VALUES (?, ?, ?)"
        )
        ejercicios.forEach { (nombre, primario, secundario) ->
            stmt.bindString(1, nombre!!)
            stmt.bindString(2, primario!!)
            if (secundario == null) stmt.bindNull(3) else stmt.bindString(3, secundario)
            stmt.executeInsert()
            stmt.clearBindings()
        }
    }

    /**
     * Catálogo de alimentos con macros reales por 100g aproximados.
     * Fuente: valores medios USDA / BEDCA aproximados redondeados.
     */
    private fun poblarAlimentos(db: SQLiteDatabase) {
        val alimentos = CatalogoAlimentos.alimentos
        val stmt = db.compileStatement(
            "INSERT INTO alimentos (nombre, calorias_100g, proteinas_100g, carbs_100g, grasas_100g, categoria) VALUES (?, ?, ?, ?, ?, ?)"
        )
        alimentos.forEach { a ->
            stmt.bindString(1, a.nombre)
            stmt.bindDouble(2, a.calorias)
            stmt.bindDouble(3, a.proteinas)
            stmt.bindDouble(4, a.carbs)
            stmt.bindDouble(5, a.grasas)
            stmt.bindString(6, a.categoria)
            stmt.executeInsert()
            stmt.clearBindings()
        }
    }

    /**
     * Crea un usuario admin inicial con contraseña 'admin' (ya hasheada).
     * Sirve para que la app sea inmediatamente probable sin tener que registrar.
     */
    private fun poblarUsuarioAdminInicial(db: SQLiteDatabase) {
        val hash = PasswordHasher.hash("admin")
        val values = ContentValues().apply {
            put("nombre", "admin")
            put("password_hash", hash)
            put("peso_corporal", 75.0)
            put("calorias_objetivo", 2500.0)
            put("proteinas_objetivo", 160.0)
            put("carbs_objetivo", 280.0)
            put("grasas_objetivo", 70.0)
            put("meta", "hipertrofia")
            put("rol", "cliente")
        }
        db.insert("usuarios", null, values)
    }

    // ========== MÉTODOS LEGACY (compatibilidad con código antiguo) ==========

    fun borrarAlimento(id: Int): Int {
        val db = this.writableDatabase
        return db.delete("registro_ingesta", "id = ?", arrayOf(id.toString()))
    }

    fun borrarEjercicio(id: Int): Int {
        val db = this.writableDatabase
        // Borrado en cascada via FK: borramos la sesión y se limpian sus sets
        return db.delete("sesion_entrenamiento", "id = ?", arrayOf(id.toString()))
    }
}
