package com.orange.pb.android.tamtam;

import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by pascalb on 8/17/16.
 */
public class MainActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private final static String LOG_TAG = "MainActivity";

    // Location types for things.
    public final static String LOC_KNOWN      = "known";
    public final static String LOC_LAST_KNOWN = "lastKnown";
    public final static String LOC_NOT_KNOWN  = "notKnown";

    // Server URL.
    public final static String SERVER_URL = "http://systev.com:5003";
    // We are user idUser0.
    // Must end with a "/".
    public final static String THINGS_URL = "/users/idUser0/sellingThings/";

    // TODO - to be replaced by a real unique id.
    public final static String UNIQUE_ID = "TelephonePascal";
    // TODO - to be replaced by local currency.
    public final static int CURRENCY = 978;

    public final static int LOCATION_INTERVAL = 10000;         // 10s
    public final static int LOCATION_FASTEST_INTERVAL = 5000;  // 5s

    private final static String FILE_NAME_PREFIX = "TAMTAM_";

    private final static String BUNDLE_KEY_PICTURE_PATH = "picturePath";

    public final static String SELLING_INTENT_EXTRA_URI = "thumbnailURI";

    private final static boolean AUTO_REFRESH = true;

    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int IMAGE_MAX_WIDTH = 200;
    private static final int IMAGE_MAX_HEIGHT = 200;

    /**
     * For tab handling.
     */
    private TabLayout mTabLayout;
    private ViewPager mViewPager;

    /**
     * For location handling.
     */
    private final static int REQUEST_CHECK_SETTINGS = 100;
    private GoogleApiClient mGoogleApiClient = null;
    private Location mCurrentLocation        = null;
    private LocationRequest mLocationRequest = null;

    /**
     * For pictures.
     */
    private String mCurrentPhotoPath;
    private String mCurrentTimeStamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "onCreate()");
        setContentView(R.layout.activity_main);

        // Restore state.
        if (savedInstanceState != null) {
            CharSequence cs = savedInstanceState.getCharSequence(BUNDLE_KEY_PICTURE_PATH);
            if (cs != null) {
                mCurrentPhotoPath = cs.toString();
                AppLog.d(LOG_TAG, "restored currentPhotoPath: " + mCurrentPhotoPath);
            }
        }

        // Adding toolbar to the activity
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initializing the tablayout
        mTabLayout = (TabLayout) findViewById(R.id.tabLayout);
        // Adding the tabs using addTab() method.
        // Ta titles can't be set here, as we call mTabLayout.setupWithViewPager() below.
        mTabLayout.addTab(mTabLayout.newTab());  // My things
        mTabLayout.addTab(mTabLayout.newTab());  // Around me
        mTabLayout.addTab(mTabLayout.newTab());  // Tracked things
        mTabLayout.addTab(mTabLayout.newTab());  // Log
        mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        // Initializing mViewPager.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        // Creating our pager adapter. Tab titles must be set here.
        String[] tabTitles = new String[4];
        tabTitles[0] = getResources().getString(R.string.tab1_title);
        tabTitles[1] = getResources().getString(R.string.tab2_title);
        tabTitles[2] = getResources().getString(R.string.tab3_title);
        tabTitles[3] = getResources().getString(R.string.tab_log);
        Pager adapter = new Pager(getSupportFragmentManager(), tabTitles);
        // Adding adapter to pager.
        mViewPager.setAdapter(adapter);

        mTabLayout.setupWithViewPager(mViewPager, AUTO_REFRESH);
        // Adding onTabSelectedListener to swipe views
        mTabLayout.addOnTabSelectedListener(this);

        adapter.createFragments();

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
        mLocationRequest.setInterval(LOCATION_INTERVAL);
        mLocationRequest.setFastestInterval(LOCATION_FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }

    @Override
    protected void onStart() {

        AppLog.d(LOG_TAG, "onStart()");
        mGoogleApiClient.connect();
        super.onStart();

    }

    @Override
    protected void onStop() {

        AppLog.d(LOG_TAG, "onStop()");
        stopLocationUpdates();
        mGoogleApiClient.disconnect();
        super.onStop();

    }

    /**
     *
     * One use case: the user wants to add a new thing. Our activity
     * calls the camera activity. Then the user rotates the phone. Our
     * activity will be destroyed, and re-created once the picture is
     * accepted by the user. Consequence: our internal state has to be
     * saved and then restored.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);
        outState.putCharSequence(BUNDLE_KEY_PICTURE_PATH, mCurrentPhotoPath);

    }

    /**
     * Implementation of TabLayout.OnTabSelectedListener interface.
     * @param tab
     */
    @Override
    public void onTabSelected(TabLayout.Tab tab) {

        mViewPager.setCurrentItem(tab.getPosition());

    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

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
                                    MainActivity.this,
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
        AppLog.d(LOG_TAG, "onConnectionFailed()");

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

    /**
     *
     * Action bar menu handling.
     *
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;

            case R.id.action_add:
                dispatchTakePictureIntent();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    /**
     *
     * Camera handling.
     *
     */

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go.
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File.
                AppLog.d(LOG_TAG, "createImageFile() => " + ex.getMessage());
            }
            // Continue only if the File was successfully created.
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.orange.pb.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                // When the user exits from the camera application, onActivityResult() is called.
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            AppLog.d(LOG_TAG, "Calling createThumbnailFile()");
            try {
                Uri thumbnailURI = createThumbnailFile();
                // Start the selling activity.
                Intent intent = new Intent(this, SellingActivity.class);
                intent.putExtra(SELLING_INTENT_EXTRA_URI, thumbnailURI.toString());
                startActivity(intent);
            } catch (IOException e) {
                AppLog.d(LOG_TAG, "createThumbnailFile() => " + e.getMessage());
            }
        }

    }

    /**
     *
     * Side effects:
     * - sets mCurrentTimeStamp
     * - sets mCurrentPhotoPath
     *
     */
    private File createImageFile() throws IOException {
        // Create an image file name.
        mCurrentTimeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());
        String imageFileName = FILE_NAME_PREFIX + mCurrentTimeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save file path.
        mCurrentPhotoPath = image.getAbsolutePath();
        AppLog.d(LOG_TAG, "creatImageFile() - mCurrentPhotoPath: " + mCurrentPhotoPath);
        return image;
    }

    /**
     *
     * Returns URI to thumbnail file to be displayed by selling activity.
     *
     */
    private Uri createThumbnailFile() throws IOException {

        // Target dimensions.
        int targetW = IMAGE_MAX_WIDTH;
        int targetH = IMAGE_MAX_HEIGHT;

        // Get the dimensions of the bitmap.
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        AppLog.d(LOG_TAG, "current photo path: " + mCurrentPhotoPath);
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        AppLog.d(LOG_TAG, "Picture W x H: " + photoW + " x " + photoH);

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);
        AppLog.d(LOG_TAG, "Scale factor: " + scaleFactor);

        // Decode the image file into a Bitmap sized according to our requirements.
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        AppLog.d(LOG_TAG, "Bitmap width:  " + bitmap.getWidth());
        AppLog.d(LOG_TAG, "Bitmap height: " + bitmap.getHeight());

        // Create file.
        String imageFileName = FILE_NAME_PREFIX + mCurrentTimeStamp + "_TN";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Create URI.
        Uri photoURI = FileProvider.getUriForFile(this,
                "com.orange.pb.android.fileprovider",
                image);
        // Save bitmap to file.
        OutputStream outStream = new FileOutputStream(image);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
        outStream.flush();
        outStream.close();
        return photoURI;

    }

}
