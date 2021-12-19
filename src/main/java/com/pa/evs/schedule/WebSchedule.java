package com.pa.evs.schedule;

import com.pa.evs.enums.JasperFormat;
import com.pa.evs.model.GroupTask;
import com.pa.evs.model.ReportTask;
import com.pa.evs.repository.GroupTaskRepository;
import com.pa.evs.repository.ReportFileRepository;
import com.pa.evs.repository.ReportTaskRepository;
import com.pa.evs.sv.EVSPAService;
import com.pa.evs.sv.ReportService;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.persistence.EntityManager;

import java.util.Date;
import java.util.List;

@Component
public class WebSchedule {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EVSPAService evsPAService;

    @Autowired
    private GroupTaskRepository groupTaskRepository;
    
    @Autowired
    private ReportTaskRepository reportTaskRepository;
    
    @Autowired
    private ReportFileRepository reportFileRepository;
    
    @Autowired
    EntityManager em;
    
    @Autowired
    private ReportService reportService;

    @Value("${evs.pa.mqtt.publish.topic.alias}") private String alias;

    @Value("${evs.pa.privatekey.path}")
    private String pkPath;

    private SchedulerFactory schedulerFactory;

    @PostConstruct
    public void init() {
        schedulerFactory = new StdSchedulerFactory();
        restart();
        restartReportSchedule();
    }

    public void restart() {
        try {
            logger.debug("All scheduled tasks");
            logger.debug("Getting list of scheduled tasks");
            List<GroupTask> groupTasks = groupTaskRepository.findAll();
            if(!groupTasks.isEmpty()) {
                groupTasks.forEach(task -> {
                    if (!(GroupTask.Type.ONE_TIME == task.getType() && task.getStartTime().compareTo(new Date()) < 0)) {
                        this.addSchedule(new GroupTaskSchedule(task, evsPAService, alias, pkPath));
                    }
                });
            }
            logger.trace("Scheduled reports list gotten successfully");
        } catch (Exception e) {
            logger.error("Silent catch for restart schedule: ", e);
        }
    }
    
    public void restartReportSchedule() {
        try {
            logger.debug("All reports scheduled");
            logger.debug("Getting list of reports scheduled tasks");
            String parameter = "";
            JasperFormat format = null;
            List<ReportTask> reportTask = reportTaskRepository.findAll();
            if(!reportTask.isEmpty()) {
            	reportTask.forEach(task -> {
                    if (!(ReportTask.Type.ONE_TIME == task.getType() && task.getStartTime().compareTo(new Date()) < 0)) {
                        this.addSchedule(new ReportTaskSchedule(task, reportFileRepository, em, parameter, format, reportService, reportTaskRepository, evsPAService, alias, pkPath));
                    }
                });
            }
            logger.trace("Scheduled reports list gotten successfully");
        } catch (Exception e) {
            logger.error("Silent catch for restart schedule: ", e);
        }
    }

    public void addSchedule(ISchedule schedule) {
        try {
            Trigger trigger = schedule.getTrigger();
            if (trigger != null) {
                Scheduler scheduler = schedulerFactory.getScheduler();
                if (!scheduler.isStarted()) {
                    scheduler.start();
                }
                if (scheduler.getTrigger(trigger.getKey()) == null) {
                    JobDetail jobDetail = schedule.getJobDetail();
                    if (jobDetail != null) {
                        jobDetail.getJobBuilder()
                                .withIdentity(trigger.getJobKey())
                                .build();
                        scheduler.scheduleJob(jobDetail, trigger);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Silent catch for " + schedule.getClass().getCanonicalName() + ": ", e);
        }
    }


    public void removeSchedule(ISchedule schedule) {
        try {
            Trigger trigger = schedule.getTrigger();
            if (trigger != null) {
                Scheduler scheduler = schedulerFactory.getScheduler();
                scheduler.deleteJob(trigger.getJobKey());
                scheduler.pauseTrigger(trigger.getKey());
            }
        } catch (Exception e) {
            logger.error("Silent catch: ", e);
        }
    }

    @PreDestroy
    public void destroy() {
        try {
            logger.debug("All scheduled reports cancelling");
            Scheduler scheduler = schedulerFactory.getScheduler();
            if (!scheduler.isShutdown()) {
                scheduler.shutdown();
            }
            logger.info("Shutdown complete");
        } catch (Exception e) {
            logger.error("Silent catch: ", e);
        }
    }
}
