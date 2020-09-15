package com.example.videocapture

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log


class CaptureService : Service() {

    //val slee : Log.(String) -> Unit = {msg -> Log.v("SLEE", msg)}
    companion object {
        val FD_NOT_SETTED = -1
    }

    inner class CaptureBinder : Binder() {
        fun getService() : CaptureService {
            return this@CaptureService
        }
    }

    private val binder = CaptureBinder()

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }



    private var stored_fd : Int = FD_NOT_SETTED

    fun pushFD(fd : Int) {
        if(stored_fd != FD_NOT_SETTED) {
            Log.v("slee", "pushFD() : there is previous FD ! : fd = $stored_fd : close it !")
            // close previous fd
            val parcelFD = ParcelFileDescriptor.adoptFd(stored_fd)
            parcelFD.close()

            stored_fd = FD_NOT_SETTED
        }
        stored_fd = fd
        Log.v("slee", "push FD : $stored_fd")
     }

    fun pullFD() : Int {
        Log.v("slee", "pull FD : $stored_fd")
        val ret = stored_fd
        stored_fd = FD_NOT_SETTED
        return ret
    }

}