package com.library.live.view;

import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.library.util.mLog;

/**
 * Created by android1 on 2018/1/4.
 */

public class WeightRunnable implements Runnable {
    private double weight;
    private RelativeLayout layout;
    private View view;

    public WeightRunnable(RelativeLayout layout, View view) {
        this.view = view;
        this.layout = layout;
    }

    @Override
    public void run() {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (layout.getHeight() * weight > layout.getWidth()) {
            lp.width = layout.getWidth();
            lp.height = (int) (layout.getWidth() / weight);
        } else {
            lp.width = (int) (layout.getHeight() * weight);
            lp.height = layout.getHeight();
        }
        mLog.log("View_Size", lp.width + "--" + lp.height);
        view.setLayoutParams(lp);
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
