package com.dermify.dermify.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dermify.dermify.R
import com.dermify.dermify.data.SkinScan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SkinScanAdapter : ListAdapter<SkinScan, SkinScanAdapter.ScanViewHolder>(DIFF_CALLBACK) {

    class ScanViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvDisease: TextView = itemView.findViewById(R.id.tvDisease)
        private val tvConfidence: TextView = itemView.findViewById(R.id.tvConfidence)
        private val tvSeverity: TextView = itemView.findViewById(R.id.tvSeverity)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)

        private val sdf = SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault())

        fun bind(scan: SkinScan) {
            val displayName = scan.userCorrectedLabel?.let { "✏ $it (corrected)" } ?: scan.predictedDisease
            tvDisease.text = displayName
            tvConfidence.text = "Confidence: ${"%.1f".format(scan.confidence * 100)}%"
            tvSeverity.text = "Severity: ${scan.severity}"
            tvDate.text = sdf.format(Date(scan.timestamp))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_skin_scan, parent, false)
        return ScanViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScanViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SkinScan>() {
            override fun areItemsTheSame(oldItem: SkinScan, newItem: SkinScan): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: SkinScan, newItem: SkinScan): Boolean {
                return oldItem == newItem
            }
        }
    }
}