package com.pa.evs.enums;

public enum CommandEnum {
    
    CFG(false),
    INF(true);

    private final boolean visible;
    CommandEnum(boolean visible) {
        this.visible = visible;
    }

}
