package com.orange.pb.android.tamtam;

import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class SellingActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private final static String LOG_TAG = "SellingActivity";

    private final static String BUNDLE_KEY_ROTATION = "rotation";

    /**
     * To control rotation.
     */
    private final static float ROT_QUARTER = 90.0f;
    private float mCurrentRotation = 0.0f;

    private Bitmap mPictureBitmap;

    private EditText mDescriptionET;
    private EditText mPriceET;
    private ImageView mPictureIV;

    /**
     * For location handling.
     *
     * mCurrentLocation can be null. In this case, thing's locationType will be set to notKnown.
     * If mCurrentLocation is not null and mLocationAvailable is false, then thing's locationType
     * will be set to lastKnown. If mLocationAvailable is true, locationType will be set to
     * known.
     */
    private final static int REQUEST_CHECK_SETTINGS = 100;
    private GoogleApiClient mGoogleApiClient = null;
    private Location mCurrentLocation        = null;
    private LocationRequest mLocationRequest = null;
    // Set to true when first new location is received. Set to false when location
    // updates are stopped.
    private boolean mLocationAvailable = false;

    // HTTP request queue.
    private RequestQueue mRequestQueue;

    /**
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selling);

        // Restore state.
        if (savedInstanceState != null) {
            mCurrentRotation = savedInstanceState.getFloat(BUNDLE_KEY_ROTATION);
        }

        // Get references to UI elements.
        mDescriptionET = (EditText)findViewById(R.id.selling_description_value_tf);
        mPriceET = (EditText)findViewById(R.id.selling_price_value_tf);
        // Initialize UI.
        Button rotateB = (Button)findViewById(R.id.selling_rotate_b);
        rotateB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Rotate picture by a 90 degree angle.
                mCurrentRotation += ROT_QUARTER;
                if (mCurrentRotation >= 360.0f) {
                    mCurrentRotation = 0.0f;
                }
                mPictureIV.setRotation(mCurrentRotation);
            }
        });
        Button validateB = (Button)findViewById(R.id.selling_validate_b);
        validateB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Create JSON object for thing to be sold.
                String priceStr = mPriceET.getText().toString();
                float price;
                try {
                    price = Float.parseFloat(priceStr);
                } catch (NumberFormatException e) {
                    AppLog.d(LOG_TAG, "Bad price: " + e.getMessage());
                    price = 0.0f;
                }
                double lon, lat;
                String locationType;
                if (mCurrentLocation == null) {
                    lon = 0.0;
                    lat = 0.0;
                    locationType = MainActivity.LOC_NOT_KNOWN;
                } else {
                    lon = mCurrentLocation.getLongitude();
                    lat = mCurrentLocation.getLatitude();
                    if (mLocationAvailable) {
                        locationType = MainActivity.LOC_KNOWN;
                    } else {
                        locationType = MainActivity.LOC_LAST_KNOWN;
                    }
                }
                String thingId = MainActivity.UNIQUE_ID + Utils.getMsStr();
                ThingJson jsonObject = new ThingJson(thingId,
                        Utils.bitmapToJpeg(mPictureBitmap),
                        mDescriptionET.getText().toString(),
                        MainActivity.CURRENCY,
                        price,
                        lon, lat, locationType,
                        false);
                // Dump object.
                boolean weLoop = true;
                String str = jsonObject.getJson().toString();
                int l = str.length();
                if (l < 200) {
                    AppLog.d(LOG_TAG, str);
                } else {
                    int i = 0;
                    String subStr;
                    while (weLoop) {
                        subStr = str.substring(i * 200, (i + 1) * 200);
                        AppLog.d(LOG_TAG, subStr);
                        i += 1;
                        if ((i + 1) * 200 > l) {
                            subStr = str.substring(i * 200, l);
                            AppLog.d(LOG_TAG, subStr);
                            weLoop = false;
                        }
                    }
                }
                // Create request for thing creation.
                Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.d(LOG_TAG, "Successful response");
                    }
                };
                Response.ErrorListener errorListener = new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.d(LOG_TAG, "Error: " + error.getMessage());
                    }
                };
                ThingRequest request = new ThingRequest(
                        MainActivity.SERVER_URL + MainActivity.THINGS_URL + thingId,
                        jsonObject.getJson(),
                        responseListener,
                        errorListener);
                mRequestQueue.add(request.getRequest());

            }
        });

        // Get URI of thumbnail.
        Intent intent = getIntent();
        Uri pictureUri = Uri.parse(intent.getStringExtra(MainActivity.SELLING_INTENT_EXTRA_URI));
        // Get bitmap.
        try {
            mPictureBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),
                    pictureUri);
            mPictureIV = (ImageView) findViewById(R.id.selling_picture_iv);
            mPictureIV.setImageBitmap(mPictureBitmap);
            // Restore state.
            mPictureIV.setRotation(mCurrentRotation);

        } catch (IOException e) {
            AppLog.d(LOG_TAG, "SellingActivity.onCreate() => " + e.getMessage());
        }

        /**
         * Location generation configuration.
         */
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        // Prepare a LocationRequest to later set location service configuration.
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(MainActivity.LOCATION_INTERVAL);
        mLocationRequest.setFastestInterval(MainActivity.LOCATION_FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Get HTTP request queue.
        mRequestQueue = AppRequestQueue.getInstance(this.getApplicationContext()).
                getRequestQueue();

    }

    /**
     *
     */
    @Override
    protected void onStart() {

        AppLog.d(LOG_TAG, "onStart()");
        mGoogleApiClient.connect();
        super.onStart();

    }

    /**
     *
     */
    @Override
    protected void onStop() {

        AppLog.d(LOG_TAG, "onStop()");
        stopLocationUpdates();
        mGoogleApiClient.disconnect();
        super.onStop();

    }

    /**
     *
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);
        outState.putFloat(BUNDLE_KEY_ROTATION, mCurrentRotation);

    }

     /**
     *
     * Location handling.
     *
     */

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        AppLog.d(LOG_TAG, "onConnected()");
//        if (ActivityCompat.checkSelfPermission(this,
//                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
//                PackageManager.PERMISSION_GRANTED) {
//            AppLog.d(LOG_TAG, "Location permissions not granted");
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//
        // Request to set our configuration.
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        // And ask to check result.
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                AppLog.d(LOG_TAG, "onResult()");
                final Status status = result.getStatus();
                final LocationSettingsStates locationSettingsStates = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.
                        startLocationUpdates();
                        AppLog.d(LOG_TAG, "Location settings OK");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    SellingActivity.this,
                                    REQUEST_CHECK_SETTINGS);
                            // TODO: implement onActivityResult().
                            AppLog.d(LOG_TAG, "Location settings NOK");
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        AppLog.d(LOG_TAG, "Location settings not available");
                        break;
                }
            }
        });

        // Get last location.
        try {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
        } catch (SecurityException e) {
            AppLog.d(LOG_TAG, "onConnected() - " + e.getMessage());
            mCurrentLocation = null;
        }
        if (mCurrentLocation != null) {
            AppLog.d(LOG_TAG, "latitude:  " + mCurrentLocation.getLatitude());
            AppLog.d(LOG_TAG, "longitude: " + mCurrentLocation.getLongitude());
        }

    }

    /**
     *
     */
    @Override
    public void onConnectionSuspended(int i) {

        // TODO
        AppLog.d(LOG_TAG, "onConnectionSuspended()");

    }

    /**
     *
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        // TODO
        AppLog.d(LOG_TAG, "onConnectionFalied()");

    }

    /**
     *
     */
    @Override
    public void onLocationChanged(Location location) {

        mCurrentLocation = location;
        mLocationAvailable = true;
        AppLog.d(LOG_TAG, "new latitude: " + location.getLatitude());
        AppLog.d(LOG_TAG, "new longitude: " + location.getLongitude());

    }

    /**
     *
     */
    protected void startLocationUpdates() {

//        // TODO:
//        // Permission checking below should not be required, as it is already performed
//        // at location settings time, above. Check how to remove it.
//        if (ActivityCompat.checkSelfPermission(this,
//                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
//                PackageManager.PERMISSION_GRANTED) {
//            AppLog.d(LOG_TAG, "Location permissions not granted");
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        } catch (SecurityException e) {
            AppLog.d(LOG_TAG, "startLocationUpdates() - " + e.getMessage());
        }
    }


    /**
     *
     */
    protected void stopLocationUpdates() {

        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
        mLocationAvailable = false;

    }

}
