package com.orange.pb.android.tablayout;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.IOException;

public class SellingActivity extends AppCompatActivity {

    private final static String LOG_TAG = "SellingActivity";

    private final static String BUNDLE_KEY_ROTATION = "rotation";

    // To control rotation.
    private final static float ROT_QUARTER = 90.0f;
    private float mCurrentRotation = 0.0f;

    private ImageView mPictureIV;

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
            mPictureIV = (ImageView) findViewById(R.id.selling_image_iv);
            mPictureIV.setImageBitmap(bitmap);
            // Restore state.
            mPictureIV.setRotation(mCurrentRotation);

        } catch (IOException e) {
            AppLog.d(LOG_TAG, "SellingActivity.onCreate() => " + e.getMessage());
        }

    }

    @Override
    protected void onStart() {

        AppLog.d(LOG_TAG, "onStart()");
        super.onStart();

    }

    @Override
    protected void onStop() {

        AppLog.d(LOG_TAG, "onStop()");
        super.onStop();

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);
        outState.putFloat(BUNDLE_KEY_ROTATION, mCurrentRotation);

    }

}
