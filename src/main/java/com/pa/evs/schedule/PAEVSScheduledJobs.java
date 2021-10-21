package com.pa.evs.schedule;

import com.pa.evs.sv.CaRequestLogService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class PAEVSScheduledJobs {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(PAEVSScheduledJobs.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    final CaRequestLogService caRequestLogService;

    public PAEVSScheduledJobs(CaRequestLogService caRequestLogService) {
        this.caRequestLogService = caRequestLogService;
    }

    @Scheduled(fixedRate = 5*60*1000)
    public void checkDevicesOffline() {
        LOG.debug("starting CheckDevicesOfflineThe at: {}", dateFormat.format(new Date()));
        caRequestLogService.checkDevicesOffline();
        LOG.debug("end CheckDevicesOfflineThe at: {}", dateFormat.format(new Date()));
    }

}
