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
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.library.R;

/**
 * Created by android1 on 2018/1/3.
 */

public class PublishView extends RelativeLayout {
    private boolean isCenterScaleType = false;
    private TextureView textureView;

    public PublishView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.publish_view, this, true);
        setBackgroundColor(ContextCompat.getColor(context, R.color.black));
        textureView = findViewById(R.id.textureView);
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

    public void setWeight(final double weight) {
        if (isCenterScaleType) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ViewGroup.LayoutParams lp = textureView.getLayoutParams();
                    if (getHeight() * weight > getWidth()) {
                        lp.width = getWidth();
                        lp.height = (int) (getWidth() / weight);
                    } else {
                        lp.width = (int) (getHeight() * weight);
                        lp.height = getHeight();
                    }
                    textureView.setLayoutParams(lp);
                }
            });
        }
    }
}
