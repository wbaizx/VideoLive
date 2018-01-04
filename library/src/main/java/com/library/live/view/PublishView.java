package com.library.live.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.widget.RelativeLayout;

import com.library.R;

/**
 * Created by android1 on 2018/1/3.
 */

public class PublishView extends RelativeLayout {
    private boolean isCenterScaleType = false;
    private boolean isShould = false;
    private TextureView textureView;
    private Handler handler;
    private WeightRunnable weightRunnable;

    public PublishView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.publish_view, this, true);
        setBackgroundColor(ContextCompat.getColor(context, R.color.black));
        textureView = findViewById(R.id.textureView);
        handler = new Handler(Looper.getMainLooper());
        weightRunnable = new WeightRunnable(this, textureView);
    }

    public void setCenterScaleType(boolean centerScaleType) {
        isCenterScaleType = centerScaleType;
    }

    public void setSurfaceTextureListener(TextureView.SurfaceTextureListener listener) {
        textureView.setSurfaceTextureListener(listener);
    }

    public SurfaceTexture getSurfaceTexture() {
        return textureView.getSurfaceTexture();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (isShould) {
            isShould = false;
            handler.post(weightRunnable);
        }
    }

    public void setWeight(double weight) {
        if (isCenterScaleType) {
            weightRunnable.setWeight(weight);
            if (getWidth() != 0 && getHeight() != 0) {
                handler.post(weightRunnable);
            } else {
                isShould = true;
            }
        }
    }
}
