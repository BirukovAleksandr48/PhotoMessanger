package com.bignerdranch.android.photomessanger;

import android.graphics.Bitmap;

public class MyMessage {
    String mName;
    String mMessage;
    Bitmap mImage;

    public MyMessage(String name, String message, Bitmap image) {
        mName = name;
        mMessage = message;
        mImage = image;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getMessage() {
        return mMessage;
    }

    public void setMessage(String message) {
        mMessage = message;
    }

    public Bitmap getImage() {
        return mImage;
    }

    public void setImage(Bitmap image) {
        mImage = image;
    }
}
