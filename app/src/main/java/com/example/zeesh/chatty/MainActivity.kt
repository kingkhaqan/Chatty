package com.example.zeesh.chatty

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
//import com.example.zeesh.chatty.R.id.viewPager
import com.google.firebase.database.*
import java.io.File
import java.io.OutputStream
import java.nio.file.Files.size
import java.util.*


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.FullscreenTheme_Launcher)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


//        androidId = getAndroidId(this)
//        GetAndroidIdTask().execute(this)

        androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

       /* val STR_FIRST_TIME_LAUNCHED = "firstTimeLaunched"
        val pref = getSharedPreferences("com.example.zeesh.chatty", Context.MODE_PRIVATE)
        val firstTime = pref.getBoolean(STR_FIRST_TIME_LAUNCHED, true)
        if (firstTime){

            lastAdTimeInMillis =  CONST_TIME_IN_MILLIS.toLong()
            adTimeRef.child(androidId).setValue(CONST_TIME_IN_MILLIS)
//            val file = File(COINS_FILE_NAME)
//            file.createNewFile()
//            file.writeText(0.toString(), Charsets.UTF_8)
//            mCoins = Coins(openFileInput(COINS_FILE_NAME), openFileOutput(COINS_FILE_NAME, Context.MODE_PRIVATE))
            val foa = openFileOutput(COINS_FILE_NAME, Context.MODE_PRIVATE)
            foa.write(9)
            pref.edit().putBoolean(STR_FIRST_TIME_LAUNCHED, false)
        }
        else{
            val lis = object: ValueEventListener{
                override fun onCancelled(p0: DatabaseError?) {}
                override fun onDataChange(ds: DataSnapshot?) {
                    val data = ds?.getValue(String::class.java)
                    if (data!=null)
                        lastAdTimeInMillis = data.toLong()
                    else
                        lastAdTimeInMillis = -1
                }
            }

            adTimeRef.child(androidId).addListenerForSingleValueEvent(lis)
        }


       val items = listOf<SliderItem>(
               SliderItem("Welcom to Chatty", "Chat with someone somewhere in the world at one click, no Login required", R.drawable.chat_bubble),
               SliderItem("Global range", "Talk to strangers from all over the world through global village", R.drawable.buildings),
               SliderItem("Secure and safe", "Potentially secure from hacking. End to end decryption is used. Sharing someone other's information to strangers is prohibbited", R.drawable.security)
       )

        viewPager.adapter = SliderAdapter(this, items)
        indicator.setupWithViewPager(viewPager, true)
        val timer = Timer()
        timer.scheduleAtFixedRate(SliderTimer(), 4000, 6000)*/

//        btn_chat_main.setOnClickListener {
            startActivity(Intent(this, StartActivity::class.java))
            finish()
//        }
    }

    companion object {
//        var androidId: String? = ""
        var chatCount = 0
        var isAdWatched = false
    }

    /*private inner class SliderTimer : TimerTask() {

        override fun run() {
            this@MainActivity.runOnUiThread {
                if (viewPager.currentItem <  2) {
                    viewPager.currentItem = viewPager.currentItem + 1
                } else {
                    viewPager.currentItem = 0
                }
            }
        }
    }*/
}
