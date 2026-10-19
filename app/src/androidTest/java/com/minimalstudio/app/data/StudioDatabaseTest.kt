package com.minimalstudio.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudioDatabaseTest {
    @Test fun softDeletedCardsRemainInTrash() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), StudioDatabase::class.java
        ).build()
        try {
            val item = StudioCard(title = "Моя идея", kind = CardKind.IDEA)
            database.dao().putCard(item)
            assertEquals(1, database.dao().cards().first().size)
            database.dao().putCard(item.copy(deletedAt = System.currentTimeMillis()))
            assertTrue(database.dao().cards().first().isEmpty())
            assertEquals(item.id, database.dao().trashedCards().first().single().id)
            database.dao().putCard(item.copy(deletedAt = 0))
            assertEquals(1, database.dao().cards().first().size)
        } finally { database.close() }
    }
}
