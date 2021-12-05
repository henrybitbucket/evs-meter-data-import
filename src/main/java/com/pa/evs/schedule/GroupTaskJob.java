package com.pa.evs.schedule;

import com.pa.evs.enums.CommandEnum;
import com.pa.evs.model.Group;
import com.pa.evs.sv.EVSPAService;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class GroupTaskJob implements Job {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void execute(JobExecutionContext cntxt) throws JobExecutionException {
        logger.debug("Executing scheduled event at: " + new Date());
        JobDataMap ma = cntxt.getJobDetail().getJobDataMap();
        EVSPAService service = (EVSPAService) ma.get("EVS_PA_SERVICE");
        Group group = (Group) ma.get("GROUP");
        CommandEnum command = (CommandEnum) ma.get("COMMAND");
        logger.debug("Group: " + group.getId());
        logger.debug("command: " + command.name());
        try {
            //TO-DO
            //query all device belong to group
            //send command to all devive
        } catch (Exception e) {
            logger.error("Error when executing zwave task", e);
        }

    }
}
