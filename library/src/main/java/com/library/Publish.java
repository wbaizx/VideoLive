package com.library;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.library.file.WriteMp4;
import com.library.stream.BaseSend;
import com.library.stream.UdpControlInterface;
import com.library.util.ImagUtil;
import com.library.util.mLog;
import com.library.vc.VoiceRecord;
import com.library.vd.VDEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class Publish implements TextureView.SurfaceTextureListener {
    private Context context;
    private final int frameMax = 4;
    //帧率控制队列
    private ArrayBlockingQueue<Image> frameRateControlQueue = new ArrayBlockingQueue<>(frameMax);
    //视频编码
    private VDEncoder vdEncoder = null;
    //音频采集
    private VoiceRecord voiceRecord;
    //UDP发送类
    private BaseSend baseSend;
    //是否翻转，默认后置
    private boolean rotate = false;
    private boolean ispreview = true;
    //帧率
    private int frameRate;
    private int bitrate;
    private int bitrate_vc;
    private String codetype;
    //相机设备
    private CameraDevice cameraDevice;
    //用于获取预览数据相关
    private ImageReader imageReader;
    //用于实时显示预览
    private TextureView textureView;
    //预览分辨率
    private Size previewSize;
    //推流分辨率
    private Size publishSize;
    //采集分辨率
    private Size collectionSize;
    //控制前后摄像头
    private int facingFront;

    //异步线程
    private HandlerThread controlFrameRateThread;
    private HandlerThread handlerCamearThread;
    private Handler camearHandler;
    private Handler frameHandler;

    private WriteMp4 writeMp4;


    public Publish(Context context, TextureView textureView, boolean ispreview, Size publishSize, Size previewSize, Size collectionSize, int frameRate, int bitrate, int bitrate_vc, String codetype, boolean rotate,
                   String path, BaseSend baseSend, UdpControlInterface udpControl) {
        this.context = context;
        this.publishSize = publishSize;
        this.previewSize = previewSize;
        this.collectionSize = collectionSize;
        this.bitrate = bitrate;
        this.frameRate = frameRate;
        this.bitrate_vc = bitrate_vc;
        this.codetype = codetype;
        this.rotate = rotate;
        facingFront = rotate ? CameraCharacteristics.LENS_FACING_FRONT : CameraCharacteristics.LENS_FACING_BACK;
        this.ispreview = ispreview;
        //文件录入类
        writeMp4 = new WriteMp4(path);

        handlerCamearThread = new HandlerThread("Camear2");
        handlerCamearThread.start();
        camearHandler = new Handler(handlerCamearThread.getLooper());

        //发送实例
        this.baseSend = baseSend;
        this.baseSend.setUdpControl(udpControl);
        startControlFrameRate();

        if (ispreview) {//如果需要显示预览
            this.textureView = textureView;
            textureView.setSurfaceTextureListener(this);
        } else {
            initCamera();
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        //初始化并打开相机
        initCamera();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    /*
    初始化并打开相机
     */
    private void initCamera() {
        //获取摄像头的管理者CameraManager
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        //遍历所有摄像头
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                //查找符合当前选择的摄像头
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == facingFront) {
                    //获取StreamConfigurationMap管理摄像头支持的所有输出格式和尺寸,根据TextureView的尺寸设置预览尺寸
                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//                    previewSize = map.getOutputSizes(SurfaceTexture.class)[0];//预览分辨率直接设置为最大分辨率
                    //选取采集分辨率（未必和设置的匹配，由于摄像头不支持设置的分辨率）
                    setPublshSize(map.getOutputSizes(SurfaceTexture.class));

                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    try {
                        //打开相机
                        manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                            @Override
                            public void onOpened(@NonNull CameraDevice device) {
                                cameraDevice = device;
                                //开启预览
                                startPreview();
                            }

                            @Override
                            public void onDisconnected(@NonNull CameraDevice cameraDevice) {

                            }

                            @Override
                            public void onError(@NonNull CameraDevice cameraDevice, int i) {

                            }
                        }, null);//创建在线程
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setPublshSize(Size[] outputSizes) {
        int numw = 10000;
        int numh = 10000;
        int num = 0;
        for (int i = 0; i < outputSizes.length; i++) {
            mLog.log("Size_app", outputSizes[i].getWidth() + "--" + outputSizes[i].getHeight());
            if (Math.abs(outputSizes[i].getWidth() - publishSize.getWidth()) <= numw) {
                numw = Math.abs(outputSizes[i].getWidth() - publishSize.getWidth());
                if (Math.abs(outputSizes[i].getHeight() - publishSize.getHeight()) <= numh) {
                    numh = Math.abs(outputSizes[i].getHeight() - publishSize.getHeight());
                    num = i;
                }
            }
        }
        publishSize = outputSizes[num];
        initEncode();

        numw = 10000;
        numh = 10000;
        num = 0;
        for (int i = 0; i < outputSizes.length; i++) {
            mLog.log("Size_app", outputSizes[i].getWidth() + "--" + outputSizes[i].getHeight());
            if (Math.abs(outputSizes[i].getWidth() - previewSize.getWidth()) <= numw) {
                numw = Math.abs(outputSizes[i].getWidth() - previewSize.getWidth());
                if (Math.abs(outputSizes[i].getHeight() - previewSize.getHeight()) <= numh) {
                    numh = Math.abs(outputSizes[i].getHeight() - previewSize.getHeight());
                    num = i;
                }
            }
        }
        previewSize = outputSizes[num];

        numw = 10000;
        numh = 10000;
        num = 0;
        for (int i = 0; i < outputSizes.length; i++) {
            mLog.log("Size_app", outputSizes[i].getWidth() + "--" + outputSizes[i].getHeight());
            if (Math.abs(outputSizes[i].getWidth() - collectionSize.getWidth()) <= numw) {
                numw = Math.abs(outputSizes[i].getWidth() - collectionSize.getWidth());
                if (Math.abs(outputSizes[i].getHeight() - collectionSize.getHeight()) <= numh) {
                    numh = Math.abs(outputSizes[i].getHeight() - collectionSize.getHeight());
                    num = i;
                }
            }
        }
        collectionSize = outputSizes[num];

        mLog.log("pictureSize", "推流分辨率  =  " + publishSize.getWidth() + " * " + publishSize.getHeight());
        mLog.log("pictureSize", "预览分辨率  =  " + previewSize.getWidth() + " * " + previewSize.getHeight());
        mLog.log("pictureSize", "采集分辨率  =  " + collectionSize.getWidth() + " * " + collectionSize.getHeight());

    }

    private void initEncode() {
        if (vdEncoder == null) {
            vdEncoder = new VDEncoder(publishSize, frameRate, bitrate, writeMp4, codetype, baseSend);
            //初始化音频编码
            voiceRecord = new VoiceRecord(baseSend, bitrate_vc, writeMp4);

            vdEncoder.StartEncoderThread();
            voiceRecord.star();
        }
    }

    private void startPreview() {
        try {
            //创建CaptureRequestBuilder，TEMPLATE_PREVIEW表示预览请求
            final CaptureRequest.Builder mCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            List<Surface> surfaces = new ArrayList<>();
            //添加帧数据Target
            Surface imageReaderSurface = getImageReaderSurface();
            mCaptureRequestBuilder.addTarget(imageReaderSurface);
            surfaces.add(imageReaderSurface);
            if (ispreview) {
                //如果需要预览添加实时预览Target
                Surface textureSurface = getTextureSurface();
                mCaptureRequestBuilder.addTarget(textureSurface);
                surfaces.add(textureSurface);
            }
            //创建相机捕获会话，第一个参数是捕获数据的输出Surface列表(同时输出屏幕和输出预览)，第二个参数是CameraCaptureSession的状态回调接口，当它创建好后会回调onConfigured方法，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {

                    //设置反复捕获数据的请求，这样预览界面就会一直有数据显示
                    try {
                        session.setRepeatingRequest(mCaptureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                                super.onCaptureStarted(session, request, timestamp, frameNumber);
                            }
                        }, camearHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /*
    获取textureView的Surface
     */
    private Surface getTextureSurface() {
        SurfaceTexture mSurfaceTexture = textureView.getSurfaceTexture();
        mSurfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());//设置TextureView的缓冲区大小
        return new Surface(mSurfaceTexture);
    }

    /*
    创建ImageReader,注册回调监听（在这里获取每一帧数据）并返回Surface
     */
    private Surface getImageReaderSurface() {
        //最后一个参数代表每次最多获取几帧数据
        imageReader = ImageReader.newInstance(collectionSize.getWidth(), collectionSize.getHeight(), ImageFormat.YUV_420_888, frameMax);
        //监听ImageReader的事件，它的参数就是预览帧数据，可以对这帧数据进行处理,类似于Camera1的PreviewCallback回调的预览帧数据
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                if (frameRateControlQueue.size() >= (frameMax - 1)) {
                    //超出限制丢弃
                    frameRateControlQueue.poll().close();
                }
                frameRateControlQueue.add(reader.acquireNextImage());
            }
        }, camearHandler);
        return imageReader.getSurface();
    }

    //耗时检测
    private long time = 0;

    //帧率控制策略
    private void startControlFrameRate() {
        controlFrameRateThread = new HandlerThread("FrameRateControl");
        controlFrameRateThread.start();
        frameHandler = new Handler(controlFrameRateThread.getLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (controlFrameRateThread.isAlive()) {
                    frameHandler.postDelayed(this, 1000 / frameRate);//帧率控制时间
                }
                if (frameRateControlQueue.size() > 0) {
                    time = System.currentTimeMillis();
                    Image image = frameRateControlQueue.poll();
                    /*
                    480*320图像处理大概14ms，1280*720大概48ms
                     */
                    if (rotate) {
                        //先转成NV21再旋转图片然后交给编码器等待编码
                        vdEncoder.addFrame(ImagUtil.rotateYUV270AndMirror(
                                ImagUtil.YUV420888toNV21(image, ImagUtil.COLOR_FormatNV21), collectionSize));
                    } else {
                        //后置
                        vdEncoder.addFrame(ImagUtil.rotateYUV90(
                                ImagUtil.YUV420888toNV21(image, ImagUtil.COLOR_FormatNV21), collectionSize));
                    }
                    image.close();
                    if ((System.currentTimeMillis() - time) > (1000 / frameRate)) {
                        mLog.log("Frame_slow", "图像处理速度过慢");
                    }
                } else {
                    mLog.log("Frame_loss", "图像采集速率不够");
                }
            }
        };
        frameHandler.post(runnable);
    }

    private void releaseCamera() {
        //释放相机
        cameraDevice.close();
        cameraDevice = null;
    }


    public void starRecode() {
        writeMp4.star();
    }

    public void stopRecode() {
        writeMp4.destroy();
    }

    //旋转
    public void rotate() {
        rotate = !rotate;
        facingFront = rotate ? CameraCharacteristics.LENS_FACING_FRONT : CameraCharacteristics.LENS_FACING_BACK;
        releaseCamera();
        initCamera();
    }

    public void setWriteCallback(WriteMp4.writeCallback writeCallback) {
        writeMp4.setWriteCallback(writeCallback);
    }

    //开始
    public void star() {
        baseSend.starsend();
    }

    //停止
    public void stop() {
        baseSend.stopsend();
    }

    //销毁
    public void destroy() {
        vdEncoder.destroy();
        voiceRecord.destroy();
        baseSend.destroy();
        frameHandler.removeCallbacksAndMessages(null);
        handlerCamearThread.quitSafely();
        controlFrameRateThread.quitSafely();
        releaseCamera();
        imageReader.close();
        writeMp4.destroy();
    }

    public static class Buider {
        private TextureView textureView;
        private Context context;
        //编码参数
        private int frameRate = 15;
        private int bitrate = 600 * 1024;
        private int bitrate_vc = 20 * 1024;
        //推流分辨率
        private Size publishSize = new Size(480, 320);
        //预览分辨率
        private Size previewSize = new Size(480, 320);
        //采集分辨率
        private Size collectionSize = new Size(480, 320);
        //是否翻转，默认后置
        private boolean rotate = false;
        //设置是否需要显示预览,默认显示
        private boolean ispreview = true;
        private String codetype = VDEncoder.H264;
        //录制地址
        private String path = null;

        private BaseSend baseSend;
        private UdpControlInterface udpControl = null;

        public Buider(Context context, TextureView textureView) {
            this.context = context;
            this.textureView = textureView;
        }

        public Buider(Context context) {
            this.context = context;
        }

        //编码分辨率
        public Buider setPublishSize(int publishWidth, int publishHeight) {
            publishSize = new Size(publishWidth, publishHeight);
            return this;
        }

        public Buider setPreviewSize(int previewWidth, int previewHeight) {
            previewSize = new Size(previewWidth, previewHeight);
            return this;
        }

        public Buider setCollectionSize(int collectionWidth, int collectionHeight) {
            collectionSize = new Size(collectionWidth, collectionHeight);
            return this;
        }

        public Buider setFrameRate(int frameRate) {
            this.frameRate = Math.max(8, frameRate);//限制最小8帧
            return this;
        }

        public Buider setIsPreview(boolean ispreview) {
            this.ispreview = ispreview;
            return this;
        }

        public Buider setBitrate(int bitrate) {
            this.bitrate = bitrate;
            return this;
        }

        public Buider setVideoCode(String codetype) {
            this.codetype = codetype;
            return this;
        }


        public Buider setVideoPath(String path) {
            this.path = path;
            return this;
        }

        public Buider setRotate(boolean rotate) {
            this.rotate = rotate;
            return this;
        }

        public Buider setPushMode(BaseSend baseSend) {
            this.baseSend = baseSend;
            return this;
        }

        public Buider setUdpControl(UdpControlInterface udpControl) {
            this.udpControl = udpControl;
            return this;
        }

        public Buider setBitrateVC(int bitrate_vc) {
            this.bitrate_vc = Math.min(48 * 1024, bitrate_vc);//限制最大48，因为发送会合并5个包，过大会导致溢出
            return this;
        }

        public Publish build() {
            return new Publish(context, textureView, ispreview, publishSize, previewSize, collectionSize, frameRate, bitrate, bitrate_vc, codetype, rotate, path, baseSend, udpControl);
        }
    }
}
