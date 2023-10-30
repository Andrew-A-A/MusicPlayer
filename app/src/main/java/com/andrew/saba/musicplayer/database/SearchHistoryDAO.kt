package com.andrew.saba.musicplayer.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

class SearchHistoryDAO(context: Context) {

    private val dbHelper: SearchHistoryDatabaseHelper = SearchHistoryDatabaseHelper(context)

    fun insertSearchQuery(searchQuery: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues()
        values.put(SearchHistoryDatabaseHelper.COLUMN_SEARCH_QUERY, searchQuery)
        db.insert(SearchHistoryDatabaseHelper.TABLE_NAME, null, values)
        db.close()
    }


    fun isSearchQueryExists(query: String): Boolean {
        val db = dbHelper.readableDatabase
        val selection = "${SearchHistoryDatabaseHelper.COLUMN_SEARCH_QUERY} = ?" // Use the correct column name
        val selectionArgs = arrayOf(query)

        val cursor = db.query(
            SearchHistoryDatabaseHelper.TABLE_NAME, // Use the correct table name
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        val exists = cursor.count > 0
        cursor.close()
        db.close()

        return exists
    }


    fun getAllSearchQueries(): List<String> {
        val db = dbHelper.readableDatabase
        val query = "SELECT * FROM ${SearchHistoryDatabaseHelper.TABLE_NAME}"
        val cursor: Cursor = db.rawQuery(query, null)

        val searchQueries = mutableListOf<String>()
        val columnIndex = cursor.getColumnIndex(SearchHistoryDatabaseHelper.COLUMN_SEARCH_QUERY)
        if (columnIndex != -1) {
            while (cursor.moveToNext()) {
                val searchQuery = cursor.getString(columnIndex)
                if (searchQuery != null) {
                    searchQueries.add(searchQuery)
                }
            }
        }

        cursor.close()
        db.close()
        return searchQueries
    }
}
