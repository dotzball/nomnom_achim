/*
 * Copyright 2013 Friederike Wild, created 29.06.2013
 */
package de.devmob.nomnom;

import android.database.Cursor;
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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
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
    private static final String   SAVE_MAP_OPTIONS = "mapOptions";
    
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
        }
        
        // Check if we were successful in obtaining the map
        if (this.mMap != null)
        {
            this.setUpMap();
            
            if (savedInstanceState != null)
            {
                // Correct the map look according to the persistent one
                GoogleMapOptions mapOptions = savedInstanceState.getParcelable(SAVE_MAP_OPTIONS);
                this.mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mapOptions.getCamera()));
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
     * @see android.support.v4.app.Fragment#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        if (this.mMap != null)
        {
            // Store the current map specific look
            GoogleMapOptions mapOptions = new GoogleMapOptions();
            mapOptions.camera(this.mMap.getCameraPosition());
            outState.putParcelable(SAVE_MAP_OPTIONS, mapOptions);
        }
        
        super.onSaveInstanceState(outState);
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
        if (data == null || data.isClosed())
        {
            // The cursor can be closed for various reasons, most likely cause some new data is currently written to it.
            // Therefore ignore call and wait for the next update
            Log.w(NomNomActivity.TAG, "onLoadFinished received a closed cursor. Ignoring new data.");
            return;
        }
        
        // Prepare the cursor to read the first entry.
        boolean cursorFilled = data.moveToFirst();

        if (!cursorFilled)
        {
            Log.w(NomNomActivity.TAG, "onLoadFinished received an empty cursor (count= " + data.getCount() + "). Ignoring new data.");
            return;
        }

        // Remove any old markers and overlays
        this.mMap.clear();

        // Create object to collect the bounds
        Builder boundsBuilder = new LatLngBounds.Builder();

        // Get column indices for the needed entries
        int columnNameId = data.getColumnIndex(DatabaseOpenHelper.COLUMN_NAME);
        int columnVicinityId = data.getColumnIndex(DatabaseOpenHelper.COLUMN_VICINITY);
        int columnLatitudeId = data.getColumnIndex(DatabaseOpenHelper.COLUMN_LATITUDE);
        int columnLongitudeId = data.getColumnIndex(DatabaseOpenHelper.COLUMN_LONGITUDE);
        
        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.pin_nom);
        
        // Step through the cursor
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
            
            // Add our nomnom map pin
            marker.icon(icon);
            
            // Markers shall not be draggable
            marker.draggable(false);

            // Add the marker to the map
            this.mMap.addMarker(marker);
        }
        while (data.moveToNext());
        
        Log.d(NomNomActivity.TAG, "Map: Added new markers.");
        
        // Pan to see all markers in view.
        final LatLngBounds bounds = boundsBuilder.build();
        final int markerPadding = this.getResources().getDimensionPixelSize(R.dimen.map_camera_padding);
        
        // Cannot zoom to bounds until the map has a size.
        // Unfortunately checking this doesn't work on all devices.
        // Therefore we try to move the camera and catch if this doesn't work atm
        try
        {
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, markerPadding));
        }
        catch (IllegalStateException e)
        {
            // In case the map isn't completely layouted yet, the call needs to be postponed
            Log.i(NomNomActivity.TAG, "Map wasn't ready while markers got added. Postponed camera update.");
            
            // Register to listen to the camera to be ready
            mMap.setOnCameraChangeListener(new OnCameraChangeListener()
            {
                @Override
                public void onCameraChange(CameraPosition position)
                {
                    // Remove the listener again to not be called again during camera move
                    mMap.setOnCameraChangeListener(null);

                    // Now trigger the camera update to the before created markers
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, markerPadding));
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
        // Remove any markers and close mLastCursor
        this.mMap.clear();
        
        // Close the old cursor and save the just received one
        if (mLastCursor != null)
        {
            mLastCursor.close();
        }
    }
}
