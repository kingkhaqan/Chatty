package com.example.zeesh.chatty

import android.app.IntentService
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.support.multidex.MultiDexApplication
import android.widget.Toast
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Created by zeesh on 4/7/2019.
 */


class MyApplication: MultiDexApplication(){

}

class ClearCashService: IntentService(ClearCashService::class.simpleName) {
    override fun onHandleIntent(intent: Intent?) {

    }
}

class UploadUriToServer: IntentService(UploadUriToServer::class.simpleName) {
    override fun onHandleIntent(intent: Intent?) {

        val thumbnailPath = intent?.getStringExtra("thumbnailPath")
        val videoPath = intent?.getStringExtra("videoPath")
        val participentId = intent?.getStringExtra("participentId")
        val thumbnailUri = Uri.parse(thumbnailPath)
        val videoUri = Uri.parse(videoPath)

        val ref = chatRef.child(participentId)
        val videoKey = ref.push().key
        val sref = storageRef.child("${androidId}/videos/$videoKey")
        val thumbRef = storageRef.child("${androidId}/thumbnails/$videoKey")

        sref.putFile(videoUri).addOnSuccessListener {
            val k = ref.push().key
            ref.child(k).setValue("$MSG_TYPE_VIDEO$videoKey")
        }

        thumbRef.putFile(thumbnailUri)

    }
}

public fun getImageUri(bitmap: Bitmap): Uri {
    val file = File.createTempFile("${System.currentTimeMillis()}", "PNG")
    val fos = FileOutputStream(file)
    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
//        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "title", null)
    fos.write(baos.toByteArray())
    return Uri.fromFile(file)
}
