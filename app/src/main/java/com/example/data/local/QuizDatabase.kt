package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.QuizDao
import com.example.data.local.entities.*
import com.example.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        QuizSetEntity::class,
        QuestionEntity::class,
        QuizAttemptEntity::class,
        NotificationLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class QuizDatabase : RoomDatabase() {

    abstract fun quizDao(): QuizDao

    companion object {
        @Volatile
        private var INSTANCE: QuizDatabase? = null

        fun getInstance(context: Context): QuizDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuizDatabase::class.java,
                    "quiz_platform_database"
                )
                .addCallback(DatabaseCallback())
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database.quizDao())
                    }
                }
            }
        }

        private suspend fun populateInitialData(dao: QuizDao) {
            // Initial Users
            val adminUser = UserEntity(
                id = "google_admin_001",
                name = "Dr. Eleanor Vance",
                email = "admin@quizplatform.edu",
                photoUrl = "https://lh3.googleusercontent.com/a/default-user",
                role = UserRole.ADMIN,
                preferredSubject = "All"
            )
            val teacherUser = UserEntity(
                id = "google_teacher_001",
                name = "Prof. Alan Turing",
                email = "alan.turing@quizplatform.edu",
                photoUrl = "https://lh3.googleusercontent.com/a/default-user",
                role = UserRole.TEACHER,
                preferredSubject = "Computer Science"
            )
            val studentUser1 = UserEntity(
                id = "google_student_001",
                name = "Aashish Gentleman",
                email = "gentlemanaashish222@gmail.com",
                photoUrl = "https://lh3.googleusercontent.com/a/default-user",
                role = UserRole.STUDENT,
                preferredSubject = "Computer Science"
            )
            val studentUser2 = UserEntity(
                id = "google_student_002",
                name = "Sophia Chen",
                email = "sophia.chen@student.edu",
                photoUrl = "https://lh3.googleusercontent.com/a/default-user",
                role = UserRole.STUDENT,
                preferredSubject = "Mathematics"
            )
            val studentUser3 = UserEntity(
                id = "google_student_003",
                name = "Marcus Johnson",
                email = "marcus.j@student.edu",
                photoUrl = "https://lh3.googleusercontent.com/a/default-user",
                role = UserRole.STUDENT,
                preferredSubject = "Science"
            )

            dao.insertUser(adminUser)
            dao.insertUser(teacherUser)
            dao.insertUser(studentUser1)
            dao.insertUser(studentUser2)
            dao.insertUser(studentUser3)

            // Initial Categories
            val cat1 = CategoryEntity("cat_cs", "Computer Science", "Algorithms, Data Structures & AI", "Computer", "#3F51B5")
            val cat2 = CategoryEntity("cat_math", "Mathematics", "Calculus, Linear Algebra & Probability", "Functions", "#00897B")
            val cat3 = CategoryEntity("cat_sci", "Physics & Chemistry", "Quantum Mechanics & Thermodynamics", "Science", "#E64A19")
            val cat4 = CategoryEntity("cat_hist", "World History", "Ancient Civilizations & Modern World", "History", "#7B1FA2")

            dao.insertCategory(cat1)
            dao.insertCategory(cat2)
            dao.insertCategory(cat3)
            dao.insertCategory(cat4)

            // Initial Quiz Set 1 - CS
            val quiz1 = QuizSetEntity(
                id = "quiz_cs_01",
                title = "Algorithms & Data Structures Mastery",
                description = "Test your knowledge of Time Complexity, Binary Trees, and Sorting Algorithms.",
                categoryId = cat1.id,
                categoryName = cat1.name,
                creatorTeacherId = teacherUser.id,
                creatorTeacherName = teacherUser.name,
                durationMinutes = 5,
                passPercentage = 70,
                difficulty = "Medium",
                tags = "Computer Science, Algorithms, Data Structures, Coding, STEM"
            )
            dao.insertQuizSet(quiz1)

            val q1_1 = QuestionEntity(
                id = "q1_1",
                quizSetId = quiz1.id,
                questionText = "What is the worst-case time complexity of QuickSort?",
                optionA = "O(N log N)",
                optionB = "O(N)",
                optionC = "O(N^2)",
                optionD = "O(log N)",
                correctOptionIndex = 2,
                explanation = "In the worst case (when the pivot choice is poor, e.g. already sorted list), QuickSort takes O(N^2) time."
            )
            val q1_2 = QuestionEntity(
                id = "q1_2",
                quizSetId = quiz1.id,
                questionText = "Which data structure follows the LIFO (Last In First Out) principle?",
                optionA = "Queue",
                optionB = "Stack",
                optionC = "LinkedList",
                optionD = "Binary Heap",
                correctOptionIndex = 1,
                explanation = "A Stack processes items in Last In, First Out order (push and pop)."
            )
            val q1_3 = QuestionEntity(
                id = "q1_3",
                quizSetId = quiz1.id,
                questionText = "What is the optimal average time complexity of searching in a Balanced Binary Search Tree?",
                optionA = "O(1)",
                optionB = "O(N)",
                optionC = "O(log N)",
                optionD = "O(N log N)",
                correctOptionIndex = 2,
                explanation = "In a balanced BST (like AVL or Red-Black tree), search tree height is bounded by O(log N)."
            )
            dao.insertQuestions(listOf(q1_1, q1_2, q1_3))

            // Initial Quiz Set 2 - Math
            val quiz2 = QuizSetEntity(
                id = "quiz_math_01",
                title = "Advanced Calculus & Linear Algebra",
                description = "Fundamentals of derivatives, matrices, and eigenvalues.",
                categoryId = cat2.id,
                categoryName = cat2.name,
                creatorTeacherId = teacherUser.id,
                creatorTeacherName = teacherUser.name,
                durationMinutes = 5,
                passPercentage = 60,
                difficulty = "Hard",
                tags = "Math, Calculus, Linear Algebra, Matrices, STEM"
            )
            dao.insertQuizSet(quiz2)

            val q2_1 = QuestionEntity(
                id = "q2_1",
                quizSetId = quiz2.id,
                questionText = "What is the derivative of f(x) = e^(2x) with respect to x?",
                optionA = "e^(2x)",
                optionB = "2e^(2x)",
                optionC = "2x e^(2x)",
                optionD = "1/2 e^(2x)",
                correctOptionIndex = 1,
                explanation = "Using the chain rule, d/dx[e^(2x)] = 2 * e^(2x)."
            )
            val q2_2 = QuestionEntity(
                id = "q2_2",
                quizSetId = quiz2.id,
                questionText = "What is the determinant of a 2x2 matrix [[a, b], [c, d]]?",
                optionA = "ac - bd",
                optionB = "ad + bc",
                optionC = "ad - bc",
                optionD = "ab - cd",
                correctOptionIndex = 2,
                explanation = "The determinant of a 2x2 matrix is computed as ad - bc."
            )
            dao.insertQuestions(listOf(q2_1, q2_2))

            // Initial Quiz Set 3 - Science (Physics & Chemistry)
            val quiz3 = QuizSetEntity(
                id = "quiz_sci_01",
                title = "Physics & Chemistry: Thermodynamics & Quantum",
                description = "Explore conservation of energy, atomic models, and thermodynamic laws.",
                categoryId = cat3.id,
                categoryName = cat3.name,
                creatorTeacherId = teacherUser.id,
                creatorTeacherName = teacherUser.name,
                durationMinutes = 8,
                passPercentage = 65,
                difficulty = "Medium",
                tags = "Science, Physics, Chemistry, Thermodynamics, Quantum, STEM"
            )
            dao.insertQuizSet(quiz3)

            val q3_1 = QuestionEntity(
                id = "q3_1",
                quizSetId = quiz3.id,
                questionText = "Which law states that energy cannot be created or destroyed, only transformed?",
                optionA = "Second Law of Thermodynamics",
                optionB = "First Law of Thermodynamics",
                optionC = "Zeroth Law of Thermodynamics",
                optionD = "Heisenberg Uncertainty Principle",
                correctOptionIndex = 1,
                explanation = "The First Law of Thermodynamics is the principle of conservation of energy."
            )
            val q3_2 = QuestionEntity(
                id = "q3_2",
                quizSetId = quiz3.id,
                questionText = "What is the pH value of a neutral aqueous solution at 25°C?",
                optionA = "0",
                optionB = "5",
                optionC = "7",
                optionD = "14",
                correctOptionIndex = 2,
                explanation = "Pure water at 25°C has [H+] = 10^-7 M, resulting in a neutral pH of 7."
            )
            val q3_3 = QuestionEntity(
                id = "q3_3",
                quizSetId = quiz3.id,
                questionText = "Which subatomic particle has a positive elementary charge?",
                optionA = "Proton",
                optionB = "Electron",
                optionC = "Neutron",
                optionD = "Neutrino",
                correctOptionIndex = 0,
                explanation = "Protons carry a charge of +1e and are located inside the atomic nucleus."
            )
            dao.insertQuestions(listOf(q3_1, q3_2, q3_3))

            // Initial Quiz Set 4 - History (World History)
            val quiz4 = QuizSetEntity(
                id = "quiz_hist_01",
                title = "World History & Ancient Civilizations",
                description = "Journey through the cradle of civilization, the Silk Road, and the Renaissance.",
                categoryId = cat4.id,
                categoryName = cat4.name,
                creatorTeacherId = teacherUser.id,
                creatorTeacherName = teacherUser.name,
                durationMinutes = 6,
                passPercentage = 60,
                difficulty = "Easy",
                tags = "History, Civilizations, Antiquity, SilkRoad, Renaissance, Humanities"
            )
            dao.insertQuizSet(quiz4)

            val q4_1 = QuestionEntity(
                id = "q4_1",
                quizSetId = quiz4.id,
                questionText = "Which ancient civilization developed cuneiform, one of the earliest systems of writing?",
                optionA = "Ancient Egypt",
                optionB = "Sumerians of Mesopotamia",
                optionC = "Indus Valley Civilization",
                optionD = "Minoan Civilization",
                correctOptionIndex = 1,
                explanation = "Cuneiform writing was developed around 3500-3000 BCE by the Sumerians of ancient Mesopotamia."
            )
            val q4_2 = QuestionEntity(
                id = "q4_2",
                quizSetId = quiz4.id,
                questionText = "In which European city did the Renaissance cultural movement primarily originate?",
                optionA = "Paris",
                optionB = "Florence",
                optionC = "Vienna",
                optionD = "Madrid",
                correctOptionIndex = 1,
                explanation = "Florence, Italy is widely recognized as the birthplace of the European Renaissance in the 14th century."
            )
            val q4_3 = QuestionEntity(
                id = "q4_3",
                quizSetId = quiz4.id,
                questionText = "The Code of Hammurabi is an ancient legal text originating from which empire?",
                optionA = "Babylonian Empire",
                optionB = "Roman Empire",
                optionC = "Persian Empire",
                optionD = "Ottoman Empire",
                correctOptionIndex = 0,
                explanation = "The Code of Hammurabi was enacted by the Babylonian king Hammurabi around 1754 BCE."
            )
            dao.insertQuestions(listOf(q4_1, q4_2, q4_3))

            // Initial Quiz Set 5 - Math (Probability & Statistics)
            val quiz5 = QuizSetEntity(
                id = "quiz_math_02",
                title = "Probability, Statistics & Combinatorics",
                description = "Master Bayes' theorem, permutations, and normal distributions.",
                categoryId = cat2.id,
                categoryName = cat2.name,
                creatorTeacherId = teacherUser.id,
                creatorTeacherName = teacherUser.name,
                durationMinutes = 7,
                passPercentage = 70,
                difficulty = "Medium",
                tags = "Math, Probability, Statistics, Combinatorics, Data, STEM"
            )
            dao.insertQuizSet(quiz5)

            val q5_1 = QuestionEntity(
                id = "q5_1",
                quizSetId = quiz5.id,
                questionText = "What is the probability of flipping two consecutive heads with a fair coin?",
                optionA = "1/2",
                optionB = "1/4",
                optionC = "1/8",
                optionD = "3/4",
                correctOptionIndex = 1,
                explanation = "Since the two coin flips are independent events, P(H and H) = (1/2) * (1/2) = 1/4."
            )
            val q5_2 = QuestionEntity(
                id = "q5_2",
                quizSetId = quiz5.id,
                questionText = "In a standard normal distribution, what percentage of values lie within 1 standard deviation of the mean?",
                optionA = "50%",
                optionB = "68.2%",
                optionC = "95.4%",
                optionD = "99.7%",
                correctOptionIndex = 1,
                explanation = "According to the empirical rule (68-95-99.7), approximately 68.2% of data falls within 1 standard deviation."
            )
            dao.insertQuestions(listOf(q5_1, q5_2))

            // Initial Student Attempt records for Leaderboard & History
            val attempt1 = QuizAttemptEntity(
                id = "att_001",
                quizSetId = quiz1.id,
                quizTitle = quiz1.title,
                categoryName = quiz1.categoryName,
                studentId = studentUser1.id,
                studentName = studentUser1.name,
                studentEmail = studentUser1.email,
                score = 3,
                totalQuestions = 3,
                percentage = 100f,
                timeSpentSeconds = 85,
                completedAt = System.currentTimeMillis() - (1000 * 3600 * 2),
                userAnswersJson = "{\"q1_1\":2, \"q1_2\":1, \"q1_3\":2}"
            )
            val attempt2 = QuizAttemptEntity(
                id = "att_002",
                quizSetId = quiz1.id,
                quizTitle = quiz1.title,
                categoryName = quiz1.categoryName,
                studentId = studentUser2.id,
                studentName = studentUser2.name,
                studentEmail = studentUser2.email,
                score = 2,
                totalQuestions = 3,
                percentage = 66.6f,
                timeSpentSeconds = 120,
                completedAt = System.currentTimeMillis() - (1000 * 3600 * 5)
            )
            val attempt3 = QuizAttemptEntity(
                id = "att_003",
                quizSetId = quiz2.id,
                quizTitle = quiz2.title,
                categoryName = quiz2.categoryName,
                studentId = studentUser3.id,
                studentName = studentUser3.name,
                studentEmail = studentUser3.email,
                score = 2,
                totalQuestions = 2,
                percentage = 100f,
                timeSpentSeconds = 90,
                completedAt = System.currentTimeMillis() - (1000 * 3600 * 12)
            )
            val attempt4 = QuizAttemptEntity(
                id = "att_004",
                quizSetId = quiz2.id,
                quizTitle = quiz2.title,
                categoryName = quiz2.categoryName,
                studentId = studentUser2.id,
                studentName = studentUser2.name,
                studentEmail = studentUser2.email,
                score = 2,
                totalQuestions = 2,
                percentage = 100f,
                timeSpentSeconds = 105,
                completedAt = System.currentTimeMillis() - (1000 * 3600 * 8)
            )
            val attempt5 = QuizAttemptEntity(
                id = "att_005",
                quizSetId = quiz3.id,
                quizTitle = quiz3.title,
                categoryName = quiz3.categoryName,
                studentId = studentUser1.id,
                studentName = studentUser1.name,
                studentEmail = studentUser1.email,
                score = 3,
                totalQuestions = 3,
                percentage = 100f,
                timeSpentSeconds = 140,
                completedAt = System.currentTimeMillis() - (1000 * 3600 * 4)
            )
            val attempt6 = QuizAttemptEntity(
                id = "att_006",
                quizSetId = quiz1.id,
                quizTitle = quiz1.title,
                categoryName = quiz1.categoryName,
                studentId = studentUser3.id,
                studentName = studentUser3.name,
                studentEmail = studentUser3.email,
                score = 2,
                totalQuestions = 3,
                percentage = 66.7f,
                timeSpentSeconds = 110,
                completedAt = System.currentTimeMillis() - (1000 * 3600 * 20)
            )
            dao.insertAttempt(attempt1)
            dao.insertAttempt(attempt2)
            dao.insertAttempt(attempt3)
            dao.insertAttempt(attempt4)
            dao.insertAttempt(attempt5)
            dao.insertAttempt(attempt6)

            // Initial Notification Log
            dao.insertNotificationLog(
                NotificationLogEntity(
                    id = "notif_001",
                    recipientEmail = studentUser1.email,
                    recipientName = studentUser1.name,
                    subject = "New Quiz Assignment: " + quiz1.title,
                    body = "Hello " + studentUser1.name + ",\n\nProf. Alan Turing has assigned a new quiz: '" + quiz1.title + "'. Please log in to complete your test.",
                    quizSetId = quiz1.id
                )
            )
        }
    }
}
