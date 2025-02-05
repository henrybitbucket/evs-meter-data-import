package com.pa.evs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.Getter;

@Component
@Getter
public class LocalMapStorage {

	private Map<String, String> uidMsnMap;
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
        uidMsnMap = new ConcurrentHashMap<>();
    }
}
