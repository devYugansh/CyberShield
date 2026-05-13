package com.nielit.cybershield.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nielit.cybershield.ui.theme.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

// ─────────────────────────────────────────────────────────────────────────────
// CsTopBar  –  Hamburger variant (Home) & Back-arrow variant (Settings)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsTopBar(
    modifier       : Modifier = Modifier,
    title          : String,
    navigationIcon : @Composable () -> Unit = {},
    actions        : @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text  = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = navigationIcon,
        actions        = actions,
        colors         = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor   = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor     = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        windowInsets = TopAppBarDefaults.windowInsets,
        modifier = modifier
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// CsPrimaryButton  –  Full-width CTA with loading state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CsPrimaryButton(
    text     : String,
    onClick  : () -> Unit,
    modifier : Modifier = Modifier,
    enabled  : Boolean  = true,
    isLoading: Boolean  = false
) {
    Button(
        onClick  = onClick,
        enabled  = enabled && !isLoading,
        shape    = MaterialTheme.shapes.medium,
        colors   = ButtonDefaults.buttonColors(
            containerColor         = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color     = MaterialTheme.colorScheme.onPrimary,
                modifier  = Modifier.size(22.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text  = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CsErrorText  –  Inline red error message
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CsErrorText(
    message : String,
    modifier: Modifier = Modifier
) {
    if (message.isNotBlank()) {
        Text(
            text     = message,
            color    = MaterialTheme.colorScheme.error,
            style    = MaterialTheme.typography.bodySmall,
            modifier = modifier.padding(top = 4.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ProgressRing  –  Custom Canvas arc showing overall completion %
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProgressRing(
    percentage   : Float,          // 0f..100f
    modifier     : Modifier = Modifier,
    size         : Dp       = 120.dp,
    strokeWidth  : Dp       = 12.dp,
    trackColor   : Color?   = null,
    progressColor: Color?   = null
) {
    val actualTrackColor = trackColor ?: MaterialTheme.colorScheme.outline
    val actualProgressColor = progressColor ?: MaterialTheme.colorScheme.primary

    val animatedSweep by animateFloatAsState(
        targetValue   = (percentage / 100f) * 360f,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label         = "progressRingAnimation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier         = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(
                width = strokeWidth.toPx(),
                cap   = StrokeCap.Round
            )
            val arcSize = Size(
                width  = this.size.width  - strokeWidth.toPx(),
                height = this.size.height - strokeWidth.toPx()
            )
            val topLeft = Offset(strokeWidth.toPx() / 2f, strokeWidth.toPx() / 2f)

            // Track
            drawArc(color = actualTrackColor, startAngle = 0f, sweepAngle = 360f,
                useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
            // Progress
            drawArc(color = actualProgressColor, startAngle = -90f, sweepAngle = animatedSweep,
                useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = "${percentage.toInt()}%",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = "Complete",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ProgressDashBar  –  N-segment horizontal bar for Flashcard Viewer
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProgressDashBar(
    total      : Int,
    current    : Int,      // 0-indexed current page
    modifier   : Modifier = Modifier,
    segmentGap : Dp       = 4.dp
) {
    val filledColor = MaterialTheme.colorScheme.primary
    val currentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val pendingColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
    ) {
        if (total <= 0) return@Canvas
        val gap      = segmentGap.toPx()
        val totalGaps = gap * (total - 1)
        val segWidth = (size.width - totalGaps) / total

        repeat(total) { i ->
            val color = when {
                i < current  -> filledColor
                i == current -> currentColor
                else         -> pendingColor
            }
            drawRect(
                color   = color,
                topLeft = Offset(x = i * (segWidth + gap), y = 0f),
                size    = Size(segWidth, size.height)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AvatarCircle  –  Initials-based avatar for Side Drawer
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AvatarCircle(
    initials : String,
    modifier : Modifier = Modifier,
    size     : Dp       = 48.dp
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier         = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondary)
    ) {
        Text(
            text  = initials.uppercase(),
            color = MaterialTheme.colorScheme.onSecondary,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SectionHeader  –  Muted upper-case section label
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    text    : String,
    modifier: Modifier = Modifier
) {
    Text(
        text  = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            color  = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// DrawerNavItem  –  Icon + label + trailing chevron row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DrawerNavItem(
    icon     : ImageVector,
    label    : String,
    onClick  : () -> Unit,
    modifier : Modifier = Modifier,
    trailing : @Composable (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ToggleRow  –  Label + sublabel + Switch for Settings screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ToggleRow(
    label    : String,
    sublabel : String,
    checked  : Boolean,
    onChecked: (Boolean) -> Unit,
    modifier : Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label,    style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = sublabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onChecked,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor   = MaterialTheme.colorScheme.primary,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LoadingDots  –  3-dot pulsating animation for Splash screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LoadingDots(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loadingDots")
    val dotCount = 3

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        modifier              = modifier
    ) {
        repeat(dotCount) { i ->
            val alpha by infiniteTransition.animateFloat(
                initialValue   = 0.25f,
                targetValue    = 1f,
                animationSpec  = infiniteRepeatable(
                    animation  = tween(600, delayMillis = i * 200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$i"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// InfoRow  –  read-only label / value pair
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun InfoRow(
    label   : String,
    value   : String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.titleMedium,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text  = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ConfirmationDialog  –  Generic destructive action confirmation
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ConfirmationDialog(
    title      : String,
    message    : String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    onConfirm  : () -> Unit,
    onDismiss  : () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text  = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText, color = MaterialTheme.colorScheme.primary)
            }
        },
        shape = MaterialTheme.shapes.medium
    )
}
