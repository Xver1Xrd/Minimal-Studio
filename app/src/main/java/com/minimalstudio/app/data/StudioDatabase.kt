package com.minimalstudio.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface StudioDao {
    @Query("SELECT * FROM cards WHERE deletedAt = 0 ORDER BY updatedAt DESC")
    fun cards(): Flow<List<StudioCard>>

    @Query("SELECT * FROM cards WHERE deletedAt != 0 ORDER BY deletedAt DESC")
    fun trashedCards(): Flow<List<StudioCard>>

    @Query("SELECT * FROM attachments ORDER BY createdAt ASC")
    fun attachments(): Flow<List<StudioAttachment>>

    @Query("SELECT * FROM cards ORDER BY updatedAt DESC")
    suspend fun allCards(): List<StudioCard>

    @Query("SELECT * FROM attachments ORDER BY createdAt ASC")
    suspend fun allAttachments(): List<StudioAttachment>

    @Query("SELECT * FROM cards WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): StudioCard?

    @Query("SELECT * FROM attachments WHERE cardId = :cardId")
    suspend fun attachmentsFor(cardId: String): List<StudioAttachment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCard(card: StudioCard)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCards(cards: List<StudioCard>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAttachment(item: StudioAttachment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAttachments(items: List<StudioAttachment>)

    @Query("DELETE FROM attachments WHERE id IN (:ids)")
    suspend fun removeAttachments(ids: List<String>)

    @Query("DELETE FROM attachments WHERE cardId = :cardId")
    suspend fun removeAttachmentsFor(cardId: String)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun removeCard(id: String)

    @Query("SELECT * FROM cards WHERE deletedAt != 0")
    suspend fun trashedNow(): List<StudioCard>

    @Query("DELETE FROM attachments")
    suspend fun removeAllAttachments()

    @Query("DELETE FROM cards")
    suspend fun removeAllCards()
}

@Database(entities = [StudioCard::class, StudioAttachment::class], version = 2, exportSchema = false)
abstract class StudioDatabase : RoomDatabase() {
    abstract fun dao(): StudioDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cards ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0")
            }
        }
        @Volatile private var INSTANCE: StudioDatabase? = null
        fun get(context: Context): StudioDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, StudioDatabase::class.java, "minimal-studio.db")
                .addMigrations(MIGRATION_1_2)
                .build().also { INSTANCE = it }
        }
    }
}
