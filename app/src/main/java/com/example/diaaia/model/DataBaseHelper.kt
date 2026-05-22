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
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "DiaAIA.db", null, 11) {

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
        poblarDatosDemoCompleto(db)
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

    // ========== DATOS DEMO ==========

    /**
     * Rellena la BD con datos realistas para poder demostrar todas las pantallas
     * sin necesidad de introducir nada manualmente.
     *
     * Usuario demo: nombre="admin", contraseña="admin"
     * Usuario entrenador: nombre="coach", contraseña="admin"
     *
     * Incluye:
     *  - 3 rutinas con ejercicios
     *  - 10 sesiones de entrenamiento (últimos 28 días)
     *  - 14 días de registros nutricionales
     *  - 21 registros de peso corporal (últimos 60 días, bajando de 78 a 75 kg)
     *  - Vínculo entrenador-cliente entre coach y admin
     */
    private fun poblarDatosDemoCompleto(db: SQLiteDatabase) {

        // ── Entrenador (id=2) ─────────────────────────────────────────────
        val hashCoach = PasswordHasher.hash("admin")
        db.execSQL("""
            INSERT INTO usuarios (nombre, password_hash, peso_corporal, calorias_objetivo,
                proteinas_objetivo, carbs_objetivo, grasas_objetivo, meta, rol)
            VALUES ('coach', '$hashCoach', 82.0, 2800.0, 180.0, 300.0, 80.0, 'hipertrofia', 'entrenador')
        """.trimIndent())

        // ── Vínculo coach→admin ───────────────────────────────────────────
        db.execSQL("INSERT INTO entrenador_cliente (entrenador_id, cliente_id) VALUES (2, 1)")

        // ── Rutinas del admin (usuario_id=1) ──────────────────────────────
        db.execSQL("INSERT INTO rutinas (usuario_id, nombre, descripcion) VALUES (1, 'Pecho + Tríceps', 'Press plano, aperturas y extensiones de tríceps')")
        db.execSQL("INSERT INTO rutinas (usuario_id, nombre, descripcion) VALUES (1, 'Espalda + Bíceps', 'Dominadas, remos y curl de bíceps')")
        db.execSQL("INSERT INTO rutinas (usuario_id, nombre, descripcion) VALUES (1, 'Piernas', 'Sentadilla, prensa, curl femoral y hip thrust')")
        // rutina IDs: 1, 2, 3

        // Ejercicios rutina 1 – Pecho + Tríceps
        // ejercicio IDs del catálogo: 1=Press Banca Plano, 4=Aperturas, 6=Fondos, 31=Press Francés, 32=Extensión Tríceps
        db.execSQL("INSERT INTO rutina_ejercicios (rutina_id, ejercicio_id, orden, series_planeadas, reps_planeadas) VALUES (1,  1, 0, 4,  8)")
        db.execSQL("INSERT INTO rutina_ejercicios (rutina_id, ejercicio_id, orden, series_planeadas, reps_planeadas) VALUES (1,  4, 1, 3, 12)")
        db.execSQL("INSERT INTO rutina_ejercicios (rutina_id, ejercicio_id, orden, series_planeadas, reps_planeadas) VALUES (1,  6, 2, 3, 10)")
        db.execSQL("INSERT INTO rutina_ejercicios (rutina_id, ejercicio_id, orden, series_planeadas, reps_planeadas) VALUES (1, 31, 3, 3, 10)")
        db.execSQL("INSERT INTO rutina_ejercicios (rutina_id, ejercicio_id, orden, series_planeadas, reps_planeadas) VALUES (1, 32, 4, 4, 12)")

        // Ejercicios rutina 2 – Espalda + Bíceps
        // 17=Dominadas, 18=Jalón, 19=Remo Barra, 28=Curl Bíceps Barra, 29=Curl Martillo
        db.execSQL("INSERT INTO rutina_ejercicios (rutina_id, ejercicio_id, orden, series_planeadas, reps_planeadas) VALUES (2, 17, 0, 4,  8)")
        db.execSQL("INSERT INTO rutina_ejercicios (rutina_id, ejercicio_id, orden, series_planeadas, reps_planeadas) VALUES (2, 19, 1, 4, 10)")
        db.execSQL("INSERT INTO rutina_ejercicios (rutina_id, ejercicio_id, orden, series_planeadas, reps_planeadas) VALUES (2, 18, 2, 3, 12)")
        db.execSQL("INSERT INTO rutina_ejercicios (rutina_id, ejercicio_id, orden, series_planeadas, reps_planeadas) VALUES (2, 28, 3, 4, 10)")
        db.execSQL("INSERT INTO rutina_ejercicios (rutina_id, ejercicio_id, orden, series_planeadas, reps_planeadas) VALUES (2, 29, 4, 3, 12)")

        // Ejercicios rutina 3 – Piernas
        // 7=Sentadilla, 9=Prensa, 11=Curl Femoral, 15=Hip Thrust, 16=Elevación Talones
        db.execSQL("INSERT INTO rutina_ejercicios (rutina_id, ejercicio_id, orden, series_planeadas, reps_planeadas) VALUES (3,  7, 0, 4,  8)")
        db.execSQL("INSERT INTO rutina_ejercicios (rutina_id, ejercicio_id, orden, series_planeadas, reps_planeadas) VALUES (3,  9, 1, 3, 12)")
        db.execSQL("INSERT INTO rutina_ejercicios (rutina_id, ejercicio_id, orden, series_planeadas, reps_planeadas) VALUES (3, 11, 2, 4, 12)")
        db.execSQL("INSERT INTO rutina_ejercicios (rutina_id, ejercicio_id, orden, series_planeadas, reps_planeadas) VALUES (3, 15, 3, 3, 12)")
        db.execSQL("INSERT INTO rutina_ejercicios (rutina_id, ejercicio_id, orden, series_planeadas, reps_planeadas) VALUES (3, 16, 4, 4, 15)")

        // ── Sesiones de entrenamiento (10 sesiones, últimos 28 días) ──────
        // sesión 1 (-28d): Pecho
        db.execSQL("INSERT INTO sesion_entrenamiento (usuario_id, rutina_id, nombre_rutina, fecha) VALUES (1, 1, 'Pecho + Tríceps', date('now','-28 days'))")
        insertarSetsDemo(db, 1,  1, 80.0, 75.0, 70.0, 8)
        insertarSetsDemo(db, 1,  4, 16.0, 14.0, 12.0, 12)
        insertarSetsDemo(db, 1, 31, 30.0, 28.0, 26.0, 10)

        // sesión 2 (-25d): Espalda
        db.execSQL("INSERT INTO sesion_entrenamiento (usuario_id, rutina_id, nombre_rutina, fecha) VALUES (1, 2, 'Espalda + Bíceps', date('now','-25 days'))")
        insertarSetsDemo(db, 2, 17,  0.0,  0.0,  0.0,  8)
        insertarSetsDemo(db, 2, 19, 60.0, 55.0, 50.0, 10)
        insertarSetsDemo(db, 2, 28, 18.0, 16.0, 14.0, 10)

        // sesión 3 (-22d): Piernas
        db.execSQL("INSERT INTO sesion_entrenamiento (usuario_id, rutina_id, nombre_rutina, fecha) VALUES (1, 3, 'Piernas', date('now','-22 days'))")
        insertarSetsDemo(db, 3,  7, 100.0,  95.0,  90.0, 8)
        insertarSetsDemo(db, 3,  9, 130.0, 120.0, 110.0, 12)
        insertarSetsDemo(db, 3, 11,  40.0,  37.5,  35.0, 12)

        // sesión 4 (-19d): Pecho
        db.execSQL("INSERT INTO sesion_entrenamiento (usuario_id, rutina_id, nombre_rutina, fecha) VALUES (1, 1, 'Pecho + Tríceps', date('now','-19 days'))")
        insertarSetsDemo(db, 4,  1, 82.5, 77.5, 72.5, 8)
        insertarSetsDemo(db, 4,  4, 18.0, 16.0, 14.0, 12)
        insertarSetsDemo(db, 4, 32, 25.0, 22.5, 20.0, 12)

        // sesión 5 (-16d): Espalda
        db.execSQL("INSERT INTO sesion_entrenamiento (usuario_id, rutina_id, nombre_rutina, fecha) VALUES (1, 2, 'Espalda + Bíceps', date('now','-16 days'))")
        insertarSetsDemo(db, 5, 17,  0.0,  0.0,  0.0,  9)
        insertarSetsDemo(db, 5, 19, 62.5, 57.5, 52.5, 10)
        insertarSetsDemo(db, 5, 28, 20.0, 18.0, 16.0, 10)

        // sesión 6 (-13d): Piernas
        db.execSQL("INSERT INTO sesion_entrenamiento (usuario_id, rutina_id, nombre_rutina, fecha) VALUES (1, 3, 'Piernas', date('now','-13 days'))")
        insertarSetsDemo(db, 6,  7, 102.5,  97.5,  92.5, 8)
        insertarSetsDemo(db, 6,  9, 135.0, 125.0, 115.0, 12)
        insertarSetsDemo(db, 6, 15,  70.0,  65.0,  60.0, 12)

        // sesión 7 (-10d): Pecho
        db.execSQL("INSERT INTO sesion_entrenamiento (usuario_id, rutina_id, nombre_rutina, fecha) VALUES (1, 1, 'Pecho + Tríceps', date('now','-10 days'))")
        insertarSetsDemo(db, 7,  1, 85.0, 80.0, 75.0, 8)
        insertarSetsDemo(db, 7,  4, 18.0, 16.0, 14.0, 12)
        insertarSetsDemo(db, 7, 31, 32.0, 30.0, 28.0, 10)

        // sesión 8 (-7d): Espalda
        db.execSQL("INSERT INTO sesion_entrenamiento (usuario_id, rutina_id, nombre_rutina, fecha) VALUES (1, 2, 'Espalda + Bíceps', date('now','-7 days'))")
        insertarSetsDemo(db, 8, 17,  0.0,  0.0,  0.0, 10)
        insertarSetsDemo(db, 8, 19, 65.0, 60.0, 55.0, 10)
        insertarSetsDemo(db, 8, 28, 22.0, 20.0, 18.0, 10)

        // sesión 9 (-4d): Piernas
        db.execSQL("INSERT INTO sesion_entrenamiento (usuario_id, rutina_id, nombre_rutina, fecha) VALUES (1, 3, 'Piernas', date('now','-4 days'))")
        insertarSetsDemo(db, 9,  7, 105.0, 100.0,  95.0, 8)
        insertarSetsDemo(db, 9,  9, 140.0, 130.0, 120.0, 12)
        insertarSetsDemo(db, 9, 11,  42.5,  40.0,  37.5, 12)

        // sesión 10 (-1d): Pecho
        db.execSQL("INSERT INTO sesion_entrenamiento (usuario_id, rutina_id, nombre_rutina, fecha) VALUES (1, 1, 'Pecho + Tríceps', date('now','-1 days'))")
        insertarSetsDemo(db, 10,  1, 87.5, 82.5, 77.5, 8)
        insertarSetsDemo(db, 10,  4, 20.0, 18.0, 16.0, 12)
        insertarSetsDemo(db, 10, 32, 27.5, 25.0, 22.5, 12)

        // ── Historial de peso (últimos 60 días, 78 → 75 kg) ──────────────
        val pesos = listOf(
            -60 to 78.2, -57 to 77.9, -54 to 78.0, -51 to 77.7, -48 to 77.5,
            -45 to 77.3, -42 to 77.4, -39 to 77.1, -36 to 76.9, -33 to 76.8,
            -30 to 76.7, -27 to 76.5, -24 to 76.6, -21 to 76.3, -18 to 76.1,
            -15 to 75.9, -12 to 75.8,  -9 to 75.7,  -6 to 75.5,  -3 to 75.3,
              0 to 75.0
        )
        pesos.forEach { (dias, peso) ->
            db.execSQL("INSERT INTO peso_corporal_historico (usuario_id, peso, fecha) VALUES (1, $peso, date('now','$dias days'))")
        }
        db.execSQL("UPDATE usuarios SET peso_corporal = 75.0 WHERE id = 1")

        // ── Registro nutricional (últimos 14 días) ────────────────────────
        // Alimento IDs del catálogo (inserción en orden):
        //   1 = Pechuga de Pollo (165 kcal, 31g P)
        //  13 = Huevo Entero     (155 kcal, 13g P)
        //  18 = Atún al natural  (116 kcal, 26g P)
        for (d in -13..0) {
            val f = "date('now','$d days')"
            db.execSQL("INSERT INTO registro_ingesta (usuario_id, alimento_id, cantidad_g, fecha) VALUES (1, 13, 200.0, $f)") // 2 huevos desayuno
            db.execSQL("INSERT INTO registro_ingesta (usuario_id, alimento_id, cantidad_g, fecha) VALUES (1,  1, 200.0, $f)") // pechuga comida
            db.execSQL("INSERT INTO registro_ingesta (usuario_id, alimento_id, cantidad_g, fecha) VALUES (1, 18, 100.0, $f)") // atún merienda
            db.execSQL("INSERT INTO registro_ingesta (usuario_id, alimento_id, cantidad_g, fecha) VALUES (1,  1, 150.0, $f)") // pechuga cena
        }
    }

    /** Inserta 3 sets de un ejercicio en una sesión con pesos progresivos descendentes. */
    private fun insertarSetsDemo(
        db: SQLiteDatabase,
        sesionId: Int,
        ejercicioId: Int,
        p1: Double, p2: Double, p3: Double,
        reps: Int
    ) {
        listOf(1 to p1, 2 to p2, 3 to p3).forEach { (set, peso) ->
            db.execSQL("""
                INSERT INTO registro_set (sesion_id, ejercicio_id, numero_set, peso, reps_planeadas, reps_reales)
                VALUES ($sesionId, $ejercicioId, $set, $peso, $reps, $reps)
            """.trimIndent())
        }
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
