package com.fmusic.app.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Main : Screen("main")
    object Home : Screen("home")
    object Search : Screen("search")
    object Charts : Screen("charts")
    object Library : Screen("library")
    object Lyrics : Screen("lyrics")

    // Dynamic routes
    object BrowseDetail : Screen("browse_detail/{browseId}?title={title}&type={type}") {
        fun createRoute(browseId: String, title: String? = null, type: String? = null): String {
            val encodedTitle = java.net.URLEncoder.encode(title ?: "", "UTF-8")
            val encodedType = java.net.URLEncoder.encode(type ?: "playlist", "UTF-8")
            return "browse_detail/$browseId?title=$encodedTitle&type=$encodedType"
        }
    }
}
