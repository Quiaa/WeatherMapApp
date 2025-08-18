package com.example.weathermapapp.ui.webrtc

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.weathermapapp.R
import com.example.weathermapapp.data.model.webrtc.NSDataModelType
import com.example.weathermapapp.data.repository.webrtc.MainRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class IncomingCallService : Service() {

    @Inject
    lateinit var mainRepository: MainRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        observeIncomingCalls()
        return START_NOT_STICKY
    }

    private fun observeIncomingCalls() {
        mainRepository.signalingEvent.onEach {
            if (it.type == NSDataModelType.StartVideoCall) {
                val intent = Intent(this, IncomingCallActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("callerId", it.sender)
                }
                startActivity(intent)
            }
        }.launchIn(serviceScope)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun startForegroundService() {
        val notificationChannelId = "incoming_call_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "Incoming Call Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("WeatherMap App")
            .setContentText("Listening for incoming calls.")
            .setSmallIcon(R.mipmap.ic_launcher) // You should replace this with a real icon
            .build()

        startForeground(1, notification)
    }
}
