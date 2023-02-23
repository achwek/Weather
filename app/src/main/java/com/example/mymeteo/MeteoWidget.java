package com.example.mymeteo;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Implementation of App Widget functionality.
 */
public class MeteoWidget extends AppWidgetProvider {
    String temperature;
    String conditionIconUrl;
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Start the location service to get the current location
        Intent intent = new Intent(context, LocationService.class);
        context.startService(intent);

        // Get the current location from the location service
        Location location = LocationService.getLastKnownLocation(context);

        // If location data is available, update the widget with the location information
        if (location != null) {
            String cityName = getCityName(context, location.getLatitude(), location.getLongitude());
            updateWidget(context, appWidgetManager, appWidgetIds, cityName);
        }
    }

    private String getCityName(Context context, double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);
            return address.getLocality();
        }
        return "";
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, String cityName) {
        // Update the widget with the city name
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.meteo_widget);
        remoteViews.setTextViewText(R.id.idTVCity, cityName);
        getWeatherInfo(cityName, context, remoteViews);

        remoteViews.setTextViewText(R.id.idTVTemp, temperature + "°C");
        Picasso.get().load(conditionIconUrl).into(remoteViews, R.id.idIVCond, new int[]{0});

        // Set up a pending intent to open the MainActivity when the widget is clicked
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        remoteViews.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

        // Update all instances of the widget
        for (int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }

    private void getWeatherInfo(String cityName, Context cntx, RemoteViews remoteViews) {
        String url = "https://api.weatherapi.com/v1/forecast.json?key=8384ce731c8c416892e111040231802&q=" + cityName + "&days=1&aqi=yes&alerts=yes&fbclid=IwAR3cgprPItyFmp8DDtBC5sHhourQgr3tORXcRmYBE9Rjb0b_OiAycsBpv8o";

        RequestQueue requestQueue = Volley.newRequestQueue(cntx);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {



                try {
                    JSONObject current = response.getJSONObject("current");
                    temperature = current.getString("temp_c");
                    Toast.makeText(cntx,temperature,Toast.LENGTH_LONG).show();
                    JSONObject condition = current.getJSONObject("condition");
                   conditionIconUrl = condition.getString("icon");
                         } catch (JSONException e) {
                    e.printStackTrace(); // print the error stack trace for debugging
                    // Handle JSON parsing error, e.g. update the views to show an error message
                    remoteViews.setTextViewText(R.id.idTVTemp, "N/A");
                    //remoteViews.setImageViewResource(R.id.idIVCond, R.drawable.ic_error);
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });


        requestQueue.add(jsonObjectRequest);

    }
}