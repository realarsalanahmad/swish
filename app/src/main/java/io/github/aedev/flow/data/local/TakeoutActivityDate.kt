package io.github.aedev.flow.data.local

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.TextStyle
import java.util.Locale

private val TAKEOUT_DATE =
    Regex("""(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) \d{1,2}, \d{4}(?:, \d{1,2}:\d{2}:\d{2} [AP]M(?: [A-Za-z0-9+:/_-]+)?)?""")
private val TAKEOUT_DAY_FORMAT = DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.US)
private val TAKEOUT_LOCAL_FORMAT = DateTimeFormatter.ofPattern("MMM d, uuuu, h:mm:ss a", Locale.US)
private val TAKEOUT_ZONED_FORMAT =
    DateTimeFormatterBuilder()
        .appendPattern("MMM d, uuuu, h:mm:ss a ")
        .appendZoneText(TextStyle.SHORT, setOf(ZoneId.of("Asia/Kolkata")))
        .toFormatter(Locale.US)

internal fun takeoutHtmlWatchedAt(cell: String): Long {
    val date = TAKEOUT_DATE.find(cell)?.value ?: return 0L
    return runCatching { ZonedDateTime.parse(date, TAKEOUT_ZONED_FORMAT).toInstant().toEpochMilli() }
        .recoverCatching { LocalDateTime.parse(date, TAKEOUT_LOCAL_FORMAT).toInstant(ZoneOffset.UTC).toEpochMilli() }
        .recoverCatching {
            LocalDate
                .parse(date, TAKEOUT_DAY_FORMAT)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        }.getOrDefault(0L)
}
