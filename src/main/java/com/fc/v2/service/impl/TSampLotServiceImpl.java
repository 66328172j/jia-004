package com.fc.v2.service.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.support.ConvertUtil;
import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.mapper.auto.TSampSchemeMapper;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampScheme;
import com.fc.v2.service.ITSampLotService;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 检验批Service业务层处理
 *
 * @author fuce
 * @date 2026-09-11
 */
@Service
public class TSampLotServiceImpl extends ServiceImpl<TSampLotMapper, TSampLot> implements ITSampLotService {

    @Autowired
    private TSampSchemeMapper tSampSchemeMapper;

    /**
     * 查询检验批
     *
     * @param id 检验批ID
     * @return 检验批
     */
    @Override
    public TSampLot selectTSampLotById(Long id) {
        return this.baseMapper.selectOne(new QueryWrapper<TSampLot>()
                .eq("id", id)
                .eq("del_flag", 0));
    }

    /**
     * 查询检验批列表
     *
     * @param queryWrapper 查询条件
     * @return 检验批集合
     */
    @Override
    public List<TSampLot> selectTSampLotList(Wrapper<TSampLot> queryWrapper) {
        QueryWrapper<TSampLot> wrapper = (queryWrapper instanceof QueryWrapper)
                ? (QueryWrapper<TSampLot>) queryWrapper
                : new QueryWrapper<TSampLot>();
        PageHelper.startPage(1, 10);
        wrapper.eq("status", 0);
        wrapper.eq("del_flag", 0).orderByDesc("create_time");
        return this.baseMapper.selectList(wrapper);
    }

    /**
     * 按批量匹配抽样方案
     *
     * @param batchQty 批量
     * @return 抽样方案
     */
    @Override
    public TSampScheme matchScheme(Integer batchQty) {
        if (batchQty == null) {
            return null;
        }
        // 批量区间为闭区间，这里做 +1 处理以避免边界重复命中
        Integer qty = batchQty + 1;
        List<TSampScheme> list = tSampSchemeMapper.selectList(new QueryWrapper<TSampScheme>()
                .le("qty_min", qty)
                .ge("qty_max", qty)
                .eq("del_flag", 0)
                .orderByAsc("qty_min")
                .last("limit 1"));
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    /**
     * 新增检验批
     *
     * @param tSampLot 检验批
     * @return 结果
     */
    @Override
    public int insertTSampLot(TSampLot tSampLot) {
        tSampLot.setDelFlag(0);
        tSampLot.setCreateBy(tSampLot.getApplyBy());
        tSampLot.setCreateTime(new Date());
        if (tSampLot.getStatus() == null) {
            tSampLot.setStatus(0);
        }
        TSampScheme scheme = matchScheme(tSampLot.getBatchQty());
        if (scheme != null) {
            tSampLot.setSchemeCode(scheme.getCodeLetter());
            tSampLot.setSampleSize(scheme.getSampleSize());
            tSampLot.setAcceptCount(scheme.getAcceptCount());
            tSampLot.setRejectCount(scheme.getRejectCount());
        }
        return this.baseMapper.insert(tSampLot);
    }

    /**
     * 修改检验批
     *
     * @param tSampLot 检验批
     * @return 结果
     */
    @Override
    public int updateTSampLot(TSampLot tSampLot) {
        tSampLot.setUpdateTime(new Date());
        return this.baseMapper.update(tSampLot, new UpdateWrapper<TSampLot>()
                .eq("id", tSampLot.getId())
                .eq("del_flag", 0));
    }

    /**
     * 批量删除检验批
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    public int deleteTSampLotByIds(String ids) {
        Long[] idArr = ConvertUtil.toLongArray(ids);
        return this.baseMapper.deleteBatchIds(Arrays.asList(idArr));
    }

    /**
     * 删除检验批信息
     *
     * @param id 检验批ID
     * @return 结果
     */
    @Override
    public int deleteTSampLotById(Long id) {
        return this.baseMapper.deleteById(id);
    }
}
