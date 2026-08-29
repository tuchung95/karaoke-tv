package com.athr.karaoketv.data.prefs

/**
 * The rows the home screen can show. Which ones appear, and in what order, is the
 * viewer's call: a room that only ever sings from one folder wants that folder at
 * the top, and a box shared by a family may not want everyone's history on screen.
 */
enum class HomeShelf(val key: String, val label: String) {
    QUEUE("queue", "Đang chờ"),
    RECENTLY_PLAYED("recent", "Hát gần đây"),
    FAVORITES("favorites", "Yêu thích"),
    MOST_PLAYED("most", "Hát nhiều nhất"),
    CATEGORIES("categories", "Thể loại / Thư mục"),
    ARTISTS("artists", "Ca sĩ"),
    RECENTLY_ADDED("added", "Mới thêm vào ổ cứng");

    companion object {
        val DEFAULT_ORDER: List<HomeShelf> = listOf(
            QUEUE, RECENTLY_PLAYED, FAVORITES, MOST_PLAYED,
            CATEGORIES, ARTISTS, RECENTLY_ADDED,
        )

        fun fromKey(key: String): HomeShelf? = entries.firstOrNull { it.key == key }
    }
}
