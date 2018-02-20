package com.example.stemmeriky.testapplication;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;
import org.opencv.android.JavaCameraView;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;
import java.util.ArrayList;
import java.util.List;

public class MainActivity_show_camera extends AppCompatActivity implements CvCameraViewListener2 {

    private static final String TAG = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;
    Mat mRgba ;
    Mat mRgbaF;
    Mat mRgbaT;
    Mat mGrey;
    Mat mFGMask;
    BackgroundSubtractorMOG2 fgbg;
    List<MatOfPoint> contours;
    Toast toast;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    fgbg =Video.createBackgroundSubtractorMOG2();
                    mRgba =new Mat();
                    mRgbaF=new Mat();
                    mRgbaT=new Mat();
                    mGrey=new Mat();
                    mFGMask=new Mat();
                    contours = new ArrayList<>();
                    mOpenCvCameraView.enableView();
                    Context context = getApplicationContext();
                    CharSequence text = "Movement detected!";
                    int duration = Toast.LENGTH_SHORT;
                    toast = Toast.makeText(context,text,duration);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity_show_camera() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.show_camera);
        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.show_camera_activity_java_surface_view);
        mOpenCvCameraView.setMaxFrameSize(400,300);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);

    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        contours.clear();
        mGrey = inputFrame.gray();
        Imgproc.cvtColor(mGrey,mRgba,Imgproc.COLOR_GRAY2RGB);
        fgbg.apply(mRgba,mFGMask,0.1);
        Imgproc.erode(mFGMask, mFGMask, new Mat());
        Imgproc.dilate(mFGMask, mFGMask, new Mat());
        Imgproc.findContours(mFGMask, contours, new Mat(), Imgproc.RETR_EXTERNAL , Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.drawContours(mRgba, contours, -1, new Scalar(255,0,0),1);

        if(contours.size()>700){
            System.out.println(contours.size());
            toast.show();
        }

        return mRgba; //
    }
}