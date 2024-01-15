package com.example.proj;

import android.graphics.Bitmap;

public class ImageItem {
    Bitmap image;
    String tags, stamp;

    ImageItem(Bitmap image, String tags, String stamp) {
        this.image = image;
        this.tags = tags;
        this.stamp = stamp;
    }
}
