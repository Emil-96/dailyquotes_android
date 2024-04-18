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
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * This file contains all code required for the local database.
 */

/**
 * Represents a quote.
 *
 * @param id The unique id that is also used in the remote backend.
 * @param category The category the quote falls into.
 * @param quote The actual text of the quote.
 * @param quoteUrl The link to the website the quote is from.
 */
@Entity
data class Quote(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "quote") val quote: String,
    @ColumnInfo(name = "image_url") val imageUrl: String,
    @ColumnInfo(name = "quote_url") val quoteUrl: String,
    @ColumnInfo(name = "is_favorite") var isFavorite: Boolean
)

/**
 * The Data Access Object used to interact with the local database.
 */
@Dao
interface QuoteDao{

    /**
     * @return A [List] of all [Quote] elements.
     */
    @Query("SELECT * FROM quote")
    suspend fun getAll(): List<Quote>

    /**
     * Returns the [Quote] element corresponding to the [quoteId].
     *
     * @param quoteId The id of the desired [Quote] element.
     *
     * @return The [Quote] element corresponding to the [quoteId].
     */
    @Query("SELECT * FROM quote WHERE id = (:quoteId)")
    suspend fun getQuoteById(quoteId: String): Quote

    /**
     * Inserts all [Quote] elements in the local database.
     *
     * @param quotes All [Quote] elements that should get inserted into the local database.
     */
    @Insert (onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg quotes: Quote)

    /**
     * Deletion of the [Quote] element from the local database.
     *
     * @param quote The [Quote] element that should be deleted.
     */
    @Delete
    suspend fun delete(quote: Quote)

    @Update
    suspend fun update(quote: Quote)
}

/**
 * The [RoomDatabase] representing the local database.
 */
@Database(entities = [Quote::class], version = 2)
abstract class QuoteDatabase : RoomDatabase(){

    /**
     * @return The Data Access Object used to interact with the database.
     */
    abstract fun quoteDao(): QuoteDao
}