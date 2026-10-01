package com.example.happyhunt.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalLocale
import com.example.happyhunt.R
import com.example.happyhunt.domain.Category
import com.example.happyhunt.domain.Highlight
import com.example.happyhunt.domain.Kind
import com.example.happyhunt.domain.OpeningHours
import com.example.happyhunt.domain.Place
import com.example.happyhunt.ui.theme.Hunt
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

@StringRes
fun Category.label(): Int = when (this) {
    Category.EAT -> R.string.category_eat
    Category.TREATS -> R.string.category_treats
    Category.PARKS -> R.string.category_parks
    Category.PLAYGROUNDS -> R.string.category_playgrounds
    Category.ATTRACTIONS -> R.string.category_attractions
    Category.CULTURE -> R.string.category_culture
}

/** The category inside a sentence: "No playgrounds within 1 km." */
@StringRes
fun Category.inSentence(): Int = when (this) {
    Category.EAT -> R.string.category_eat_lower
    Category.TREATS -> R.string.category_treats_lower
    Category.PARKS -> R.string.category_parks_lower
    Category.PLAYGROUNDS -> R.string.category_playgrounds_lower
    Category.ATTRACTIONS -> R.string.category_attractions_lower
    Category.CULTURE -> R.string.category_culture_lower
}

@StringRes
fun Kind.label(): Int = when (this) {
    Kind.MUSEUM -> R.string.kind_museum
    Kind.GALLERY -> R.string.kind_gallery
    Kind.ZOO -> R.string.kind_zoo
    Kind.AQUARIUM -> R.string.kind_aquarium
    Kind.THEME_PARK -> R.string.kind_theme_park
    Kind.WATER_PARK -> R.string.kind_water_park
    Kind.PLAYGROUND -> R.string.kind_playground
    Kind.PARK -> R.string.kind_park
    Kind.GARDEN -> R.string.kind_garden
    Kind.NATURE_RESERVE -> R.string.kind_nature_reserve
    Kind.RESTAURANT -> R.string.kind_restaurant
    Kind.FAST_FOOD -> R.string.kind_fast_food
    Kind.FOOD_COURT -> R.string.kind_food_court
    Kind.CAFE -> R.string.kind_cafe
    Kind.ICE_CREAM -> R.string.kind_ice_cream
    Kind.BAKERY -> R.string.kind_bakery
    Kind.PASTRY -> R.string.kind_pastry
    Kind.SWEETS -> R.string.kind_sweets
    Kind.ARTS_CENTRE -> R.string.kind_arts_centre
    Kind.THEATRE -> R.string.kind_theatre
    Kind.CINEMA -> R.string.kind_cinema
    Kind.MINI_GOLF -> R.string.kind_mini_golf
    Kind.BOWLING -> R.string.kind_bowling
    Kind.ARCADE -> R.string.kind_arcade
    Kind.VIEWPOINT -> R.string.kind_viewpoint
    Kind.ATTRACTION -> R.string.kind_attraction
}

@StringRes
fun Highlight.label(): Int = when (this) {
    Highlight.WHEELCHAIR -> R.string.highlight_wheelchair
    Highlight.PARTLY_WHEELCHAIR -> R.string.highlight_partly_wheelchair
    Highlight.FREE -> R.string.highlight_free
    Highlight.OUTDOOR_SEATING -> R.string.highlight_outdoor_seating
    Highlight.VEGAN -> R.string.highlight_vegan
    Highlight.VEGETARIAN -> R.string.highlight_vegetarian
    Highlight.TAKEAWAY -> R.string.highlight_takeaway
    Highlight.DOGS -> R.string.highlight_dogs
    Highlight.CHANGING_TABLE -> R.string.highlight_changing_table
    Highlight.TOILETS -> R.string.highlight_toilets
    Highlight.WIFI -> R.string.highlight_wifi
}

/** A place's name, or what it is for the many playgrounds without one. */
@Composable
@ReadOnlyComposable
fun Place.title(): String = name ?: stringResource(kind.label())

/** "Café · Coffee shop": what the place is, and what it serves when it says. */
@Composable
@ReadOnlyComposable
fun Place.kindLine(): String = (listOf(stringResource(kind.label())) + cuisines).distinct().joinToString(" · ")

/** The line under the name in lists. A place without a name is already called by its kind, so its address goes there instead. */
@Composable
@ReadOnlyComposable
fun Place.subtitle(): String = if (name == null) address.orEmpty() else kindLine()

/** The open-or-closed line, and the colour it is shown in. */
data class StatusLine(val text: String, val color: Color, val background: Color)

@Composable
@ReadOnlyComposable
fun OpeningHours.Status.line(): StatusLine {
    val colors = Hunt.colors
    return when (this) {
        OpeningHours.Status.AlwaysOpen -> StatusLine(stringResource(R.string.status_open_24), colors.open, colors.openSoft)
        is OpeningHours.Status.Open -> when {
            closesAt == null -> StatusLine(stringResource(R.string.status_open), colors.open, colors.openSoft)
            closesSoon -> StatusLine(stringResource(R.string.status_closes_soon, clock(closesAt)), colors.soon, colors.soonSoft)
            else -> StatusLine(stringResource(R.string.status_open_until, clock(closesAt)), colors.open, colors.openSoft)
        }
        is OpeningHours.Status.Closed -> {
            val text = when {
                opensAt == null || opensOn == null -> stringResource(R.string.status_closed)
                opensToday -> stringResource(R.string.status_opens_today, clock(opensAt))
                else -> stringResource(R.string.status_opens_on, shortDay(opensOn), clock(opensAt))
            }
            StatusLine(text, colors.closed, colors.closedSoft)
        }
    }
}

/** "10:00 PM" or "22:00", as the phone's language writes times. */
@Composable
@ReadOnlyComposable
fun clock(time: LocalTime): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(LocalLocale.current.platformLocale).format(time)

@Composable
@ReadOnlyComposable
fun shortDay(day: DayOfWeek): String = day.getDisplayName(TextStyle.SHORT, LocalLocale.current.platformLocale)

@Composable
@ReadOnlyComposable
fun fullDay(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL, LocalLocale.current.platformLocale)
