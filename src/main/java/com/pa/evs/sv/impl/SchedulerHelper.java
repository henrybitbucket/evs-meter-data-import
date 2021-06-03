package com.pa.evs.sv.impl;

import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 
 * SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
    Scheduler scheduler = schedFact.getScheduler();
    scheduler.start();
    
    JobBuilder jobBuilder = JobBuilder.newJob(QuartzJob.class);
    JobDataMap data = new JobDataMap();
    data.put("lol", this);
     
    JobDetail jobDetail = jobBuilder 
            .usingJobData(data)
            .withIdentity("myJob", "group1")
            .build();
     
     
    Trigger trigger = TriggerBuilder.newTrigger()
    .withSchedule(CronScheduleBuilder.cronSchedule("0/1 * * ? * * *"))      
    .build();
     
    // Tell quartz to schedule the job using our trigger
    scheduler.scheduleJob(jobDetail, trigger);
    scheduler.shutdown();
    
 * @author thanh
 *
 */
public class SchedulerHelper {	
	
	private static Scheduler scheduler = null;
	
	private static final Logger log = LoggerFactory.getLogger(Scheduler.class);

	public static void scheduleJob(String cronExpression, Job job, String triggerKey) {
		try {
			scheduleJob(QuartzJob.class, cronExpression, job, triggerKey);
			job.start();	
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public static void unscheduleJob(String triggerKey) throws SchedulerException {
		scheduler.unscheduleJob(new TriggerKey(triggerKey));
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void scheduleJob(Class jobClazz, String cronExpression, Job job, String triggerKey) throws SchedulerException {
		
		log.info("{}{}", "--> Schedule job ", cronExpression);
		JobBuilder jobBuilder = JobBuilder.newJob(jobClazz);
		JobDataMap data = new JobDataMap();
	    data.put("job", job);
	    JobDetail jobDetail = jobBuilder.usingJobData(data).build();
	    Trigger trigger = TriggerBuilder.newTrigger()
	    		.withIdentity(triggerKey)
	    	    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression).inTimeZone(TimeZone.getDefault()))      
	    	    .build();
		scheduler.scheduleJob(jobDetail, trigger);
	}
	
	public static class QuartzJob implements org.quartz.Job {

		private static Map<String, Object> currentJobs = new ConcurrentHashMap<>();
		private static Map<String, Lock> locks = new ConcurrentHashMap<>();
		private static final Logger log = LoggerFactory.getLogger(QuartzJob.class);
		
		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {

			String key = context.getTrigger().getKey().getName();
			
			if (currentJobs.containsKey(key)) {
				return;
			}
			Lock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
			lock.lock();
			
			/** log.info("{}({}) {}", "Starting job ", key, new Date()); */
			try {
				currentJobs.put(key, 1);
				Job job = (Job) context.getJobDetail().getJobDataMap().get("job");
				job.start();	
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			} finally {
				currentJobs.remove(key);
				lock.unlock();
			}
			
			/** log.info("{}({}) {}", "End job ", key, new Date()); */
		}

	}
	
	public static interface Job {
		void start();
	}
	
	static {
		try {
			SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
	        scheduler = schedFact.getScheduler();
	        scheduler.start();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	
	public static void main(String[] args) {
		SchedulerHelper.scheduleJob("0/1 * * * * ? *", () -> {
			try {
				Thread.sleep(10000l);
			} catch (InterruptedException e) {/**/}
			System.out.println(1111);
		}, "Test");
	}
}
