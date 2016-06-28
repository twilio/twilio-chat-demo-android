package com.twilio.ipmessaging.demo;

import java.util.Map;
import org.json.JSONObject;

import com.twilio.ipmessaging.Member;
import com.twilio.ipmessaging.Message;
import com.twilio.ipmessaging.demo.R;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import uk.co.ribot.easyadapter.ItemViewHolder;
import uk.co.ribot.easyadapter.PositionInfo;
import uk.co.ribot.easyadapter.annotations.LayoutId;
import uk.co.ribot.easyadapter.annotations.ViewId;

@LayoutId(R.layout.message_item_layout)
public class MessageViewHolder extends ItemViewHolder<MessageActivity.MessageItem>
{
    private static int[] HORIZON_COLORS = {
        Color.RED, Color.BLUE, Color.CYAN, Color.GREEN, Color.MAGENTA
    };

    @ViewId(R.id.body)
    TextView body;

    @ViewId(R.id.txtInfo)
    TextView txtInfo;

    @ViewId(R.id.singleMessageContainer)
    LinearLayout singleMessageContainer;

    @ViewId(R.id.consumptionHorizon)
    LinearLayout consumptionHorizon;

    @ViewId(R.id.avatar_message_left)
    ImageView imageViewLeft;

    @ViewId(R.id.avatar_message_right)
    ImageView imageViewRight;

    View view;

    public MessageViewHolder(View view)
    {
        super(view);
        this.view = view;
    }

    @Override
    public void onSetListeners()
    {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                OnMessageClickListener listener = getListener(OnMessageClickListener.class);
                if (listener != null) {
                    listener.onMessageClicked(getItem());
                }
            }
        });
    }

    @Override
    public void onSetValues(MessageActivity.MessageItem message, PositionInfo pos)
    {
        StringBuffer textInfo = new StringBuffer();
        if (message != null) {
            textInfo.append(message.getMessage().getAuthor());
            String dateString = message.getMessage().getTimeStamp();
            if (dateString != null) {
                textInfo.append(":").append(dateString);
            }
            txtInfo.setText(textInfo.toString());
            body.setText(message.getMessage().getMessageBody());

            boolean left =
                (message.getMessage().getAuthor().equals(message.getCurrentUser())) ? true : false;
            body.setBackgroundResource(left ? R.drawable.bubble_b : R.drawable.bubble_a);
            singleMessageContainer.setGravity(left ? Gravity.LEFT : Gravity.RIGHT);
            txtInfo.setGravity(left ? Gravity.LEFT : Gravity.RIGHT);
            consumptionHorizon.removeAllViews();
            imageViewLeft.setVisibility(View.GONE);
            imageViewRight.setVisibility(View.GONE);

            if (message.getMembers() != null && message.getMembers().getMembers() != null) {
                for (Member member : message.getMembers().getMembers()) {
                    if (message.getMessage().getAuthor().equals(
                            member.getUserInfo().getIdentity())) {
                        if (left)
                            fillUserAvatar(imageViewLeft, member);
                        else
                            fillUserAvatar(imageViewRight, member);
                    }
                    if (member.getLastConsumedMessageIndex() != null
                        && member.getLastConsumedMessageIndex()
                               == message.getMessage().getMessageIndex()) {
                        drawConsumptionHorizon(left, member);
                    }
                }
            }
        }
    }

    private void drawConsumptionHorizon(boolean left, Member member)
    {
        LinearLayout seenByView = new LinearLayout(getContext());
        seenByView.setOrientation(LinearLayout.VERTICAL);
        TextView identity = new TextView(getContext());
        identity.setGravity(left ? Gravity.LEFT : Gravity.RIGHT);
        identity.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        identity.setText(member.getUserInfo().getIdentity());
        View line = new View(getContext());
        line.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 5));
        line.setBackgroundColor(getMemberRgb(member.getUserInfo().getIdentity()));
        identity.setTextColor(getMemberRgb(member.getUserInfo().getIdentity()));
        identity.setTextSize(8);
        seenByView.addView(identity);
        seenByView.addView(line);
        consumptionHorizon.addView(seenByView);
    }

    private void fillUserAvatar(ImageView avatarView, Member member)
    {
        JSONObject attributes = member.getUserInfo().getAttributes();
        String     avatar = (String)attributes.opt("avatar");
        avatarView.setVisibility(View.VISIBLE);
        avatarView.setImageBitmap(null);
        if (avatar != null) {
            byte[] data = Base64.decode(avatar, Base64.NO_WRAP);
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            avatarView.setImageBitmap(bitmap);
        }
    }

    public interface OnMessageClickListener {
        void onMessageClicked(MessageActivity.MessageItem message);
    }

    public int getMemberRgb(String identity)
    {
        return HORIZON_COLORS[Math.abs(identity.hashCode()) % HORIZON_COLORS.length];
    }
}
