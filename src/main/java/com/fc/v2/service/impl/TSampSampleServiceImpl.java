package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.support.ConvertUtil;
import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.mapper.auto.TSampSampleMapper;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampSample;
import com.fc.v2.service.ITSampLotService;
import com.fc.v2.service.ITSampSampleService;
import com.fc.v2.service.judge.SampJudgeContext;
import com.fc.v2.service.judge.SampJudgeExecutor;
import com.fc.v2.service.judge.SampJudgeResult;
import com.fc.v2.service.judge.SampJudgeType;
import com.fc.v2.shiro.util.ShiroUtils;
import com.fc.v2.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private SampJudgeExecutor sampJudgeExecutor;

    /**
     * 查询样本检测记录
     *
     * @param id 主键
     * @return 样本检测记录
     */
    @Override
    public TSampSample selectTSampSampleById(Long id) {
        TSampSample sample = this.baseMapper.selectOne(new QueryWrapper<TSampSample>()
                .eq("id", id)
                .eq("del_flag", 0));
        if (sample != null && sample.getLotId() != null) {
            TSampLot lot = tSampLotService.selectTSampLotById(sample.getLotId());
            if (lot != null) {
                sample.setLotNo(lot.getLotNo());
            }
        }
        return sample;
    }

    /**
     * 查询样本检测记录列表：默认按检验批、样本编号升序
     *
     * @param queryWrapper 查询条件
     * @return 样本检测记录集合
     */
    @Override
    public List<TSampSample> selectTSampSampleList(Wrapper<TSampSample> queryWrapper) {
        QueryWrapper<TSampSample> wrapper = (queryWrapper instanceof QueryWrapper)
                ? (QueryWrapper<TSampSample>) queryWrapper
                : new QueryWrapper<TSampSample>();
        wrapper.eq("del_flag", 0);
        wrapper.orderByAsc("lot_id", "sample_no");
        List<TSampSample> list = this.baseMapper.selectList(wrapper);
        fillLotNo(list);
        return list;
    }

    /**
     * 查询某检验批下已录入的样本编号
     *
     * @param lotId 检验批ID
     * @return 样本编号集合
     */
    @Override
    public List<String> selectRecordedSampleNos(Long lotId) {
        List<TSampSample> list = this.baseMapper.selectList(new QueryWrapper<TSampSample>()
                .select("sample_no")
                .eq("lot_id", lotId)
                .eq("del_flag", 0)
                .orderByAsc("sample_no"));
        List<String> nos = new ArrayList<String>();
        for (TSampSample sample : list) {
            nos.add(sample.getSampleNo());
        }
        return nos;
    }

    /**
     * 新增样本检测记录：样本编号按检验批应抽样本量自动生成（S01..Sn），
     * 检测人取当前登录人、检测时间取当前时间；录入后自动汇总检验批，录满时自动判定
     *
     * @param tSampSample 样本检测记录
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertTSampSample(TSampSample tSampSample) {
        TSampLot lot = checkLotEditable(tSampSample.getLotId());
        Integer planned = lot.getSampleSize();
        if (planned == null || planned <= 0) {
            throw new IllegalArgumentException("检验批[" + lot.getLotNo() + "]未确定应抽样本量，无法生成样本编号");
        }
        Set<String> recorded = new HashSet<String>(selectRecordedSampleNos(lot.getId()));
        if (recorded.size() >= planned) {
            throw new IllegalArgumentException("检验批[" + lot.getLotNo() + "]应抽" + planned + "个样本，已录满");
        }
        validateSampleFields(tSampSample);
        // 编号服务端生成：取 S01..Sn 中第一个未占用编号，删掉重录也能补位，前端传值一律忽略
        String sampleNo = nextSampleNo(recorded, planned);
        tSampSample.setSampleNo(sampleNo);
        tSampSample.setCheckBy(ShiroUtils.getLoginName());
        tSampSample.setCheckTime(new Date());
        tSampSample.setDelFlag(0);
        int rows = this.baseMapper.insert(tSampSample);
        if (rows > 0) {
            refreshLotSummary(lot.getId());
        }
        return rows;
    }

    /**
     * 修改样本检测记录：样本编号/所属检验批/检测人/检测时间为系统记录项不允许改动，
     * 只更新检测内容；修改后重新汇总判定检验批
     *
     * @param tSampSample 样本检测记录
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateTSampSample(TSampSample tSampSample) {
        if (tSampSample.getId() == null) {
            throw new IllegalArgumentException("样本记录ID不能为空");
        }
        TSampSample dbSample = this.baseMapper.selectOne(new QueryWrapper<TSampSample>()
                .eq("id", tSampSample.getId())
                .eq("del_flag", 0));
        if (dbSample == null) {
            return 0;
        }
        checkLotEditable(dbSample.getLotId());
        if (tSampSample.getItemResult() == null
                || (tSampSample.getItemResult() != 0 && tSampSample.getItemResult() != 1)) {
            throw new IllegalArgumentException("单项判定只能为合格或不合格");
        }
        // 白字段更新，防止篡改样本编号、检测人、检测时间、所属检验批
        TSampSample upd = new TSampSample();
        upd.setId(dbSample.getId());
        upd.setItemCode(tSampSample.getItemCode());
        upd.setItemName(tSampSample.getItemName());
        upd.setStdValue(tSampSample.getStdValue());
        upd.setMeasuredValue(tSampSample.getMeasuredValue());
        upd.setItemResult(tSampSample.getItemResult());
        upd.setRemark(tSampSample.getRemark());
        int rows = this.baseMapper.update(upd, new UpdateWrapper<TSampSample>().eq("id", dbSample.getId()));
        if (rows > 0) {
            refreshLotSummary(dbSample.getLotId());
        }
        return rows;
    }

    /**
     * 批量删除样本检测记录，删除后重新汇总检验批
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteTSampSampleByIds(String ids) {
        Long[] idArr = ConvertUtil.toLongArray(ids);
        List<Long> idList = Arrays.asList(idArr);
        List<TSampSample> samples = this.baseMapper.selectList(new QueryWrapper<TSampSample>()
                .in("id", idList)
                .eq("del_flag", 0));
        if (samples.isEmpty()) {
            return 0;
        }
        // 已关闭的检验批检测记录冻结，不允许删除
        for (TSampSample sample : samples) {
            checkLotEditable(sample.getLotId());
        }
        int rows = this.baseMapper.deleteBatchIds(idList);
        if (rows > 0) {
            Set<Long> lotIds = new HashSet<Long>();
            for (TSampSample sample : samples) {
                lotIds.add(sample.getLotId());
            }
            for (Long lotId : lotIds) {
                refreshLotSummary(lotId);
            }
        }
        return rows;
    }

    /**
     * 删除样本检测记录
     *
     * @param id 主键
     * @return 结果
     */
    @Override
    public int deleteTSampSampleById(Long id) {
        return deleteTSampSampleByIds(String.valueOf(id));
    }

    /**
     * 校验检验批是否允许登记/修改/删除样本检测记录：
     * 已判定说明样本已录满并作出结论，已关闭则整批冻结，均不再允许改动
     *
     * @param lotId 检验批ID
     * @return 检验批
     */
    private TSampLot checkLotEditable(Long lotId) {
        if (lotId == null) {
            throw new IllegalArgumentException("检验批不能为空");
        }
        TSampLot lot = tSampLotService.selectTSampLotById(lotId);
        if (lot == null) {
            throw new IllegalArgumentException("检验批不存在或已删除");
        }
        if (lot.getStatus() != null && lot.getStatus() == 4) {
            throw new IllegalArgumentException("检验批[" + lot.getLotNo() + "]已关闭，检测记录不允许改动");
        }
        if (lot.getStatus() != null && lot.getStatus() == 3) {
            throw new IllegalArgumentException("检验批[" + lot.getLotNo() + "]已判定，检测记录不允许改动");
        }
        return lot;
    }

    /**
     * 校验检测内容必填与单项判定取值
     *
     * @param sample 样本检测记录
     */
    private void validateSampleFields(TSampSample sample) {
        if (StringUtils.isEmpty(sample.getItemCode())) {
            throw new IllegalArgumentException("检测项编码不能为空");
        }
        if (StringUtils.isEmpty(sample.getItemName())) {
            throw new IllegalArgumentException("检测项名称不能为空");
        }
        if (StringUtils.isEmpty(sample.getStdValue())) {
            throw new IllegalArgumentException("标准值不能为空");
        }
        if (StringUtils.isEmpty(sample.getMeasuredValue())) {
            throw new IllegalArgumentException("实测值不能为空");
        }
        if (sample.getItemResult() == null
                || (sample.getItemResult() != 0 && sample.getItemResult() != 1)) {
            throw new IllegalArgumentException("单项判定只能为合格或不合格");
        }
    }

    /**
     * 生成下一个样本编号：S01..Sn 中取第一个未占用编号（%02d，超过99自动变三位）
     *
     * @param recorded 已占用编号
     * @param planned  应抽样本量
     * @return 样本编号
     */
    private String nextSampleNo(Set<String> recorded, int planned) {
        for (int i = 1; i <= planned; i++) {
            String no = String.format("S%02d", i);
            if (!recorded.contains(no)) {
                return no;
            }
        }
        // 唯一索引兜底前的理论不可达分支：已录满由调用方拦截
        throw new IllegalArgumentException("样本编号已生成完毕");
    }

    /**
     * 按当前样本检测记录刷新检验批汇总与状态：
     * 0待抽样（无记录）→ 1抽样中（记录不足额）→ 2待判定（录满，瞬时）→ 3已判定（按一次抽样口径自动判定）；
     * 记录被删导致不足额时，已判定的批自动回退并清空结论
     *
     * @param lotId 检验批ID
     */
    private void refreshLotSummary(Long lotId) {
        TSampLot lot = tSampLotService.selectTSampLotById(lotId);
        if (lot == null) {
            return;
        }
        List<TSampSample> samples = this.baseMapper.selectList(new QueryWrapper<TSampSample>()
                .eq("lot_id", lotId)
                .eq("del_flag", 0));
        int count = samples.size();
        int defect = countDefect(samples);
        int planned = lot.getSampleSize() == null ? 0 : lot.getSampleSize();

        UpdateWrapper<TSampLot> wrapper = new UpdateWrapper<TSampLot>()
                .eq("id", lotId)
                .set("defect_count", defect)
                .set("pass_rate", calcPassRate(samples));

        if (planned > 0 && count >= planned) {
            // 样本录满：按一次抽样口径自动判定，判定后流转到已判定；
            // 落在 Ac/Re 间隙（个别加严方案）时停在待判定
            SampJudgeContext context = new SampJudgeContext();
            context.setDefect(defect);
            context.setAccept(lot.getAcceptCount());
            context.setReject(lot.getRejectCount());
            SampJudgeResult result = sampJudgeExecutor.judge(SampJudgeType.AC_RE, context);
            wrapper.set("status", result.getStatus()).set("conclude", result.getConclude());
            tSampLotMapper.update(null, wrapper);
            return;
        }
        // 未录满：抽样中（一条都没有则回到待抽样）；原结论作废，等待录满后重新自动判定
        wrapper.set("status", count == 0 ? 0 : 1).set("conclude", null);
        tSampLotMapper.update(null, wrapper);
    }

    /** 回填检验批号用于列表展示 */
    private void fillLotNo(List<TSampSample> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> lotIds = new HashSet<Long>();
        for (TSampSample sample : list) {
            if (sample.getLotId() != null) {
                lotIds.add(sample.getLotId());
            }
        }
        if (lotIds.isEmpty()) {
            return;
        }
        List<TSampLot> lots = tSampLotMapper.selectList(new QueryWrapper<TSampLot>()
                .select("id", "lot_no")
                .in("id", lotIds)
                .eq("del_flag", 0));
        Map<Long, String> lotNoMap = new HashMap<Long, String>();
        for (TSampLot lot : lots) {
            lotNoMap.put(lot.getId(), lot.getLotNo());
        }
        for (TSampSample sample : list) {
            sample.setLotNo(lotNoMap.get(sample.getLotId()));
        }
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

    /**
     * 按已录样本数计算合格率（百分比，保留两位小数），无记录时返回 null
     *
     * @param samples 样本记录
     * @return 合格率
     */
    private BigDecimal calcPassRate(List<TSampSample> samples) {
        if (samples.isEmpty()) {
            return null;
        }
        int defect = countDefect(samples);
        return new BigDecimal(samples.size() - defect)
                .multiply(new BigDecimal(100))
                .divide(new BigDecimal(samples.size()), 2, RoundingMode.HALF_UP);
    }
}
