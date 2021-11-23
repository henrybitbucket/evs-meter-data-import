package com.pa.evs;

import lombok.Getter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Getter
public class LocalMapStorage {

    private Map<Long, Long> localMap;
    private Map<Long, Long> onboardingMap;
    private Map<Long, Long> otaMap;
    private Map<Long, Map<String, Object>> cfgMap;

    @PostConstruct
    public void init() {
        localMap = new ConcurrentHashMap<>();
        onboardingMap = new ConcurrentHashMap<>();
        otaMap = new ConcurrentHashMap<>();
        cfgMap = new ConcurrentHashMap<>();
    }
}
