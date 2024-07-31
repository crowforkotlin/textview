@file:Suppress("MemberVisibilityCanBePrivate", "SameParameterValue",
    "SpellCheckingInspection", "KotlinConstantConditions", "LocalVariableName"
)

package com.crow.text

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Region
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Choreographer
import android.view.View
import android.view.ViewTreeObserver
import java.lang.reflect.Constructor
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sqrt

/**
 * ⦁ 代码较多 ctrl + shift + 减号先全部折叠在展开看说明
 *
 * ⦁ 2024-07-12 18:47:13 周五 下午
 * @author crowforkotlin
 */

/**
 * ⦁ 静态文本组件  V2.0   在此之前有一个新静态文本V1.0是 一个Layout + 两个View实现的性能比之前通用的静态文本高了但仍有不足，后来再次基础上
 * 更近了V2.0 增加了"连续左右移动"（意思是：文本没有进行翻页、而是一行全都显示出来进行走字），当然你也可以切换其他特效从而开启翻页功能，例如默认的左右移动就是翻页
 * V2.0的开发周期较短，移除了V1.0的大量动画特效，在常量中定义我还没有删除，后续可自行在V2.0上扩展
 * V2.0的性能已经比OpenGL绘制的字幕性能高出了很多，并且不开特效最大取决于系统允许的app最大占用内存
 *
 * Choreographer 这个类很重要，用于重载doFrame实现当一帧画面下来时而触发此回调，每一帧必须在16ms内处理完成否则卡顿
 * 当然想要触发doFrame的回调你得获取当前线程的Choreographer，可以通过Choreographer.getInstance得到，我这里已经实现
 * 然后通过postFrameCallback提交回调的触发
 *
 * 在此代码你可以看到有xxxA、xxxB这是用于绘制两个文本而定义的，如果是一个文本默认用xxxA
 *
 * ⦁ 2024-07-12 18:56:18 周五 下午
 * @author: crowforkotlin
 * @formatter:on
 */
class StaticTextView : View, Choreographer.FrameCallback {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { initAttr(context, attrs) }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr) { initAttr( context, attrs) }
    private fun initAttr(context: Context, attributeSet: AttributeSet?) {

        // 兼容在XML中直接创建组件
        context.obtainStyledAttributes(attributeSet, R.styleable.StaticTextView).apply {
            val defaultValue = 0
            mTextFontAbsolutePath = getString(R.styleable.StaticTextView_textFontAbsolutePath)
            mTextFontAssetsPath = getString(R.styleable.StaticTextView_textFontAssetsPath)
            mTextSize = getDimension(R.styleable.StaticTextView_textSize, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, context.resources.displayMetrics))
            mTextBoldEnable = getBoolean(R.styleable.StaticTextView_textBoldEnable, false)
            mTextFakeBoldEnable = getBoolean(R.styleable.StaticTextView_textFakeBoldEnable, false)
            mTextItalicEnable = getBoolean(R.styleable.StaticTextView_textItalicEnable, false)
            mTextFakeItalicEnable = getBoolean(R.styleable.StaticTextView_textFakeItalicEnable, false)
            mTextAntiAliasEnable = getBoolean(R.styleable.StaticTextView_textAntiAliasEnable, false)
            mTextDefaultMonoSpaceEnable = getBoolean(R.styleable.StaticTextView_textDefaultMonoSpaceEnable, false)
            mTextMultipleLineEnable = getBoolean(R.styleable.StaticTextView_textMultipleLineEnable, false)
            mSingleTextAnimationEnable = getBoolean(R.styleable.StaticTextView_singleTextAnimationEnable, false)
            mTextAnimationSpeed = getInt(R.styleable.StaticTextView_textAnimationSpeed, defaultValue)
            mTextRowMargin = getDimensionPixelOffset(R.styleable.StaticTextView_textRowMargin, defaultValue).toFloat()
            mTextCharSpacing = getDimensionPixelOffset(R.styleable.StaticTextView_textCharSpacing, defaultValue).toFloat()
            mTextResidenceTime = getInt(R.styleable.StaticTextView_textResidenceTime, defaultValue).toLong()
            mTextAnimationLeftEnable = getInt(R.styleable.StaticTextView_textAnimationX, defaultValue) == defaultValue
            mTextAnimationTopEnable = getInt(R.styleable.StaticTextView_textAnimationY, defaultValue) == defaultValue
            mTextColor = getColor(R.styleable.StaticTextView_textColor, mTextColor)
            mTextUpdateStrategy = when(val value = getInt(R.styleable.StaticTextView_textUpdateStrategy, STRATEGY_TEXT_UPDATE_ALL_RESET.toInt())) {
                in STRATEGY_TEXT_UPDATE_ALL_CONTINUE..STRATEGY_TEXT_UPDATE_ALL_RESET -> value.toShort()
                else -> kotlin.error("StaticTextView Get Unknow Gravity Value $value!")
            }
            mTextGravity = when(val value = getInt(R.styleable.StaticTextView_textGravity, 1)) {
                in GRAVITY_TOP_START..GRAVITY_BOTTOM_END -> value.toByte()
                else -> kotlin.error("StaticTextView Get Unknow Gravity Value $value!")
            }
            mTextGradientDirection = when(val value = getInt(R.styleable.StaticTextView_textGradientDirection, defaultValue)) {
                0 -> null
                in GRADIENT_BEVEL..GRADIENT_VERTICAL -> value.toByte()
                else -> kotlin.error("StaticTextView Get Unknow GradientDirection Value $value!")
            }
            mTextAnimationMode = when(val value = getInt(R.styleable.StaticTextView_textAnimationMode, ANIMATION_DEFAULT.toInt())) {
                in ANIMATION_DEFAULT..ANIMATION_MOVE_X_HIGH_BRUSH_NO_PAGING_DRAW -> value.toShort()
                else -> kotlin.error("StaticTextView Get Unknow AnimationMode Value $value!")
            }
            mTextAnimationStrategy = when(val value = getInt(R.styleable.StaticTextView_textAnimationStrategy, STRATEGY_ANIMATION_UPDATE_RESET.toInt())) {
                in STRATEGY_ANIMATION_UPDATE_RESET..STRATEGY_ANIMATION_UPDATE_CONTINUA -> value.toShort()
                else -> kotlin.error("StaticTextView Get Unknow AnimationStrategy Value $value!")
            }
            initTextPaint()
            mTextGravity = when(val value = getInt(R.styleable.StaticTextView_textGravity, GRAVITY_TOP_START.toInt())) {
                in GRAVITY_TOP_START..GRAVITY_BOTTOM_END -> value.toByte()
                else -> kotlin.error("StaticTextView Get Unknow Gravity Value $value!")
            }
            val text = getString(R.styleable.StaticTextView_text)
            recycle()
            mText = text ?: return
        }
    }

    var mTextResidenceTime = 1000L
    var mTextAnimationSpeed: Int = 11
        set(value) {
            field = value
            mTextAnimationDuration = when(mTextAnimationSpeed) {
                in 0..10 -> 16L + (abs(11 - mTextAnimationSpeed) shl 2)
                11, 12, 13, 14, 15 -> 0L
                else -> 0L
            }
        }

    var mTextMultipleLineEnable: Boolean = false
        set(value) {
            val isFieldAndValueNotSame = field != value
            field = value
            updateValue {
                if (isFieldAndValueNotSame && !mFirstInit && mLayoutComplete) {
                    initTextListPosition()
                    launchAnimation()
                }
            }
        }

    var mTextAnimationTopEnable: Boolean = false
        set(value) {
            val isFieldAndValueNotSame = field != value
            field = value
            restartViewState(isFieldAndValueNotSame)
        }

    var mTextAnimationLeftEnable: Boolean = true
        set(value) {
            val isFieldAndValueNotSame = field != value
            field = value
            restartViewState(isFieldAndValueNotSame)
        }

    var mTextAnimationMode: Short = ANIMATION_DEFAULT
        set(value) {
            val isFieldAndValueNotSame = field != value
            field = value
            restartViewState(isFieldAndValueNotSame)
        }

    var mSingleTextAnimationEnable: Boolean = true
    var mTextUpdateStrategy : Short = STRATEGY_TEXT_UPDATE_ALL_RESET
    var mTextAnimationStrategy : Short = STRATEGY_ANIMATION_UPDATE_CONTINUA
    var mTextGradientDirection: Byte? = null
    var mTextColor: Int = Color.RED
    var mTextAntiAliasEnable: Boolean = false
    var mTextFakeBoldEnable: Boolean = false
    var mTextFakeItalicEnable: Boolean = false
    var mTextBoldEnable: Boolean = false
    var mTextItalicEnable: Boolean = false
    var mTextCharSpacing: Float = 0f
    var mTextSize: Float = 12f
    var mTextRowMargin: Float = 0f
    var mTextFontAbsolutePath: String? = null
    var mTextFontAssetsPath: String? = null

    // 字体类型是否是等宽字体，如果设置为是，如果字体非等宽却强制设置是，那么绘制将出现问题，如果字体是等宽对于测量文本性能会有所提升
    var mIsMonospace: Boolean = false
    var mTextDefaultMonoSpaceEnable: Boolean = false
        set(value) {
            val isFieldAndValueNotSame = field != value
            field = value
            mIsMonospace = value
            restartViewState(isFieldAndValueNotSame)
        }

    var mTextGravity: Byte = GRAVITY_TOP_START
        set(value) {
            val isFieldAndValueNotSame = field != value
            field = value
            restartViewState(isFieldAndValueNotSame)
        }

    var mText: String? = null
        set(value) {
            field = value
            mHandler?.post {
                if (!mLayoutComplete) {
                    addTask(FLAG_TEXT)
                } else {
                    onVariableChanged(FLAG_TEXT)
                }
            }
        }

    /**
     * ⦁ Async Handler
     *
     * ⦁ 2024-02-20 16:17:18 周二 下午
     * @author crowforkotlin
     */
    private var mHandler: Handler? = null
    private val mChoreographer = Choreographer.getInstance()
    private val mTextPaint = TextPaint()
    private var mHighBrushAnimationListener: (() -> Boolean)? = null
    private var mTextAnimationDuration: Long = 50L
    private var mTextAnimationIsRunning: Boolean = false
    private var mTextAnimationIsTopOrLeft: Boolean = true
    private var mTextAnimationPixelCount = 0
    private var mTextTotalWidth: Float = 0f
    private var mTypeface: Typeface? = null
    private var mTextAListPosition: Int = 0
    private var mTextBListPosition: Int = 0
    private var mTextAX: Float = 0f
    private var mTextAY: Float = 0f
    private var mTextBX: Float = 0f
    private var mTextBY: Float = 0f
    private var mTextAxisValue: Float = 0f
    private var mTextList = mutableListOf<Pair<String, Float>>()
    private var mLayoutComplete: Boolean = false
    private var mTask: MutableList<Byte>? = null
    private var mFirstInit: Boolean = true
    private var mIsCleanText: Boolean = false
    private var _mText: String? = mText
    private var mIsTextReverse: Boolean = false
    private var mIsAwait: Boolean = false
    private val mTaskListRunnable: MutableList<Runnable> = mutableListOf()
    private var mUpdateTextQuickNow: Boolean = false

    init {
        mHandler = Looper.getMainLooper().asHandler(true)

        // 必须设置TextPaint的xfermode，不然绘制的文本会出现丢失边缘，这里选择的是ADD解决和webview以及硬件渲染打开后导致的一些问题
        mTextPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)

        // 在一开始先监听布局是否完成，如果完成，在进行遍历任务，这里的任务是为了防止在View创建的时候还没布局完成就设置变量什么的，可能多余了因为我这里有用到post
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                mLayoutComplete = true
                mTask?.let { task ->
                    task.forEach(::onVariableChanged)
                    task.clear()
                    mTask = null
                }
            }
        })

        // 考虑到如果绘制的文本内容特别多，采用了一个单独的全局线程来处理文本计算宽度
        if (mTaskHandlerThread == null) {
            // 单独利用一个全局的任务线程Handler
            val thread = HandlerThread("StaticTextView_TaskSingletonThread").also { mTaskHandlerThread = it }
            thread.start()
            mTaskHandler = thread.looper.asHandler(true)
        }
    }

    override fun onDetachedFromWindow() {
        // 分离视图的时候移出异步handler的所有消息，停止动画状态，置空handler防止内存泄露
        mTextAnimationIsRunning = false
        mHandler?.let { handler ->
            handler.removeMessages(MESSAGE_TEXT_RESIDENCE_TIME)
            handler.removeMessages(MESSAGE_LAUNCH_DEFAULT_ANIMATION)
            handler.removeMessages(MESSAGE_LAUNCH_ANIMATION)
            handler.removeCallbacksAndMessages(null)
        }
        mHandler = null
        super.onDetachedFromWindow()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 清除文本标志，为true动画并且进行了绘制 将会清空画布内容
        if (mIsCleanText) {
            mIsCleanText = false
            canvas.drawColor(Color.TRANSPARENT)
            return
        }

        val textAxisValue = mTextAxisValue

        // 如果动画是 连续动画，那么就绘制连续动画相关的文本
        if(mTextAnimationMode == ANIMATION_MOVE_X_HIGH_BRUSH_NO_PAGING_DRAW) {
            drawNoPagingText(canvas, _mText ?: return,
                onInitializeTextAX = { tryLayoutHighBrushAX(textAxisValue, if(mTextAnimationLeftEnable) measuredWidth.toFloat() else -mTextTotalWidth, true) },
                onInitializeTextAY = { tryLayoutHighBrushAY(textAxisValue, it) }
            )
            return
        }

        // 先检查文本列表是否为空
        val textListSize = mTextList.size
        if (textListSize == 0) return

        // 检查a和b的文本位置是否超过文本列表超过全部置0 防止数组越界
        val textA = if (mTextAListPosition < textListSize) mTextList[mTextAListPosition] else mTextList[0]
        val textB = if (mTextBListPosition < textListSize) mTextList[mTextBListPosition] else mTextList[0]

        // 这里根据方向 来进行对应绘制，由于函数是inline 对于编译后的代码会很冗余增加内存大小，但相对的减少进行构造多个回调函数和逻辑带来的开销，也可也自己在优化下
        when(mTextGravity) {
            GRAVITY_TOP_START -> {
                drawTopText(canvas, textA, textB, textListSize,
                    onInitializeTextAX = { tryLayoutHighBrushAX(textAxisValue, 0f) },
                    onInitializeTextAY = { tryLayoutHighBrushAY(textAxisValue, it) },
                    onInitializeTextBX = { tryLayoutHighBrushBX(textAxisValue, 0f) },
                    onInitializeTextBY = { tryLayoutHighBrushBY(textAxisValue, it) }
                )
            }
            GRAVITY_TOP_CENTER -> {
                val halfWidth = measuredWidth shr 1
                drawTopText(canvas, textA, textB, textListSize,
                    onInitializeTextAX = { tryLayoutHighBrushAX(textAxisValue, halfWidth - it / 2f) },
                    onInitializeTextAY = { tryLayoutHighBrushAY(textAxisValue, it) },
                    onInitializeTextBX = { tryLayoutHighBrushBX(textAxisValue, halfWidth - it / 2f) },
                    onInitializeTextBY = { tryLayoutHighBrushBY(textAxisValue, it) }
                )
            }
            GRAVITY_TOP_END -> {
                val measuredWidth = measuredWidth
                drawTopText(canvas, textA, textB, textListSize,
                    onInitializeTextAX = { tryLayoutHighBrushAX(textAxisValue, measuredWidth - it) },
                    onInitializeTextAY = { tryLayoutHighBrushAY(textAxisValue, it) },
                    onInitializeTextBX = { tryLayoutHighBrushBX(textAxisValue, measuredWidth - it) },
                    onInitializeTextBY = { tryLayoutHighBrushBY(textAxisValue, it) }
                )
            }
            GRAVITY_CENTER_START -> {
                drawCenterText(canvas, textA, textB, textListSize,
                    onInitializeTextAX = { tryLayoutHighBrushAX(textAxisValue, 0f) },
                    onInitializeTextAY = { tryLayoutHighBrushAY(textAxisValue, it) },
                    onInitializeTextBX = { tryLayoutHighBrushBX(textAxisValue, 0f) },
                    onInitializeTextBY = { tryLayoutHighBrushBY(textAxisValue, it) }
                )
            }
            GRAVITY_CENTER -> {
                val halfMeasuredWidth = measuredWidth shr 1
                drawCenterText(canvas, textA, textB, textListSize,
                    onInitializeTextAX = { tryLayoutHighBrushAX(textAxisValue, halfMeasuredWidth - it / 2f) },
                    onInitializeTextAY = { tryLayoutHighBrushAY(textAxisValue, it) },
                    onInitializeTextBX = { tryLayoutHighBrushBX(textAxisValue, halfMeasuredWidth - it / 2f) },
                    onInitializeTextBY = { tryLayoutHighBrushBY(textAxisValue, it) }
                )
            }
            GRAVITY_CENTER_END -> {
                val measuredWidth = measuredWidth
                drawCenterText(canvas, textA, textB, textListSize,
                    onInitializeTextAX = { tryLayoutHighBrushAX(textAxisValue, measuredWidth - it) },
                    onInitializeTextAY = { tryLayoutHighBrushAY(textAxisValue, it) },
                    onInitializeTextBX = { tryLayoutHighBrushBX(textAxisValue, measuredWidth - it) },
                    onInitializeTextBY = { tryLayoutHighBrushBY(textAxisValue, it) }
                )
            }
            GRAVITY_BOTTOM_START -> {
                drawBottomText(canvas, textA, textB, textListSize,
                    onInitializeTextAX = { tryLayoutHighBrushAX(textAxisValue, 0f) },
                    onInitializeTextAY = { tryLayoutHighBrushAY(textAxisValue, it) },
                    onInitializeTextBX = { tryLayoutHighBrushBX(textAxisValue, 0f) },
                    onInitializeTextBY = { tryLayoutHighBrushBY(textAxisValue, it) }
                )
            }
            GRAVITY_BOTTOM_CENTER -> {
                val halfMeasuredWidth = measuredWidth shr 1
                drawBottomText(canvas, textA, textB, textListSize,
                    onInitializeTextAX = { tryLayoutHighBrushAX(textAxisValue, halfMeasuredWidth - it / 2f) },
                    onInitializeTextAY = { tryLayoutHighBrushAY(textAxisValue, it) },
                    onInitializeTextBX = { tryLayoutHighBrushBX(textAxisValue, halfMeasuredWidth - it / 2f) },
                    onInitializeTextBY = { tryLayoutHighBrushBY(textAxisValue, it) }
                )
            }
            GRAVITY_BOTTOM_END -> {
                val measuredWidth = measuredWidth
                drawBottomText(canvas, textA, textB, textListSize,
                    onInitializeTextAX = { tryLayoutHighBrushAX(textAxisValue, measuredWidth - it) },
                    onInitializeTextAY = { tryLayoutHighBrushAY(textAxisValue, it) },
                    onInitializeTextBX = { tryLayoutHighBrushBX(textAxisValue, measuredWidth - it) },
                    onInitializeTextBY = { tryLayoutHighBrushBY(textAxisValue, it) }
                )
            }
        }
    }
    override fun doFrame(frameTimeNanos: Long) {
        when(mTextAnimationMode) {
            ANIMATION_MOVE_X_HIGH_BRUSH_DRAW -> {
                mTextAnimationPixelCount = if (mTextAnimationLeftEnable) -measuredWidth else measuredWidth
                invalidateHighBrushViewAnimation(mTextAnimationLeftEnable, mTextAnimationPixelCount, mTextAnimationDuration)
            }
            ANIMATION_MOVE_Y_HIGH_BRUSH_DRAW -> {
                mTextAnimationPixelCount = if (mTextAnimationTopEnable) -measuredHeight else measuredHeight
                invalidateHighBrushViewAnimation(mTextAnimationTopEnable, mTextAnimationPixelCount,  mTextAnimationDuration)
            }
            ANIMATION_MOVE_X_HIGH_BRUSH_NO_PAGING_DRAW -> {
                mTextAnimationPixelCount = if (mTextAnimationLeftEnable) -(mTextTotalWidth.toInt() + measuredWidth) else (mTextTotalWidth.toInt() + measuredWidth)
                invalidateHighBrushViewAnimation(mTextAnimationLeftEnable, mTextAnimationPixelCount,  mTextAnimationDuration)
            }
        }
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 重写测量逻辑，实现xml warp 、match 、 固定大小的适配
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val widthIsWrapContent = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST || widthMode == MeasureSpec.UNSPECIFIED
        val heightIsWrapContent = heightMode == MeasureSpec.AT_MOST || heightMode == MeasureSpec.UNSPECIFIED
        val width: Int
        val height: Int
        initTextPaint()
        if (widthIsWrapContent) {
            val screenWidth = resources.displayMetrics.widthPixels
            width = with(mTextPaint.measureText(mText)) { if (this > screenWidth) screenWidth else ceil(this).toInt() }
        } else {
            width = measuredWidth
        }
        if (heightIsWrapContent) {
            val screenHeight = resources.displayMetrics.heightPixels
            height = with(getExactlyTextHeight(mTextPaint.fontMetrics)) { if (this > screenHeight) screenHeight else ceil(this).toInt() }
        } else {
            height = measuredHeight
        }
        setMeasuredDimension(width, height)
    }

    /**
     * ⦁ 动态计算可容纳字符个数获取文本列表
     *
     * ⦁ 2023-10-31 13:34:58 周二 下午
     * @author crowforkotlin
     */
    private fun getTextLists(originText: String, isAnimationNoPaging: Boolean, onComplete: (MutableList<Pair<String, Float>>) -> Unit) {
        mText?.length.info()
        mTaskHandler.sendMessage(object : Runnable {
            override fun run() {
                if (isAnimationNoPaging) {
                    mTextTotalWidth = mTextPaint.measureText(mText)
                    onComplete(mTextList)
                    return
                }
                val viewMeasureWidth = measuredWidth
                if (mIsMonospace) {
                    val width = mTextPaint.measureText(originText[0].toString(), 0, 1)
                    val textMaxCount = (viewMeasureWidth / width).toInt()
                    val textMaxCountWidth = width * textMaxCount
                    val textList = mutableListOf<Pair<String, Float>>()
                    val stringBuilder = StringBuilder()
                    var indexCount = 0
                    originText.forEachIndexed { _, char ->
                        if (indexCount < textMaxCount) {
                            stringBuilder.append(char)
                            indexCount ++
                        } else {
                            textList.add(stringBuilder.toString() to textMaxCountWidth)
                            stringBuilder.clear()
                            stringBuilder.append(char)
                            indexCount = 1
                        }
                    }
                    if (stringBuilder.isNotEmpty()) {
                        textList.add(stringBuilder.toString() to indexCount * width)
                    }
                    onComplete(textList)
                } else {
                    var textStringWidth = 0f
                    val textStringBuilder = StringBuilder()
                    val textList: MutableList<Pair<String, Float>> = mutableListOf()
                    val textMaxIndex = originText.length - 1
                    mTextTotalWidth = 0f
                    originText.forEachIndexed { index, char ->
                        val textWidth = mTextPaint.measureText(char.toString(), 0, 1)
                        mTextTotalWidth += textWidth
                        textStringWidth += textWidth
                        // 字符串宽度 < 测量宽度 假设宽度是 128  那么范围在 0 - 127 故用小于号而不是小于等于
                        if (textStringWidth <= viewMeasureWidth) {
                            if (char == NEWLINE_CHAR_FLAG) {
                                textList.add(textStringBuilder.toString() to textStringWidth - textWidth)
                                textStringBuilder.clear()
                                textStringWidth = 0f
                            } else {
                                if (index == textMaxIndex) {
                                    textStringBuilder.append(char)
                                    textList.add(textStringBuilder.toString() to textStringWidth)
                                    textStringWidth = 0f
                                } else {
                                    textStringBuilder.append(char)
                                }
                            }
                        } else {
                            textList.add(textStringBuilder.toString() to textStringWidth - textWidth)
                            textStringBuilder.clear()
                            textStringBuilder.append(char)
                            if (index == textMaxIndex) {
                                textList.add(textStringBuilder.toString() to textWidth)
                            } else {
                                textStringWidth = textWidth
                            }
                        }
                    }
                    onComplete(textList)
                }
                mTaskListRunnable.remove(this)
            }
        }.also { mTaskListRunnable.add(it) }) { what = this.hashCode() }
    }
    private fun addTask(flag: Byte) {
        if (mTask == null) mTask = mutableListOf(flag) else mTask?.add(flag)
    }
    private fun onVariableChanged(flag: Byte) {
        // 根据FLAG 执行对于Logic
        when(flag) {
            FLAG_TEXT -> {
                val text = mText
                if (text.isNullOrEmpty()) {
                    mTextAListPosition = 0
                    mTextBListPosition = 1
                    mTextList.clear()
                    cancelAnimation()
                    mFirstInit = true
                    mIsCleanText = true
                    invalidate()
                    return
                }
                val isAnimationNoPaging = mTextAnimationMode == ANIMATION_MOVE_X_HIGH_BRUSH_NO_PAGING_DRAW
                if (isAnimationNoPaging) {
                    getTextLists(text, isAnimationNoPaging) {
                        post {
                            if (mFirstInit) {
                                mFirstInit = false
                                mIsAwait = true
                                invalidate()
                                initTextListPosition()
                                launchAnimation(isAwait = false)
                                return@post
                            }
                            if (mTextAnimationStrategy == STRATEGY_ANIMATION_UPDATE_RESET) {
                                launchAnimation()
                            } else {
                                _mText = mText
                            }
                        }
                    }
                } else {
                    mIsAwait = false
                    getTextLists(text, isAnimationNoPaging) {
                        post {
                            mTextList = it
                            if (mFirstInit) {
                                mFirstInit = false
                                invalidate()
                                initTextListPosition()
                                launchAnimation(true)
                                return@post
                            }
                            when(mTextUpdateStrategy) {
                                STRATEGY_TEXT_UPDATE_ALL_RESET -> {
                                    updateTextListPosition(
                                        blockA = { mTextAListPosition = 0 },
                                        blockB = { mTextBListPosition = 0 },
                                    )
                                }
                                STRATEGY_TEXT_UPDATE_ALL_CONTINUE -> { continueTextListPosition() }
                            }
                            if (mUpdateTextQuickNow) {
                                mUpdateTextQuickNow = false
                                invalidate()
                            }
                            if (mTextAnimationStrategy == STRATEGY_ANIMATION_UPDATE_RESET) {
                                launchAnimation(true)
                            }
                            else if(mTextAnimationMode == ANIMATION_DEFAULT) {
                                if (mTextMultipleLineEnable) {
                                    if (getTextMaxLine() <= mTextList.size) launchAnimation()
                                } else {
                                    if (mTextList.size == 1) launchAnimation()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * ⦁ 布局高刷Y轴位置
     *
     * ⦁ 2024-02-01 11:15:21 周四 上午
     * @author crowforkotlin
     */
    private fun tryLayoutHighBrushAY(textAxisValue: Float, originY: Float) : Float {
        var y = originY
        if (mTextAnimationMode == ANIMATION_MOVE_Y_HIGH_BRUSH_DRAW) {
            y += textAxisValue
        }
        return y
    }
    /**
     * ⦁ 布局高刷Y轴位置
     *
     * ⦁ 2024-02-01 11:15:21 周四 上午
     * @author crowforkotlin
     */
    private fun tryLayoutHighBrushBY(textAxisValue: Float, originY: Float) : Float {
        var y = originY
        if (mTextAnimationMode == ANIMATION_MOVE_Y_HIGH_BRUSH_DRAW) {
            y +=((if(mTextAnimationTopEnable) measuredHeight.toFloat() else -measuredHeight.toFloat())) + textAxisValue
        }
        return y
    }
    /**
     * ⦁ 布局高刷X轴位置
     *
     * ⦁ 2024-02-01 11:15:40 周四 上午
     * @author crowforkotlin
     */
    private inline fun tryLayoutHighBrushAX(textAxisValue: Float, originX: Float, isSkip: Boolean = false)  {
        mTextAX = if (mTextAnimationMode == ANIMATION_MOVE_X_HIGH_BRUSH_DRAW || isSkip) {
            originX + textAxisValue
        } else originX
    }
    /**
     * ⦁ 布局高刷X轴位置
     *
     * ⦁ 2024-02-01 11:15:40 周四 上午
     * @author crowforkotlin
     */
    private fun tryLayoutHighBrushBX(textAxisValue: Float, originX: Float)  {
        mTextBX = if (mTextAnimationMode == ANIMATION_MOVE_X_HIGH_BRUSH_DRAW) {
            (if(mTextAnimationLeftEnable) measuredWidth + originX else -(measuredWidth - originX)) + textAxisValue
        } else originX
    }

    private fun invalidateHighBrushViewAnimation(isTopOrLeft: Boolean, highBrushPixelCount: Int, animationDuration: Long) {
        if (!mTextAnimationIsRunning) { return }
        val pixelCount = if(mTextAnimationSpeed > 11) mTextAnimationSpeed - 10 else 1
        if (isTopOrLeft) {
            if (mTextAxisValue > highBrushPixelCount) {
                if (mTextResidenceTime != 0L ) {
                    if (mTextAxisValue - pixelCount <= highBrushPixelCount) {
                        doFrameInvalidate(animationDuration) { mTextAxisValue = highBrushPixelCount.toFloat() }
                    } else {
                        doFrameInvalidate(animationDuration) { mTextAxisValue -= pixelCount }
                    }
                } else {
                    doFrameInvalidate(animationDuration) { mTextAxisValue -= pixelCount }
                }
            } else {
                if (mHighBrushAnimationListener?.invoke() == false) {
                    doFrameInvalidate(animationDuration) {
                        mTextAxisValue += (abs(highBrushPixelCount) - pixelCount)
                    }
                }
            }
        } else {
            if (mTextAxisValue < highBrushPixelCount) {
                if (mTextResidenceTime != 0L ) {
                    if (mTextAxisValue + pixelCount >= highBrushPixelCount) {
                        doFrameInvalidate(animationDuration) { mTextAxisValue = highBrushPixelCount.toFloat() }
                    } else {
                        doFrameInvalidate(animationDuration) { mTextAxisValue += pixelCount }
                    }
                } else {
                    doFrameInvalidate(animationDuration) { mTextAxisValue += pixelCount }
                }
            } else {
                if (mHighBrushAnimationListener?.invoke() == false) {
                    doFrameInvalidate(animationDuration) {
                        mTextAxisValue -= (abs(highBrushPixelCount) + pixelCount)
                    }
                } else {
                    mTextAxisValue = 0f
                }
            }
        }
    }

    /**
     * ⦁ 更新每一帧的视图
     *
     * ⦁ 2024-07-15 16:50:27 周一 下午
     * @author crowforkotlin
     */
    private inline fun doFrameInvalidate(animationDuration: Long, crossinline block: () -> Unit) {
        if (animationDuration == 0L) {
            block()
            invalidate()
            mChoreographer.postFrameCallback(this)
        } else {
            block()
            invalidate()
            mChoreographer.postFrameCallbackDelayed(this, animationDuration)
        }
    }

    /**
     * ⦁ 启动动画
     *
     * ⦁ 2024-07-15 16:53:18 周一 下午
     * @author crowforkotlin
     */
    private fun launchAnimation(isAwait: Boolean = false) {
        if (isAwait) {
            mHandler?.removeMessages(MESSAGE_LAUNCH_ANIMATION)
            cancelAnimation()
            mHandler?.sendMessageDelayed(
                runnable = {
                    if (mTextAnimationMode == ANIMATION_MOVE_X_HIGH_BRUSH_NO_PAGING_DRAW) {
                        if (mText.isNullOrEmpty()) return@sendMessageDelayed
                    } else {
                        if (mTextList.isEmpty()) return@sendMessageDelayed
                    }
                    _mText = mText
                    mIsAwait = false
                    onDrawAnimation()
                },
                config = { what = MESSAGE_LAUNCH_ANIMATION },
                mTextResidenceTime
            )
        } else {
            if (mTextAnimationMode == ANIMATION_MOVE_X_HIGH_BRUSH_NO_PAGING_DRAW) {
                if (mText.isNullOrEmpty()) return
            } else {
                if (mTextList.isEmpty()) return
            }
            cancelAnimation()
            _mText = mText
            mIsAwait = false
            onDrawAnimation()
        }
    }

    /**
     * ⦁  开始执行动画
     *
     * ⦁ 2024-07-15 16:54:34 周一 下午
     * @author crowforkotlin
     */
    private fun onDrawAnimation() {
        runCatching {
            mTextAnimationIsRunning = true
            when(mTextAnimationMode) {
                ANIMATION_DEFAULT -> { launchDefaultAnimation() }
                ANIMATION_MOVE_X_HIGH_BRUSH_DRAW -> { launchPagingXYHighBrushAnimation() }
                ANIMATION_MOVE_Y_HIGH_BRUSH_DRAW -> { launchPagingXYHighBrushAnimation() }
                ANIMATION_MOVE_X_HIGH_BRUSH_NO_PAGING_DRAW -> { launchNoPagingDrawXAnimation() }
            }
        }
            .onFailure { catch ->
                catch.stackTraceToString().error()
            }
    }
    private fun launchNoPagingDrawXAnimation() {
        mHighBrushAnimationListener = {
            mIsTextReverse = !mIsTextReverse
            false
        }
        mChoreographer.postFrameCallback(this)
    }
    private fun launchDefaultAnimation() {
        mHandler?.sendMessage(
            runnable =object : Runnable {
                override fun run() {
                    updateTextListPosition()
                    invalidate()
                    // 如果多行开启 继续检测是否一页即可显示下 或者 单行是否只有一行，满足则退出递归 减少额外性能开销
                    if (mTextMultipleLineEnable) {
                        if (getTextMaxLine() >= mTextList.size) return
                    } else {
                        if (mTextList.size <= 1) return
                    }
                    mHandler?.sendMessageDelayed(this, {
                        what = MESSAGE_LAUNCH_DEFAULT_ANIMATION
                    }, if(mTextResidenceTime < ANIMATION_DEFAULT_MIN_TEXT_RESIDENCE_TIME) ANIMATION_DEFAULT_MIN_TEXT_RESIDENCE_TIME.toLong() else mTextResidenceTime)
                }
            },
            config = { what = MESSAGE_LAUNCH_DEFAULT_ANIMATION }
        )
    }
    private fun launchPagingXYHighBrushAnimation() {
        mHighBrushAnimationListener = {
            if (mTextUpdateStrategy == STRATEGY_TEXT_UPDATE_ALL_RESET) {
                if (mTextAListPosition == mTextBListPosition && mTextList.size >= 2) {
                    initTextListPosition()
                } else {
                    updateTextListPosition()
                }
            } else {
                updateTextListPosition()
            }
            if (mTextResidenceTime != 0L) {
                mHandler?.sendMessageDelayed({
                    mTextAxisValue = 0f
                    mChoreographer.postFrameCallback(this)
                 }, { what = MESSAGE_TEXT_RESIDENCE_TIME }, mTextResidenceTime)
                true
            } else {
                false
            }
        }
        mChoreographer.postFrameCallback(this)
    }

    private fun initTextListPosition()  {
        updateTextListPosition(
            blockA = { mTextAListPosition = 0 },
            blockB = { mTextBListPosition = 1 },
        )
    }
    private fun continueTextListPosition() {
        updateTextListPosition(
            blockA = { },
            blockB = {  },
        )
    }
    private fun updateTextListPosition() {
        updateTextListPosition(
            blockA = {
                mTextAListPosition ++
            },
            blockB = {
                 mTextBListPosition ++
            },
        )
    }
    private inline fun updateTextListPosition(blockA: () -> Unit, blockB: () -> Unit) {
        val textListSize = mTextList.size
        if (mTextMultipleLineEnable) {
            val textMaxLine = getTextMaxLine()
            if (textMaxLine <= 0) return
            var textTotalCount: Int = textListSize / textMaxLine
            if (textListSize  % textMaxLine != 0) { textTotalCount ++ }
            val textTotalCountSubtractOne = textTotalCount - 1
            if (mTextAListPosition < textTotalCountSubtractOne) {
                blockA()
            } else {
                mTextAListPosition = 0
            }
            if (mTextBListPosition < textTotalCountSubtractOne) {
                blockB()
            } else {
                mTextBListPosition = 0
            }
        } else {
            if (mTextAListPosition < textListSize - 1) {
                blockA()
            } else {
                mTextAListPosition = 0
            }
            if (mTextBListPosition < textListSize - 1) {
                blockB()
            } else {
                mTextBListPosition = 0
            }
        }
    }
    private fun getTextMaxLine():Int {
        val textHeightWithMargin = getExactlyTextHeight(mTextPaint.fontMetrics)
        val height = measuredHeight
        return if(textHeightWithMargin > height) 1 else  (height / textHeightWithMargin).toInt()
    }
    private fun getExactlyTextHeight(fontMetrics: Paint.FontMetrics): Float {
        return (fontMetrics.descent -fontMetrics.ascent) + mTextRowMargin
    }

    /**
     * ⦁ 绘制顶部文本 （参数详情和drawCenter函数定义一致）
     *
     * ⦁ 2023-11-04 17:53:43 周六 下午
     * @author crowforkotlin
     */
    private inline fun drawTopText(
        canvas: Canvas,
        textA: Pair<String, Float>,
        textB: Pair<String, Float>,
        textListSize: Int,
        onInitializeTextAY: (Float) -> Float,
        onInitializeTextBY: (Float) -> Float,
        onInitializeTextAX: (Float) -> Unit,
        onInitializeTextBX: (Float) -> Unit
    ) {
        val fontMetrics = mTextPaint.fontMetrics
        if (mTextMultipleLineEnable && textListSize > 1) {
            val measuredHeight = measuredHeight
            val heightHalf = measuredHeight shr 1
            val textHeight = getTextHeight(fontMetrics)
            val textMarginRow = if (mTextRowMargin >= heightHalf) heightHalf.toFloat() else mTextRowMargin
            val textYIncremenet = textHeight + textMarginRow
            val textHeightWithMargin = textHeight + textMarginRow
            val textMaxLine = min(if (measuredHeight < textHeightWithMargin) 1 else (measuredHeight / textHeightWithMargin).toInt(), textListSize)
            var textStartAPos = mTextAListPosition * textMaxLine
            val textAscent = abs(fontMetrics.ascent)
            mTextAY = onInitializeTextAY(textAscent)
            if (mTextAnimationMode == ANIMATION_DEFAULT) {
                repeat(textMaxLine) {
                    if (textStartAPos < textListSize) {
                        val currentTextA = mTextList[textStartAPos]
                        onInitializeTextAX(currentTextA.second)
                        canvas.drawText(currentTextA.first, mTextAX, mTextAY)
                        textStartAPos ++
                        mTextAY += textYIncremenet
                    } else return
                }
            } else {
                var isAOk: Boolean = false
                var isBOk: Boolean = false
                var textStartBPos = mTextBListPosition * textMaxLine
                mTextBY = onInitializeTextBY(textAscent)
                repeat(textMaxLine) {
                    if (textStartAPos < textListSize) {
                        val currentTextA = mTextList[textStartAPos]
                        onInitializeTextAX(currentTextA.second)
                        canvas.drawText(currentTextA.first, mTextAX, mTextAY)
                        textStartAPos ++
                        mTextAY += textYIncremenet
                    } else isAOk = true
                    if (textStartBPos < textListSize) {
                        val currentTextB = mTextList[textStartBPos]
                        onInitializeTextBX(currentTextB.second)
                        canvas.drawText(currentTextB.first, mTextBX, mTextBY)
                        textStartBPos ++
                        mTextBY += textYIncremenet
                    } else isBOk = true
                    if (isAOk && isBOk) return
                }
            }
        } else {
            val absAscent = abs(fontMetrics.ascent)
            onInitializeTextAX(textA.second)
            onInitializeTextBX(textB.second)
            if (mTextAnimationMode == ANIMATION_DEFAULT) {
                mTextAY = onInitializeTextAY(absAscent)
                canvas.drawText(textA.first, mTextAX, mTextAY)
            } else {
                mTextAY = onInitializeTextAY(absAscent)
                mTextBY = onInitializeTextBY(absAscent)
                canvas.drawText(textA.first, textB.first)
            }
        }
    }

    /**
     * ⦁ 绘制中心文本、单行、多行
     *
     * ⦁ 2024-07-12 18:17:44 周五 下午
     * @author crowforkotlin
     * @param textA 文本A
     * @param textB 文本B
     * @param textListSize 文本列表总大小
     * @param onInitializeTextAX 初始化文本A的X坐标
     * @param onInitializeTextAY 初始化文本A的Y坐标
     * @param onInitializeTextBX 初始化文本B的X坐标
     * @param onInitializeTextBY 初始化文本B的Y坐标
     */
    private inline fun drawCenterText(
        canvas: Canvas,
        textA: Pair<String, Float>,
        textB: Pair<String, Float>,
        textListSize: Int,
        onInitializeTextAY: (Float) -> Float,
        onInitializeTextBY: (Float) -> Float,
        onInitializeTextAX: (Float) -> Unit,
        onInitializeTextBX: (Float) -> Unit
    ) {
        val measuredHeight = measuredHeight
        val heightHalf = measuredHeight shr 1
        val fontMetrics = mTextPaint.fontMetrics
        val textBaseLineOffsetY = calculateBaselineOffsetY(fontMetrics)
        if (mTextMultipleLineEnable && textListSize > 1) {
            val textHeight = getTextHeight(fontMetrics)
            var textMarginRowHalf = mTextRowMargin / 2f
            val textHeightWithMargin = textHeight + mTextRowMargin
            val textMaxLine = if (measuredHeight < textHeightWithMargin) 1 else min((measuredHeight / (textHeightWithMargin)).toInt(), textListSize)
            var textAStartPos = (mTextAListPosition * textMaxLine).let { if (it >= textListSize) it - textMaxLine else it }
            val textAValidRow = if (textAStartPos + textMaxLine <= textListSize) textMaxLine else textListSize - textAStartPos
            val textAValidRowHalf = textAValidRow shr 1
            if (textMaxLine == 1 || textAValidRow == 1) textMarginRowHalf = 0f
            var _y: Float
            val yAPosition: Float
            if (mTextAnimationMode == ANIMATION_DEFAULT) {
                if (textAValidRow % 2 == 0) { // 考虑到 偶数、奇数 行居中的效果
                    _y = textBaseLineOffsetY - textMarginRowHalf + ROW_DEVIATION
                    yAPosition = (heightHalf - (textHeightWithMargin * if(textAValidRow < TEXT_HEIGHT_VALID_ROW) 0 else textAValidRowHalf - 1)) - _y
                } else {
                    _y  = textBaseLineOffsetY - ROW_DEVIATION
                    yAPosition = (heightHalf - (textHeightWithMargin * if(textAValidRow < TEXT_HEIGHT_VALID_ROW) 0 else textAValidRowHalf)) + _y
                }
                mTextAY = onInitializeTextAY(yAPosition)
                repeat(textMaxLine) {
                    if (textAStartPos < textListSize) {
                        val currentTextA: Pair<String, Float> = mTextList[textAStartPos]
                        onInitializeTextAX(currentTextA.second)
                        canvas.drawText(currentTextA.first, mTextAX, mTextAY)
                        textAStartPos++
                        mTextAY += textHeightWithMargin
                    } else return
                }
            } else {
                var textBStartPos = (mTextBListPosition * textMaxLine).let { if (it >= textListSize) it - textMaxLine else it }
                val textBValidRow = if (textBStartPos + textMaxLine <= textListSize) textMaxLine else textListSize - textBStartPos
                val textBValidRowHalf = textBValidRow shr 1
                val yBPosition: Float
                if (textAValidRow % 2 == 0) { // 考虑到 偶数、奇数 行居中的效果
//                    _y = textBaseLineOffsetY - textMarginRowHalf + ROW_DEVIATION
                    _y = textBaseLineOffsetY / 2 - textMarginRowHalf + ROW_DEVIATION
                    yAPosition = (heightHalf - (textHeightWithMargin * if(textAValidRow < TEXT_HEIGHT_VALID_ROW) 0 else textAValidRowHalf - 1)) - _y
                } else {
//                    _y = textBaseLineOffsetY / 2 - textMarginRowHalf + ROW_DEVIATION
                    _y  = textBaseLineOffsetY - textMarginRowHalf + ROW_DEVIATION
                    yAPosition = (heightHalf - (textHeightWithMargin * if(textAValidRow < TEXT_HEIGHT_VALID_ROW) 0 else textAValidRowHalf)) + _y
                }
                if (textBValidRow % 2 == 0) { // 考虑到 偶数、奇数 行居中的效果
//                    _y = textBaseLineOffsetY - textMarginRowHalf + ROW_DEVIATION
                    _y = textBaseLineOffsetY / 2 - textMarginRowHalf + ROW_DEVIATION
                    yBPosition = (heightHalf - (textHeightWithMargin * if(textBValidRow < TEXT_HEIGHT_VALID_ROW) 0 else textBValidRowHalf - 1)) - _y
                } else {
//                    _y = textBaseLineOffsetY / 2 - textMarginRowHalf + ROW_DEVIATION
                    _y  = textBaseLineOffsetY - textMarginRowHalf  + ROW_DEVIATION
                    yBPosition = (heightHalf - (textHeightWithMargin * if(textBValidRow < TEXT_HEIGHT_VALID_ROW) 0 else textBValidRowHalf)) + _y
                }
                mTextAY = onInitializeTextAY(yAPosition)
                mTextBY = onInitializeTextBY(yBPosition)
                var isAOk: Boolean = false
                var isBOk: Boolean = false
                repeat(textMaxLine) {
                    if (textAStartPos < textListSize) {
                        val currentTextA: Pair<String, Float> = mTextList[textAStartPos]
                        onInitializeTextAX(currentTextA.second)
                        canvas.drawText(currentTextA.first, mTextAX, mTextAY)
                        textAStartPos ++
                        mTextAY += textHeightWithMargin
                    } else isAOk = true
                    if (textBStartPos < textListSize) {
                        val currentTextB: Pair<String, Float> = mTextList[textBStartPos]
                        onInitializeTextBX(currentTextB.second)
                        canvas.drawText(currentTextB.first, mTextBX, mTextBY)
                        textBStartPos ++
                        mTextBY += textHeightWithMargin
                    } else isBOk = true
                    if (isAOk && isBOk) return
                }
            }
        } else {
            onInitializeTextAX(textA.second)
            onInitializeTextBX(textB.second)
            val centerY = heightHalf + textBaseLineOffsetY
            if (mTextAnimationMode == ANIMATION_DEFAULT) {
                mTextAY = onInitializeTextAY(centerY)
                canvas.drawText(textA.first, mTextAX, mTextAY)
            } else {
                mTextAY = onInitializeTextAY(centerY)
                mTextBY = onInitializeTextBY(centerY)
                canvas.drawText(textA.first, textB.first)
            }
        }
    }

    /**
     * ⦁ 绘制底部文本 （参数详情和drawCenter函数定义一致）
     *
     * ⦁ 2024-07-12 18:20:56 周五 下午
     * @author crowforkotlin
     */
    private inline fun drawBottomText(
        canvas: Canvas,
        textA: Pair<String, Float>,
        textB: Pair<String, Float>,
        textListSize: Int,
        onInitializeTextAY: (Float) -> Float,
        onInitializeTextBY: (Float) -> Float,
        onInitializeTextAX: (Float) -> Unit,
        onInitializeTextBX: (Float) -> Unit
    ) {
        if (mTextMultipleLineEnable && textListSize > 1) {
            val measuredHeight = measuredHeight
            val heightHalf = measuredHeight shr 1
            val textFontMetrics = mTextPaint.fontMetrics
            val textMarginRow = if (mTextRowMargin >= heightHalf) heightHalf.toFloat() else mTextRowMargin
            val textHeight = getTextHeight(textFontMetrics)
            val textHeightWithMargin = textHeight + textMarginRow
            val textMaxLine =  if (measuredHeight < textHeightWithMargin) 1 else (measuredHeight / textHeightWithMargin).toInt()
            var textAStartPos = (mTextAListPosition + 1) * textMaxLine
            val textAEndPos = mTextAListPosition * textMaxLine
            val textYIncrement = textHeight + textMarginRow
            textAStartPos = if (textListSize >= textAStartPos) textAStartPos - 1 else textListSize - 1
            val yPosition = measuredHeight - calculateBaselineOffsetY(textFontMetrics)
            mTextAY = onInitializeTextAY(yPosition)
            if (mTextAnimationMode == ANIMATION_DEFAULT) {
                repeat(textMaxLine) {
                    if (textAStartPos >= textAEndPos) {
                        val currentTextA: Pair<String, Float> = mTextList[textAStartPos]
                        onInitializeTextAX(currentTextA.second)
                        canvas.drawText(currentTextA.first, mTextAX, mTextAY)
                        textAStartPos--
                        mTextAY -= textYIncrement
                    } else return
                }
            } else {
                var textBStartPos = (mTextBListPosition + 1) * textMaxLine
                val textBEndPos = mTextBListPosition * textMaxLine
                textBStartPos = if (textListSize >= textBStartPos) textBStartPos - 1 else textListSize - 1
                mTextBY = onInitializeTextBY(yPosition)
                var isAOk: Boolean = false
                var isBOk: Boolean = false
                repeat(textMaxLine) {
                    if (textAStartPos >= textAEndPos) {
                        val currentTextA: Pair<String, Float> = mTextList[textAStartPos]
                        onInitializeTextAX(currentTextA.second)
                        canvas.drawText(currentTextA.first, mTextAX, mTextAY)
                        textAStartPos --
                        mTextAY -= textYIncrement
                    } else isAOk = true
                    if (textBStartPos >= textBEndPos) {
                        val currentTextB: Pair<String, Float> = mTextList[textBStartPos]
                        onInitializeTextBX(currentTextB.second)
                        canvas.drawText(currentTextB.first, mTextBX, mTextBY)
                        textBStartPos --
                        mTextBY -= textYIncrement
                    } else isBOk = true
                    if (isAOk && isBOk) return
                }
            }
        } else {
            val bottomY = measuredHeight - calculateBaselineOffsetY(mTextPaint.fontMetrics)
            onInitializeTextAX(textA.second)
            onInitializeTextBX(textB.second)
            if (mTextAnimationMode == ANIMATION_DEFAULT) {
                mTextAY = onInitializeTextAY(bottomY)
                canvas.drawText(textA.first, mTextAX, mTextAY)
            } else {
                mTextAY = onInitializeTextAY(bottomY)
                mTextBY = onInitializeTextBY(bottomY)
                canvas.drawText(textA.first, textB.first)
            }
        }
    }


    /**
     * ⦁ 绘制无翻页的文本，无翻页只需要一个A即可。
     *
     * ⦁ 2024-07-12 18:21:12 周五 下午
     * @author crowforkotlin
     */
    private  inline fun drawNoPagingText(
        canvas: Canvas,
        text: String,
        onInitializeTextAY: (Float) -> Float,
        onInitializeTextAX: (Float) -> Unit
    ) {
        val fontMetrics = mTextPaint.fontMetrics
        val absAscent = abs(fontMetrics.ascent)
        onInitializeTextAX(mTextTotalWidth)
        mTextAY = onInitializeTextAY(absAscent)
        canvas.drawText(text, mTextAX, mTextAY)
    }

    /**
     * ⦁ 绘制文本 可以根据是否开启调试模式进行一些额外的效果预览
     *
     * ⦁ 2023-12-22 19:05:29 周五 下午
     * @author crowforkotlin
     */
    private fun Canvas.drawText(textA: String, textB: String) {
        debugText(
            onDebug = {
                withSave {
                    drawText(textA, 0, textA.length, mTextAX, mTextAY, mTextPaint)
                    drawDebugTextLine(this)
                }
                withSave {
                    drawText(textB, 0, textB.length, mTextBX, mTextBY, mTextPaint)
                    drawDebugTextLine(this)
                }

            },
            orElse = {
                withSave { drawText(textA, 0, textA.length, mTextAX, mTextAY, mTextPaint) }
                withSave { drawText(textB, 0, textB.length, mTextBX, mTextBY, mTextPaint) }
            }
        )
    }
    private fun Canvas.drawText(text: String, x: Float, y: Float) {
        debugText(
            onDebug = {
                withSave {
                    drawText(text, 0, text.length, x, y, mTextPaint)
                    drawDebugTextLine(this)
                }
            },
            orElse = {
                withSave { drawText(text, 0, text.length, x, y, mTextPaint) }
            }
        )
    }
    private inline fun Canvas.withSave(block: () -> Unit) {
        save()
        block()
        restore()
    }

    /**
     * ⦁ 重新启动view的状态 实际上就是重新启动动画
     *
     * ⦁ 2024-07-12 18:23:04 周五 下午
     * @author crowforkotlin
     */
    private fun restartViewState(isOk: Boolean) {
        if (isOk && !mFirstInit && mLayoutComplete) {
            mHandler?.post {
                launchAnimation()
            }
        }
    }

    /**
     * ⦁ 调试模式的文本线
     *
     * ⦁ 2023-11-07 18:44:26 周二 下午
     * @author crowforkotlin
     */
    private fun drawDebugTextLine(canvas: Canvas) {

        // 绘制中线
        val paint = TextPaint()
        paint.color = Color.GREEN
        paint.strokeWidth = DEBUG_STROKE_WIDTH
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
        canvas.drawLine(0f, (measuredHeight / 2).toFloat(), measuredWidth.toFloat(), (measuredHeight / 2).toFloat(), paint)
        canvas.drawLine(measuredWidth / 2f, 0f, measuredWidth / 2f, measuredHeight.toFloat(), paint)

        // 绘制底部线
        paint.color = Color.WHITE
        canvas.drawLine(0f, mTextAY, measuredWidth.toFloat(), mTextAY, paint)
        canvas.drawLine(0f, mTextBY, measuredWidth.toFloat(), mTextBY, paint)

        // 绘制基线
        val ascentAY = mTextAY - abs(mTextPaint.fontMetrics.ascent)
        canvas.drawLine(0f, ascentAY, measuredWidth.toFloat(), ascentAY, paint)

        // 绘制基线
        val ascentBY = mTextBY - abs(mTextPaint.fontMetrics.ascent)
        canvas.drawLine(0f, ascentBY, measuredWidth.toFloat(), ascentBY, paint)


        // 蓝框范围
        paint.color = Color.CYAN
        paint.style = Paint.Style.STROKE
        canvas.drawRect(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat(), paint)

        mTextPaint.color = Color.RED
    }

    /**
     * ⦁ 计算 baseline 的相对文字中心的偏移量
     *
     * ⦁ 2023-10-31 13:34:50 周二 下午
     * @author crowforkotlin
     */
    private fun calculateBaselineOffsetY(fontMetrics: Paint.FontMetrics): Float {
        return -fontMetrics.ascent / 2f - fontMetrics.descent / 2f
    }

    /**
     * ⦁ 获取文本绝对高度：ascent绝对值 + descent
     *
     * ⦁ 2023-11-29 17:01:15 周三 下午
     * @author crowforkotlin
     */
    private fun getTextHeight(fontMetrics: Paint.FontMetrics): Float {
        return fontMetrics.descent - fontMetrics.ascent
    }

    /**
     * ⦁ 初始化文本画笔
     *
     * ⦁ 2023-12-28 18:33:08 周四 下午
     * @author crowforkotlin
     */
    private fun initTextPaint() {
        mTextPaint.apply {
            // 设置线性渐变效果
            val widthFloat = width.toFloat()
            val heightFloat = height.toFloat()
            val halfWidth = width / 2f
            val halfHeight = height / 2f
            shader = when(mTextGradientDirection) {
                GRADIENT_BEVEL -> LinearGradient(0f, 0f, widthFloat, heightFloat, intArrayOf(Color.RED, Color.GREEN, Color.BLUE), null, Shader.TileMode.CLAMP)
                GRADIENT_VERTICAL -> LinearGradient(halfWidth, 0f, halfWidth, heightFloat, intArrayOf(Color.RED, Color.GREEN, Color.BLUE), null, Shader.TileMode.CLAMP)
                GRADIENT_HORIZONTAL -> LinearGradient(0f, halfHeight, widthFloat, halfHeight, intArrayOf(Color.RED, Color.GREEN, Color.BLUE), null, Shader.TileMode.CLAMP)
                else -> { null }
            }
            color = mTextColor
            isAntiAlias = mTextAntiAliasEnable
            textSize = mTextSize
            isFakeBoldText = mTextFakeBoldEnable
            textSkewX = if (mTextFakeItalicEnable) -0.25f else 0f
            letterSpacing = mTextCharSpacing / mTextPaint.textSize
            initTextPaintTypeFace(this)
        }
    }
    private fun initTextPaintTypeFace(textPaint: TextPaint) {
        val value = when {
            mTextDefaultMonoSpaceEnable -> {
                textPaint.typeface = Typeface.MONOSPACE
                return
            }
            mTextBoldEnable && mTextItalicEnable -> Typeface.BOLD_ITALIC
            mTextBoldEnable -> Typeface.BOLD
            mTextItalicEnable -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        val typeface = runCatching {
            when {
                mTextFontAssetsPath != null -> createTypefaceFromAssets(value)
                mTextFontAbsolutePath != null -> createTypefaceFromFile(value)
                else -> if(mTypeface != null) Typeface.create(mTypeface, value) else Typeface.create(
                    Typeface.DEFAULT, value)
            }
        }.getOrElse { cause ->
            cause.stackTraceToString().error()
            Typeface.create(if (mTextDefaultMonoSpaceEnable) Typeface.MONOSPACE else Typeface.DEFAULT, value)
        }
        mTextPaint.typeface = typeface
    }
    private fun createTypefaceFromAssets(value: Int?): Typeface {
        val baseTypeface = Typeface.createFromAsset(context.assets, mTextFontAssetsPath ?: return if (mTypeface != null) Typeface.create(mTypeface, value ?: Typeface.NORMAL) else Typeface.DEFAULT)
        return if (value == null) baseTypeface else Typeface.create(baseTypeface, value)
    }
    private fun createTypefaceFromFile(value: Int?): Typeface {
        val baseTypeface = Typeface.createFromFile(mTextFontAbsolutePath ?: return if (mTypeface != null) Typeface.create(mTypeface, value ?: Typeface.NORMAL) else Typeface.DEFAULT)
        return if (value == null) baseTypeface else Typeface.create(baseTypeface, value)
    }

    /*  ------------------------------------------------------------------------------------
    *下面的drawXXX是用于绘制一些动画效果,以及下面有一些功能性函数，扩展使用
    * */

    /**
     * ⦁ 绘制菱形
     *
     * ⦁ 2023-12-25 15:19:02 周一 下午
     * @author crowforkotlin
     */
    private fun Canvas.drawRhombus(path: Path, width: Int, height: Int, fraction: Float) {
        val halfWidth = width shr 1
        val halfHeight = height shr 1
        val halfWidthFloat = halfWidth.toFloat()
        val halfHeightFloat = halfHeight.toFloat()
        val xRate = width * fraction
        val yRate = height * fraction
        path.moveTo(halfWidthFloat, -halfHeight + yRate)
        path.lineTo(-halfWidth + xRate, halfHeightFloat)
        path.lineTo(halfWidthFloat, height + halfHeight - yRate)
        path.lineTo(width + halfWidth - xRate, halfHeightFloat)
        debugAnimation {
            drawLine(halfWidthFloat, -halfHeight + yRate, halfWidthFloat + halfWidthFloat, (-halfHeight + yRate) + (-halfHeight + yRate),
                mDebugYellowPaint
            )
            drawLine(-halfWidth + xRate, halfHeightFloat, (-halfWidth + xRate) + (-halfWidth + xRate), halfHeightFloat + halfHeightFloat,
                mDebugYellowPaint
            )
            drawLine(halfWidthFloat, height + halfHeight - yRate, halfWidthFloat + halfWidthFloat, (height + halfHeight - yRate) + (height + halfHeight - yRate),
                mDebugYellowPaint
            )
            drawLine(width + halfWidth - xRate, halfHeightFloat, (width + halfWidth - xRate) + (width + halfWidth - xRate), halfHeightFloat + halfHeightFloat,
                mDebugYellowPaint
            )
        }
    }

    /**
     * ⦁ 绘制圆形 时钟动画
     *
     * ⦁ 2023-12-25 15:22:48 周一 下午
     * @author crowforkotlin
     */
    private fun Canvas.drawOval(path: Path, width: Int, height: Int, fraction: Float) {
        val widthFloat = width.toFloat()
        val heightFloat = height.toFloat()
        val diagonal = sqrt(widthFloat * widthFloat + heightFloat * heightFloat)
        val widthHalf = widthFloat / 2f
        val heightHalf = heightFloat / 2f
        path.addArc(widthHalf - diagonal, heightHalf - diagonal, widthFloat + diagonal - widthHalf, heightFloat + diagonal -heightHalf,270f,360 * fraction)
        path.lineTo(widthHalf,heightHalf)
        debugAnimation {
            drawLine(widthHalf - diagonal, heightHalf - diagonal, width + diagonal - widthHalf, height + diagonal - heightHalf,
                mDebugBluePaint
            )
            drawLine(0f, 0f, widthHalf, heightHalf, mDebugBluePaint)
        }
    }

    /**
     * ⦁ 绘制十字扩展 动画
     *
     * ⦁ 2023-12-25 15:23:15 周一 下午
     * @author crowforkotlin
     */
    private fun Canvas.drawCrossExtension(width: Int, height: Int, fraction: Float) {
        val rectXRate = (width shr 1) * fraction
        val rectYRate = (height shr 1) * fraction
        val widthFloat = width.toFloat()
        val heightFloat = height.toFloat()
        drawCrossExtension(rectXRate, rectYRate, widthFloat, heightFloat)
    }

    /**
     * ⦁ 绘制十字扩展 动画
     *
     * ⦁ 2023-12-25 15:23:15 周一 下午
     * @author crowforkotlin
     */
    private fun Canvas.drawCrossExtension(rectXRate: Float, rectYRate: Float, widthFloat: Float, heightFloat: Float) {
        withApiO(
            leastO = {
                clipOutRect(0f,  rectYRate, widthFloat, heightFloat - rectYRate) // 上下
                clipOutRect(rectXRate, 0f, widthFloat - rectXRate, heightFloat)  // 左右
                debugAnimation {
                    drawLine(0f,  rectYRate, widthFloat, heightFloat - rectYRate,
                        mDebugBluePaint
                    ) // 上下
                    drawLine(rectXRate, 0f, widthFloat - rectXRate, heightFloat,
                        mDebugBluePaint
                    )  // 左右
                }
            },
            lessO = {
                clipRect(0f,  rectYRate, widthFloat, heightFloat - rectYRate, Region.Op.DIFFERENCE) // 上下
                clipRect(rectXRate, 0f, widthFloat - rectXRate, heightFloat, Region.Op.DIFFERENCE)  // 左右
                debugAnimation {
                    drawLine(0f,  rectYRate, widthFloat, heightFloat - rectYRate,
                        mDebugBluePaint
                    ) // 上下
                    drawLine(rectXRate, 0f, widthFloat - rectXRate, heightFloat,
                        mDebugBluePaint
                    )  // 左右
                }
            }
        )
    }

    /**
     * ⦁ 绘制同方向 反效果的十字扩展 动画
     *
     * ⦁ 2023-12-25 15:23:52 周一 下午
     * @author crowforkotlin
     */
    private fun Canvas.drawDifferenceCrossExtension(rectXRate: Float, rectYRate: Float, widthFloat: Float, heightFloat: Float) {
        clipRect(0f,  rectYRate, widthFloat, height - rectYRate) // 上下
        clipRect(rectXRate, 0f, width - rectXRate, height.toFloat())  // 左右
        debugAnimation {
            drawLine(0f,  rectYRate, widthFloat, height - rectYRate, mDebugYellowPaint)
            drawLine(rectXRate, 0f, width - rectXRate, height.toFloat(), mDebugYellowPaint)
        }
    }

    /**
     * ⦁ 绘制擦除Y轴方向的动画
     *
     * ⦁ 2023-12-25 15:24:31 周一 下午
     * @author crowforkotlin
     */
    private fun Canvas.drawEraseY(widthFloat: Float, heightFloat: Float, yRate: Float) {
        drawY(
            onTop = {
                clipRect(0f, heightFloat - yRate, widthFloat, heightFloat)
                debugAnimation { drawLine(0f, heightFloat - yRate, widthFloat, heightFloat,
                    mDebugBluePaint
                ) }
            },
            onBottom = {
                clipRect(0f, 0f, widthFloat, yRate)
                debugAnimation { drawLine(0f, 0f, widthFloat, yRate, mDebugYellowPaint) }
            }
        )
    }

    /**
     * ⦁ 绘制同方向 反效果的擦除Y轴方向的动画
     *
     * ⦁ 2023-12-25 15:24:43 周一 下午
     * @author crowforkotlin
     */
    private fun Canvas.drawDifferenceEraseY(widthFloat: Float, heightFloat: Float, yRate: Float) {
        drawY(
            onTop = {
                clipRect(0f, 0f, widthFloat, heightFloat - yRate)
                debugAnimation { drawLine(0f, 0f, widthFloat, heightFloat - yRate,
                    mDebugYellowPaint
                ) }
            },
            onBottom = {
                clipRect(0f, yRate, widthFloat, heightFloat)
                debugAnimation { drawLine(0f, yRate, widthFloat, heightFloat,
                    mDebugBluePaint
                ) }
            }
        )
    }

    /**
     * ⦁ 绘制擦除X轴方向的动画
     *
     * ⦁ 2023-12-25 15:25:01 周一 下午
     * @author crowforkotlin
     */
    private fun Canvas.drawEraseX(widthFloat: Float, heightFloat: Float, xRate: Float) {
        drawX(
            onLeft = {
                clipRect(widthFloat - xRate, 0f, widthFloat, heightFloat)
                debugAnimation {
                    drawLine(widthFloat - xRate, 0f, widthFloat, heightFloat,
                        mDebugBluePaint
                    )
                }
            },
            onRight = {
                debugAnimation {
                    drawLine(0f, 0f, xRate, heightFloat, mDebugYellowPaint)
                }
                clipRect(0f, 0f, xRate, heightFloat)
            }
        )
    }

    /**
     * ⦁ 绘制同方向 反效果的擦除X轴方向的动画
     *
     * ⦁ 2023-12-25 15:25:48 周一 下午
     * @author crowforkotlin
     */
    private fun Canvas.drawDifferenceEraseX(widthFloat: Float, heightFloat: Float, xRate: Float) {
        drawX(
            onLeft = {
                clipRect(0f, 0f, widthFloat - xRate, heightFloat)
                debugAnimation {
                    drawLine(0f, 0f, widthFloat - xRate, heightFloat, mDebugYellowPaint)
                }
            },
            onRight = {
                clipRect(xRate, 0f, widthFloat, heightFloat)
                debugAnimation {
                    drawLine(xRate, 0f, widthFloat, heightFloat, mDebugBluePaint)
                }
            }
        )
    }
    private fun Context.px2dp(dp: Float): Float {
        return dp * resources.displayMetrics.density + 0.5f
    }
    private fun Context.px2sp(px: Float): Float {
        return px * resources.displayMetrics.density + 0.5f
    }
    private inline fun drawY(onTop: () -> Unit, onBottom: () -> Unit) {
        if (mTextAnimationTopEnable) onTop() else onBottom()
    }
    private inline fun drawX(onLeft: () -> Unit, onRight: () -> Unit) {
        if (mTextAnimationLeftEnable) onLeft() else onRight()
    }
    private inline fun debug(onDebug: () -> Unit) {
        if (DEBUG) onDebug()
    }
    private inline fun debugText(onDebug: () -> Unit, orElse: () -> Unit) {
        if (DEBUG_TEXT) onDebug() else orElse()
    }
    private inline fun debugAnimation(onDebug: () -> Unit) {
        if (DEBUG_ANIMATION) onDebug()
    }
    private inline fun withPath(path: Path, pathOperations: Path.() -> Unit) {
        path.reset()
        path.pathOperations()
        path.close()
    }
    private inline fun withApiO(leastO: () -> Unit, lessO: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) leastO() else lessO()
    }
    private fun Any?.error(tag: String = TAG) {
        debug { Log.e(tag, this.toString()) }
    }
    private fun Any?.info(tag: String = TAG) {
        debug { Log.i(tag, this.toString()) }
    }
    private fun Handler.sendMessage(runnable: Runnable, config: Message.() -> Unit) {
        sendMessage(Message.obtain(this, runnable).also { it.config() })
    }
    private fun Handler.sendMessageDelayed(runnable: Runnable, config: Message.() -> Unit, duration: Long) {
        sendMessageDelayed(Message.obtain(this, runnable).also { it.config() }, duration)
    }
    private inline fun Handler.asyncMessage(delay: Long, runnable: Runnable, config: Message.() -> Unit = { }) {
        sendMessageDelayed(Message.obtain(this, runnable).also {
            it.config()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                it.isAsynchronous = true
            }
        }, delay)
    }
    private fun Handler.asyncMessage(runnable: Runnable) {
        sendMessage(Message.obtain(this, runnable).also {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                it.isAsynchronous = true
            }
        })
    }

    /**
     * ⦁ 异步handler 这个不处理view的绘制，只进行一些延时任务，高刷不建议用这个 这个函数代码拷贝参考至kotlin协程库源码Dispatcher.Main内部的实现用到的一部分
     *
     * ⦁ 2024-03-20 15:54:02 周三 下午
     * @author crowforkotlin
     */
    @SuppressLint("ObsoleteSdkInt")
    private fun Looper.asHandler(async: Boolean): Handler {
        // Async support was added in API 16.
        if (!async || Build.VERSION.SDK_INT < 16) { return Handler(this) }

        if (Build.VERSION.SDK_INT >= 28) {
//         TODO compile against API 28 so this can be invoked without reflection.
//        val factoryMethod = Handler::class.java.getDeclaredMethod("createAsync", Looper::class.java)
//        return factoryMethod.invoke(null, this) as Handler
            return Handler.createAsync(this)
        }

        val constructor: Constructor<Handler>
        try {
            constructor = Handler::class.java.getDeclaredConstructor(Looper::class.java,
                Handler.Callback::class.java, Boolean::class.javaPrimitiveType)
        } catch (ignored: NoSuchMethodException) {
            // Hidden constructor absent. Fall back to non-async constructor.
            return Handler(this)
        }
        return constructor.newInstance(this, null, true)
    }

    /**
     * ⦁ 确保在主线程内更新数值，确保正在绘制的内容可以拿到最新的数值
     *
     * ⦁ 2024-07-12 17:55:46 周五 下午
     * @author crowforkotlin
     */
    private inline fun updateValue(crossinline block: () -> Unit) {
        val handler = mHandler ?: return run { block() }
        if (handler.looper.thread.id != Thread.currentThread().id)
            mHandler?.post { block() }
        else {
            block()
        }
    }

    fun update() {
        initTextPaint()
        onVariableChanged(FLAG_TEXT)
    }

    /**
     * ⦁ 直接设置mText和调用updateTextNow的区别在于，每次设置后是否会调用延迟启动动画
     * 因为有个停留时间，文本的绘制都是在动画开始的时候执行，以及初始化的时候仅绘制一次用于立马显示文本
     *
     * ⦁ 2024-07-12 18:30:35 周五 下午
     * @author crowforkotlin
     */
    fun updateTextNow(text: String?) {
        mUpdateTextQuickNow = true
        mText = text
    }

    // 一定要增加，用于即使更新文本内容，如果不调用那么文本的一些效果将不会生效！可以自己改成设置属性直接更新文本，这里暂时不这样处理
    fun updateTextPaint() { initTextPaint() }

    // 可以实现动画过程中直接暂停
    fun pauseAnimation() {
        mHandler?.removeMessages(MESSAGE_TEXT_RESIDENCE_TIME)
        mHandler?.removeMessages(MESSAGE_LAUNCH_DEFAULT_ANIMATION)
        runCatching { mChoreographer.removeFrameCallback(this) }
        mTextAnimationIsRunning = false
    }

    // 也可直接会反复动画的状态继续执行
    fun resumeAnimation() {
        if (mTextAnimationMode != ANIMATION_DEFAULT) {
            mTextAnimationIsRunning = true
            invalidateHighBrushViewAnimation(
                animationDuration = if (mTextAnimationMode == ANIMATION_MOVE_X_HIGH_BRUSH_NO_PAGING_DRAW) 0L else mTextAnimationDuration,
                isTopOrLeft = mTextAnimationIsTopOrLeft,
                highBrushPixelCount = mTextAnimationPixelCount
            )
        } else {
            launchDefaultAnimation()
        }
    }

    // 直接取消动画
    fun cancelAnimation() {
        val isRunning = mTextAnimationIsRunning
        mTextAnimationIsRunning = false
        mTextAxisValue = 0f
        mHandler?.let { handler ->
            handler.removeMessages(MESSAGE_TEXT_RESIDENCE_TIME)
            handler.removeMessages(MESSAGE_LAUNCH_DEFAULT_ANIMATION)
            handler.removeMessages(MESSAGE_LAUNCH_ANIMATION)
        }
        // 当动画不在执行，此时取消动画后要重置位置需要手动调用invalidate触发绘制
        if (isRunning) {
            runCatching { mChoreographer.removeFrameCallback(this) }
            invalidate()
        }
    }

    companion object {

        // 调式模式，用于打印日志或其他处理
        private const val DEBUG = true

        // 调试模式，用于文本绘制
        private const val DEBUG_TEXT = false

        // 调试模式，用于动画
        private const val DEBUG_ANIMATION = false

        private const val TAG = "AttrTextView"
        private const val DEBUG_STROKE_WIDTH = 8f
        private val mDebugYellowPaint by lazy { Paint().also { it.strokeWidth = DEBUG_STROKE_WIDTH; it.color = Color.YELLOW } }
        private val mDebugBluePaint by lazy { Paint().also { it.strokeWidth = DEBUG_STROKE_WIDTH; it.color = Color.BLUE } }

        /**
         * ⦁ 动画默认最小的停留时间
         *
         * ⦁ 2024-07-12 18:35:02 周五 下午
         * @author crowforkotlin
         */
        private const val ANIMATION_DEFAULT_MIN_TEXT_RESIDENCE_TIME = 500

        /**
         * ⦁ TaskScope 单例 暂时预留 考虑到文本数据处理采用单一线程解析，最后交由View进行对于处理
         *
         * ⦁ 2023-12-28 15:24:09 周四 下午
         * @author crowforkotlin
         */
        private var mTaskHandlerThread: HandlerThread? = null
        private lateinit var mTaskHandler: Handler

        /**
         * ⦁ 文本有效行默认为小于3，1奇、2偶 为3则 另外手动处理，直接给文本高度设置0 详情见 drawCenterText 函数
         *
         * ⦁ 2023-12-25 17:29:59 周一 下午
         * @author crowforkotlin
         */
        private const val TEXT_HEIGHT_VALID_ROW: Int = 3

        /**
         * ⦁ 用于解决文本Y轴的精准度 减少由浮点数带来的微小误差，在像素级视图中 效果十分明显
         *
         * ⦁ 2023-12-25 17:27:58 周一 下午
         * @author crowforkotlin
         */
        private const val ROW_DEVIATION: Float = 0.5f
        private const val NEWLINE_CHAR_FLAG = '\n'

        /**
         * ⦁ 文本位置方向相关
         *
         * ⦁ 2024-07-12 18:45:32 周五 下午
         * @author crowforkotlin
         */
        const val GRAVITY_TOP_START: Byte = 1
        const val GRAVITY_TOP_CENTER: Byte = 2
        const val GRAVITY_TOP_END: Byte = 3
        const val GRAVITY_CENTER_START:Byte = 4
        const val GRAVITY_CENTER: Byte = 5
        const val GRAVITY_CENTER_END: Byte = 6
        const val GRAVITY_BOTTOM_START: Byte = 7
        const val GRAVITY_BOTTOM_CENTER: Byte = 8
        const val GRAVITY_BOTTOM_END: Byte = 9

        /**
         * ⦁ 渐变色相关
         *
         * ⦁ 2024-07-12 18:44:45 周五 下午
         * @author crowforkotlin
         */
        // 渐变 - 斜面
        const val GRADIENT_BEVEL: Byte = 10

        // 渐变 - 水平
        const val GRADIENT_HORIZONTAL: Byte = 11

        // 渐变 - 垂直
        const val GRADIENT_VERTICAL: Byte = 12

        // 设置文本的标，更新文本后传入这个标志会触发文本重新计算、动画重新启动或不启动、绘制文本
        private const val FLAG_TEXT: Byte = 30

        /**
         * ⦁ handler任务消息相关的
         *
         * ⦁ 2024-07-12 18:42:57 周五 下午
         * @author crowforkotlin
         */
        // 停留时间任务
        private const val MESSAGE_TEXT_RESIDENCE_TIME = 200

        // 启动动画任务
        private const val MESSAGE_LAUNCH_ANIMATION = 201

        // 启动默认的动画的任务
        private const val MESSAGE_LAUNCH_DEFAULT_ANIMATION = 202


        /**
         * ⦁ 动画模式相关
         *
         * ⦁ 2024-07-12 18:39:53 周五 下午
         * @author crowforkotlin
         */
        // 默认的动画就是静止显示（翻页效果）
        const val ANIMATION_DEFAULT: Short = 300

        // 高刷动画 x轴移动 - 绘制（翻页效果）
        const val ANIMATION_MOVE_X_HIGH_BRUSH_DRAW: Short = 318

        // 高刷动画y轴移动你 - 绘制 （翻页效果）
        const val ANIMATION_MOVE_Y_HIGH_BRUSH_DRAW: Short = 319

        // 高刷动画x轴连续移动 - 绘制 （无翻页效果）
        const val ANIMATION_MOVE_X_HIGH_BRUSH_NO_PAGING_DRAW: Short = 320

        /**
         * ⦁  策略相关
         *
         * ⦁ 2024-07-12 18:36:20 周五 下午
         * @author crowforkotlin
         */
        // 动画策略 ： 重置，代表更新文本时动画重新开始
        const val STRATEGY_ANIMATION_UPDATE_RESET: Short = 602

        // 动画策略：连续，代表更新文本时动画继续执行不打断动画
        const val STRATEGY_ANIMATION_UPDATE_CONTINUA: Short = 603

        // 文本更新策略：全部-连续索引，这个代表文本更新的时候会记住当前文本的索引，在更新后自动判断如果最新的文本列表大小不超过当前文本索引，当前的文本将继承上个文本的索引位置
        const val STRATEGY_TEXT_UPDATE_ALL_CONTINUE: Short = 900

        // 文本更新策略：全部-重置，这个代表文本更新的时候索引全都从0开始
        const val STRATEGY_TEXT_UPDATE_ALL_RESET: Short = 905
    }
}