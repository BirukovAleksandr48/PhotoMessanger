package com.bignerdranch.android.photomessanger;

import android.content.Intent;
import android.graphics.Bitmap;
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
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.zip.Inflater;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    ImageButton btnSendMes, btnSendImg;
    RecyclerView recView;
    EditText etName, etMessage;

    public static final int REQUEST_CODE_PHOTO = 1111;
    public static final int UPDATE = 2222;
    public static final String KEY_JSONMES = "KEY_JSONMES";

    public static Handler handler;
    private ArrayList<MyMessage> mMessages = new ArrayList<>();
    MessageAdapter adapter;
    File directory;

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

        directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyFolder");
        if(!directory.exists()){
            directory.mkdirs();
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

    public void sendMessage(String jsonObj){
        Log.e("MyLog", "sendMessage");
        Intent intent = new Intent(this, MessangerService.class);
        intent.putExtra(KEY_JSONMES, jsonObj);
        startService(intent);
    }

    public void onMessageBtnClick(){
        Log.e("MyLog", "onMessageBtnClick");
        String name = etName.getText().toString();
        String messageText = etMessage.getText().toString();
        MyMessage message = new MyMessage(name, messageText, null);
        Gson gson = new Gson();
        String json = gson.toJson(message);
        sendMessage(json);
    }

    public void onPhotoBtnClick(){
        Log.e("MyLog", "onPhotoBtnClick");
        File file = new File(directory.getPath() + "/" + "photo_" + System.currentTimeMillis() + ".jpg");
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, file);
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
                    Bundle bndl = data.getExtras();
                    if(bndl != null){
                        Object obj = data.getExtras().get("data");
                        if(obj instanceof Bitmap){
                            Bitmap bitmap = (Bitmap) obj;
                            String name = etName.getText().toString();
                            String messageText = etMessage.getText().toString();
                            MyMessage message = new MyMessage(name, messageText, bitmap);
                            Gson gson = new Gson();
                            String json = gson.toJson(message);
                            sendMessage(json);
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
                String jsonObj = (String) msg.obj;
                Gson gson = new Gson();
                MyMessage myMessage = gson.fromJson(jsonObj, MyMessage.class);

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

}
