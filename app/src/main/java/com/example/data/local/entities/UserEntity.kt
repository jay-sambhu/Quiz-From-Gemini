package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.UserRole

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val photoUrl: String = "",
    val role: UserRole,
    val preferredSubject: String = "All",
    val emailNotificationsEnabled: Boolean = true,
    val registeredAt: Long = System.currentTimeMillis()
)
