package com.pa.evs.constant;

import java.util.Arrays;
import java.util.List;

public interface ValueConstant {
    boolean TRUE = true;
    boolean FALSE = false;
    String SUCCESS = "Success";
    String GLOBAL_CONTEXT = "GLOBAL_CONTEXT";
    String PROMPT = "PROMPT";
    String STATEMENT = "STATEMENT";
    String BRANCH = "BRANCH";
    String ACTION = "ACTION";
    String SWITCH = "SWITCH";
    String API_CALL = "API_CALL";
    String YES_NO = "YES_NO_PROMPT";
    String START = "START";
    String STOP = "STOP";
    String START_NODE_ID = "start";
    String SLACK_TYPE = "SLACK_TYPE";
    String LUIS = "LUIS";
    String CARD = "CARD";
    String EXT_COMPONENT = "EXT_COMPONENT";
    List<String> NODE_TYPES = Arrays.asList(SUCCESS, GLOBAL_CONTEXT, PROMPT, STATEMENT, BRANCH, ACTION, SWITCH, API_CALL, YES_NO, START, STOP, LUIS, CARD, EXT_COMPONENT);
    List<String> PROVIDER_TYPES = Arrays.asList(SLACK_TYPE);
    String YES = "yes";
    String NO = "no";
}
