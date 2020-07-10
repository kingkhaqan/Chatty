 package com.example.zeesh.chatty

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import kotlinx.android.synthetic.main.activity_reward.*

 class RewardActivity : AppCompatActivity() , RewardedVideoAdListener {

     lateinit var mRewardedVideoAd: RewardedVideoAd

     override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reward)

         var coins = openFileInput(COINS_FILE_NAME).read()
         num_star_rewarded.text = coins.toString()
         MobileAds.initialize(this, REWARDED_VIDEO_AD_TEST_DEVICE_ID)
         mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this)
         mRewardedVideoAd.rewardedVideoAdListener = this
         loadRewardedVideoAd()

        btn_watch_video_rewarded.setOnClickListener {
            if (mRewardedVideoAd.isLoaded)
                showRewardedVideoAd()
//            else
//                coins += 1
//                openFileOutput(COINS_FILE_NAME, Context.MODE_PRIVATE).write(coins)
            num_star_rewarded.text = coins.toString()
        }
    }

     fun loadRewardedVideoAd(){
         mRewardedVideoAd.loadAd(REWARDED_VIDEO_AD_TEST_AD_ID,
                AdRequest.Builder().build()
//                AdRequest.Builder().addTestDevice("2052CF35504CC46E8C949A44804DF14D").build()
//                 AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build()
         )
     }
     fun showRewardedVideoAd(){
         mRewardedVideoAd.show()
     }
     override fun onRewardedVideoAdClosed() {}
     override fun onRewardedVideoAdLeftApplication() {}
     override fun onRewardedVideoAdLoaded() {}
     override fun onRewardedVideoAdOpened() {
//         addCoins(15)
     }
     override fun onRewarded(p0: RewardItem?) {
         var coins = openFileInput(COINS_FILE_NAME).read()
         coins+=10
         openFileOutput(COINS_FILE_NAME, Context.MODE_PRIVATE).write(coins)
         num_star_rewarded.text = coins.toString()
     }
     override fun onRewardedVideoStarted() {}
     override fun onRewardedVideoAdFailedToLoad(p0: Int) {
         makeToast("$p0 not loaded", this)
     }
}
