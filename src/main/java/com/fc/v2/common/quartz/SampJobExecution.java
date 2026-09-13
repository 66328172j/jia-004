package com.fc.v2.common.quartz;

import com.fc.v2.common.spring.SpringUtils;
import com.fc.v2.service.ITSampJobService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 检验超期扫描定时任务执行类：到点后调用 ITSampJobService.runOverdueScan，
 * 执行日志由 service 落 t_samp_job_log，这里不重复记录
 *
 * @author fuce
 * @date 2026-09-13
 */
@DisallowConcurrentExecution
public class SampJobExecution implements Job {

    private static final Logger log = LoggerFactory.getLogger(SampJobExecution.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String jobId = context.getMergedJobDataMap().getString(SampJobScheduler.JOB_DATA_KEY);
        try {
            SpringUtils.getBean(ITSampJobService.class).runOverdueScan(jobId);
        } catch (Exception e) {
            // service 内部已兜底并落日志，这里只防意外漏出
            log.error("超期扫描任务执行异常，jobId={}", jobId, e);
        }
    }
}
