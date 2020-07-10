package com.example.zeesh.chatty

import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.MediaController
import kotlinx.android.synthetic.main.activity_video_view.*
import kotlinx.android.synthetic.main.activity_view_image.*

class ViewImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_image)

        val path = intent.getStringExtra("path")
        if (path!=null && path!=""){
            val uri = Uri.parse(path)

            image_view_image_activity.setImageURI(uri)
        }
//        val index = intent.getIntExtra("index", -1)
//        if (index != -1){
//            image_view_image_activity.setImageBitmap(imagesMap[index])
//        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
