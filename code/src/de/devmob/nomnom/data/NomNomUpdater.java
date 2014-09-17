/*
 * Copyright 2013 Friederike Wild, created 01.07.2013
 */
package de.devmob.nomnom.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import de.devmob.nomnom.NomNomActivity;

/**
 * Class to handle location updates and request restaurants via 
 * Google Places API.
 * 
 * @author Friederike Wild
 */
public class NomNomUpdater implements LocationListener
{
    /** Static singleton instance */
    private static NomNomUpdater sIntstance = null;

    /** Instance of the location manager */
    private LocationManager mLocationManager;

    /** Store the activity to handle location updates */
    private Activity        mActivity;
    
    /** Instance of the currently running update task. null if none is present */
    private RestaurantUpdateTask mRunningTask;

    /**
     * Hidden default constructor, to only allow singleton access.
     */
    private NomNomUpdater()
    {
        // Reset the task object to be explicitly null = nothing is running
        mRunningTask = null;
    }

    /**
     * Singleton access. Once created it will always return the same instance.
     * @return
     */
    public static NomNomUpdater getInstance()
    {
        if (sIntstance == null)
        {
            sIntstance = new NomNomUpdater();
        }
        
        return sIntstance;
    }

    /**
     * Method to call during activity creation to check for an available location.
     * @param activity
     */
    public void onActivityCreate(Activity activity)
    {
        this.mActivity = activity;

        // NOTE (fwild): This connects to the LocationManager. Could use google LocationClient too. 
        this.mLocationManager = (LocationManager)activity.getSystemService(Context.LOCATION_SERVICE);
        // Let the system decide on the best provider
        String provider = this.mLocationManager.getBestProvider(new Criteria(), false);
        Location lastLocation = this.mLocationManager.getLastKnownLocation(provider);

        // Last location can be empty in case the provider is currently disabled
        if (lastLocation != null)
        {
            LatLng lastLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
            updatePlacesForLocation(lastLatLng);
        }
    }

    /**
     * Method to call during start to check for an available location.
     * If non available we trigger to get informed as soon as one comes up.
     * @param activity
     */
    public void onActivityResume()
    {
        // Let the system decide on the best provider
        String provider = this.mLocationManager.getBestProvider(new Criteria(), false);

        // Register for location updates every 30secs and when the user moves around 100 meters
        this.mLocationManager.requestLocationUpdates(provider, 30000, 100, this);
    }

    /**
     * Method to call during pausing the activity.
     */
    public void onActivityPause()
    {
        // Remove the location listener again
        if (this.mLocationManager != null)
        {
            this.mLocationManager.removeUpdates(this);
        }
    }

    /**
     * Util method to request an update of the nearby places manually
     */
    public void requestPlacesUpdate()
    {
        Log.i(NomNomActivity.TAG, "Manual requestPlacesUpdate()");
        
        // Let the system decide on the best provider
        String provider = this.mLocationManager.getBestProvider(new Criteria(), false);
        Location lastLocation = this.mLocationManager.getLastKnownLocation(provider);

        // Last location can be empty in case the provider is currently disabled
        if (lastLocation != null)
        {
            LatLng lastLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
            updatePlacesForLocation(lastLatLng);
        }
    }

    /**
     * Util method to start the places update task for the given location
     * @param location
     */
    private void updatePlacesForLocation(LatLng location)
    {
        // Check if an update request is already running and in this case ignore the new one
        if (mRunningTask != null)
        {
            return;
        }

        // Start the update task
        mRunningTask = new RestaurantUpdateTask(this.mActivity);
        mRunningTask.execute(location);
    }

    /* (non-Javadoc)
     * @see android.location.LocationListener#onLocationChanged(android.location.Location)
     */
    @Override
    public void onLocationChanged(Location location)
    {
        Log.i(NomNomActivity.TAG, "User location changed");
        updatePlacesForLocation(new LatLng(location.getLatitude(), location.getLongitude()));
    }

    /* (non-Javadoc)
     * @see android.location.LocationListener#onStatusChanged(java.lang.String, int, android.os.Bundle)
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
        // Ignore for now
    }

    /* (non-Javadoc)
     * @see android.location.LocationListener#onProviderEnabled(java.lang.String)
     */
    @Override
    public void onProviderEnabled(String provider)
    {
        // Ignore for now
    }

    /* (non-Javadoc)
     * @see android.location.LocationListener#onProviderDisabled(java.lang.String)
     */
    @Override
    public void onProviderDisabled(String provider)
    {
        // Ignore for now
    }

    /**
     * Util class to fetch the nearest restaurants to the users position
     * running the network requests on an extra thread.
     * 
     * @author Friederike Wild
     */
    public class RestaurantUpdateTask extends AsyncTask<LatLng, Void, Void>
    {
        /** Our google api key for web requests */
        private static final String GOOGLE_API_KEY = "AIzaSyDaMci9yHm4j7ydGszWK85csoIsSAK3SuM";
        /** Nom Nom shall only show restaurants nearby */
        private static final String WANTED_CATEGORY = "restaurant";
        /** Constant of one mile in meters */
        private static final double MILE_IN_METERS = 1609.344;

        /** The context the asyncTask is started from */
        private Activity            mActivity;

        /**
         * Constructor providing an activity context
         * @param activity
         */
        public RestaurantUpdateTask(Activity activity)
        {
            this.mActivity = activity;
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Void doInBackground(LatLng... lastPositionArray)
        {
            if (lastPositionArray.length > 0)
            {
                // Construct the url
                String googleUrl = createUrl(lastPositionArray[0], MILE_IN_METERS, WANTED_CATEGORY, GOOGLE_API_KEY);
                // Request the json from the google API
                String json = requestJson(googleUrl);
                
                if (!TextUtils.isEmpty(json))
                {
                    // Read the data that shall be shown and store it via ContentResolver
                    storePlacesViaContentResolver(json);
                }
            }
            return null;
        }


        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);
            
            mRunningTask = null;
        }

        /**
         * Util method to create the request url.
         * 
         * @param lastPosition
         * @param radius
         * @param category
         * @param apiKey
         * @return
         */
        private String createUrl(LatLng lastPosition, double radius, String category, String apiKey)
        {
            StringBuilder googleUrlBuilder = new StringBuilder();
            googleUrlBuilder.append("https://maps.googleapis.com/maps/api/place/nearbysearch/json");
            // Insert location
            googleUrlBuilder.append("?location=");
            googleUrlBuilder.append(lastPosition.latitude);
            googleUrlBuilder.append(",");
            googleUrlBuilder.append(lastPosition.longitude);
            // Insert radius of one mile
            googleUrlBuilder.append("&radius=");
            googleUrlBuilder.append(radius);
            // Insert type
            googleUrlBuilder.append("&types=");
            googleUrlBuilder.append(category);
            // Insert api key information
            googleUrlBuilder.append("&key=");
            googleUrlBuilder.append(apiKey);

            return googleUrlBuilder.toString();
        }

        /**
         * Request the json response from the given url
         * 
         * @param googleUrl The api url
         * @return The json string
         */
        private String requestJson(String googleUrl)
        {
            StringBuilder resultJson = new StringBuilder();

            // Pre-define reader to be able to close it in the end
            BufferedReader bufferedReader = null;

            try
            {
                // Request a response via default http client
                HttpClient httpClient = new DefaultHttpClient();
                HttpResponse httpResponse = httpClient.execute(new HttpGet(googleUrl));
                // Check if status is ok
                StatusLine statusLine = httpResponse.getStatusLine();
                if (statusLine.getStatusCode() == HttpURLConnection.HTTP_OK)
                {
                    HttpEntity httpEntity = httpResponse.getEntity();
                    if (httpEntity != null)
                    {
                        // Construct a buffered reader of the response
                        bufferedReader = new BufferedReader(new InputStreamReader(httpEntity.getContent()));
                        // Read the result line by line to our string builder
                        String readLineString;
                        while ((readLineString = bufferedReader.readLine()) != null)
                        {
                            resultJson.append(readLineString);
                        }
                        
                        return resultJson.toString();
                    }
                }
            }
            catch (Exception e)
            {
                // Ignore for first version
                e.printStackTrace();
            }
            finally
            {
                // Close the buffered reader, this will close the inputStreamReader, too
                if (bufferedReader != null)
                {
                    try
                    {
                        bufferedReader.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }

            return null;
        }

        /**
         * Util method to read from a given json string
         * the information that shall be shown as NomNoms and 
         * store it through the ContentProvider for persistence and notifying update listeners.
         * @param json
         */
        private void storePlacesViaContentResolver(String json)
        {
            Log.d(NomNomActivity.TAG, "Json received" /*json*/);

            ContentResolver contentResolver = this.mActivity.getContentResolver();
            
            // Pre-Define the index in case an error occurs
            int index = -1;
            
            try
            {
                JSONObject object = new JSONObject(json);

                String status = object.getString("status");
                Log.d(NomNomActivity.TAG, "Json status: " + status);

                // If ok or no results we delete the old list and read (if available) the results
                if (status.equals("OK") || status.equals("ZERO_RESULTS"))
                {
                    // Prepare an array to contain the ContentValues for all entries
                    // This way all data can be written in one transaction - leading to fast performance and consistency at all time.
                    ContentValues[] contentValuesBulk = null;
                    
                    // Read the result if the status was ok
                    if (status.equals("OK"))
                    {
                        // All the places of the result are stored in a results list
                        JSONArray resultPlaces = object.getJSONArray("results");
                        Log.d(NomNomActivity.TAG, "Json contains #" + resultPlaces.length() + " places");

                        contentValuesBulk = new ContentValues[resultPlaces.length()];
                        
                        // Process each place
                        for (index = 0; index < resultPlaces.length(); index++)
                        {
                            JSONObject currentPlace = (JSONObject) resultPlaces.get(index);
                            
                            // Create a contentVaues object for the current place
                            ContentValues values = new ContentValues();
                            
                            // Store the index in the json for sorting. This way previous search-results will be overwriten 
                            values.put(DatabaseOpenHelper.COLUMN_ID, index);

                            // Retrieve base information
                            values.put(DatabaseOpenHelper.COLUMN_NAME, currentPlace.getString("name"));
                            values.put(DatabaseOpenHelper.COLUMN_GID, currentPlace.getString("id"));
                            values.put(DatabaseOpenHelper.COLUMN_GREF, currentPlace.getString("reference"));
                            
                            // Read the optional vicinity value
                            String vicinityValue;
                            if (currentPlace.has("vicinity"))
                            {
                                vicinityValue = currentPlace.getString("vicinity");
                            }
                            else
                            {
                                // Provide null as an alternative
                                vicinityValue = null;
                            }
                            values.put(DatabaseOpenHelper.COLUMN_VICINITY, vicinityValue);
                            
                            // Read the optional rating value
                            Double ratingValue;
                            if (currentPlace.has("rating"))
                            {
                                ratingValue = currentPlace.getDouble("rating");
                            }
                            else
                            {
                                // Provide null as an alternative
                                ratingValue = null;
                            }
                            values.put(DatabaseOpenHelper.COLUMN_RATING, ratingValue);
                            
                            // Retrieve the location
                            JSONObject geometry = (JSONObject) currentPlace.get("geometry");
                            JSONObject location = (JSONObject) geometry.get("location");
                            values.put(DatabaseOpenHelper.COLUMN_LATITUDE, location.getDouble("lat"));
                            values.put(DatabaseOpenHelper.COLUMN_LONGITUDE, location.getDouble("lng"));

                            // Mark this entry as real
                            values.put(DatabaseOpenHelper.COLUMN_IS_SET, DatabaseOpenHelper.VALUE_TRUE);

                            contentValuesBulk[index] = values;
                        }
                    }

                    // After preparing the dataset changes, all contentResolver requests are now applied
                    
                    // Mark all existing entries as not valid anymore.
                    // This way it's possible to have less entries for a future request then stored before.
                    // With the re-usage we do not need to delete and insert lines over and over again, 
                    // but re-use the before created upto 20 entries (found on one google api response page)
                    ContentValues resetValues = new ContentValues();
                    resetValues.put(DatabaseOpenHelper.COLUMN_IS_SET, DatabaseOpenHelper.VALUE_FALSE);
                    contentResolver.update(NomNomContentProvider.CONTENT_URI, resetValues, null, null);

                    if (contentValuesBulk != null)
                    {
                        // Insert to content resolver as one bulk
                        // NOTE (fwild): For the moment we use the default ContentProvider version of this.
                        // With more then 20 entries at once and a network sync 
                        // it would be wise to overwrite this method and speed it up, using a transaction and a pre-created SQLiteStatement
                        contentResolver.bulkInsert(NomNomContentProvider.CONTENT_URI, contentValuesBulk);
                    }
                }
                else
                {
                    // Fast hack to give the error information to the user
                    Toast.makeText(
                            this.mActivity,
                            "Retrieving the places around you failed. Please try later (Status=" + status + ")", 
                            Toast.LENGTH_LONG).show();
                }
            }
            catch (Exception e)
            {
                Log.e(NomNomActivity.TAG, "Error reading/storing the json " + (index == -1 ? "" : "with entry " + index));
                e.printStackTrace();
            }
        }
    }

}