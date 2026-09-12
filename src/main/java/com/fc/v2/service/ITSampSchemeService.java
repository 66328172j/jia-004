package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;

import com.fc.v2.model.auto.TSampScheme;

/**
 * 抽样方案Service接口
 *
 * @author fuce
 * @date 2026-09-11
 */
public interface ITSampSchemeService extends IService<TSampScheme> {

    /**
     * 查询抽样方案列表
     *
     * @param queryWrapper 查询条件
     * @return 抽样方案集合
     */
    public List<TSampScheme> selectTSampSchemeList(Wrapper<TSampScheme> queryWrapper);
}
