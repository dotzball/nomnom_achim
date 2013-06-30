/*
 * Copyright 2013 Friederike Wild, created 29.06.2013
 */
package de.devmob.nomnom;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

/**
 * Fragment holding the map with restaurants.
 * 
 * @author Friederike Wild
 */
public class NomNomMapFragment extends Fragment
{
    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        this.setHasOptionsMenu(true);
        
        // TODO (fwild): Initialize the map stuff
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
        // TODO (fwild): Switch tabs

        return super.onOptionsItemSelected(item);
    }
}
