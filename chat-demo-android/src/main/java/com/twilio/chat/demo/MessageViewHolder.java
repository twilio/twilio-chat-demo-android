package com.twilio.chat.demo;

import java.util.Map;
import org.json.JSONObject;

import com.twilio.chat.Member;
import com.twilio.chat.Message;
import com.twilio.chat.demo.R;

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
        Color.GRAY, Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA
    };

    @ViewId(R.id.avatar)
    ImageView imageView;

    @ViewId(R.id.reachability)
    ImageView reachabilityView;

    @ViewId(R.id.body)
    TextView body;

    @ViewId(R.id.author)
    TextView author;

    @ViewId(R.id.date)
    TextView date;

    @ViewId(R.id.consumptionHorizonIdentities)
    RelativeLayout identities;

    @ViewId(R.id.consumptionHorizonLines)
    LinearLayout lines;

    View view;

    public MessageViewHolder(View view)
    {
        super(view);
        this.view = view;
    }

    @Override
    public void onSetListeners()
    {
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v)
            {
                OnMessageClickListener listener = getListener(OnMessageClickListener.class);
                if (listener != null) {
                    listener.onMessageClicked(getItem());
                    return true;
                }
                return false;
            }
        });

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                date.setVisibility(date.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onSetValues(MessageActivity.MessageItem message, PositionInfo pos)
    {
        if (message != null) {
            Message msg = message.getMessage();

            author.setText(msg.getAuthor());
            body.setText(msg.getMessageBody());
            date.setText(msg.getTimeStamp());

            identities.removeAllViews();
            lines.removeAllViews();

            if (message.getMembers() != null && message.getMembers().getMembers() != null) {
                for (Member member : message.getMembers().getMembers()) {
                    if (msg.getAuthor().equals(member.getUserInfo().getIdentity())) {
                        fillUserAvatar(imageView, member);
                        fillUserReachability(reachabilityView, member);
                    }

                    if (member.getLastConsumedMessageIndex() != null
                        && member.getLastConsumedMessageIndex()
                               == message.getMessage().getMessageIndex()) {
                        drawConsumptionHorizon(member);
                    }
                }
            }
        }
    }

    private void drawConsumptionHorizon(Member member)
    {
        String ident = member.getUserInfo().getIdentity();
        int color = getMemberRgb(ident);

        TextView identity = new TextView(getContext());
        identity.setText(ident);
        identity.setTextSize(8);
        identity.setTextColor(color);

        // Layout
        final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        int cc = identities.getChildCount();
        if (cc > 0) {
            params.addRule(RelativeLayout.RIGHT_OF, identities.getChildAt(cc - 1).getId());
        }
        identity.setLayoutParams(params);

        View line = new View(getContext());
        line.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 5));
        line.setBackgroundColor(color);

        identities.addView(identity);
        lines.addView(line);
    }

    private void fillUserAvatar(ImageView avatarView, Member member)
    {
        JSONObject attributes = member.getUserInfo().getAttributes();
        String     avatar = (String)attributes.opt("avatar");
        if (avatar != null) {
            byte[] data = Base64.decode(avatar, Base64.NO_WRAP);
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            avatarView.setImageBitmap(bitmap);
        }
        else {
            avatarView.setImageResource(R.drawable.avatar2);
        }
    }

    private void fillUserReachability(ImageView reachabilityView, Member member) {
        if (!TwilioApplication.get().getBasicClient().getChatClient().isReachabilityEnabled()) {
            reachabilityView.setImageResource(R.drawable.reachability_disabled);
        } else if (member.getUserInfo().isOnline()) {
            reachabilityView.setImageResource(R.drawable.reachability_online);
        } else if (member.getUserInfo().isNotifiable()) {
            reachabilityView.setImageResource(R.drawable.reachability_notifiable);
        } else {
            reachabilityView.setImageResource(R.drawable.reachability_offline);
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
