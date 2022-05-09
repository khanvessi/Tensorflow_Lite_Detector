package com.example.textrecognitionyolo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.BitSet;

public class StoragePrediction extends AppCompatActivity {

    private Button select_image;
    private ImageView image_v;
    private objectDetectorClass objectDetectorClass;
    int SELECT_IMAGE=200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage_prediction);

        select_image=findViewById(R.id.select_image);
        image_v=findViewById(R.id.image_v);

        try{
            // input size is 300 for this model
            objectDetectorClass=new objectDetectorClass(getAssets(),"custom_hand_model.tflite","labelmap.txt",320);
            Log.d("MainActivity","Model is successfully loaded");
        }
        catch (IOException e){
            Log.d("MainActivity","Getting some error");
            e.printStackTrace();
        }

        select_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                image_chooser();
            }
        });


    }

    private void image_chooser() {

        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(i, "Select Picture"),SELECT_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != Activity.RESULT_CANCELED){
            if(resultCode==RESULT_OK) {
                if(requestCode==SELECT_IMAGE && data != null && data.getData() != null){
                    Uri selectedImageUri=data.getData();
                    if(selectedImageUri!=null){
                        Log.e("StoragePredictionAct", "onActivityResult: "+selectedImageUri );

                        Bitmap bitmap=null;
                        try {
                            bitmap= MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);

                        }catch (IOException e){
                            e.printStackTrace();
                        }

                        Mat selectedImage = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
                        Utils.bitmapToMat(bitmap, selectedImage);
                        selectedImage=objectDetectorClass.recognizeImageFromGallery(selectedImage);
                        //covnert mat to bitmap
                        Bitmap bitmap1=null;
                        bitmap1=Bitmap.createBitmap(selectedImage.cols(),selectedImage.rows(),Bitmap.Config.ARGB_8888);

                        Utils.matToBitmap(selectedImage,bitmap1);
                        image_v.setImageBitmap(bitmap1);

                    }
                }
            }

        }

    }
}