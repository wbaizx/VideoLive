# VideoLive
视频直播库，视频H264,H265硬编码，音频AAC编码，使用UDP协议提供实时预览，传输和解码播放以及本地录制。

Add it in your root build.gradle at the end of repositories:

Step 1：

	  allprojects {
		  repositories {
			  ...
			  maven { url 'https://jitpack.io' }
		  }
	  }
 
 
Step 2：

	dependencies {
	        compile 'com.github.wbaizx:VideoLive:1.0.1'
	}


    视频直播采用UDP协议发送，自行设置url和port。（需要5.0以上。仅支持硬编码）

    服务器端需要事先知道接收方的IP（自行设置）。
    
    
    使用示例：
    
     推流端：
 
    <TextureView
        android:id="@+id/textureView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_weight="1" />
        
        
        publish = new Publish.Buider(this, (TextureView) findViewById(R.id.textureView))
                .setUrl("192.168.2.106")
                .setPort(8765)
                .setPushMode(new UdpSend())
                //如果socket已经创建需要使用已经有的socket
                //.setPushMode(new UdpSend(socket))
                //自定义UPD包发送
                .setUdpControl(new UdpControlInterface() {
                    @Override
                    public byte[] Control(byte[] bytes) {//bytes为udp包数据
                        Log.d("udp_send", bytes.length + "--" + Arrays.toString(bytes));
                        return bytes;//将自定义后的udp包数据返回
                    }
                })
                .setFrameRate(15)//帧率
                .setVideoCode(VDEncoder.H264)//编码方式
                .setIsPreview(true)//是否需要显示预览(如需后台推流必须设置false),如果设置false，则构建此Buider可以调用单参数方法Publish.Buider(context)
                .setBitrate(600 * 1024)//比特率
                .setPreviewSize(480, 320)//分辨率，如果系统不支持会自动选取最相近的
                .setRotate(true)//是否为前置摄像头,默认后置
                .setVideoPath(Environment.getExternalStorageDirectory().getPath() + "/VideoLive.mp4")//录制文件位置,如果为空则每次录制以当前时间命名
                .build();

      然后在需要推流的地方调用
        
         publish.star();
         
      停止推流
         
         publish.stop();

      推流过程中可调用以下方法
         
        publish.rotate();//旋转相机

        publish.starRecode();//停止录制

        publish.stopRecode();//开始录制

      最后销毁资源
         
         publish.destroy();

        /**
         *   上面发送方式为UDP,如果需要自定义发送方式，需要新建类并继承BaseSend。
         *   另外注意如果设置了自定义UPD包发送，要在发送包之前做处理，如下
         if (udpControl != null) {
         udpControl.Control(buffvideo.array(), 0, buffvideo.position());
         }
         */



      接收端：
      
        <SurfaceView
        android:id="@+id/textureView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_weight="1" />
        
          //需要让服务器知道自己是接收方并且知道自己的IP，这个自行完成
          
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
                .setUdpControl(new UdpControlInterface() {
                    @Override
                    public byte[] Control(byte[] bytes) {//bytes为接收到的原始数据
                        Log.d("udp_recive", bytes.length + "--" + Arrays.toString(bytes));
                        return bytes;//在这里将发送时的自定义处理去掉后返回
                    }
                })
                .setVideoCode(VDDecoder.H264)//设置解码方式
                .setVideoSize(320, 480)//分辨率，由于图片旋转过，所以高度宽度需要对调
                .setVideoPath(Environment.getExternalStorageDirectory().getPath() + "/VideoLive.mp4")//录制文件位置,如果为空则每次录制以当前时间命名
                .build();


        调用播放
          
           player.star();
           
        停止播放
          
           player.stop();

      播放过程中可调用以下方法（必须在已经开始渲染后才能调用录制）

        player.starRecode();//停止录制

        player.stopRecode();//开始录制

        销毁资源

           player.destroy();

        /**
         * 上面接收方式为UDP,如果需要自定义接收方式，需要新建类并继承BaseRecive。注意在包含解码器需要的配置信息的地方
         * 调用getInformation(byte[] important)给解码器（important为包含解码器需要的配置信息的视频帧数据，可以不完整）
         * 然后注意一下缓存策略
         */
	 
	 最后记住获取相关权限
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.INTERNET" />

    
