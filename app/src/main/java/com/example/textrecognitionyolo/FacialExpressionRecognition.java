package com.example.textrecognitionyolo;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FacialExpressionRecognition {

    private Interpreter interpreter;
    private int INPUT_SIZE;
    private int height=0;
    private int width=0;

    private GpuDelegate gpuDelegate=null;

    private CascadeClassifier cascadeClassifier;

    public FacialExpressionRecognition(AssetManager assetManager, Context context, String modelPath, int inputSize) throws IOException {

        INPUT_SIZE =inputSize;
        Interpreter.Options options = new Interpreter.Options();
        gpuDelegate=new GpuDelegate();
        options.addDelegate(gpuDelegate);
        options.setNumThreads(4);
        interpreter=new Interpreter(loadModelFile(assetManager,modelPath),options);
        Log.d(TAG, "FacialExpressionRecognition: Model is Loaded ");

        try {
            InputStream is= context.getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            //create a folder
            File cascadeDir = context.getDir("casecade", Context.MODE_PRIVATE);
            //now create a new file in that folder
            File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt");
            // now define output stream to transfer data to file we created
            FileOutputStream os = new FileOutputStream(mCascadeFile);
            byte[] buffer = new byte[4096];
            int byteRead;
            //read byte in while loop
            //when it reads -1 that means no data to read
            while ((byteRead=is.read(buffer)) != -1){
                //writing bytes on cascade file
                os.write(buffer, 0, byteRead);
            }

            is.close();
            os.close();
            cascadeClassifier= new CascadeClassifier(mCascadeFile.getAbsolutePath());
            //if cascade file is loaded
            Log.d(TAG, "FacialExpressionRecognition: Classifier is loaded");

        }catch (IOException e){
            e.printStackTrace();
        }


    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        // use to get description of file
        AssetFileDescriptor fileDescriptor=assetManager.openFd(modelPath);
        FileInputStream inputStream=new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel=inputStream.getChannel();
        long startOffset =fileDescriptor.getStartOffset();
        long declaredLength=fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declaredLength);
    }

    public Mat recognizeImage(Mat matImage) {
        //rotate image by 90 degree
        Core.flip(matImage.t(), matImage,1);


        //convert to gray
        Mat grayScaled=new Mat();
        Imgproc.cvtColor(matImage,grayScaled,Imgproc.COLOR_RGB2GRAY);
        height=grayScaled.height();
        width=grayScaled.width();

        //define minimum heigt of face in original image
        //below this size no face in original image will show
        int absoluteFaceSize = (int) (height*0.1);
        // now create MatofRect to store face
        MatOfRect faces = new MatOfRect();
        //check if cascadeclassifier is loaded or not
        if(cascadeClassifier != null){
            cascadeClassifier.detectMultiScale(grayScaled,faces,1.1,2,2,new Size(absoluteFaceSize,absoluteFaceSize), new Size());

        }

        Rect[] faceArray = faces.toArray();
        //loop through each face
        for(int i=0; i<faceArray.length;i++){
            //if you want to draw rect around face
            //input/out starting point ending point   color R G B alpha thickness
            Imgproc.rectangle(matImage, faceArray[i].tl(), faceArray[i].br(), new Scalar(0,255,255), 2);

            Rect roi = new Rect((int)faceArray[i].tl().x,(int)faceArray[i].tl().y,
                    ((int)faceArray[i].br().x)-(int)(faceArray[i].tl().x),
                    ((int)faceArray[i].br().y)-(int)(faceArray[i].tl().y));

            Mat croppedRgba = new Mat(matImage, roi);
            //convert to bitmap
            Bitmap bitmap=null;

            bitmap = Bitmap.createBitmap(croppedRgba.cols(),croppedRgba.rows(),Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(croppedRgba,bitmap);

            //resize bitmap to (48,48)
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 48,48, false);
            ByteBuffer byteBuffer=convertBitmapToByteBuffer(scaledBitmap);

            //now create an object to output
            float[][] emotion = new float[1][1];
            //now predict with bytebuffer as an input and emotion as an output
            interpreter.run(byteBuffer,emotion);
            //if emotion is recognized print value of it
            Log.d(TAG, "Output: "+ Array.get(Array.get(emotion,0),0));

            //define float value of emotion
            float emotion_v = (float) Array.get(Array.get(emotion,0),0);
            Log.d(TAG, "Output: "+emotion_v);
            //create a function that return text emotion

            String emotion_s = getEmotionText(emotion_v);
            //now put text on original frame(matImage)
            //              input/output    text: Angry(2.93232)

            Imgproc.putText(matImage,emotion_s+" ("+emotion_v+")",
                    new Point((int)faceArray[i].tl().x+10,(int)faceArray[i].tl().y+20),
                    1,1.5,new Scalar(0,0,255,150),2);
            //use to scale text color R G B alpha thickness


            //done
        }
        //rotate back
        Core.flip(matImage.t(),matImage,0);
        return matImage;
    }

    private String getEmotionText(float emotion_v) {
        String val="";

        if(emotion_v>0 & emotion_v<0.5){
            val="Surprise";
        }else if(emotion_v>=0.5 & emotion_v<1.5){
            val="Fear";
        }else if(emotion_v>=1.5 & emotion_v<2.5){
            val="Angry";
        }else if(emotion_v>=2.5 & emotion_v<3.5){
            val="Neutral";
        }else if(emotion_v>=3.5 & emotion_v<4.5){
            val="Sad";
        }else if(emotion_v>=5.5 & emotion_v<5.5){
            val="Disgust";
        }else {
            val = "Happy";
        }

        return val;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap scaledBitmap) {
        ByteBuffer byteBuffer;
        int sizeImage = INPUT_SIZE;// 48;

        byteBuffer = ByteBuffer.allocateDirect(4*1*sizeImage*sizeImage*3);
        //4 is mul for float input
        //3 is mul for rgb

        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[sizeImage*sizeImage];
        scaledBitmap.getPixels(intValues,0,scaledBitmap.getWidth(),0,0,scaledBitmap.getWidth(),scaledBitmap.getHeight());
        int pixel = 0;
        for(int i=0; i<sizeImage;++i) {
            for (int j=0; j < sizeImage; ++j) {
                final int val = intValues[pixel++];
                //now put float value to bytebuffer
                //scale image to convert image from 0-255 to 0-1
                byteBuffer.putFloat((((val>>16)&0xFF))/255.0f);
                byteBuffer.putFloat((((val>>8) & 0xFF)) / 255.0f);
                byteBuffer.putFloat(((val & 0xFF)) / 255.0f);

            }
        }

        return byteBuffer;
    }
}
