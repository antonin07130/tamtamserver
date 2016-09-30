package com.orange.pb.android.tamtam;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.util.Date;

/**
 * Created by pascalbodin on 17/09/16.
 */
public class Utils {

    private final static String LOG_TAG = "Utils";

    /**
     *
     * Returns a unique string for every call, if called with a period
     * greater than 1 ms.
     *
     */
    public static String getMsStr() {

        Date currentDate = new Date();
        long ms = currentDate.getTime();
        return String.format("%016X", ms);

    }

    /**
     *
     * Converts a bitmap to an hex string containing the JPEG version of the bitmap,
     * converted with no loss.
     *
     */
    public static String bitmapToJpeg(Bitmap bitmap) {

        final char[] hexArray = "0123456789ABCDEF".toCharArray();

        MyByteArrayOutputStream stream = new MyByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        // TODO - what's the maximum index for an array? Protect against overflow.
        byte[] jpegBytes = stream.getBytes();
        int byteCount = stream.getByteCount();
        AppLog.d(LOG_TAG, "JPEG data bytes: " + byteCount);

        char[] hexChars = new char[byteCount * 2];
        for (int i = 0; i < byteCount; i++) {
            int v = jpegBytes[i] & 0xFF;
            hexChars[i * 2] = hexArray[v >>> 4];
            hexChars[i * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);

    }

    /**
     *
     */
    private static class MyByteArrayOutputStream extends ByteArrayOutputStream {

        public byte[] getBytes() {

            return buf;

        }

        public int getByteCount() {

            return count;

        }
    }
}
