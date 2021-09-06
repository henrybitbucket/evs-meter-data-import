package com.pa.evs.constant;

public interface Message {
    String USERNAME_OR_PASSWORD_NOT_NULL = "Username and password is required";
    String EXIST_EMAIL = "This email is already in use";
    String INVALID_USERNAME_PASSWORD = "Username or password not correct";
    String USER_IS_DISABLE = "User is disable";
    String NOT_FOUND_WF = "workflow not  found";
    String INVALID_NODE_TYPE = "Invalid node type";
    String DELETE_WF_SUCCESS = "Delete workflow successfully";
    String SLACK_CONNECT_ERROR = "Slack connection error";
    String SLACK_BOT_ALREADY = "Slack bot already enable";
    String YES_NO_MESSAGE = "Please say yes or no. Thanks";
    String FIRMWARE_NOT_FOUND = "Firmware not found!";
    String MSN_WAS_ASSIGNED = "MSN was assigned for another UID";
}
