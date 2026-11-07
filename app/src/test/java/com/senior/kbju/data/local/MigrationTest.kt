package com.senior.kbju.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationTest {

    @Test
    fun testMigration5To6Versions() {
        val migration = AppDatabase.MIGRATION_5_6
        assertEquals(5, migration.startVersion)
        assertEquals(6, migration.endVersion)
        assertTrue(AppDatabase.ALL_MIGRATIONS.contains(migration))
    }

    @Test
    fun testMigration6To7Versions() {
        val migration = AppDatabase.MIGRATION_6_7
        assertEquals(6, migration.startVersion)
        assertEquals(7, migration.endVersion)
        assertTrue(AppDatabase.ALL_MIGRATIONS.contains(migration))
    }
}
