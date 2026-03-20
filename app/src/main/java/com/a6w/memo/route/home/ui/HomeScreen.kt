package com.a6w.memo.route.home.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.a6w.memo.route.home.viewmodel.HomeViewModel
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.a6w.memo.route.home.ui.subscreen.HomeError
import com.a6w.memo.route.home.ui.subscreen.HomeLoading
import com.a6w.memo.route.home.ui.subscreen.HomeNormal
import com.a6w.memo.route.home.viewmodel.HomeUiState

/**
 * Home Screen
 * - Home UI for mapmo application
 * - Uses state from [HomeViewModel]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    navigateToMapmo: (mapmoID: String?) -> Unit,
    navigateToSetting: () -> Unit,
) {
    // Lifecycle callback for Mapmo list refresh
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // On Resume Event, load mapmo list data
            if(event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadMapmoList()
            }
        }

        // Add / Remove Lifecycle observer by composable lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // UI State
    val uiState = viewModel.uiState.collectAsState().value

    // Conditionally show each composable by current ui state
    when(uiState) {
        // Home Normal
        is HomeUiState.Normal -> {
            HomeNormal(
                modifier = modifier,
                uiState = uiState,
                moveMapCamera = viewModel::moveMapCameraToLabel,
                navigateToMapmo = navigateToMapmo,
                toggleMapmoNotify = viewModel::toggleMapmoNotify,
            )
        }

        // Home Loading
        is HomeUiState.Loading -> {
            HomeLoading()
        }

        // Home Error
        is HomeUiState.Error -> {
            HomeError(
                modifier = modifier,
                uiState = uiState,
            )
        }
    }

    // TODO: Remove Debug UI
    DebugUI(
        navigateToMapmo = navigateToMapmo,
        navigateToSetting = navigateToSetting,
    )
}

/**
 * TODO: Remove Debug UI
 */
@Composable
private fun DebugUI(
    modifier: Modifier = Modifier,
    navigateToMapmo: (mapmoID: String?) -> Unit,
    navigateToSetting: () -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        Button(
            onClick = { navigateToMapmo(null) },
        ) {
            Text("[DEBUG] Open Mapmo Screen")
        }

        Button(
            onClick = navigateToSetting,
        ) {
            Text("[DEBUG] Open Setting Screen")
        }
    }
}