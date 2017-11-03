package com.videolive;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import com.library.stream.upd.UdpSend;
import com.library.vd.Publish;
import com.library.vd.VDEncoder;

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
                .setUrl("192.168.2.106")
                .setPort(8765)
                .setPushMode(new UdpSend())
                //如果socket已经创建需要使用已经有的socket
                //.setPushMode(new UdpSend(socket))
                //自定义UPD包发送
//                .setUdpControl(new UdpControlInterface() {
//                    @Override
//                    public byte[] Control(byte[] bytes) {//bytes为udp包数据
//                        Log.d("udp_send", bytes.length + "--" + Arrays.toString(bytes));
//                        return bytes;//将自定义后的udp包数据返回
//                    }
//                })
                .setFrameRate(15)//帧率
                .setVideoCode(VDEncoder.H264)//编码方式
                .setIsPreview(true)//是否需要显示预览(如需后台推流必须设置false),如果设置false，则构建此Buider可以调用单参数方法Publish.Buider(context)
                .setBitrate(600 * 1024)//比特率
                .setPreviewSize(480, 320)//分辨率，如果系统不支持会自动选取最相近的
                .setRotate(true)//是否为前置摄像头,默认后置
                .setVideoPath(Environment.getExternalStorageDirectory().getPath() + "/VideoLive.mp4")//录制文件位置,如果为空则每次录制以当前时间命名
                .build();

        /**
         *   自定义发送协议：
         *   上面发送方式为UDP,如果需要自定义发送方式，需要新建类并继承BaseSend。
         *   另外注意如果设置了自定义UPD包发送，要在发送包之前做处理，如下
         if (udpControl != null) {
         udpControl.Control(buffvideo.array(), 0, buffvideo.position());
         }
         */

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
