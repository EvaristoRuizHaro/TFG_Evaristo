package com.example.diaaia

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.diaaia.model.SessionManager

/**
 * Hub principal de la app. Muestra las tarjetas de navegación hacia cada módulo.
 *
 * Si el usuario tiene rol de entrenador, también se muestra la tarjeta de
 * gestión de clientes (requisito deseable del TFG).
 */
class MenuPrincipal : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_principal)

        val session = SessionManager(this)
        if (!session.estaLogueado()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val tvSaludo = findViewById<TextView>(R.id.tvSaludoUsuario)
        tvSaludo.text = "Hola, ${session.usuarioNombre()}"

        findViewById<CardView>(R.id.cardRutinas).setOnClickListener {
            startActivity(Intent(this, GestionRutinas::class.java))
        }
        findViewById<CardView>(R.id.cardEntrenamiento).setOnClickListener {
            startActivity(Intent(this, SeleccionRutina::class.java))
        }
        findViewById<CardView>(R.id.cardNutricion).setOnClickListener {
            startActivity(Intent(this, Nutricion::class.java))
        }
        findViewById<CardView>(R.id.cardIA).setOnClickListener {
            startActivity(Intent(this, ConsejosIA::class.java))
        }
        findViewById<CardView>(R.id.cardHistorial).setOnClickListener {
            startActivity(Intent(this, Historial::class.java))
        }
        findViewById<CardView>(R.id.cardPerfil).setOnClickListener {
            startActivity(Intent(this, Perfil::class.java))
        }
        findViewById<CardView>(R.id.cardEstadisticas).setOnClickListener {
            startActivity(Intent(this, Estadisticas::class.java))
        }
        findViewById<CardView>(R.id.cardExportar).setOnClickListener {
            startActivity(Intent(this, ExportarPDF::class.java))
        }

        // Tarjeta de clientes: solo visible para entrenadores
        val cardClientes = findViewById<CardView>(R.id.cardClientes)
        if (session.esEntrenador()) {
            cardClientes.visibility = View.VISIBLE
            cardClientes.setOnClickListener {
                startActivity(Intent(this, MisClientes::class.java))
            }
        } else {
            cardClientes.visibility = View.GONE
        }

        findViewById<CardView>(R.id.cardCerrarSesion).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Quieres cerrar sesión?")
                .setPositiveButton("Sí") { _, _ ->
                    session.cerrarSesion()
                    Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }
}
