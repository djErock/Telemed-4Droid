package com.caduceususa.telemed;

/**
 * Created by Erik Grosskurth on 07/07/2017.
 */
class Visit {
    public Integer status;
    public Integer visit_id;
    public String name;
    public String company;
    public String dateOfService;
    public String roomName;

    public Integer getStatus() {
        return status;
    }

    public Integer getVisitId() {
        return visit_id;
    }

    public String getName() { return name; }

    public String getRoom() { return roomName; }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getDateOfService() {
        return dateOfService;
    }
}
