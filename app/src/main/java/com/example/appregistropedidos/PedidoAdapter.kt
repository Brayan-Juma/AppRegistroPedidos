package com.example.appregistropedidos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.appregistropedidos.data.Pedido
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PedidoAdapter(
    private val onItemClick: (Pedido) -> Unit
) : RecyclerView.Adapter<PedidoAdapter.PedidoViewHolder>() {

    private var pedidos: List<Pedido> = emptyList()

    fun actualizarLista(nuevaLista: List<Pedido>) {
        pedidos = nuevaLista
        notifyDataSetChanged()
    }

    inner class PedidoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombreCliente: TextView = view.findViewById(R.id.tvNombreCliente)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val tvEstadoPedido: TextView = view.findViewById(R.id.tvEstadoPedido)

        init {
            view.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(pedidos[adapterPosition])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pedido, parent, false)
        return PedidoViewHolder(view)
    }

    private val taskColors = intArrayOf(
        R.color.task_cream,
        R.color.task_blue,
        R.color.task_pink,
        R.color.task_green
    )

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        val pedido = pedidos[position]
        val context = holder.itemView.context
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        holder.tvNombreCliente.text = pedido.nombreCliente
        holder.tvFecha.text = sdf.format(Date(pedido.fecha))

        // Card background blanco limpio Neo-Tactile
        val cardView: com.google.android.material.card.MaterialCardView = holder.itemView as com.google.android.material.card.MaterialCardView
        cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))

        // Status styling: Simple colored text for Digital-Sleek vibe
        val estado = pedido.estado
        holder.tvEstadoPedido.text = when {
            estado.contains("Sincronizado", true) -> "✓ Sincronizado"
            estado.contains("Pendiente", true) -> "• Pendiente"
            estado.contains("Error", true) -> "✕ Error"
            else -> estado
        }

        when {
            estado.contains("Sincronizado", true) -> {
                holder.tvEstadoPedido.setTextColor(ContextCompat.getColor(context, R.color.emerald_600))
                holder.tvEstadoPedido.setBackgroundResource(R.drawable.bg_badge_synced)
            }
            estado.contains("Error", true) -> {
                holder.tvEstadoPedido.setTextColor(ContextCompat.getColor(context, R.color.rose_600))
                holder.tvEstadoPedido.setBackgroundResource(R.drawable.bg_badge_error)
            }
            else -> {
                holder.tvEstadoPedido.setTextColor(ContextCompat.getColor(context, R.color.amber_600))
                holder.tvEstadoPedido.setBackgroundResource(R.drawable.bg_badge_pending)
            }
        }
    }

    override fun getItemCount(): Int = pedidos.size
}
