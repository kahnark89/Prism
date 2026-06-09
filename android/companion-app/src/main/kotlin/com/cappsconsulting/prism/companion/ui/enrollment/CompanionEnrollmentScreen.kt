package com.cappsconsulting.prism.companion.ui.enrollment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.cappsconsulting.prism.companion.orchestrator.CompanionViewModel
import kotlinx.coroutines.launch

private enum class EnrollmentPhase { IDLE, CAPTURING, SUCCESS, FAILED }

/**
 * Guided enrollment flow — the "Dad and child enroll her together (name, face, voice)"
 * ritual from Epigenome 024 / Principle 10, reduced to the face-recognition step the
 * biometric gate requires.
 *
 * Five frames are captured from the live camera feed (already bound by [CompanionViewModel]'s
 * [CameraXSource]) and passed to [CompanionViewModel.enrollChild], which hands them to
 * [MlKitRecognitionEngine.enroll]. If ML Kit detects a face in at least one frame, the
 * normalized template is stored in `recognition.db` and the engine's [isEnrolled] gate
 * flips — the next [CompanionOrchestrator.handleTapToLook] will run the recognition check,
 * and a confident match triggers [AwakeningMachine.checkAndTrigger].
 *
 * Skip/Cancel exits without enrolling — the app continues in mechanical mode indefinitely,
 * [NotEnrolledRecognitionEngine]-style. No awakening; no false recognition either.
 */
@Composable
fun CompanionEnrollmentScreen(
    viewModel: CompanionViewModel,
    onEnrollmentComplete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var phase by remember { mutableStateOf(EnrollmentPhase.IDLE) }
    val scope = rememberCoroutineScope()
    val previewView = viewModel.cameraPreviewView

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (previewView != null) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            // Status card
            Column(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (phase) {
                    EnrollmentPhase.IDLE -> {
                        Text(
                            text = "Enroll ${viewModel.childName}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Hold the camera so ${viewModel.childName}'s face fills the frame, then tap Start.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center,
                        )
                    }
                    EnrollmentPhase.CAPTURING -> {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Capturing…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                        )
                    }
                    EnrollmentPhase.SUCCESS -> {
                        Text(
                            text = "Enrolled!",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Prism will now recognize ${viewModel.childName}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center,
                        )
                    }
                    EnrollmentPhase.FAILED -> {
                        Text(
                            text = "Couldn't find a face",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Make sure the face is well-lit and centered. Try again.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            when (phase) {
                EnrollmentPhase.IDLE, EnrollmentPhase.FAILED -> {
                    Button(
                        onClick = {
                            phase = EnrollmentPhase.CAPTURING
                            scope.launch {
                                val success = viewModel.enrollChild()
                                phase = if (success) EnrollmentPhase.SUCCESS else EnrollmentPhase.FAILED
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.6f),
                    ) {
                        Text(if (phase == EnrollmentPhase.IDLE) "Start" else "Try Again")
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onCancel) {
                        Text("Skip for now", color = Color.White.copy(alpha = 0.55f))
                    }
                }
                EnrollmentPhase.SUCCESS -> {
                    Button(
                        onClick = onEnrollmentComplete,
                        modifier = Modifier.fillMaxWidth(0.6f),
                    ) {
                        Text("Continue")
                    }
                }
                EnrollmentPhase.CAPTURING -> { /* buttons hidden while capturing */ }
            }

            Spacer(Modifier.weight(0.3f))
        }
    }
}
