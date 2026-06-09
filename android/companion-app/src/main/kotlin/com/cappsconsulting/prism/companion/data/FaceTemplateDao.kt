package com.cappsconsulting.prism.companion.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FaceTemplateDao {
    @Insert
    suspend fun insert(template: FaceTemplateEntity)

    /** Returns the single stored template, or null if no enrollment has happened yet. */
    @Query("SELECT * FROM face_templates LIMIT 1")
    suspend fun getFirst(): FaceTemplateEntity?

    @Query("SELECT COUNT(*) FROM face_templates")
    suspend fun count(): Int

    /** Parent-deletable wipe — called by [com.cappsconsulting.prism.companion.recognition.MlKitRecognitionEngine.deleteTemplates]. */
    @Query("DELETE FROM face_templates")
    suspend fun deleteAll()
}
