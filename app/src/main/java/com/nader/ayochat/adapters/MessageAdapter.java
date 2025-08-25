package com.nader.ayochat.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.nader.ayochat.R;
import com.nader.ayochat.models.Message;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Message> messages;
    private Context context;
    private OnMessageLongClickListener longClickListener;
    private SimpleDateFormat dateFormat;

    public interface OnMessageLongClickListener {
        void onMessageLongClick(Message message, int position);
    }

    public MessageAdapter(Context context) {
        this.context = context;
        this.messages = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public void addMessage(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void removeMessage(int messageId) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getId() == messageId) {
                messages.get(i).setDeleted(true);
                messages.get(i).setMessage("[Message deleted]");
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);

        // Set username with flag
        String displayName = message.getCountryFlag() + " " + message.getUsername();
        holder.tvUsername.setText(displayName);

        // Set message text
        holder.tvMessage.setText(message.getMessage());

        // Set timestamp
        String timeAgo = getTimeAgo(message.getCreatedAt());
        holder.tvTime.setText(timeAgo);

        // Style based on message type
        if (message.isSystemMessage()) {
            // System message styling
            holder.cardView.setCardBackgroundColor(Color.parseColor("#E0E0E0"));
            holder.tvUsername.setVisibility(View.GONE);
            holder.tvMessage.setTextColor(Color.parseColor("#666666"));
            holder.tvMessage.setTypeface(null, Typeface.ITALIC);
            holder.tvMessage.setGravity(Gravity.CENTER);
            holder.messageContainer.setGravity(Gravity.CENTER);
        } else if (message.isOwnMessage()) {
            // Own message styling
            holder.cardView.setCardBackgroundColor(Color.parseColor("#DCF8C6"));
            holder.tvUsername.setVisibility(View.VISIBLE);
            holder.tvMessage.setTextColor(Color.BLACK);
            holder.tvMessage.setTypeface(null, Typeface.NORMAL);
            holder.tvMessage.setGravity(Gravity.START);
            holder.messageContainer.setGravity(Gravity.END);
        } else {
            // Others' message styling
            holder.cardView.setCardBackgroundColor(Color.WHITE);
            holder.tvUsername.setVisibility(View.VISIBLE);
            holder.tvMessage.setTextColor(Color.BLACK);
            holder.tvMessage.setTypeface(null, Typeface.NORMAL);
            holder.tvMessage.setGravity(Gravity.START);
            holder.messageContainer.setGravity(Gravity.START);
        }

        // Handle deleted messages
        if (message.isDeleted()) {
            holder.tvMessage.setText("[Message deleted]");
            holder.tvMessage.setTextColor(Color.GRAY);
            holder.tvMessage.setTypeface(null, Typeface.ITALIC);
        }

        // Long click listener for own messages
        if (message.isOwnMessage() && !message.isDeleted() && longClickListener != null) {
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    longClickListener.onMessageLongClick(message, holder.getAdapterPosition());
                    return true;
                }
            });
        } else {
            holder.itemView.setOnLongClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private String getTimeAgo(String timestamp) {
        if (timestamp == null) return "";

        try {
            Date date = dateFormat.parse(timestamp.replace(".000Z", ""));
            if (date != null) {
                long time = date.getTime();
                long now = System.currentTimeMillis();

                CharSequence ago = DateUtils.getRelativeTimeSpanString(
                        time, now, DateUtils.MINUTE_IN_MILLIS
                );

                return ago.toString();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return timestamp;
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvUsername;
        TextView tvMessage;
        TextView tvTime;
        LinearLayout messageContainer;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            messageContainer = itemView.findViewById(R.id.messageContainer);
        }
    }
}