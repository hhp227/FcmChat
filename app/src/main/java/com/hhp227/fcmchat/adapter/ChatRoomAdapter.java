package com.hhp227.fcmchat.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.hhp227.fcmchat.R;
import com.hhp227.fcmchat.dto.ChatRoom;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ChatRoomAdapter extends RecyclerView.Adapter {
    private static String today;
    private Activity mActivity;
    private List<ChatRoom> mChatRoomList;
    private OnItemClickListener mOnItemClickListener;

    public ChatRoomAdapter(Activity activity, List<ChatRoom> chatRoomList) {
        this.mActivity = activity;
        this.mChatRoomList = chatRoomList;
        Calendar calendar = Calendar.getInstance();
        today = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mActivity).inflate(R.layout.chat_room_list_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        ChatRoom chatRoom = mChatRoomList.get(position);

        ((ViewHolder) holder).itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOnItemClickListener != null)
                    mOnItemClickListener.onItemClick(view, position);
            }
        });
        ((ViewHolder) holder).name.setText(chatRoom.getName());
        ((ViewHolder) holder).message.setText(chatRoom.getLastMessage());
        if (chatRoom.getUnreadCount() > 0) {
            ((ViewHolder) holder).count.setText(String.valueOf(chatRoom.getUnreadCount()));
            ((ViewHolder) holder).count.setVisibility(View.VISIBLE);
        } else
            ((ViewHolder) holder).count.setVisibility(View.GONE);
        ((ViewHolder) holder).timestamp.setText(getTimeStamp(chatRoom.getTimestamp()));
    }

    @Override
    public int getItemCount() {
        return mChatRoomList.size();
    }

    public void setOnItemClickListener(OnItemClickListener OnItemClickListener) {
        this.mOnItemClickListener = OnItemClickListener;
    }

    public static String getTimeStamp(String dateStr) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = "";

        today = today.length() < 2 ? "0" + today : today;

        try {
            Date date = format.parse(dateStr);
            SimpleDateFormat todayFormat = new SimpleDateFormat("dd");
            String dateToday = todayFormat.format(date);
            format = dateToday.equals(today) ? new SimpleDateFormat("hh:mm a") : new SimpleDateFormat("dd LLL, hh:mm a");
            String date1 = format.format(date);
            timestamp = date1;
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return timestamp;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView name, message, timestamp, count;

        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            message = itemView.findViewById(R.id.message);
            timestamp = itemView.findViewById(R.id.timestamp);
            count = itemView.findViewById(R.id.count);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View v, int position);
    }
}