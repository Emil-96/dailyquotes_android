package com.emil.dailyquotes.room

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val Migration1to2 = object : Migration(1,2){
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE quote ADD COLUMN is_favorite INTEGER NOT NULL DEFAULT 0")
    }
}
