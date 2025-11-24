package com.kotla.anifloat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.kotla.anifloat.data.AuthRepository
import com.kotla.anifloat.navigation.AppNavigation
import com.kotla.anifloat.ui.theme.AniFloatTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        authRepository = AuthRepository(this)
        
        // Handle Deep Link
        handleIntent(intent)

        setContent {
            AniFloatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // Observe token status to decide start destination or navigate
                    // For simplicity, we rely on manual navigation or check in AppNavigation
                    // Ideally, we'd have a ViewModel exposing "isLoggedIn" state.
                    
                    AppNavigation(navController = navController)
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null && data.scheme == "anifloat" && data.host == "auth") {
            // The URL will be something like: anifloat://auth/callback#access_token=...&token_type=Bearer...
            // Anilist returns the token in the fragment (hash), not query parameters for implicit grant.
            val fragment = data.fragment
            if (fragment != null) {
                val params = fragment.split("&").associate { 
                    val (key, value) = it.split("=")
                    key to value 
                }
                val accessToken = params["access_token"]
                if (accessToken != null) {
                    lifecycleScope.launch {
                        authRepository.saveAccessToken(accessToken)
                        // After saving, we might want to navigate to Home. 
                        // Since we are in onCreate/onNewIntent, the UI will react if observing, 
                        // or we can trigger a restart/navigation event.
                        // For now, let's assume the user re-opens the app or we refresh the state.
                    }
                }
            }
        }
    }
}
