package com.example.diaaia

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.diaaia.model.Usuario

/**
 * Adapter que muestra la lista de clientes vinculados a un entrenador.
 * Al pulsar la papelera se invoca [onQuitar] para confirmar la desvinculación.
 */
class ClientesAdapter(
    private val clientes: List<Usuario>,
    private val onQuitar: (Usuario) -> Unit
) : RecyclerView.Adapter<ClientesAdapter.ClienteVH>() {

    class ClienteVH(v: View) : RecyclerView.ViewHolder(v) {
        val tvNombre: TextView = v.findViewById(R.id.tvNombreCliente)
        val tvDetalle: TextView = v.findViewById(R.id.tvDetalleCliente)
        val btnQuitar: ImageButton = v.findViewById(R.id.btnQuitarCliente)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClienteVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cliente, parent, false)
        return ClienteVH(v)
    }

    override fun onBindViewHolder(holder: ClienteVH, position: Int) {
        val cli = clientes[position]
        holder.tvNombre.text = cli.nombre
        val meta = cli.meta.ifBlank { "—" }
        holder.tvDetalle.text = "Meta: $meta · ${cli.pesoCorporal} kg"
        holder.btnQuitar.setOnClickListener { onQuitar(cli) }
    }

    override fun getItemCount(): Int = clientes.size
}
