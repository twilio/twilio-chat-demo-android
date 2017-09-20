package com.twilio.chat.demo.views

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.TextView

class MessageTextView(context: Context, attrs: AttributeSet) : TextView(context, attrs) {

    init {
        var font: String? = attrs.getAttributeValue(androidNS, "fontFamily")
        if (font == null) {
            font = "AlegreyaSans-Light"
        }
        this.typeface = Typeface.createFromAsset(context.assets, "fonts/$font.ttf")
    }

    companion object {
        private val androidNS = "http://schemas.android.com/apk/res/android"
    }
}
