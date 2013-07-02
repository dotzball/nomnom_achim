/*
 * Copyright 2013 Friederike Wild, created 02.07.2013
 */
package de.devmob.nomnom.data;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

/**
 * The content provider for the restaurants.
 * All the gained data is persisted in an sqlite database and can 
 * therefore be shown even when the activity is started without a connection.
 * 
 * @author Friederike Wild
 */
public class NomNomContentProvider extends ContentProvider
{
    /** Static values for the UriMatcher */
    private static final int        NOMS              = 10;
    private static final int        NOM_DETAIL        = 20;
    
    private static final String     ERROR_MESSAGE     = "Unknown NomNom Content URI: ";

    private static final String     AUTHORITY         = "de.devmob.nomnom.contentprovider";

    private static final String     SCHEME            = "content://";
    
    private static final String     BASE_PATH         = "places";
    public static final Uri         CONTENT_URI       = Uri.parse(SCHEME + AUTHORITY + "/" + BASE_PATH);
    public static final Uri         CONTENT_ID_URI    = Uri.parse(SCHEME + AUTHORITY + "/" + BASE_PATH + "/");

    public static final String      CONTENT_TYPE      = ContentResolver.CURSOR_DIR_BASE_TYPE + "/noms";
    public static final String      CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/nom";

    private static final UriMatcher sURIMatcher       = new UriMatcher(UriMatcher.NO_MATCH);

    /** The database open helper adapter */
    public DatabaseOpenHelper mDBAdapter              = null;

    // Static initialization block
    static
    {
        // Pattern to route URIs ending with our base path to a NOMS operation
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, NOMS);
        // Patter to route URIs ending with the base path plus an integer to a NOM Detail operation
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", NOM_DETAIL);
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#onCreate()
     */
    @Override
    public boolean onCreate()
    {
        // Initialize the data base manager. This will not create/open the database before it is accessed for reading/writing.
        mDBAdapter = new DatabaseOpenHelper(this.getContext());

        // Leave this methods quickly again without time consuming requests

        // Assume everything worked fine
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable
    {
        // Ensure any still open database gets closed
        if (mDBAdapter != null)
        {
            mDBAdapter.close();
        }

        super.finalize();
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#getType(android.net.Uri)
     */
    @Override
    public String getType(Uri uri)
    {
        switch (sURIMatcher.match(uri))
        {
            case NOMS:
                return CONTENT_TYPE;
            case NOM_DETAIL:
                return CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException(ERROR_MESSAGE + uri);
        }
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        // Construct a SQLiteQueryBuilder to fill it with the projection
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        // Check if the caller has requested columns that don't exist
        DatabaseOpenHelper.checkColumns(projection);
        
        // Set the table
        queryBuilder.setTables(DatabaseOpenHelper.TABLE_NAME);

        // Add the details part if needed
        switch (sURIMatcher.match(uri))
        {
            case NOMS:
            {
                // Nothing extra to be added
            } break;
            
            case NOM_DETAIL:
            {
                // Adding the ID to the original query
                queryBuilder.appendWhere(DatabaseOpenHelper.COLUMN_ID + "= " + uri.getLastPathSegment());
            } break;
            
            default:
            {
                throw new IllegalArgumentException(ERROR_MESSAGE + uri);
            }
        }

        String orderBy;
        // Check if a sort order was provided
        if (TextUtils.isEmpty(sortOrder))
        {
            orderBy = DatabaseOpenHelper.DEFAULT_SORT_ORDER;
        }
        else
        {
            // If available use the provided sort order
            orderBy = sortOrder;
        }

        // Request the database, no grouping of rows wanted
        Cursor cursor = queryBuilder.query(
                this.mDBAdapter.getReadableDatabase(), projection, selection, selectionArgs, null, null, orderBy);
        // Make sure that potential listeners are getting notified
        cursor.setNotificationUri(this.getContext().getContentResolver(), uri);

        return cursor;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#insert(android.net.Uri, android.content.ContentValues)
     */
    @Override
    public Uri insert(Uri uri, ContentValues values)
    {
        long id = 0;

        switch (sURIMatcher.match(uri))
        {
            case NOMS:
            {
                id = this.mDBAdapter.getWritableDatabase().insertWithOnConflict(
                        DatabaseOpenHelper.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            } break;

            default:
            {
                throw new IllegalArgumentException(ERROR_MESSAGE + uri);
            }
        }

        // Check if successful
        if (id > 0)
        {
            // Creates a URI with the note ID pattern and the new row ID appended to it.
            Uri newUri = ContentUris.withAppendedId(CONTENT_ID_URI, id);
            // Make sure that potential listeners are getting notified
            this.getContext().getContentResolver().notifyChange(newUri, null);

            return newUri;
        }

        throw new SQLException("NomNom ContentProvider: Failed to insert values into " + uri);
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs)
    {
        SQLiteDatabase database = this.mDBAdapter.getWritableDatabase();

        // Create the variable to hold the count
        int count;

        switch (sURIMatcher.match(uri))
        {
            case NOMS:
            {
                // With the general pattern we use the provided where selection
                count = database.delete(
                        DatabaseOpenHelper.TABLE_NAME, selection, selectionArgs);
            } break;
            
            case NOM_DETAIL:
            {
                // Restrict the selection where clause to the detail id
                String finalSelection = DatabaseOpenHelper.COLUMN_ID + "=" + uri.getLastPathSegment();

                // Add any given additional selection
                if (!TextUtils.isEmpty(selection))
                {
                    finalSelection += " AND " + selection;
                }

                count = database.delete(
                        DatabaseOpenHelper.TABLE_NAME, finalSelection, selectionArgs);
            } break;
            
            default:
            {
                throw new IllegalArgumentException(ERROR_MESSAGE + uri);
            }
        }

        // Make sure that potential listeners are getting notified
        this.getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[])
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        SQLiteDatabase database = this.mDBAdapter.getWritableDatabase();

        // Create the variable to hold the count
        int count;

        switch (sURIMatcher.match(uri))
        {
            case NOMS:
            {
                count = database.update(
                        DatabaseOpenHelper.TABLE_NAME, values, selection, selectionArgs);
            } break;
            
            case NOM_DETAIL:
            {
                // Restrict the selection where clause to the detail id
                String finalSelection = DatabaseOpenHelper.COLUMN_ID + "=" + uri.getLastPathSegment();
                
                // Add any given additional selection
                if (!TextUtils.isEmpty(selection))
                {
                    finalSelection += " AND " + selection;
                }
                
                count = database.update(
                        DatabaseOpenHelper.TABLE_NAME, values, finalSelection, selectionArgs);
                
            } break;
            
            default:
            {
                throw new IllegalArgumentException(ERROR_MESSAGE + uri);
            }
        }

        // Make sure that potential listeners are getting notified
        this.getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }
}
