package com.example.diaaia

import android.content.ContentValues
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.diaaia.model.DatabaseHelper

class Nutricion : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nutricion)

        val dbHelper = DatabaseHelper(this)
        val etAlimento = findViewById<AutoCompleteTextView>(R.id.etAlimento)
        val etCalorias = findViewById<EditText>(R.id.etCalorias)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarComida)
        val pbCalorias = findViewById<ProgressBar>(R.id.pbCalorias)
        val tvProgreso = findViewById<TextView>(R.id.tvProgresoCalorias)

        // BASE DE DATOS DE 200 ALIMENTOS
        val alimentos = arrayOf(
            // PROTEÍNAS Y CARNES (50)
            "Pechuga de Pollo", "Muslo de Pollo", "Alitas de Pollo", "Pavo", "Ternera Magra", "Chuletón", "Entrecot", "Lomo de Cerdo", "Jamón Serrano", "Jamón Cocido",
            "Bacon", "Salchichas", "Huevo Entero", "Clara de Huevo", "Cordero", "Conejo", "Codorniz", "Pato", "Hígado", "Riñones",
            "Atún al natural", "Atún en aceite", "Salmón", "Merluza", "Bacalao", "Sardinas", "Boquerones", "Trucha", "Dorada", "Lubina",
            "Gambas", "Langostinos", "Pulpo", "Sepia", "Calamares", "Mejillones", "Almejas", "Cangrejo", "Gulas", "Emperador",
            "Tofu", "Seitán", "Tempeh", "Soja Texturizada", "Queso Batido 0%", "Requesón", "Proteína de Suero (Whey)", "Caseína", "Queso Fresco", "Burguer de Pavo",

            // CARBOHIDRATOS Y CEREALES (50)
            "Arroz Blanco", "Arroz Integral", "Arroz Vaporizado", "Arroz Basmati", "Pasta de Trigo", "Pasta Integral", "Espaguetis", "Macarrones", "Cuscús", "Quinoa",
            "Avena en copos", "Harina de Avena", "Pan Integral", "Pan de Molde", "Pan de Centeno", "Biscotes", "Tortitas de Arroz", "Tortitas de Maíz", "Patata Cocida", "Patata Asada",
            "Boniato", "Yuca", "Garbanzos", "Lentejas", "Alubias Blancas", "Alubias Pintas", "Guisantes", "Habas", "Maíz dulce", "Cereales de Maíz",
            "Muesli", "Granola", "Salvado de Trigo", "Harina de Trigo", "Gnocchi", "Trigo Sarraceno", "Mijo", "Cebada", "Harina de Almendra", "Harina de Coco",
            "Pasta de Lentejas", "Tallarines", "Lasaña (placas)", "Noodles", "Pan de Pita", "Bagel", "Croissant", "Magdalena", "Galletas María", "Galletas Integrales",

            // FRUTAS Y VERDURAS (50)
            "Manzana", "Pera", "Plátano", "Naranja", "Mandarina", "Limón", "Fresa", "Arándanos", "Frambuesas", "Moras",
            "Uvas", "Kiwi", "Piña", "Mango", "Papaya", "Sandía", "Melón", "Melocotón", "Nectarina", "Albaricoque",
            "Ciruela", "Cereza", "Higos", "Granada", "Aguacate", "Tomate", "Lechuga", "Espinacas", "Acelgas", "Brócoli",
            "Coliflor", "Repollo", "Lombarda", "Escarola", "Canónigos", "Rúcula", "Pepino", "Calabacín", "Berenjena", "Pimiento Rojo",
            "Pimiento Verde", "Cebolla", "Ajo", "Puerro", "Zanahoria", "Calabaza", "Espárragos", "Setas", "Champiñones", "Judías Verdes",

            // LÁCTEOS, GRASAS Y OTROS (50)
            "Leche Desnatada", "Leche Semidesnatada", "Leche Entera", "Leche de Almendras", "Leche de Soja", "Leche de Avena", "Leche de Arroz", "Yogur Natural", "Yogur Griego", "Yogur de Proteínas",
            "Kéfir", "Queso Cheddar", "Queso Mozzarella", "Queso Parmesano", "Queso de Cabra", "Mantequilla", "Margarina", "Aceite de Oliva", "Aceite de Coco", "Manteca",
            "Nueces", "Almendras", "Avellanas", "Cacahuetes", "Pistachos", "Anacardos", "Semillas de Chía", "Semillas de Lino", "Semillas de Girasol", "Semillas de Calabaza",
            "Mantequilla de Cacahuete", "Crema de Almendras", "Hummus", "Guacamole", "Mayonesa", "Kétchup", "Mostaza", "Salsa de Soja", "Miel", "Mermelada",
            "Chocolate Negro (>85%)", "Cacao en Polvo", "Café", "Té Verde", "Infusión", "Bebida Energética", "Zumo de Naranja", "Vino Tinto", "Cerveza", "Aceitunas"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, alimentos)
        etAlimento.setAdapter(adapter)

        actualizarProgreso(dbHelper, pbCalorias, tvProgreso)

        btnGuardar.setOnClickListener {
            val nombre = etAlimento.text.toString()
            val cal = etCalorias.text.toString().toIntOrNull() ?: 0

            if (nombre.isNotEmpty() && cal > 0) {
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put("alimento", nombre)
                    put("calorias", cal)
                }
                db.insert("nutricion", null, values)
                Toast.makeText(this, "Comida registrada", Toast.LENGTH_SHORT).show()
                actualizarProgreso(dbHelper, pbCalorias, tvProgreso)
                etAlimento.text.clear()
                etCalorias.text.clear()
            } else {
                Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun actualizarProgreso(dbHelper: DatabaseHelper, pb: ProgressBar, tv: TextView) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT SUM(calorias) FROM nutricion WHERE fecha = CURRENT_DATE", null)
        var total = 0
        if (cursor.moveToFirst()) {
            total = cursor.getInt(0)
        }
        cursor.close()

        val objetivo = 2500
        pb.max = objetivo
        pb.progress = total
        tv.text = "Hoy: $total / $objetivo kcal"
    }
}