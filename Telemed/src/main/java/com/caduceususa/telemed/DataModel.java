package com.caduceususa.telemed;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.quickblox.auth.session.QBSessionManager;
import com.quickblox.users.model.QBUser;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;

/**
 * Created by Erik Grosskurth on 06/13/2017.
 */

public class DataModel {

    private static DataModel mInstance = null;

    private Context mContext;

    ProgressDialog progressDialog;

    protected DataModel(){}

    public static synchronized DataModel sharedInstance(){
        if(null == mInstance){
            mInstance = new DataModel();
        }
        return mInstance;
    }

    public String Caduceus_API = "https://telemed.caduceususa.com/ws";
    public String key;
    public String firebaseAuthenticationToken;
    public String email;
    public String password;
    public String QB_EXT_USER_ID;
    public String QB_APP_ID = "5";
    public String QB_AUTH_KEY = "ucYFVeFnKyxSNgj";
    public String QB_AUTH_SECRET = "qXtVhw658vcL5XM";
    public String QB_API_DOMAIN = "https://apicaduceustelemed.quickblox.com";
    public String QB_CHAT_DOMAIN = "chatcaduceustelemed.quickblox.com";

    public static QBUser qbUserParams = new QBUser();

    public static void resetQBUserObject() {
        qbUserParams = new QBUser();
    }

    public void AlertDialogBuilder(Context ctx, String mTitle, String mMessage) {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(ctx, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(ctx);
        }
        builder.setTitle(mTitle)
        .setMessage(mMessage)
        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // continue with delete
            }
        })
        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // do nothing
            }
        })
        .setIcon(android.R.drawable.ic_dialog_alert)
        .show();
    }

    public void volleyJsonObjectPost(final Boolean showProgressDialog, final Context ctx, String url, JSONObject data, final ServerCallback callback){

        String  REQUEST_TAG = "com.Telemed.volleyJsonObjectRequest";

        if (showProgressDialog) {
            progressDialog = new ProgressDialog(ctx);
            progressDialog.setMessage("Accessing Network Data...");
            progressDialog.show();
        }

        Boolean connectionStatus = NetworkUtils.isNetworkConnected(ctx);
        Log.d("Connected? ", connectionStatus.toString());
        if( !NetworkUtils.isNetworkConnected(ctx) ) {
            if (showProgressDialog) {
                progressDialog.hide();
            }
            AlertDialogBuilder(
                ctx,
                "Connectivity?",
                "Experiencing issues connecting to Telemed servers. Please check internet connection and try again. If the problem persists, please contact a Telemed staff member."
            );
        }

        JsonObjectRequest jsonObjectReq = new JsonObjectRequest(url, data, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    if (showProgressDialog) {
                        progressDialog.hide();
                    }
                    Log.d("VolleySuccess Handler: ", response.toString());
                    callback.onSuccess(response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("Server Error: ", new String(error.networkResponse.data));
                    if (showProgressDialog) {
                        progressDialog.hide();
                    }
                    AlertDialogBuilder(
                        ctx,
                        "Session Expired",
                        "Please try logging in again."
                    );

                    AlertDialog alertDialog = new AlertDialog.Builder(ctx).create();
                    alertDialog.setTitle("Session Expired");
                    alertDialog.setMessage("Please try logging in again.");
                    alertDialog.setButton(
                        AlertDialog.BUTTON_NEUTRAL,
                        "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent();
                                intent.setClass(ctx, Login.class);
                                ctx.startActivity(intent);
                                //dialog.dismiss();
                            }
                        });
                    alertDialog.show();
                }
            }
        );

        // Adding JsonObject request to request queue
        AppSingleton.getInstance(ctx).addToRequestQueue(jsonObjectReq,REQUEST_TAG);
    }

    public void volleyCacheRequest(Context ctx, String url){
        Cache cache = AppSingleton.getInstance(ctx).getRequestQueue().getCache();
        Cache.Entry entry = cache.get(url);
        if(entry != null){
            try {
                String data = new String(entry.data, "UTF-8");
                // handle data, like converting it to xml, json, bitmap etc.,
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        else{

        }
    }

    public void volleyInvalidateCache(Context ctx, String url){
        AppSingleton.getInstance(ctx).getRequestQueue().getCache().invalidate(url, true);
    }

    public void volleyDeleteCache(Context ctx, String url){
        AppSingleton.getInstance(ctx).getRequestQueue().getCache().remove(url);
    }

    public void volleyClearCache(Context ctx){
        AppSingleton.getInstance(ctx).getRequestQueue().getCache().clear();
    }

    public boolean checkSignIn() {
        return QBSessionManager.getInstance().getSessionParameters() != null;
    }

    public interface ServerCallback{
        void onSuccess(JSONObject result);
    }
}
