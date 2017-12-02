package com.videolive;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import com.library.Publish;
import com.library.stream.upd.UdpSend;

public class Send extends AppCompatActivity {
    private Publish publish;
    private Button tuistar;
    private Button rot;
    private Button record;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        tuistar = findViewById(R.id.tuistar);
        rot = findViewById(R.id.rot);
        record = findViewById(R.id.record);

        publish = new Publish.Buider(this, (TextureView) findViewById(R.id.textureView))
                .setPushMode(new UdpSend(getIntent().getExtras().getString("url"), getIntent().getExtras().getInt("port")))
                .setFrameRate(getIntent().getExtras().getInt("framerate"))
                .setVideoCode(getIntent().getExtras().getString("videoCode"))
                .setIsPreview(getIntent().getExtras().getBoolean("ispreview"))
                .setBitrate(getIntent().getExtras().getInt("bitrate"))
                .setBitrateVC(getIntent().getExtras().getInt("bitrate_vc"))
                .setPublishSize(getIntent().getExtras().getInt("pu_width"), getIntent().getExtras().getInt("pu_height"))
                .setPreviewSize(getIntent().getExtras().getInt("pr_width"), getIntent().getExtras().getInt("pr_height"))
                .setCollectionSize(getIntent().getExtras().getInt("c_width"), getIntent().getExtras().getInt("c_height"))
                .setRotate(getIntent().getExtras().getBoolean("rotate"))
                .setVideoPath(Environment.getExternalStorageDirectory().getPath() + "/VideoLive.mp4")
                .build();

        tuistar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tuistar.getText().toString().equals("开始推流")) {
                    publish.star();
                    tuistar.setText("停止推流");
                } else {
                    publish.stop();
                    tuistar.setText("开始推流");
                }
            }
        });

        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (record.getText().toString().equals("开始录制")) {
                    publish.starRecode();
                    record.setText("停止录制");
                } else {
                    publish.stopRecode();
                    record.setText("开始录制");
                }
            }
        });

        rot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                publish.rotate();
            }
        });
    }

    @Override
    protected void onDestroy() {
        publish.destroy();
        super.onDestroy();
    }
}
