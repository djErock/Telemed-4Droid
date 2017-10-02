package com.caduceususa.telemed;

import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

/**
 * Created by Erik Grosskurth on 07/07/2017.
 */
class VisitViewHolder {
    public View status;
    public TextView name;
    public TextView company;
    public TextView dateOfService;

    VisitViewHolder(View visit_item) {
        status = (View) visit_item.findViewById(R.id.statusIndicator);
        name = (TextView) visit_item.findViewById(R.id.PatientNameTextView);
        company = (TextView) visit_item.findViewById(R.id.CompanyNameTextView);
        dateOfService = (TextView) visit_item.findViewById(R.id.DateOfServiceTextView);
    }

    void populateFrom(Visit r) {
        if (r.status == 1) {
            status.setBackgroundColor(Color.parseColor("#00FF00")); // GREEN = 1
        }else {
            status.setBackgroundColor(Color.parseColor("#E32004")); // RED = 0
        }

        name.setText(r.name);
        company.setText(r.company);
        dateOfService.setText(r.dateOfService);
    }
}
