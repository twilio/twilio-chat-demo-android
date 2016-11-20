#==============================================
# Proguard rules for use with IP Messaging SDK
#==============================================

-keep class com.twilio.ipmessaging.** { *; }
-keep class com.twilio.common.** { *; }
-keepattributes InnerClasses
#-keep interface com.twilio.ipmessaging.** { *; }
#-keep enum com.twilio.ipmessaging.** { *; }

## Keep native methods

-keepclasseswithmembernames class com.twilio.ipmessaging.** {
    native <methods>;
}

## Keep callbacks from native
# ?

#======================================
# Local demo application configuration
#======================================

-keepclassmembers class **.R$* {
    public static <fields>;
}

## EasyAdapter

-dontwarn uk.co.ribot.easyadapter.**
-keepattributes *Annotation*
-keepclassmembers class * extends uk.co.ribot.easyadapter.ItemViewHolder {
    public <init>(...);
}

## Google libraries

-dontwarn android.support.**
-keep class com.google.ads.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.firebase.** { *; }
-keepattributes Signature
