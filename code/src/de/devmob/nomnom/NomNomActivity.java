/*
 * Copyright 2013 Friederike Wild, created 29.06.2013
 */
package de.devmob.nomnom;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import de.devmob.nomnom.data.NomNomUpdater;

/**
 * Activity holding the result restaurants on a map or a list.
 * 
 * For now the activity is not allowed to rotate.
 * There seems to be a bug with the getSupportFragmentManager showing 2 fragments after rotation
 * on top of each other. Found a hint that it is ignoring setRetainInstance while doing this, but 
 * workarounds not helping 100%.
 * 
 * @author Friederike Wild
 */
public class NomNomActivity extends FragmentActivity
{
    /** Share tag for logging */
    public static final String TAG = "NomNom";

    private static final String MAP_CLASS = NomNomMapFragment.class.getName();
    private static final String LIST_CLASS = NomNomListFragment.class.getName();

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Initialize the view
        this.setContentView(R.layout.layout_one_fragment);

        if (savedInstanceState == null)
        {
            FragmentManager fragmentManager = this.getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            // Start the app with the map fragment
            Fragment fragment = Fragment.instantiate(this, MAP_CLASS);
            // Add the fragment and use the class name as tag too
            fragmentTransaction.add(R.id.box_content, fragment, MAP_CLASS);
            // Commit the transaction
            fragmentTransaction.commit();
        }
        
        // Check for updates of places near the current location.
        NomNomUpdater.getInstance().onActivityCreate(this);
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
    @Override
    protected void onResume()
    {
        super.onResume();

        // Register for updates of places near the current location.
        NomNomUpdater.getInstance().onActivityResume();
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onPause()
     */
    @Override
    protected void onPause()
    {
        NomNomUpdater.getInstance().onActivityPause();

        super.onPause();
    }

    /**
     * Method to toggle the current representation to map view.
     */
    public void switchRepresentationToMap()
    {
        this.switchRepresentation(true);
    }

    /**
     * Method to toggle the current representation to list view.
     */
    public void switchRepresentationToList()
    {
        this.switchRepresentation(false);
    }

    /**
     * Method to request a places update manually
     */
    public void requestPlacesUpdate()
    {
        NomNomUpdater.getInstance().requestPlacesUpdate();
    }

    /**
     * Internal method to handle switching the fragment of the current representation.
     * @param isSwitchToMap
     */
    private void switchRepresentation(boolean isSwitchToMap)
    {
        FragmentManager fragmentManager = this.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        Fragment newFragment;
        Fragment oldFragment;
        String newTag;
        String oldTag;

        if (isSwitchToMap)
        {
            newTag = MAP_CLASS;
            oldTag = LIST_CLASS;
        }
        else
        {
            newTag = LIST_CLASS;
            oldTag = MAP_CLASS;
        }

        // Find the old fragment (if available)
        oldFragment = fragmentManager.findFragmentByTag(oldTag);
        if (oldFragment != null)
        {
            // In case it was already shown, we hide it for fast switching between both representations
            fragmentTransaction.hide(oldFragment);
        }

        // Find the now wanted fragment 
        newFragment = fragmentManager.findFragmentByTag(newTag);
        if (newFragment == null)
        {
            newFragment = Fragment.instantiate(this, newTag);
            // Add the fragment
            fragmentTransaction.add(R.id.box_content, newFragment, newTag);
        }
        else
        {
            fragmentTransaction.show(newFragment);
        }

        // Commit the transaction
        fragmentTransaction.commit();
    }
}
