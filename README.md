# VideoLive
视频直播库，视频H264,H265硬编码，音频AAC编码，使用UDP协议提供实时预览，传输和解码播放以及本地录制。
也提供单独语音对讲功能。

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
	        compile 'com.github.wbaizx:VideoLive:3.0.9'
	}


    视频直播和语音都是采用UDP协议发送，自行设置url和port。（需要5.0以上，仅支持硬编码，分包和命名不规范的地方望谅解0.0）



    相关权限

    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.INTERNET" />

    
## 视频使用示例：
    
### 推流端：
 
    <com.library.live.view.PublishView
        android:id="@+id/publishView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_weight="1"></com.library.live.view.PublishView>
        
        
        publish = new Publish.Buider(this, (PublishView) findViewById(R.id.publishView))
                .setPushMode(new UdpSend("192.168.2.106", 8765))
                .setFrameRate(15)//帧率
                .setVideoCode(VDEncoder.H264)//编码方式
                .setIsPreview(true)//是否需要显示预览(如需后台推流最好设置false，如果设置false则构建Buider可以调用单参数方法Publish.Buider(context))
                .setPublishBitrate(600 * 1024)//推流采样率
                .setCollectionBitrate(600 * 1024)//采集采样率
                .setCollectionBitrateVC(64 * 1024)//音频采集采样率
                .setPublishBitrateVC(24 * 1024)//音频推流采样率
                .setPublishSize(480, 320)//推流分辨率，不要高于采集分辨率
                .setPreviewSize(480, 320)//预览分辨率，模式为TAKEPHOTO时决定截屏分辨率
                .setCollectionSize(480, 320)//采集分辨率，决定录制文件的分辨率，模式为CONVERSION时决定截屏分辨率
                .setRotate(true)//是否为前置摄像头,默认后置
                .setScreenshotsMode(Publish.CONVERSION)//截屏模式
                .setVideoDirPath(Environment.getExternalStorageDirectory().getPath() + File.separator + "VideoLive")//录制路径,当前为默认路径
                .setPictureDirPath(Environment.getExternalStorageDirectory().getPath() + File.separator + "VideoPicture")//拍照路径,当前为默认路径
                .build();


  如果socket已经创建需要使用已经有的socket
       
       .setPushMode(new UdpSend(socket, "192.168.2.106", 8765))

  如果需要按比例显示图像

                .setCenterScaleType(true)

  如果需要添加自己的协议
       
                .setUdpControl(new UdpControlInterface() {
                    @Override
                    public byte[] Control(byte[] bytes, int offset, int length) {//bytes为udp包数据,offset为起始位,length为长度
                        //返回自定义后udp包数据,不要做耗时操作。如果调用了此方法不要将原数组返回
                        return new byte[0];
                    }
                })

  如果需要自定义发送方式，需要新建类并继承BaseSend。注意回调自定义UPD包发送，如下
        
        if (udpControl != null) {
            bytes = udpControl.Control(bytes, 0, bytes.length);
        }

  然后在需要推流的地方调用
         
	 publish.start();
         
  停止推流
         
	 publish.stop();

  推流过程中可调用以下方法
       
        publish.rotate();//旋转相机

        publish.startRecode();//开始录制

        publish.stopRecode();//停止录制

        publish.takePicture();//截屏

   截屏回调通过如下方式实现

        publish.setPictureCallback(new PictureCallback() {
            @Override
            public void Success(String path) {
                //截屏回调，path为图片路径
            }
        });

   最后销毁资源
        
	publish.destroy();



### 接收端：
      
    <com.library.live.view.PlayerView
        android:id="@+id/playerView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_weight="1" />
        
   需要让服务器知道自己是接收方并且知道自己的IP，自行完成
          
        player = new Player.Buider((PlayerView) findViewById(R.id.playerView))
                .setPullMode(new UdpRecive(8765))
                .setVideoCode(VDDecoder.H264)//设置解码方式
                .setMultiple(1)//音频调节，倍数限制为1-8倍。1为原声,放大后可能导致爆音。
                .build();

   如果程序中其他位置已经使用了相同端口的socket，需要使用UdpRecive的无参构造并手动调用player.write喂数据给解码器

        player.write(byte[]);

   如果想要控制缓存策略可以在构建时设置如下参数

                .setUdpPacketCacheMin(3)//udp包缓存数量,以音频为基准
                .setVideoFrameCacheMin(6)//视频帧达到播放条件的缓存帧数

   如果需要按比例显示图像

                .setCenterScaleType(true)

   可以关闭默认缓冲动画

                .setBufferAnimator(false)//默认开启

   如果觉得动画太丑也可以自己根据状态去做个dialog，通过如下设置回调缓冲状态

                .setIsOutBuffer(new IsOutBuffer() {
                    @Override
                    public void isBuffer(boolean isBuffer) {
                        /*
                        isBuffer为true开始缓冲，为false表示结束缓冲。
                        另外更新ui注意，此处回调在线程中。
                         */
                    }
                })


   如果需要去掉自己添加的协议
                
                .setUdpControl(new UdpControlInterface() {
                    @Override
                    public byte[] Control(byte[] bytes, int offset, int length) {//bytes为接收到的原始数据
                        return new byte[0];//在这里将发送时的自定义处理去掉后返回
                    }
                })

   如果需要自定义接收方式，需要新建类并继承BaseRecive。注意在包含解码器需要的配置信息的地方
	 
         调用getInformation(byte[] important)给解码器（important为包含解码器需要的配置信息的视频帧数据，可以不完整）

   在处理完数据后回调给解码器

            videoCallback.videoCallback(video);
            voiceCallback.voiceCallback(voice);

   动态调节音量

        player.setVoiceIncreaseMultiple();//动态调整音量(放大pcm音量，与设备音量无关)

   调用播放
   
         player.start();
	   
   停止播放
   
         player.stop();
  
   销毁资源
           
         player.destroy();

    
## 单独语音对讲使用示例：

### 发送端：

        speak = new Speak.Buider()
                .setPushMode(new SpeakSend("192.168.2.106", 8765))
                .setPublishBitrate(24 * 1024)//音频推流采样率
                .setVoiceDirPath(Environment.getExternalStorageDirectory().getPath() + File.separator + "VideoTalk")//录制路径,当前为默认路径
                .build();

  如果socket已经创建需要使用已经有的socket

                .setPushMode(new SpeakSend(socket, "192.168.2.106",8765))

  如果需要添加自己的协议同视频推流一样

                .setUdpControl(new UdpControlInterface() {
                    @Override
                    public byte[] Control(byte[] bytes, int offset, int length) {
                        return Arrays.copyOf(bytes, length);
                    }
                })

  然后在需要推流的地方调用（不需要发送语音的时候最好关闭）

        speak.start();

  停止推流

        speak.stop();

  关于录制应当与推流同步启动同步关闭

        speak.startRecode();//开始录制

        speak.stopRecode();//停止录制

  注意如果同时视频推流和语音推流会出现冲突，如果正在视频推流，则不应该启动语音推流，如果已经启动，需调用stop方法关闭。
  然后此时启动发送和关闭发送对应如下方法，此时无法录制

        speak.startJustSend();
        speak.stopJustSend();

  然后从视频的setUdpControl回调中通过音频tag取出语音数据，调用如下方法添加到发送队列

        speak.addbytes(byte[]);

  获取音量分贝大小（0-90）

        speak.getDecibel();

   最后销毁资源

        speak.destroy();



### 接收端：

        listen = new Listen.Buider()
                .setPullMode(new ListenRecive(8765))
                .setMultiple(1)//音频调节，倍数限制为1-8倍。1为原声,放大后可能导致爆音。
                .build();

  同视频推流一样如果需要需要自行传入数据，则调用ListenRecive的无参构造，然后调用write传入数据

        listenRecive.write();

  还可以传入一个接收socket

        new ListenRecive(socket)

  然后控制策略

                .setUdpPacketCacheMin(2)
                .setVoiceFrameCacheMin(5)

  包控制方式相同

                  .setUdpControl(new UdpControlInterface() {
                      @Override
                      public byte[] Control(byte[] bytes, int offset, int length) {
                          return Arrays.copyOf(bytes, length);
                      }
                  })

  动态调节音量

        listen.setVoiceIncreaseMultiple(3);//动态调整音量(放大pcm音量，与设备音量无关)

  启动接收

        listen.start();

  停止接收

        listen.stop();

  销毁资源

        listen.destroy();


  视频和语音推流时如果网络断开不需要做任何处理，网络恢复后会继续推流。
  讨厌写文档！
