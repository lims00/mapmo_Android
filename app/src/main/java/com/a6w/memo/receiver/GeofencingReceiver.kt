package com.a6w.memo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint

/**
 * Geofencing Receiver
 * - Receives Action for Android GMS Geofencing Service
 */
@AndroidEntryPoint
class GeofencingReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // TODO: Handle Geofencing Event
    }
}