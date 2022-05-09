package com.example.textrecognitionyolo.handrecog_j;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.textrecognitionyolo.FacialExpressionRecognition;
import com.example.textrecognitionyolo.R;
import com.example.textrecognitionyolo.objectDetectorClass;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.photo.CalibrateDebevec;
import org.opencv.photo.MergeDebevec;
import org.opencv.photo.MergeMertens;
import org.opencv.photo.Photo;
import org.opencv.photo.Tonemap;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class OpenCVCameraActivityJava extends AppCompatActivity implements CvCameraViewListener2, View.OnTouchListener {

    //Prefixes for logging success and failure messages
    private static final String TAG = "OCVSample::Activity";

    //Loads camera view of OpenCV for us to use. This lets us see using OpenCV
    private JavaCameraView mOpenCvCameraView;

    //Preview Builder which changes exposure (i think)
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private long exposureTime = 1000, frameDuration = 1000;
    private int sensitivity = 200;
    Bitmap b;

    //OPENCV Variables
    Mat matRGBA;
    Mat matGray;

    private FacialExpressionRecognition facialExpressionRecognition;

    private com.example.textrecognitionyolo.objectDetectorClass objectDetectorClass;

    public OpenCVCameraActivityJava() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_open_cvcamera_java);

        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // More code here ...
        } else {
            Log.e("tag", "Permissions granted");
            mOpenCvCameraView.setCameraPermissionGranted();
        }

        try{
            // input size is 300 for this model
            //objectDetectorClass=new objectDetectorClass(getAssets(),"custom_hand_model.tflite","labelmap.txt",320);
            objectDetectorClass=new objectDetectorClass(getAssets(),"model_facial_expression.tflite","labelmap.txt",320);
            Log.d("OpenCVCameraActivityJ","Model is successfully loaded");
        }
        catch (IOException e){
            Log.d("MainActivity","Getting some error");
            e.printStackTrace();
        }

        try {
            facialExpressionRecognition = new FacialExpressionRecognition(getAssets(), OpenCVCameraActivityJava.this,
                    "model_facial_expression.tflite", 48);


        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onCameraViewStarted(int width, int height) {
//        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
//        mPreviewRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, Long.valueOf(exposureTime));
//        mPreviewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, Integer.valueOf(sensitivity));
//        mPreviewRequestBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, Long.valueOf(frameDuration));
        matRGBA = new Mat(width, height, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        matRGBA.release();
    }


    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i(TAG, "onTouch event");
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentDateandTime = sdf.format(new Date());
        String fileName = Environment.getExternalStorageDirectory().getPath() +
                "/sample_picture_" + currentDateandTime + ".jpg";
       // mOpenCvCameraView.takePicture(fileName);
        Toast.makeText(this, fileName + " saved", Toast.LENGTH_SHORT).show();

        b = getBitmap(fileName);

        byte[] array = new byte[b.getWidth() * b.getHeight() * 4];
        Buffer dst = ByteBuffer.wrap(array);
        b.copyPixelsFromBuffer(dst);

        byte[] ba = new byte[dst.remaining()];

        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        matRGBA = inputFrame.rgba();
        matGray = inputFrame.gray();

        String path = Environment.getExternalStorageDirectory().getPath();
//        Mat response = new Mat();


//        Bitmap b2 = Bitmap.createBitmap(matRGBA.cols(), matRGBA.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(matRGBA, b2);
//
////        byte[] array = new byte[b2.getWidth() * b2.getHeight() * 4];
////        Buffer dst = ByteBuffer.wrap(array);
////        b2.copyPixelsFromBuffer(dst);
////
////        byte[] ba = new byte[dst.remaining()];
//
//
//        InputStream bm = getResources().openRawResource(R.raw.eminem);
//        BufferedInputStream bufferedInputStream = new BufferedInputStream(bm);
//        Bitmap bmp = BitmapFactory.decodeStream(bufferedInputStream);
//
//        //COMPRESSED BITMAP BYTES
//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
//        byte[] byteArray = stream.toByteArray();
//        b2.recycle();



//        int size = b2.getRowBytes() * b2.getHeight();
//        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
//        b2.copyPixelsToBuffer(byteBuffer);
//        byte[] byteArray = byteBuffer.array();



        //===========OBJECDT DETECTION==================
        // now call that function
//        Mat out=new Mat();
//        out=objectDetectorClass.recognizeImage(matRGBA);
//        return out;
        //============================================

        //EXPESSION RECOGNITION===================

        matRGBA = facialExpressionRecognition.recognizeImage(matRGBA);
        return matRGBA;



//
        // RGBA BYTES
//        MatOfByte matOfByte = new MatOfByte();
//        Imgcodecs.imencode(".jpg", matRGBA, matOfByte);
//        byte[] rgbBytes = matOfByte.toArray();
//
//        Bitmap b = Bitmap.createBitmap(matRGBA.cols(), matRGBA.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(matRGBA, b);
//
//        //CONVERTING TO GRAY
//        Mat mGray = new Mat(b.getWidth(), b.getHeight(), CvType.CV_8UC1);
//        Utils.bitmapToMat(b, mGray);
//        Imgproc.cvtColor(mGray, mGray, Imgproc.COLOR_RGB2GRAY);
//
//
//        //GRAY BYTES
//        MatOfByte matOfByte1 = new MatOfByte();
//        Imgcodecs.imencode(".jpg", mGray, matOfByte1);
//        byte[] grayBytes = matOfByte1.toArray();
//
//        Bitmap b2 = Bitmap.createBitmap(mGray.cols(), mGray.rows(), Bitmap.Config.ARGB_8888);
//
//
//        Utils.matToBitmap(mGray, b2);


        //opencvExposure(path, matRGBA);
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void opencvExposure(String path, Mat output1) {
//        String path = args.length > 0 ? args[0] : "";
        if (path.isEmpty()) {
            System.out.println("Path is empty. Use the directory that contains images and exposure times.");
            System.exit(0);
        }
        List<Mat> images = new ArrayList<>();
        List<Float> times = new ArrayList<>();
        loadExposureSeq(path, images, times);
        Mat output = new Mat();
        CalibrateDebevec calibrate = Photo.createCalibrateDebevec();
        Mat matTimes = new Mat(times.size(), 1, CvType.CV_32F);
        float[] arrayTimes = new float[(int) (matTimes.total() * matTimes.channels())];
        for (int i = 0; i < times.size(); i++) {
            arrayTimes[i] = times.get(i);
        }
        matTimes.put(0, 0, arrayTimes);
        calibrate.process(images, output, matTimes);
        Bitmap bitmap = Bitmap.createBitmap(output.cols(), output.rows(), Bitmap.Config.ARGB_8888);


        Mat hdr = new Mat();
        MergeDebevec mergeDebevec = Photo.createMergeDebevec();
        mergeDebevec.process(images, hdr, matTimes);

        Mat ldr = new Mat();
        Tonemap tonemap = Photo.createTonemap(2.2f);
        tonemap.process(hdr, ldr);


        Mat fusion = new Mat();
        MergeMertens mergeMertens = Photo.createMergeMertens();
        mergeMertens.process(images, fusion);
        Core.multiply(fusion, new Scalar(255, 255, 255), output1);
        Bitmap bitmap1 = Bitmap.createBitmap(output1.cols(), output1.rows(), Bitmap.Config.ARGB_8888);

        Core.multiply(ldr, new Scalar(255, 255, 255), ldr);
        Imgcodecs.imwrite("fusion.png", fusion);
        Imgcodecs.imwrite("ldr.png", ldr);
        Imgcodecs.imwrite("hdr.hdr", hdr);
        //System.exit(0);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void loadExposureSeq(String path, List<Mat> images, List<Float> times) {
        path += "/cam_exposure/";
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(path + "lists.txt"));
            for (String line : lines) {
                String[] splitStr = line.split("\\s+");
                if (splitStr.length == 2) {
                    String name = splitStr[0];
                    Mat img = Imgcodecs.imread(path + name);
                    images.add(img);
                    float val = Float.parseFloat(splitStr[1]);
                    times.add(1 / val);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Bitmap getBitmap(String path) {
        Bitmap bitmap = null;
        try {
            File f = new File(path);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            bitmap = BitmapFactory.decodeStream(new FileInputStream(f), null, options);
            //image.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }


    @Override
    public void onPause() {
        super.onPause();

        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for init");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

}

//
//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//import org.opencv.android.BaseLoaderCallback;
//import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
//import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
//import org.opencv.android.JavaCameraView;
//import org.opencv.android.LoaderCallbackInterface;
//import org.opencv.android.OpenCVLoader;
//import org.opencv.android.Utils;
//import org.opencv.core.Core;
//import org.opencv.core.CvException;
//import org.opencv.core.CvType;
//import org.opencv.core.KeyPoint;
//import org.opencv.core.Mat;
//import org.opencv.core.MatOfInt;
//import org.opencv.core.MatOfInt4;
//import org.opencv.core.MatOfKeyPoint;
//import org.opencv.core.MatOfPoint;
//import org.opencv.core.MatOfPoint2f;
//import org.opencv.core.Point;
//import org.opencv.core.Rect;
//import org.opencv.core.RotatedRect;
//import org.opencv.core.Scalar;
//import org.opencv.imgproc.Imgproc;
//
//import android.Manifest;
//import android.annotation.SuppressLint;
//import android.app.Activity;
//import android.content.pm.PackageManager;
//import android.graphics.Bitmap;
//import android.hardware.Camera;
//import android.hardware.camera2.CaptureRequest;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Handler;
//import android.util.Log;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.View.OnTouchListener;
//import android.view.WindowManager;
//import android.widget.SeekBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.core.content.ContextCompat;
//
//import com.example.textrecognitionyolo.OcrManager;
//import com.example.textrecognitionyolo.R;
//import com.example.textrecognitionyolo.handrecogk.CustomSurfaceView;
//
//import org.opencv.core.Size;
//import org.opencv.core.Size;
//
//public class OpenCVCameraActivityJava extends Activity implements OnTouchListener, CvCameraViewListener2 {
//
//    private static final String TAG = "MainActivity";
//    OcrManager manager = new OcrManager();
//
//    static {
//        //System.loadLibrary("opencv_java3")
//
//        OpenCVLoader.initDebug();
//
//        if (OpenCVLoader.initDebug()) {
//            Log.e(TAG, "OPENCV SUCCESS: ");
//        } else {
//            Log.e(TAG, "OPENCV FAILED: ");
//        }
//    }
//
//    public static final int JAVA_DETECTOR = 0;
//    public static final int NATIVE_DETECTOR = 1;
//
//    private Mat mRgba;
//    private Mat mGray;
//    private Mat mIntermediateMat;
//
//    private int mDetectorType = JAVA_DETECTOR;
//
//    private CustomSurfaceView mOpenCvCameraView;
//    private List<Size> mResolutionList;
//
//    private SeekBar minTresholdSeekbar = null;
//    private SeekBar maxTresholdSeekbar = null;
//    private TextView minTresholdSeekbarText = null;
//    private TextView numberOfFingersText = null;
//
//    double iThreshold = 0;
//
//    private Scalar mBlobColorHsv;
//    private Scalar mBlobColorRgba;
//    private ColorBlobDetector mDetector;
//    private Mat mSpectrum;
//    private boolean mIsColorSelected = false;
//
//    private Size SPECTRUM_SIZE;
//    private Scalar CONTOUR_COLOR;
//    private Scalar CONTOUR_COLOR_WHITE;
//
//    final Handler mHandler = new Handler();
//    int numberOfFingers = 0;
//    int xRight;
//    int xLeft;
//    boolean isRightMet = false;
//    boolean isLeftMet = true;
//    boolean isTouched;
//    private CaptureRequest.Builder mPreviewRequestBuilder;
//    private CaptureRequest mPreviewRequest;
//    private long exposureTime = 1000,frameDuration = 1000;
//    private int sensitivity = 200;
//
//    final Runnable mUpdateFingerCountResults = new Runnable() {
//        public void run() {
//            updateNumberOfFingers();
//        }
//    };
//
//    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
//        @SuppressLint("ClickableViewAccessibility")
//        @Override
//        public void onManagerConnected(int status) {
//            switch (status) {
//                case LoaderCallbackInterface.SUCCESS: {
//                    Log.i(TAG, "OpenCV loaded successfully");
//                    mOpenCvCameraView.enableView();
//                    mOpenCvCameraView.setOnTouchListener(OpenCVCameraActivityJava.this);
//                    // 640x480
//                }
//                break;
//                default: {
//                    super.onManagerConnected(status);
//                }
//                break;
//            }
//        }
//    };
//
//    public OpenCVCameraActivityJava() {
//        Log.i(TAG, "Instantiated new " + this.getClass());
//    }
//
//    /**
//     * Called when the activity is first created.
//     */
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        Log.i(TAG, "called onCreate");
//
//        super.onCreate(savedInstanceState);
//
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//
//        setContentView(R.layout.main_surface_view);
//        if (!OpenCVLoader.initDebug()) {
//            Log.e("Test", "man");
//        } else {
//        }
//
//        mOpenCvCameraView = findViewById(R.id.main_surface_view);
//        mOpenCvCameraView.setCvCameraViewListener(this);
//
//        minTresholdSeekbarText = findViewById(R.id.textView3);
//
//        numberOfFingersText = (TextView) findViewById(R.id.numberOfFingers);
//
//        minTresholdSeekbar = (SeekBar) findViewById(R.id.seekBar1);
//        minTresholdSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            int progressChanged = 0;
//
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                progressChanged = progress;
//                minTresholdSeekbarText.setText(String.valueOf(progressChanged));
//            }
//
//            public void onStartTrackingTouch(SeekBar seekBar) {
//                // TODO Auto-generated method stub
//            }
//
//            public void onStopTrackingTouch(SeekBar seekBar) {
//                minTresholdSeekbarText.setText(String.valueOf(progressChanged));
//            }
//        });
//
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//                != PackageManager.PERMISSION_GRANTED) {
//            // More code here ...
//        } else {
//            Log.e("tag", "Permissions granted");
//            mOpenCvCameraView.setCameraPermissionGranted();
//        }
//
//        minTresholdSeekbar.setProgress(8700);
//    }
//
//    public void onCameraViewStarted(int width, int height) {
//        mGray = new Mat();
//        mRgba = new Mat();
//        mIntermediateMat = new Mat();
//
//        //captureBitmap();
//
//        Camera.Size resolution = mOpenCvCameraView.getResolution();
//        String caption = "Resolution " + Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
//        Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
//
//        Camera.Parameters cParams = mOpenCvCameraView.getParameters();
//        cParams.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
//        mOpenCvCameraView.setParameters(cParams);
//        Toast.makeText(this, "Focus mode : " + cParams.getFocusMode(), Toast.LENGTH_SHORT).show();
////
////        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
//////        mPreviewRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTime);
////        mPreviewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, sensitivity);
////        mPreviewRequestBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, frameDuration);
//
//        mRgba = new Mat(height, width, CvType.CV_8UC4);
//        mDetector = new ColorBlobDetector();
//        mSpectrum = new Mat();
//        mBlobColorRgba = new Scalar(255);
//        mBlobColorHsv = new Scalar(255);
//        SPECTRUM_SIZE = new Size(200, 64);
//        CONTOUR_COLOR = new Scalar(255, 0, 0, 255);
//        CONTOUR_COLOR_WHITE = new Scalar(255, 255, 255, 255);
//
//
////        mRgba = new Mat(width, height, CvType.CV_8UC4);
//
//    }
//
//    public void onCameraViewStopped() {
//        mGray.release();
//        mRgba.release();
//    }
//
//    @SuppressLint("ClickableViewAccessibility")
//    public boolean onTouch(View v, MotionEvent event) {
//        int cols = mRgba.cols();
//        int rows = mRgba.rows();
//        isTouched = true;
//
//        Log.e(TAG, "onTouch1: "+ mRgba.cols() + "Rows" + mRgba.rows() );
//
//        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
//        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;
//
//        int x = (int) event.getX() - xOffset;
//        int y = (int) event.getY() - yOffset;
//
//        Log.i("Touch", "Touch image coordinates: (" + x + ", " + y + ")");
//
//        xRight = y + 350;
//        xLeft = y - 350;
//
//
//        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;
//
//        Rect touchedRect = new Rect();
//
//        touchedRect.x = (x > 5) ? x - 5 : 0;
//        touchedRect.y = (y > 5) ? y - 5 : 0;
//
//        touchedRect.width = (x + 5 < cols) ? x + 5 - touchedRect.x : cols - touchedRect.x;
//        touchedRect.height = (y + 5 < rows) ? y + 5 - touchedRect.y : rows - touchedRect.y;
//
//        Mat touchedRegionRgba = mRgba.submat(touchedRect);
//
//        Mat touchedRegionHsv = new Mat();
//        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);
//
//        // Calculate average color of touched region
//        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
//        int pointCount = touchedRect.width * touchedRect.height;
//        for (int i = 0; i < mBlobColorHsv.val.length; i++)
//            mBlobColorHsv.val[i] /= pointCount;
//
//        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);
//
//        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
//                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");
//
//        mDetector.setHsvColor(mBlobColorHsv);
//
//        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);
//
//        mIsColorSelected = true;
//
//        touchedRegionRgba.release();
//        touchedRegionHsv.release();
//
//        return false; // don't need subsequent touch events
//    }
//
//
//
//    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
//        mRgba = inputFrame.rgba();
//      //  mGray = inputFrame.gray();
//
////        if(isTouched) {
////
////            Bitmap bmp = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.RGB_565);
////            Utils.matToBitmap(mRgba, bmp);
////
////            String ocr = manager.startRecognize(bmp);
//////            captureBitmap();
//////
//////            Mat tmp = new Mat (800, 600, CvType.CV_8U, new Scalar(4));
//////            try {
//////                //Imgproc.cvtColor(seedsImage, tmp, Imgproc.COLOR_RGB2BGRA);
////////                Imgproc.cvtColor(mRgba, tmp, Imgproc.COLOR_GRAY2RGBA, 4);
////////                Utils.matToBitmap();
////////                bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
////
//////            }
//////            catch (CvException ex){Log.d("Exception",ex.getMessage());}
////
////            isTouched = false;
////        }
////
////        iThreshold = minTresholdSeekbar.getProgress();
////
////        //Imgproc.blur(mRgba, mRgba, new Size(5,5));
////        Imgproc.GaussianBlur(mRgba, mRgba, new org.opencv.core.Size(3, 3), 1, 1);
////        //Imgproc.medianBlur(mRgba, mRgba, 3);
////
////        if (!mIsColorSelected) return mRgba;
////
////        List<MatOfPoint> contours = mDetector.getContours();
////        mDetector.process(mRgba);
////
////        Log.d(TAG, "Contours count: " + contours.size());
////
////        if (contours.size() <= 0) {
////            return mRgba;
////        }
////
////        RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(0).toArray()));
////
////        double boundWidth = rect.size.width;
////        double boundHeight = rect.size.height;
////        int boundPos = 0;
////
////        for (int i = 1; i < contours.size(); i++) {
////            rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(i).toArray()));
////            if (rect.size.width * rect.size.height > boundWidth * boundHeight) {
////                boundWidth = rect.size.width;
////                boundHeight = rect.size.height;
////                boundPos = i;
////            }
////        }
////
////        Rect boundRect = Imgproc.boundingRect(new MatOfPoint(contours.get(boundPos).toArray()));
////
////        int xCords = boundRect.x;
////        Log.e("Touch", "onCameraFrame: "+ xCords + " RIGHGT"+ xRight+ " LEFT"+ xLeft );
////
////        if(xCords >= xRight){
////            isRightMet = true;
////        }
////
////
////        if (xCords <= xLeft) {
////            isLeftMet = true;
////        }
////
////        if(isLeftMet && isRightMet){
////            OpenCVCameraActivityJava.this.runOnUiThread(new Runnable() {
////                public void run() {
////                    Toast.makeText(OpenCVCameraActivityJava.this, "HAND Waved", Toast.LENGTH_SHORT).show();
////                }
////            });
////            //Toast.makeText(this, "HandWaved", Toast.LENGTH_SHORT).show();
////            Log.e("Touch", "HAND WAVED");
////            isLeftMet = false;
////            isRightMet = false;
////        }
////
////        Imgproc.rectangle(mRgba, boundRect.tl(), boundRect.br(), CONTOUR_COLOR_WHITE, 2, 8, 0);
////
////
////        Log.d(TAG,
////                " Row start [" +
////                        (int) boundRect.tl().y + "] row end [" +
////                        (int) boundRect.br().y + "] Col start [" +
////                        (int) boundRect.tl().x + "] Col end [" +
////                        (int) boundRect.br().x + "]");
////
////        int rectHeightThresh = 0;
////        double a = boundRect.br().y - boundRect.tl().y;
////        a = a * 0.7;
////        a = boundRect.tl().y + a;
////
////        Log.d(TAG,
////                " A [" + a + "] br y - tl y = [" + (boundRect.br().y - boundRect.tl().y) + "]");
////
////        //Core.rectangle( mRgba, boundRect.tl(), boundRect.br(), CONTOUR_COLOR, 2, 8, 0 );
////        Imgproc.rectangle(mRgba, boundRect.tl(), new Point(boundRect.br().x, a), CONTOUR_COLOR, 2, 8, 0);
////
////        MatOfPoint2f pointMat = new MatOfPoint2f();
////        Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(boundPos).toArray()), pointMat, 3, true);
////        contours.set(boundPos, new MatOfPoint(pointMat.toArray()));
////
////        MatOfInt hull = new MatOfInt();
////        MatOfInt4 convexDefect = new MatOfInt4();
////        Imgproc.convexHull(new MatOfPoint(contours.get(boundPos).toArray()), hull);
////
////        if (hull.toArray().length < 3) return mRgba;
////
////        Imgproc.convexityDefects(new MatOfPoint(contours.get(boundPos).toArray()), hull, convexDefect);
////
////        List<MatOfPoint> hullPoints = new LinkedList<MatOfPoint>();
////        List<Point> listPo = new LinkedList<Point>();
////        for (int j = 0; j < hull.toList().size(); j++) {
////            listPo.add(contours.get(boundPos).toList().get(hull.toList().get(j)));
////        }
////
////        MatOfPoint e = new MatOfPoint();
////        e.fromList(listPo);
////        hullPoints.add(e);
////
////        List<MatOfPoint> defectPoints = new LinkedList<MatOfPoint>();
////        List<Point> listPoDefect = new LinkedList<Point>();
////        for (int j = 0; j < convexDefect.toList().size(); j = j + 4) {
////            Point farPoint = contours.get(boundPos).toList().get(convexDefect.toList().get(j + 2));
////            Integer depth = convexDefect.toList().get(j + 3);
////            if (depth > iThreshold && farPoint.y < a) {
////                listPoDefect.add(contours.get(boundPos).toList().get(convexDefect.toList().get(j + 2)));
////            }
////            Log.d(TAG, "defects [" + j + "] " + convexDefect.toList().get(j + 3));
////        }
////
////        MatOfPoint e2 = new MatOfPoint();
////        e2.fromList(listPo);
////        defectPoints.add(e2);
////
////        Log.d(TAG, "hull: " + hull.toList());
////        Log.d(TAG, "defects: " + convexDefect.toList());
////
////        Imgproc.drawContours(mRgba, hullPoints, -1, CONTOUR_COLOR, 3);
////
////        int defectsTotal = (int) convexDefect.total();
////        Log.d(TAG, "Defect total " + defectsTotal);
////
////        this.numberOfFingers = listPoDefect.size();
////        if (this.numberOfFingers > 5) this.numberOfFingers = 5;
////
////        mHandler.post(mUpdateFingerCountResults);
////
////        for (Point p : listPoDefect) {
////            Imgproc.circle(mRgba, p, 6, new Scalar(255, 0, 255));
////        }
////
////
//
//        return mRgba;
//    }
//
//    private void captureBitmap(){
//        Bitmap bitmap = Bitmap.createBitmap(mOpenCvCameraView.getWidth()/4,mOpenCvCameraView.getHeight()/4, Bitmap.Config.ARGB_8888);
//        Mat tmp = new Mat (100, 100, CvType.CV_8U, new Scalar(4));
//
//
//        try {
//          //  Imgproc.cvtColor(mRgba, tmp, Imgproc.COLOR_GRAY2RGBA, 4);
//            //bitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(mRgba, bitmap);
//
//        }catch(Exception ex){
//            System.out.println(ex.getMessage());
//        }
//    }
//
////    private void detectText() {
////        Scalar CONTOUR_COLOR = new Scalar(255);
////        MatOfKeyPoint keypoint = new MatOfKeyPoint();
////        List<KeyPoint> listpoint;
////        KeyPoint kpoint;
////        Mat mask = Mat.zeros(mGrey.size(), CvType.CV_8UC1);
////        int rectanx1;
////        int rectany1;
////        int rectanx2;
////        int rectany2;
////        int imgsize = mGrey.height() * mGrey.width();
////        Scalar zeos = new Scalar(0, 0, 0);
////
////        List<MatOfPoint> contour2 = new ArrayList<MatOfPoint>();
////        Mat kernel = new Mat(1, 50, CvType.CV_8UC1, Scalar.all(255));
////        Mat morbyte = new Mat();
////        Mat hierarchy = new Mat();
////
////        Rect rectan3;
////        //
////        FeatureDetector detector = FeatureDetector
////                .create(FeatureDetector.MSER);
////        detector.detect(mGrey, keypoint);
////        listpoint = keypoint.toList();
////        //
////        for (int ind = 0; ind < listpoint.size(); ind++) {
////            kpoint = listpoint.get(ind);
////            rectanx1 = (int) (kpoint.pt.x - 0.5 * kpoint.size);
////            rectany1 = (int) (kpoint.pt.y - 0.5 * kpoint.size);
////            rectanx2 = (int) (kpoint.size);
////            rectany2 = (int) (kpoint.size);
////            if (rectanx1 <= 0)
////                rectanx1 = 1;
////            if (rectany1 <= 0)
////                rectany1 = 1;
////            if ((rectanx1 + rectanx2) > mGrey.width())
////                rectanx2 = mGrey.width() - rectanx1;
////            if ((rectany1 + rectany2) > mGrey.height())
////                rectany2 = mGrey.height() - rectany1;
////            Rect rectant = new Rect(rectanx1, rectany1, rectanx2, rectany2);
////            try {
////                Mat roi = new Mat(mask, rectant);
////                roi.setTo(CONTOUR_COLOR);
////            } catch (Exception ex) {
////                Log.d("mylog", "mat roi error " + ex.getMessage());
////            }
////        }
////
////        Imgproc.morphologyEx(mask, morbyte, Imgproc.MORPH_DILATE, kernel);
////        Imgproc.findContours(morbyte, contour2, hierarchy,
////                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
////        for (int ind = 0; ind < contour2.size(); ind++) {
////            rectan3 = Imgproc.boundingRect(contour2.get(ind));
////            rectan3 = Imgproc.boundingRect(contour2.get(ind));
////            if (rectan3.area() > 0.5 * imgsize || rectan3.area() < 100
////                    || rectan3.width / rectan3.height < 2) {
////                Mat roi = new Mat(morbyte, rectan3);
////                roi.setTo(zeos);
////
////            } else
////                Imgproc.rectangle(mRgba, rectan3.br(), rectan3.tl(),
////                        CONTOUR_COLOR);
////        }
////    }
//
//    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
//        Mat pointMatRgba = new Mat();
//        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
//        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);
//
//        return new Scalar(pointMatRgba.get(0, 0));
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        if (mOpenCvCameraView != null) {
//            mOpenCvCameraView.disableView();
//        }
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        // OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
//        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
//    }
//
//    public void onDestroy() {
//        super.onDestroy();
//        mOpenCvCameraView.disableView();
//    }
//
//    public void updateNumberOfFingers() {
//        numberOfFingersText.setText(String.valueOf(this.numberOfFingers));
//    }
//}