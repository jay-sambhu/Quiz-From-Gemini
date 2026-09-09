package com.example

import com.example.util.QuizCsvHelper
import org.junit.Assert.*
import org.junit.Test

class QuizCsvImporterTest {

    @Test
    fun parseCsvContent_validQuestionsWithHeader_parsedSuccessfully() {
        val csv = """
            Question,Option A,Option B,Option C,Option D,Correct Option,Explanation
            "What is the powerhouse of the cell?","Ribosome","Mitochondria","Nucleus","Golgi","B","Produces ATP via oxidative phosphorylation."
            "What is 2 + 2?","3","4","5","6","B","Basic arithmetic addition."
        """.trimIndent()

        val result = QuizCsvHelper.parseCsvContent(csv, "test.csv")

        assertTrue(result.isSuccessful)
        assertEquals(2, result.questions.size)
        assertTrue(result.warningsOrErrors.isEmpty())

        val q1 = result.questions[0]
        assertEquals("What is the powerhouse of the cell?", q1.questionText)
        assertEquals("Ribosome", q1.optionA)
        assertEquals("Mitochondria", q1.optionB)
        assertEquals("Nucleus", q1.optionC)
        assertEquals("Golgi", q1.optionD)
        assertEquals(1, q1.correctOptionIndex) // B = index 1
        assertEquals("B", q1.correctOptionLetter)
        assertEquals("Mitochondria", q1.correctOptionText)
        assertEquals("Produces ATP via oxidative phosphorylation.", q1.explanation)

        val q2 = result.questions[1]
        assertEquals("What is 2 + 2?", q2.questionText)
        assertEquals(1, q2.correctOptionIndex)
    }

    @Test
    fun parseCsvContent_correctOptionVariations_resolvedProperly() {
        // Test "A", "C", numeric "3", and exact matching text
        val csv = """
            Question,Option A,Option B,Option C,Option D,Correct Option,Explanation
            "Q1","Alpha","Beta","Gamma","Delta","A",""
            "Q2","Alpha","Beta","Gamma","Delta","C",""
            "Q3","Alpha","Beta","Gamma","Delta","Delta",""
            "Q4","Alpha","Beta","Gamma","Delta","0",""
        """.trimIndent()

        val result = QuizCsvHelper.parseCsvContent(csv, "variations.csv")

        assertEquals(4, result.questions.size)
        assertEquals(0, result.questions[0].correctOptionIndex) // "A" -> 0
        assertEquals(2, result.questions[1].correctOptionIndex) // "C" -> 2
        assertEquals(3, result.questions[2].correctOptionIndex) // "Delta" matching optionD -> 3
        assertEquals(0, result.questions[3].correctOptionIndex) // "0" -> 0
    }

    @Test
    fun parseCsvContent_malformedRows_recordedInWarnings() {
        val csv = """
            Question,Option A,Option B,Option C,Option D,Correct Option
            "Complete Question","A1","B1","C1","D1","A"
            "Missing Options","A2","B2"
            "","A3","B3","C3","D3","A"
            "Invalid Correct Answer","A4","B4","C4","D4","Z"
        """.trimIndent()

        val result = QuizCsvHelper.parseCsvContent(csv, "invalid.csv")

        assertEquals(1, result.questions.size)
        assertEquals("Complete Question", result.questions[0].questionText)
        assertEquals(3, result.warningsOrErrors.size)
        assertTrue(result.warningsOrErrors.any { it.contains("Incomplete data") })
        assertTrue(result.warningsOrErrors.any { it.contains("Question prompt text is empty") })
        assertTrue(result.warningsOrErrors.any { it.contains("Invalid correct answer") })
    }

    @Test
    fun parseCsvContent_quotedCommasAndEscapedQuotes_handledCorrectly() {
        val line1 = "Question,Option A,Option B,Option C,Option D,Correct Option,Explanation"
        val line2 = "\"Which countries, among France, Spain, and Italy, speak French?\",\"France\",\"Spain\",\"Italy\",\"None of the \"\"above\"\"\",\"A\",\"France speaks French.\""
        val csv = "$line1\n$line2"

        val result = QuizCsvHelper.parseCsvContent(csv, "quotes.csv")

        assertEquals(1, result.questions.size)
        val q = result.questions[0]
        assertEquals("Which countries, among France, Spain, and Italy, speak French?", q.questionText)
        assertEquals("None of the \"above\"", q.optionD)
        assertEquals(0, q.correctOptionIndex)
    }

    @Test
    fun getSampleCsvContent_parsesAllSampleQuestionsSuccessfully() {
        val sampleCsv = QuizCsvHelper.getSampleCsvContent()
        val result = QuizCsvHelper.parseCsvContent(sampleCsv, "sample.csv")

        assertTrue(result.isSuccessful)
        assertTrue(result.questions.size >= 5)
        assertTrue("No warnings expected in default sample template", result.warningsOrErrors.isEmpty())

        // Verify entity conversion
        val entity = result.questions[0].toQuestionEntity("quiz_set_123")
        assertEquals("quiz_set_123", entity.quizSetId)
        assertNotNull(entity.id)
        assertTrue(entity.id.startsWith("q_csv_"))
    }
}
