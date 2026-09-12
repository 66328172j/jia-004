package com.fc.v2.service.impl;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.mapper.auto.TSampProductMapper;
import com.fc.v2.model.auto.TSampProduct;
import com.fc.v2.service.ITSampProductService;
import org.springframework.stereotype.Service;

/**
 * 受检产品档案Service业务层处理
 *
 * @author fuce
 * @date 2026-09-11
 */
@Service
public class TSampProductServiceImpl extends ServiceImpl<TSampProductMapper, TSampProduct> implements ITSampProductService {

    /**
     * 查询产品档案
     *
     * @param id 产品ID
     * @return 产品档案
     */
    @Override
    public TSampProduct selectTSampProductById(Long id) {
        return this.baseMapper.selectOne(new QueryWrapper<TSampProduct>()
                .eq("id", id)
                .eq("del_flag", 0));
    }

    /**
     * 查询产品档案列表
     *
     * @param queryWrapper 查询条件
     * @return 产品档案集合
     */
    @Override
    public List<TSampProduct> selectTSampProductList(Wrapper<TSampProduct> queryWrapper) {
        QueryWrapper<TSampProduct> wrapper = (queryWrapper instanceof QueryWrapper)
                ? (QueryWrapper<TSampProduct>) queryWrapper
                : new QueryWrapper<TSampProduct>();
        wrapper.eq("del_flag", 0).orderByAsc("code");
        return this.baseMapper.selectList(wrapper);
    }
}
