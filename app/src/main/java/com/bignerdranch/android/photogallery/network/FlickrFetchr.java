package com.bignerdranch.android.photogallery.network;

import android.net.Uri;
import android.util.Log;

import com.bignerdranch.android.photogallery.model.Gallery;
import com.bignerdranch.android.photogallery.model.GalleryItem;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetchr {

    private static final String TAG = "FlickrFetchr";

    private static final String API_KEY = "c4319665608a3f214acb9e2d1f5a97d2";

    public List<GalleryItem> fetchItems(int page) {

        List<GalleryItem> items = new ArrayList<>();

        try {
            String url = Uri.parse("https://api.flickr.com/services/rest/")
                    .buildUpon()
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("extras", "url_s")
                    .appendQueryParameter("page", String.valueOf(page))
                    .build().toString();

            String jsonString = getUrlString(url);

            items.addAll(parseItems(jsonString));

            Log.i(TAG, "Received JSON: " + jsonString);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items", ioe);
        } catch (JSONException jsone) {
            Log.e(TAG, "Failed to parse JSON", jsone);
        }

        return items;
    }

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() +
                        ": with " +
                        urlSpec);
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
        return new String(getUrlBytes(urlSpec));
    }

    /**
     *
     "photos":{
         "page":1,
         "pages":10,
         "perpage":100,
         "total":1000,
         "photo":[
         {
             "id":"26934626614",
             "owner":"114459395@N04",
             "secret":"bf7132959c",
             "server":"7563",
             "farm":8,
             "title":"Polish armored car Korfanty in 1920 made by Polish fighters in Wo\u017aniak foundry. It was one of two created. [800x461] #HistoryPorn #history #retro http:\/\/ift.tt\/1TVXlAI",
             "ispublic":1,
             "isfriend":0,
             "isfamily":0,
             "url_s":"https:\/\/farm8.staticflickr.com\/7563\/26934626614_bf7132959c_m.jpg",
             "height_s":"138",
             "width_s":"240"
         },
         {
             "id":"26934629124",
             "owner":"142308160@N04",
             "secret":"e038c11c85",
             "server":"7318",
             "farm":8,
             "title":"New photo added to gallery",
             "ispublic":1,
             "isfriend":0,
             "isfamily":0,
             "url_s":"https:\/\/farm8.staticflickr.com\/7318\/26934629124_e038c11c85_m.jpg",
             "height_s":"240",
             "width_s":"180"
         },
         ...

     * @param jsonBody
     */
    private List<GalleryItem> parseItems(String jsonBody) throws JSONException {

        Gson gson = new GsonBuilder().create();
        Gallery gallery = gson.fromJson(jsonBody, Gallery.class);
            
        return gallery.getLegacyItems();

    }

}
