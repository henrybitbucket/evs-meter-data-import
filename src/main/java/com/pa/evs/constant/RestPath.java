package com.pa.evs.constant;

public interface RestPath {
    String API = "/api";

    String USER_USERNAME = API + "/user/username.php";
    String USER_USER_DETAILS = API + "/user/user_details.php";
    
	String LOGIN = API + "/login";
	String LOGIN1 = API + "/v1/login";
	String WHOAMI = API + "/user/me";
	String USERS = API + "/users";
	String USER = API + "/user";
	String USERPERMISSION = API + "/user/permission";
	String USERROLELOGGING = API + "/user/role/logging";
	String USER_PROJECT_LOGGING = API + "/user/project/logging";
	String USERROLE = API + "/user/role";
	String USER_PROJECT = API + "/user/project";
	String USERPLATFORM = API + "/user/pf";
	String USERGROUP = API + "/user/group";
	String EACHUSERPERMISSION = API + "/each-user/permission";
	String UPDATEROLE = API + "/update/role";
	String UPDATEGROUP = API + "/update/group";
	String UPDATEPERMISSON = API + "/update/permission";
	String UPDATEPROJECT = API + "/update/project";
	String WHOAMI1 = API + "/whoami";
	String CREATE_NEW_USER = API + "/user/save";
	
	String GET_CA_REQUEST_LOG = API + "/ca-request-logs";
	String CA_REQUEST_LOG = API + "/ca-request-log";
	String CA_REQUEST_LOG_GET_CIDS = API + "/ca-request-log/cids";
	String CA_REQUEST_LOG_POST_SEND_COMMAND = API + "/ca-request-log/command";
	String CA_REQUEST_LOG_BATCH_COUPLE_DEVICES = API + "/ca-request-log/batch-couple";

	String RLS_STATUS_GET_LOGS = API + "/rls-command-logs";
	
	String CA_CAL_DASHBOARD = API + "/cal-dashboard";
	
	String CA_ALARM_MARK_VIEW_ALL = API + "/alarm-mark-view-all";

    String CA_REQUEST_LOG_GET_COUNT_DEVICES = API + "/ca-request-log/count-devices";
    String GET_DASHBOARD = API + "/dashboard";
}
