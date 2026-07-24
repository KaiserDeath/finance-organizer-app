package pe.moneyflow.core.database

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/** Stores [LocalDate] as epoch-day and [Instant] as epoch-millis (both nullable-safe). */
class Converters {
    @TypeConverter
    fun toLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()
}
