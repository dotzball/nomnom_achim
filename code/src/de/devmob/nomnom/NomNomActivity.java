/*
 * Copyright 2013 Friederike Wild, created 29.06.2013
 */
package de.devmob.nomnom;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

/**
 * Activity holding the result restaurants on a map or a list.
 * 
 * @author Friederike Wild
 */
public class NomNomActivity extends FragmentActivity
{
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.layout_one_fragment);

        if (savedInstanceState == null)
        {
            FragmentManager fragmentManager = this.getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            // Start the app with the map fragment
            String tag = NomNomMapFragment.class.getName();
            Fragment fragment = Fragment.instantiate(this, tag);
            // Add the fragment and use the class name as tag too
            fragmentTransaction.add(R.id.box_content, fragment, tag);
            // Commit the transaction
            fragmentTransaction.commit();
        }
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
     * Internal method to handle switching the fragment of the current representation.
     * @param isSwitchToMap
     */
    private void switchRepresentation(boolean isSwitchToMap)
    {
        FragmentManager fragmentManager = this.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        Fragment newFragment;
        String tag;

        if (isSwitchToMap)
        {
            tag = NomNomMapFragment.class.getName();
        }
        else
        {
            tag = NomNomListFragment.class.getName();
        }
        
        newFragment = fragmentManager.findFragmentByTag(tag);
        if (newFragment == null)
        {
            newFragment = Fragment.instantiate(this, tag);
        }

        // Replace the current with the new fragment
        fragmentTransaction.replace(R.id.box_content, newFragment, tag);
        // Commit the transaction
        fragmentTransaction.commit();
    }
}
