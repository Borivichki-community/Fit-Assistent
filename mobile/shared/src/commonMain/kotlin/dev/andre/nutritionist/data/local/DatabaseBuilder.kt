package dev.andre.nutritionist.data.local

import androidx.room.RoomDatabase

interface DatabaseBuilder<T : RoomDatabase> {
    fun build(): T
}