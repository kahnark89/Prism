package com.cappsconsulting.prism.companion.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The recognition-only database — kept separate from any future main session database by
 * both file name (`recognition.db`) and this distinct [RoomDatabase] class, matching
 * [RecognitionEngine]'s kdoc requirement: "separate recognition database (never in the main db)."
 * Templates are stored here; no session events, no grounding records, nothing that would
 * co-mingle behavioral data with the biometric descriptor.
 */
@Database(entities = [FaceTemplateEntity::class], version = 1, exportSchema = false)
abstract class RecognitionDatabase : RoomDatabase() {

    abstract fun faceTemplateDao(): FaceTemplateDao

    companion object {
        @Volatile private var INSTANCE: RecognitionDatabase? = null

        fun getDatabase(context: Context): RecognitionDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RecognitionDatabase::class.java,
                    "recognition.db",
                ).build().also { INSTANCE = it }
            }
    }
}
