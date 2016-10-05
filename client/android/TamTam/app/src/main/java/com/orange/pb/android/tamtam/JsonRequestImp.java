package com.orange.pb.android.tamtam;

/**
 * Created by pascalbodin on 05/10/2016.
 */

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonRequest;

import org.json.JSONObject;

/**
 * A request to send a JSONObject. copied from JsonObjectRequest.
 * But does not expect a returned JSON object as JsonObjectRequest does.
 */
public class JsonRequestImp extends JsonRequest<JSONObject> {

    /**
     *
     * @param method
     * @param url
     * @param jsonRequest
     * @param listener
     * @param errorListener
     */
    public JsonRequestImp(int method, String url, JSONObject jsonRequest,
                          Response.Listener<JSONObject> listener,
                          Response.ErrorListener errorListener) {

        super(method, url, jsonRequest.toString(), listener, errorListener);

    }

    /**
     *
     * @param response with JSONObject always null.
     * @return
     */
    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {

        return Response.success(null, null);

    }


}
