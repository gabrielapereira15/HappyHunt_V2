package com.example.happyhunt.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.happyhunt.R
import com.example.happyhunt.domain.Category
import com.example.happyhunt.domain.Kind
import com.example.happyhunt.ui.StatusLine
import com.example.happyhunt.ui.theme.Hunt
import com.example.happyhunt.ui.theme.HuntIcons

/** A soft square in the category's colour with the kind's icon: the face of a place in every list. */
@Composable
fun KindTile(kind: Kind, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val colors = Hunt.colors.category(kind.category)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(colors.soft),
        contentAlignment = Alignment.Center,
    ) {
        Icon(HuntIcons.forKind(kind), contentDescription = null, tint = colors.pin, modifier = Modifier.size(size * 0.5f))
    }
}

/** The same, for a category as a whole. */
@Composable
fun CategoryDot(category: Category, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    val colors = Hunt.colors.category(category)
    Box(modifier.size(size).clip(CircleShape).background(colors.pin), contentAlignment = Alignment.Center) {
        Icon(HuntIcons.forCategory(category), contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.6f))
    }
}

/** The heart: saves or unsaves, with a little bounce. */
@Composable
fun HeartButton(saved: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Hunt.colors.inkMuted) {
    val color by animateColorAsState(if (saved) Hunt.colors.heart else tint, label = "heart")
    val scale by animateFloatAsState(
        targetValue = if (saved) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        label = "heart-scale",
    )
    val description = stringResource(if (saved) R.string.place_unsave else R.string.place_save)
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = if (saved) HuntIcons.HeartFilled else HuntIcons.Heart,
            contentDescription = description,
            tint = color,
            modifier = Modifier.size(24.dp).scale(scale),
        )
    }
}

/** "Open · until 22:00" with a coloured dot. */
@Composable
fun StatusText(line: StatusLine, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(line.color))
        Spacer(Modifier.width(6.dp))
        Text(
            line.text,
            style = MaterialTheme.typography.labelMedium,
            color = line.color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The same as a pill, for the place screen. */
@Composable
fun StatusPill(line: StatusLine, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(CircleShape)
            .background(line.background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(line.color))
        Spacer(Modifier.width(7.dp))
        Text(line.text, style = MaterialTheme.typography.labelLarge, color = line.color)
    }
}

/** A friendly message where content would be: nothing found, offline, nothing saved. */
@Composable
fun MessageState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    tint: Color = Hunt.colors.primary,
    tileColor: Color = Hunt.colors.primarySoft,
    primaryAction: Pair<String, () -> Unit>? = null,
    secondaryAction: Pair<String, () -> Unit>? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(76.dp).clip(RoundedCornerShape(26.dp)).background(tileColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = Hunt.colors.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Hunt.colors.inkMuted, textAlign = TextAlign.Center)
        if (primaryAction != null || secondaryAction != null) Spacer(Modifier.height(20.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            primaryAction?.let { (label, action) ->
                Button(onClick = action, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
                    Text(label, fontWeight = FontWeight.Bold)
                }
            }
            secondaryAction?.let { (label, action) ->
                OutlinedButton(onClick = action) { Text(label, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

/** A tappable row role for screen readers on custom rows. */
fun Modifier.buttonRole(): Modifier = semantics { role = Role.Button }
