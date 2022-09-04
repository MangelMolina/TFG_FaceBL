package com.example.javacvtest;

import android.util.Log;
import org.opencv.core.Core;
import java.text.DecimalFormat;

public class MedidorFPS {
    private static final String TAG               = "FpsMeter";
    private static final int    STEP              = 60;
    private static final DecimalFormat FPS_FORMAT = new DecimalFormat("0.00");

    private double              fps;
    private int                 mFramesCouner;
    private double              mFrequency;
    private long                mprevFrameTime;
    private String              mStrfps;
    boolean                     mIsInitialized = false;
    int                         mWidth = 0;
    int                         mHeight = 0;

    public void init() {
        mFramesCouner = 0;
        mFrequency = Core.getTickFrequency();
        mprevFrameTime = Core.getTickCount();
        mStrfps = "";
        fps = 0;
    }

    public void measure() {
        if (!mIsInitialized) {
            init();
            mIsInitialized = true;
        } else {
            mFramesCouner++;
            if (mFramesCouner % STEP == 0) {
                long time = Core.getTickCount();
                fps = STEP * mFrequency / (time - mprevFrameTime);
                mprevFrameTime = time;
                if (mWidth != 0 && mHeight != 0)
                    mStrfps = FPS_FORMAT.format(fps) + " FPS@" + Integer.valueOf(mWidth) + "x" + Integer.valueOf(mHeight);
                else
                    mStrfps = Math.round(fps*100)/100d + " FPSX";
                Log.i(TAG, mStrfps);
            }
        }
    }

    public void setResolution(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public int getFPS(){
        return (int) fps;
    }
}
