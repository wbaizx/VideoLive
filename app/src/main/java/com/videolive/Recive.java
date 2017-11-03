package com.videolive;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.library.stream.upd.UdpRecive;
import com.library.vd.Player;
import com.library.vd.VDDecoder;

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

        player = new Player.Buider((SurfaceView) findViewById(R.id.surfaceview))
                .setUrl("192.168.2.106")
                .setPort(8765)
                //如果使用默认接收则UdpRecive必须在设置url和port之后匿名创建
                .setPullMode(new UdpRecive())
                //如果程序中其他位置已经使用了相同端口的socket，需要自行接收数据并送入解码器
                /**
                 * 自行接收数据
                 * 1.UdpRecive udpRecive = new UdpRecive();(此时参数url和port可以不设)
                 * 2.设置setPullMode方法参数为UdpRecive实例。 setPullMode(udpRecive)
                 * 3.得到数据后调用udpRecive.write(bytes); 注意bytes的处理不要和setUdpControl冲突(只用处理一方)
                 */
//                .setUdpControl(new UdpControlInterface() {
//                    @Override
//                    public byte[] Control(byte[] bytes) {//bytes为接收到的原始数据
//                        Log.d("udp_recive", bytes.length + "--" + Arrays.toString(bytes));
//                        return bytes;//在这里将发送时的自定义处理去掉后返回
//                    }
//                })
                .setVideoCode(VDDecoder.H264)//设置解码方式
                .setVideoSize(320, 480)//分辨率，由于图片旋转过，所以高度宽度需要对调
                .setVideoPath(Environment.getExternalStorageDirectory().getPath() + "/VideoLive.mp4")//录制文件位置,如果为空则每次录制以当前时间命名
                .build();

        /**
         * 上面接收方式为UDP,如果需要自定义接收方式，需要新建类并继承BaseRecive。注意在包含解码器需要的配置信息的地方
         * 调用getInformation(byte[] important)给解码器（important为包含解码器需要的配置信息的视频帧数据，可以不完整）
         * 然后注意一下缓存策略
         */
        jiestar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (jiestar.getText().toString().equals("开始播放")) {
                    player.star();
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
                    player.starRecode();
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
