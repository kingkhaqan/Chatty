package com.example.zeesh.chatty

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.Image
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.graphics.BitmapCompat
import android.support.v4.graphics.drawable.RoundedBitmapDrawable
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
//import com.bumptech.glide.Glide
//import com.github.ybq.android.spinkit.style.FoldingCube
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.content_main2.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ChatActivity : AppCompatActivity() {



    val videoTaskList = mutableListOf<UploadVideoDirectTask>()
    val imageTaskList = mutableListOf<SendImageDirectTask>()

    var firstTurn = true
    var terminateRecursion = false
    var myAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>? = null
    val messagesList = mutableListOf<Any>()
    val messagesKey = mutableSetOf<String>()
    val poolListener = object : ValueEventListener {
        override fun onCancelled(p0: DatabaseError?) {}
        override fun onDataChange(ds: DataSnapshot?) {
            val v = ds?.value
            if (v == null) {
//                distroyWithchat()
                finish()
            }
        }

    }

    val chatListener = object : ChildEventListener{
        override fun onCancelled(p0: DatabaseError?) {}
        override fun onChildMoved(p0: DataSnapshot?, p1: String?) {}
        override fun onChildChanged(ds: DataSnapshot?, p1: String?) {
            val k = ds?.key
            if(k!=null){
                val v: String = ds?.getValue(String::class.java)!!
                val type = v[0]
                val msg = v.subSequence(1, v.length).toString()
                when(type){
                    IMAGE_TYPE_B->{
                        downloadImage(msg, k, IMAGE_TYPE_B)
                    }
                }
            }
        }
        override fun onChildAdded(ds: DataSnapshot?, p1: String?) {
            val k = ds?.key

            if (k!=null && !messagesKey.contains(k)){
                messagesKey.add(k)
                val v: String = ds?.getValue(String::class.java)!!
                val type = v[0]
                val msg = v.subSequence(1, v.length).toString()
                var content = ""
                when(type){
                    MSG_TYPE_TEXT-> content = msg
                    MSG_TYPE_IMAGE-> content = "an image recieved"
                    MSG_TYPE_VIDEO-> content = "a video is recieved"

                }
                if (appRunningInBackground(this@ChatActivity)){
                    val id: String = "my_channel_01"

//                    val intent = Intent(this@ChatActivity, ChatActivity::class.java).apply {
//                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                    }
//                    val pendingIntent: PendingIntent = PendingIntent.getActivity(this@ChatActivity, 0, intent, 0)

                    var builder = NotificationCompat.Builder(this@ChatActivity, id)
                            .setContentTitle("Chatty")
                            .setContentText(content)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                            .setContentIntent(pendingIntent)
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        builder.setSmallIcon(R.drawable.ic_message_black_24dp);
                        builder.setColor(resources.getColor(R.color.colorPrimary));
                    } else {
                        builder.setSmallIcon(R.drawable.icon_transperent);
                    }

                    with(NotificationManagerCompat.from(this@ChatActivity)) {
                        // notificationId is a unique int for each notification that you must define
                        notify(notificationId++, builder.build())
                    }
                }
                when(type){

                    MSG_TYPE_TEXT -> { addMessage(TextMessage(msg, USER_TYPE_RECEIVER)) }
                    MSG_TYPE_FULL_IMAGE-> {
                        downloadImageDirect(msg)
//                        DownloadImageDirectTask(this@ChatActivity).execute(msg, participentId)
                    }
                    MSG_TYPE_VIDEO-> {
                        downloadVideoDirectly(msg)
//                        DownloadVideoDirectTask(this@ChatActivity).execute(msg, participentId)
                    }
                    MSG_TYPE_IMAGE -> { downloadImage(msg, k, IMAGE_TYPE_A)}
                    IMAGE_TYPE_B-> { messagesKey.remove(k) }
                }
            }
        }
        override fun onChildRemoved(p0: DataSnapshot?) {}
    }


    val typingListener = object : ValueEventListener{
        override fun onCancelled(p0: DatabaseError?) {}
        override fun onDataChange(ds: DataSnapshot?) {
            if (ds!=null) {
                val s = ds?.getValue(String::class.java)
                when(s){
                    "y" -> setActionBarSubtitle("typing...")
                    "n" -> setActionBarSubtitle("online")
                }
            }
        }


    }


//    val connectionListener = object: ValueEventListener{
//        override fun onCancelled(p0: DatabaseError?) {}
//
//        override fun onDataChange(ds: DataSnapshot?) {
//
//            var prePK = ""
//            var postPK = ""
//            var preDev = ""
//            var postDev = ""
//            var counter = 1
//            var found = false
//            if(ds!=null) {
//                for (child in ds.children) {
//                   val currPK = child.key
//                   val currDev = child.getValue(String::class.java)!!
//                    if(currPK == poolKey){
//                        found = true
//                    }
//                    else if(!found){
//                        prePK = currPK
//                        preDev = currDev
//                        counter++
//                    }
//                    else{
//                        postPK = currPK
//                        postDev = currDev
//                        counter++
//                        break
//                    }
//                }
//
//                if(counter%2==0){
//                    participentPoolKey = prePK
//                    participentId = preDev
//                    dismissConnectionListener()
//                    startListening()
//                    setActionBarSubtitle("online2")
//                }
//                else if(postPK != ""){
//                        participentPoolKey = postPK
//                        participentId = postDev
//                        dismissConnectionListener()
//                    startListening()
//                    setActionBarSubtitle("online1")
//
//                }
//            }
//        }
//
//    }
//
//    fun activateConnectionListener(){
//        poolRef.addValueEventListener(connectionListener)
//    }
//    fun dismissConnectionListener(){
//        poolRef.removeEventListener(connectionListener)
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)


//        setImageSelectorInVisible()


        

        setRecyclerView()

//        poolKey = intent.getStringExtra("poolKey")
//        setActionBarSubtitle("waiting...")
        startListening()

//        connectToServer()
//        activateConnectionListener()
        initializeButtons()
    }

    fun addMessage(msg: Message){
        var mins = -1L
        if (messagesList.size!=0){
            val preMsg = messagesList[messagesList.size-1]
            if (preMsg is Message){
                val diff = msg.timeString-preMsg.timeString
                mins = diff/(1000*60)
//                makeToast("$mins mins ${msg.timeString} ${preMsg.timeString}\n diff $diff", this)

            }
        }
        if (mins==-1L || mins>=1L && (msg is TextMessage))
            messagesList.add(getTimeString())

        messagesList.add(msg)
        myAdapter?.notifyDataSetChanged()
        recycler_view_chat.layoutManager.scrollToPosition(messagesList.size-1)
    }

    private fun setRecyclerView() {
        myAdapter = MyRecyclerViewAdapter(this, messagesList)
        recycler_view_chat.layoutManager = LinearLayoutManager(this)
        recycler_view_chat.adapter = myAdapter
    }

    private fun desetRecyclerView(){
        messagesList.forEach { msg->
            if (msg is ImageMessage){
                msg.thumbnail.recycle()
                File(msg.uri.path).deleteRecursively()
            }
            else if (msg is VideoMessage) {
                File(msg.uri.path).deleteRecursively()
                msg.thumbnail?.recycle()
            }
        }
        messagesList.clear()
        messagesKey.clear()
        messagesKeyMap.clear()
        imagesStatusMap.clear()
        imagesMap.clear()
        myAdapter = null
    }

    private fun connectToServer() {

        val listener = object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError?) {}

            override fun onDataChange(ds: DataSnapshot?) {

                var currKey = ""
                var prePK = ""
                var postPK = ""
                var preDevId = ""
                var postDevId = ""
                var counter = 1
                var found = false
                for (child in ds?.children!!) {
                    currKey = child?.key!!

                    if (currKey.equals(poolKey)) {
                        found = true
                    } else if (!found) {
                        prePK = currKey
                        preDevId = child.getValue(String::class.java)!!
                        counter++
                    } else {
                        postPK = currKey
                        postDevId = child.getValue(String::class.java)!!
                        break
                    }
                }

                if (counter % 2 == 0) {
                    participentPoolKey = prePK
                    participentId = preDevId
                    startListening()
                    setActionBarSubtitle("online2")
                } else {
                    if (firstTurn) {
                        firstTurn = false
                        Handler().postDelayed(Runnable { terminateRecursion = true }, RECURSION_TERMINATION_TIME)
                    }
                    if (!postDevId.equals("")) {
                        participentPoolKey = postPK
                        participentId = postDevId
                        startListening()
                        setActionBarSubtitle("online1")
                    } else if (terminateRecursion)
                        distroy()
                    else
                        connectToServer()
                }


            }

        }

        poolRef.addListenerForSingleValueEvent(listener)

    }


    private fun initializeButtons() {
        val watcher = object: TextWatcher{
            override fun afterTextChanged(text: Editable?) {
                when(text.toString()){
                    "" -> {
                        typingRef.child(androidId).setValue("n")
                        if (sendBtnActive){
                            btn_send_chat.setImageResource(R.drawable.ic_attach_file_black_24dp)
                            sendBtnActive = false
                        }
                    }
                    else -> {
                        typingRef.child(androidId).setValue("y")
                        if (!sendBtnActive){
                            btn_send_chat.setImageResource(R.drawable.ic_menu_send)
                            sendBtnActive = true
                        }
                    }
                }
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        }
        edit_text_chat.addTextChangedListener(watcher)
        val msgSender = {s: String ->
            val msg = s.trim()
            if(msg!=""){
                edit_text_chat.setText("")
                sendMessage(msg)
            }
        }
        edit_text_chat.setOnKeyListener(View.OnKeyListener { view, keycode, keyEvent ->

            when(keycode){
                KeyEvent.KEYCODE_ENTER -> {
                    val str = edit_text_chat.text.toString()
                    msgSender(str)
                    return@OnKeyListener true
                }
            }

            false
        })



        btn_send_chat.setOnTouchListener{view, motionEvent ->

            if (motionEvent.action== MotionEvent.ACTION_UP){
                if (!sendBtnActive){
                    val itemView = LayoutInflater.from(this).inflate(R.layout.popup_view_message_type, null, false)
                    val gallary = itemView?.findViewById<ImageView>(R.id.pv_iv_gallary)
                    val video = itemView?.findViewById<ImageView>(R.id.pv_iv_video)
                    val builder = AlertDialog.Builder(this)
                    builder.setView(itemView)
                    builder.setNegativeButton("cancle", {di, id->
                        di.dismiss()
                    })

                    val ad = builder.create()
                    video?.setOnClickListener {
                        dispatchTakeVideoIntent()
                        ad.dismiss()
                    }
                    gallary?.setOnClickListener {
                        pickImage()
                        ad.dismiss()

                    }


                    ad.show()

//                    pickImage()
//                    captureImage()
//                        dispatchTakeVideoIntent()

                }
                else{
                    val msgStr = edit_text_chat.text.toString()
                     msgSender(msgStr)

                 }

            }
            true
        }
//        btn_send_chat.setOnClickListener {
//
//            if (!sendBtnActive){
//                pickImage()
//
//
//            }
//            else{
//                val msgStr = edit_text_chat.text.toString()
//                msgSender(msgStr)
//
//            }
//        }



//        fab1.setOnClickListener {
//
//        }
//        fab2.setOnClickListener {
//
//        }


//        btn_pic_chat.setOnClickListener {
//
//            if (imageSelectorShowen){
//                setImageSelectorInVisible()
//            }
//            else {
//                setImageSelectorVisible()
//            }
//
//            pick_image_from_gallery_chat.setOnClickListener {
//                setImageSelectorInVisible()
//                pickImage()
//            }
//
//            capture_image_from_camera_chat.setOnClickListener{
//                setImageSelectorInVisible()
//                captureImage()
//            }
//
//        }



    }


    private fun endListening() {

//        val valLis = {r: DatabaseReference, str: String, lis: ValueEventListener-> if(str!= "") r.child(str).removeEventListener(lis) }
//        val childLis = {r: DatabaseReference, str: String, lis: ChildEventListener-> if(str!= "") r.child(str).removeEventListener(lis) }
//        valLis(poolRef, participentPoolKey, poolListener)
//        childLis(chatRef, MainActivity.androidId!!, chatListener)
//        valLis(typingRef, participentId, typingListener)
        if(participentPoolKey!="") poolRef.child(participentPoolKey).removeEventListener(poolListener)
        chatRef.child(androidId).removeEventListener(chatListener)
        if(participentId!="") typingRef.child(participentId).removeEventListener(typingListener)

    }


    private fun startListening() {

        if(participentPoolKey!="" && androidId!="" && participentId!="") {
            poolRef.child(participentPoolKey).addValueEventListener(poolListener)
            chatRef.child(androidId).addChildEventListener(chatListener)
            typingRef.child(participentId).addValueEventListener(typingListener)
        }
        else
            finish()
    }



    private fun sendMessage(msgStr: String) {
        val r = chatRef.child(participentId)
        val k = r.push().key
        addMessage(TextMessage(msgStr, USER_TYPE_SENDER))
        r.child(k).setValue("$MSG_TYPE_TEXT$msgStr")

    }

    var sendBtnActive = false
    private fun animateBtn(){
        if (sendBtnActive){
            btn_send_chat.setBackgroundResource(R.drawable.ic_launcher_foreground)
            sendBtnActive = true
        }
        else{
            btn_send_chat.setBackgroundResource(R.drawable.send_button_bg)
            sendBtnActive = true
        }
    }

//    private fun animateFab(){
//        if (imageSelectorShowen){
//            fab1.visibility = View.INVISIBLE
//            fab2.visibility = View.INVISIBLE
//            imageSelectorShowen = false
//        }
//        else{
//            fab1.visibility = View.VISIBLE
//            fab2.visibility = View.VISIBLE
//            imageSelectorShowen = true
//        }
//    }
//
//    private fun setImageSelectorVisible(){
////        pick_image_from_gallery_chat.visibility = View.VISIBLE
////        capture_image_from_camera_chat.visibility = View.VISIBLE
//        fab1.visibility = View.VISIBLE
//        fab2.visibility = View.VISIBLE
//        imageSelectorShowen = true
//    }
//    private fun setImageSelectorInVisible(){
////        pick_image_from_gallery_chat.visibility = View.INVISIBLE
////        capture_image_from_camera_chat.visibility = View.INVISIBLE
//        fab1.visibility = View.INVISIBLE
//        fab2.visibility = View.INVISIBLE
//        imageSelectorShowen = false
//    }
    private fun pickImage(){
        val intnt = Intent()
        intnt.type = "image/*"
//        intnt.type = "image/* video/*"
        intnt.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intnt, "Select Image"), SELECT_IMAGE_FROM_GALLERY)
    }

    private fun captureImage(){
        val intnt = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if(intnt.resolveActivity(packageManager) != null)
            startActivityForResult(Intent.createChooser(intnt, "Capture Image"), CAPTURE_IMAGE_FROM_CAMERA)
        else
            pickImage()
    }

    private fun dispatchTakeVideoIntent() {

        Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
            takeVideoIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE)
            }
        }

//        val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
//        if (takeVideoIntent.resolveActivity(packageManager) != null) {
//            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE)
//        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (resultCode == RESULT_OK){
            when(requestCode){
                SELECT_IMAGE_FROM_GALLERY -> {
                    try {
                        val uri = data?.data
                        val inputStream = contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        val thumbnail = ThumbnailUtils.extractThumbnail(bitmap, 200, 200)
                        addMessage(ImageMessage(thumbnail,uri!!,  USER_TYPE_SENDER))

//                        addMessage(ImageMessage(thumbnail, USER_TYPE_SENDER))

//                        sendImageDirect(bitmap)
                        val task = SendImageDirectTask(participentId)
                        task.execute(bitmap)
                        imageTaskList.add(task)
//                        sendMessage(bitmap)
                    }
                    catch(e: Exception){}
                }

                CAPTURE_IMAGE_FROM_CAMERA -> {
                    try {
                        val extras = data?.extras
                        val bitmap = extras?.get("data") as Bitmap
                        val scaled = scaledBmp(100, bitmap)
                        val thumbnail = ThumbnailUtils.extractThumbnail(bitmap, 200, 200)

                        addMessage(ImageMessage(thumbnail,getImageUri(bitmap),  USER_TYPE_SENDER))

//                        val uri = data?.data
//                        val builder = AlertDialog.Builder(this)
//                        val itemview = LayoutInflater.from(this).inflate(R.layout.receiver_image, null, false)
//                        val iv = itemview.findViewById<ImageView>(R.id.receiver_msg_image)
//                        iv.setImageURI(getImageUri(bitmap))
//                        builder.setView(itemview)
//                        builder.show()


//                        sendImageDirect(bitmap)
                        val task = SendImageDirectTask(participentId)
                        task.execute(bitmap)
                        imageTaskList.add(task)
//                        sendMessage(bitmap)
                    }
                    catch(e: Exception){}

                }

                REQUEST_VIDEO_CAPTURE-> {
                    try {
                        val videoUri = data?.data as Uri

//                        val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
//                        val cursor = getContentResolver().query(videoUri, filePathColumn, null, null, null)
//                        cursor.moveToFirst()
//                        val columnIndex = cursor.getColumnIndex(filePathColumn[0])
//                        val picturePath = cursor.getString(columnIndex)
//                        cursor.close()
//
//                        val bitmap = ThumbnailUtils.createVideoThumbnail(picturePath, MediaStore.Video.Thumbnails.MICRO_KIND)

//                        val thumbnail = ThumbnailUtils.createVideoThumbnail(videoUri.toString(), MediaStore.Video.Thumbnails.MICRO_KIND )


                          val mMMR = MediaMetadataRetriever();
                          mMMR.setDataSource(this, videoUri);
                          val bmp = mMMR.getFrameAtTime();

                        val thumbnail = scaledBmp(200, bmp)


                        addMessage(VideoMessage(thumbnail, videoUri, USER_TYPE_SENDER))
                        sendVideoDirectly(thumbnail, videoUri)
//                        val tempFile = File.createTempFile("myvid", "mp4")
//                        val file = File(videoUri?.path)
//                        val vrui = Uri.fromFile(file)
//                        val builder = AlertDialog.Builder(this)
//                        val itemView = LayoutInflater.from(this).inflate(R.layout.sender_video, null, false)
//                        val videoView = itemView?.findViewById<VideoView>(R.id.sender_video_video)
//                        videoView?.setVideoURI(vrui)
////                        videoView?.setVideoPath(file.absolutePath)
//                        builder.setView(itemView)
//                        builder.show()
//                        videoView?.start()
                    }
                    catch (e: Exception){
                        sendMessage(e.message!!)
                        makeToast("video error", this)
                    }
                }
            }
        }
    }


    public fun getImageUri(bitmap: Bitmap): Uri{
        val file = File.createTempFile("${System.currentTimeMillis()}", "PNG")
        val fos = FileOutputStream(file)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
//        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "title", null)
        fos.write(baos.toByteArray())
        return Uri.fromFile(file)
    }

    private fun sendVideoDirectly(thumbnail: Bitmap, videoUri: Uri){


        val thumbUri = getImageUri(thumbnail)
        val task = UploadVideoDirectTask()
        task.execute(thumbUri.toString(), videoUri.toString(), participentId)
        videoTaskList.add(task)
//        Intent(this, UploadUriToServer::class.java)
//                .putExtra("thumbnailPath", thumbUri.toString())
//                .putExtra("videoPath", videoUri.toString())
//                .putExtra("participentId", participentId)
//                .also {
//                    intent ->
//                    startService(intent)
//                }

        /*val ref = chatRef.child(participentId)
        val videoKey = ref.push().key
        val sref = storageRef.child("${MainActivity.androidId}/videos/$videoKey")
        val thumbRef = storageRef.child("${MainActivity.androidId}/thumbnails/$videoKey")
        val baos = ByteArrayOutputStream()
        val image = thumbnail.compress(Bitmap.CompressFormat.PNG, 60, baos)
        val bytes = baos.toByteArray()
        thumbRef.putBytes(bytes)

//        thumbRef.putFile()
        sref.putFile(videoUri).addOnSuccessListener {
            val k = ref.push().key
            ref.child(k).setValue("$MSG_TYPE_VIDEO$videoKey")
        }.addOnFailureListener {
            addMessage(TextMessage("Video sending failed...", USER_TYPE_SENDER))
        }*/


//        val baos = ByteArrayOutputStream()
//        val image = bitmap.compress(Bitmap.CompressFormat.PNG, 60, baos)
//        val bytes = baos.toByteArray()
//        sref.putBytes(bytes).addOnSuccessListener {
//            val k = ref.push().key
//            ref.child(k).setValue("$MSG_TYPE_FULL_IMAGE$imageKey")
//        }
    }
    private fun downloadVideoDirectly(videoKey: String){
        val sref = storageRef.child("$participentId/videos/$videoKey")
        val thumbRef = storageRef.child("$participentId/thumbnails/$videoKey")

        val imgFile = File.createTempFile(videoKey, "PNG")
        thumbRef.getFile(imgFile).addOnSuccessListener {
            val thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(imgFile.absolutePath), 200, 200*6/5)
            sref.downloadUrl.addOnSuccessListener {

                addMessage(VideoMessage(thumbnail, it, USER_TYPE_RECEIVER))
            }


        }.addOnFailureListener{
            addMessage(TextMessage("image download faile\n${it.message}", USER_TYPE_RECEIVER))
        }


        /*
        val file = File.createTempFile("$videoKey", "MP4")
        sref.getFile(file).addOnSuccessListener {
            val uri = Uri.fromFile(file)
//            val thumbnail = ThumbnailUtils.createVideoThumbnail(file.absolutePath, MediaStore.Video.Thumbnails.MICRO_KIND)
            val mMMR = MediaMetadataRetriever();
            mMMR.setDataSource(this, uri);
            val bmp = mMMR.getFrameAtTime();
            val thumbnail = scaledBmp(200, bmp)
            val size = BitmapCompat.getAllocationByteCount(thumbnail)
            val fsize = file.length()
//            addMessage(TextMessage("fsize: $fsize\n bmp size: $size", USER_TYPE_RECEIVER))
            addMessage(VideoMessage(thumbnail, uri, USER_TYPE_RECEIVER))
        }.addOnFailureListener {
            addMessage(TextMessage("${it.message}", USER_TYPE_RECEIVER))
        }*/
    }

    private fun sendImageDirect(bitmap: Bitmap){
        val ref = chatRef.child(participentId)
        val imageKey = ref.push().key
        val sref = storageRef.child("${androidId}/images/$imageKey")
        val baos = ByteArrayOutputStream()
        val image = bitmap.compress(Bitmap.CompressFormat.PNG, 60, baos)
        val bytes = baos.toByteArray()
        sref.putBytes(bytes).addOnSuccessListener {
            val k = ref.push().key
            ref.child(k).setValue("$MSG_TYPE_FULL_IMAGE$imageKey")
        }

    }
    private fun downloadImageDirect(imageKey: String){
        val ref = storageRef.child("$participentId/images/$imageKey")
//        ref.getBytes(ONE_MEGA_BYTE).addOnSuccessListener {
//            val bmp = BitmapFactory.decodeByteArray(it, 0, it.size)
//            addMessage(ImageMessage(bmp, USER_TYPE_RECEIVER))
//        }

        val file = File.createTempFile(imageKey, "PNG")
        ref.getFile(file).addOnSuccessListener {
            val thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(file.absolutePath), 200, 200)
            addMessage(ImageMessage(thumbnail, Uri.fromFile(file), USER_TYPE_RECEIVER))
//            val builder = AlertDialog.Builder(this)
//            val itemview = LayoutInflater.from(this).inflate(R.layout.receiver_image, null, false)
//            val iv = itemview.findViewById<ImageView>(R.id.receiver_msg_image)
//            iv.setImageURI(Uri.fromFile(file))
//            builder.setView(itemview)
//            builder.show()
        }.addOnFailureListener{
            addMessage(TextMessage("image download faile\n${it.message}", USER_TYPE_RECEIVER))
        }

    }

    private fun sendMessage(bitmap: Bitmap){
        val scaledBmp = scaledBmp(100, bitmap)
        val ref = chatRef.child(participentId)
        val imageKey = ref.push().key
        val refA = storageRef.child("${androidId}/a/$imageKey")
        val refB = storageRef.child("${androidId}/b/$imageKey")
        val baosA = ByteArrayOutputStream()
        val baosB = ByteArrayOutputStream()
        val imageA = scaledBmp(5, scaledBmp).compress(Bitmap.CompressFormat.PNG, 10, baosA)
        val imageB = bitmap.compress(Bitmap.CompressFormat.PNG, 60, baosB)
        val bytesA = baosA.toByteArray()
        val bytesB = baosB.toByteArray()
        val index = messagesList.size
        addMessage(ImageMessage(scaledBmp,getImageUri(bitmap), USER_TYPE_SENDER))
        imagesMap.put(index, bitmap)
        refA.putBytes(bytesA).addOnSuccessListener {
            val k = ref.push().key
            ref.child(k).setValue("$MSG_TYPE_IMAGE$imageKey")
            refB.putBytes(bytesB).addOnSuccessListener {
                ref.child(k).setValue("$IMAGE_TYPE_B$imageKey")
//                (messagesList[index] as ImageMessage).status = UPLOADING_STATUS_COMPLETE
//                myAdapter?.notifyDataSetChanged()
            }.addOnFailureListener {
//                (messagesList[index] as ImageMessage).status = UPLOADING_STATUS_CANCELED
//                myAdapter?.notifyDataSetChanged()
            }
        }.addOnFailureListener{
//            (messagesList[index] as ImageMessage).status = UPLOADING_STATUS_CANCELED
//            myAdapter?.notifyDataSetChanged()
        }
    }

    private fun downloadImage(imageKey: String, key: String, type: Char){
        when(type){
            IMAGE_TYPE_A-> {
                val refA = storageRef.child("$participentId/a/$imageKey")
                refA.getBytes(ONE_MEGA_BYTE).addOnSuccessListener {
                    val bmp = BitmapFactory.decodeByteArray(it, 0, it.size)
                    val index = messagesList.size
                    messagesKeyMap.put(key, index)
                    addMessage(ImageMessage(bmp, getImageUri(bmp), USER_TYPE_RECEIVER))

                }
            }
            IMAGE_TYPE_B-> {
                val refB = storageRef.child("$participentId/b/$imageKey")
                val index = messagesKeyMap[key]!!
                val msg = messagesList[index]
                refB.getBytes(ONE_MEGA_BYTE).addOnSuccessListener {
                    val bmp = BitmapFactory.decodeByteArray(it, 0, it.size)
                    if(msg is ImageMessage){
                        msg.thumbnail = scaledBmp(150, bmp)
//                        msg.status = UPLOADING_STATUS_COMPLETE
                    }
                    myAdapter?.notifyDataSetChanged()
                    imagesMap.put(index, bmp)
                }.addOnFailureListener {
                    if (msg is ImageMessage)
//                        msg.status = UPLOADING_STATUS_CANCELED
                    myAdapter?.notifyDataSetChanged()
                }
            }
        }

    }


    private fun scaledBmp(i: Int, bmp: Bitmap?): Bitmap {
        return Bitmap.createScaledBitmap(bmp, i, i*6/5, true)
    }

    private fun cancelAllTask(){
        videoTaskList.forEach {
            it.cancel(true)
        }
        imageTaskList.forEach {
            it.cancel(true)
        }
    }

    private fun eraseChatNode(){
        chatRef.child(androidId).setValue(null)
    }

    private fun eraseData() {
        poolRef.child(poolKey).setValue(null)
        poolRef.child(participentPoolKey).setValue(null)
//        eraseChatNode()
    }


     fun setActionBarSubtitle(msg: String) {
        val ab = supportActionBar
        ab?.subtitle = msg
    }
    private fun closeChat(){
//        finish()
        endListening()
//        desetRecyclerView()
    }

    private fun distroy(){


        endListening()
        eraseData()
//        closeChat()
//        dismissConnectionListener()
        cancelAllTask()
    }

    private fun distroyWithchat(){
        eraseChatNode()
        closeChat()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

            menuInflater
                    .inflate(R.menu.option_menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        finish()
        return super.onOptionsItemSelected(item)
    }


    override fun onBackPressed() {
        super.onBackPressed()
        finish()



    }

    override fun onDestroy() {
        super.onDestroy()
//        statusRef.child("clear").child(MainActivity.androidId).setValue("n")
        distroy()

        deleteCache(this)
    }

    fun deleteCache(context: Context) {
        try {
            val dir = context.cacheDir
            deleteDir(dir)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            for (i in children.indices) {
                val success = deleteDir(File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
            return dir.delete()
        } else return if (dir != null && dir.isFile) {
            dir.delete()
        } else {
            false
        }
    }

    val PARTICIPENT_POOL_KEY = "participentPoolKey"
    val PARTICIPENT_ID = "participentId"
    val POOL_KEY = "poolKey"
    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.run {
            putString(PARTICIPENT_POOL_KEY, participentPoolKey)
            putString(PARTICIPENT_ID, participentId)
            putString(POOL_KEY, poolKey)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        poolKey = savedInstanceState?.getString(POOL_KEY)!!
        participentPoolKey = savedInstanceState.getString(PARTICIPENT_POOL_KEY)!!
        participentId = savedInstanceState.getString(PARTICIPENT_ID)!!
    }

}


