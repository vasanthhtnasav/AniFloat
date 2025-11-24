package com.kotla.anifloat.ui.shortcuts

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.kotla.anifloat.data.AnilistRepository
import com.kotla.anifloat.data.AuthRepository
import com.kotla.anifloat.data.api.NetworkModule
import com.kotla.anifloat.service.FloatingOverlayService
import kotlinx.coroutines.launch

class RecentOverlayActivity : ComponentActivity() {

    private lateinit var repository: AnilistRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = AnilistRepository(NetworkModule.api, AuthRepository(this))

        lifecycleScope.launch {
            try {
                val result = repository.getCurrentUserAndList()
                val entry = result.entries.maxByOrNull { it.updatedAt ?: 0L }
                if (entry != null) {
                    val intent = Intent(this@RecentOverlayActivity, FloatingOverlayService::class.java).apply {
                        action = FloatingOverlayService.ACTION_START_OVERLAY
                        putExtra("EXTRA_ENTRY_ID", entry.id)
                        putExtra("EXTRA_TITLE", entry.media.title.userPreferred)
                        putExtra("EXTRA_PROGRESS", entry.progress)
                        putExtra("EXTRA_EPISODES", entry.media.episodes ?: 0)
                        putExtra("EXTRA_COVER_IMAGE", entry.media.coverImage.medium)
                        entry.media.relations?.edges?.firstOrNull { it.relationType == "SEQUEL" }?.node?.let { sequel ->
                            putExtra("EXTRA_SEQUEL_ID", sequel.id)
                            putExtra("EXTRA_SEQUEL_TITLE", sequel.title.userPreferred)
                            putExtra("EXTRA_SEQUEL_COVER", sequel.coverImage.medium)
                            putExtra("EXTRA_SEQUEL_EPISODES", sequel.episodes ?: 0)
                        }
                    }
                    startForegroundService(intent)
                }
            } finally {
                finish()
            }
        }
    }
}

