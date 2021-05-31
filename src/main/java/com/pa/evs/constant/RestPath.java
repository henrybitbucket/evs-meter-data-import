package com.pa.evs.constant;

public interface RestPath {
    String API = "/api";
    
    String APPROVAL_LOCK_LIST_APPROVAL_FOR_LOCK = API + "/approval/lock/list_approval_for_lock.php";
    String APPROVAL_LOCK_ADD_APPROVAL_FOR_LOCK = API + "/approval/lock/add_approval_for_lock.php";
    String APPROVAL_LOCK_LIST_ADMIN_REQUEST = API + "/approval/lock/list_admin_request.php";
    String APPROVAL_LOCK_LIST_REQUEST = API + "/approval/lock/list_request.php";
    String APPROVAL_LOCK_ADD_REQUEST = API + "/approval/lock/add_request.php";
    String APPROVAL_LOCK_APPROVE = API + "/approval/lock/approve.php";
    String APPROVAL_LOCK_REJECT = API + "/approval/lock/reject.php";
    
    String LOCK_BLUETOOTH_LIST_LOCK = API + "/lock/bluetooth/list_lock.php";
    String LOCK_BLUETOOTH_OPEN_LOCK = API + "/lock/bluetooth/open_lock.php";
    
    String USER_USERNAME = API + "/user/username.php";
    String USER_USER_DETAILS = API + "/user/user_details.php";

    String COMPANIES = API + "/companies";
    String COMPANY = API + "/company";
    
    String KEYS = API + "/keys";
    String KEY = API + "/key";
    
    String KEY_GROUPS = API + "/keyGroups";
    String KEY_GROUP = API + "/keyGroup";
    
    String LOCKS = API + "/locks";
    String LOCK = API + "/lock";
    
    String LOCK_GROUPS = API + "/lockGroups";
    String LOCK_GROUP = API + "/lockGroup";
    
    String LOCK_REQUESTS_NORMAL = API + "/lockRequestsNormal";
    String LOCK_REQUEST_NORMAL = API + "/lockRequestNormal";

    String LOCK_REQUESTS_SPECIAL = API + "/lockRequestsSpecial";
    String LOCK_REQUEST_SPECIAL = API + "/lockRequestSpecial";

    String LOCK_REQUESTS_SECOND = API + "/lockRequestsSecond";
    String LOCK_REQUEST_SECOND = API + "/lockRequestSecond";

    String PAYMENTS = API + "/payments";
    String PAYMENT = API + "/payment";
    
    String SETTINGS = API + "/settings";
    String SETTING = API + "/setting";
    
    String HISTORY_LOGS = API + "/historyLogs";
    String HISTORY_LOG = API + "/historyLog";
    
    String PERMITS_TO_ENTER = API + "/permitsToEnter";
    String PERMIT_TO_ENTER = API + "/permitToEnter";
    
    String QR_CODES = API + "/qrCodes";
    String QR_CODE = API + "/qrCode";
    
    String BEACONS = API + "/beacons";
    String BEACON = API + "/beacon";
    
    String USER_LOCATIONS = API + "/userLocations";
    String USER_LOCATION = API + "/userLocation";
    
    String FIRE_ALARM_RESPONSES = API + "fireAlarmRespones";
    String FIRE_ALARM_RESPONSE = API + "fireAlarmRespone";
    
    String FIRE_ALARMS = API + "fireAlarms";
    String FIRE_ALARM = API + "fireAlarm";
    
	String LOGIN = API + "/login";
	String LOGIN1 = API + "/v1/login";
	String WHOAMI = API + "/user/me";
	String USERS = API + "/users";
	String USER = API + "/user";
	String WHOAMI1 = API + "/whoami";
}
