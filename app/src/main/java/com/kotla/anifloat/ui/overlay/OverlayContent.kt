package com.kotla.anifloat.ui.overlay

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.draw.shadow

@Composable
fun OverlayContent(
    title: String,
    currentProgress: Int,
    totalEpisodes: Int,
    coverImage: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    isCollapsed: Boolean,
    onExpand: () -> Unit,
    isBlurSupported: Boolean = true,
    showAddSequelButton: Boolean = false,
    onAddSequel: () -> Unit = {}
) {
    if (isCollapsed) {
        CollapsedOverlay(onExpand = onExpand)
    } else {
        ExpandedOverlay(
            title = title,
            progress = currentProgress,
            total = totalEpisodes,
            coverImage = coverImage,
            onIncrement = onIncrement,
            onDecrement = onDecrement,
            onMinimize = onMinimize,
            onClose = onClose,
            isBlurSupported = isBlurSupported,
            showAddSequelButton = showAddSequelButton,
            onAddSequel = onAddSequel
        )
    }
}

@Composable
fun CollapsedOverlay(onExpand: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0xFF1F1F1F).copy(alpha = 0.8f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            .clickable { onExpand() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = "Expand",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpandedOverlay(
    title: String,
    progress: Int,
    total: Int,
    coverImage: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    isBlurSupported: Boolean,
    showAddSequelButton: Boolean,
    onAddSequel: () -> Unit
) {
    val containerAlpha = if (isBlurSupported) 0.5f else 0.9f
    
    val glassBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.3f),
            Color.White.copy(alpha = 0.05f)
        )
    )

    Card(
        modifier = Modifier
            .width(220.dp) // Slightly wider to fit poster + controls
            .wrapContentHeight()
            .border(1.dp, glassBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF000000).copy(alpha = containerAlpha)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Minimize Icon + "Tracker"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Minimize",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onMinimize() }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Tracker",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Content Row: Poster + (Title/Progress/Controls)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Poster
                if (coverImage.isNotEmpty()) {
                    AsyncImage(
                        model = coverImage,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(60.dp)
                            .height(90.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                // Title + Controls Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Marquee Title
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(
                                animationMode = MarqueeAnimationMode.Immediately,
                                delayMillis = 1000
                            )
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Progress
                    Text(
                        text = "$progress / ${if (total > 0) total else "?"} Episodes",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Compact Controls (Beside Poster effectively)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Minus Button
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { onDecrement() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "-",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Light,
                                color = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))

                        // Plus Button
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { onIncrement() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Increment",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (showAddSequelButton) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Row(
                                modifier = Modifier
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF2AF598),
                                                Color(0xFF009EFD)
                                            )
                                        )
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(50))
                                    .clickable { onAddSequel() }
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Add",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
