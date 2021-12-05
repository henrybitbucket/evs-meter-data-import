package com.pa.evs.schedule;

import org.quartz.JobDetail;
import org.quartz.Trigger;

public interface ISchedule {

	public Trigger getTrigger() throws Exception;

	public JobDetail getJobDetail();
}
