package com.hhp227.fcmchat.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.*;
import com.android.volley.toolbox.StringRequest;
import com.hhp227.fcmchat.R;
import com.hhp227.fcmchat.adapter.MessageListAdapter;
import com.hhp227.fcmchat.app.AppController;
import com.hhp227.fcmchat.app.Config;
import com.hhp227.fcmchat.app.EndPoints;
import com.hhp227.fcmchat.dto.Message;
import com.hhp227.fcmchat.dto.User;
import com.hhp227.fcmchat.fcm.NotificationUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = ChatActivity.class.getSimpleName();
    private String mChatRoomId;
    private BroadcastReceiver mBroadcastReceiver;
    private CardView mButtonSend;
    private EditText mInputMessage;
    private MessageListAdapter mAdapter;
    private List<Message> mMessageList;
    private RecyclerView mRecyclerView;
    private TextView mSendText;
    private TextWatcher mTextWatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toolbar toolbar = findViewById(R.id.toolbar);
        Intent intent = getIntent();
        mChatRoomId = intent.getStringExtra("chat_room_id");
        String title = intent.getStringExtra("name");
        String selfUserId = AppController.getInstance().getPrefManager().getUser().getId();
        mButtonSend = findViewById(R.id.cv_send);
        mRecyclerView = findViewById(R.id.recycler_view);
        mInputMessage = findViewById(R.id.et_message);
        mSendText = findViewById(R.id.tv_send);
        mMessageList = new ArrayList<>();
        mAdapter = new MessageListAdapter(this, mMessageList, selfUserId);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Config.PUSH_NOTIFICATION)) {
                    Message message = (Message) intent.getSerializableExtra("message");
                    String chatRoomId = intent.getStringExtra("chat_room_id");

                    if (message != null && chatRoomId != null) {
                        mMessageList.add(message);
                        mAdapter.notifyDataSetChanged();
                        if (mAdapter.getItemCount() > 1) {
                            mRecyclerView.getLayoutManager().smoothScrollToPosition(mRecyclerView, null, mAdapter.getItemCount() - 1);
                        }
                    }
                }
            }
        };
        mTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mButtonSend.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), s.length() > 0 ? R.color.colorAccent : androidx.cardview.R.color.cardview_light_background));
                mSendText.setTextColor(ContextCompat.getColor(getApplicationContext(), s.length() > 0 ? android.R.color.white : android.R.color.darker_gray));
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (mChatRoomId == null) {
            Toast.makeText(getApplicationContext(), "채팅방을 찾을수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);
        mButtonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String message = mInputMessage.getText().toString().trim();
                if (!TextUtils.isEmpty(message)) {
                    StringRequest stringRequest = new StringRequest(Request.Method.POST, EndPoints.CHAT_ROOM_MESSAGE.replace("_ID_", mChatRoomId), new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject obj = new JSONObject(response);

                                if (!obj.getBoolean("error")) {
                                    JSONObject commentObj = obj.getJSONObject("message");
                                    String commentId = commentObj.getString("message_id");
                                    String commentText = commentObj.getString("message");
                                    String createdAt = commentObj.getString("created_at");
                                    JSONObject userObj = obj.getJSONObject("user");
                                    String userId = userObj.getString("user_id");
                                    String userName = userObj.getString("name");
                                    User user = new User(userId, userName, null);
                                    Message message = new Message();

                                    message.setId(commentId);
                                    message.setMessage(commentText);
                                    message.setCreatedAt(createdAt);
                                    message.setUser(user);
                                    mMessageList.add(message);
                                    mAdapter.notifyDataSetChanged();
                                    if (mAdapter.getItemCount() > 1)
                                        mRecyclerView.getLayoutManager().smoothScrollToPosition(mRecyclerView, null, mAdapter.getItemCount() - 1);
                                } else
                                    Toast.makeText(getApplicationContext(), "" + obj.getString("message"), Toast.LENGTH_LONG).show();
                            } catch (JSONException e) {
                                Log.e(TAG, "json 파싱 에러: " + e.getMessage());
                                Toast.makeText(getApplicationContext(), "json 파싱 에러: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            NetworkResponse networkResponse = error.networkResponse;

                            Log.e(TAG, "Volley 에러: " + error.getMessage() + ", 코드: " + networkResponse);
                            Toast.makeText(getApplicationContext(), "Volley 에러: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            mInputMessage.setText(message);
                        }
                    }) {
                        @Override
                        protected Map<String, String> getParams() {
                            Map<String, String> params = new HashMap<>();
                            params.put("user_id", AppController.getInstance().getPrefManager().getUser().getId());
                            params.put("message", message);

                            return params;
                        }
                    };
                    int socketTimeout = 0;
                    RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

                    stringRequest.setRetryPolicy(policy);
                    AppController.getInstance().addToRequestQueue(stringRequest);
                    mInputMessage.setText("");
                } else
                    Toast.makeText(getApplicationContext(), "메시지를 입력하세요.", Toast.LENGTH_SHORT).show();
            }
        });
        mInputMessage.addTextChangedListener(mTextWatcher);
        fetchMessageList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, new IntentFilter(Config.PUSH_NOTIFICATION));

        NotificationUtils.clearNotifications();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mInputMessage.removeTextChangedListener(mTextWatcher);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return super.onOptionsItemSelected(item);
    }

    private void fetchMessageList() {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, EndPoints.CHAT_THREAD.replace("_ID_", mChatRoomId), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e(TAG, "응답: " + response);

                try {
                    JSONObject obj = new JSONObject(response);

                    if (!obj.getBoolean("error")) {
                        JSONArray commentsObj = obj.getJSONArray("messages");

                        for (int i = 0; i < commentsObj.length(); i++) {
                            JSONObject commentObj = (JSONObject) commentsObj.get(i);
                            String commentId = commentObj.getString("message_id");
                            String commentText = commentObj.getString("message");
                            String createdAt = commentObj.getString("created_at");
                            JSONObject userObj = commentObj.getJSONObject("user");
                            String userId = userObj.getString("user_id");
                            String userName = userObj.getString("username");
                            User user = new User(userId, userName, null);
                            Message message = new Message();

                            message.setId(commentId);
                            message.setMessage(commentText);
                            message.setCreatedAt(createdAt);
                            message.setUser(user);
                            mMessageList.add(message);
                        }
                        mAdapter.notifyDataSetChanged();
                        if (mAdapter.getItemCount() > 1)
                            mRecyclerView.getLayoutManager().smoothScrollToPosition(mRecyclerView, null, mAdapter.getItemCount() - 1);
                    } else
                        Toast.makeText(getApplicationContext(), "" + obj.getJSONObject("error").getString("message"), Toast.LENGTH_LONG).show();
                } catch (JSONException e) {
                    Log.e(TAG, "json 파싱 에러: " + e.getMessage());
                    Toast.makeText(getApplicationContext(), "json 파싱 에러: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                Log.e(TAG, "Volley 에러: " + error.getMessage() + ", 코드: " + networkResponse);
                Toast.makeText(getApplicationContext(), "Volley 에러: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        AppController.getInstance().addToRequestQueue(stringRequest);
    }
}
