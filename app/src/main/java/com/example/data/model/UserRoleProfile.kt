package com.example.data.model

/**
 * Data class representing a User Role with permissions and metadata.
 * Matches user roles: ADMIN, TEACHER, STUDENT.
 */
data class UserRoleProfile(
    val role: UserRole = UserRole.STUDENT,
    val title: String = "Student",
    val description: String = "Can take quizzes, track progress and view leaderboards",
    val canCreateQuiz: Boolean = false,
    val canManageUsers: Boolean = false,
    val canTakeQuiz: Boolean = true
) {
    companion object {
        fun fromRole(role: UserRole): UserRoleProfile = when (role) {
            UserRole.ADMIN -> UserRoleProfile(
                role = UserRole.ADMIN,
                title = "Administrator",
                description = "Full administrative access, user role assignments, and system audit monitoring",
                canCreateQuiz = true,
                canManageUsers = true,
                canTakeQuiz = true
            )
            UserRole.TEACHER -> UserRoleProfile(
                role = UserRole.TEACHER,
                title = "Teacher / Educator",
                description = "Can design, edit, publish quizzes and review aggregate student statistics",
                canCreateQuiz = true,
                canManageUsers = false,
                canTakeQuiz = true
            )
            UserRole.STUDENT -> UserRoleProfile(
                role = UserRole.STUDENT,
                title = "Student / Learner",
                description = "Can participate in assessments, review answer explanations, and view leaderboards",
                canCreateQuiz = false,
                canManageUsers = false,
                canTakeQuiz = true
            )
        }

        fun fromString(roleStr: String?): UserRoleProfile {
            val role = try {
                if (roleStr != null) UserRole.valueOf(roleStr.trim().uppercase()) else UserRole.STUDENT
            } catch (e: Exception) {
                UserRole.STUDENT
            }
            return fromRole(role)
        }
    }
}
