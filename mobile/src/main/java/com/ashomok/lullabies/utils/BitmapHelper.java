/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ashomok.lullabies.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.ashomok.lullabies.MyApplication;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BitmapHelper {
    private static final String TAG = LogHelper.makeLogTag(BitmapHelper.class);

    // Max read limit that we allow our input stream to mark/reset.
    private static final int MAX_READ_LIMIT_PER_IMG = 1024 * 1024;

    public static Bitmap scaleBitmap(Bitmap src, int maxWidth, int maxHeight) {
       double scaleFactor = Math.min(
           ((double) maxWidth)/src.getWidth(), ((double) maxHeight)/src.getHeight());
        return Bitmap.createScaledBitmap(src,
            (int) (src.getWidth() * scaleFactor), (int) (src.getHeight() * scaleFactor), false);
    }

    public static Bitmap scaleBitmap(int scaleFactor, InputStream is) {
        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeStream(is, null, bmOptions);
    }

    public static int findScaleFactor(int targetW, int targetH, InputStream is) {
        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, bmOptions);
        int actualW = bmOptions.outWidth;
        int actualH = bmOptions.outHeight;

        // Determine how much to scale down the image
        return Math.min(actualW/targetW, actualH/targetH);
    }

    @SuppressWarnings("SameParameterValue")
    public static Bitmap fetchAndRescaleBitmap(String uri, int width, int height)
            throws IOException {
        URL url = new URL(uri);
        BufferedInputStream is = null;
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            is = new BufferedInputStream(urlConnection.getInputStream());
            is.mark(MAX_READ_LIMIT_PER_IMG);
            int scaleFactor = findScaleFactor(width, height, is);
            LogHelper.d(TAG, "Scaling bitmap ", uri, " by factor ", scaleFactor, " to support ",
                    width, "x", height, "requested dimension");
            is.reset();
            return scaleBitmap(scaleFactor, is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    public static Bitmap fetchAndRescaleBitmap(int drawableId, int width, int height) {

        Context context = MyApplication.getAppContext();
        int scaleFactor = 0;
        try {
            scaleFactor = findScaleFactor(width, height, context, drawableId);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        Log.d(TAG, "Scaling bitmap from id " + drawableId + " by factor " + scaleFactor + " to support " +
                width + "x" + height + "requested dimension");
        return scaleBitmap(scaleFactor, context, drawableId);
    }

    private static int findScaleFactor(int targetW, int targetH, Context context, int drawableId) {
        // Get the dimensions of the image
        Drawable d = context.getResources().getDrawable(drawableId);
        int actualH = d.getIntrinsicHeight();
        int actualW = d.getIntrinsicWidth();

        // Determine how much to scale down the image
        return Math.min(actualW / targetW, actualH / targetH);
    }

    private static Bitmap scaleBitmap(int scaleFactor, Context context, int drawableId) {
        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeResource(context.getResources(), drawableId, bmOptions);
    }
}
