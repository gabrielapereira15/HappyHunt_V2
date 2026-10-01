package com.example.happyhunt.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import com.example.happyhunt.domain.Category
import com.example.happyhunt.domain.Highlight
import com.example.happyhunt.domain.Kind

/**
 * The app's icons: Lucide (ISC licence, see app/licenses), rounded 2px strokes
 * on a 24-unit grid, kept as SVG path data. `Icon(HuntIcons.Heart, ...)` tints
 * them like any Material icon; the map draws its markers with the same ones.
 */
object HuntIcons {
    val Explore by icon(
        "M14.106 5.553a2 2 0 0 0 1.788 0l3.659-1.83A1 1 0 0 1 21 4.619v12.764a1 1 0 0 1-.553.894l-4.553 2.277a2 2 0 0 1-1.788 0l-4.212-2.106a2 2 0 0 0-1.788 0l-3.659 1.83A1 1 0 0 1 3 19.381V6.618a1 1 0 0 1 .553-.894l4.553-2.277a2 2 0 0 1 1.788 0z",
        "M15 5.764v15",
        "M9 3.236v15",
    )
    val Heart by icon(
        "M2 9.5a5.5 5.5 0 0 1 9.591-3.676.56.56 0 0 0 .818 0A5.49 5.49 0 0 1 22 9.5c0 2.29-1.5 4-3 5.5l-5.492 5.313a2 2 0 0 1-3 .019L5 15c-1.5-1.5-3-3.2-3-5.5",
    )
    val Settings by icon(
        "M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915",
        "M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
    )
    val Search by icon("m21 21-4.34-4.34", "M3 11a8 8 0 1 0 16 0a8 8 0 1 0 -16 0")
    val Close by icon("M18 6 6 18", "m6 6 12 12")
    val Back by icon("m15 18-6-6 6-6")
    val ChevronRight by icon("m9 18 6-6-6-6")
    val ChevronDown by icon("m6 9 6 6 6-6")
    val Locate by icon(
        "M2 12L5 12",
        "M19 12L22 12",
        "M12 2L12 5",
        "M12 19L12 22",
        "M5 12a7 7 0 1 0 14 0a7 7 0 1 0 -14 0",
        "M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
    )
    val Pin by icon(
        "M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0",
        "M9 10a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
    )
    val Directions by icon("M12 2L19 21L12 17L5 21L12 2z")
    val Phone by icon(
        "M13.832 16.568a1 1 0 0 0 1.213-.303l.355-.465A2 2 0 0 1 17 15h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2A18 18 0 0 1 2 4a2 2 0 0 1 2-2h3a2 2 0 0 1 2 2v3a2 2 0 0 1-.8 1.6l-.468.351a1 1 0 0 0-.292 1.233 14 14 0 0 0 6.392 6.384",
    )
    val Globe by icon("M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0", "M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20", "M2 12h20")
    val Share by icon(
        "M15 5a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
        "M3 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
        "M15 19a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
        "M8.59 13.51L15.42 17.49",
        "M15.41 6.51L8.59 10.49",
    )
    val Clock by icon("M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0", "M12 6v6l4 2")
    val Info by icon("M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0", "M12 16v-4", "M12 8h.01")
    val Walk by icon(
        "M4 16v-2.38C4 11.5 2.97 10.5 3 8c.03-2.72 1.49-6 4.5-6C9.37 2 10 3.8 10 5.5c0 3.11-2 5.66-2 8.68V16a2 2 0 1 1-4 0Z",
        "M20 20v-2.38c0-2.12 1.03-3.12 1-5.62-.03-2.72-1.49-6-4.5-6C14.63 6 14 7.8 14 9.5c0 3.11 2 5.66 2 8.68V20a2 2 0 1 0 4 0Z",
        "M16 17h4",
        "M4 13h4",
    )
    val Refresh by icon(
        "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8",
        "M21 3v5h-5",
        "M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16",
        "M8 16H3v5",
    )
    val Sun by icon(
        "M8 12a4 4 0 1 0 8 0a4 4 0 1 0 -8 0",
        "M12 2v2",
        "M12 20v2",
        "m4.93 4.93 1.41 1.41",
        "m17.66 17.66 1.41 1.41",
        "M2 12h2",
        "M20 12h2",
        "m6.34 17.66-1.41 1.41",
        "m19.07 4.93-1.41 1.41",
    )
    val Moon by icon("M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401")
    val SunMoon by icon(
        "M12 2v2",
        "M14.837 16.385a6 6 0 1 1-7.223-7.222c.624-.147.97.66.715 1.248a4 4 0 0 0 5.26 5.259c.589-.255 1.396.09 1.248.715",
        "M16 12a4 4 0 0 0-4-4",
        "m19 5-1.256 1.256",
        "M20 12h2",
    )
    val Ruler by icon(
        "M21.3 15.3a2.4 2.4 0 0 1 0 3.4l-2.6 2.6a2.4 2.4 0 0 1-3.4 0L2.7 8.7a2.41 2.41 0 0 1 0-3.4l2.6-2.6a2.41 2.41 0 0 1 3.4 0Z",
        "m14.5 12.5 2-2",
        "m11.5 9.5 2-2",
        "m8.5 6.5 2-2",
        "m17.5 15.5 2-2",
    )
    val Trash by icon("M10 11v6", "M14 11v6", "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6", "M3 6h18", "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2")
    val Shield by icon(
        "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z",
        "m9 12 2 2 4-4",
    )
    val External by icon("M15 3h6v6", "M10 14 21 3", "M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6")
    val Offline by icon(
        "M10.94 5.274A7 7 0 0 1 15.71 10h1.79a4.5 4.5 0 0 1 4.222 6.057",
        "M18.796 18.81A4.5 4.5 0 0 1 17.5 19H9A7 7 0 0 1 5.79 5.78",
        "m2 2 20 20",
    )
    val History by icon("M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8", "M3 3v5h5", "M12 7v5l4 2")
    val Sparkles by icon(
        "M11.017 2.814a1 1 0 0 1 1.966 0l1.051 5.558a2 2 0 0 0 1.594 1.594l5.558 1.051a1 1 0 0 1 0 1.966l-5.558 1.051a2 2 0 0 0-1.594 1.594l-1.051 5.558a1 1 0 0 1-1.966 0l-1.051-5.558a2 2 0 0 0-1.594-1.594l-5.558-1.051a1 1 0 0 1 0-1.966l5.558-1.051a2 2 0 0 0 1.594-1.594z",
        "M20 2v4",
        "M22 4h-4",
        "M2 20a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
    )
    val Compass by icon(
        "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0",
        "m16.24 7.76-1.804 5.411a2 2 0 0 1-1.265 1.265L7.76 16.24l1.804-5.411a2 2 0 0 1 1.265-1.265z",
    )
    val Copy by icon(
        "M10 8h10a2 2 0 0 1 2 2v10a2 2 0 0 1 -2 2h-10a2 2 0 0 1 -2 -2v-10a2 2 0 0 1 2 -2z",
        "M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2",
    )
    val Accessible by icon(
        "M15 4a1 1 0 1 0 2 0a1 1 0 1 0 -2 0",
        "m18 19 1-7-6 1",
        "m5 8 3-3 5.5 3-2.36 3.5",
        "M4.24 14.5a5 5 0 0 0 6.88 6",
        "M13.76 17.5a5 5 0 0 0-6.88-6",
    )
    val Ticket by icon(
        "M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z",
        "M13 5v2",
        "M13 17v2",
        "M13 11v2",
    )
    val Umbrella by icon("M12 13v7a2 2 0 0 0 4 0", "M12 2v2", "M20.992 13a1 1 0 0 0 .97-1.274 10.284 10.284 0 0 0-19.923 0A1 1 0 0 0 3 13z")
    val Vegan by icon("M16 8q6 0 6-6-6 0-6 6", "M17.41 3.59a10 10 0 1 0 3 3", "M2 2a26.6 26.6 0 0 1 10 20c.9-6.82 1.5-9.5 4-14")
    val Leaf by icon(
        "M11 20a10 10 0 0010-10 25.9 25.9 0 00-1.04-7.281 1 1 0 00-1.755-.325C15.833 5.5 13 5.5 9.8 6.1A7 7 0 0011 20",
        "M2 21a5 5 0 012.911-4.544C7.613 15.212 8.351 15.24 11 13",
    )
    val Bag by icon(
        "M16 10a4 4 0 0 1-8 0",
        "M3.103 6.034h17.794",
        "M3.4 5.467a2 2 0 0 0-.4 1.2V20a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6.667a2 2 0 0 0-.4-1.2l-2-2.667A2 2 0 0 0 17 2H7a2 2 0 0 0-1.6.8z",
    )
    val Dog by icon(
        "M11.25 16.25h1.5L12 17z",
        "M16 14v.5",
        "M4.42 11.247A13.152 13.152 0 0 0 4 14.556C4 18.728 7.582 21 12 21s8-2.272 8-6.444a11.702 11.702 0 0 0-.493-3.309",
        "M8 14v.5",
        "M8.5 8.5c-.384 1.05-1.083 2.028-2.344 2.5-1.931.722-3.576-.297-3.656-1-.113-.994 1.177-6.53 4-7 1.923-.321 3.651.845 3.651 2.235A7.497 7.497 0 0 1 14 5.277c0-1.39 1.844-2.598 3.767-2.277 2.823.47 4.113 6.006 4 7-.08.703-1.725 1.722-3.656 1-1.261-.472-1.855-1.45-2.239-2.5",
    )
    val Baby by icon(
        "M10 16c.5.3 1.2.5 2 .5s1.5-.2 2-.5",
        "M15 12h.01",
        "M19.38 6.813A9 9 0 0 1 20.8 10.2a2 2 0 0 1 0 3.6 9 9 0 0 1-17.6 0 2 2 0 0 1 0-3.6A9 9 0 0 1 12 3c2 0 3.5 1.1 3.5 2.5s-.9 2.5-2 2.5c-.8 0-1.5-.4-1.5-1",
        "M9 12h.01",
    )
    val Toilet by icon(
        "M7 12h13a1 1 0 0 1 1 1 5 5 0 0 1-5 5h-.598a.5.5 0 0 0-.424.765l1.544 2.47a.5.5 0 0 1-.424.765H5.402a.5.5 0 0 1-.424-.765L7 18",
        "M8 18a5 5 0 0 1-5-5V4a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v8",
    )
    val Wifi by icon("M12 20h.01", "M2 8.82a15 15 0 0 1 20 0", "M5 12.859a10 10 0 0 1 14 0", "M8.5 16.429a5 5 0 0 1 7 0")
    val Utensils by icon("M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2", "M7 2v20", "M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3Zm0 0v7")
    val Sandwich by icon(
        "M12 16H4a2 2 0 1 1 0-4h16a2 2 0 1 1 0 4h-4.25",
        "M5 12a2 2 0 0 1-2-2 9 7 0 0 1 18 0 2 2 0 0 1-2 2",
        "M5 16a2 2 0 0 0-2 2 3 3 0 0 0 3 3h12a3 3 0 0 0 3-3 2 2 0 0 0-2-2q0 0 0 0",
        "m6.67 12 6.13 4.6a2 2 0 0 0 2.8-.4l3.15-4.2",
    )
    val FoodCourt by icon(
        "m16 2-2.3 2.3a3 3 0 0 0 0 4.2l1.8 1.8a3 3 0 0 0 4.2 0L22 8",
        "M15 15 3.3 3.3a4.2 4.2 0 0 0 0 6l7.3 7.3c.7.7 2 .7 2.8 0L15 15Zm0 0 7 7",
        "m2.1 21.8 6.4-6.3",
        "m19 5-7 7",
    )
    val Coffee by icon("M10 2v2", "M14 2v2", "M16 8a1 1 0 0 1 1 1v8a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V9a1 1 0 0 1 1-1h14a4 4 0 1 1 0 8h-1", "M6 2v2")
    val IceCream by icon("m7 11 4.08 10.35a1 1 0 0 0 1.84 0L17 11", "M17 7A5 5 0 0 0 7 7", "M17 7a2 2 0 0 1 0 4H7a2 2 0 0 1 0-4")
    val Croissant by icon(
        "M10.2 18H4.774a1.5 1.5 0 0 1-1.352-.97 11 11 0 0 1 .132-6.487",
        "M18 10.2V4.774a1.5 1.5 0 0 0-.97-1.352 11 11 0 0 0-6.486.132",
        "M18 5a4 3 0 0 1 4 3 2 2 0 0 1-2 2 10 10 0 0 0-5.139 1.42",
        "M5 18a3 4 0 0 0 3 4 2 2 0 0 0 2-2 10 10 0 0 1 1.42-5.14",
        "M8.709 2.554a10 10 0 0 0-6.155 6.155 1.5 1.5 0 0 0 .676 1.626l9.807 5.42a2 2 0 0 0 2.718-2.718l-5.42-9.807a1.5 1.5 0 0 0-1.626-.676",
    )
    val Cake by icon(
        "M16 13H3",
        "M16 17H3",
        "m7.2 7.9-3.388 2.5A2 2 0 0 0 3 12.01V20a1 1 0 0 0 1 1h16a1 1 0 0 0 1-1v-8.654c0-2-2.44-6.026-6.44-8.026a1 1 0 0 0-1.082.057L10.4 5.6",
        "M7 7a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
    )
    val Candy by icon(
        "M10 7v10.9",
        "M14 6.1V17",
        "M16 7V3a1 1 0 0 1 1.707-.707 2.5 2.5 0 0 0 2.152.717 1 1 0 0 1 1.131 1.131 2.5 2.5 0 0 0 .717 2.152A1 1 0 0 1 21 8h-4",
        "M16.536 7.465a5 5 0 0 0-7.072 0l-2 2a5 5 0 0 0 0 7.07 5 5 0 0 0 7.072 0l2-2a5 5 0 0 0 0-7.07",
        "M8 17v4a1 1 0 0 1-1.707.707 2.5 2.5 0 0 0-2.152-.717 1 1 0 0 1-1.131-1.131 2.5 2.5 0 0 0-.717-2.152A1 1 0 0 1 3 16h4",
    )
    val Trees by icon(
        "M10 10v.2A3 3 0 0 1 8.9 16H5a3 3 0 0 1-1-5.8V10a3 3 0 0 1 6 0Z",
        "M7 16v6",
        "M13 19v3",
        "M12 19h8.3a1 1 0 0 0 .7-1.7L18 14h.3a1 1 0 0 0 .7-1.7L16 9h.2a1 1 0 0 0 .8-1.7L13 3l-1.4 1.5",
    )
    val Flower by icon(
        "M12 5a3 3 0 1 1 3 3m-3-3a3 3 0 1 0-3 3m3-3v1M9 8a3 3 0 1 0 3 3M9 8h1m5 0a3 3 0 1 1-3 3m3-3h-1m-2 3v-1",
        "M10 8a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
        "M12 10v12",
        "M12 22c4.2 0 7-1.667 7-5-4.2 0-7 1.667-7 5Z",
        "M12 22c-4.2 0-7-1.667-7-5 4.2 0 7 1.667 7 5Z",
    )
    val Mountain by icon("m8 3 4 8 5-5 5 15H2L8 3z")
    val Landmark by icon(
        "M10 18v-7",
        "M11.119 2.205a2 2 0 0 1 1.762 0l7.84 3.846A.5.5 0 0 1 20.5 7h-17a.5.5 0 0 1-.22-.949z",
        "M14 18v-7",
        "M18 18v-7",
        "M3 22h18",
        "M6 18v-7",
    )
    val Palette by icon(
        "M12 22a1 1 0 0 1 0-20 10 9 0 0 1 10 9 5 5 0 0 1-5 5h-2.25a1.75 1.75 0 0 0-1.4 2.8l.3.4a1.75 1.75 0 0 1-1.4 2.8z",
        "M13 6.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0",
        "M17 10.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0",
        "M6 12.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0",
        "M8 7.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0",
    )
    val Frame by icon("M22 6L2 6", "M22 18L2 18", "M6 2L6 22", "M18 2L18 22")
    val Drama by icon(
        "M10 11h.01",
        "M14 6h.01",
        "M18 6h.01",
        "M6.5 13.1h.01",
        "M22 5c0 9-4 12-6 12s-6-3-6-12c0-2 2-3 6-3s6 1 6 3",
        "M17.4 9.9c-.8.8-2 .8-2.8 0",
        "M10.1 7.1C9 7.2 7.7 7.7 6 8.6c-3.5 2-4.7 3.9-3.7 5.6 4.5 7.8 9.5 8.4 11.2 7.4.9-.5 1.9-2.1 1.9-4.7",
        "M9.1 16.5c.3-1.1 1.4-1.7 2.4-1.4",
    )
    val Film by icon(
        "m12.296 3.464 3.02 3.956",
        "M20.2 6 3 11l-.9-2.4c-.3-1.1.3-2.2 1.3-2.5l13.5-4c1.1-.3 2.2.3 2.5 1.3z",
        "M3 11h18v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z",
        "m6.18 5.276 3.1 3.899",
    )
    val Fish by icon(
        "M6.5 12c.94-3.46 4.94-6 8.5-6 3.56 0 6.06 2.54 7 6-.94 3.47-3.44 6-7 6s-7.56-2.53-8.5-6Z",
        "M18 12v.5",
        "M16 17.93a9.77 9.77 0 0 1 0-11.86",
        "M7 10.67C7 8 5.58 5.97 2.73 5.5c-1 1.5-1 5 .23 6.5-1.24 1.5-1.24 5-.23 6.5C5.58 18.03 7 16 7 13.33",
        "M10.46 7.26C10.2 5.88 9.17 4.24 8 3h5.8a2 2 0 0 1 1.98 1.67l.23 1.4",
        "m16.01 17.93-.23 1.4A2 2 0 0 1 13.8 21H9.5a5.96 5.96 0 0 0 1.49-3.98",
    )
    val Rabbit by icon(
        "M13 16a3 3 0 0 1 2.24 5",
        "M18 12h.01",
        "M18 21h-8a4 4 0 0 1-4-4 7 7 0 0 1 7-7h.2L9.6 6.4a1 1 0 1 1 2.8-2.8L15.8 7h.2c3.3 0 6 2.7 6 6v1a2 2 0 0 1-2 2h-1a3 3 0 0 0-3 3",
        "M20 8.54V4a2 2 0 1 0-4 0v3",
        "M7.612 12.524a3 3 0 1 0-1.6 4.3",
    )
    val FerrisWheel by icon(
        "M10 12a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
        "M12 2v4",
        "m6.8 15-3.5 2",
        "m20.7 7-3.5 2",
        "M6.8 9 3.3 7",
        "m20.7 17-3.5-2",
        "m9 22 3-8 3 8",
        "M8 22h8",
        "M18 18.7a9 9 0 1 0-12 0",
    )
    val Waves by icon("M2 12q2.5 2 5 0t5 0 5 0 5 0", "M2 19q2.5 2 5 0t5 0 5 0 5 0", "M2 5q2.5 2 5 0t5 0 5 0 5 0")
    val Flag by icon("M6 22V2.8a.8.8 0 0 1 1.17-.71l11.38 5.69a.8.8 0 0 1 0 1.44L6 15.5")
    val Target by icon("M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0", "M6 12a6 6 0 1 0 12 0a6 6 0 1 0 -12 0", "M10 12a2 2 0 1 0 4 0a2 2 0 1 0 -4 0")
    val Gamepad by icon(
        "M6 11L10 11",
        "M8 9L8 13",
        "M15 12L15.01 12",
        "M18 10L18.01 10",
        "M17.32 5H6.68a4 4 0 0 0-3.978 3.59c-.006.052-.01.101-.017.152C2.604 9.416 2 14.456 2 16a3 3 0 0 0 3 3c1 0 1.5-.5 2-1l1.414-1.414A2 2 0 0 1 9.828 16h4.344a2 2 0 0 1 1.414.586L17 18c.5.5 1 1 2 1a3 3 0 0 0 3-3c0-1.545-.604-6.584-.685-7.258-.007-.05-.011-.1-.017-.151A4 4 0 0 0 17.32 5z",
    )
    val Binoculars by icon(
        "M10 10h4",
        "M19 7V4a1 1 0 0 0-1-1h-2a1 1 0 0 0-1 1v3",
        "M20 21a2 2 0 0 0 2-2v-3.851c0-1.39-2-2.962-2-4.829V8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v11a2 2 0 0 0 2 2z",
        "M 22 16 L 2 16",
        "M4 21a2 2 0 0 1-2-2v-3.851c0-1.39 2-2.962 2-4.829V8a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v11a2 2 0 0 1-2 2z",
        "M9 7V4a1 1 0 0 0-1-1H6a1 1 0 0 0-1 1v3",
    )
    val Star by icon(
        "M11.017 2.814a1 1 0 0 1 1.966 0l1.051 5.558a2 2 0 0 0 1.594 1.594l5.558 1.051a1 1 0 0 1 0 1.966l-5.558 1.051a2 2 0 0 0-1.594 1.594l-1.051 5.558a1 1 0 0 1-1.966 0l-1.051-5.558a2 2 0 0 0-1.594-1.594l-5.558-1.051a1 1 0 0 1 0-1.966l5.558-1.051a2 2 0 0 0 1.594-1.594z",
        "M20 2v4",
        "M22 4h-4",
        "M2 20a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
    )

    /** Lucide has no playground, so this one is drawn in the same style: a little slide. */
    val Slide by icon(
        "M5 21V8", "M10 21V8", "M5 12.5h5", "M5 17h5", "M3.5 8 7.5 4l4 4",
        "M10 8c4 0 5.5 3.5 6.5 7s2 6 4.5 6",
    )

    /** A filled heart, for places already saved. */
    val HeartFilled by lazy(LazyThreadSafetyMode.NONE) {
        ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            addPath(
                pathData = addPathNodes(HEART),
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }.build()
    }

    fun forCategory(category: Category): ImageVector = when (category) {
        Category.EAT -> Utensils
        Category.TREATS -> IceCream
        Category.PARKS -> Trees
        Category.PLAYGROUNDS -> Slide
        Category.ATTRACTIONS -> FerrisWheel
        Category.CULTURE -> Landmark
    }

    fun forKind(kind: Kind): ImageVector = when (kind) {
        Kind.MUSEUM -> Landmark
        Kind.GALLERY -> Frame
        Kind.ARTS_CENTRE -> Palette
        Kind.THEATRE -> Drama
        Kind.CINEMA -> Film
        Kind.ZOO -> Rabbit
        Kind.AQUARIUM -> Fish
        Kind.THEME_PARK -> FerrisWheel
        Kind.WATER_PARK -> Waves
        Kind.PLAYGROUND -> Slide
        Kind.PARK -> Trees
        Kind.GARDEN -> Flower
        Kind.NATURE_RESERVE -> Mountain
        Kind.RESTAURANT -> Utensils
        Kind.FAST_FOOD -> Sandwich
        Kind.FOOD_COURT -> FoodCourt
        Kind.CAFE -> Coffee
        Kind.ICE_CREAM -> IceCream
        Kind.BAKERY -> Croissant
        Kind.PASTRY -> Cake
        Kind.SWEETS -> Candy
        Kind.MINI_GOLF -> Flag
        Kind.BOWLING -> Target
        Kind.ARCADE -> Gamepad
        Kind.VIEWPOINT -> Binoculars
        Kind.ATTRACTION -> Star
    }

    fun forHighlight(highlight: Highlight): ImageVector = when (highlight) {
        Highlight.WHEELCHAIR, Highlight.PARTLY_WHEELCHAIR -> Accessible
        Highlight.FREE -> Ticket
        Highlight.OUTDOOR_SEATING -> Umbrella
        Highlight.VEGAN -> Vegan
        Highlight.VEGETARIAN -> Leaf
        Highlight.TAKEAWAY -> Bag
        Highlight.DOGS -> Dog
        Highlight.CHANGING_TABLE -> Baby
        Highlight.TOILETS -> Toilet
        Highlight.WIFI -> Wifi
    }
}

private const val HEART =
    "M2 9.5a5.5 5.5 0 0 1 9.591-3.676.56.56 0 0 0 .818 0A5.49 5.49 0 0 1 22 9.5c0 2.29-1.5 4-3 5.5l-5.492 5.313a2 2 0 0 1-3 .019L5 15c-1.5-1.5-3-3.2-3-5.5"

/** Builds the icon the first time it is used, then keeps it. */
private fun icon(vararg paths: String) = lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        paths.forEach { data ->
            addPath(
                pathData = addPathNodes(data),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
}
