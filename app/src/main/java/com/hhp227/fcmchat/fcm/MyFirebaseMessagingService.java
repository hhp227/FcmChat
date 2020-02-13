package com.hhp227.fcmchat.fcm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.hhp227.fcmchat.activity.ChatActivity;
import com.hhp227.fcmchat.activity.MainActivity;
import com.hhp227.fcmchat.app.AppController;
import com.hhp227.fcmchat.app.Config;
import com.hhp227.fcmchat.app.EndPoints;
import com.hhp227.fcmchat.dto.Message;
import com.hhp227.fcmchat.dto.User;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = MyFirebaseMessagingService.class.getSimpleName();
    private NotificationUtils mNotificationUtils;

    @Override
    public void onMessageReceived(RemoteMessage message) {
        String from = message.getFrom();
        Map bundle = message.getData();
        String title = bundle.get("title").toString();
        Boolean isBackground = Boolean.valueOf(bundle.get("is_background").toString());
        String flag = bundle.get("flag").toString();
        String data = bundle.get("data").toString();

        Log.d(TAG, "From: " + from);
        Log.d(TAG, "title: " + title);
        Log.d(TAG, "isBackground: " + isBackground);
        Log.d(TAG, "flag: " + flag);
        Log.d(TAG, "data: " + data);
        if (flag == null)
            return;
        if (AppController.getInstance().getPrefManager().getUser() == null){
            Log.e(TAG, "푸시 알림을 건너 뛰고 사용자가 로그인하지 않았습니다.");
            return;
        }
        if (from.startsWith("/topics/")) {
            // 몇 topic 으로부터 메시지 수신
        } else {
            // 정상적인 다운 스트림 메시지.
        }
        switch (Integer.parseInt(flag)) {
            case Config.PUSH_TYPE_CHATROOM:
                processChatRoomPush(title, isBackground, data);
                break;
            case Config.PUSH_TYPE_USER:
                processUserMessage(title, isBackground, data);
                break;
        }
    }

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        Log.e(TAG, "onTokenRefresh");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            SharedPreferences preferences = getApplicationContext().getSharedPreferences(Config.SHARED_PREF, 0);
            SharedPreferences.Editor editor = preferences.edit();

            editor.putString("regId", s);
            editor.apply();
            sendRegistrationToServer(s);
            sharedPreferences.edit().putBoolean(Config.SENT_TOKEN_TO_SERVER, true).apply();
        } catch (Exception e) {
            Log.e(TAG, "토큰 새로 고침 실패", e);
            sharedPreferences.edit().putBoolean(Config.SENT_TOKEN_TO_SERVER, false).apply();
        }
        Intent registrationComplete = new Intent(Config.REGISTRATION_COMPLETE);
        registrationComplete.putExtra("token", s);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    private void processChatRoomPush(String title, Boolean isBackground, String data) {
        if (!isBackground) {
            try {
                JSONObject datObj = new JSONObject(data);
                String chatRoomId = datObj.getString("chat_room_id");
                JSONObject mObj = datObj.getJSONObject("message");
                Message message = new Message();

                message.setMessage(mObj.getString("message"));
                message.setId(mObj.getString("message_id"));
                message.setCreatedAt(mObj.getString("created_at"));

                JSONObject uObj = datObj.getJSONObject("user");

                if (uObj.getString("user_id").equals(AppController.getInstance().getPrefManager().getUser().getId())) {
                    Log.e(TAG, "푸시 메시지가 동일한 사용자에게 속하므로 생략");
                    return;
                }
                User user = new User();
                user.setId(uObj.getString("user_id"));
                user.setEmail(uObj.getString("email"));
                user.setName(uObj.getString("name"));
                message.setUser(user);
                if (!NotificationUtils.isAppIsInBackground(getApplicationContext())) {
                    Intent pushNotification = new Intent(Config.PUSH_NOTIFICATION);

                    pushNotification.putExtra("type", Config.PUSH_TYPE_CHATROOM);
                    pushNotification.putExtra("message", message);
                    pushNotification.putExtra("chat_room_id", chatRoomId);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification);

                    NotificationUtils notificationUtils = new NotificationUtils();

                    notificationUtils.playNotificationSound();
                } else {
                    Intent resultIntent = new Intent(getApplicationContext(), ChatActivity.class);
                    resultIntent.putExtra("chat_room_id", chatRoomId);
                    showNotificationMessage(getApplicationContext(), title, user.getName() + " : " + message.getMessage(), message.getCreatedAt(), resultIntent);
                }
            } catch (JSONException e) {
                Log.e(TAG, "json 파싱 에러: " + e.getMessage());
                Toast.makeText(getApplicationContext(), "Json 파싱 에러: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            // 푸시 알림이 조용히 일어남. SQLite에 삽입하는 것과 같은 다른 작업이 필요할 수 있음.
        }
    }

    private void processUserMessage(String title, Boolean isBackground, String data) {
        if (!isBackground) {
            try {
                JSONObject datObj = new JSONObject(data);
                String imageUrl = datObj.getString("image");
                JSONObject mObj = datObj.getJSONObject("message");
                Message message = new Message();

                message.setMessage(mObj.getString("message"));
                message.setId(mObj.getString("message_id"));
                message.setCreatedAt(mObj.getString("created_at"));

                JSONObject uObj = datObj.getJSONObject("user");
                User user = new User();

                user.setId(uObj.getString("user_id"));
                user.setEmail(uObj.getString("email"));
                user.setName(uObj.getString("name"));
                message.setUser(user);
                if (!NotificationUtils.isAppIsInBackground(getApplicationContext())) {
                    Intent pushNotification = new Intent(Config.PUSH_NOTIFICATION);
                    pushNotification.putExtra("type", Config.PUSH_TYPE_USER);
                    pushNotification.putExtra("message", message);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification);
                    NotificationUtils notificationUtils = new NotificationUtils();
                    notificationUtils.playNotificationSound();
                } else {
                    Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);

                    if (TextUtils.isEmpty(imageUrl)) {
                        showNotificationMessage(getApplicationContext(), title, user.getName() + " : " + message.getMessage(), message.getCreatedAt(), resultIntent);
                    } else {
                        showNotificationMessageWithBigImage(getApplicationContext(), title, message.getMessage(), message.getCreatedAt(), resultIntent, imageUrl);
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "json 파싱 에러: " + e.getMessage());
                Toast.makeText(getApplicationContext(), "Json 파싱 에러: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            // 푸시 알림이 조용히 일어남. SQLite에 삽입하는 것과 같은 다른 작업이 필요할 수 있음.
        }
    }

    private void showNotificationMessage(Context context, String title, String message, String timeStamp, Intent intent) {
        mNotificationUtils = new NotificationUtils(context);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mNotificationUtils.showNotificationMessage(title, message, timeStamp, intent);
    }

    private void showNotificationMessageWithBigImage(Context context, String title, String message, String timeStamp, Intent intent, String imageUrl) {
        mNotificationUtils = new NotificationUtils(context);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mNotificationUtils.showNotificationMessage(title, message, timeStamp, intent, imageUrl);
    }

    private void sendRegistrationToServer(final String token) {
        User user = AppController.getInstance().getPrefManager().getUser();
        if (user == null) {
            // TODO
            // 사용자를 찾지 못해 로그인 화면으로 리디렉션
            return;
        }
        StringRequest stringRequest = new StringRequest(Request.Method.PUT, EndPoints.USER.replace("_ID_", user.getId()), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e(TAG, "응답: " + response);
                try {
                    JSONObject obj = new JSONObject(response);

                    if (!obj.getBoolean("error")) {
                        Intent registrationComplete = new Intent(Config.SENT_TOKEN_TO_SERVER);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(registrationComplete);
                    } else {
                        Toast.makeText(getApplicationContext(), "서버에 fcm 등록 ID를 보낼 수 없습니다. " + obj.getJSONObject("error").getString("message"), Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "json 파싱 에러: " + e.getMessage());
                    Toast.makeText(getApplicationContext(), "Json 파싱 에러: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                Log.e(TAG, "Volley 에러: " + error.getMessage() + ", 코드: " + networkResponse);
                Toast.makeText(getApplicationContext(), "Volley 에러: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("gcm_registration_id", token);

                Log.e(TAG, "params: " + params.toString());
                return params;
            }
        };
        AppController.getInstance().addToRequestQueue(stringRequest);
    }
}
