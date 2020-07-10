package com.example.zeesh.chatty

import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.MediaController
import kotlinx.android.synthetic.main.activity_video_view.*

class VideoViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_view)

        var path = intent.getStringExtra("path")
        if (path!=null && path!=""){
            val uri = Uri.parse(path)
            video_view_videoview.setVideoURI(uri)
            val mediaControler = MediaController(this)
            mediaControler.setAnchorView(video_view_videoview)
//            mediaControler.show()

            video_view_videoview.setMediaController(mediaControler)
//            video_view_videoview
//            video_view_videoview.start()
//            mediaControler.show()
        }
    }
}
