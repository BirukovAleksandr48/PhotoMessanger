package com.bignerdranch.android.photomessanger;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class MessangerService extends Service{
    private volatile Socket s;
    private PrintWriter pw;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("MyLog", "onCreate Service");
        Log.e("MyLog", "onCreateMessangerService");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int serverPort = 6666;
                    String address = "192.168.1.4";

                    InetAddress ipAddress = InetAddress.getByName(address);
                    s = new Socket(ipAddress, serverPort);

                    Scanner sc = new Scanner(s.getInputStream());
                    while (true) {
                        String jsonString = sc.nextLine();
                        Log.e("MyLog", "Got a message from server");

                        Message mesToActivity = MainActivity.handler.obtainMessage();
                        mesToActivity.what = MainActivity.UPDATE;
                        mesToActivity.obj = jsonString;
                        MainActivity.handler.sendMessage(mesToActivity);
                    }
                } catch (Exception e) {e.printStackTrace();}
            }
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("MyLog", "onStartCommand");
        if(intent != null) {
            Log.e("MyLog", "onStartCommand");
            final String jsonString = intent.getStringExtra(MainActivity.KEY_JSONMES);

            if (jsonString != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.e("MyLog", "sendMessage");
                            pw = new PrintWriter(s.getOutputStream());
                            pw.write(jsonString + "\n");
                            pw.flush();
                        } catch (IOException e) {e.printStackTrace();}
                    }
                }).start();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.e("MyLog", "onDestroy");
        try {
            if(s.isConnected())
                s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

}
