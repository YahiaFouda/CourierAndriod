package com.kadabra.courier.utilities

import android.graphics.Bitmap
import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.SnapshotReadyCallback
import com.kadabra.courier.firebase.FirebaseManager
import java.io.OutputStream

class Screenshot {

    companion object {
        fun takeScreenshot(v: View): Bitmap {
            v.isDrawingCacheEnabled = true
            v.buildDrawingCache(true)
            val bitMap = Bitmap.createBitmap(v.drawingCache)
            v.isDrawingCacheEnabled = false
            return bitMap
        }

        fun takeScreenshotOfRootView(v: View): Bitmap {
            return takeScreenshot(v.rootView)
        }

        fun captureScreen(mMap: GoogleMap,takId:String) {
            val callback = SnapshotReadyCallback { snapshot ->
                // TODO Auto-generated method stub
                val bitmap = snapshot
                val fout: OutputStream? = null
                val filePath =
                    System.currentTimeMillis().toString() + ".jpeg"
                UtilHelper.uploadFile(bitmap,takId)
            }
            mMap.snapshot(callback)
        }
    }
}