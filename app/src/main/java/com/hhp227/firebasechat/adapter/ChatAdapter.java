package com.hhp227.firebasechat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.hhp227.firebasechat.R;
import com.hhp227.firebasechat.dto.ChatItem;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ChatAdapter extends ArrayAdapter<ChatItem> {
    private final SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("a h:mm", Locale.getDefault());

    public ChatAdapter(Context context, int resource) {
        super(context, resource);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.chat_item, null);

            viewHolder = new ViewHolder();
            viewHolder.userName = convertView.findViewById(R.id.tv_name);
            viewHolder.message = convertView.findViewById(R.id.tv_message);
            viewHolder.time = convertView.findViewById(R.id.tv_time);

            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();

        ChatItem chatData = getItem(position);
        viewHolder.userName.setText(chatData.userName);
        viewHolder.message.setText(chatData.message);
        viewHolder.time.setText(mSimpleDateFormat.format(chatData.time));

        return convertView;
    }

    private class ViewHolder {
        private TextView userName;
        private TextView message;
        private TextView time;
    }
}
