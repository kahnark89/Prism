package com.cappsconsulting.prism.companion.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single stored face template — the complete on-device recognition record for the enrolled
 * child. Only one template is stored at a time (enroll replaces whatever was there); the
 * primary key autogenerates so Room doesn't need an explicit ID at insert time.
 *
 * [templateBytes] is a little-endian `FloatArray` of normalized grayscale pixel values from
 * the [MlKitRecognitionEngine.TEMPLATE_SIZE]×[TEMPLATE_SIZE] face region — not the raw image.
 * Raw image pixels never leave [MlKitRecognitionEngine]; they are discarded the moment the
 * float template is extracted. This is the structural privacy guarantee: the database only
 * ever holds a normalized descriptor, not a photograph.
 *
 * Stored in `recognition.db`, not the main session database, per [RecognitionEngine]'s own
 * kdoc: "separate recognition database (never in the main db)."
 */
@Entity(tableName = "face_templates")
data class FaceTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateBytes: ByteArray,
    val capturedAtEpochSeconds: Double,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceTemplateEntity) return false
        return id == other.id && templateBytes.contentEquals(other.templateBytes)
    }

    override fun hashCode(): Int = 31 * id.hashCode() + templateBytes.contentHashCode()
}
