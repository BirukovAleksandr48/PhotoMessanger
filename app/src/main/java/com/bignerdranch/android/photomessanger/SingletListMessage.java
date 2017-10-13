package com.bignerdranch.android.photomessanger;

import android.content.Context;
import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

public class SingletListMessage {
    private static SingletListMessage sListMessages;
    private Context mContext;
    private ArrayList<Bitmap> mBitmaps;

    public static SingletListMessage get(Context context) {
        if (sListMessages == null) {
            sListMessages = new SingletListMessage(context);
        }
        return sListMessages;
    }

    private SingletListMessage(Context context) {
        mContext = context.getApplicationContext();
        mBitmaps = new ArrayList<>();
    }

    public List<Bitmap> getBitmaps() {
        return mBitmaps;
    }

    public int addBitmap(Bitmap param){
        mBitmaps.add(param);
        return mBitmaps.indexOf(param);
    }

    public Bitmap getBitmap(int position){
        return mBitmaps.get(position);
    }
}
