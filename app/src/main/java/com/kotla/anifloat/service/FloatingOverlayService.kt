package com.kotla.anifloat.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.kotla.anifloat.R
import com.kotla.anifloat.data.AnilistRepository
import com.kotla.anifloat.data.AuthRepository
import com.kotla.anifloat.data.api.NetworkModule
import com.kotla.anifloat.data.model.MediaListEntry
import com.kotla.anifloat.ui.overlay.CloseTarget
import com.kotla.anifloat.ui.overlay.OverlayContent
import com.kotla.anifloat.ui.theme.AniFloatTheme
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

class FloatingOverlayService : LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: ComposeView
    private lateinit var closeTargetView: ComposeView
    private lateinit var repository: AnilistRepository
    
    private var entryId by mutableStateOf(0)
    private var animeTitle by mutableStateOf("")
    private var currentProgress by mutableStateOf(0)
    private var totalEpisodes by mutableStateOf(0)
    private var coverImage by mutableStateOf("")
    private var isCollapsed by mutableStateOf(false)
    private var isBlurSupported by mutableStateOf(false)
    private var sequelInfo by mutableStateOf<SequelInfo?>(null)
    
    // Drag State
    private var isOverCloseTarget by mutableStateOf(false)

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()

    companion object {
        const val ACTION_START_OVERLAY = "ACTION_START_OVERLAY"
        const val CHANNEL_ID = "overlay_channel"
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            isBlurSupported = windowManager.isCrossWindowBlurEnabled
            val crossWindowBlurListener = java.util.function.Consumer<Boolean> { enabled ->
                isBlurSupported = enabled
            }
            windowManager.addCrossWindowBlurEnabledListener(crossWindowBlurListener)
        }

        val authRepo = AuthRepository(applicationContext)
        repository = AnilistRepository(NetworkModule.api, authRepo)

        startForeground(1, createNotification())

        setupViews()
    }

    private fun setupViews() {
        setupOverlayView()
        setupCloseTargetView()
    }

    private fun setupCloseTargetView() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 100 // Bottom margin
        }

        closeTargetView = ComposeView(this).apply {
            setViewTreeSavedStateRegistryOwner(this@FloatingOverlayService)
            setViewTreeLifecycleOwner(this@FloatingOverlayService)
            setViewTreeViewModelStoreOwner(this@FloatingOverlayService)
            visibility = View.GONE // Hidden initially
            
            setContent {
                AniFloatTheme {
                    CloseTarget(isOver = isOverCloseTarget)
                }
            }
        }
        
        windowManager.addView(closeTargetView, params)
    }

    private fun setupOverlayView() {
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                blurBehindRadius = 50
                flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            }
        }

        overlayView = ComposeView(this).apply {
            setViewTreeSavedStateRegistryOwner(this@FloatingOverlayService)
            setViewTreeLifecycleOwner(this@FloatingOverlayService)
            setViewTreeViewModelStoreOwner(this@FloatingOverlayService)
            
            setContent {
                AniFloatTheme {
                    Box(
                        modifier = Modifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    // Show Close Target
                                    closeTargetView.visibility = View.VISIBLE
                                    isOverCloseTarget = false
                                },
                                onDragEnd = {
                                    // Hide Close Target
                                    closeTargetView.visibility = View.GONE
                                    
                                    if (isOverCloseTarget) {
                                        stopSelf()
                                    } else {
                                        // Snap to edge logic if collapsed
                                        if (isCollapsed) {
                                            val displayWidth = resources.displayMetrics.widthPixels
                                            
                                            // Calculate collapsed width (40dp)
                                            val density = resources.displayMetrics.density
                                            val collapsedWidthPx = (40 * density).toInt()
                                            
                                            // Determine target X (Left or Right edge)
                                            val centerX = layoutParams.x + (collapsedWidthPx / 2)
                                            val targetX = if (centerX > displayWidth / 2) {
                                                displayWidth - collapsedWidthPx
                                            } else {
                                                0
                                            }
                                            
                                            layoutParams.x = targetX
                                            windowManager.updateViewLayout(overlayView, layoutParams)
                                        }
                                    }
                                    isOverCloseTarget = false
                                },
                                onDragCancel = {
                                    closeTargetView.visibility = View.GONE
                                    isOverCloseTarget = false
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                layoutParams.x += dragAmount.x.roundToInt()
                                layoutParams.y += dragAmount.y.roundToInt()
                                windowManager.updateViewLayout(overlayView, layoutParams)
                                
                                // Check Intersection
                                checkCloseIntersection(layoutParams)
                            }
                        }
                    ) {
                        val showAddSequel = sequelInfo != null && totalEpisodes > 0 && currentProgress >= totalEpisodes
                        OverlayContent(
                            title = animeTitle,
                            currentProgress = currentProgress,
                            totalEpisodes = totalEpisodes,
                            coverImage = coverImage,
                            onIncrement = { incrementProgress(1) },
                            onDecrement = { incrementProgress(-1) },
                            onMinimize = { isCollapsed = true },
                            onClose = { stopSelf() }, // Keep as backup or remove if strictly drag-only
                            isCollapsed = isCollapsed,
                            onExpand = { 
                                // Handle expansion logic to prevent off-screen clipping
                                val displayWidth = resources.displayMetrics.widthPixels
                                val density = resources.displayMetrics.density
                                val expandedWidthPx = (220 * density).toInt() // 220dp is our expanded width
                                
                                // If current X + expanded width goes off screen, shift it left
                                if (layoutParams.x + expandedWidthPx > displayWidth) {
                                    layoutParams.x = displayWidth - expandedWidthPx - (10 * density).toInt() // Add small margin
                                    // Ensure we don't go off left edge either
                                    if (layoutParams.x < 0) layoutParams.x = 0
                                    windowManager.updateViewLayout(overlayView, layoutParams)
                                }
                                isCollapsed = false 
                            },
                            isBlurSupported = isBlurSupported,
                            showAddSequelButton = showAddSequel,
                            onAddSequel = { addSequel() }
                        )
                    }
                }
            }
        }

        windowManager.addView(overlayView, layoutParams)
    }
    
    private fun checkCloseIntersection(overlayParams: WindowManager.LayoutParams) {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        // Close Target Position (Bottom Center, y=100 margin)
        val targetX = screenWidth / 2
        val targetY = screenHeight - 100 - (60 * displayMetrics.density).toInt() / 2 // Approx center of 60dp target
        
        // Overlay Position (Center)
        val overlayCenterX = overlayParams.x + (overlayView.width / 2)
        val overlayCenterY = overlayParams.y + (overlayView.height / 2)
        
        val distance = hypot((targetX - overlayCenterX).toDouble(), (targetY - overlayCenterY).toDouble())
        
        // Threshold: 150px radius
        val threshold = 150 * displayMetrics.density
        isOverCloseTarget = distance < threshold
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START_OVERLAY) {
            entryId = intent.getIntExtra("EXTRA_ENTRY_ID", 0)
            animeTitle = intent.getStringExtra("EXTRA_TITLE") ?: ""
            currentProgress = intent.getIntExtra("EXTRA_PROGRESS", 0)
            totalEpisodes = intent.getIntExtra("EXTRA_EPISODES", 0)
            coverImage = intent.getStringExtra("EXTRA_COVER_IMAGE") ?: ""
            val extraSequelId = intent.getIntExtra("EXTRA_SEQUEL_ID", -1)
            sequelInfo = if (extraSequelId > 0) {
                SequelInfo(
                    mediaId = extraSequelId,
                    title = intent.getStringExtra("EXTRA_SEQUEL_TITLE") ?: "",
                    cover = intent.getStringExtra("EXTRA_SEQUEL_COVER") ?: "",
                    episodes = intent.getIntExtra("EXTRA_SEQUEL_EPISODES", 0).takeIf { it > 0 }
                )
            } else {
                null
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
    
    private fun incrementProgress(amount: Int) {
        lifecycleScope.launch {
            try {
                val newProgress = currentProgress + amount
                
                if (newProgress < 0) return@launch
                if (totalEpisodes > 0 && newProgress > totalEpisodes) return@launch

                val status = if (totalEpisodes > 0 && currentProgress == totalEpisodes && newProgress < totalEpisodes) {
                    "CURRENT"
                } else null

                val oldProgress = currentProgress
                currentProgress = newProgress
                
                try {
                    val entry = repository.updateProgress(entryId, newProgress, status)
                    if (totalEpisodes > 0 && newProgress == totalEpisodes) {
                        val updatedSequel = extractSequel(entry)
                        if (updatedSequel != null) {
                            sequelInfo = updatedSequel
                        } // else keep existing sequel info (e.g. from initial payload)
                    } else {
                        sequelInfo = null
                    }
                } catch (e: Exception) {
                    currentProgress = oldProgress
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun addSequel() {
        val info = sequelInfo ?: return
        lifecycleScope.launch {
            try {
                val entry = repository.saveMediaListEntry(info.mediaId, 0, "CURRENT")
                entryId = entry.id
                animeTitle = entry.media.title.userPreferred
                currentProgress = entry.progress
                totalEpisodes = entry.media.episodes ?: 0
                coverImage = entry.media.coverImage.medium
                sequelInfo = extractSequel(entry)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Overlay Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("AniFloat Active")
            .setContentText("Tracking anime progress...")
            .setSmallIcon(R.drawable.ic_notification)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::windowManager.isInitialized) {
            if (::overlayView.isInitialized) windowManager.removeView(overlayView)
            if (::closeTargetView.isInitialized) windowManager.removeView(closeTargetView)
        }
    }

    private fun extractSequel(entry: MediaListEntry): SequelInfo? {
        val edge = entry.media.relations?.edges?.firstOrNull { it.relationType.equals("SEQUEL", ignoreCase = true) } ?: return null
        val node = edge.node
        return SequelInfo(
            mediaId = node.id,
            title = node.title.userPreferred,
            cover = node.coverImage.medium,
            episodes = node.episodes
        )
    }

    private data class SequelInfo(
        val mediaId: Int,
        val title: String,
        val cover: String,
        val episodes: Int?
    )
}
