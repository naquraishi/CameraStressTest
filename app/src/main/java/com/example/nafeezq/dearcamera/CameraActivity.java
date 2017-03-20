package com.example.nafeezq.dearcamera;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    Camera mCamera;
    SurfaceHolder mSurfaceHolder;
    static final int MEDIA_TYPE_IMAGE = 1;
    AtomicInteger count = new AtomicInteger();
    static final int PERMISSIONS_ALL = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
//        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
//        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK|PowerManager.ACQUIRE_CAUSES_WAKEUP,"INFO");
        wl.acquire();


        KeyguardManager km = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock kl = km.newKeyguardLock("name");
        kl.disableKeyguard();



        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);




        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        String[] PERMISSIONS = {Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECEIVE_BOOT_COMPLETED};
        if(!hasPermissions(this,PERMISSIONS)){
            ActivityCompat.requestPermissions(this,PERMISSIONS,PERMISSIONS_ALL);
        }

        wl.release();
    }

    private boolean hasPermissions(Context context,String ... permissions) {

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M && context !=null && permissions !=null){

            for (String permission : permissions){
                if(ActivityCompat.checkSelfPermission(context,permission) != PackageManager.PERMISSION_GRANTED){
                    return false;
                }
            }
        }
        return true;
    }


    @Override

    protected void onResume() {
        super.onResume();

        safeCameraOpen();
        SurfaceView SV = (SurfaceView) findViewById(R.id.camera_preview);
        mSurfaceHolder = SV.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    }

    @Override

    protected void onPause(){
        super.onPause();
        releaseCameraAndPreview();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCameraAndPreview();

    }

    @Override

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview

        try {

            if (mCamera != null) {

                mCamera.setPreviewDisplay(holder);
                Log.d("Camera", "SurfaceCreated..");
            }

        } catch (IOException e1) {
            e1.printStackTrace();
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        if(mCamera!=null){

            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size size = getBestPreviewSize(width,height,parameters);
            parameters.setPreviewSize(size.width,size.height);
            Camera.Size pictureSize = getBestPictureSize(parameters);
            parameters.setPictureSize(pictureSize.width,pictureSize.height);
            mCamera.setParameters(parameters);
            mCamera.setDisplayOrientation(90);
            mCamera.startPreview();
            mCamera.takePicture(null,null,mPicture);
        }

    }


    protected Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d("Camera", "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d("Camera", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("Camera", "Error accessing file: " + e.getMessage());
            }

            mCamera.startPreview();
            mCamera.takePicture(null, null, mPicture);
            Log.i("TakePicture","Picture Clicked");
        }


    };

    public File getOutputMediaFile(int type){

        File mediaStorageDir = new File(Environment.getExternalStorageDirectory().getPath() + "/AutoClickCamera/");

        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){

                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }


        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){

            count.getAndIncrement();
            String pictureCountPost = count.toString();
            pictureCountStatus(pictureCountPost);

            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + "_" + pictureCountPost + ".png");

//            Log.i("PICTURE NO: ", " "+ pictureCountPost);

            if (mediaStorageDir.isDirectory())
            {
                String[] children = mediaStorageDir.list();

                if (children.length > 3000){

                    for (int i = 0; i < children.length; i++)
                    {
                        new File(mediaStorageDir, children[i]).delete();
                    }

//                    Log.i("Pictures Deleted: ","Done");
                }
            }

        } else {
            return null;
        }

        return mediaFile;
    }

    public void pictureCountStatus(String text)
    {
        File pictureCountFile = new File("sdcard/pictureCountLog.file");
        if (!pictureCountFile.exists())
        {
            try
            {
                pictureCountFile.createNewFile();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(pictureCountFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
        releaseCameraAndPreview();
    }


    private Camera.Size getBestPictureSize(Camera.Parameters parameters) {
        Camera.Size result=null;

        for (Camera.Size size : parameters.getSupportedPictureSizes()) {
            if (result == null) {
                result=size;
            }
            else {
                int resultArea=result.width * result.height;
                int newArea=size.width * size.height;

                if (newArea > resultArea) {
                    result=size;
                }
            }
        }

        Log.i("Best Picture Size", " Width: " + result.width + " Height: " + result.height);
        return(result);
    }

    private Camera.Size getBestPreviewSize(int width, int height,
                                           Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }

        Log.i("BEST PREVIEW IS", result.width + " " + result.height);
        return(result);
    }


    private void safeCameraOpen() {
        releaseCameraAndPreview();
        if(mCamera==null){

            mCamera = Camera.open();
        }
    }

    private void releaseCameraAndPreview() {

        Log.d("Camera","RELEASECALLED");
        if (mCamera!= null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }


}

