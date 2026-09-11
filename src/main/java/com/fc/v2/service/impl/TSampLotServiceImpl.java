package com.fc.v2.service.impl;

import java.util.Arrays;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.support.ConvertUtil;
import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.mapper.auto.TSampProductMapper;
import com.fc.v2.mapper.auto.TSampSchemeMapper;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampProduct;
import com.fc.v2.model.auto.TSampScheme;
import com.fc.v2.service.ITSampLotService;
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

    @Autowired
    private TSampProductMapper tSampProductMapper;

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
        // 批量区间为闭区间 [qty_min, qty_max]，取批量落入的第一档
        List<TSampScheme> list = tSampSchemeMapper.selectList(new QueryWrapper<TSampScheme>()
                .le("qty_min", batchQty)
                .ge("qty_max", batchQty)
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
        // 新登记的检验批为待抽样
        tSampLot.setStatus(0);
        fillDerivedFields(tSampLot);
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
        fillDerivedFields(tSampLot);
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

    /**
     * 填充派生字段：冗余产品编号/名称/AQL，并按批量匹配抽样方案填充字码、样本量、接收数、拒收数
     *
     * @param tSampLot 检验批
     */
    private void fillDerivedFields(TSampLot tSampLot) {
        if (tSampLot.getProductId() != null) {
            TSampProduct product = tSampProductMapper.selectById(tSampLot.getProductId());
            if (product != null) {
                tSampLot.setProductCode(product.getCode());
                tSampLot.setProductName(product.getName());
                tSampLot.setAql(product.getAql());
            }
        }
        TSampScheme scheme = matchScheme(tSampLot.getBatchQty());
        if (scheme != null) {
            tSampLot.setSchemeCode(scheme.getCodeLetter());
            tSampLot.setSampleSize(scheme.getSampleSize());
            tSampLot.setAcceptCount(scheme.getAcceptCount());
            tSampLot.setRejectCount(scheme.getRejectCount());
        }
    }
}
