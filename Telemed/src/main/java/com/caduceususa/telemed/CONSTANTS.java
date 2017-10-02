package com.caduceususa.telemed;

/**
 * Created by Erik Grosskurth on 06/16/2017.
 */



public class CONSTANTS {

    //public static final String ACCEPT_EXAM = "Exam Accepted";
    //public static final String DISMISS_EXAM = "Exam Dismissed";
    public static final String EMPTY_STRING = "";
    public static final String LOGIN_WS_URL  = DataModel.sharedInstance().Caduceus_API + "/telemed.asmx/telemedLogin";
    public static final String GETUSER_WS_URL = DataModel.sharedInstance().Caduceus_API + "/telemed.asmx/getUser";
    public static final String UPDATECREDS_WS_URL = DataModel.sharedInstance().Caduceus_API + "/telemed.asmx/updateCreds";
    public static final String UPDATEQBUSER_WS_URL = DataModel.sharedInstance().Caduceus_API + "/telemed.asmx/updateQbUser";

}
