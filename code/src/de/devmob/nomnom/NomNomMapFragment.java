/*
 * Copyright 2013 Friederike Wild, created 29.06.2013
 */
package de.devmob.nomnom;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.android.gms.maps.model.MarkerOptions;

import de.devmob.nomnom.data.DatabaseOpenHelper;
import de.devmob.nomnom.data.NomNomContentProvider;

/**
 * Fragment holding the map with restaurants.
 * 
 * @author Friederike Wild
 */
public class NomNomMapFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>
{
    /** Note that this may be null if the Google Play services APK is not available. */
    private GoogleMap mMap;
    
    /** Store a reference to the last received cursor */
    private Cursor mLastCursor;

    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        this.setHasOptionsMenu(true);

        // Initialize the map stuff
        if (this.mMap == null)
        {
            // Try to obtain the map from the SupportMapFragment of the activity
            this.mMap = ((SupportMapFragment) this.getActivity().getSupportFragmentManager().findFragmentById(R.id.map)).getMap();

            // Check if we were successful in obtaining the map
            if (this.mMap != null)
            {
                this.setUpMap();
            }
        }
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.layout_map, container, false);

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
        inflater.inflate(R.menu.menu_map, menu);
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Switch tabs
        if (item.getItemId() == R.id.action_list)
        {
            ((NomNomActivity)this.getActivity()).switchRepresentationToList();
        }
        else if (item.getItemId() == R.id.action_refresh)
        {
            ((NomNomActivity)this.getActivity()).requestPlacesUpdate();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Util method to setup the map
     */
    private void setUpMap()
    {
        this.mMap.setMyLocationEnabled(true);
        
        // Initialize the loader manager to start listening to cursor changes
        this.getLoaderManager().initLoader(0, null, this);
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
        // Remove any old markers and overlays
        this.mMap.clear();

        // Step through the cursor
        boolean cursorFilled = data.moveToFirst();

        if (!cursorFilled)
        {
            Log.w(NomNomActivity.TAG, "onLoadFinished received an empty cursor. " + data.getCount());
            return;
        }

        // Create object to collect the bounds
        Builder boundsBuilder = new LatLngBounds.Builder();

        // Get column indices for the needed entries
        int columnNameId = data.getColumnIndex(DatabaseOpenHelper.COLUMN_NAME);
        int columnVicinityId = data.getColumnIndex(DatabaseOpenHelper.COLUMN_VICINITY);
        int columnLatitudeId = data.getColumnIndex(DatabaseOpenHelper.COLUMN_LATITUDE);
        int columnLongitudeId = data.getColumnIndex(DatabaseOpenHelper.COLUMN_LONGITUDE);
        
        do
        {
            // Create marker
            MarkerOptions marker = new MarkerOptions();

            // Use title and vicinity as title and snippet of the marker
            marker.title(data.getString(columnNameId));
            marker.snippet(data.getString(columnVicinityId));

            // Add the position to the marker and to the collected bounds
            double latitude = data.getDouble(columnLatitudeId);
            double longitude = data.getDouble(columnLongitudeId);
            LatLng position = new LatLng(latitude, longitude);
            marker.position(position);
            boundsBuilder.include(position);
            
            marker.draggable(false);

            // Add the marker to the map
            this.mMap.addMarker(marker);
        }
        while (data.moveToNext());
        
        // Pan to see all markers in view.
        final LatLngBounds bounds = boundsBuilder.build();
        // Cannot zoom to bounds until the map has a size.
        final View mapView = ((SupportMapFragment) this.getActivity().getSupportFragmentManager().findFragmentById(R.id.map)).getView();
        if (mapView.getViewTreeObserver().isAlive())
        {
            mapView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener()
            {
                @SuppressWarnings("deprecation") // We use the new method when supported
                @SuppressLint("NewApi") // We check which build version we are using.
                @Override
                public void onGlobalLayout()
                {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
                    {
                        mapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                    else
                    {
                        mapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
                }
            });
        }

        // Close the old cursor and save the just received one
        if (mLastCursor != null)
        {
            mLastCursor.close();
        }
        mLastCursor = data;
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoaderReset(android.support.v4.content.Loader)
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader)
    {
        // Data is not available anymore.
        // TODO: Ignored for now. Remove any markers and close mLastCursor
    }
}
