package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_logs")
data class NotificationLogEntity(
    @PrimaryKey val id: String,
    val recipientEmail: String,
    val recipientName: String,
    val subject: String,
    val body: String,
    val sentAt: Long = System.currentTimeMillis(),
    val quizSetId: String = ""
)
