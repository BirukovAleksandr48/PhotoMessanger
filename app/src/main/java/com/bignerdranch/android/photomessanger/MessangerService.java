package com.bignerdranch.android.photomessanger;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class MessangerService extends Service{
    private volatile Socket s;
    private InputStream inputStream = null;
    private ByteArrayOutputStream byteArray = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("MyLog", "onCreateMessangerService");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int serverPort = 6666;
                    String address = "192.168.1.6";

                    InetAddress ipAddress = InetAddress.getByName(address);
                    s = new Socket(ipAddress, serverPort);
                    inputStream = s.getInputStream();

                    while(true){
                        byteArray = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int count = 0;
                        try {
                            while((count = inputStream.read(buffer,0,buffer.length)) != -1) {
                                byteArray.write(buffer, 0, count);
                            }
                        } catch (IOException e) {e.printStackTrace();}

                        Log.e("MyLog", "Получил сообщение, размер: " + String.valueOf(byteArray.size()));
                        if(byteArray.size() == 0){
                            continue;
                        }

                        String jsonString = byteArray.toByteArray().toString();
                        Gson gson = new Gson();
                        MyMessage m = gson.fromJson(jsonString, MyMessage.class);
                        String path = null;

                        if(m.getImage() != null){
                            path = MainActivity.SaveImage(m.getImage());
                        }

                        Message mesToActivity = MainActivity.handler.obtainMessage();
                        mesToActivity.what = MainActivity.UPDATE;
                        Bundle bundle = new Bundle();
                        bundle.putString(MainActivity.KEY_NAME, m.getName());
                        bundle.putString(MainActivity.KEY_MES, m.getMessage());
                        bundle.putString(MainActivity.KEY_PATH, path);
                        mesToActivity.setData(bundle);
                        MainActivity.handler.sendMessage(mesToActivity);

                        Log.e("MyLog", "Сообщение в активити отправлено.");

                        try {
                            byteArray.close();
                            byteArray = null;
                        } catch (IOException e) {e.printStackTrace();}
                    }
                } catch (IOException e) {e.printStackTrace();}
            }
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            Log.e("MyLog", "onStartCommand");

            final String name = intent.getStringExtra(MainActivity.KEY_NAME);
            final String mes = intent.getStringExtra(MainActivity.KEY_MES);
            final String path = intent.getStringExtra(MainActivity.KEY_PATH);

            if (name != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Bitmap bmp = null;
                            if(path != null){
                                bmp = BitmapFactory.decodeFile(path);
                            }

                            MyMessage message = new MyMessage(name, mes, bmp);
                            Gson gson = new Gson();
                            String jsonString = gson.toJson(message);
                            sendData(jsonString.getBytes());

                            Log.e("MyLog", "Отправил сообщение");
                        } catch (IOException e) {e.printStackTrace();}
                        catch(Exception e){e.printStackTrace();}
                    }
                }).start();
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.e("MyLog", "onDestroy");
        try {
            if(!s.isClosed())
                s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    public void sendData(byte[] data) throws Exception {

        if (s == null || s.isClosed()) {
            throw new Exception("Невозможно отправить данные. Сокет не создан или закрыт");
        }

        try {
            OutputStream os = s.getOutputStream();
            os.write(data);
            os.flush();
            os.close();
        } catch (IOException e) {
            throw new Exception("Невозможно отправить данные: "+e.getMessage());
        }
    }
}
