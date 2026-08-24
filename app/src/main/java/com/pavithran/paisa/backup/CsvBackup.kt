package com.pavithran.paisa.backup

import android.content.Context
import android.net.Uri
import com.pavithran.paisa.data.Expense
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * CSV export and import. Import exists because an export you cannot restore is
 * only half a backup.
 */
object CsvBackup {

    private const val HEADER = "id,timestamp,rawText,amount,category,merchant,needsReview"

    fun toCsv(expenses: List<Expense>): String = buildString {
        appendLine(HEADER)
        expenses.forEach { e ->
            appendLine(
                listOf(
                    e.id.toString(),
                    e.timestamp.toString(),
                    e.rawText,
                    e.amount.toString(),
                    e.category,
                    e.merchant.orEmpty(),
                    if (e.needsReview) "1" else "0"
                ).joinToString(",") { quote(it) }
            )
        }
    }

    fun export(context: Context, uri: Uri, expenses: List<Expense>): Int {
        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.write(toCsv(expenses).toByteArray())
        } ?: error("Could not open $uri for writing")
        return expenses.size
    }

    fun import(context: Context, uri: Uri): List<Expense> {
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
        } ?: error("Could not open $uri for reading")
        return fromCsv(text)
    }

    fun fromCsv(text: String): List<Expense> {
        val rows = splitRows(text)
        if (rows.isEmpty()) return emptyList()
        val body = if (rows.first().firstOrNull()?.equals("id", ignoreCase = true) == true) {
            rows.drop(1)
        } else {
            rows
        }
        return body.mapNotNull { fields ->
            if (fields.size < 7) return@mapNotNull null
            val timestamp = fields[1].toLongOrNull() ?: return@mapNotNull null
            Expense(
                // id 0 so Room assigns a fresh one and an import never collides.
                id = 0,
                timestamp = timestamp,
                rawText = fields[2],
                amount = fields[3].toDoubleOrNull() ?: 0.0,
                category = fields[4].ifBlank { "Other" },
                merchant = fields[5].ifBlank { null },
                needsReview = fields[6] == "1" || fields[6].equals("true", ignoreCase = true)
            )
        }
    }

    /** Merchant names contain commas more often than you would think. */
    private fun quote(value: String): String =
        "\"" + value.replace("\"", "\"\"") + "\""

    private fun splitRows(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val field = StringBuilder()
        var fields = mutableListOf<String>()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                    field.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    fields.add(field.toString()); field.clear()
                }
                (c == '\n' || c == '\r') && !inQuotes -> {
                    if (field.isNotEmpty() || fields.isNotEmpty()) {
                        fields.add(field.toString()); field.clear()
                        rows.add(fields); fields = mutableListOf()
                    }
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || fields.isNotEmpty()) {
            fields.add(field.toString())
            rows.add(fields)
        }
        return rows
    }
}
