package com.pa.evs.schedule;

import com.pa.evs.enums.JasperFormat;
import com.pa.evs.model.ReportTask;
import com.pa.evs.repository.ReportFileRepository;
import com.pa.evs.repository.ReportTaskRepository;
import com.pa.evs.sv.EVSPAService;
import com.pa.evs.sv.ReportService;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import java.util.Calendar;

import javax.persistence.EntityManager;

public class ReportTaskSchedule implements ISchedule {

    public static final String REPORT_NAME = "ReportOperationSchedule";
    private ReportTask task;
    private String parameter;
    private JasperFormat format;
    private EVSPAService evsPAService;
    private String alias;
    private String pkPath;
    private ReportService reportService;    
    private ReportTaskRepository reportTaskRepository;
    private ReportFileRepository reportFileRepository;
    private EntityManager em;

    public ReportTaskSchedule(ReportTask task, ReportFileRepository reportFileRepository, EntityManager em, String parameter, JasperFormat format, ReportService reportService,
    		ReportTaskRepository reportTaskRepository, EVSPAService evsPAService, String alias, String pkPath) {
        this.task = task;
        this.reportFileRepository = reportFileRepository;
        this.em = em;
        this.parameter = parameter;
        this.format = format;
        this.reportService = reportService;
        this.reportTaskRepository = reportTaskRepository;
        this.evsPAService = evsPAService;
        this.alias = alias;
        this.pkPath = pkPath;
    }

    @Override
    public Trigger getTrigger() throws Exception {
        if (ReportTask.Type.ONE_TIME == task.getType()) {
            return TriggerBuilder.newTrigger()
                    .withIdentity("report_schedule_id_" + task.getId(), REPORT_NAME)
                    .startAt(task.getStartTime())
                    .forJob("report_schedule_job_" + task.getId(), REPORT_NAME)
                    .build();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(task.getStartTime());
        String cron = null;
        if (ReportTask.Type.WEEKLY == task.getType()) {
        	cron = "0 " + cal.get(Calendar.MINUTE) + " " + cal.get(Calendar.HOUR_OF_DAY) + " ? * " + cal.get(Calendar.DAY_OF_WEEK) + " *";
        } else if (ReportTask.Type.MONTHLY == task.getType()) {
        	cron = "0 " + cal.get(Calendar.MINUTE) + " " + cal.get(Calendar.HOUR_OF_DAY) + " " + cal.get(Calendar.DAY_OF_MONTH) + " * ? *";
        } else {
        	cron = "0 " + cal.get(Calendar.MINUTE) + " " + cal.get(Calendar.HOUR_OF_DAY) + " * * ? *";
		}
        return TriggerBuilder.newTrigger()
                .withIdentity("report_schedule_id_" + task.getId(), REPORT_NAME)
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                .forJob("report_schedule_job_" + task.getId(), REPORT_NAME)
                .build();
    }

    @Override
    public JobDetail getJobDetail() {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("PARAMETER", parameter);
        jobDataMap.put("ENTITY_MANAGER", em);
        jobDataMap.put("FORMAT", format);
        jobDataMap.put("REPORT", task.getReport());
        jobDataMap.put("REPORT_TASK_ID", task.getId());
        jobDataMap.put("EVS_PA_SERVICE", evsPAService);
        jobDataMap.put("REPORT_SERVICE", reportService);
        jobDataMap.put("REPORT_TASK_REPOSITORY", reportTaskRepository);
        jobDataMap.put("REPORT_FILE_REPOSITORY", reportFileRepository);
        jobDataMap.put("ALIAS", alias);
        jobDataMap.put("PK_PATH", pkPath);
        return JobBuilder.newJob(ReportTaskJob.class)
                .withIdentity("report_schedule_job_" + task.getId(), REPORT_NAME)
                .usingJobData(jobDataMap)
                .build();
    }
}
