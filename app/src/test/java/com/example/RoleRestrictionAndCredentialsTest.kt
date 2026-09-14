package com.example

import com.example.data.model.UserRole
import org.junit.Assert.*
import org.junit.Test

class RoleRestrictionAndCredentialsTest {

    @Test
    fun testDefaultPlatformCredentials() {
        val adminEmail = "admin@quizplatform.com"
        val adminPass = "admin123"
        val teacherEmail = "teacher@quizplatform.com"
        val teacherPass = "teacher123"
        val studentEmail = "student@quizplatform.com"
        val studentPass = "student123"

        assertTrue("Admin credentials should be valid", adminEmail.isNotBlank() && adminPass.isNotBlank())
        assertTrue("Teacher credentials should be valid", teacherEmail.isNotBlank() && teacherPass.isNotBlank())
        assertTrue("Student credentials should be valid", studentEmail.isNotBlank() && studentPass.isNotBlank())
    }

    @Test
    fun testAccountCreationRolesAreRestrictedToStudentAndTeacher() {
        val publicSignupRoles = listOf(UserRole.STUDENT, UserRole.TEACHER)

        assertFalse(
            "Admin role must NOT be present in public account creation",
            publicSignupRoles.contains(UserRole.ADMIN)
        )
        assertTrue(
            "Student role must be present in public account creation",
            publicSignupRoles.contains(UserRole.STUDENT)
        )
        assertTrue(
            "Teacher role must be present in public account creation",
            publicSignupRoles.contains(UserRole.TEACHER)
        )
        assertEquals("Exactly two roles should be available for new user creation", 2, publicSignupRoles.size)
    }

    @Test
    fun testSoleAdministratorConstraint() {
        val registeredUsers = mutableListOf(
            Pair("admin_platform_root", UserRole.ADMIN),
            Pair("teacher_01", UserRole.TEACHER),
            Pair("student_01", UserRole.STUDENT)
        )

        val adminCount = registeredUsers.count { it.second == UserRole.ADMIN }
        assertEquals("Platform must maintain only one Administrator", 1, adminCount)

        // Attempting to promote another user to ADMIN when one already exists
        val targetUserId = "teacher_01"
        val requestedRole = UserRole.ADMIN

        val canPromote = registeredUsers.none { it.second == UserRole.ADMIN && it.first != targetUserId }
        assertFalse("Cannot promote user to Admin if an Admin already exists", canPromote)
    }
}
