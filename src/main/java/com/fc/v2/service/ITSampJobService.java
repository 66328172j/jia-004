package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;

import com.fc.v2.model.auto.TSampJob;
import com.fc.v2.model.auto.TSampJobLog;

/**
 * 定时任务Service接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITSampJobService extends IService<TSampJob> {

    /**
     * 查询任务配置
     *
     * @param id 主键
     * @return 任务配置
     */
    public TSampJob selectTSampJobById(String id);

    /**
     * 查询任务配置列表
     *
     * @param queryWrapper 查询条件
     * @return 任务集合
     */
    public List<TSampJob> selectTSampJobList(Wrapper<TSampJob> queryWrapper);

    /**
     * 查询任务日志列表
     *
     * @param queryWrapper 查询条件
     * @return 日志集合
     */
    public List<TSampJobLog> selectTSampJobLogList(Wrapper<TSampJobLog> queryWrapper);

    /**
     * 执行一次超期扫描：扫描未判定且超期的检验批，自动生成催办记录，并落一条任务日志
     *
     * @param jobId 任务配置ID
     * @return 任务日志；任务不存在或已停用时返回 null
     */
    public TSampJobLog runOverdueScan(String jobId);
}
