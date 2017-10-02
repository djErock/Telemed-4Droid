package com.caduceususa.telemed;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * Created by Erik Grosskurth on 04/17/2017.
 */

public class AppFirebaseInstanceIdService extends FirebaseInstanceIdService {
    public static final String TOKEN_BROADCAST = "tokenBroadcast";

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d("myFireBaseId", "Refreshed token: " + refreshedToken);
        DataModel.sharedInstance().firebaseAuthenticationToken = refreshedToken;
        getApplicationContext().sendBroadcast(new Intent(TOKEN_BROADCAST));
        storeToken(refreshedToken);
    }

    private void storeToken(String token) {
        SharedPrefManager.getInstance(getApplicationContext()).storeToken(token);
    }

}
