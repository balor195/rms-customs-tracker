package com.rms.customs.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<CustomsDatabase> {
    val dbFile = context.getDatabasePath("customs_tracker.db")
    return Room.databaseBuilder<CustomsDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath,
    )
}
