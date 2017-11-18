package com.library.view;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.library.IsOutBuffer;
import com.library.R;
import com.library.stream.IsInBuffer;


/**
 * Created by android1 on 2017/11/18.
 */

public class PlayerView extends RelativeLayout implements IsInBuffer {
    private ImageView loadimag;
    private SurfaceView surfaceview;
    private TextView loadtext;
    private ObjectAnimator rota;
    private IsOutBuffer isOutBuffer;
    private boolean bufferAnimator = true;//缓冲动画标志，默认开启
    private Handler handler;
    private UIRunnable uiRunnable;

    public PlayerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.player_view, this, true);//引用布局文件，使用XML文件来当做布局更加方便直观一些，使用了merge标签就必须使用这种方法加载布局
        loadimag = findViewById(R.id.loadimag);
        surfaceview = findViewById(R.id.surfaceview);
        loadtext = findViewById(R.id.loadtext);

        rota = ObjectAnimator.ofFloat(loadimag, "rotation", 0f, 360f);
        rota.setDuration(1000);//设置动画时间
        rota.setInterpolator(new LinearInterpolator());//设置动画插入器
        rota.setRepeatCount(-1);//设置动画重复次数，这里-1代表无限

        handler = new Handler(context.getMainLooper());//获取主线程消息队列，用于更新UI
        uiRunnable = new UIRunnable();//初始化执行体
    }

    public SurfaceHolder getHolder() {
        return surfaceview.getHolder();
    }

    @Override
    public void isBuffer(boolean isBuffer) {
        if (isOutBuffer != null) {
            isOutBuffer.isBuffer(isBuffer);
        }
        if (bufferAnimator) {
            uiRunnable.setBuffer(isBuffer);
            handler.post(uiRunnable);
        }
    }

    /*
    主线程消息队列执行的Runnable
     */
    private class UIRunnable implements Runnable {
        private boolean isBuffer;

        @Override
        public void run() {
            if (isBuffer) {
                if (loadimag.getVisibility() == GONE) {
                    loadimag.setVisibility(VISIBLE);
                    loadtext.setVisibility(VISIBLE);
                    rota.start();//启动动画
                }
            } else {
                if (loadimag.getVisibility() == VISIBLE) {
                    loadimag.setVisibility(GONE);
                    loadtext.setVisibility(GONE);
                    rota.cancel();//关闭动画
                }
            }
        }

        public void setBuffer(boolean buffer) {
            isBuffer = buffer;
        }
    }

    public void setIsOutBuffer(IsOutBuffer isOutBuffer) {
        this.isOutBuffer = isOutBuffer;
    }

    public void setBufferAnimator(boolean bufferAnimator) {
        this.bufferAnimator = bufferAnimator;
    }

    public void stop() {
        uiRunnable.setBuffer(false);
        handler.post(uiRunnable);
    }
}
