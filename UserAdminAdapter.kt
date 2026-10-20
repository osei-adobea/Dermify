package com.dermify.dermify

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class UserAdminAdapter(
    private val users: MutableList<User>,
    private val onTierChange: (User, Int) -> Unit
) : RecyclerView.Adapter<UserAdminAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAvatar: TextView = view.findViewById(R.id.tvAvatar)
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvUserEmail: TextView = view.findViewById(R.id.tvUserEmail)
        val tvRoleBadge: TextView = view.findViewById(R.id.tvRoleBadge)
        val btnTier1: MaterialButton = view.findViewById(R.id.btnTier1)
        val btnTier2: MaterialButton = view.findViewById(R.id.btnTier2)
        val btnTier3: MaterialButton = view.findViewById(R.id.btnTier3)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_admin, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        val name = if (user.displayName.isNotEmpty()) user.displayName else "(no name)"

        holder.tvUserName.text = name
        holder.tvUserEmail.text = user.email

        // Avatar initials
        val initials = name.trim().split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
        holder.tvAvatar.text = if (initials.isNotEmpty()) initials else "?"

        // Role badge text
        holder.tvRoleBadge.text = when {
            user.isOwner -> "Owner"
            user.tier == 3 -> "Admin"
            user.tier == 2 -> "Specialist"
            else -> "Clinician"
        }

        // Highlight the current tier button (filled), others outlined
        highlightTier(holder, user.tier)

        holder.btnTier1.setOnClickListener { onTierChange(user, 1) }
        holder.btnTier2.setOnClickListener { onTierChange(user, 2) }
        holder.btnTier3.setOnClickListener { onTierChange(user, 3) }
    }

    private fun highlightTier(holder: UserViewHolder, tier: Int) {
        val buttons = listOf(holder.btnTier1, holder.btnTier2, holder.btnTier3)
        buttons.forEachIndexed { index, button ->
            val isActive = (index + 1) == tier
            // Filled look for active, outlined for inactive
            button.alpha = if (isActive) 1f else 0.55f
        }
    }

    override fun getItemCount(): Int = users.size

    fun updateUsers(newUsers: List<User>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }

    fun updateUserTier(uid: String, newTier: Int) {
        val index = users.indexOfFirst { it.uid == uid }
        if (index != -1) {
            users[index] = users[index].copy(tier = newTier)
            notifyItemChanged(index)
        }
    }
}