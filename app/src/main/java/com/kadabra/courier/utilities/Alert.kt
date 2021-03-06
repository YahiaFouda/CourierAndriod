package com.kadabra.courier.utilities

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.widget.RelativeLayout
import android.widget.Toast
import com.kadabra.courier.R


object Alert {

    var mLoadingDialog: Dialog? = null

    fun showProgress(context: Context) {
        if (mLoadingDialog == null) {
            mLoadingDialog = Dialog(context)
            mLoadingDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
            mLoadingDialog?.setContentView(R.layout.progress_bar)
            mLoadingDialog?.window!!.setLayout(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            mLoadingDialog?.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            mLoadingDialog?.setCancelable(false)
        }
        mLoadingDialog?.show()
    }

    fun hideProgress() {
        if(mLoadingDialog!=null)
        mLoadingDialog?.hide()
    }

    fun showMessage(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
 fun showMessage( message: String) {
        Toast.makeText(AppController.getContext(), message, Toast.LENGTH_SHORT).show()
    }

    fun showAlertMessage(context: Context, title: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setIcon(R.mipmap.ic_launcher)
            .setPositiveButton("ok", null)
            .show()

    }

}