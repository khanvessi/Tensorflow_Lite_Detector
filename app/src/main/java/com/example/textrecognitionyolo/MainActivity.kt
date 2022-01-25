package com.example.textrecognitionyolo

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.textrecognitionyolo.databinding.ActivityMainBinding
import com.example.textrecognitionyolo.handrecog_j.OpenCVCameraActivityJava
import org.opencv.android.OpenCVLoader
import java.io.*
import kotlin.math.floor

class MainActivity : AppCompatActivity() {

    protected val CAMERA_REQUEST = 0
    protected val GALLERY_PICTURE = 1
    private val pictureActionIntent: Intent? = null
    var selectedImagePath: String? = null
    val PERMISSION_CODE = 2000
    lateinit var binding: ActivityMainBinding
    val manager = OcrManager()


    init {
        if (!OpenCVLoader.initDebug()) {
            Log.e("MainActivity", "Unable to load OpenCV")
        } else {
            Log.e("MainActivity", "Success loading OpenCV")
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        manager.initAPI()

        // no error, init api ok.
        // how to use to recognize the text
        val assetManager = applicationContext.assets
        val istr: InputStream
        var bitmap: Bitmap? = null
        try {
            istr = assetManager.open("cnic.jpeg")
            bitmap = BitmapFactory.decodeStream(istr)
        } catch (e: IOException) {
            // handle exception
        }
        val ocrManager = OcrManager()
        val result = ocrManager.startRecognize(bitmap)
        Log.e("MainActivity", "onCreate: $result")

        binding.btnImg.setOnClickListener(View.OnClickListener {
            checkPermissions()
        })

        binding.btnOpencv.setOnClickListener(View.OnClickListener {
            val recognizeActivity = Intent(applicationContext, OpenCVCameraActivityJava::class.java)
            // Clears History of Activity
            // Clears History of Activity
            val b = Bundle()
            recognizeActivity.putExtras(b)
            recognizeActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(recognizeActivity)
        })
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
                PERMISSION_CODE
            )
        } else {
            startDialog()
        }
    }


    private fun startDialog() {
        val myAlertDialog: AlertDialog.Builder = AlertDialog.Builder(
            this
        )
        myAlertDialog.setTitle("Upload Pictures Option")
        myAlertDialog.setMessage("How do you want to set your picture?")
        myAlertDialog.setPositiveButton("Gallery",
            DialogInterface.OnClickListener { _, _ ->
                openGallery()
            })
        myAlertDialog.setNegativeButton("Camera",
            DialogInterface.OnClickListener { _, _ ->
                openCamera()
            })
        myAlertDialog.show()
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, 1)
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, 2)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == CAMERA_REQUEST){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                openCamera()
            }

            if(grantResults[1] == PackageManager.PERMISSION_GRANTED){
                openGallery()
            }
        }
    }


    @Throws(FileNotFoundException::class, IOException::class)
    fun getThumbnail(uri: Uri?): Bitmap? {
        var input = this.contentResolver.openInputStream(uri!!)
        val onlyBoundsOptions = BitmapFactory.Options()
        onlyBoundsOptions.inJustDecodeBounds = true
        //onlyBoundsOptions.inDither = true //optional
        onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888 //optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions)
        input!!.close()
        if (onlyBoundsOptions.outWidth == -1 || onlyBoundsOptions.outHeight == -1) {
            return null
        }
        val originalSize =
            if (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) onlyBoundsOptions.outHeight else onlyBoundsOptions.outWidth

       // val ratio = if (originalSize > THUMBNAIL_SIZE) originalSize / THUMBNAIL_SIZE else 1.0
        val bitmapOptions = BitmapFactory.Options()
       // bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio)
        bitmapOptions.inDither = true //optional
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888 //
        input = this.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions)
        input!!.close()
        return bitmap
    }

    private fun getPowerOfTwoForSampleRatio(ratio: Double): Int {
        val k = Integer.highestOneBit(floor(ratio).toInt())
        return if (k == 0) 1 else k
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1) {
            if(data?.data != null){
                val uri = data.data
                Log.e("Gallery Image Uri: ", uri.toString() + "")

                val bitmap = MediaStore.Images.Media.getBitmap(
                    contentResolver,
                    uri
                )
                val imagePath = uri?.let { getRealPathFromURI(it) }

                val tempUri: Uri =
                    getImageUri(this, bitmap )
//
                val bitmap2 = getThumbnail(tempUri)
             //val bitmap2 = BitmapFactory.decodeFile(imagePath)


                val numb = manager.startRecognize(bitmap2)

                Toast.makeText(this, numb, Toast.LENGTH_LONG).show()

                Log.e("MainActivity", "onActivityResult: $numb")
                binding.txtView.text = numb.toString()
                binding.profilePic.setImageBitmap(bitmap2)
            }

        }

        if (requestCode == 2) {
            if (data?.extras?.get("data") != null) {
                val uri = data.extras?.get("data")

                val tempUri: Uri =
                    getImageUri(this, uri as Bitmap)

                val imagePath = getRealPathFromURI(tempUri)

                val image = File(imagePath, "TempImage")
                val options = BitmapFactory.Options()

                //val bitmap2 = getThumbnail(tempUri)
                val bitmap2 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, tempUri))
                } else {
                    MediaStore.Images.Media.getBitmap(contentResolver, tempUri)
                }

//                options.inSampleSize = 2
                //val bitmap = BitmapFactory.decodeFile(image.absolutePath, options)


//                val scaledBitmap = Bitmap.createScaledBitmap(
//                    bitmap, 512,
//                    ((bitmap.getHeight() * (512.0 / bitmap.getWidth())).toInt()), true
//                )

                val numb = manager.startRecognize(bitmap2)

                Toast.makeText(this, numb, Toast.LENGTH_LONG).show()
                binding.txtView.text = numb.toString()
//

                binding.profilePic.setImageBitmap(bitmap2)

            }
        }
    }


    //UTILITY FUNCTION
    private fun getRealPathFromURI(uri: Uri): String {
        var path = ""
        if (contentResolver != null) {
            val cursor: Cursor? = contentResolver
                ?.query(uri, null, null, null, null)
            if (cursor != null) {
                cursor.moveToFirst()
                val idx: Int = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                path = cursor.getString(idx)
                cursor.close()
            }
        }
        return path
    }



    private fun getImageUri(inContext: Context, inImage: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Image", null)
        return Uri.parse(path)
    }

}