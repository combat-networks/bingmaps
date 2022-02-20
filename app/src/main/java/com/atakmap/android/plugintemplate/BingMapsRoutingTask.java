package com.atakmap.android.plugintemplate;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.routes.RouteGenerationTask;
import com.atakmap.android.routes.RoutePointPackage;
import com.atakmap.android.routes.nav.NavigationCue;
import com.atakmap.coremap.maps.coords.GeoPoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class BingMapsRoutingTask extends RouteGenerationTask {

    private final static String TAG = "BingMapsRoutingTask";
    private final Context context;

    public BingMapsRoutingTask(RouteGenerationTask.RouteGenerationEventListener routeGenerationEventListener, Context context) {
        super(routeGenerationEventListener);
        this.context = context;
    }

    @Override
    public RoutePointPackage generateRoute(SharedPreferences sharedPreferences, GeoPoint start, GeoPoint finish, List<GeoPoint> list) {

        String key = sharedPreferences.getString("plugin_bingmaps_key_set_apikey", "");
        String travelMode = sharedPreferences.getString("plugin_bingmaps_travel_mode", "walking");
        String optimize = sharedPreferences.getString("plugin_bingmaps_optimize", "distance");

        if (key.isEmpty()) {
            Toast toast = Toast.makeText(context, "No API Key set", Toast.LENGTH_LONG);
            toast.show();
            return null;
        }

        String line = null;
        URL url = null;
        try {
            url = new URL(String.format(Locale.US, "https://dev.virtualearth.net/REST/V1/Routes/Map/Roads/Routes?wp.1=%f,%f;1;Start&wp.2=%f,%f;7;End&travelMode=%s&optmz=%s&key=%s", start.getLatitude(), start.getLongitude(), finish.getLatitude(), finish.getLongitude(), travelMode, optimize, key));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(url.openStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String json = "";
        while (true) {
            try {
                if (!((line = in.readLine()) != null)) break;
            } catch (IOException e) {
                e.printStackTrace();
            }
            json += line;
        }
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONObject routeData;
        try {
            routeData = new JSONObject(json);
        } catch (JSONException e) {
            Log.i(TAG, "Failed to parse json response from server");
            e.printStackTrace();
            return null;
        }

        try {
            int statusCode = routeData.getInt("statusCode");
            String statusDesc = routeData.getString("statusDescription");
            if (statusCode != 200) {
                Log.i(TAG, String.format(Locale.US, "ERROR: statusCode was not 200, %s", statusDesc));
                return null;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        try {
            JSONArray resources = routeData.getJSONArray("resourceSets").getJSONObject(0).getJSONArray("resources");
            JSONArray itineraryItems = resources.getJSONObject(0).getJSONArray("routeLegs").getJSONObject(0).getJSONArray("itineraryItems");
            int points = itineraryItems.length();
            Log.i(TAG, String.format(Locale.US, "Got %d points", points));
            List<PointMapItem> wayPoints = new LinkedList<PointMapItem>();
            Map<String, NavigationCue> wayCues = new HashMap<>();
            double lat, lng;
            GeoPoint gp;
            BingMapsPointMapItem pmi;
            String id, instruction;
            NavigationCue navCue;

            for (int i = 0; i < points; i++) {
                JSONObject maneuverPoint = itineraryItems.getJSONObject(i).getJSONObject("maneuverPoint");
                lat = (double) maneuverPoint.getJSONArray("coordinates").get(0);
                lng = (double) maneuverPoint.getJSONArray("coordinates").get(1);
                instruction = itineraryItems.getJSONObject(i).getJSONObject("instruction").getString("text");
                Log.i(TAG, String.format(Locale.US, "LAT: %f LNG: %f Instruction: %s", lat, lng, instruction));
                id = UUID.randomUUID().toString();
                gp = new GeoPoint(lat, lng, 0);
                pmi = new BingMapsPointMapItem(gp, id);
                pmi.setMetaString("type", "b-m-p-w");
                if (i==0)
                    pmi.setMetaString("title", "Start");
                else if (i==points)
                    pmi.setMetaString("title", "Finish");
                else
                    pmi.setMetaString("title", String.format(Locale.US, "CP%d", i));
                wayPoints.add(pmi);
                navCue = new NavigationCue(UUID.randomUUID().toString(), instruction, instruction);
                navCue.addCue(NavigationCue.TriggerMode.DISTANCE, 25);
                wayCues.put(id, navCue);
            }

            Log.i(TAG, String.format(Locale.US, "Created route package with %d waypoints and %d cues", wayPoints.size(), wayCues.size()));
            return new RoutePointPackage(wayPoints, wayCues);
        } catch (JSONException e) {
            Log.i(TAG, "Failed to parse resources from routeData");
            e.printStackTrace();
            return null;
        }
    }
}