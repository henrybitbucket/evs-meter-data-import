package com.pa.evs.enums;

import lombok.Getter;

@Getter
public enum CommandEnum {
    
    CFG(false),
    INF(true);

    private final boolean visible;
    CommandEnum(boolean visible) {
        this.visible = visible;
    }

}
