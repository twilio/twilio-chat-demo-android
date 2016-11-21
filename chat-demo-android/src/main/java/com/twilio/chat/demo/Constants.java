package com.twilio.chat.demo;

public interface Constants {
    /** Key into an Intent's extras data that points to a {@link Channel} object. */
    public static final String EXTRA_CHANNEL = "com.twilio.chat.Channel";
    /** Key into an Intent's extras data that contains Channel SID. */
    public static final String EXTRA_CHANNEL_SID = "C_SID";
    /** Key into an Intent's extras data that indicates an action is requested from the user. */
    public static final String EXTRA_ACTION = "channel.action";
    /** Key into an Intent's extras data that indicates an action type. */
    public static final String EXTRA_ACTION_INVITE = "channel.action.invite";
}
