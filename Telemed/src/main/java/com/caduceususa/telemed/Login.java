package com.caduceususa.telemed;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.quickblox.auth.QBAuth;
import com.quickblox.auth.session.QBSession;
import com.quickblox.auth.session.QBSessionManager;
import com.quickblox.auth.session.QBSessionParameters;
import com.quickblox.auth.session.QBSettings;
import com.quickblox.chat.QBChatService;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.ServiceZone;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.helper.StringifyArrayList;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static android.R.attr.password;
import static com.caduceususa.telemed.CONSTANTS.*;
import static com.caduceususa.telemed.R.id.login;
import static com.google.firebase.messaging.FirebaseMessaging.getInstance;

public class Login extends AppCompatActivity {


    private static final String TAG = "Login";
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.title_activity_login);
        setContentView(R.layout.activity_login);



        FirebaseApp.initializeApp(this);
        QBSettings.getInstance().init(getApplicationContext(), DataModel.sharedInstance().QB_APP_ID, DataModel.sharedInstance().QB_AUTH_KEY, DataModel.sharedInstance().QB_AUTH_SECRET);
        QBSettings.getInstance().setEndpoints( DataModel.sharedInstance().QB_API_DOMAIN, DataModel.sharedInstance().QB_CHAT_DOMAIN, ServiceZone.PRODUCTION);
        QBSettings.getInstance().setZone(ServiceZone.PRODUCTION);

        findViewById(R.id.LoginLayout).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d("OnTouchListener", "Touch");
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                return true;
            }
        });

        loginButton = (Button)findViewById(login);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            final EditText username =  (EditText) findViewById(R.id.sUser);
            final EditText password =  (EditText) findViewById(R.id.sPass);

            Map<String, String> params = new HashMap();
            params.put("sUser", username.getText().toString());
            params.put("sPass", password.getText().toString());
            final JSONObject parameters = new JSONObject(params);
            Log.d("parameters: ", parameters.toString());
            DataModel.sharedInstance().volleyJsonObjectPost(true, Login.this, LOGIN_WS_URL, parameters, new DataModel.ServerCallback() {
                public void onSuccess(JSONObject response) {
                try {
                    final JSONObject loginData = response.getJSONObject("d");
                    DataModel.sharedInstance().key = loginData.getString("key");
                    DataModel.sharedInstance().email = username.getText().toString();
                    DataModel.sharedInstance().password = password.getText().toString();
                    DataModel.sharedInstance().firebaseAuthenticationToken = FirebaseInstanceId.getInstance().getToken();

                    Map<String, String> params = new HashMap();
                    params.put("sKey", loginData.getString("key"));
                    final JSONObject parameters = new JSONObject(params);
                    Log.d("parameters: ", parameters.toString());
                    DataModel.sharedInstance().volleyJsonObjectPost(true, Login.this, GETUSER_WS_URL, parameters, new DataModel.ServerCallback() {
                        public void onSuccess(JSONObject response) {
                        try {
                            DataModel.sharedInstance().resetQBUserObject();
                            final JSONObject oUser = response.getJSONObject("d");
                            DataModel.sharedInstance().qbUserParams.setLogin(oUser.getString("first_name").replaceAll("[^a-zA-Z]+","") + oUser.getString("last_name").replaceAll("[^a-zA-Z]+","") + oUser.getString("user_id").replaceAll("[^0-9]+",""));
                            DataModel.sharedInstance().qbUserParams.setPassword("=TelemedUser#"+oUser.getString("user_id")+"!");
                            DataModel.sharedInstance().qbUserParams.setEmail(oUser.getString("email_address"));
                            DataModel.sharedInstance().qbUserParams.setFullName(oUser.getString("first_name") + " " + oUser.getString("last_name"));
                            DataModel.sharedInstance().qbUserParams.setExternalId(oUser.getString("user_id"));
                            DataModel.sharedInstance().QB_EXT_USER_ID = oUser.getString("user_id");
                            StringifyArrayList tags = new StringifyArrayList();
                            DataModel.sharedInstance().qbUserParams.setTags(tags);

                            QBUsers.signIn(DataModel.sharedInstance().qbUserParams).performAsync(new QBEntityCallback<QBUser>() {
                                @Override
                                public void onSuccess(QBUser user, Bundle args) {
                                    //Log.d("QB USER Params: ", DataModel.sharedInstance().qbUserParams.toString());
                                    correlateUsers(oUser, "edit", new correlateCallback() {
                                        public void onSuccess(Intent intent) {
                                            startActivity(intent);
                                        }
                                    });
                                }
                                @Override
                                public void onError(QBResponseException error) {
                                    Log.d("QB signIn error: ", error.toString());
                                    Log.d("QB USER Params: ", DataModel.sharedInstance().qbUserParams.toString());
                                    QBUsers.signUp(DataModel.sharedInstance().qbUserParams).performAsync( new QBEntityCallback<QBUser>() {
                                        @Override
                                        public void onSuccess(QBUser user, Bundle args) {
                                            correlateUsers(oUser, "add", new correlateCallback() {
                                                public void onSuccess(Intent intent) {
                                                    startActivity(intent);
                                                }
                                            });
                                        }
                                        @Override
                                        public void onError(QBResponseException error) {
                                            Log.d("QB signUp error: ", error.toString());
                                        }
                                    });
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                }
            });
            }
        });

        QBSessionManager.getInstance().addListener(new QBSessionManager.QBSessionListener() {
            @Override
            public void onSessionCreated(QBSession session) {
                //calls when session was created firstly or after it has been expired
                Log.d("onSessionCreated: ", session.toString());
            }

            @Override
            public void onSessionUpdated(QBSessionParameters sessionParameters) {
                //calls when user signed in or signed up
                //QBSessionParameters stores information about signed in user.
                Log.d("onSessionUpdated: ", sessionParameters.toString());
            }

            @Override
            public void onSessionDeleted() {
                //calls when user signed Out or session was deleted
                Log.d("onSessionDeleted: ", "Deleted");
            }

            @Override
            public void onSessionRestored(QBSession session) {
                //calls when session was restored from local storage
                Log.d("onSessionCreated: ", session.toString());
            }

            @Override
            public void onSessionExpired() {
                //calls when session is expired
                Log.d("onSessionExpired: ", "Expired");
            }

            public void onProviderSessionExpired(String provider) {
                //calls when provider's access token is expired or invalid
                Log.d("onProvSessExpired: ", provider.toString());
            }

        });
    }

    public void correlateUsers(final JSONObject oUser, final String status, final correlateCallback callback){
        JSONObject parameters = new JSONObject();
        final JSONObject qbUser;
        Gson gson = new Gson();
        String jsonString = gson.toJson(DataModel.sharedInstance().qbUserParams);
        try {
            qbUser = new JSONObject(jsonString);
            parameters.put("sKey", DataModel.sharedInstance().key);
            parameters.put("sUpdateType", status);
            parameters.put("oQbUser", qbUser);
            Log.d("parameters: ", parameters.toString());
            DataModel.sharedInstance().volleyJsonObjectPost(true, Login.this, UPDATEQBUSER_WS_URL, parameters, new DataModel.ServerCallback() {
                public void onSuccess(JSONObject response) {
                    Map<String, String> params = new HashMap();
                    params.put("sKey", DataModel.sharedInstance().key);
                    params.put("sRegToken", DataModel.sharedInstance().firebaseAuthenticationToken);
                    final JSONObject parameters = new JSONObject(params);
                    Log.d("parameters: ", parameters.toString());
                    DataModel.sharedInstance().volleyJsonObjectPost(true, Login.this, UPDATECREDS_WS_URL, parameters, new DataModel.ServerCallback() {
                        public void onSuccess(JSONObject response) {
                            try {
                                Class<?> destination = null;
                                if (oUser.getInt("userlevel_id") == 11) {
                                    getInstance().subscribeToTopic("providers");
                                    destination = ProviderDashboard.class;
                                } else if (oUser.getInt("userlevel_id") == 3) {
                                    getInstance().subscribeToTopic("safetyManagers");
                                    //destination = safetyManagerDashboard.class;
                                } else {
                                    getInstance().subscribeToTopic("patients");
                                    //destination = patientDashboard.class;
                                }
                                Intent intent = new Intent(Login.this, destination);
                                intent.putExtra("key", DataModel.sharedInstance().key);
                                callback.onSuccess(intent);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                }
            });
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public interface correlateCallback{
        void onSuccess(Intent intent);
    }
}
