package com.athr.karaoketv.data.library

import com.athr.karaoketv.util.TextNormalizer

/**
 * Vietnamese karaoke rips are named by whoever ripped them. Real-world samples:
 *
 *   "12345 - Gần Như Là - Đen Vâu [Karaoke Beat Tone Nam].mp4"
 *   "Karaoke Duyên Phận - Tone Nữ (Beat chuẩn) HD.mp4"
 *   "[MTV] 60123 Chuyện Của Mùa Đông.mkv"
 *
 * We pull out a song number, a title, an artist and a tone, and throw away the
 * boilerplate so search hits the words a person would actually say.
 */
object FilenameParser {

    data class Parsed(
        val title: String,
        val artist: String?,
        val songNumber: String?,
        val tone: String?,
    )

    private val LEADING_NUMBER = Regex("""^\[?(\d{3,7})]?[\s._\-]+""")
    private val TRAILING_NUMBER = Regex("""[\s._\-]+\[?(\d{3,7})]?$""")
    private val BRACKETED = Regex("""[\[(【][^\[\]()【】]*[\])】]""")
    private val SEPARATORS = Regex("""\s+[-–—_|]\s+""")
    private val MULTI_SPACE = Regex("""\s{2,}""")

    /**
     * "x", "ft." and friends only ever join performers, never words of a song
     * title, so the segment carrying one is the artist — which is what tells
     * "PHƯƠNG MỸ CHI x DTAP _ THỬ LÒNG QUÂN TỬ" apart from the karaoke-rip
     * convention where the title comes first.
     */
    private val COLLAB = Regex(
        """(^|\s)(x|ft\.?|feat\.?|featuring|vs\.?)(\s|$)""",
        RegexOption.IGNORE_CASE,
    )

    /** Dropped wherever they appear; every one of these is rip boilerplate, not a title. */
    private val NOISE_WORDS = setOf(
        "karaoke", "beat", "beat chuan", "nhac song", "nhac song beat",
        "co loi", "khong loi", "loi bai hat", "lyrics", "lyric", "official", "mv",
        "hd", "full hd", "fullhd", "sd", "4k", "2k", "1080p", "1080", "720p", "720",
        "480p", "360p", "hq", "audio", "video", "playback", "instrumental",
        "am thanh", "chat luong cao", "ban chuan", "moi nhat", "vol", "remix karaoke",
    )

    private val TONE_PATTERNS = listOf(
        Regex("""tone\s*nam""") to "Nam",
        Regex("""tone\s*nu""") to "Nữ",
        Regex("""tone\s*thap""") to "Tông thấp",
        Regex("""tone\s*cao""") to "Tông cao",
        Regex("""giong\s*nam""") to "Nam",
        Regex("""giong\s*nu""") to "Nữ",
    )

    /**
     * [parentFolder] disambiguates the two naming conventions that are equally
     * common on Vietnamese karaoke drives — "Tên bài - Ca sĩ" and "Ca sĩ - Tên bài".
     * Nothing in the filename itself tells them apart, but rips are almost always
     * filed under the singer, so a segment matching the folder it sits in is the
     * singer and the other one is the song.
     */
    fun parse(fileName: String, parentFolder: String? = null): Parsed {
        val stem = fileName.substringBeforeLast('.', fileName).trim()
        val folded = TextNormalizer.fold(stem)

        val tone = TONE_PATTERNS.firstOrNull { it.first.containsMatchIn(folded) }?.second

        // Song number can lead or trail; brackets are optional in both positions.
        var work = stem
        var number: String? = null
        LEADING_NUMBER.find(work)?.let {
            number = it.groupValues[1]
            work = work.removeRange(it.range)
        }
        if (number == null) {
            TRAILING_NUMBER.find(work)?.let {
                number = it.groupValues[1]
                work = work.removeRange(it.range)
            }
        }

        // Bracketed segments are almost always tags ([Karaoke], (Beat chuẩn), 【HD】).
        work = BRACKETED.replace(work, " ")

        val segments = SEPARATORS.split(work)
            .map { cleanSegment(it) }
            .filter { it.isNotBlank() }

        val fallbackTitle = cleanSegment(work).ifBlank { stem }
        val first = segments.firstOrNull()?.takeIf { it.isNotBlank() }
        val second = segments.getOrNull(1)?.takeIf { it.length in 2..60 }

        val folderKey = parentFolder?.let { TextNormalizer.key(it) }?.takeIf { it.isNotBlank() }
        val filedUnderArtist = folderKey != null && first != null && second != null &&
            TextNormalizer.key(first) == folderKey
        val firstNamesPerformers = first != null && second != null &&
            COLLAB.containsMatchIn(first) && !COLLAB.containsMatchIn(second)
        val artistLeads = filedUnderArtist || firstNamesPerformers

        val title = (if (artistLeads) second else first) ?: fallbackTitle
        val artist = if (artistLeads) first else second

        return Parsed(
            title = title,
            artist = artist,
            songNumber = number,
            tone = tone,
        )
    }

    /** Strips noise words from a single "Title" / "Artist" segment, preserving case. */
    private fun cleanSegment(segment: String): String {
        val words = segment.split(' ', '_', '\t').filter { it.isNotBlank() }
        val kept = ArrayList<String>(words.size)
        var i = 0
        while (i < words.size) {
            // Try the longest multi-word noise phrase first ("beat chuan" before "beat").
            val three = foldJoin(words, i, 3)
            val two = foldJoin(words, i, 2)
            val one = foldJoin(words, i, 1)
            when {
                three != null && three in NOISE_WORDS -> i += 3
                two != null && two in NOISE_WORDS -> i += 2
                one != null && one in NOISE_WORDS -> i += 1
                two != null && TONE_PATTERNS.any { it.first.matches(two) } -> i += 2
                else -> { kept += words[i]; i += 1 }
            }
        }
        return MULTI_SPACE.replace(kept.joinToString(" "), " ")
            .trim(' ', '-', '_', '.', '|', '–', '—')
    }

    private fun foldJoin(words: List<String>, from: Int, count: Int): String? {
        if (from + count > words.size) return null
        return TextNormalizer.key(words.subList(from, from + count).joinToString(" "))
            .takeIf { it.isNotBlank() }
    }
}
