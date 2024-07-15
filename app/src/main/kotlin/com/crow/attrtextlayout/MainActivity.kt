@file:Suppress("SameParameterValue")

package com.crow.attrtextlayout

import android.graphics.Color
import android.os.Bundle
import android.provider.SyncStateContract.Helpers.update
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.crow.text.StaticTextView
import com.crow.attrtextlayout.databinding.ActivityMainBinding
import com.crow.base.tools.extensions.copyFolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import kotlin.concurrent.thread


@Suppress("SpellCheckingInspection")
class MainActivity : AppCompatActivity() {

    private val mBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private var mContent: String = ""
//    val mContent = "好吧我觉得有BUG-确定吗？？？？我觉得是肯定的！！qweiqx@%!xTIQNAQWENXOQWEM#&IA我阿斯顿维拉4i9992188nnaduqwuzxucqwbdq!@$@#@snajaiw"
//    val mContent = "好吧我觉得有BUG-确定吗？？？？我觉得是肯定的！！qweiqx@%!xTIQNAQWENXOQWEM#&IA"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onCreate()



        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                copyFolder("content")
                copyFolder("font")
                mContent = File(filesDir, "content/Content.txt").readText()
            }
            lifecycleScope.launch {
//                mBinding.text.mText = "111111111111\n2222222222222\n33333333333\n789\n890\n901"
//                mBinding.text2.mText = ""
//                mBinding.text3.mText = "111111111111\n2222222222222\n33333333333\n789\n890\n901"
            }
//            mBinding.attrTextLayout.mText = mContent
            /*withContext(Dispatchers.IO) {
                repeat(Int.MAX_VALUE) {
                    mBinding.attrTextLayout.mText = "$it $mContent"
                    delay(1000)
                }
            }*/
            /*repeat(10) {
               delay((100..700).random().toLong())
                createAttrTextLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, AttrTextLayout.ANIMATION_MOVE_X_HIGH_BRUSH_DRAW)
            }*/
            /*repeat(Int.MAX_VALUE) {
                delay(500)
                mBinding.attrTextLayout.mTextFrameConfig = AttrTextFrameConfig(
                    mLeft = true,
                    mTop = true,
                    mRight = true,
                    mBottom = true, mLineWidth = (20..50).random().toFloat())
            }*/
//             createAttrTextLayout(128, FrameLayout.LayoutParams.WRAP_CONTENT, AttrTextLayout.ANIMATION_MOVE_X_HIGH_BRUSH_DRAW)
        }
    }

    private fun onCreate() {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.WHITE, Color.WHITE),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContentView(mBinding.root)
        WindowCompat.getInsetsController(window, mBinding.root).apply {
            isAppearanceLightStatusBars = false
            hide(WindowInsetsCompat.Type.systemBars())
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    private fun createAttrTextLayout(width: Int, height: Int, animationStrategy: Short) {
TextView(this).setText("")
    }
}