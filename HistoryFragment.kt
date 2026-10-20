package com.dermify.dermify.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import com.dermify.dermify.R
import com.dermify.dermify.data.SkinScanRepository
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.snackbar.Snackbar
import com.dermify.dermify.data.SkinScan
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class HistoryFragment : Fragment() {

    private lateinit var repository: SkinScanRepository
    private lateinit var adapter: SkinScanAdapter
    private lateinit var rvScans: RecyclerView
    private lateinit var tvEmpty: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = SkinScanRepository(requireContext())
        adapter = SkinScanAdapter()

        rvScans = view.findViewById(R.id.rvScans)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        rvScans.layoutManager = LinearLayoutManager(requireContext())
        rvScans.adapter = adapter
        // Enable swipe-to-delete
        // Enable swipe-to-delete with red background and trash icon
        val deleteIcon = androidx.core.content.ContextCompat.getDrawable(
            requireContext(),
            R.drawable.ic_delete
        )?.apply {
            setTint(android.graphics.Color.WHITE)
        }
        val redBackground = android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#D32F2F"))

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val scan = adapter.currentList[position]
                deleteWithUndo(scan)
            }

            override fun onChildDraw(
                c: android.graphics.Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView

                // Draw red background behind the card as it slides
                redBackground.setBounds(
                    itemView.right + dX.toInt(),
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
                redBackground.draw(c)

                // Draw the trash icon on the right side
                deleteIcon?.let { icon ->
                    val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
                    val iconTop = itemView.top + iconMargin
                    val iconBottom = iconTop + icon.intrinsicHeight
                    val iconRight = itemView.right - iconMargin
                    val iconLeft = iconRight - icon.intrinsicWidth

                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    icon.draw(c)
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(rvScans)
        ItemTouchHelper(swipeCallback).attachToRecyclerView(rvScans)

        // Observe the database
        repository.allScans.observe(viewLifecycleOwner) { scans ->
            adapter.submitList(scans)
            tvEmpty.visibility = if (scans.isEmpty()) View.VISIBLE else View.GONE
            rvScans.visibility = if (scans.isEmpty()) View.GONE else View.VISIBLE
        }
    }
    private fun deleteWithUndo(scan: SkinScan) {
        // Remove from the visible list immediately for instant feedback
        val currentList = adapter.currentList.toMutableList()
        currentList.remove(scan)
        adapter.submitList(currentList)

        val snackbar = Snackbar.make(
            requireView(),
            "Scan deleted",
            Snackbar.LENGTH_LONG
        )

        var undone = false
        snackbar.setAction("Undo") {
            undone = true
            // Re-add by re-observing — the database hasn't been touched yet
            adapter.submitList(repository.allScans.value)
        }

        snackbar.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                if (!undone) {
                    // User didn't undo — commit the delete
                    lifecycleScope.launch {
                        repository.deleteScan(scan)
                    }
                }
            }
        })

        snackbar.show()
    }
}