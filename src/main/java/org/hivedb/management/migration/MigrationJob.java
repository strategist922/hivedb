package org.hivedb.management.migration;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Moves a record from one node of the hive to another.
 * @author Britt Crawford (bcrawford@cafepress.com)
 *
 */
public class MigrationJob implements Job {
	public static final String MIGRATION_KEY = "Migration";
	
	@SuppressWarnings("unchecked")
	public void execute(JobExecutionContext context) throws JobExecutionException {
		JobDataMap map = context.getMergedJobDataMap();
		Migration migration = (Migration) map.get(MIGRATION_KEY);
		Mover mover = MoverFactory.getMover();
		try {
			mover.move(migration);
		} catch (MigrationException e) {
			throw new JobExecutionException(e);
		}
	}
	
	private static JobDataMap buildJobDataMap(JobDataMap map, Migration migration) {
		map.put(MIGRATION_KEY, migration);
		return map;
	}
	
	public static JobDetail createDetail(String name, String jobGroup, Migration migration) {
		JobDetail detail = new JobDetail(name, jobGroup, MigrationJob.class);
		detail.setJobDataMap(buildJobDataMap(detail.getJobDataMap(), migration));
		detail.setDurability(false);
		detail.setVolatility(false);
		detail.setRequestsRecovery(true);
		return detail;
	}
}