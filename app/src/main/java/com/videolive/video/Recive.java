package com.videolive.video;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.library.live.Player;
import com.library.live.stream.UdpRecive;
import com.library.live.view.PlayerView;
import com.videolive.R;

public class Recive extends AppCompatActivity {
    private Player player;
    private Button jiestar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recive);

        //需要让服务器知道自己是接收方并且知道自己的IP，这个自行完成

        jiestar = findViewById(R.id.jiestar);

        player = new Player.Buider((PlayerView) findViewById(R.id.playerView))
                .setPullMode(new UdpRecive(getIntent().getExtras().getInt("port")))
                .setVideoCode(getIntent().getExtras().getString("videoCode"))
                .setMultiple(getIntent().getExtras().getInt("multiple"))
                .setCenterScaleType(true)
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
    }

    @Override
    protected void onDestroy() {
        player.destroy();
        super.onDestroy();
    }
}
