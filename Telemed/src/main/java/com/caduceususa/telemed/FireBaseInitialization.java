package com.caduceususa.telemed;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import static com.google.firebase.messaging.FirebaseMessaging.getInstance;

public class FireBaseInitialization extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fire_base_initialization);

        Button btnClockOut = (Button) findViewById(R.id.btnClockOut);

        // Program Clock Out
        btnClockOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Clock Out: ", "Now");
                getInstance().unsubscribeFromTopic("providers");
                DataModel.sharedInstance().key = CONSTANTS.EMPTY_STRING;
                DataModel.sharedInstance().firebaseAuthenticationToken = CONSTANTS.EMPTY_STRING;
                DataModel.sharedInstance().email = CONSTANTS.EMPTY_STRING;
                DataModel.sharedInstance().password = CONSTANTS.EMPTY_STRING;
                Intent intent = new Intent(FireBaseInitialization.this, Login.class);
                startActivity(intent);
            }
        });
    }
}
