package com.example.videocapture

import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    val video_URL = "https://vt.tumblr.com/tumblr_o600t8hzf51qcbnq0_480.mp4"
    val mediaRet = MediaMetadataRetriever()
    lateinit var captureService : CaptureService
    var isBound = false

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as CaptureService.CaptureBinder
            captureService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // init Video.
        videoView.setVideoURI(Uri.parse(video_URL))

        // init video retriever
        mediaRet.setDataSource(video_URL, HashMap<String, String>())

        // init buttons.
        btn_captureAndPush.setOnClickListener {
            val pos = videoView.currentPosition
            val fileName = "${pos}.jpg"
            val uri = storeBitmap(captureVideo(pos), fileName)

            // if uri != null, push  "native" FD to server.
            if(uri != null) {
                val parcelFD = contentResolver.openFileDescriptor(uri, "r", null)

                 if(parcelFD != null) {
                     Log.v("slee", "Let's pushFD() : URI = ${uri.toString()}")

                    // get native FD.
                    val nativeFD = parcelFD.detachFd()

                    // push 'native FD' to server.
                    captureService.pushFD(nativeFD)
                }
            }
        }

        btn_pullAndDraw.setOnClickListener {
            val nativeFD = captureService.pullFD()

            if(nativeFD != CaptureService.FD_NOT_SETTED) {
                val bitMap = getBitmapFromNativeFD(nativeFD)
                imageView.setImageBitmap(bitMap)
            }
        }



    }

    fun captureVideo(pos : Int) : Bitmap? {
        val bitmap = mediaRet.getFrameAtTime(pos * 1000L)

        return bitmap
    }

    fun storeBitmap(src: Bitmap?, fileName: String) : Uri? {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/*")

        val item = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        if(item != null) {
            val parcelFD = contentResolver.openFileDescriptor(item, "w", null)

            if(parcelFD != null && src != null) {
                val fos = FileOutputStream(parcelFD.getFileDescriptor())
                src.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                fos.flush()
                //src.recycle()
                fos.close()

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(item, values, null, null)

                return item
            }
            else {
                Log.v("SLEE", "storeBitmap() : return null ! : parcelFD == null OR src == null   !!! ")
                return null
            }
        }
        else {
            Log.v("SLEE", "storeBitmap() : return null ! : contentResolver.insert() return null !")
            return null
        }

    }

    fun getBitmapFromNativeFD(nativeFD : Int) : Bitmap {
        val parcelFD = ParcelFileDescriptor.adoptFd(nativeFD)
        val FD = parcelFD.fileDescriptor
        val bitMap = BitmapFactory.decodeFileDescriptor(FD)

        parcelFD.close()

        return bitMap
    }

    override fun onStart() {
        super.onStart()
        // Bind to CaptureService
        Intent(this, CaptureService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        // start Video.
        videoView.start()
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        isBound = false
    }
}