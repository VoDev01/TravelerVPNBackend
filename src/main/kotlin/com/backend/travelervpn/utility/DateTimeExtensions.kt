package com.backend.travelervpn.utility

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun Instant.toLocalDateTime(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
    return LocalDateTime.ofInstant(this, zoneId)
}

fun Instant.toLocalDateTimeUtc(): LocalDateTime {
    return LocalDateTime.ofInstant(this, ZoneId.of("UTC"))
}
