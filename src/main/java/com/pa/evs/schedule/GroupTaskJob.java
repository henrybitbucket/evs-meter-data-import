package com.pa.evs.schedule;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.BooleanUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.enums.CommandEnum;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.model.Group;
import com.pa.evs.model.Users;
import com.pa.evs.sv.EVSPAService;
import com.pa.evs.utils.AppProps;
import com.pa.evs.utils.RSAUtil;
import com.pa.evs.utils.SimpleMap;

@Component
public class GroupTaskJob implements Job {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void execute(JobExecutionContext cntxt) throws JobExecutionException {
        logger.debug("Executing scheduled event at: " + new Date());
        JobDataMap ma = cntxt.getJobDetail().getJobDataMap();
        EVSPAService evsPAService = (EVSPAService) ma.get("EVS_PA_SERVICE");
        Users user = (Users) ma.get("USER");
        Group group = (Group) ma.get("GROUP");
        CommandEnum command = (CommandEnum) ma.get("COMMAND");
        String alias = (String) ma.get("ALIAS");
        String pkPath = (String) ma.get("PK_PATH");
        logger.debug("Group: " + group.getId());
        logger.debug("command: " + command.name());
        try {
            String batchId = UUID.randomUUID().toString();
            evsPAService.createTaskLog(batchId, (Long) ma.get("TASK_ID"), user);
            logger.debug("GroupTaskJob, batchId: {}", batchId);
            List<Long> groupIDs = new ArrayList<>();
            groupIDs.add(group.getId());
            List<CARequestLog> caRequestLogs = evsPAService.findDevicesInGroup(groupIDs);
            if (!caRequestLogs.isEmpty()) {
                caRequestLogs.forEach(ca -> {
                    try {
                        Long mid = evsPAService.nextvalMID();
                        SimpleMap<String, Object> map = SimpleMap.init("id", ca.getUid()).more("cmd", command.name());
                        String sig = BooleanUtils.isTrue(ca.getVendor().getEmptySig()) ? "" : RSAUtil.initSignedRequest(pkPath, new ObjectMapper().writeValueAsString(map));
                        evsPAService.publish(alias + ca.getUid(), SimpleMap.init(
                                "header", SimpleMap.init("uid", ca.getUid()).more("mid", mid).more("gid", ca.getUid()).more("msn", ca.getMsn()).more("sig", sig)
                        ).more(
                                "payload", map
                        ), command.name(), batchId);
                        try { Thread.sleep(1000l); } catch (InterruptedException e) {}
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                });
            }
        } catch (Exception e) {
            logger.error("Error when executing zwave task", e);
        }
    }
}
