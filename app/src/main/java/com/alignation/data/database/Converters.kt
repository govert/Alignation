package com.alignation.data.database

import androidx.room.TypeConverter
import com.alignation.data.model.EventType
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }

    @TypeConverter
    fun toLocalDate(epochDay: Long?): LocalDate? {
        return epochDay?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }

    @TypeConverter
    fun toInstant(epochMilli: Long?): Instant? {
        return epochMilli?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun fromEventType(eventType: EventType): String {
        return eventType.name
    }

    @TypeConverter
    fun toEventType(name: String): EventType {
        return EventType.valueOf(name)
    }
}
