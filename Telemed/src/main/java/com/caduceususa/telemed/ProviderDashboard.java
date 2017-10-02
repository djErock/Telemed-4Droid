package com.caduceususa.telemed;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.os.Handler;

import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.helper.StringifyArrayList;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

public class ProviderDashboard extends AppCompatActivity {

    ListView visitsListView;
    Boolean isRefreshing;
    ArrayList<Visit> arrayOfVisits = new ArrayList<Visit>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.title_activity_provider_dashboard);
        setContentView(R.layout.activity_provider_dashboard);
        isRefreshing = false;
        populateVisitListView();

        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refreshVisitList);
        swipeRefreshLayout.setColorSchemeResources(R.color.refresh,R.color.refresh1,R.color.refresh2);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(true);
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("Are we Refreshing", isRefreshing.toString());
                        swipeRefreshLayout.setRefreshing(false);
                        if (!isRefreshing) {
                            Log.d("Lets", "refresh");
                            populateVisitListView();
                        }
                    }
                },3000);
            }
        });
    }

    private void populateVisitListView() {
        isRefreshing = true;
        arrayOfVisits.clear();
        visitsListView = (ListView) findViewById(R.id.visitsListView);
        visitsListView.setAdapter(null);

        Map<String, String> params = new HashMap();
        params.put("sKey", DataModel.sharedInstance().key);
        final JSONObject parameters = new JSONObject(params);
        Log.d("parameters: ", parameters.toString());
        DataModel.sharedInstance().volleyJsonObjectPost(
            true,
            ProviderDashboard.this,
            "https://telemed.caduceususa.com/ws/util.asmx/returnActiveVisits",
            parameters,
            new DataModel.ServerCallback() {
                public void onSuccess(JSONObject response) {
                    try {
                        final JSONArray visitData = response.getJSONArray("d");
                        Log.d("visitData", visitData.toString());
                        Integer lengthOfStr = visitData.length();
                        Log.d("visitData length", lengthOfStr.toString());
                        String company_id = "";
                        for(int i=0; i <= visitData.length() - 1; i++) {
                            Integer count = i;
                            Log.d("countdown", count.toString());
                            JSONObject obj = null;
                            try {
                                obj = visitData.getJSONObject(i);
                                Visit visitRow = new Visit();
                                if ( obj.getBoolean("is_occupied_sm") ) {
                                    // 1 == THE PATIENT IS IN A ROOM and the VISIT IS ACTIVE >>> CHANGE TO GREEN == 1
                                    visitRow.status = 1;
                                }else {
                                    // 0 == THE PATIENT IS NOT IN THE ROOM and VISIT IS INACTIVE >>> CHANGE TO RED == 0
                                    visitRow.status = 0;
                                }
                                visitRow.visit_id = Integer.parseInt(obj.getString("visit_id"));
                                visitRow.name = obj.getString("first_name") + " " + obj.getString("last_name");
                                visitRow.company = obj.getString("company_name");
                                visitRow.dateOfService = obj.getString("date_of_service");
                                visitRow.roomName = obj.getString("room_name");
                                arrayOfVisits.add(visitRow);
                            } catch (JSONException e) {
                                Log.d("WEB SERVICE DATA ERROR", e.toString());
                                e.printStackTrace();
                            }
                        }
                        visitsListView.setAdapter(new VisitAdapter(ProviderDashboard.this, R.layout.visit_item, arrayOfVisits));
                        visitsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                                final Context ctx = getApplicationContext();

                                Map<String, String> params = new HashMap();
                                params.put("sKey", DataModel.sharedInstance().key);
                                params.put("iVisitId", Integer.toString(arrayOfVisits.get(position).getVisitId()));
                                final JSONObject parameters = new JSONObject(params);
                                Log.d("parameters: ", parameters.toString());
                                DataModel.sharedInstance().volleyJsonObjectPost(false, ctx, "https://telemed.caduceususa.com/ws/Telemed.asmx/acceptExam", parameters, new DataModel.ServerCallback() {
                                    public void onSuccess(JSONObject response) {
                                        try {
                                            final JSONObject providerStatus = response.getJSONObject("d");
                                            if ( providerStatus.getBoolean("status") ) {
                                                // Someone is already in the exam room with the patient
                                                DataModel.sharedInstance().AlertDialogBuilder(ProviderDashboard.this,  "Sorry", "A Provider has already accepted this exam.");
                                                populateVisitListView();
                                            }else {

                                                /////////////////////////////////////////////////////////////////////////////
                                                // THIS CODE IS TO TAKE PROVIDER TO WEB APP
                                                /////////////////////////////////////////////////////////////////////////////


/*

                                                Map<String, String> params = new HashMap();
                                                params.put("sUser", DataModel.sharedInstance().email);
                                                params.put("sPass", DataModel.sharedInstance().password);
                                                final JSONObject parameters = new JSONObject(params);
                                                Log.d("parameters: ", parameters.toString());
                                                DataModel.sharedInstance().volleyJsonObjectPost(false, ctx, "https://telemed.caduceususa.com/ws/telemed.asmx/telemedLogin", parameters, new DataModel.ServerCallback() {
                                                    public void onSuccess(JSONObject response) {
                                                        try {
                                                            final JSONObject loginData = response.getJSONObject("d");
                                                            Log.d("SESSION KEY: ", loginData.getString("key"));
                                                            DataModel.sharedInstance().key = loginData.getString("key");
                                                            String urlString = "https://telemed.caduceususa.com/index.html?" + DataModel.sharedInstance().key + "," + Integer.toString(arrayOfVisits.get(position).getVisitId());

                                                            Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
                                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                            intent.setPackage("com.android.chrome");
                                                            try {
                                                                ctx.startActivity(intent);
                                                            } catch (ActivityNotFoundException ex) {
                                                                // Chrome browser presumably not installed so allow user to choose instead
                                                                intent.setPackage(null);
                                                                ctx.startActivity(intent);
                                                            }
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                });

*/

                                                /////////////////////////////////////////////////////////////////////////////
                                                // THIS CODE IS TO TAKE PROVIDER INTO THE REST OF THE APP
                                                ///////////////////////////////////////////////////////////////////////////
//

                                                Log.d("Bundle Extras: ", parameters.toString());
                                                Intent intent = new Intent(ctx, TelemedExamRoom.class);
                                                intent.putExtra("roomName", arrayOfVisits.get(position).getRoom());
                                                intent.putExtra("visitId", arrayOfVisits.get(position).getVisitId());
                                                startActivity(intent);

//
                                                //////////////////////////////////////////////////////////////////////////////
                                            }
                                        }catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            }

                        });
                        isRefreshing = false;
                    } catch (JSONException e) {
                        e.printStackTrace();

                    }
                }
            }
        );
    }

    public class VisitAdapter extends ArrayAdapter<Visit> {

        List list = new ArrayList();

        public VisitAdapter(Activity self, int index, ArrayList<Visit> data) {
            super(self, android.R.layout.simple_list_item_1, data);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            VisitViewHolder holder;

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.visit_item, null);

                holder = new VisitViewHolder(convertView);
                convertView.setTag(holder);
            }else {
                holder = (VisitViewHolder)convertView.getTag();
            }
            holder.populateFrom(arrayOfVisits.get(position));

            return(convertView);
        }


    }

}
