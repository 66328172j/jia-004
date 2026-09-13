package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;

import com.fc.v2.model.auto.TSampUrge;

/**
 * 检验批超期催办记录Service接口
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
    public TSampUrge selectTSampUrgeById(Long id);

    /**
     * 查询催办记录列表
     *
     * @param queryWrapper 查询条件
     * @return 催办记录集合
     */
    public List<TSampUrge> selectTSampUrgeList(Wrapper<TSampUrge> queryWrapper);

    /**
     * 发起催办：只有当前仍在超期（未判定且超过要求完成天数）的检验批允许催办；
     * 催办单号/催办次数/超期天数/催办人/催办时间均由服务端生成，
     * 同一催办人同一天对同一检验批只记一次
     *
     * @param tSampUrge 催办记录（lotId 必填）
     * @return 结果
     */
    public int insertTSampUrge(TSampUrge tSampUrge);

    /**
     * 批量删除催办记录
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteTSampUrgeByIds(String ids);

    /**
     * 删除催办记录
     *
     * @param id 主键
     * @return 结果
     */
    public int deleteTSampUrgeById(Long id);
}
