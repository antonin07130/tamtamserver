package com.orange.pb.android.tablayout;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import java.io.IOException;

public class SellingActivity extends AppCompatActivity {

    private final static String LOG_TAG = "SellingActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selling);

        // Get URI of thumbnail.
        Intent intent = getIntent();
        Uri thumbnailURI = Uri.parse(intent.getStringExtra(MainActivity.SELLING_INTENT_EXTRA_URI));
        // Get bitmap.
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), thumbnailURI);
            ImageView imageView = (ImageView) findViewById(R.id.selling_image_iv);
            imageView.setImageBitmap(bitmap);

        } catch (IOException e) {
            AppLog.d(LOG_TAG, "onCreate() => " + e.getMessage());
        }

    }
}
