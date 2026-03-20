package com.a6w.memo.route.home.ui.subscreen

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.a6w.memo.common.def.ErrorMessageDef
import com.a6w.memo.route.home.viewmodel.HomeUiState

/**
 * Home Error UI
 */
@Composable
fun HomeError(
    modifier: Modifier = Modifier,
    uiState: HomeUiState.Error,
) {
    // UI State
    val errMessage = uiState.errMessage

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        // Error Message Text
        Text(
            text = errMessage ?: ErrorMessageDef.DEFAULT_ERROR_MESSAGE,
            fontSize = 16.sp,
        )
    }
}