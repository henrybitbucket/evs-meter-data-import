package com.pa.evs.schedule;

import com.pa.evs.model.GroupTask;
import com.pa.evs.model.Users;
import com.pa.evs.sv.EVSPAService;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import java.util.Calendar;

public class GroupTaskSchedule implements ISchedule {

    public static final String GROUP_NAME = "GroupOperationSchedule";
    
    public GroupTaskSchedule(GroupTask task, Users user, EVSPAService evsPAService, String alias, String pkPath) {
        this.task = task;
        this.user = user;
        this.evsPAService = evsPAService;
        this.alias = alias;
        this.pkPath = pkPath;
    }

    @Override
    public Trigger getTrigger() throws Exception {
        if (GroupTask.Type.ONE_TIME == task.getType()) {
            return TriggerBuilder.newTrigger()
                    .withIdentity("group_schedule_id_" + task.getId(), GROUP_NAME)
                    .startAt(task.getStartTime())
                    .forJob("group_schedule_job_" + task.getId(), GROUP_NAME)
                    .build();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(task.getStartTime());
        String cron = null;
        if (GroupTask.Type.WEEKLY == task.getType()) {
        	cron = "0 " + cal.get(Calendar.MINUTE) + " " + cal.get(Calendar.HOUR_OF_DAY) + " ? * " + cal.get(Calendar.DAY_OF_WEEK) + " *";
        } else if (GroupTask.Type.MONTHLY == task.getType()) {
        	cron = "0 " + cal.get(Calendar.MINUTE) + " " + cal.get(Calendar.HOUR_OF_DAY) + " " + cal.get(Calendar.DAY_OF_MONTH) + " * ? *";
        } else {
        	cron = "0 " + cal.get(Calendar.MINUTE) + " " + cal.get(Calendar.HOUR_OF_DAY) + " * * ? *";
		}
        return TriggerBuilder.newTrigger()
                .withIdentity("group_schedule_id_" + task.getId(), GROUP_NAME)
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                .forJob("group_schedule_job_" + task.getId(), GROUP_NAME)
                .build();
    }

    @Override
    public JobDetail getJobDetail() {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("COMMAND", task.getCommand());
        jobDataMap.put("GROUP", task.getGroup());
        jobDataMap.put("EVS_PA_SERVICE", evsPAService);
        jobDataMap.put("ALIAS", alias);
        jobDataMap.put("PK_PATH", pkPath);
        jobDataMap.put("TASK_ID", task.getId());
        jobDataMap.put("USER", user);
        return JobBuilder.newJob(GroupTaskJob.class)
                .withIdentity("group_schedule_job_" + task.getId(), GROUP_NAME)
                .usingJobData(jobDataMap)
                .build();
    }

    private GroupTask task;
    private EVSPAService evsPAService;
    private String alias;
    private String pkPath;
    private Users user;
}
