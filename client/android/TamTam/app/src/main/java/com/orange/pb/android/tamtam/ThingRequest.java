package com.orange.pb.android.tamtam;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

/**
 * Created by pascalbodin on 17/09/16.
 */
public class ThingRequest {

    private JsonRequestImp mJsonRequest;

    /**
     *
     * PUT request creation.
     *
     */
    public ThingRequest(String url, JSONObject jsonObject, Response.Listener<JSONObject> listener,
                        Response.ErrorListener errorListener) {

       mJsonRequest = new JsonRequestImp(Request.Method.PUT, url, jsonObject, listener,
               errorListener);

    }

    /**
     *
     */
    public JsonRequestImp getRequest() {

        return mJsonRequest;

    }

}
