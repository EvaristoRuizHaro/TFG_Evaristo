package com.example.diaaia

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class MenuPrincipal : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_principal)

        // Enlazamos las tarjetas
        val cardEntreno = findViewById<CardView>(R.id.cardEntrenamiento)
        val cardDieta = findViewById<CardView>(R.id.cardNutricion)
        val cardIA = findViewById<CardView>(R.id.cardIA)
        val cardHistorial = findViewById<CardView>(R.id.cardHistorial)

        // Configuramos los clics
        cardEntreno.setOnClickListener {
            startActivity(Intent(this, Entrenamiento::class.java))
        }

        cardDieta.setOnClickListener {
            startActivity(Intent(this, Nutricion::class.java))
        }

        cardIA.setOnClickListener {
            startActivity(Intent(this, ConsejosIA::class.java))
        }

        cardHistorial.setOnClickListener {
            startActivity(Intent(this, Historial::class.java))
        }
    }
}