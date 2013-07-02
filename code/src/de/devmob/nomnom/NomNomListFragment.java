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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import de.devmob.nomnom.adapter.NomNomListAdapter;
import de.devmob.nomnom.data.DatabaseOpenHelper;
import de.devmob.nomnom.data.NomNomContentProvider;

/**
 * Fragment holding the result list with restaurants.
 * 
 * @author Friederike Wild
 */
public class NomNomListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>
{
    /** Store a reference to the list adapter */
    private NomNomListAdapter mAdapter;
    
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
        // Creates a new loader after the initLoader () call - In addition add the check if the entry shall be shown and is NOT the current location, cause that one shall not be shown in the list
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
        mAdapter.swapCursor(data);
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoaderReset(android.support.v4.content.Loader)
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader)
    {
        // data is not available anymore, delete reference
        mAdapter.swapCursor(null);
    }

    /**
     * Fill the list with data from the cursor.
     */
    private void initAdapter()
    {
        // Fields from the database (projection)
        // Must include the _id column for the adapter to work
        String[] from = new String[] { DatabaseOpenHelper.COLUMN_NAME, DatabaseOpenHelper.COLUMN_VICINITY, DatabaseOpenHelper.COLUMN_RATING };
        // Fields on the UI to which we map
        int[] to = new int[] { R.id.textName, R.id.textVicinity, R.id.textRating };

        // Initialize the loader manager to start listening to cursor changes
        this.getLoaderManager().initLoader(0, null, this);
        mAdapter = new NomNomListAdapter(
                this.getActivity(), R.layout.layout_list_entry, 
                null, // Initialize the adapter without a valid cursor. Will be swaped when ready
                from, to, 0);

        setListAdapter(mAdapter);
    }
}
