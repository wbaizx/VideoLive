package com.videolive.voice;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.library.talk.Listen;
import com.library.talk.Speak;
import com.library.talk.stream.ListenRecive;
import com.library.talk.stream.SpeakSend;
import com.videolive.R;

import java.net.DatagramSocket;
import java.net.SocketException;

public class Voice extends AppCompatActivity {
    private Button startVoice;
    private Button reciveVoice;
    private Speak speak;
    private Listen listen;
    private DatagramSocket socket = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_send);

        startVoice = findViewById(R.id.startVoice);
        reciveVoice = findViewById(R.id.reciveVoice);

        try {
            socket = new DatagramSocket(getIntent().getExtras().getInt("port"));
        } catch (SocketException e) {
            e.printStackTrace();
        }

        speak = new Speak.Buider()
                .setPushMode(new SpeakSend(socket, getIntent().getExtras().getString("url"), getIntent().getExtras().getInt("port")))
                .setPublishBitrate(getIntent().getExtras().getInt("publishbitrate_vc"))
                .setVoicePath(Environment.getExternalStorageDirectory().getPath() + "/VideoTalk.mp3")
                .build();

        listen = new Listen.Buider()
                .setPullMode(new ListenRecive(socket))
                .setUdpPacketCacheMin(2)
                .setVoiceFrameCacheMin(5)
                .setMultiple(getIntent().getExtras().getInt("multiple"))
                .build();

        startVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (startVoice.getText().toString().equals("开始对讲")) {
                    speak.start();
                    speak.startRecord();
                    startVoice.setText("停止对讲");
                } else {
                    speak.stop();
                    speak.stopRecord();
                    startVoice.setText("开始对讲");
                }
            }
        });

        reciveVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (reciveVoice.getText().toString().equals("开始接收")) {
                    listen.start();
                    reciveVoice.setText("停止接收");
                } else {
                    listen.stop();
                    reciveVoice.setText("开始接收");
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        speak.destroy();
        listen.destroy();
        socket.close();
        socket = null;
        super.onDestroy();
    }
}
