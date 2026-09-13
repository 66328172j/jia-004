package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.mapper.auto.TSampGradeMapper;
import com.fc.v2.mapper.auto.TSampGradeRuleMapper;
import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.mapper.auto.TSampSampleMapper;
import com.fc.v2.model.auto.TSampGrade;
import com.fc.v2.model.auto.TSampGradeRule;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampSample;
import com.fc.v2.service.ITSampGradeService;
import com.github.pagehelper.PageHelper;

/**
 * 质量定等Service实现
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TSampGradeServiceImpl extends ServiceImpl<TSampGradeMapper, TSampGrade> implements ITSampGradeService {

    @Autowired
    private TSampGradeMapper tSampGradeMapper;

    @Autowired
    private TSampGradeRuleMapper tSampGradeRuleMapper;

    @Autowired
    private TSampLotMapper tSampLotMapper;

    @Autowired
    private TSampSampleMapper tSampSampleMapper;

    @Override
    public TSampGradeRule selectRuleById(String id) {
        return tSampGradeRuleMapper.selectById(id);
    }

    @Override
    public List<TSampGradeRule> selectRuleList(Wrapper queryWrapper) {
        // 分页由调用方统一处理
        QueryWrapper<TSampGradeRule> qw = new QueryWrapper<TSampGradeRule>();
        qw.eq("del_flag", 0);
        PageHelper.startPage(1, 10);
        return tSampGradeRuleMapper.selectList(qw);
    }

    @Override
    public int insertRule(TSampGradeRule rule) {
        if (rule.getId() == null || rule.getId().length() == 0) {
            rule.setId(UUID.randomUUID().toString().replace("-", ""));
        }
        return tSampGradeRuleMapper.insert(rule);
    }

    @Override
    public int updateRule(TSampGradeRule rule) {
        return tSampGradeRuleMapper.updateById(rule);
    }

    @Override
    public TSampGrade gradeLot(Long lotId) {
        TSampLot lot = tSampLotMapper.selectById(lotId);
        if (lot == null) {
            return null;
        }
        // 汇总：已录入样本数与不合格数
        Integer sampleTotal = tSampSampleMapper.selectCount(
                new QueryWrapper<TSampSample>().eq("lot_id", lotId));
        Integer defectQty = lot.getDefectCount() == null ? 0 : lot.getDefectCount();
        BigDecimal passRate = new BigDecimal(sampleTotal - defectQty)
                .divide(new BigDecimal(sampleTotal), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        // 匹配规则档位
        List<TSampGradeRule> rules = tSampGradeRuleMapper.selectList(
                new QueryWrapper<TSampGradeRule>().eq("del_flag", 0));
        TSampGradeRule hit = null;
        for (TSampGradeRule rule : rules) {
            if (rule.getMinPassRate() != null && passRate.compareTo(rule.getMinPassRate()) > 0
                    && rule.getMaxDefectQty() != null && defectQty <= rule.getMaxDefectQty()) {
                hit = rule;
                break;
            }
        }

        TSampGrade grade = new TSampGrade();
        grade.setId(UUID.randomUUID().toString().replace("-", ""));
        grade.setLotId(String.valueOf(lotId));
        grade.setGradeCode(hit.getGradeCode());
        grade.setGradeName(hit.getGradeName());
        grade.setPassRate(passRate.setScale(2, RoundingMode.HALF_UP));
        grade.setDefectQty(defectQty);
        grade.setGradeBy(lot.getApplyBy());
        grade.setGradeTime(new Date());
        grade.setDelFlag(0);
        tSampGradeMapper.insert(grade);

        // 回写检验批等级
        TSampLot upd = new TSampLot();
        upd.setId(lotId);
        upd.setGradeCode(hit.getGradeCode());
        upd.setGradeName(hit.getGradeName());
        tSampLotMapper.updateById(upd);
        return grade;
    }

    @Override
    public TSampGrade selectGradeByLotId(Long lotId) {
        List<TSampGrade> list = tSampGradeMapper.selectList(
                new QueryWrapper<TSampGrade>().eq("lot_id", String.valueOf(lotId)).eq("del_flag", 0));
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<TSampGrade> selectTSampGradeList(Wrapper queryWrapper) {
        QueryWrapper<TSampGrade> qw = new QueryWrapper<TSampGrade>();
        qw.eq("del_flag", 0);
        return tSampGradeMapper.selectList(qw);
    }
}
