package com.example.zeesh.chatty

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.storage.FirebaseStorage
import android.content.BroadcastReceiver
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.os.Build
import java.net.InetAddress
import android.net.ConnectivityManager
import android.net.Uri
import android.os.AsyncTask
import android.os.IBinder
import android.provider.Settings
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.firebase.database.*
import android.support.v4.view.ViewPager
import android.widget.LinearLayout
import android.widget.TextView
import java.nio.file.Files.size
import android.support.v4.view.PagerAdapter
import java.io.*
//import com.bumptech.glide.Glide


/**
 * Created by zeesh on 6/11/2018.
 */

val projectString = "Created by zeesh on 6/11/2018."

var isVideoAdWatched = false
var notificationId = 1000
var imageSelectorShowen = false
val imagesMap = mutableMapOf<Int, Bitmap>()


val REQUEST_VIDEO_CAPTURE = 1005
val RECURSION_TERMINATION_TIME: Long = 10000
val RECEIVER_TXT_MSG = 1000
val RECEIVER_IMG_MSG = 1001
val RECEIVER_VID_MSG = 1004
val SENDER_TXT_MSG = 1002
val SENDER_IMG_MSG = 1003
val SENDER_VID_MSG = 1005
val TIME_STAMP_STR = 2000
val VIEW_TYPE_ERR = -1
val USER_TYPE_SENDER = 's'
val USER_TYPE_RECEIVER = 'r'
val MSG_TYPE_TEXT = 'T'
val MSG_TYPE_IMAGE = 'I'
val MSG_TYPE_FULL_IMAGE = 'F'
val MSG_TYPE_VIDEO = 'V'
val SELECT_IMAGE_FROM_GALLERY = 1002
val CAPTURE_IMAGE_FROM_CAMERA = 2001
val IMAGE_TYPE_A = 'A'
val IMAGE_TYPE_B = 'B'
val ONE_MEGA_BYTE: Long = 1024* 1024
val UPLOADING_STATUS_PROGRESS = 'P'
val UPLOADING_STATUS_COMPLETE = 'D'
val UPLOADING_STATUS_CANCELED = 'C'




val database: FirebaseDatabase = FirebaseDatabase.getInstance()
val poolRef = database.getReference("pool")
val chatRef = database.getReference("chat")
val statusRef = database.getReference("status")
val typingRef = statusRef.child("typing")
val adRef = statusRef.child("ad")
val storageRef = FirebaseStorage.getInstance().reference


//=============================================================================


val messagesKeyMap = mutableMapOf<String, Int>()
val imagesStatusMap = mutableMapOf<Int, Char>()



//*****************************************************************************



//==============================================================================
class GetAndroidIdTask: AsyncTask<Context, Unit, String>() {
    override fun doInBackground(vararg param: Context?) : String{
        val context = param[0]
        return Settings.Secure.getString(context?.getContentResolver(), Settings.Secure.ANDROID_ID)
    }

    override fun onPostExecute(result: String?) {

        if (result!=null)
        androidId = result
    }
}

fun getAndroidId(context: Context): String?{
    return  Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID)
}
class SendImageDirectTask(val participentId: String): AsyncTask<Bitmap, Unit, Unit>() {
    override fun doInBackground(vararg param: Bitmap?) {
        val bitmap = param[0]
        val ref = chatRef.child(participentId)
        val imageKey = ref.push().key
        val sref = storageRef.child("${androidId}/images/$imageKey")
        val baos = ByteArrayOutputStream()
        val image = bitmap?.compress(Bitmap.CompressFormat.PNG, 60, baos)
        val bytes = baos.toByteArray()
        sref.putBytes(bytes).addOnSuccessListener {
            if (!isCancelled) {
                val k = ref.push().key
                ref.child(k).setValue("$MSG_TYPE_FULL_IMAGE$imageKey")
            }
        }
    }
}

class UploadVideoDirectTask: AsyncTask<String, Unit, Unit>() {
    override fun doInBackground(vararg param: String?) {
        val thumbnailPath = param[0]
        val videoPath = param[1]
        val participentId = param[2]
        val thumbnailUri = Uri.parse(thumbnailPath)
        val videoUri = Uri.parse(videoPath)

        val ref = chatRef.child(participentId)
        val videoKey = ref.push().key
        val sref = storageRef.child("${androidId}/videos/$videoKey")
        val thumbRef = storageRef.child("${androidId}/thumbnails/$videoKey")

        sref.putFile(videoUri).addOnSuccessListener {
            if (!isCancelled) {
                val k = ref.push().key
                ref.child(k).setValue("$MSG_TYPE_VIDEO$videoKey")
            }
        }

        thumbRef.putFile(thumbnailUri)
    }
}
class DownloadImageDirectTask(val context: Context): AsyncTask<String, Unit, Message>() {
    override fun doInBackground(vararg param: String?): Message? {
        var message: Message? = null
        val imageKey = param[0]
        val participentId = param[1]
        val ref = storageRef.child("$participentId/images/$imageKey")
        val file = File.createTempFile(imageKey, "PNG")
        ref.getFile(file).addOnSuccessListener {
            val thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(file.absolutePath), 200, 200)
            message = (ImageMessage(thumbnail, Uri.fromFile(file), USER_TYPE_RECEIVER))
//            val builder = AlertDialog.Builder(this)
//            val itemview = LayoutInflater.from(this).inflate(R.layout.receiver_image, null, false)
//            val iv = itemview.findViewById<ImageView>(R.id.receiver_msg_image)
//            iv.setImageURI(Uri.fromFile(file))
//            builder.setView(itemview)
//            builder.show()
        }.addOnFailureListener{
            message = (TextMessage("image download faile\n${it.message}", USER_TYPE_RECEIVER))
        }

        return message
    }

    override fun onPostExecute(result: Message?) {

        if (result!=null){
            val ca = context as ChatActivity
            ca.addMessage(result)
        }
    }
}

class DownloadVideoDirectTask(val context: Context): AsyncTask<String, Unit, Message>() {
    override fun doInBackground(vararg param: String?): Message? {
//        if (param.size>0){
            var message: Message? = null
            val videoKey = param[0]
            val participentId = param[1]
            val sref = storageRef.child("$participentId/videos/$videoKey")
            val thumbRef = storageRef.child("$participentId/thumbnails/$videoKey")
            val imgFile = File.createTempFile(videoKey, "PNG")
            thumbRef.getFile(imgFile).addOnSuccessListener {
                val thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(imgFile.absolutePath), 200, 200*6/5)
                sref.downloadUrl.addOnSuccessListener {

                    message = VideoMessage(thumbnail, it, USER_TYPE_RECEIVER)
                }


            }.addOnFailureListener{
                message = TextMessage("image download faile\n${it.message}", USER_TYPE_RECEIVER)
            }
            return message
//        }
    }

    override fun onPostExecute(result: Message?) {

        when{
            result==null->{}
            result is TextView || result is VideoMessage->{
                val a = context as ChatActivity
                a.addMessage(result)
            }
        }
    }
}

val EXTRA_ANDROID_ID = "androidId"
class ClearFirebaseIntentService: Service() {
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val id = intent?.getStringExtra(EXTRA_ANDROID_ID)
        if(id!=null){
//////            Toast.makeText(this, "$id", Toast.LENGTH_SHORT).show()
////            chatRef.child(id).setValue(null)
////            val lis = object: ValueEventListener{
////                override fun onCancelled(p0: DatabaseError?) {}
////
////                override fun onDataChange(ds: DataSnapshot?) {
////                    if(ds != null){
//////                        Toast.makeText(this@ClearFirebaseIntentService, "${ds.childrenCount}", Toast.LENGTH_SHORT).show()
////                        val arr = mutableListOf<String>()
////                        for (child in ds.children){
////                            val k = child.key
//////                            Toast.makeText(this@ClearFirebaseIntentService, "$k", Toast.LENGTH_SHORT).show()
////                            val v = child.getValue(String::class.java)
////                            if(v.equals(id)){
////                                arr.add(k)
////                            }
////
////                        }
////
////                        if (arr.size != 0)
////                            arr.forEach { poolRef.child(it).setValue(null) }
//////                        for (k in arr)
//////                            poolRef.child(k).setValue(null)
////                    }
////                }
//
//            }
//            poolRef.addListenerForSingleValueEvent(lis)

            clearFDB(id)
        }


        return START_STICKY
    }
}

fun clearFDB(id: String?){

    if(id!=null){
        chatRef.child(id).setValue(null)
        val lis = object: ValueEventListener{
            override fun onCancelled(p0: DatabaseError?) {}

            override fun onDataChange(ds: DataSnapshot?) {
                if(ds != null){
                    val arr = mutableListOf<String>()
                    for (child in ds.children){
                        val k = child.key
                        val v = child.getValue(String::class.java)
                        if(v.equals(id)){
                            arr.add(k)
                        }

                    }

                    if (arr.size != 0)
                        arr.forEach { poolRef.child(it).setValue(null) }
//                        for (k in arr)
//                            poolRef.child(k).setValue(null)
                }
            }

        }
        poolRef.addListenerForSingleValueEvent(lis)
    }
}

class NetworkChangeReceiver: BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent?) {
//        Toast.makeText(context, "received", Toast.LENGTH_SHORT).show()
        val offlineString = "Chatty is offline"
        val onlineString = "Chatty is online"
        if (networkIsAvailable(context!!)){

            val androidId = getAndroidId(context)
            if(appRunningInBackground(context)){


                val r =  statusRef.child("clear").child(androidId)
                val lis = object: ValueEventListener{
                    override fun onCancelled(p0: DatabaseError?) {}

                    override fun onDataChange(ds: DataSnapshot?) {
                        val v = ds?.getValue(String::class.java)
                        if(v!=null && v == "n"){
                            clearFDB(androidId)
                            r.setValue("y")
                        }
                    }

                }
               r.addListenerForSingleValueEvent(lis)
            }else {
                val transaction = object : Transaction.Handler {
                    override fun onComplete(e: DatabaseError?, completed: Boolean, d: DataSnapshot?) {
                        val v = d?.getValue(String::class.java)
                        if (completed && v.equals(projectString)) {

                            Toast.makeText(context, onlineString, Toast.LENGTH_SHORT).show()
                        } else Toast.makeText(context, offlineString, Toast.LENGTH_SHORT).show()


                    }

                    override fun doTransaction(md: MutableData?): Transaction.Result = Transaction.success(md)
                }
                database.getReference("project").runTransaction(transaction)
            }

        }
        else  if(!appRunningInBackground(context))   Toast.makeText(context, offlineString, Toast.LENGTH_SHORT).show()

    }

}



fun isOnline(): Boolean? {
    try {
        val p1 = java.lang.Runtime.getRuntime().exec("ping -c 1 www.google.com")
        val returnVal = p1.waitFor()
        return returnVal != 0
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return false
}

fun networkIsAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return connectivityManager.activeNetworkInfo != null && connectivityManager.activeNetworkInfo.isConnected
}

fun internetIsAvailable(): Boolean {
    try {
        val ipAddr = InetAddress.getByName("www.google.com")
        //You can replace it with your name
        return !ipAddr.equals("")

    } catch (e: Exception) {
        return false
    }

}


fun appRunningInBackground(context: Context): Boolean {
    var isInBackground = true
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
        val runningProcesses = am.runningAppProcesses
        for (processInfo in runningProcesses) {
            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                for (activeProcess in processInfo.pkgList) {
                    if (activeProcess == context.packageName) {
                        isInBackground = false
                    }
                }
            }
        }
    } else {

        val taskInfo = am.getRunningTasks(1)
        val componentInfo = taskInfo[0].topActivity
        if (componentInfo.packageName == context.packageName) {
            isInBackground = false
        }
    }

    return isInBackground
}


fun getTimeString(): String {return "10:00"}



open class Message(val timeString:Long = 0L, val user:Char)
class TextMessage(val msg: String, u:Char):Message(System.currentTimeMillis(), u)
class ImageMessage(var thumbnail: Bitmap, val uri: Uri, u:Char):Message(System.currentTimeMillis(), u)
class VideoMessage(var thumbnail: Bitmap?, val uri: Uri, u: Char): Message(System.currentTimeMillis(), u)

class SenderTextMessage(val msg: String): Message(System.currentTimeMillis(), USER_TYPE_SENDER)
class ReceiverTextMessage(val msg: String): Message(System.currentTimeMillis(), USER_TYPE_RECEIVER)
class SenderImageMessage(val thumbnail: Bitmap, val uri: Uri): Message(System.currentTimeMillis(), USER_TYPE_SENDER)
class ReceiverImageMessage(val thumbnail: Bitmap, val uri: Uri): Message(System.currentTimeMillis(), USER_TYPE_RECEIVER)

//class StatusImageMessage(img: Bitmap, u: Char,var  status: Char): ImageMessage(img, u)

class MyRecyclerViewAdapter(val context: Context, val dataset: MutableList<Any>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val lem = {p: Int ->
            val intnt = Intent(context, ViewImageActivity::class.java)
            intnt.putExtra("index", p)
            context.startActivity(intnt)
        }


        if (position%2==0){
            val v = LayoutInflater.from(context).inflate(R.layout.item_view_ad, null, false)
            val adViewHolder = AdViewHolder(v)
            adViewHolder.bind()
        }


        val msg = dataset[position]
//        var mins = 0L
//        if(position!=0){
//            val preMsg = dataset[position-1]
//            val diff = msg.timeString-preMsg.timeString
//            mins = diff/(1000*60)
//
//        }
//        if (mins==0L || mins>=1L){
//            val h = TimeStampHolder(LayoutInflater.from(context).inflate(R.layout.item_view_timestamp, null, false))
//            h.time.text = getTimeString()
//        }

        if (msg is Message) {
            when (holder?.itemViewType) {
                SENDER_TXT_MSG -> {
                    val txtMsg = msg as TextMessage
                    (holder as SenderTextHolder).bind(txtMsg)
                }
                RECEIVER_TXT_MSG -> {
                    val txtMsg = msg as TextMessage
                    (holder as ReceiverTextHolder).bind(txtMsg)
                }
                SENDER_IMG_MSG -> {
                    val imgMsg = msg as ImageMessage
                    (holder as SenderImageHolder).bind(imgMsg)
                    holder.itemView.setOnClickListener {
                        context.startActivity(Intent(context, ImagePreviewActivity::class.java).putExtra("path", imgMsg.uri.toString()))
//                        lem(position)
                    }
                }
                RECEIVER_IMG_MSG -> {
                    val imgMsg = msg as ImageMessage
                    (holder as ReceiverImageHolder).bind(imgMsg)
                    holder.itemView.setOnClickListener {
                        context.startActivity(Intent(context, ImagePreviewActivity::class.java).putExtra("path", imgMsg.uri.toString()))

//                        if (holder.progress.visibility == View.INVISIBLE && holder.cancel.visibility == View.INVISIBLE)
//                            lem(position)
                    }
                }
                SENDER_VID_MSG->{
                    val vidMsg = msg as VideoMessage
                    (holder as SenderVideoHolder).bind(vidMsg)
                    holder.itemView.setOnClickListener {
                        val intnt = Intent(context, VideoPreviewActivity::class.java)
                        intnt.putExtra("path", vidMsg.uri.toString())
                        context.startActivity(intnt)
                    }
                }
                RECEIVER_VID_MSG->{
                    val vidMsg = msg as VideoMessage
                    (holder as ReceiverVideoHolder).bind(vidMsg)
//                    Intent(context, ViewImageActivity::class.java).putExtra("videoUri", vidMsg.uri)
                    holder.itemView.setOnClickListener {
                        val intnt = Intent(context, VideoPreviewActivity::class.java)
                        intnt.putExtra("path", vidMsg.uri.toString())
                        context.startActivity(intnt)
                    }
                }

            }
        }
        else if (msg is String && holder?.itemViewType== TIME_STAMP_STR){
            (holder as TimeStampHolder).time.text = msg
        }

    }

    override fun getItemViewType(position: Int): Int {
        var viewType = VIEW_TYPE_ERR

        val msg = dataset[position]
        if (msg is TextMessage){
            when(msg.user){
                USER_TYPE_SENDER-> viewType = SENDER_TXT_MSG
                USER_TYPE_RECEIVER-> viewType = RECEIVER_TXT_MSG
            }
        }
        else if(msg is ImageMessage){
            when(msg.user){
                USER_TYPE_SENDER-> viewType = SENDER_IMG_MSG
                USER_TYPE_RECEIVER-> viewType = RECEIVER_IMG_MSG
            }
        }
        else if(msg is VideoMessage){
            when(msg.user){
                USER_TYPE_SENDER-> viewType = SENDER_VID_MSG
                USER_TYPE_RECEIVER-> viewType = RECEIVER_VID_MSG
            }
        }
        else if(msg is String){
            viewType = TIME_STAMP_STR
        }
//        if (msg is Message) {
//
//            when (msg.user) {
//                USER_TYPE_SENDER -> {
//                    if (msg is TextMessage)
//                        viewType = SENDER_TXT_MSG
//                    else if(msg is ImageMessage)
//                        viewType = SENDER_IMG_MSG
//                    else if (msg is VideoMessage)
//                        viewType = SENDER_VID_MSG
//                }
//                USER_TYPE_RECEIVER -> {
//                    if (msg is TextMessage)
//                        viewType = RECEIVER_TXT_MSG
//                    else if(msg is ImageMessage)
//                        viewType = RECEIVER_IMG_MSG
//                    else if(msg is VideoMessage)
//                        viewType = RECEIVER_VID_MSG
//                }
//            }
//        }
//        else if (msg is String)
//            viewType = TIME_STAMP_STR

        return viewType
    }

    override fun getItemCount(): Int = dataset.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val inflater = LayoutInflater.from(context)

        when(viewType){
            SENDER_TXT_MSG -> { }
            SENDER_IMG_MSG -> {return SenderImageHolder(inflater.inflate(R.layout.sender_image, parent, false))}
            RECEIVER_TXT_MSG -> {return ReceiverTextHolder(inflater.inflate(R.layout.receiver_text, parent, false))}
            RECEIVER_IMG_MSG -> {return ReceiverImageHolder(inflater.inflate(R.layout.receiver_image, parent, false))}
            TIME_STAMP_STR -> {return TimeStampHolder(inflater.inflate(R.layout.item_view_timestamp, parent, false))}
            SENDER_VID_MSG -> {return SenderVideoHolder(inflater.inflate(R.layout.sender_video, parent, false))}
            RECEIVER_VID_MSG -> {return ReceiverVideoHolder(inflater.inflate(R.layout.receiver_video, parent, false))}
            VIEW_TYPE_ERR -> {}
        }

        return SenderTextHolder(inflater.inflate(R.layout.sender_text, parent, false))
    }

}

class TimeStampHolder(v: View?): RecyclerView.ViewHolder(v){
    val time: TextView

    init {
        time = v?.findViewById<TextView>(R.id.timestamp_text_view)!!
    }
}

class SenderTextHolder(v: View?): RecyclerView.ViewHolder(v){
    val msg: TextView
//    val timestamp: TextView
    init {
        msg = v?.findViewById(R.id.sender_msg_text)!!
//        timestamp = v.findViewById(R.id.sender_timestamp_text)
    }

    fun bind(m: TextMessage){
//        val message = m as TextMessage
        msg.text = m.msg
//        timestamp.text = message.timeString
    }
}


class ReceiverTextHolder(v: View?): RecyclerView.ViewHolder(v){
    val msg: TextView
//    val timestamp: TextView
    init {
        msg = v?.findViewById(R.id.receiver_msg_text)!!
//        timestamp = v.findViewById(R.id.receiver_timestamp_text)
    }

    fun bind(m: TextMessage){
//        val message = m as TextMessage
        msg.text = m.msg
//        timestamp.text = message.timeString
    }
}


class SenderImageHolder(v: View?): RecyclerView.ViewHolder(v){
    val img: ImageView
    val timestamp: TextView
//    val progress: ProgressBar
//    val cancel: ImageView
    init {
        img = v?.findViewById(R.id.sender_msg_image)!!
        timestamp = v.findViewById(R.id.sender_timestamp_image)
//        progress = v.findViewById(R.id.sender_img_progress_bar)
//        cancel = v.findViewById(R.id.sender_img_cancel_image)
    }

    fun bind(m: ImageMessage){
//        val message = m as ImageMessage
        img.setImageBitmap(m.thumbnail)
        timestamp.text = getTimeString()
//        updateStatus(message.status, progress, cancel)
    }
}

fun updateStatus(s: Char, progressBar: ProgressBar, imageView: ImageView){
    progressBar.visibility = View.INVISIBLE
    when(s){
        UPLOADING_STATUS_PROGRESS->{progressBar.visibility = View.VISIBLE}
        UPLOADING_STATUS_CANCELED->{imageView.visibility = View.VISIBLE}
        UPLOADING_STATUS_COMPLETE->{}
    }

}

class ReceiverImageHolder(v: View?): RecyclerView.ViewHolder(v){
    val img: ImageView
    val timestamp: TextView
//    val progress: ProgressBar
//    val cancel: ImageView
    init {
        img = v?.findViewById(R.id.receiver_msg_image)!!
        timestamp = v.findViewById(R.id.receiver_timastampp_image)
//        progress = v.findViewById(R.id.receciver_img_progress_bar)
//        cancel = v.findViewById(R.id.receiver_img_cancel_image)
    }

    fun bind(m: ImageMessage){
//        val message = m as ImageMessage
        img.setImageBitmap(m.thumbnail)
        timestamp.text = getTimeString()
//        updateStatus(message.status, progress, cancel)
    }

}

class SenderVideoHolder(v:View?): RecyclerView.ViewHolder(v){
    val img: ImageView
    val timestamp: TextView

    init {
        img = v?.findViewById(R.id.sender_video_image)!!
        timestamp = v?.findViewById(R.id.sender_video_timestamp)
    }

    fun bind(msg: VideoMessage){
        img.setImageBitmap(msg.thumbnail)
        timestamp.text = getTimeString()
    }
}

class ReceiverVideoHolder(v:View?): RecyclerView.ViewHolder(v){
    val img: ImageView?
    val timestamp: TextView?

    init {
        img = v?.findViewById(R.id.receiver_video_image)
        timestamp = v?.findViewById(R.id.receiver_video_timestamp)
    }

    fun bind(msg: VideoMessage){

        img?.setImageBitmap(msg.thumbnail)
        timestamp?.text = getTimeString()
    }
}

class AdViewHolder(v: View?): RecyclerView.ViewHolder(v){
    val adView: AdView?
    init {
        adView = v?.findViewById(R.id.adView)
    }

    fun bind(){
        val adRequest = AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build()
        adView?.loadAd(adRequest)
    }
}

data class SliderItem(val title: String, val text: String, val image: Int)

class SliderAdapter(private val context: Context,val dataset: List<SliderItem>) : PagerAdapter() {

    override fun getCount(): Int = dataset.size

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.item_slider, null)

//        val linearLayout = view.findViewById<View>(R.id.linearLayout) as LinearLayout
        val titleTV = view.findViewById<TextView>(R.id.slider_title)
        val textTV = view.findViewById<TextView>(R.id.slider_text)
        val imageView = view.findViewById<ImageView>(R.id.slider_image)
        titleTV.text = dataset[position].title
        textTV.text = dataset[position].text
        imageView.setImageResource(dataset[position].image)

        val viewPager = container as ViewPager
        viewPager.addView(view, 0)

        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val viewPager = container as ViewPager
        val view = `object` as View
        viewPager.removeView(view)
    }
}

val REWARDED_VIDEO_AD_TEST_AD_ID = "ca-app-pub-3940256099942544/5224354917"
val REWARDED_VIDEO_AD_TEST_DEVICE_ID = "ca-app-pub-3940256099942544~3347511713"

    val COINS_FILE_NAME = "coins.txt"
var mCoins: Coins? = null
class Coins(val fis: FileInputStream,val fos: FileOutputStream){

    fun setCoins(num: Int) = fos.write(num)
    fun getCoins() = fis.read()
}
fun makeToast(msg: String, context: Context) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

val CONST_TIME_IN_MILLIS = "852058800000"
var lastAdTimeInMillis:Long = 0
var chatCount = 4
val adTimeRef = database.getReference("ad_time")