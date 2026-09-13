package com.fc.v2.common.quartz;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TSampJob;
import com.fc.v2.service.ITSampJobService;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 检验超期扫描任务调度器：启动时把 t_samp_job 里的任务注册进 Quartz，
 * 停用的任务注册后即暂停，启停在页面上操作后同步暂停/恢复
 *
 * @author fuce
 * @date 2026-09-13
 */
@Component
public class SampJobScheduler {

    private static final Logger log = LoggerFactory.getLogger(SampJobScheduler.class);

    /** 任务组 */
    public static final String JOB_GROUP = "SAMP";

    /** JobDataMap 中任务ID的key */
    public static final String JOB_DATA_KEY = "sampJobId";

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private ITSampJobService sampJobService;

    /**
     * 容器初始化时把库里的任务全部注册进调度器
     */
    @PostConstruct
    public void init() {
        List<TSampJob> jobs = sampJobService.selectTSampJobList(new QueryWrapper<TSampJob>());
        for (TSampJob job : jobs) {
            try {
                createSchedule(job);
            } catch (Exception e) {
                // 单个任务注册失败（如 cron 写错）不影响其他任务
                log.error("注册超期扫描任务失败，jobId={}", job.getId(), e);
            }
        }
    }

    /**
     * 创建（或重建）一个任务的调度；停用状态的任务建好后立即暂停
     *
     * @param job 任务配置
     * @throws SchedulerException 调度异常
     */
    public void createSchedule(TSampJob job) throws SchedulerException {
        if (checkJobExists(job)) {
            deleteJob(job);
        }
        JobDetail jobDetail = JobBuilder.newJob(SampJobExecution.class)
                .withIdentity(getJobKey(job))
                .build();
        CronTrigger cronTrigger = TriggerBuilder.newTrigger()
                .withIdentity(TriggerKey.triggerKey(job.getId(), JOB_GROUP))
                .withSchedule(CronScheduleBuilder.cronSchedule(job.getCron())
                        .withMisfireHandlingInstructionDoNothing())
                .build();
        jobDetail.getJobDataMap().put(JOB_DATA_KEY, job.getId());
        scheduler.scheduleJob(jobDetail, cronTrigger);
        if (job.getStatus() != null && job.getStatus() == 1) {
            pauseJob(job);
        }
    }

    /**
     * 暂停任务（停用）
     *
     * @param job 任务配置
     */
    public void pauseJob(TSampJob job) {
        try {
            scheduler.pauseJob(getJobKey(job));
        } catch (SchedulerException e) {
            log.error("暂停超期扫描任务异常，jobId={}", job.getId(), e);
        }
    }

    /**
     * 恢复任务（启用）；调度器里还没有（如启动时注册失败）则补建
     *
     * @param job 任务配置
     */
    public void resumeJob(TSampJob job) {
        try {
            if (!checkJobExists(job)) {
                createSchedule(job);
                return;
            }
            scheduler.resumeJob(getJobKey(job));
        } catch (SchedulerException e) {
            log.error("恢复超期扫描任务异常，jobId={}", job.getId(), e);
        }
    }

    /**
     * 删除任务的调度
     *
     * @param job 任务配置
     */
    public void deleteJob(TSampJob job) {
        try {
            scheduler.deleteJob(getJobKey(job));
        } catch (SchedulerException e) {
            log.error("删除超期扫描任务调度异常，jobId={}", job.getId(), e);
        }
    }

    /**
     * 判断任务是否已注册进调度器
     *
     * @param job 任务配置
     * @return true=已注册
     * @throws SchedulerException 调度异常
     */
    public boolean checkJobExists(TSampJob job) throws SchedulerException {
        return scheduler.checkExists(getJobKey(job));
    }

    /**
     * 获取jobKey
     *
     * @param job 任务配置
     * @return jobKey
     */
    private JobKey getJobKey(TSampJob job) {
        return JobKey.jobKey(job.getId(), JOB_GROUP);
    }
}
