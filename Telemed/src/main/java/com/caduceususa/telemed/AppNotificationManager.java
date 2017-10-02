package com.caduceususa.telemed;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.EditText;

import com.android.volley.Cache;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.caduceususa.telemed.ProviderDashboard;
import com.caduceususa.telemed.R;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.google.firebase.messaging.FirebaseMessaging.getInstance;

/**
 * Created by Erik Grosskurth on 04/17/2017.
 */

public class AppNotificationManager {

    private Context ctx;

    public AppNotificationManager(Context ctx) {
        this.ctx = ctx;
    }

    public static final String DEFAULT_ACTION = "Provider_Dashboard";

    public void showNotification(String from, final String notification, final String link, final String visitId, Intent intent) {

        Uri alertSound = Uri.parse("android.resource://" + ctx.getPackageName() + "/raw/page_the_doctor");

        NotificationManager mNotificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        //CLICK ON NOTIFICATION
        Intent notificationIntent = new Intent(ctx, ProviderDashboard.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent clickIntent = PendingIntent.getActivity(ctx, Integer.parseInt(visitId), notificationIntent, 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(ctx)
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.telemed_logo)
                .setContentTitle("TELEMED PATIENT READY")
                .setContentText(notification)
                .setDefaults( Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                .setSound( alertSound )
                .setPriority(Notification.PRIORITY_MAX)
                .setContentIntent(clickIntent)
                .setAutoCancel(true);

        Notification noti = notificationBuilder.build();
        noti.flags |= Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS;

        mNotificationManager.notify(Integer.parseInt(visitId), noti);

    }




}
