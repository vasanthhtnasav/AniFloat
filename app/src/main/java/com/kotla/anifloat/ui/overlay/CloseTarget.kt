package com.kotla.anifloat.ui.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CloseTarget(isOver: Boolean) {
    val scale by animateFloatAsState(if (isOver) 1.5f else 1.0f, label = "scale")
    val backgroundColor = if (isOver) Color.Red.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f)
    val iconColor = Color.White

    Box(
        modifier = Modifier
            .size(60.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close Service",
            tint = iconColor,
            modifier = Modifier.size(30.dp)
        )
    }
}

