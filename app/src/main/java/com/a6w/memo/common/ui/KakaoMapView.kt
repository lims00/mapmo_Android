package com.a6w.memo.common.ui

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.a6w.memo.R
import com.a6w.memo.common.model.MapCameraFocusData
import com.a6w.memo.common.model.MapMarkerData
import com.a6w.memo.common.util.FirebaseLogUtil
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelManager
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.label.LabelTextStyle
import java.lang.Exception

private const val KAKAO_MAP_CAMERA_MOVE_DURATION_MS = 500

/**
 * Kakao Map View
 * - Render KakaoMap View Instance as Composable AndroidView
 * - Map markers can be added with [MapMarkerData] data
 */
@Composable
fun KakaoMapView(
    modifier: Modifier = Modifier,
    cameraFocus: MapCameraFocusData? = null,
    markers: List<MapMarkerData>? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // KakaoMap Instance
    // - Initialized when MapView is Ready
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    // KakaoMap View Instance
    val mapView = remember { MapView(context) }

    DisposableEffect(lifecycleOwner, mapView) {
        // Lifecycle Observer
        // - Watch lifecycle event, and get onResume, onPause State
        // - Kakao Map View Instance must be controlled based on lifecycle
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when(event) {
                Lifecycle.Event.ON_RESUME -> mapView.resume()
                Lifecycle.Event.ON_PAUSE -> mapView.pause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        // Start KakaoMap View Instance
        mapView.start(
        object: MapLifeCycleCallback() {
            override fun onMapDestroy() {
                Log.d("KakaoMapView", "onMapDestroy()")
            }

            override fun onMapError(e: Exception?) {
                Log.e("KakaoMapView", "onMapError()")
                e?.let {
                    FirebaseLogUtil.logException(it, "KakaoMapView onMapError()")
                    it.printStackTrace()
                }
            }
        },
        object: KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                Log.d("KakaoMapView", "onMapReady()")
                kakaoMap = map
            }
        })

        onDispose {
            // Remove Lifecycle Observer when Disposed
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            // Clean up MapView Instance
            mapView.finish()
        }
    }

    // Launched Effect - Move camera focus
    LaunchedEffect(kakaoMap, cameraFocus) {
        // Return if kakao map instance or focus data is null
        if(kakaoMap == null || cameraFocus == null) return@LaunchedEffect

        // Get Latitude / Longitude data from focus data
        val focusLat = cameraFocus.latitude.toDouble()
        val focusLng = cameraFocus.longitude.toDouble()
        // Generate CameraUpdate instance
        val cameraUpdate = CameraUpdateFactory
            .newCenterPosition(LatLng.from(focusLat, focusLng))

        // Camera move animation
        val cameraAnimation = CameraAnimation.from(KAKAO_MAP_CAMERA_MOVE_DURATION_MS, true, true)

        // Move map camera to CameraUpdate instance
        kakaoMap?.moveCamera(cameraUpdate, cameraAnimation)
    }

    // Launched Effect - Add Markers to Map
    LaunchedEffect(kakaoMap, markers) {
        if(kakaoMap == null || markers.isNullOrEmpty()) return@LaunchedEffect

        val labelManager = kakaoMap?.labelManager
        labelManager?.let {
            Log.d("KakaoMapView", "Add Label: $markers")
            addMarkers(it, markers)
        }
    }

    // Map View
    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize(),
            factory = { mapView },
        )
    }
}

/**
 * Add markers on KakaoMap View
 */
private fun addMarkers(
    labelManager: LabelManager?,
    markers: List<MapMarkerData>?,
) {
    val layer = labelManager?.layer ?: return
    val markerList = markers ?: return

    // Marker groups by same locations
    val groupedMarkers = markerList.groupBy {
        LatLng.from(it.latitude.toDouble(), it.longitude.toDouble())
    }

    // Generate kakao map label style and cache it
    val labelStyles = getKakaoMapLabelStyles()

    groupedMarkers.forEach { (position, infoList) ->
        // Marker title
        // - If grouped size is 1, display only its title
        // - If size is more than 1, show count beside title
        val mainTitle = infoList.first().markerTitle
        val displayTitle = if (infoList.size > 1) {
            "$mainTitle +${infoList.size - 1}"
        } else {
            mainTitle
        }

        // Generate Label data based on marker info
        val labelText = LabelTextBuilder().setTexts(displayTitle)

        // Setup Label Option
        val labelOption = LabelOptions.from(position).apply {
            setStyles(labelStyles)
            setTexts(labelText)
        }

        // Add label to kakao map
        layer.addLabel(labelOption)
    }
}

/**
 * Get Kakao Map label style
 * - Apply themed icon drawable
 * - Apply text color / size
 */
private fun getKakaoMapLabelStyles() = LabelStyles.from(
        LabelStyle
            .from(R.drawable.map_label)
            .setTextStyles(LabelTextStyle.from(32, Color.Black.toArgb()))
    )