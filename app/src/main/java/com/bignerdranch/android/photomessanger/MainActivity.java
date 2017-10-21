package com.bignerdranch.android.photomessanger;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.*;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.zip.Inflater;

/*
* Ошибка иногда в том, что с сервера отправляется картинка на 500 000,
* а в телефон иногда приходит на много меньше, около 10 000
*
* Иногда ошибка в том что в методе "handleMessage"  при декодировке битмапа с карты памяти
* получается нулевой битмап, хотя он там лежит на самом деле, существует
*
* А иногда все прекрасно работает, причем я ничего не меняю.
*/

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    ImageButton btnSendMes, btnSendImg;
    RecyclerView recView;
    EditText etName, etMessage;

    public static final int REQUEST_CODE_PHOTO = 1111;
    public static final int UPDATE = 2222;
    public static final String KEY_NAME = "KEY_NAME";
    public static final String KEY_MES = "KEY_MES";
    public static final String KEY_PATH = "KEY_PATH";
    public static File sdPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/aPhotoMes");
    public static Handler handler;
    private ArrayList<MyMessage> mMessages = new ArrayList<>();
    MessageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSendMes = (ImageButton) findViewById(R.id.btnSendMes);
        btnSendImg = (ImageButton) findViewById(R.id.btnPhoto);
        recView = (RecyclerView) findViewById(R.id.recyclerView);
        etName = (EditText) findViewById(R.id.et_name);
        etMessage = (EditText) findViewById(R.id.et_message);
        btnSendMes.setOnClickListener(this);
        btnSendImg.setOnClickListener(this);

        handler = new MyHandler();
        Intent intent = new Intent(this, MessangerService.class);
        startService(intent);

        adapter = new MessageAdapter(mMessages);
        recView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recView.setAdapter(adapter);

        if(!sdPath.exists()){
            sdPath.mkdir();
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btnSendMes:
                onMessageBtnClick();
                break;
            case R.id.btnPhoto:
                onPhotoBtnClick();
                break;
        }
    }

    public void sendMessage(String name, String mes, String path){
        Log.e("MyLog", "sendMessage");
        Intent intent = new Intent(this, MessangerService.class);
        intent.putExtra(KEY_NAME, name);
        intent.putExtra(KEY_MES, mes);
        intent.putExtra(KEY_PATH, path);
        startService(intent);
    }

    public void onMessageBtnClick(){
        Log.e("MyLog", "onMessageBtnClick");
        String name = etName.getText().toString();
        String messageText = etMessage.getText().toString();
        sendMessage(name, messageText, null);
    }

    public void onPhotoBtnClick(){
        Log.e("MyLog", "onPhotoBtnClick");
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CODE_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("MyLog", "onActivityResult");
        if (requestCode == REQUEST_CODE_PHOTO){
            if (resultCode == RESULT_OK){
                if(data == null) return;
                else {
                    if(data.getExtras() != null){
                        Object obj = data.getExtras().get("data");
                        if(obj instanceof Bitmap){
                            Bitmap bitmap = (Bitmap) obj;
                            String name = etName.getText().toString();
                            String messageText = etMessage.getText().toString();
                            String path = SaveImage(bitmap);
                            sendMessage(name, messageText, path);
                        }
                    }
                }
            }
        }
    }

    public class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.e("MyLog", "handleMessage");
            int what = msg.what;
            if(what == MainActivity.UPDATE){
                Bundle bundle = msg.getData();
                String name = bundle.getString(KEY_NAME);
                String mes = bundle.getString(KEY_MES);
                String path = bundle.getString(KEY_PATH);

                MyMessage myMessage;
                if(path == null){
                    Log.e("MyLog", "No Bitmap");
                    myMessage = new MyMessage(name, mes, null);
                }else {
                    Bitmap bmp = null;
                    Log.e("MyLog", "Bitmap enabled, path = " + path);
                    BitmapFactory.Options options;
                    try{
                         bmp = BitmapFactory.decodeFile(path);
                    }catch (OutOfMemoryError e) {
                        options = new BitmapFactory.Options();
                        options.inSampleSize = 2;
                        bmp = BitmapFactory.decodeFile(path, options);

                        Log.e("MyLog", "Zashlo");
                    }
                    if(bmp == null)
                        Log.e("MyLog", "Bitmap is null");
                    myMessage = new MyMessage(name, mes, bmp);
                }
                Log.e("MyLog", "MyMessage created");
                mMessages.add(myMessage);
                updateRecyclerView();
            }
        }
    }

    public class MessageHolder extends RecyclerView.ViewHolder{
        MyMessage mMes;
        TextView tvName, tvMessage;
        ImageView imgView;

        public MessageHolder(View itemView) {
            super(itemView);
            Log.e("MyLog", "MessageHolder");
            tvName = (TextView) itemView.findViewById(R.id.tvName);
            tvMessage = (TextView) itemView.findViewById(R.id.tvMessage);
            imgView = (ImageView) itemView.findViewById(R.id.imageView);
        }
        public void bindViewHolder(MyMessage mes) {
            this.mMes = mes;
            Log.e("MyLog", "bindViewHolder");
            if(mMes.getImage() != null)
                imgView.setImageBitmap(mMes.getImage());
            tvName.setText(mMes.getName());
            tvMessage.setText(mMes.getMessage());
        }
    }
    public class MessageAdapter extends RecyclerView.Adapter<MessageHolder>{
        ArrayList<MyMessage> mMessages;

        public MessageAdapter(ArrayList<MyMessage> messages) {
            mMessages = messages;
        }

        @Override
        public MessageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getApplication());
            View view = inflater.inflate(R.layout.message_item, parent, false);
            return new MessageHolder(view);
        }

        @Override
        public void onBindViewHolder(MessageHolder holder, int position) {
            holder.bindViewHolder(mMessages.get(position));
        }

        @Override
        public int getItemCount() {
            return mMessages.size();
        }
    }
    public void updateRecyclerView(){
        adapter.notifyDataSetChanged();
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
