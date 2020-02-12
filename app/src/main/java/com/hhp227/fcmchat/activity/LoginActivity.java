package com.hhp227.fcmchat.activity;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.hhp227.fcmchat.R;
import com.hhp227.fcmchat.app.AppController;
import com.hhp227.fcmchat.app.EndPoints;
import com.hhp227.fcmchat.dto.User;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = LoginActivity.class.getSimpleName();
    private EditText mInputName, mInputEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = findViewById(R.id.toolbar);
        Button buttonEnter = findViewById(R.id.b_enter);
        mInputName = findViewById(R.id.et_name);
        mInputEmail = findViewById(R.id.et_email);

        setSupportActionBar(toolbar);
        if (AppController.getInstance().getPrefManager().getUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
        buttonEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String name = mInputName.getText().toString();
                final String email = mInputEmail.getText().toString();
                if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    StringRequest stringRequest = new StringRequest(Request.Method.POST, EndPoints.LOGIN, new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.e(TAG, "response: " + response);
                            try {
                                JSONObject obj = new JSONObject(response);
                                if (!obj.getBoolean("error")) {
                                    JSONObject userObj = obj.getJSONObject("user");
                                    User user = new User(userObj.getString("user_id"), userObj.getString("name"), userObj.getString("email"));

                                    AppController.getInstance().getPrefManager().storeUser(user);
                                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                    finish();
                                } else
                                    Toast.makeText(getApplicationContext(), "" + obj.getJSONObject("error").getString("message"), Toast.LENGTH_LONG).show();
                            } catch (JSONException e) {
                                Log.e(TAG, "json parsing error: " + e.getMessage());
                                Toast.makeText(getApplicationContext(), "Json parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            NetworkResponse networkResponse = error.networkResponse;
                            Log.e(TAG, "Volley error: " + error.getMessage() + ", code: " + networkResponse);
                            Toast.makeText(getApplicationContext(), "Volley error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }) {
                        @Override
                        protected Map<String, String> getParams() {
                            Map<String, String> params = new HashMap<>();
                            params.put("name", name);
                            params.put("email", email);

                            Log.e(TAG, "params: " + params.toString());
                            return params;
                        }
                    };
                    AppController.getInstance().addToRequestQueue(stringRequest);
                } else {
                    mInputName.setError(name.isEmpty() ? "이름을 입력하세요." : null);
                    mInputEmail.setError(email.isEmpty() ? "이메일을 입력하세요." : !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ? "올바른 이메일 형식이 아닙니다." : null);
                }
            }
        });
    }
}

