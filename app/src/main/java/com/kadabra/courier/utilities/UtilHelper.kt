package com.kadabra.courier.utilities

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.kadabra.courier.firebase.FirebaseManager
import com.kadabra.courier.firebase.FirebaseManager.updateTaskScreenShot
import java.io.ByteArrayOutputStream

object UtilHelper {
    private val mUploadTask: StorageTask<*>? = null
    private val mUploader: IUploader? = null
    private var tasksFolderName="task_images"


    internal interface IUploader {
        fun onSuccess()
        fun onFailure()
    }



    private fun getFileExtension(
        uri: Uri,
        context: Context
    ): String? {
        val cR = context.contentResolver
        val mime = MimeTypeMap.getSingleton()
        return mime.getExtensionFromMimeType(cR.getType(uri))
    }

    private fun openFileChooser(
        context: Context,
        PICK_IMAGE_REQUEST: Int
    ) {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        //        context.startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

     fun uploadFile(
        bitmap: Bitmap,
        taskId: String
    ) {
        var mStorageRef = FirebaseStorage.getInstance().getReference(tasksFolderName)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()
        if (data != null) {

            val fileReference = mStorageRef.child(
                taskId
                        + "." + "jpeg"
            )
            fileReference.putBytes(data).continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception!!
                }
                fileReference.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    val downloadUri = task.result
                    fileReference.downloadUrl
                    updateTaskScreenShot(
                        taskId,downloadUri.toString()
//                        fileReference.downloadUrl.result?.path.toString()
                    )
                    mUploader?.onSuccess()
                } else {
                    mUploader?.onFailure()
                    Toast.makeText(
                        AppController.getContext(),
                        "upload failed: " + task.exception!!.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

}