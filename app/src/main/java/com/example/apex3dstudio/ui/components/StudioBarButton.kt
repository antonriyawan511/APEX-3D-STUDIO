package com.example.apex3dstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apex3dstudio.ui.theme.AccentCyan

@Composable
fun StudioBarButton(
    label: String,
    icon: ImageVector,
    isActive: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = AccentCyan
) {
    val contentColor = when {
        !enabled -> Color.White.copy(alpha = 0.35f)
        isActive -> activeColor
        else -> Color.White.copy(alpha = 0.85f)
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 22.dp),
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.95f),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 4f
                    )
                ),
                color = contentColor
            )

            if (isActive && enabled) {
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .size(width = 14.dp, height = 2.5.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(activeColor)
                )
            } else {
                Spacer(modifier = Modifier.height(5.5.dp))
            }
        }
    }
}
