package com.hhp227.fcmchat.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.hhp227.fcmchat.dto.User;

public class PreferenceManager {
    private static final int PRIVATE_MODE = 0;
    private static final String TAG = PreferenceManager.class.getSimpleName();
    private static final String PREF_NAME = "hhp227_fcm";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_NOTIFICATIONS = "notifications";
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private Context mContext;

    public PreferenceManager(Context context) {
        this.mContext = context;
        mSharedPreferences = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        mEditor = mSharedPreferences.edit();
    }

    public void storeUser(User user) {
        mEditor.putString(KEY_USER_ID, user.getId());
        mEditor.putString(KEY_USER_NAME, user.getName());
        mEditor.putString(KEY_USER_EMAIL, user.getEmail());
        mEditor.commit();

        Log.e(TAG, "User is stored in shared preferences. " + user.getName() + ", " + user.getEmail());
    }

    public User getUser() {
        if (mSharedPreferences.getString(KEY_USER_ID, null) != null) {
            String id, name, email;
            id = mSharedPreferences.getString(KEY_USER_ID, null);
            name = mSharedPreferences.getString(KEY_USER_NAME, null);
            email = mSharedPreferences.getString(KEY_USER_EMAIL, null);

            User user = new User(id, name, email);
            return user;
        }
        return null;
    }

    public void addNotification(String notification) {
        String oldNotifications = getNotifications();

        if (oldNotifications != null) {
            oldNotifications += "|" + notification;
        } else {
            oldNotifications = notification;
        }

        mEditor.putString(KEY_NOTIFICATIONS, oldNotifications);
        mEditor.commit();
    }

    public String getNotifications() {
        return mSharedPreferences.getString(KEY_NOTIFICATIONS, null);
    }

    public void clear() {
        mEditor.clear();
        mEditor.commit();
    }
}
