/*
 * Copyright 2013 Friederike Wild, created 02.07.2013
 */
package de.devmob.nomnom.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;

/**
 * A simple list adapter to fill the list of restaurants nearby from a given cursor.
 * 
 * @author Friederike Wild
 */
public class NomNomListAdapter extends SimpleCursorAdapter
{
    /**
     * Constructor for the cursor adapter.
     * 
     * @param context The context to which the list belongs
     * @param layout The resource id of a layout file to use for each item
     * @param c The cursor to the data to show in the list.
     * @param from List of columns in the cursor with data to show.
     * @param to List of views in the layout in the same order as the from parameter
     * @param flags Additional flags
     */
    public NomNomListAdapter(
            Context context, int layout, Cursor c, String[] from, int[] to, int flags)
    {
        super(context, layout, c, from, to, flags);

        // TODO (fwild): Use setViewBinder to do additional binding
        
        /*
         * ViewBinder#setViewValue(android.view.View, android.database.Cursor, int)}
         * is invoked. If the returned value is true, binding has occured. If the
         * returned value is false
         */
    }
}