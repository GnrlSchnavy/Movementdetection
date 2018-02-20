package com.example.stemmeriky.testapplication;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MainActivity_show_camera_service extends Service implements CvCameraViewListener2 {

    Toast toast = null;
    private boolean serviceRunning = false;
    private final String TAG = "OnboardCamera";
    private List<MatOfPoint> contours;
    private int mWidth = 480;
    private int mHeight = 320;
    private Mat mFGMask;
    private BackgroundSubtractorMOG2 fgbg;
    private int threshold = 50;
    private HandlerThread mCameraHandlerThread;
    private Handler mCameraHandler;
    private ImageReader mImageReader = ImageReader.newInstance(mWidth, mHeight, ImageFormat.YUV_420_888, 2);
    private int thresholdRate = 1;

    @Override
    public void onCreate(){
        super.onCreate();
        toast = Toast.makeText(this,null,Toast.LENGTH_SHORT);
        EventBus.getDefault().register(this);
        serviceRunning = true;
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        mFGMask = new Mat();
        startProducing();
    }

    public int onStartCommand(Intent intent, int i, int x){
        Bundle extras = intent.getExtras();
        if(extras != null) {
            String data = extras.getString("Threshold"); // retrieve the data using keyName
            threshold = thresholdRate * Integer.parseInt(data);
//            System.out.println("threshold set to " + threshold);
        }
        return i;
    }

    @Override
    public void onDestroy(){
        mCameraHandler.getLooper().quitSafely();
        serviceRunning = false;
        mImageReader.close();
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    private void convertImageToMat(Mat inputFrame) {
        contours.clear();
        fgbg.apply(inputFrame,mFGMask,0.1);
        Imgproc.erode(mFGMask, mFGMask, new Mat());
        Imgproc.dilate(mFGMask, mFGMask, new Mat());
        Imgproc.findContours(mFGMask, contours, new Mat(), Imgproc.RETR_EXTERNAL , Imgproc.CHAIN_APPROX_SIMPLE);
//        System.out.println(contours.size());
        if(contours.size()>threshold){
          toast.setText("movement detected");
          toast.show();
          System.out.println("movement detected");
      }
    }



    //https://gist.github.com/camdenfullmer/dfd83dfb0973663a7974
    private static Mat imageToMat(Image image) {

            ByteBuffer buffer;
            int rowStride;
            int pixelStride;
            int width = image.getWidth();
            int height = image.getHeight();
            int offset = 0;

            Image.Plane[] planes = image.getPlanes();
            byte[] data = new byte[image.getWidth() * image.getHeight() * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
            byte[] rowData = new byte[planes[0].getRowStride()];

            for (int i = 0; i < planes.length; i++) {
                buffer = planes[i].getBuffer();
                rowStride = planes[i].getRowStride();
                pixelStride = planes[i].getPixelStride();
                int w = (i == 0) ? width : width / 2;
                int h = (i == 0) ? height : height / 2;
                for (int row = 0; row < h; row++) {
                    int bytesPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8;
                    if (pixelStride == bytesPerPixel) {
                        int length = w * bytesPerPixel;
                        buffer.get(data, offset, length);

                        // Advance buffer the remainder of the row stride, unless on the last row.
                        // Otherwise, this will throw an IllegalArgumentException because the buffer
                        // doesn't include the last padding.
                        if (h - row != 1) {
                            buffer.position(buffer.position() + rowStride - length);
                        }
                        offset += length;
                    } else {

                        // On the last row only read the width of the image minus the pixel stride
                        // plus one. Otherwise, this will throw a BufferUnderflowException because the
                        // buffer doesn't include the last padding.
                        if (h - row == 1) {
                            buffer.get(rowData, 0, width - pixelStride + 1);
                        } else {
                            buffer.get(rowData, 0, rowStride);
                        }

                        for (int col = 0; col < w; col++) {
                            data[offset++] = rowData[col * pixelStride];
                        }
                    }
                }

        }

        // Finally, create the Mat.
        Mat mat = new Mat(height + height / 2, width, CvType.CV_8UC1);
        mat.put(0, 0, data);
        return mat;
    }

    ImageReader.OnImageAvailableListener mImageAvailListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            if(serviceRunning) {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    convertImageToMat(imageToMat(image));
                    image.close();
                } catch (Exception e) {
                    if (image != null) {
                        image.close();
                    }
                }
            }

        }
    };

    private Surface mCameraRecieverSurface = mImageReader.getSurface();
    {
        mImageReader.setOnImageAvailableListener(mImageAvailListener, mCameraHandler);
    }


    public MainActivity_show_camera_service() {
        super();
        mCameraHandlerThread = new HandlerThread("mCameraHandlerThread");
        mCameraHandlerThread.start();
        mCameraHandler = new Handler(mCameraHandlerThread.getLooper());

    }

    @SuppressLint("MissingPermission")
    private void startProducing() {

        CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraList = cm.getCameraIdList();
            for (String cd: cameraList) {
                CameraCharacteristics mCameraCharacteristics = cm.getCameraCharacteristics(cd);

                if (mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) != CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                cm.openCamera(cd, mDeviceStateCallback, mCameraHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "CameraAccessException detected", e);
        }
    }


    private final CameraDevice.StateCallback mDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            List<Surface> surfaceList = new ArrayList<>();
            surfaceList.add(mCameraRecieverSurface);

            try {
                camera.createCaptureSession(surfaceList, mCaptureSessionStateCallback, mCameraHandler);
            } catch (CameraAccessException e) {
                Log.e(TAG, "createCaptureSession threw CameraAccessException.", e);
            }
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
        }
        @Override
        public void onError(CameraDevice camera, int error) {
        }
    };
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ThresholChangeEvent event){
        threshold= thresholdRate * event.getMessage();
//        System.out.println("Threshold set to " + threshold);
    }


    private final CameraCaptureSession.StateCallback mCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            try {
                CaptureRequest.Builder requestBuilder = session.getDevice().createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                requestBuilder.addTarget(mCameraRecieverSurface);
                session.setRepeatingRequest(requestBuilder.build(), null, mCameraHandler);

            } catch (CameraAccessException e) {
                Log.e(TAG, "createCaptureSession threw CameraAccessException.", e);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
    };


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    fgbg = Video.createBackgroundSubtractorMOG2();
                    contours = new ArrayList<>();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        return null;
    }
}



