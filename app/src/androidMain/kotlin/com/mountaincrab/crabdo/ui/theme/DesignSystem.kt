package com.mountaincrab.crabdo.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Uppercase tracked "eyebrow" section label — matches the .ds-label /
 * .section-eyebrow style from the design system (11sp, weight 700, 0.10em
 * tracking, fg-muted). Use to introduce a list of cards or a section in
 * a screen (e.g. "UPCOMING", "ABOUT", "APPEARANCE").
 */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
        ),
        color = color,
        modifier = modifier,
    )
}

/**
 * Equal-width pill button used inside a PillGroup. When selected: accent
 * background, white-ish foreground. When not: surface-high background, muted
 * foreground. Matches the .pillgroup button pattern from the design system.
 */
@Composable
fun RowScope.PillButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    text: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalAppPalette.current
    val bg = if (selected) MaterialTheme.colorScheme.primary else palette.surfaceHigh
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary
             else MaterialTheme.colorScheme.onSurfaceVariant
    val border = if (selected) MaterialTheme.colorScheme.primary
                 else MaterialTheme.colorScheme.outline
    Surface(
        modifier = modifier.weight(1f),
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = fg,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

/** Convenience row container for a horizontal pill group with 8dp spacing. */
@Composable
fun PillGroup(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}
