package com.example.zeesh.chatty

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.graphics.drawable.RoundedBitmapDrawable
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.view.View
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_reward.*
import kotlinx.android.synthetic.main.activity_start.*

var participentId = ""
var participentPoolKey = ""
var poolKey = ""
var androidId = ""

class StartActivity : AppCompatActivity(), RewardedVideoAdListener {

    private var adFailedCount = 0
    private lateinit var mRewardedVideoAd: RewardedVideoAd


    val connectionListener = object: ValueEventListener{
        override fun onCancelled(p0: DatabaseError?) {}

        override fun onDataChange(ds: DataSnapshot?) {

            var prePK = ""
            var postPK = ""
            var preDev = ""
            var postDev = ""
            var counter = 1
            var found = false
            if(ds!=null) {
                for (child in ds.children) {
                    val currPK = child.key
                    val currDev = child.getValue(String::class.java)!!
                    if(currPK == poolKey){
                        found = true
                    }
                    else if(!found){
                        prePK = currPK
                        preDev = currDev
                        counter++
                    }
                    else{
                        postPK = currPK
                        postDev = currDev
                        counter++
                        break
                    }
                }

                if(counter%2==0){
//                    participentPoolKey = prePK
//                    participentId = preDev
//                    dismissConnectionListener()
//                    startActivity(Intent(this@StartActivity, ChatActivity::class.java))
                    startChat(prePK, preDev)
                }
                else if(postPK != ""){
//                    participentPoolKey = postPK
//                    participentId = postDev
//                    dismissConnectionListener()
//                    startActivity(Intent(this@StartActivity, ChatActivity::class.java))

                    startChat(postPK, postDev)
                }
            }
        }

    }

    fun startChat(participentPK: String, participentDev: String){
        participentPoolKey = participentPK
        participentId = participentDev
        dismissConnectionListener()
        startActivity(Intent(this, ChatActivity::class.java))
    }
    fun activateConnectionListener(){
        poolRef.addValueEventListener(connectionListener)
    }
    fun dismissConnectionListener(){
        poolRef.removeEventListener(connectionListener)
    }

    fun jumpIntoPool(key: String){


        poolRef.child(key).setValue(androidId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)


       MobileAds.initialize(this, REWARDED_VIDEO_AD_TEST_DEVICE_ID)
        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this)
        mRewardedVideoAd.rewardedVideoAdListener = this

        loadRewardedVideoAd()

       /* val fio = openFileInput(COINS_FILE_NAME)
        var coins = fio.read()
        num_star_start.text = coins.toString()*/


//        val adRequest = AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build()
//        adView_start.loadAd(adRequest)


        btn_watch_video_start.setOnClickListener {
            if (mRewardedVideoAd.isLoaded)
                mRewardedVideoAd.show()
            else
                Toast.makeText(this, "video is not loaded", Toast.LENGTH_SHORT).show()
        }

        btn_start_start.setOnClickListener {

            MainActivity.chatCount++
            chatCount--
//            val coins = getNumCoins()?.toInt()
//            var coins = fio.read()
//            if (coins>0) {
//                coins--
//                openFileOutput(COINS_FILE_NAME, Context.MODE_PRIVATE).write(coins)
////                num_star_start.text = coins.toString()

            incrementAdRef()

            if (poolKey=="") {
                poolKey = poolRef.push().key
                jumpIntoPool(poolKey)
                activateConnectionListener()
                progress_bar.visibility = View.VISIBLE
            }
//                val intnt = Intent(this, ChatActivity::class.java)
//                intnt.putExtra("poolKey", k)
//                startActivity(intnt)
//            }
//            else
//                makeToast( "Not enough coins", this)



        }

//        btn_get_coins_start.setOnClickListener {
//            mRewardedVideoAd.show()
//        }
    }

    private fun loadRewardedVideoAd() {

        mRewardedVideoAd.loadAd(REWARDED_VIDEO_AD_TEST_AD_ID,
                AdRequest.Builder().build()
//                AdRequest.Builder().addTestDevice("2052CF35504CC46E8C949A44804DF14D").build()
//                AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build()
        )
    }


    fun incrementAdRef(){
        val aref = adRef.child(androidId)


        val transection = object : Transaction.Handler{
            override fun onComplete(e: DatabaseError?, s: Boolean, d: DataSnapshot?) {}
            override fun doTransaction(md: MutableData?): Transaction.Result {
                var num = md?.getValue(Int::class.java)
                if (num!=null)
                    md?.value = num+1

                return Transaction.success(md)
            }
        }

        aref.runTransaction(transection)
    }

    override fun onRewardedVideoAdClosed() {}
    override fun onRewardedVideoAdLeftApplication() {}
    override fun onRewardedVideoAdLoaded() {
//        mRewardedVideoAd.show()
    }
    override fun onRewardedVideoAdOpened() {}
    override fun onRewarded(p0: RewardItem?) {
//        MainActivity.chatCount++
        MainActivity.isAdWatched = true
//        incrementAdRef()
        isVideoAdWatched = true
        enableStartButton()
        adFailedCount = 0
//        adTimeRef.child(MainActivity.androidId).setValue(System.currentTimeMillis().toString())
//        enableStartButton()
    }
    override fun onRewardedVideoStarted() {}
    override fun onRewardedVideoAdFailedToLoad(ec: Int) {
//        if (ec==3)
        adFailedCount++
            enableStartButton()
        if (ec==3)
        loadRewardedVideoAd()
        Toast.makeText(this, "add fialed to load $ec", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()

        progress_bar.visibility = View.GONE
        dismissConnectionListener()
        poolKey = ""
        participentId = ""
        participentPoolKey = ""
        chatRef.child(androidId).setValue(null)

//        val aref = adRef.child(MainActivity.androidId)
//
//        val listener = object: ValueEventListener{
//            override fun onCancelled(de: DatabaseError?) {}
//            override fun onDataChange(ds: DataSnapshot?) {
//                val num = ds?.getValue(Int::class.java)
//                if (num==null)
//                    aref.setValue(0)
//                else if (num%15==0 && !isVideoAdWatched)
//                    disableStartButton()
//
//            }
//
//        }
//
//        aref.addListenerForSingleValueEvent(listener)

        if (MainActivity.chatCount>5 && !MainActivity.isAdWatched)
            disableStartButton()

       /* var coins = openFileInput(COINS_FILE_NAME).read()
        num_star_start.text = coins.toString()

        if (lastAdTimeInMillis!=0L){
            when(lastAdTimeInMillis){
//                CONST_TIME_IN_MILLIS.toLong()->{
//                    if (chatCount==0){
//                        disableStartButton()
//                     //   adTimeRef.child(MainActivity.androidId).setValue(System.currentTimeMillis().toString())
//                    }
//                }
                -1L-> {
                    makeToast("error", this)
                }
                else-> {
                    val current = System.currentTimeMillis()
                    val diff = current- lastAdTimeInMillis
                    val hours = diff/(1000*60*60)
                    if (hours>20 && chatCount==0){
                        if (mRewardedVideoAd.isLoaded)
                            disableStartButton()
                        makeToast("hourse: $hours", this)
                    }
                }
            }
        }*/

    }

    private fun disableStartButton() {
        btn_start_start.setBackgroundResource(R.drawable.start_button_background_gray)
        btn_start_start.setTextColor(resources.getColor(R.color.background_material_dark))
        btn_start_start.isEnabled = false
        btn_watch_video_start.visibility = View.VISIBLE
//        btn_get_coins_start.visibility = View.VISIBLE
    }

    private fun enableStartButton() {
        btn_start_start.setBackgroundResource(R.drawable.start_button_background)
        btn_start_start.setTextColor(resources.getColor(R.color.white))
        btn_start_start.isEnabled = true
        btn_watch_video_start.visibility = View.INVISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()

//        if(poolKey!="")
//        poolRef.child(poolKey).setValue(null)
        Intent(this, ClearFirebaseIntentService::class.java).also { intent ->
            intent.putExtra(EXTRA_ANDROID_ID, androidId)
            startService(intent)
        }
//        clearFDB(MainActivity.androidId)
//        val intent = Intent(this, ClearFirebaseIntentService::class.java)
//        intent.putExtra(EXTRA_ANDROID_ID, MainActivity.androidId)
//        startService(intent)
    }

}
