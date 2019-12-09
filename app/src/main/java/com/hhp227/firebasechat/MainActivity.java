package com.hhp227.firebasechat;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.google.firebase.database.*;
import com.hhp227.firebasechat.adapter.ChatAdapter;
import com.hhp227.firebasechat.dto.ChatItem;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements ChildEventListener, View.OnClickListener, TextWatcher {
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    private ChatAdapter mAdapter;
    private ListView mListView;
    private EditText mEdtMessage;
    private TextView buttonSend;
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initFirebaseDatabase();
        initValues();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDatabaseReference.removeEventListener(this);
    }

    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
        ChatItem chatData = dataSnapshot.getValue(ChatItem.class);
        chatData.fireBaseKey = dataSnapshot.getKey();
        mAdapter.add(chatData);
        mListView.smoothScrollToPosition(mAdapter.getCount());
    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot) {
        String firebaseKey = dataSnapshot.getKey();
        int count = mAdapter.getCount();
        for (int i = 0; i < count; i++) {
            if (mAdapter.getItem(i).fireBaseKey.equals(firebaseKey)) {
                mAdapter.remove(mAdapter.getItem(i));
                break;
            }
        }
    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
    }

    @Override
    public void onClick(View v) {
        String message = mEdtMessage.getText().toString();
        if (!TextUtils.isEmpty(message)) {
            mEdtMessage.setText("");
            ChatItem chatData = new ChatItem();
            chatData.userName = userName;
            chatData.message = message;
            chatData.time = System.currentTimeMillis();
            mDatabaseReference.push().setValue(chatData);
        } else
            Toast.makeText(getApplicationContext(), "내용을 입력하세요.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        buttonSend.setBackgroundResource(s.length() > 0 ? R.drawable.background_sendbtn_p : R.drawable.background_sendbtn_n);
        buttonSend.setTextColor(getResources().getColor(s.length() > 0 ? android.R.color.white : android.R.color.darker_gray));
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    private void initViews() {
        mListView = findViewById(R.id.lv_message);
        buttonSend = findViewById(R.id.btn_send);
        mEdtMessage = findViewById(R.id.et_message);
        mAdapter = new ChatAdapter(this, R.layout.chat_item);
        mListView.setAdapter(mAdapter);
        buttonSend.setOnClickListener(this);
        buttonSend.addTextChangedListener(this);
    }

    private void initFirebaseDatabase() {
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mDatabaseReference = mFirebaseDatabase.getReference("message");
        mDatabaseReference.addChildEventListener(this);
    }

    private void initValues() {
        userName = "Guest" + new Random().nextInt(5000);
    }
}
