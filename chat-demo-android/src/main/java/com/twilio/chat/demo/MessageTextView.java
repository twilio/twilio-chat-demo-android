package com.twilio.ipmessaging.demo;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class MessageTextView extends TextView
{
    private static final String androidNS = "http://schemas.android.com/apk/res/android";

    public MessageTextView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        String font = attrs.getAttributeValue(androidNS, "fontFamily");
        if (font == null) {
            font = "AlegreyaSans-Light";
        }
        this.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/" + font + ".ttf"));
    }
}
