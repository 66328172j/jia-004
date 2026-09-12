package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;

import com.fc.v2.model.auto.TSampUrge;

/**
 * 超期催办Service接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITSampUrgeService extends IService<TSampUrge> {

    /**
     * 查询催办记录
     *
     * @param id 主键
     * @return 催办记录
     */
    public TSampUrge selectTSampUrgeById(String id);

    /**
     * 查询催办记录列表
     *
     * @param queryWrapper 查询条件
     * @return 催办记录集合
     */
    public List<TSampUrge> selectTSampUrgeList(Wrapper<TSampUrge> queryWrapper);

    /**
     * 计算检验批当前超期天数（超过要求完成日期起算）
     *
     * @param lotId 检验批ID
     * @return 超期天数，未超期返回 0
     */
    public int calcOverdueDays(Long lotId);

    /**
     * 发起催办：写入催办单号、催办次数、超期天数、催办人与催办时间
     *
     * @param tSampUrge 催办记录（lotId 必填）
     * @return 结果
     */
    public int insertTSampUrge(TSampUrge tSampUrge);

    /**
     * 处理催办
     *
     * @param tSampUrge 催办记录
     * @return 结果
     */
    public int updateTSampUrge(TSampUrge tSampUrge);

    /**
     * 删除催办记录
     *
     * @param id 主键
     * @return 结果
     */
    public int deleteTSampUrgeById(String id);
}
