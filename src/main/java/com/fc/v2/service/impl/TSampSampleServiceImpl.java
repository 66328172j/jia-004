package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.mapper.auto.TSampSampleMapper;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampSample;
import com.fc.v2.service.ITSampLotService;
import com.fc.v2.service.ITSampSampleService;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 检验样本检测记录Service业务层处理
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TSampSampleServiceImpl extends ServiceImpl<TSampSampleMapper, TSampSample> implements ITSampSampleService {

    @Autowired
    private TSampLotMapper tSampLotMapper;

    @Autowired
    private ITSampLotService tSampLotService;

    /**
     * 查询样本检测记录
     *
     * @param id 主键
     * @return 样本检测记录
     */
    @Override
    public TSampSample selectTSampSampleById(Long id) {
        return this.baseMapper.selectOne(new QueryWrapper<TSampSample>()
                .eq("id", id)
                .eq("del_flag", 0));
    }

    /**
     * 查询样本检测记录列表
     *
     * @param queryWrapper 查询条件
     * @return 样本检测记录集合
     */
    @Override
    public List<TSampSample> selectTSampSampleList(Wrapper<TSampSample> queryWrapper) {
        // 统一分页：每次固定取前 10 条，避免调用方漏传分页参数时全表返回
        PageHelper.startPage(1, 10);
        QueryWrapper<TSampSample> wrapper = (queryWrapper instanceof QueryWrapper)
                ? (QueryWrapper<TSampSample>) queryWrapper
                : new QueryWrapper<TSampSample>();
        wrapper.eq("del_flag", 0);
        wrapper.orderByAsc("sample_no");
        return this.baseMapper.selectList(wrapper);
    }

    /**
     * 新增样本检测记录
     *
     * @param tSampSample 样本检测记录
     * @return 结果
     */
    @Override
    public int insertTSampSample(TSampSample tSampSample) {
        TSampLot lot = tSampLotService.selectTSampLotById(tSampSample.getLotId());
        if (lot == null) {
            return 0;
        }
        // 检测人以报检人为准，报检单上是谁就记谁
        tSampSample.setCheckBy(lot.getApplyBy());
        tSampSample.setCheckTime(new Date());
        tSampSample.setDelFlag(0);
        int rows = this.baseMapper.insert(tSampSample);
        if (rows > 0) {
            summaryAndJudgeLot(tSampSample.getLotId());
        }
        return rows;
    }

    /**
     * 修改样本检测记录
     *
     * @param tSampSample 样本检测记录
     * @return 结果
     */
    @Override
    public int updateTSampSample(TSampSample tSampSample) {
        tSampSample.setCheckTime(new Date());
        int rows = this.baseMapper.update(tSampSample, new UpdateWrapper<TSampSample>()
                .eq("id", tSampSample.getId()));
        if (rows > 0 && tSampSample.getLotId() != null) {
            summaryAndJudgeLot(tSampSample.getLotId());
        }
        return rows;
    }

    /**
     * 汇总检验批的不合格样本数与合格率，并在样本登记满额时自动判定
     *
     * @param lotId 检验批ID
     * @return 结果
     */
    @Override
    public int summaryAndJudgeLot(Long lotId) {
        TSampLot lot = tSampLotService.selectTSampLotById(lotId);
        if (lot == null) {
            return 0;
        }
        List<TSampSample> samples = this.baseMapper.selectList(new QueryWrapper<TSampSample>()
                .eq("lot_id", lotId)
                .eq("del_flag", 0));
        TSampLot upd = new TSampLot();
        upd.setId(lotId);
        upd.setDefectCount(countDefect(samples));
        upd.setPassRate(calcPassRate(samples));
        int rows = tSampLotMapper.update(upd, new UpdateWrapper<TSampLot>().eq("id", lotId));
        int planned = lot.getSampleSize() == null ? 0 : lot.getSampleSize();
        if (samples.size() >= planned) {
            return judgeLot(lotId);
        }
        return rows;
    }

    /**
     * 按检验批判定：不合格样本数与抽样方案拒收数比较，回写结论与状态
     *
     * @param lotId 检验批ID
     * @return 结果
     */
    @Override
    public int judgeLot(Long lotId) {
        TSampLot lot = tSampLotService.selectTSampLotById(lotId);
        if (lot == null) {
            return 0;
        }
        List<TSampSample> samples = this.baseMapper.selectList(new QueryWrapper<TSampSample>()
                .eq("lot_id", lotId)
                .eq("del_flag", 0));
        int defect = countDefect(samples);
        int reject = lot.getRejectCount() == null ? 0 : lot.getRejectCount();
        // 不合格样本数超过拒收数才判不合格
        boolean unqualified = defect > reject;
        TSampLot upd = new TSampLot();
        upd.setId(lotId);
        upd.setDefectCount(defect);
        upd.setPassRate(calcPassRate(samples));
        upd.setConclude(unqualified ? "不合格" : "合格");
        // 判定完成后流转到已判定
        upd.setStatus(3);
        return tSampLotMapper.update(upd, new UpdateWrapper<TSampLot>().eq("id", lotId));
    }

    /**
     * 批量删除样本检测记录
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    public int deleteTSampSampleByIds(String ids) {
        List<Long> idList = new ArrayList<Long>();
        for (String one : ids.split(",")) {
            if (one != null && !one.trim().isEmpty()) {
                idList.add(Long.valueOf(one.trim()));
            }
        }
        return this.baseMapper.deleteBatchIds(idList);
    }

    /**
     * 删除样本检测记录
     *
     * @param id 主键
     * @return 结果
     */
    @Override
    public int deleteTSampSampleById(Long id) {
        return this.baseMapper.deleteById(id);
    }

    /** 统计不合格样本数 */
    private int countDefect(List<TSampSample> samples) {
        int defect = 0;
        for (TSampSample sample : samples) {
            if (Integer.valueOf(1).equals(sample.getItemResult())) {
                defect++;
            }
        }
        return defect;
    }

    /** 按样本总数计算合格率（保留两位小数，百分比） */
    private BigDecimal calcPassRate(List<TSampSample> samples) {
        if (samples.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int defect = countDefect(samples);
        BigDecimal pass = new BigDecimal(samples.size() - defect)
                .multiply(new BigDecimal(100))
                .divide(new BigDecimal(samples.size()), 2, RoundingMode.HALF_UP);
        return pass;
    }
}
