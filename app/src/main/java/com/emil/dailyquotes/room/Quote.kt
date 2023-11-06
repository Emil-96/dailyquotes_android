package com.emil.dailyquotes.room

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity
data class Quote(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "quote") val quote: String,
    @ColumnInfo(name = "image_url") val imageUrl: String,
    @ColumnInfo(name = "quote_url") val quoteUrl: String,
)

@Dao
interface QuoteDao{
    @Query("SELECT * FROM quote")
    suspend fun getAll(): List<Quote>

    @Query("SELECT * FROM quote WHERE id = (:quoteId)")
    suspend fun getQuoteById(quoteId: String): Quote

    @Insert (onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg quotes: Quote)

    @Delete
    suspend fun delete(quote: Quote)
}

@Database(entities = [Quote::class], version = 1)
abstract class QuoteDatabase : RoomDatabase(){
    abstract fun quoteDao(): QuoteDao
}