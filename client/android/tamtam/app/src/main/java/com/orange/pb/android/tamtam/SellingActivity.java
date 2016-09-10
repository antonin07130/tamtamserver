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
import android.widget.ImageView;

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

import java.io.IOException;

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

    private ImageView mPictureIV;

    /**
     * For location handling.
     */
    private final static int REQUEST_CHECK_SETTINGS = 100;
    private GoogleApiClient mGoogleApiClient = null;
    private Location mCurrentLocation        = null;
    private LocationRequest mLocationRequest = null;

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

        // Get URI of thumbnail.
        Intent intent = getIntent();
        Uri thumbnailURI = Uri.parse(intent.getStringExtra(MainActivity.SELLING_INTENT_EXTRA_URI));
        // Get bitmap.
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), thumbnailURI);
            mPictureIV = (ImageView) findViewById(R.id.selling_picture_iv);
            mPictureIV.setImageBitmap(bitmap);
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

//        AppLog.d(LOG_TAG, "onConnected()");
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

    }

}
