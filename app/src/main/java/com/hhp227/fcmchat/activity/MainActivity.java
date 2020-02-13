package com.hhp227.fcmchat.activity;

import android.content.*;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.messaging.FirebaseMessaging;
import com.hhp227.fcmchat.R;
import com.hhp227.fcmchat.adapter.ChatRoomAdapter;
import com.hhp227.fcmchat.app.AppController;
import com.hhp227.fcmchat.app.Config;
import com.hhp227.fcmchat.app.EndPoints;
import com.hhp227.fcmchat.dto.ChatRoom;
import com.hhp227.fcmchat.dto.Message;
import com.hhp227.fcmchat.fcm.NotificationUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private String TAG = MainActivity.class.getSimpleName();
    private BroadcastReceiver mBroadcastReceiver;
    private ChatRoomAdapter mAdapter;
    private List<ChatRoom> mChatRoomList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case Config.REGISTRATION_COMPLETE:
                        FirebaseMessaging.getInstance().subscribeToTopic(Config.TOPIC_GLOBAL);
                        break;
                    case Config.SENT_TOKEN_TO_SERVER:
                        Log.e(TAG, "FCM 등록 ID가 서버로 전송됩니다");
                        break;
                    case Config.PUSH_NOTIFICATION:
                        int type = intent.getIntExtra("type", -1);
                        if (type == Config.PUSH_TYPE_CHATROOM) {
                            Message message = (Message) intent.getSerializableExtra("message");
                            String chatRoomId = intent.getStringExtra("chat_room_id");

                            if (message != null && chatRoomId != null) {
                                for (ChatRoom cr : mChatRoomList) {
                                    if (cr.getId().equals(chatRoomId)) {
                                        int index = mChatRoomList.indexOf(cr);
                                        cr.setLastMessage(message.getMessage());
                                        cr.setUnreadCount(cr.getUnreadCount() + 1);
                                        mChatRoomList.remove(index);
                                        mChatRoomList.add(index, cr);
                                        break;
                                    }
                                }
                                mAdapter.notifyDataSetChanged();
                            }
                        } else if (type == Config.PUSH_TYPE_USER) {
                            Message message = (Message) intent.getSerializableExtra("message");

                            Toast.makeText(getApplicationContext(), "새로운 푸시: " + message.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        break;
                }
            }
        };
        mChatRoomList = new ArrayList<>();
        mAdapter = new ChatRoomAdapter(this, mChatRoomList);

        setSupportActionBar(toolbar);
        if (AppController.getInstance().getPrefManager().getUser() == null) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
        mAdapter.setOnItemClickListener(new ChatRoomAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                ChatRoom chatRoom = mChatRoomList.get(position);
                Intent intent = new Intent(MainActivity.this, ChatActivity.class);

                intent.putExtra("chat_room_id", chatRoom.getId());
                intent.putExtra("name", chatRoom.getName());
                startActivity(intent);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);
        if (checkPlayServices()) {
            displayFirebaseRegId();
            fetchChatRooms();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, new IntentFilter(Config.REGISTRATION_COMPLETE));
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, new IntentFilter(Config.PUSH_NOTIFICATION));
        NotificationUtils.clearNotifications();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            AppController.getInstance().getPrefManager().clear();
            Intent intent = new Intent(this, LoginActivity.class);

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "이 장치는 지원되지 않습니다. Google Play 서비스가 설치되지 않았습니다!");
                Toast.makeText(getApplicationContext(), "이 장치는 지원되지 않습니다. Google Play 서비스가 설치되지 않았습니다!", Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        }
        return true;
    }

    private void displayFirebaseRegId() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences(Config.SHARED_PREF, 0);
        String regId = pref.getString("regId", null);

        Log.e(TAG, "Firebase reg id: " + regId);
        if (!TextUtils.isEmpty(regId))
            Toast.makeText(getApplicationContext(), "파이어베이스 등록 Id: " + regId, Toast.LENGTH_LONG).show();
        else
            Toast.makeText(getApplicationContext(), "Firebase 등록 ID를 아직받지 못했습니다!", Toast.LENGTH_LONG).show();
    }

    private void fetchChatRooms() {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, EndPoints.CHAT_ROOMS, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e(TAG, "응답: " + response);
                try {
                    JSONObject obj = new JSONObject(response);

                    if (!obj.getBoolean("error")) {
                        JSONArray chatRoomsArray = obj.getJSONArray("chat_rooms");

                        for (int i = 0; i < chatRoomsArray.length(); i++) {
                            JSONObject chatRoomsObj = (JSONObject) chatRoomsArray.get(i);
                            ChatRoom cr = new ChatRoom();

                            cr.setId(chatRoomsObj.getString("chat_room_id"));
                            cr.setName(chatRoomsObj.getString("name"));
                            cr.setLastMessage("");
                            cr.setUnreadCount(0);
                            cr.setTimestamp(chatRoomsObj.getString("created_at"));
                            mChatRoomList.add(cr);
                        }
                    } else
                        Toast.makeText(getApplicationContext(), "" + obj.getJSONObject("error").getString("message"), Toast.LENGTH_LONG).show();
                } catch (JSONException e) {
                    Log.e(TAG, "json 파싱 에러: " + e.getMessage());
                    Toast.makeText(getApplicationContext(), "Json 파싱 에러: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
                mAdapter.notifyDataSetChanged();
                for (ChatRoom cr : mChatRoomList)
                    FirebaseMessaging.getInstance().subscribeToTopic("topic_" + cr.getId());
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
