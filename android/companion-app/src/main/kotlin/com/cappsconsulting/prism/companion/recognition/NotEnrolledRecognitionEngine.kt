package com.cappsconsulting.prism.companion.recognition

import com.cappsconsulting.prism.companion.hal.CameraFrame

/**
 * Pre-enrollment state of the recognition system — the honest starting point for a
 * device where no child has been enrolled yet. [isEnrolled] returns `false`; the
 * [com.cappsconsulting.prism.companion.orchestrator.CompanionOrchestrator] checks
 * this before calling [recognize] and skips the recognition path entirely, so
 * [recognize] is an unreachable state in normal operation and throws if somehow reached.
 *
 * This is NOT a mock that fakes recognition results. [RecognitionEngine]'s own kdoc
 * draws the line: "a fake 'yes, this is your enrolled child' is never an honest
 * placeholder for a biometric safety gate." This class says "no, nobody is enrolled"
 * — which is true, and which the orchestrator handles gracefully by staying in
 * mechanical mode indefinitely. The awakening is gated on a real enrollment UX (a
 * guided "look at the camera" setup flow) + a real model (Google ML Kit Face Detection
 * or equivalent), both of which are concrete follow-on work named in
 * [RecognitionEngine]'s kdoc.
 */
class NotEnrolledRecognitionEngine : RecognitionEngine {

    override fun isEnrolled(): Boolean = false

    override suspend fun recognize(frame: CameraFrame): RecognitionResult =
        error("recognize() called when no templates are enrolled — isEnrolled() should be checked first")

    override suspend fun enroll(frames: List<CameraFrame>): Boolean = false

    override fun deleteTemplates() = Unit
}
