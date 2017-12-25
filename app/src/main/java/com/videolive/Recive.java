package com.videolive;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.library.live.Player;
import com.library.live.stream.upd.UdpRecive;
import com.library.live.view.PlayerView;

public class Recive extends AppCompatActivity {
    private Player player;
    private Button jiestar;
    private Button record;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recive);

        //需要让服务器知道自己是接收方并且知道自己的IP，这个自行完成

        jiestar = findViewById(R.id.jiestar);
        record = findViewById(R.id.record);

        player = new Player.Buider((PlayerView) findViewById(R.id.playerView))
                .setPullMode(new UdpRecive(getIntent().getExtras().getInt("port")))
                .setVideoCode(getIntent().getExtras().getString("videoCode"))
                .setVideoPath(Environment.getExternalStorageDirectory().getPath() + "/VideoLive.mp4")
                .build();

        jiestar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (jiestar.getText().toString().equals("开始播放")) {
                    player.start();
                    jiestar.setText("停止播放");
                } else {
                    jiestar.setText("开始播放");
                    player.stop();
                }
            }
        });

        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (record.getText().toString().equals("开始录制")) {
                    player.startRecode();
                    record.setText("停止录制");
                } else {
                    player.stopRecode();
                    record.setText("开始录制");
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        player.destroy();
        super.onDestroy();
    }
}
