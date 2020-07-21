package com.example.opencvappeye;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2, View.OnClickListener{

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:{
                    Log.i(TAG, "OpenCV loaded successfully");
                    _cameraBridgeViewBase.enableView();
                    break;
                }
                default:{
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };
    static{
        System.loadLibrary("native-lib");
    }
    private static final String TAG = "OCVSample::Activity";


    private CameraBridgeViewBase _cameraBridgeViewBase;

    Button confirmBtn, startBtn, deteBtn;
    ImageView role,road;
    TextView crack;
    ConstraintLayout mainLay;
    LinearLayout btnLay;

    JumpAnim jumpAnim = null;
    MoveLeftAnim moveLeftAnim = null;
    AnimationDrawable drawAnim;

    //data
    Point windowSize = new Point();//屏幕大小
    Rect rect = new Rect();//跳动组件大小
    int[] pos = new int[2];//跳动组件位置
    int crackWidth = 0; //裂缝宽度
    long startTime;
    boolean running = false;
    boolean isOpen = false;

    private Mat mRgba;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.CAMERA},1);

        initdView();
        setViewPro();
    }

    public void initdView(){
        //相机
        _cameraBridgeViewBase = findViewById(R.id.cameraView);
        //游戏元素
        road = findViewById(R.id.road);
        role = findViewById(R.id.role);
        crack = findViewById(R.id.crack);
        //布局
        mainLay = findViewById(R.id.layout);
        btnLay = findViewById(R.id.btnLayout);
        //按钮
        confirmBtn = findViewById(R.id.confirmBtn);
        startBtn = findViewById(R.id.startBtn);
        deteBtn = findViewById(R.id.affirmBtn);
        //获得窗口大小
        Display defaultDisplay = getWindowManager().getDefaultDisplay();
        defaultDisplay.getSize(windowSize);
    }

    public void setViewPro(){
        _cameraBridgeViewBase.setCvCameraViewListener(this);
        _cameraBridgeViewBase.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        _cameraBridgeViewBase.setMaxFrameSize(320,240);

        _cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        mainLay.setVisibility(SurfaceView.VISIBLE);//VISIBLE
        confirmBtn.setVisibility(SurfaceView.INVISIBLE);

        confirmBtn.setOnClickListener(this);
        startBtn.setOnClickListener(this);
        deteBtn.setOnClickListener(this);

        drawAnim = (AnimationDrawable) role.getBackground();
    }

    @Override public void onClick(View v) {
        switch (v.getId()) {
            case R.id.affirmBtn: {
                btnLay.setVisibility(SurfaceView.INVISIBLE);
                mainLay.setVisibility(SurfaceView.INVISIBLE);
                confirmBtn.setVisibility(SurfaceView.VISIBLE);
                break;
            }
            case R.id.startBtn:{
                crack.setText("");
                btnLay.setVisibility(SurfaceView.INVISIBLE);

                role.getLocationOnScreen(pos);//位置
                role.getLocalVisibleRect(rect);//大小
                jumpAnim = new JumpAnim(role,100,1000, pos[1]);
                moveLeftAnim = new MoveLeftAnim(road, crack, windowSize.x, 20000);
                newCrack();
                Log.i("test", String.format("SIZEROLA x:%d y:%d w:%d h:%d  ms", pos[0], pos[1], rect.width(), rect.height()));

                jumpAnim.setListener(new MListener() {
                    @Override
                    public void onEnd() {
                        int passLen = (int)(windowSize.x * (System.currentTimeMillis() - startTime)/20000.0);
                        btnLay.setVisibility(SurfaceView.VISIBLE);
                        moveLeftAnim.clearAnim();
                        Log.i("iff", "结束距离 ：" + passLen);
                        if (passLen < rect.width() + crackWidth || passLen > windowSize.x){
                            crack.setText("失败");
                        } else {
                            crack.setText("通过");
                        }
                        drawAnim.stop();
                        running = false;
                    }
                });
                moveLeftAnim.setListener(new MListener() {
                    @Override
                    public void onEnd() {
                        Log.i("iff", "结束 ：moveLeftAnim->onEnd");
                        btnLay.setVisibility(SurfaceView.VISIBLE);
                        moveLeftAnim.clearAnim();
                        drawAnim.stop();
                        running = false;
                    }
                });
                running = true;
                break;
            }
            case R.id.confirmBtn: {
                if (calibration(2,45)){
                    confirmBtn.setVisibility(SurfaceView.INVISIBLE);
                    btnLay.setVisibility(SurfaceView.VISIBLE);
                    mainLay.setVisibility(SurfaceView.VISIBLE);
                }
                break;
            }
        }
    }

    @Override public void onPause()
    {
        super.onPause();
        disableCamera();
    }

    private void disableCamera() {
    }

    @Override public void onResume()
    {
        super.onResume();
        if(!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV library not found!");
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "permission was granted, yay!");
                } else {
                    Toast.makeText(MainActivity.this, "Permission denied to read it.", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (_cameraBridgeViewBase != null)
            _cameraBridgeViewBase.disableView();
    }

    @Override public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC1);//CV_8UC4
        bitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
        initcascade();
    }

    @Override public void onCameraViewStopped() {
    }

    //数据处理
    @Override public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        long startTime = System.currentTimeMillis(); //起始时间
        mRgba = inputFrame.rgba();
        Utils.matToBitmap(mRgba, bitmap);

        if (!running){
            Bitmap reBitmap = detectEyes(bitmap);
            Mat imgMat = new Mat ( reBitmap.getHeight(), reBitmap.getWidth(), CvType.CV_8UC1, new Scalar(0));//CV_8UC2
            Utils.bitmapToMat(reBitmap, imgMat);
            Log.i("time", String.format("onCameraFrame time %d ms", System.currentTimeMillis() - startTime));
            return imgMat;
        }
        else {
            boolean eyes = blink(bitmap);
            if (eyes) jump();
            Log.i("eyeNum", String.format("blink -> eyes %b", eyes));
            Log.i("time", String.format("onCameraFrame time %d ms", System.currentTimeMillis() - startTime));
            return mRgba;
        }
    }

    public void initcascade(){
        File file = null;//转移haarcascade_eye.xml文件，在CPP中打开
        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);//私有
        InputStream inputStream = getResources().openRawResource(R.raw.haarcascade_eye);//打开资源文件，输入流
        try {
            file = new File(cascadeDir, "haarcascade_eye3.xml");//创建文件
            if (!file.exists()){
                FileOutputStream os = new FileOutputStream(file);//输出
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);//资源文件数据拷贝到新文件中
                }
                os.close();
            }
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        loadCacade(file.getAbsolutePath());
    }

    public void jump(){
        jumpAnim.startAnim();
        if (!isOpen){
            startTime = System.currentTimeMillis();
            moveLeftAnim.startAnim();
            drawAnim.start();
            isOpen = true;
        }
    }

    public void newCrack(){
//        Log.i("test", String.format("%d %d %d %d  ms", pos[0], pos[1], pos[0]+rect.width(), pos[1]+rect.height()));

        int maxWidth = windowSize.x - 2*rect.width();
        int minWidth = 2*rect.width();
        crackWidth = (int) (Math.random()*(maxWidth - minWidth) + minWidth);
        moveLeftAnim.randPos(rect.width(), crackWidth);//设置位置

        isOpen = false;//重新设置
    }

    public static native boolean calibration(int num, int size);
    public static native boolean blink(Bitmap bitmap);
    public static native Bitmap detectEyes(Bitmap bitmap);
    public static native void loadCacade(String filePath);
}
