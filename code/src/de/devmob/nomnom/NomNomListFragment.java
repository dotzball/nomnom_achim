/*
 * Copyright 2013 Friederike Wild, created 29.06.2013
 */
package de.devmob.nomnom;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import de.devmob.nomnom.data.DatabaseOpenHelper;
import de.devmob.nomnom.data.NomNomContentProvider;

/**
 * Fragment holding the result list with restaurants.
 * 
 * @author Friederike Wild
 */
public class NomNomListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, ViewBinder
{
    /** Store a reference to the list adapter */
    private SimpleCursorAdapter mAdapter;
    
    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        this.setHasOptionsMenu(true);
        
        // Link the data to the fragment via an adapter
        this.initAdapter();
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.layout_list, container, false);
        return view;
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onResume()
     */
    @Override
    public void onResume()
    {
        super.onResume();
        this.getActivity().setTitle(R.string.title_results);
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onCreateOptionsMenu(android.view.Menu, android.view.MenuInflater)
     */
    @Override
    public void onCreateOptionsMenu(android.view.Menu menu, android.view.MenuInflater inflater)
    {
        inflater.inflate(R.menu.menu_list, menu);
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Switch tabs
        if (item.getItemId() == R.id.action_map)
        {
            ((NomNomActivity)this.getActivity()).switchRepresentationToMap();
        }

        return super.onOptionsItemSelected(item);
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onCreateLoader(int, android.os.Bundle)
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args)
    {
        // Creates a new loader after the initLoader () call
        CursorLoader cursorLoader = new CursorLoader(
                                    this.getActivity(), NomNomContentProvider.CONTENT_URI, 
                                    DatabaseOpenHelper.PROJECTION_ALL, 
                                    null, null, // No selection wanted
                                    null); // Sort order
        return cursorLoader;
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoadFinished(android.support.v4.content.Loader, java.lang.Object)
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data)
    {
        Cursor oldCursor = mAdapter.swapCursor(data);
        // Clean up if an old cursor was stored in the adapter before
        if (oldCursor != null)
        {
            oldCursor.close();
        }
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoaderReset(android.support.v4.content.Loader)
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader)
    {
        // data is not available anymore, delete reference
        Cursor oldCursor = mAdapter.swapCursor(null);
        
        // Clean up if an old cursor was stored in the adapter before
        if (oldCursor != null)
        {
            oldCursor.close();
        }
    }

    /**
     * Fill the list with data from the cursor.
     */
    private void initAdapter()
    {
        // List of columns in the cursor with data to show.
        String[] from = new String[] { DatabaseOpenHelper.COLUMN_NAME, DatabaseOpenHelper.COLUMN_VICINITY, DatabaseOpenHelper.COLUMN_RATING };
        // List of views in the layout in the same order as the from parameter
        int[] to = new int[] { R.id.textName, R.id.textVicinity, R.id.textRating };

        // Initialize the loader manager to start listening to cursor changes
        this.getLoaderManager().initLoader(0, null, this);
        mAdapter = new SimpleCursorAdapter(
                this.getActivity(), 
                R.layout.layout_list_entry, // The context to which the list belongs 
                null, // Initialize the adapter without a valid cursor. Will be swaped when ready
                from, to, 0);
        
        // Assign an additional binder for special handling
        mAdapter.setViewBinder(this);

        setListAdapter(mAdapter);
    }

    /* (non-Javadoc)
     * @see android.support.v4.widget.SimpleCursorAdapter.ViewBinder#setViewValue(android.view.View, android.database.Cursor, int)
     */
    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex)
    {
        // With the rating we want to bind the value in addition as a colour code
        String columnName = cursor.getColumnName(columnIndex);
        if (columnName.equals(DatabaseOpenHelper.COLUMN_RATING))
        {
            if (cursor.isNull(columnIndex))
            {
                // Hide the rating view if not available
                view.setVisibility(View.GONE);
            }
            else
            {
                view.setVisibility(View.VISIBLE);

                double ratingValue = cursor.getDouble(columnIndex);

                // Pick a colour shape with rounded corners according to the rating 
                int backgroundResourceId;
                if (ratingValue <= 2)
                {
                    backgroundResourceId = R.drawable.shape_rounded_red;
                }
                else if (ratingValue >= 4)
                {
                    backgroundResourceId = R.drawable.shape_rounded_green;
                }
                else
                {
                    backgroundResourceId = R.drawable.shape_rounded_grey;
                }
                view.setBackgroundResource(backgroundResourceId);
                
                if (view instanceof TextView)
                {
                    ((TextView)view).setText(String.valueOf(ratingValue));
                }
            }

            // Mark that a binding has occured
            return true;
        }

        // For all other columns we let the simple cursor adapter handle the binding
        return false;
    }
}
