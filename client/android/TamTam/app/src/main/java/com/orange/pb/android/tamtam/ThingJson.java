package com.orange.pb.android.tamtam;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by pascalbodin on 17/09/16.
 */
public class ThingJson {

    private final static String LOG_TAG = "ThingJson";

    private JSONObject mJsonObject;

    /**
     *
     */
    public ThingJson(String idThing, String pict, String desc, int currency, float price,
                     double lon, double lat, String locationType, boolean stuck) {

        mJsonObject = new JSONObject();

        JSONObject priceJson, locationJson;

        priceJson = new JSONObject();
        locationJson = new JSONObject();
        try {
            priceJson.put("currency", currency);
            priceJson.put("price", price);
            locationJson.put("lon", lon);
            locationJson.put("lat", lat);
            //locationJson.put("locType", locationType);
            mJsonObject.put("idThing", idThing);
            mJsonObject.put("pict", pict);
            mJsonObject.put("desc", desc);
            mJsonObject.put("price", priceJson);
            mJsonObject.put("location", locationJson);
            mJsonObject.put("stuck", stuck);
        } catch (JSONException e) {
            AppLog.d(LOG_TAG, e.getMessage());
            mJsonObject = null;
        }

    }

    /**
     *
     */
    public JSONObject getJson() {

        return mJsonObject;

    }
}
