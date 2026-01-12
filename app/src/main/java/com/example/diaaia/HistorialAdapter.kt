package com.example.diaaia

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// DEFINICIÓN ÚNICA DE LA CLASE DE DATOS
data class RegistroHistorial(val titulo: String, val detalle: String, val icono: String)

class HistorialAdapter(private val lista: List<RegistroHistorial>) :
    RecyclerView.Adapter<HistorialAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcono: TextView = view.findViewById(R.id.tvIcono)
        val tvTitulo: TextView = view.findViewById(R.id.tvTitulo)
        val tvSubtitulo: TextView = view.findViewById(R.id.tvSubtitulo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_historial, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        holder.tvTitulo.text = item.titulo
        holder.tvSubtitulo.text = item.detalle
        holder.tvIcono.text = item.icono
    }

    override fun getItemCount() = lista.size
}