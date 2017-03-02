package com.twilio.chat.demo;

import java.util.Date;

import com.twilio.chat.Channel;
import com.twilio.chat.Channel.ChannelStatus;
import com.twilio.chat.Channel.ChannelType;
import com.twilio.chat.ChannelDescriptor;
import com.twilio.chat.CallbackListener;
import com.twilio.chat.StatusListener;
import com.twilio.chat.ErrorInfo;

public class ChannelModel
{
    private Channel channel;
    private ChannelDescriptor channelDescriptor;

    public ChannelModel(Channel channel_) { channel = channel_; }
    public ChannelModel(ChannelDescriptor channel_) { channelDescriptor = channel_; }

    public String getFriendlyName()
    {
        if (channel != null) return channel.getFriendlyName();
        if (channelDescriptor != null) return channelDescriptor.getFriendlyName();
        return null;
    }

    public String getSid()
    {
        if (channel != null) return channel.getSid();
        if (channelDescriptor != null) return channelDescriptor.getSid();
        return null;
    }

    public Date getDateUpdatedAsDate()
    {
        if (channel != null) return channel.getDateUpdatedAsDate();
        if (channelDescriptor != null) return channelDescriptor.getDateUpdated();
        return null;
    }

    public Date getDateCreatedAsDate()
    {
        if (channel != null) return channel.getDateCreatedAsDate();
        if (channelDescriptor != null) return channelDescriptor.getDateCreated();
        return null;
    }

    public ChannelStatus getStatus()
    {
        if (channel != null) return channel.getStatus();
        if (channelDescriptor != null) return channelDescriptor.getStatus();
        return null;
    }

    public void getUnconsumedMessagesCount(CallbackListener<Long> listener)
    {
        if (channel != null) { channel.getUnconsumedMessagesCount(listener); return; }
        if (channelDescriptor != null) { listener.onSuccess(channelDescriptor.getUnconsumedMessagesCount()); return; }
        listener.onError(new ErrorInfo(-10001, "No channel in model"));
    }

    public void getMessagesCount(CallbackListener<Long> listener)
    {
        if (channel != null) { channel.getMessagesCount(listener); return; }
        if (channelDescriptor != null) { listener.onSuccess(channelDescriptor.getMessagesCount()); return; }
        listener.onError(new ErrorInfo(-10002, "No channel in model"));
    }

    public void getMembersCount(CallbackListener<Long> listener)
    {
        if (channel != null) { channel.getMembersCount(listener); return; }
        if (channelDescriptor != null) { listener.onSuccess(channelDescriptor.getMembersCount()); return; }
        listener.onError(new ErrorInfo(-10003, "No channel in model"));
    }

    public void join(final StatusListener listener) {
        if (channel != null) { channel.join(listener); return; }
        if (channelDescriptor != null) {
            channelDescriptor.getChannel(new CallbackListener<Channel>() {
                @Override
                public void onSuccess(Channel chan) {
                    chan.join(listener);
                }
                @Override
                public void onError(ErrorInfo err) {
                    listener.onError(err);
                }
            });
            return;
        }
        listener.onError(new ErrorInfo(-10004, "No channel in model"));
    }

    public void getChannel(CallbackListener<Channel> listener) {
        if (channel != null) { listener.onSuccess(channel); return; }
        if (channelDescriptor != null) { channelDescriptor.getChannel(listener); return; }
        listener.onError(new ErrorInfo(-10005, "No channel in model"));
    }

    public Channel.ChannelType getType() {
        if (channel != null) return channel.getType();
        if (channelDescriptor != null) return ChannelType.PUBLIC;
        throw new IllegalStateException("Invalid state");
    }
}
