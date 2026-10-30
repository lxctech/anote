package com.anote.ui.navigation

object Routes {
    const val LIST = "diary"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{entryId}"
    const val EDITOR = "editor?entryId={entryId}&dateEpoch={dateEpoch}"

    fun detail(id: Long) = "detail/$id"

    fun editor(entryId: Long? = null, dateEpoch: Long? = null): String {
        val id = entryId ?: -1L
        val date = dateEpoch ?: -1L
        return "editor?entryId=$id&dateEpoch=$date"
    }
}
