package com.kotla.anifloat.ui.screens

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.kotla.anifloat.data.model.MediaListEntry
import com.kotla.anifloat.service.FloatingOverlayService
import com.kotla.anifloat.ui.theme.BorderColor
import com.kotla.anifloat.ui.theme.CardStroke
import com.kotla.anifloat.ui.theme.DarkBackground
import com.kotla.anifloat.ui.theme.DarkSurface
import com.kotla.anifloat.ui.theme.PrimaryAccent
import com.kotla.anifloat.ui.theme.PrimaryAccentVariant
import com.kotla.anifloat.ui.theme.PrimaryGradientEnd
import com.kotla.anifloat.ui.theme.PrimaryGradientStart
import com.kotla.anifloat.ui.theme.TextPrimary
import com.kotla.anifloat.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(LocalContext.current.applicationContext as Application))
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("My Watching List", color = TextPrimary, fontWeight = FontWeight.Bold) },
                actions = {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PrimaryAccent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val viewer = (uiState as? HomeUiState.Success)?.viewer
                    ProfileMenu(
                        avatarUrl = viewer?.avatar?.large,
                        viewerName = viewer?.name,
                        onProfile = {
                            viewer?.id?.let { openAniListProfile(context, it) }
                        },
                        onLogout = {
                            viewModel.logout(onLogout)
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize().background(DarkBackground)) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    if (!isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PrimaryAccent)
                    }
                }
                is HomeUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error: ${state.message}",
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.fetchAnimeList() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
                is HomeUiState.Success -> {
                    AnimeList(
                        animeList = state.animeList,
                        onItemClick = { entry ->
                            if (Settings.canDrawOverlays(context)) {
                                val sequelNode = entry.media.relations?.edges?.firstOrNull { it.relationType == "SEQUEL" }?.node
                                val intent = Intent(context, FloatingOverlayService::class.java).apply {
                                    action = FloatingOverlayService.ACTION_START_OVERLAY
                                    putExtra("EXTRA_ENTRY_ID", entry.id)
                                    putExtra("EXTRA_TITLE", entry.media.title.userPreferred)
                                    putExtra("EXTRA_PROGRESS", entry.progress)
                                    putExtra("EXTRA_EPISODES", entry.media.episodes ?: 0)
                                    putExtra("EXTRA_COVER_IMAGE", entry.media.coverImage.medium)
                                    if (sequelNode != null) {
                                        putExtra("EXTRA_SEQUEL_ID", sequelNode.id)
                                        putExtra("EXTRA_SEQUEL_TITLE", sequelNode.title.userPreferred)
                                        putExtra("EXTRA_SEQUEL_COVER", sequelNode.coverImage.medium)
                                        putExtra("EXTRA_SEQUEL_EPISODES", sequelNode.episodes ?: 0)
                                    }
                                }
                                context.startForegroundService(intent)
                            } else {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AnimeList(animeList: List<MediaListEntry>, onItemClick: (MediaListEntry) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(animeList) { entry ->
            AnimeItem(entry, onClick = { onItemClick(entry) })
        }
    }
}

@Composable
fun AnimeItem(entry: MediaListEntry, onClick: () -> Unit) {
    val progress = entry.progress
    val total = entry.media.episodes ?: 0
    val progressFraction = if (total > 0) progress.toFloat() / total.toFloat() else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurface)
            .border(1.dp, CardStroke, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Poster
        AsyncImage(
            model = entry.media.coverImage.medium,
            contentDescription = null,
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight(),
            contentScale = ContentScale.Crop
        )

        // Info Column
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Title & Ep Info
            Column {
                Text(
                    text = entry.media.title.userPreferred,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$progress / ${if (total > 0) total else "?"} Episodes",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = PrimaryAccent,
                trackColor = Color.DarkGray,
            )
        }

        IconButton(
            onClick = onClick,
            modifier = Modifier
                .padding(end = 12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Open overlay",
                tint = TextPrimary
            )
        }
    }
}

@Composable
private fun ProfileMenu(
    avatarUrl: String?,
    viewerName: String?,
    onProfile: () -> Unit,
    onLogout: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = !expanded }) {
        if (!avatarUrl.isNullOrBlank()) {
            Image(
                painter = rememberAsyncImagePainter(avatarUrl),
                contentDescription = "Profile",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                if (!viewerName.isNullOrBlank()) {
                    Text(
                        text = viewerName.first().uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White)
                }
            }
        }
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Profile") },
            onClick = {
                expanded = false
                onProfile()
            }
        )
        DropdownMenuItem(
            text = { Text("Logout") },
            onClick = {
                expanded = false
                onLogout()
            }
        )
    }
}

private fun openAniListProfile(context: android.content.Context, userId: Int) {
    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://anilist.co/user/$userId"))
    context.startActivity(intent)
}
