package com.example.expensetracker.data

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Keeps docs/DATABASE.md honest: every table and column in Room's exported schema must be described
 * there. Reads the exported JSON with plain patterns, since there's no JSON library on the test path.
 */
class DatabaseDocTest {

    // Unit tests run from the app module's directory.
    private val schema = File("schemas/com.example.expensetracker.data.AppDatabase").listFiles()!!
        .maxBy { it.nameWithoutExtension.toInt() }
        .readText()
    private val doc = File("../docs/DATABASE.md").readText()

    @Test
    fun `every table is documented`() {
        for (table in Regex(""""tableName": "(\w+)"""").findAll(schema).map { it.groupValues[1] }) {
            assertTrue("docs/DATABASE.md has no section for table `$table`", "## `$table`" in doc)
        }
    }

    @Test
    fun `every column is documented`() {
        for (column in Regex(""""columnName": "(\w+)"""").findAll(schema).map { it.groupValues[1] }.toSet()) {
            assertTrue("docs/DATABASE.md doesn't describe column `$column`", "| `$column` |" in doc)
        }
    }
}
