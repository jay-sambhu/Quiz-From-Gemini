package com.example.util

import android.content.Context
import com.example.data.local.entities.QuestionEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Data model for a single validated question parsed from a CSV file.
 */
data class ParsedCsvQuestion(
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOptionIndex: Int, // 0 = A, 1 = B, 2 = C, 3 = D
    val explanation: String = "",
    val rowNumber: Int = 1
) {
    fun toQuestionEntity(quizSetId: String): QuestionEntity {
        return QuestionEntity(
            id = "q_csv_${UUID.randomUUID().toString().take(8)}",
            quizSetId = quizSetId,
            questionText = questionText,
            optionA = optionA,
            optionB = optionB,
            optionC = optionC,
            optionD = optionD,
            correctOptionIndex = correctOptionIndex,
            explanation = explanation
        )
    }

    val correctOptionLetter: String
        get() = when (correctOptionIndex) {
            0 -> "A"
            1 -> "B"
            2 -> "C"
            3 -> "D"
            else -> "?"
        }

    val correctOptionText: String
        get() = when (correctOptionIndex) {
            0 -> optionA
            1 -> optionB
            2 -> optionC
            3 -> optionD
            else -> ""
        }
}

/**
 * Comprehensive result of a CSV parsing operation, including questions and row-level issues.
 */
data class CsvParseResult(
    val questions: List<ParsedCsvQuestion>,
    val warningsOrErrors: List<String>,
    val fileName: String,
    val totalRowsProcessed: Int,
    val totalValidCount: Int = questions.size,
    val totalIssueCount: Int = warningsOrErrors.size
) {
    val isSuccessful: Boolean get() = questions.isNotEmpty()
}

/**
 * Metadata descriptor for CSV files detected in the app's cache directory.
 */
data class CacheCsvFile(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long
) {
    val formattedSize: String
        get() = when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", sizeBytes / 1024f)
            else -> String.format(Locale.US, "%.1f MB", sizeBytes / (1024f * 1024f))
        }

    val formattedDate: String
        get() = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(lastModified))
}

/**
 * Utility helper for managing CSV templates and parsing bulk quiz questions
 * stored in the Android application's cache directory.
 */
object QuizCsvHelper {

    private const val DEFAULT_SAMPLE_CSV_NAME = "sample_quiz_questions.csv"
    private const val CSV_CACHE_SUBDIR = "quiz_imports"

    /**
     * Retrieves the dedicated CSV subfolder inside the app's cache directory.
     */
    fun getCsvCacheDirectory(context: Context): File {
        val dir = File(context.cacheDir, CSV_CACHE_SUBDIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Lists all .csv files stored in both context.cacheDir and its dedicated quiz_imports subfolder.
     */
    fun listCacheCsvFiles(context: Context): List<CacheCsvFile> {
        val result = mutableListOf<CacheCsvFile>()
        val seenPaths = mutableSetOf<String>()

        val searchDirs = listOf(getCsvCacheDirectory(context), context.cacheDir)
        for (dir in searchDirs) {
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles { f -> f.isFile && f.extension.equals("csv", ignoreCase = true) }
                files?.forEach { file ->
                    if (seenPaths.add(file.absolutePath)) {
                        result.add(
                            CacheCsvFile(
                                file = file,
                                name = file.name,
                                sizeBytes = file.length(),
                                lastModified = file.lastModified()
                            )
                        )
                    }
                }
            }
        }
        return result.sortedByDescending { it.lastModified }
    }

    /**
     * Saves or overwrites a CSV file directly in the app cache.
     */
    fun writeCsvToCache(context: Context, fileName: String, content: String): File {
        val safeName = if (fileName.endsWith(".csv", ignoreCase = true)) fileName else "$fileName.csv"
        val targetDir = getCsvCacheDirectory(context)
        val file = File(targetDir, safeName)
        file.writeText(content, Charsets.UTF_8)
        return file
    }

    /**
     * Ensures the default sample quiz CSV template is present in cache and returns it.
     */
    fun createOrGetSampleCsvInCache(context: Context, forceRefresh: Boolean = false): File {
        val targetDir = getCsvCacheDirectory(context)
        val file = File(targetDir, DEFAULT_SAMPLE_CSV_NAME)
        if (!file.exists() || forceRefresh || file.length() == 0L) {
            file.writeText(getSampleCsvContent(), Charsets.UTF_8)
        }
        return file
    }

    /**
     * Deletes a CSV file from cache.
     */
    fun deleteCacheFile(file: File): Boolean {
        return try {
            if (file.exists()) file.delete() else true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Parses a CSV file located in the application's cache.
     */
    fun parseCsvFile(file: File): CsvParseResult {
        if (!file.exists()) {
            return CsvParseResult(
                questions = emptyList(),
                warningsOrErrors = listOf("File not found in cache: ${file.name}"),
                fileName = file.name,
                totalRowsProcessed = 0
            )
        }
        val content = try {
            file.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            return CsvParseResult(
                questions = emptyList(),
                warningsOrErrors = listOf("Failed reading cache file: ${e.localizedMessage}"),
                fileName = file.name,
                totalRowsProcessed = 0
            )
        }
        return parseCsvContent(content, file.name)
    }

    /**
     * Standard RFC 4180 CSV line parser supporting quoted fields, escaped quotes,
     * embedded commas, and both CRLF and LF linebreaks.
     */
    fun parseCsvRows(csvText: String): List<List<String>> {
        val cleanText = csvText.removePrefix("\uFEFF") // Remove UTF-8 BOM
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < cleanText.length) {
            val c = cleanText[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < cleanText.length && cleanText[i + 1] == '"') {
                        currentField.append('"')
                        i++ // Skip escaped double quote
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                    }
                    '\r' -> {
                        if (i + 1 < cleanText.length && cleanText[i + 1] == '\n') {
                            i++
                        }
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                        if (currentRow.any { it.isNotBlank() }) {
                            rows.add(currentRow.toList())
                        }
                        currentRow.clear()
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                        if (currentRow.any { it.isNotBlank() }) {
                            rows.add(currentRow.toList())
                        }
                        currentRow.clear()
                    }
                    else -> currentField.append(c)
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString().trim())
            if (currentRow.any { it.isNotBlank() }) {
                rows.add(currentRow.toList())
            }
        }

        return rows
    }

    /**
     * Parses raw CSV string content into validated questions.
     */
    fun parseCsvContent(csvText: String, sourceName: String = "quiz_import.csv"): CsvParseResult {
        val rawRows = parseCsvRows(csvText)
        if (rawRows.isEmpty()) {
            return CsvParseResult(
                questions = emptyList(),
                warningsOrErrors = listOf("CSV file is empty or contains no readable rows."),
                fileName = sourceName,
                totalRowsProcessed = 0
            )
        }

        val questions = mutableListOf<ParsedCsvQuestion>()
        val issues = mutableListOf<String>()

        val firstRow = rawRows.first()
        val hasHeaders = isHeaderRow(firstRow)
        val dataRows = if (hasHeaders) rawRows.drop(1) else rawRows

        // Column mapping indices
        var qIdx = 0
        var optAIdx = 1
        var optBIdx = 2
        var optCIdx = 3
        var optDIdx = 4
        var correctIdx = 5
        var expIdx = 6

        if (hasHeaders) {
            val headerIndices = mapHeaderIndices(firstRow)
            qIdx = headerIndices["question"] ?: 0
            optAIdx = headerIndices["optA"] ?: 1
            optBIdx = headerIndices["optB"] ?: 2
            optCIdx = headerIndices["optC"] ?: 3
            optDIdx = headerIndices["optD"] ?: 4
            correctIdx = headerIndices["correct"] ?: 5
            expIdx = headerIndices["explanation"] ?: 6
        }

        dataRows.forEachIndexed { index, row ->
            val actualRowNumber = if (hasHeaders) index + 2 else index + 1

            if (row.size < 6) {
                issues.add("Row $actualRowNumber: Incomplete data (expected at least 6 columns, found ${row.size}).")
                return@forEachIndexed
            }

            val questionText = row.getOrNull(qIdx)?.trim() ?: ""
            val optionA = row.getOrNull(optAIdx)?.trim() ?: ""
            val optionB = row.getOrNull(optBIdx)?.trim() ?: ""
            val optionC = row.getOrNull(optCIdx)?.trim() ?: ""
            val optionD = row.getOrNull(optDIdx)?.trim() ?: ""
            val rawCorrect = row.getOrNull(correctIdx)?.trim() ?: ""
            val explanation = if (expIdx < row.size) row[expIdx].trim() else ""

            if (questionText.isBlank()) {
                issues.add("Row $actualRowNumber: Question prompt text is empty.")
                return@forEachIndexed
            }

            if (optionA.isBlank() || optionB.isBlank() || optionC.isBlank() || optionD.isBlank()) {
                issues.add("Row $actualRowNumber: All 4 multiple choice options (A, B, C, D) must be provided.")
                return@forEachIndexed
            }

            val parsedCorrectIndex = resolveCorrectOptionIndex(rawCorrect, optionA, optionB, optionC, optionD)
            if (parsedCorrectIndex == -1) {
                issues.add("Row $actualRowNumber: Invalid correct answer '$rawCorrect'. Expected A, B, C, D, index 0-3, or matching text.")
                return@forEachIndexed
            }

            questions.add(
                ParsedCsvQuestion(
                    questionText = questionText,
                    optionA = optionA,
                    optionB = optionB,
                    optionC = optionC,
                    optionD = optionD,
                    correctOptionIndex = parsedCorrectIndex,
                    explanation = explanation,
                    rowNumber = actualRowNumber
                )
            )
        }

        return CsvParseResult(
            questions = questions,
            warningsOrErrors = issues,
            fileName = sourceName,
            totalRowsProcessed = rawRows.size
        )
    }

    private fun isHeaderRow(row: List<String>): Boolean {
        if (row.isEmpty()) return false
        val rowJoined = row.joinToString(" ").lowercase()
        return rowJoined.contains("question") ||
                rowJoined.contains("option") ||
                rowJoined.contains("correct") ||
                rowJoined.contains("answer")
    }

    private fun mapHeaderIndices(headers: List<String>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        headers.forEachIndexed { index, header ->
            val h = header.trim().lowercase()
            when {
                h.contains("question") || h.contains("prompt") -> map["question"] = index
                h.contains("option a") || h.contains("option_a") || h.contains("choice a") || h == "a" -> map["optA"] = index
                h.contains("option b") || h.contains("option_b") || h.contains("choice b") || h == "b" -> map["optB"] = index
                h.contains("option c") || h.contains("option_c") || h.contains("choice c") || h == "c" -> map["optC"] = index
                h.contains("option d") || h.contains("option_d") || h.contains("choice d") || h == "d" -> map["optD"] = index
                h.contains("correct") || h.contains("answer") || h.contains("solution") || h == "key" -> map["correct"] = index
                h.contains("explanation") || h.contains("rationale") || h.contains("note") -> map["explanation"] = index
            }
        }
        return map
    }

    /**
     * Resolves the correct option index (0..3) from various user-input formats.
     */
    fun resolveCorrectOptionIndex(
        raw: String,
        optionA: String,
        optionB: String,
        optionC: String,
        optionD: String
    ): Int {
        val trimmed = raw.trim()

        // 1. Direct letter matching: A, B, C, D
        when (trimmed.uppercase()) {
            "A", "OPTION A", "OPTIONA", "CHOICE A" -> return 0
            "B", "OPTION B", "OPTIONB", "CHOICE B" -> return 1
            "C", "OPTION C", "OPTIONC", "CHOICE C" -> return 2
            "D", "OPTION D", "OPTIOND", "CHOICE D" -> return 3
        }

        // 2. Numeric 0-based index: "0", "1", "2", "3"
        when (trimmed) {
            "0" -> return 0
            "1" -> return 0 // If user writes 1, 2, 3, 4 vs 0, 1, 2, 3
            "2" -> return 1
            "3" -> return 2
            "4" -> return 3
        }

        // 3. Match against option text
        when {
            trimmed.equals(optionA.trim(), ignoreCase = true) -> return 0
            trimmed.equals(optionB.trim(), ignoreCase = true) -> return 1
            trimmed.equals(optionC.trim(), ignoreCase = true) -> return 2
            trimmed.equals(optionD.trim(), ignoreCase = true) -> return 3
        }

        return -1
    }

    /**
     * Provides a standard, high quality template with sample questions ready to be loaded into cache.
     */
    fun getSampleCsvContent(): String {
        return """Question,Option A,Option B,Option C,Option D,Correct Option,Explanation
"What is the powerhouse organelle of a eukaryotic cell responsible for ATP production?","Ribosome","Mitochondria","Endoplasmic Reticulum","Golgi Apparatus","B","Mitochondria produce cellular ATP through oxidative phosphorylation."
"Which fundamental data structure operates on a Last-In, First-Out (LIFO) order?","Queue","Stack","Linked List","Binary Heap","B","Stacks adhere to LIFO principles, where the most recently inserted element is removed first."
"What is the time complexity of searching in a balanced Binary Search Tree (BST) with N nodes?","O(1)","O(N)","O(log N)","O(N log N)","C","Balanced BST operations discard half of the search tree at each level, achieving logarithmic complexity."
"Which planet in our Solar System has the greatest number of confirmed moons?","Jupiter","Saturn","Uranus","Neptune","B","Saturn has 146 recognized moons, surpassing Jupiter as confirmed by astronomical surveys."
"In Python, which built-in function returns the unique memory address identifier of an object?","type()","id()","len()","hash()","B","The id() function returns the identity (memory address in CPython) of the object."
"What is the SI unit of electrical capacitance?","Farad","Henry","Tesla","Siemens","A","The Farad (symbol F) is the SI unit of electrical capacitance, named after Michael Faraday."
"Which HTTP status code signifies that a requested web resource was successfully created?","200 OK","201 Created","204 No Content","301 Moved Permanently","B","HTTP 201 Created indicates that the request has succeeded and led to the creation of a resource."
""".trimIndent()
    }
}
