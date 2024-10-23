@file:Suppress("SameParameterValue")

package com.crow.attrtextlayout

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.crow.attrtextlayout.databinding.ActivityMainBinding
import com.crow.base.tools.extensions.copyFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


@Suppress("SpellCheckingInspection")
class MainActivity : AppCompatActivity() {

    private val mBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private var mContent: String = ""
//    val mContent = "在这个匆忙的世界里，我们常常会迷失在日复一日的繁琐之中。每个人都有梦想，但通向梦想的道路总是充满了荆棘与坎坷。正如泰戈尔所言，“天空不曾留下鸟的痕迹，但我已飞过。”我们也许无法在每一次尝试中都取得成功，但每一次努力都会留下印记，成就我们独特的经历和故事。或许，当我们回首过去，会发现那些最艰难的时刻，正是我们最值得珍藏的记忆。坚持梦想，不忘初心，终有一天，我们会到达心中的彼岸。"

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
//                mBinding.text.mText = "aaaaaaaaaaaaa\nbbbbbbbbbbbbbb\ncccccccccccccc\n123\n456\n789"
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