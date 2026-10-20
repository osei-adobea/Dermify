package com.dermify.dermify.telemedicine

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dermify.dermify.R
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReferralAdapter(
    private var cases: List<ReferralCase>,
    // When false (the "Sent" tab), the Send/Delete row is hidden.
    private var showActions: Boolean,
    private val onSend: (ReferralCase) -> Unit,
    private val onDelete: (ReferralCase) -> Unit
) : RecyclerView.Adapter<ReferralAdapter.VH>() {

    private val dateFmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val disease: TextView = view.findViewById(R.id.tvItemDisease)
        val meta: TextView = view.findViewById(R.id.tvItemMeta)
        val date: TextView = view.findViewById(R.id.tvItemDate)
        val note: TextView = view.findViewById(R.id.tvItemNote)
        val actions: View = view.findViewById(R.id.actionRow)
        val send: MaterialButton = view.findViewById(R.id.btnItemSend)
        val delete: MaterialButton = view.findViewById(R.id.btnItemDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_referral, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = cases[position]

        holder.disease.text = c.predictedClass.split(" ").joinToString(" ") {
            it.replaceFirstChar { ch -> ch.uppercase() }
        }
        val pct = Math.round(c.confidence * 100)
        holder.meta.text = "Confidence: $pct%  •  Severity: ${c.severity}"
        holder.date.text = dateFmt.format(Date(c.timestampMillis))

        if (c.clinicalNote.isNullOrBlank()) {
            holder.note.visibility = View.GONE
        } else {
            holder.note.visibility = View.VISIBLE
            holder.note.text = "Note: ${c.clinicalNote}"
        }

        // Sent tab: no Send/Delete buttons.
        holder.actions.visibility = if (showActions) View.VISIBLE else View.GONE
        holder.send.setOnClickListener { onSend(c) }
        holder.delete.setOnClickListener { onDelete(c) }
    }

    override fun getItemCount(): Int = cases.size

    fun update(newCases: List<ReferralCase>, showActions: Boolean) {
        this.cases = newCases
        this.showActions = showActions
        notifyDataSetChanged()
    }
}