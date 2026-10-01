package com.example.happyhunt.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.happyhunt.domain.Distances
import com.example.happyhunt.domain.OpeningHours
import com.example.happyhunt.domain.Place
import com.example.happyhunt.domain.Units
import com.example.happyhunt.ui.line
import com.example.happyhunt.ui.subtitle
import com.example.happyhunt.ui.theme.Hunt
import com.example.happyhunt.ui.title

/**
 * One place in a list: what it is, whether it is open, how far it is, and a
 * heart to keep it. The whole row opens the place.
 */
@Composable
fun PlaceRow(
    place: Place,
    status: OpeningHours.Status?,
    distanceMeters: Double?,
    units: Units,
    saved: Boolean,
    onClick: () -> Unit,
    onToggleSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KindTile(place.kind)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                place.title(),
                style = MaterialTheme.typography.titleMedium,
                color = Hunt.colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = place.subtitle()
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Hunt.colors.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (status != null) {
                    StatusText(status.line(), Modifier.weight(1f, fill = false))
                }
                if (distanceMeters != null) {
                    Text(
                        // Just the distance here; the walk time is on the card and the place screen.
                        text = (if (status != null) "  ·  " else "") +
                            Distances.format(distanceMeters, units, LocalLocale.current.platformLocale),
                        style = MaterialTheme.typography.labelMedium,
                        color = Hunt.colors.inkMuted,
                        maxLines = 1,
                    )
                }
            }
        }
        HeartButton(saved = saved, onClick = onToggleSaved)
    }
}
