package com.caduceususa.telemed;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.quickblox.auth.QBAuth;
import com.quickblox.auth.session.QBSession;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBGroupChat;
import com.quickblox.chat.QBGroupChatManager;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.QBSignaling;
import com.quickblox.chat.QBWebRTCSignaling;
import com.quickblox.chat.listeners.QBVideoChatSignalingManagerListener;
import com.quickblox.chat.model.QBAttachment;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.chat.model.QBDialogType;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.QBEntityCallbackImpl;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.helper.StringifyArrayList;
import com.quickblox.core.request.QBPagedRequestBuilder;
import com.quickblox.core.request.QBRequestGetBuilder;
import com.quickblox.core.request.QueryRule;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;
import com.quickblox.videochat.webrtc.QBRTCClient;
import com.quickblox.videochat.webrtc.QBRTCSession;
import com.quickblox.videochat.webrtc.callbacks.QBRTCClientSessionCallbacks;
import com.quickblox.videochat.webrtc.callbacks.QBRTCClientVideoTracksCallbacks;
import com.quickblox.videochat.webrtc.callbacks.QBRTCSessionConnectionCallbacks;
import com.quickblox.videochat.webrtc.exception.QBRTCException;
import com.quickblox.videochat.webrtc.view.QBRTCVideoTrack;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.VideoRenderer;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.firebase.messaging.FirebaseMessaging.getInstance;

public class TelemedExamRoom extends AppCompatActivity {

    QBChatService chatService;
    Integer visitId;
    String roomName;
    Integer chatSessionStatus;
    ArrayList<QBUser> peers = new ArrayList<QBUser>();
    ArrayList<QBUser> occupants = new ArrayList<QBUser>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_telemed_exam_room);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras == null) {
                Log.d( "Extras are ", "NULL" );
            } else {
                roomName = extras.getString("roomName");
                visitId = extras.getInt("visitId");
            }
        } else {
            roomName = (String) savedInstanceState.getSerializable("roomName");
            visitId = (Integer) savedInstanceState.getSerializable("visitId");
        }

        final QBUser qbuser = DataModel.sharedInstance().qbUserParams;

        QBChatService.setDebugEnabled(true); // enable chat logging
        QBChatService.setDefaultPacketReplyTimeout(10000);//set reply timeout in milliseconds for connection's packet.

        QBChatService.ConfigurationBuilder chatServiceConfigurationBuilder = new QBChatService.ConfigurationBuilder();
        chatServiceConfigurationBuilder.setSocketTimeout(60); //Sets chat socket's read timeout in seconds
        chatServiceConfigurationBuilder.setKeepAlive(true); //Sets connection socket's keepAlive option.
        chatServiceConfigurationBuilder.setUseTls(true); //Sets the TLS security mode used when making the connection. By default TLS is disabled.
        QBChatService.setConfigurationBuilder(chatServiceConfigurationBuilder);

        QBAuth.createSession(qbuser).performAsync(new QBEntityCallback<QBSession>() {
            @Override
            public void onSuccess(QBSession qbSession, Bundle bundle) {
                QBChatService.getInstance().login(qbuser, new QBEntityCallback() {
                    @Override
                    public void onSuccess(Object o, Bundle bundle) {
                        final QBUser updatedUser = new QBUser();
                        updatedUser.setId(DataModel.sharedInstance().qbUserParams.getId());
                        StringifyArrayList<String> tags = new StringifyArrayList<String>();
                        tags.add(roomName);
                        updatedUser.setTags(tags);
                        QBUsers.updateUser(updatedUser).performAsync(new QBEntityCallback<QBUser>() {
                            @Override
                            public void onSuccess(QBUser updatedQBUser, Bundle bundle) {
                                Log.d( "Updated qbuser? ", updatedQBUser.toString() );
                                DataModel.sharedInstance().qbUserParams = updatedQBUser;
                                updatePeerList(updatedQBUser, new peersCallback() {
                                    @Override
                                    public void onSuccess() {
                                        QBRequestGetBuilder requestBuilder = new QBRequestGetBuilder();
                                        requestBuilder.setLimit(100);
                                        requestBuilder.addRule("type", QueryRule.EQ, "2");
                                        requestBuilder.addRule("name", QueryRule.EQ, roomName);
                                        //requestBuilder.addRule("tags", QueryRule.EQ, updatedUser.getTags());
                                        QBRestChatService.getChatDialogs(null, requestBuilder).performAsync( new QBEntityCallback<ArrayList<QBChatDialog>>() {
                                            @Override
                                            public void onSuccess(ArrayList<QBChatDialog> result, Bundle params) {
                                                setUpExamRoom(result, params);
                                            }
                                            @Override
                                            public void onError(QBResponseException responseException) { Log.d("getChatDialogs Error: ", responseException.toString()); }
                                        });
                                    }
                                });
                            }

                            @Override
                            public void onError(QBResponseException e) {
                                Log.d( "Update qbuser error? ", e.toString() );
                            }
                        });
                    }

                    @Override
                    public void onError(QBResponseException e) {
                        Log.d("Login Error: ", e.toString());
                    }
                });
            }

            @Override
            public void onError(QBResponseException e) {

            }
        });


    }

    private void setUpExamRoom(ArrayList<QBChatDialog> result, Bundle params) {
        Log.d("test: ", "hey");
        int totalEntries = params.getInt("total_entries");
        Log.d("totalEntries count: ", String.valueOf(totalEntries));
        Log.d("peers count: ", String.valueOf(peers));

        if (totalEntries == 0 && peers.size() > 1) {
            final QBChatDialog dialog = new QBChatDialog();
            dialog.setName(roomName);
            dialog.setType(QBDialogType.GROUP);
            ArrayList<Integer> occupantIdsList = new ArrayList<Integer>();
            for (QBUser user : occupants) {
                occupantIdsList.add(user.getId());
            }
            dialog.setOccupantsIds(occupantIdsList);

            QBRestChatService.createChatDialog(dialog).performAsync(new QBEntityCallback<QBChatDialog>() {
                @Override
                public void onSuccess(final QBChatDialog qbChatDialog, Bundle bundle) {
                    Log.d("action: ", "dialog created");
                    chatSessionStatus = 1;

                    DiscussionHistory discussionHistory = new DiscussionHistory();
                    discussionHistory.setMaxStanzas(0);
                    qbChatDialog.join(discussionHistory, new QBEntityCallback() {
                        @Override
                        public void onSuccess(Object o, Bundle bundle) {
                            Log.d("join object: ", String.valueOf(o));
                            for (QBUser occupant : occupants) {
                                if (!occupant.getId().equals(DataModel.sharedInstance().qbUserParams.getId())) {
                                    Log.d("Send to: ", String.valueOf(occupant.getId()));
                                    sendChatMessage(occupant.getId(), "2", qbChatDialog, DataModel.sharedInstance().qbUserParams.getFullName()+" just logged in.");
                                } else {
                                    Log.d("Don't Send to: ", String.valueOf(occupant.getId()));
                                    Log.d("You: ", String.valueOf(occupant));
                                }
                            }
                        }

                        @Override
                        public void onError(QBResponseException e) {
                            Log.d("qbChatDialog.join error", String.valueOf(e));
                        }
                    });

                }

                @Override
                public void onError(QBResponseException e) {
                    Log.d("Create dialog err", String.valueOf(e));
                }
            });
        } else if (totalEntries == 0 && peers.size() == 1) {
            Log.d("action: ", "You are waiting");
            chatSessionStatus = 0;
        } else {
            Log.d("action: ", "Log into the chat dialog");
            chatSessionStatus = 2;
        }
        //callback.onSuccess();
    }


    private void sendChatMessage(final Integer recipient, final String nType, final QBChatDialog dialog, final String message) {

        try {
            QBChatMessage msg = new QBChatMessage();

            msg.setBody(message);

            Date d = new Date();
            long time = d.getTime();

            msg.setProperty("date_sent", time + "");
            msg.setMarkable(true);
            Log.d("Recipient", String.valueOf(msg));
            msg.setRecipientId(recipient);
            Map<String, String> params = new HashMap();
            params.put("notification_type", nType);
            params.put("_id", String.valueOf(dialog.getDialogId()));
            params.put("name", DataModel.sharedInstance().qbUserParams.getFullName());
            msg.setProperty("extension", params.toString() );
            Log.d("QBChatMessage", String.valueOf(msg));

            dialog.sendMessage(msg);
        } catch (SmackException.NotConnectedException e) {
            //Log.d("QBChatMessage err", String.valueOf(msg));
            e.printStackTrace();
        }
    }

    private void updatePeerList(final QBUser user, final peersCallback callback) {
        QBPagedRequestBuilder pagedRequestBuilder = new QBPagedRequestBuilder();
        pagedRequestBuilder.setPage(1);
        pagedRequestBuilder.setPerPage(50);

        peers.clear();
        occupants.clear();

        QBUsers.getUsersByTags(user.getTags(), pagedRequestBuilder).performAsync( new QBEntityCallback<ArrayList<QBUser>>() {
            @Override
            public void onSuccess(ArrayList<QBUser> users, Bundle params) {
                for(QBUser retrievedUser : users){
                    if (peers.indexOf(retrievedUser) < 1) {
                        peers.add(retrievedUser);
                        Log.d("user", retrievedUser.getFullName()+" was added to peers");
                    }else {
                        Log.d("user", retrievedUser.getFullName()+" was not added to peers");
                    }
                    Date current = new Date(System.currentTimeMillis() - 3600 * 1000);
                    Date lastUpdated = retrievedUser.getUpdatedAt();
                    // DOUBLE CHECK THIS MATH HERE TO DETERMINE IF IT MEANS HOURS
                    if (current.compareTo(lastUpdated) < 0) {
                        occupants.add(retrievedUser);
                        Log.d("user", retrievedUser.getFullName()+" was added to occupants");
                    }else {
                        Log.d("user", retrievedUser.getFullName()+" not added to occupants due to not logging into QB within an hour");
                    }
                }
                callback.onSuccess();
            }

            @Override
            public void onError(QBResponseException errors) {

            }
        });
    }

    public interface peersCallback{
        void onSuccess();
    }



    ConnectionListener connectionListener = new ConnectionListener() {
        @Override
        public void connected(XMPPConnection connection) {

        }

        @Override
        public void authenticated(XMPPConnection xmppConnection, boolean b) {

        }

        @Override
        public void connectionClosed() {

        }

        @Override
        public void connectionClosedOnError(Exception e) {
            // connection closed on error. It will be established soon
        }

        @Override
        public void reconnectingIn(int seconds) {

        }

        @Override
        public void reconnectionSuccessful() {

        }

        @Override
        public void reconnectionFailed(Exception e) {

        }
    };

}
