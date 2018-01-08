package com.videolive.video;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.library.live.Publish;
import com.library.live.stream.upd.UdpSend;
import com.library.live.view.PublishView;
import com.videolive.R;

import java.io.File;

public class Send extends AppCompatActivity {
    private Publish publish;
    private Button tuistar;
    private Button rot;
    private Button record;
    private Button takePicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        tuistar = findViewById(R.id.tuistar);
        rot = findViewById(R.id.rot);
        takePicture = findViewById(R.id.takePicture);
        record = findViewById(R.id.record);

        publish = new Publish.Buider(this, (PublishView) findViewById(R.id.publishView))
                .setPushMode(new UdpSend(getIntent().getExtras().getString("url"), getIntent().getExtras().getInt("port")))
                .setFrameRate(getIntent().getExtras().getInt("framerate"))
                .setVideoCode(getIntent().getExtras().getString("videoCode"))
                .setIsPreview(getIntent().getExtras().getBoolean("ispreview"))
                .setPublishBitrate(getIntent().getExtras().getInt("publishbitrate"))
                .setCollectionBitrate(getIntent().getExtras().getInt("collectionbitrate"))
                .setCollectionBitrateVC(getIntent().getExtras().getInt("collectionbitrate_vc"))
                .setPublishBitrateVC(getIntent().getExtras().getInt("publishbitrate_vc"))
                .setPublishSize(getIntent().getExtras().getInt("pu_width"), getIntent().getExtras().getInt("pu_height"))
                .setPreviewSize(getIntent().getExtras().getInt("pr_width"), getIntent().getExtras().getInt("pr_height"))
                .setCollectionSize(getIntent().getExtras().getInt("c_width"), getIntent().getExtras().getInt("c_height"))
                .setRotate(getIntent().getExtras().getBoolean("rotate"))
                .setVideoDirPath(Environment.getExternalStorageDirectory().getPath() + File.separator + "VideoLive")
                .setPictureDirPath(Environment.getExternalStorageDirectory().getPath() + File.separator + "VideoPicture")
                .setCenterScaleType(true)
                .build();

        tuistar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tuistar.getText().toString().equals("开始推流")) {
                    publish.start();
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
                    publish.startRecode();
                    record.setText("停止录制");
                } else {
                    publish.stopRecode();
                    record.setText("开始录制");
                }
            }
        });

        takePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                publish.takePicture();
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
