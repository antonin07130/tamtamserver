package com.orange.pb.clienttest1;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by pascalb on 27/07/16.
 */
public class AppRequestQueue {

    private static AppRequestQueue mInstance;
    private RequestQueue mRequestQueue;
    private static Context mContext;

    private AppRequestQueue(Context context) {

        mContext = context;
        mRequestQueue = getRequestQueue();

    }

    public static synchronized AppRequestQueue getInstance(Context context) {

        if (mInstance == null) {
            mInstance = new AppRequestQueue(context);
        }
        return mInstance;

    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(mContext.getApplicationContext());
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

}
