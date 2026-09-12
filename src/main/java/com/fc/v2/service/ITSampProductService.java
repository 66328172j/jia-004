package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;

import com.fc.v2.model.auto.TSampProduct;

/**
 * 受检产品档案Service接口
 *
 * @author fuce
 * @date 2026-09-11
 */
public interface ITSampProductService extends IService<TSampProduct> {

    /**
     * 查询产品档案
     *
     * @param id 产品ID
     * @return 产品档案
     */
    public TSampProduct selectTSampProductById(Long id);

    /**
     * 查询产品档案列表
     *
     * @param queryWrapper 查询条件
     * @return 产品档案集合
     */
    public List<TSampProduct> selectTSampProductList(Wrapper<TSampProduct> queryWrapper);
}
