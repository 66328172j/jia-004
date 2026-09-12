package com.fc.v2.service.impl;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.mapper.auto.TSampSchemeMapper;
import com.fc.v2.model.auto.TSampScheme;
import com.fc.v2.service.ITSampSchemeService;
import org.springframework.stereotype.Service;

/**
 * 抽样方案Service业务层处理
 *
 * @author fuce
 * @date 2026-09-11
 */
@Service
public class TSampSchemeServiceImpl extends ServiceImpl<TSampSchemeMapper, TSampScheme> implements ITSampSchemeService {

    /**
     * 查询抽样方案列表
     *
     * @param queryWrapper 查询条件
     * @return 抽样方案集合
     */
    @Override
    public List<TSampScheme> selectTSampSchemeList(Wrapper<TSampScheme> queryWrapper) {
        QueryWrapper<TSampScheme> wrapper = (queryWrapper instanceof QueryWrapper)
                ? (QueryWrapper<TSampScheme>) queryWrapper
                : new QueryWrapper<TSampScheme>();
        wrapper.eq("del_flag", 0).orderByAsc("qty_min");
        return this.baseMapper.selectList(wrapper);
    }
}
