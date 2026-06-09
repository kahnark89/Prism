package com.cappsconsulting.prism.parentsuite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.cappsconsulting.prism.parentsuite.ui.ParentNavHost
import com.cappsconsulting.prism.parentsuite.ui.theme.PrismParentTheme
import com.cappsconsulting.prism.parentsuite.viewmodel.ParentSuiteViewModel

/**
 * The Parent Suite's single activity — a light, readable management tool, the
 * structural opposite of
 * [com.cappsconsulting.prism.companion.MainActivity]'s all-black immersive display.
 * [enableEdgeToEdge] plus [PrismParentTheme]'s light scheme makes the window
 * chrome match Compose from frame one on Android 15.
 *
 * [ParentSuiteViewModel] is obtained here via [viewModels] and passed directly to
 * [ParentNavHost] — no DI framework, consistent with the companion module's approach.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: ParentSuiteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrismParentTheme {
                ParentNavHost(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
