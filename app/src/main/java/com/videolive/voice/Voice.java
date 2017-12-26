package com.videolive.voice;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.library.talk.Listen;
import com.library.talk.Speak;
import com.library.talk.stream.ListenRecive;
import com.library.talk.stream.SpeakSend;
import com.library.common.UdpControlInterface;
import com.videolive.R;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

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
                .setCollectionBitrate(getIntent().getExtras().getInt("collectionbitrate_vc"))
                .setPublishBitrate(getIntent().getExtras().getInt("publishbitrate_vc"))
                .setMultiple(getIntent().getExtras().getInt("multiple"))
                .setUdpControl(new UdpControlInterface() {
                    @Override
                    public byte[] Control(byte[] bytes, int offset, int length) {
                        return Arrays.copyOf(bytes, length);
                    }
                })
                .build();

        listen = new Listen.Buider()
                .setPullMode(new ListenRecive(socket))
                .setUdpPacketCacheMin(2)
                .setUdpControl(new UdpControlInterface() {
                    @Override
                    public byte[] Control(byte[] bytes, int offset, int length) {
                        return Arrays.copyOf(bytes, length);
                    }
                })
                .setVoiceFrameCacheMin(5).build();


        startVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (startVoice.getText().toString().equals("开始对讲")) {
                    speak.start();
                    startVoice.setText("停止对讲");
                } else {
                    speak.stop();
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
