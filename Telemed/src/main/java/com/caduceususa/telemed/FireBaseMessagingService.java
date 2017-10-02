package com.caduceususa.telemed;

import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Created by Erik Grosskurth on 04/17/2017.
 */

public class FireBaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "exampleMessage";
    private static String link = "https://www.caduceus24-7.com/";
    private static String visitId = "0";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d("onMessageReceived: ", "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Map data = remoteMessage.getData();
            Log.d("PAYLOAD: ", data.toString());
            link = (String) data.get("link");
            visitId = (String) data.get("visit_id");

            //Uri sound= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            //builder.setSound(sound);
            Log.d("LINK FROM SERVER", (String) data.get("link"));
            Log.d("VISIT ID FROM SERVER", (String) data.get("link"));
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
        }else {
            Log.d("PAYLOAD: ", "None");
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        };

        notifyUser(remoteMessage.getFrom(), remoteMessage.getNotification().getBody(), link);

    }

    public void notifyUser(String from, String notification, String link) {
        AppNotificationManager appNotificationManager = new AppNotificationManager(getApplicationContext());
        appNotificationManager.showNotification(from, notification, link, visitId, new Intent(getApplicationContext(), FireBaseInitialization.class));
    }
}
