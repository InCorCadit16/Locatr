package com.incorcadit16.locatr;

import android.location.Location;
import android.net.Uri;
import android.nfc.Tag;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

class FlickrFetchr {
    private static final String TAG = "FlickrFetchr";
    private static final String API_KEY = "cd8b2a871e065582e017b45f13977127";
    private final static String FETCH_RECENT_METHOD = "flickr.photos.getRecent";
    private final static String SEARCH_METHOD = "flickr.photos.search";
    private final static Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("method","flickr.photos.getRecent")
            .appendQueryParameter("api_key",API_KEY)
            .appendQueryParameter("format","json")
            .appendQueryParameter("nojsoncallback","1")
            .appendQueryParameter("extras","url_s,geo").build();

    byte[] getUrlByte(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }

            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    private String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlByte(urlSpec));
    }

    private String buildUrl(Location location) {
        return ENDPOINT.buildUpon()
                .appendQueryParameter("method","flickr.photos.getRecent")
                .appendQueryParameter("lan", "" + location.getLatitude())
                .appendQueryParameter("lon","" + location.getLongitude())
                .build().toString();
    }

    List<GalleryItem> fetchRecentPhotos(Location location) {
        String url = buildUrl(location);
        return downloadGalleryItems(url);
    }

    List<GalleryItem> searchPhotos(Location location) {
        String url = buildUrl(location);
        return downloadGalleryItems(url);
    }

    private List<GalleryItem> downloadGalleryItems(String url) {
        List<GalleryItem> items = new ArrayList<>();
        try {
            String jsonString = getUrlString(url);
            JSONObject jsonBody = new JSONObject(jsonString);
            Log.i(TAG,"Received JSON: "+ jsonString);

            parseItem(items,jsonBody);
        } catch (IOException e) {
            Log.e(TAG,"Failed to fetch items: " + e);
        } catch (JSONException e) {
            Log.e(TAG,"Failed to parse JSON: " + e);
        }

        return items;
    }

    private void parseItem(List<GalleryItem> items, JSONObject jsonBody) throws IOException, JSONException {
        JSONObject jsonObject = jsonBody.getJSONObject("photos");
        JSONArray jsonArray = jsonObject.getJSONArray("photo");

        for (int i = 0;i < jsonArray.length();i++) {
            JSONObject photoObject = jsonArray.getJSONObject(i);

            GalleryItem item = new GalleryItem();
            item.setId(photoObject.getString("id"));
            item.setCaption(photoObject.getString("title"));
            item.setOwnerId(photoObject.getString("owner"));

            if (!photoObject.has("geo")) {
                item.setLat(photoObject.getDouble("latitude"));
                item.setLon(photoObject.getDouble("longitude"));
            }

            if (!photoObject.has("url_s")) {
                continue;
            }

            item.setUrl(photoObject.getString("url_s"));
            items.add(item);


        }
    }
}
