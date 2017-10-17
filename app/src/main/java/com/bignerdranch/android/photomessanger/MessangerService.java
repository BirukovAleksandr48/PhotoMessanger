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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class MessangerService extends Service{
    private volatile Socket s;
    public static File sdPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/aPhotoMes");

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

                    Scanner sc = new Scanner(s.getInputStream());
                    while (true) {
                        String jsonString = sc.nextLine();
                        Log.e("MyLog", "Got a message from server. Length = " + String.valueOf(jsonString.length()));

                        Gson gson = new Gson();
                        MyMessage m = gson.fromJson(jsonString, MyMessage.class);
                        String path = null;

                        if(m.getImage() != null){
                            path = SaveImage(m.getImage());
                        }
                        Log.e("MyLog", "path = " + path);

                        Message mesToActivity = MainActivity.handler.obtainMessage();
                        mesToActivity.what = MainActivity.UPDATE;
                        Bundle bundle = new Bundle();
                        bundle.putString(MainActivity.KEY_NAME, m.getName());
                        bundle.putString(MainActivity.KEY_MES, m.getMessage());
                        bundle.putString(MainActivity.KEY_PATH, path);
                        mesToActivity.setData(bundle);
                        MainActivity.handler.sendMessage(mesToActivity);

                        Log.e("MyLog", "Сообщение в активити отправлено.");
                    }
                } catch (Exception e) {e.printStackTrace();}
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
                            Log.e("MyLog", "Размер отправляемого сообщения = " + String.valueOf(jsonString.length()));
                            sendData(jsonString);

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

    public void sendData(String data) throws Exception {

        if (s == null || s.isClosed()) {
            throw new Exception("Невозможно отправить данные. Сокет не создан или закрыт");
        }

        try {
            PrintWriter pw = new PrintWriter(s.getOutputStream());
            pw.write(data + "\n");
            pw.flush();
        } catch (IOException e) {
            throw new Exception("Невозможно отправить данные: "+e.getMessage());
        }
    }
    public static String SaveImage(Bitmap finalBitmap) {
        File file = new File(sdPath.getPath() + "/" + "photo_" + System.currentTimeMillis() + ".jpeg");
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 20, out);
            out.flush();
            out.close();
        } catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        return file.getAbsolutePath();
    }
}
