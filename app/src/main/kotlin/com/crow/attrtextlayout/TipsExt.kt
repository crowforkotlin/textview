package com.crow.attrtextlayout

import android.util.Log

const val TIPS_TAG = "IAttrTextExt-Crow"

fun Any?.log(level: Int = Log.INFO, tag: String = TIPS_TAG) { Log.println(level, tag, this.toString()) }