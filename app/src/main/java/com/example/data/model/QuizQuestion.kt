package com.example.data.model

import com.example.data.local.entities.QuestionEntity
import java.util.UUID

/**
 * Data class representing a quiz question with question text, options,
 * correct answer, category, and tag for Firestore storage and retrieval.
 */
data class QuizQuestion(
    val id: String = UUID.randomUUID().toString(),
    val questionText: String = "",
    val options: List<String> = emptyList(),
    val correctAnswer: String = "",
    val category: String = "",
    val tag: String = "",
    val quizSetId: String = "",
    val explanation: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {

    /**
     * Resolves the 0-based index of the correct answer among options.
     */
    fun getCorrectOptionIndex(): Int {
        if (options.isEmpty()) return 0

        // 1. Direct string match
        val matchedIndex = options.indexOfFirst { it.trim().equals(correctAnswer.trim(), ignoreCase = true) }
        if (matchedIndex != -1) return matchedIndex

        // 2. Letter representation (A, B, C, D)
        when (correctAnswer.trim().uppercase()) {
            "A" -> return 0
            "B" -> return 1.coerceAtMost(options.lastIndex)
            "C" -> return 2.coerceAtMost(options.lastIndex)
            "D" -> return 3.coerceAtMost(options.lastIndex)
        }

        // 3. Numeric string index representation ("0", "1", "2", "3")
        correctAnswer.trim().toIntOrNull()?.let { index ->
            if (index in options.indices) return index
        }

        return 0
    }

    /**
     * Converts to Firestore document field map for storage in the 'questions' collection.
     */
    fun toFirestoreMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "id" to id,
            "questionText" to questionText,
            "options" to options,
            "correctAnswer" to correctAnswer,
            "category" to category,
            "tag" to tag,
            "quizSetId" to quizSetId,
            "explanation" to explanation,
            "createdAt" to createdAt
        )

        // Compatibility fields for legacy QuestionEntity format
        if (options.isNotEmpty()) {
            map["optionA"] = options.getOrElse(0) { "" }
            map["optionB"] = options.getOrElse(1) { "" }
            map["optionC"] = options.getOrElse(2) { "" }
            map["optionD"] = options.getOrElse(3) { "" }
            map["correctOptionIndex"] = getCorrectOptionIndex()
        }

        return map
    }

    /**
     * Converts to Room local database QuestionEntity.
     */
    fun toQuestionEntity(targetQuizSetId: String = quizSetId): QuestionEntity {
        return QuestionEntity(
            id = id,
            quizSetId = targetQuizSetId,
            questionText = questionText,
            optionA = options.getOrElse(0) { "" },
            optionB = options.getOrElse(1) { "" },
            optionC = options.getOrElse(2) { "" },
            optionD = options.getOrElse(3) { "" },
            correctOptionIndex = getCorrectOptionIndex(),
            explanation = explanation
        )
    }

    companion object {
        /**
         * Builds a QuizQuestion from a Room QuestionEntity.
         */
        fun fromQuestionEntity(
            entity: QuestionEntity,
            category: String = "",
            tag: String = ""
        ): QuizQuestion {
            val options = listOf(entity.optionA, entity.optionB, entity.optionC, entity.optionD)
                .filter { it.isNotBlank() }
            val correctAnswer = options.getOrElse(entity.correctOptionIndex) {
                options.firstOrNull() ?: ""
            }
            return QuizQuestion(
                id = entity.id,
                questionText = entity.questionText,
                options = options,
                correctAnswer = correctAnswer,
                category = category,
                tag = tag,
                quizSetId = entity.quizSetId,
                explanation = entity.explanation
            )
        }

        /**
         * Parses a QuizQuestion from a Firestore document snapshot map.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromFirestoreMap(id: String, map: Map<String, Any?>): QuizQuestion {
            val qText = map["questionText"] as? String ?: ""
            val rawOptions = map["options"] as? List<*>
            val options: List<String> = if (rawOptions != null) {
                rawOptions.mapNotNull { it?.toString() }
            } else {
                listOfNotNull(
                    map["optionA"] as? String,
                    map["optionB"] as? String,
                    map["optionC"] as? String,
                    map["optionD"] as? String
                ).filter { it.isNotBlank() }
            }

            val rawCorrectAnswer = map["correctAnswer"] as? String
            val correctAnswer = if (!rawCorrectAnswer.isNullOrBlank()) {
                rawCorrectAnswer
            } else {
                val correctIndex = (map["correctOptionIndex"] as? Number)?.toInt() ?: 0
                options.getOrElse(correctIndex) { options.firstOrNull() ?: "" }
            }

            return QuizQuestion(
                id = (map["id"] as? String) ?: id,
                questionText = qText,
                options = options,
                correctAnswer = correctAnswer,
                category = (map["category"] as? String) ?: "",
                tag = (map["tag"] as? String) ?: (map["tags"] as? String) ?: "",
                quizSetId = (map["quizSetId"] as? String) ?: "",
                explanation = (map["explanation"] as? String) ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
