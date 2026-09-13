package com.fc.v2.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.support.ConvertUtil;
import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.mapper.auto.TSampRetestMapper;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampRetest;
import com.fc.v2.model.auto.TSysUser;
import com.fc.v2.service.ITSampLotService;
import com.fc.v2.service.ITSampRetestService;
import com.fc.v2.service.judge.SampJudgeContext;
import com.fc.v2.service.judge.SampJudgeExecutor;
import com.fc.v2.service.judge.SampJudgeResult;
import com.fc.v2.service.judge.SampJudgeType;
import com.fc.v2.shiro.util.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 检验批复检记录Service业务层处理
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TSampRetestServiceImpl extends ServiceImpl<TSampRetestMapper, TSampRetest> implements ITSampRetestService {

    /** 待复检 */
    private static final int STATUS_PENDING = 0;
    /** 已完成 */
    private static final int STATUS_FINISHED = 1;
    /** 已作废 */
    private static final int STATUS_CANCELED = 2;

    /** 判定合格 */
    private static final int RESULT_PASS = 0;
    /** 判定不合格 */
    private static final int RESULT_FAIL = 1;

    @Autowired
    private TSampLotMapper tSampLotMapper;

    @Autowired
    private ITSampLotService tSampLotService;

    @Autowired
    private SampJudgeExecutor sampJudgeExecutor;

    /**
     * 查询复检单
     *
     * @param id 主键
     * @return 复检单
     */
    @Override
    public TSampRetest selectTSampRetestById(Long id) {
        TSampRetest retest = this.baseMapper.selectOne(new QueryWrapper<TSampRetest>()
                .eq("id", id)
                .eq("del_flag", 0));
        if (retest != null) {
            fillLotInfo(Arrays.asList(retest));
        }
        return retest;
    }

    /**
     * 查询复检单列表
     *
     * @param queryWrapper 查询条件
     * @return 复检单集合
     */
    @Override
    public List<TSampRetest> selectTSampRetestList(Wrapper<TSampRetest> queryWrapper) {
        QueryWrapper<TSampRetest> wrapper = (queryWrapper instanceof QueryWrapper)
                ? (QueryWrapper<TSampRetest>) queryWrapper
                : new QueryWrapper<TSampRetest>();
        wrapper.eq("del_flag", 0).orderByDesc("create_time");
        List<TSampRetest> list = this.baseMapper.selectList(wrapper);
        fillLotInfo(list);
        return list;
    }

    /**
     * 发起复检：只有判定不合格（已判定、未关闭）的检验批可以发起；
     * 同一检验批存在待复检单时不允许重复发起；复检样本量按应抽样本量的两倍记录
     *
     * @param tSampRetest 复检单
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertTSampRetest(TSampRetest tSampRetest) {
        if (tSampRetest.getLotId() == null) {
            throw new IllegalArgumentException("检验批不能为空");
        }
        TSampLot lot = tSampLotService.selectTSampLotById(tSampRetest.getLotId());
        if (lot == null) {
            throw new IllegalArgumentException("检验批不存在或已删除");
        }
        if (lot.getStatus() == null || lot.getStatus() != 3 || !"不合格".equals(lot.getConclude())) {
            // 复检针对判定不合格的批发起：合格批无需复检，已关闭批记录冻结
            throw new IllegalArgumentException("只有判定不合格的检验批才允许发起复检");
        }
        if (tSampRetest.getReason() == null || tSampRetest.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("复检原因不能为空");
        }
        Integer sampleSize = lot.getSampleSize();
        if (sampleSize == null || sampleSize <= 0) {
            throw new IllegalArgumentException("检验批[" + lot.getLotNo() + "]未确定应抽样本量，无法生成复检抽样数量");
        }
        Integer pendingCount = this.baseMapper.selectCount(new QueryWrapper<TSampRetest>()
                .eq("lot_id", lot.getId())
                .eq("status", STATUS_PENDING)
                .eq("del_flag", 0));
        if (pendingCount != null && pendingCount > 0) {
            throw new IllegalArgumentException("该检验批已存在待复检单，请先完成或作废后再发起");
        }
        // 复检单号后端生成，前端传值一律忽略
        tSampRetest.setRetestNo("FJ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        // 原判定按检验批当前结论记录（能走到这里必为不合格）
        tSampRetest.setOriginResult("不合格".equals(lot.getConclude()) ? RESULT_FAIL : RESULT_PASS);
        // 复检抽样按规定加倍：应抽样本量的两倍
        tSampRetest.setSampleQty(sampleSize * 2);
        // 复检结果/复检人/复检时间由完成环节记录，发起时清空防止前端伪造
        tSampRetest.setRetestResult(null);
        tSampRetest.setRetestBy(null);
        tSampRetest.setRetestTime(null);
        tSampRetest.setStatus(STATUS_PENDING);
        tSampRetest.setDelFlag(0);
        return this.baseMapper.insert(tSampRetest);
    }

    /**
     * 录入复检结果并回写检验批判定：复检合格检验批改判合格，复检不合格维持不合格；
     * 复检人取当前登录人、复检时间取当前时间；只有待复检单允许录入
     *
     * @param id           复检单主键
     * @param retestResult 复检判定 0合格 1不合格
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int finishTSampRetest(Long id, Integer retestResult) {
        if (retestResult == null || (retestResult != RESULT_PASS && retestResult != RESULT_FAIL)) {
            throw new IllegalArgumentException("复检结果只能为合格或不合格");
        }
        TSampRetest retest = selectTSampRetestById(id);
        if (retest == null) {
            throw new IllegalArgumentException("复检单不存在或已删除");
        }
        if (Integer.valueOf(STATUS_FINISHED).equals(retest.getStatus())) {
            throw new IllegalArgumentException("复检单已完成，不能重复录入复检结果");
        }
        if (Integer.valueOf(STATUS_CANCELED).equals(retest.getStatus())) {
            throw new IllegalArgumentException("复检单已作废，不能录入复检结果");
        }
        // 复检人必须是当前实际做复检的登录人：不能取报检单上的报检人(lot.apply_by)，
        // 也不能用 ShiroUtils 在无登录上下文时返回的固定兜底值"Task"。
        // 这里直接取登录主体并显式校验，取不到登录人就拒绝录入，避免把错误的人写进复检单
        TSysUser retestUser;
        try {
            retestUser = ShiroUtils.getUser();
        } catch (org.apache.shiro.UnavailableSecurityManagerException e) {
            throw new IllegalArgumentException("无法确定复检人，请重新登录后再录入复检结果");
        }
        if (retestUser == null || retestUser.getUsername() == null
                || retestUser.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("无法确定复检人，请重新登录后再录入复检结果");
        }
        int rows = this.baseMapper.update(null, new UpdateWrapper<TSampRetest>()
                .eq("id", id)
                .eq("del_flag", 0)
                .set("retest_result", retestResult)
                .set("retest_by", retestUser.getUsername())
                .set("retest_time", new Date())
                .set("status", STATUS_FINISHED));

        // 复检合格则检验批改判合格，复检不合格维持不合格（结论保持"不合格"不变）
        SampJudgeContext context = new SampJudgeContext();
        context.setRetestResult(retestResult);
        SampJudgeResult result = sampJudgeExecutor.judge(SampJudgeType.RETEST, context);
        tSampLotMapper.update(null, new UpdateWrapper<TSampLot>()
                .eq("id", retest.getLotId())
                .eq("del_flag", 0)
                .set("conclude", result.getConclude()));
        return rows;
    }

    /**
     * 作废复检单：只有待复检的复检单允许作废，作废后不再回写检验批
     *
     * @param id 复检单主键
     * @return 结果
     */
    @Override
    public int cancelTSampRetest(Long id) {
        TSampRetest retest = selectTSampRetestById(id);
        if (retest == null) {
            throw new IllegalArgumentException("复检单不存在或已删除");
        }
        if (!Integer.valueOf(STATUS_PENDING).equals(retest.getStatus())) {
            throw new IllegalArgumentException("只有待复检的复检单才允许作废");
        }
        return this.baseMapper.update(null, new UpdateWrapper<TSampRetest>()
                .eq("id", id)
                .eq("del_flag", 0)
                .set("status", STATUS_CANCELED));
    }

    /**
     * 批量删除复检单
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    public int deleteTSampRetestByIds(String ids) {
        Long[] idArr = ConvertUtil.toLongArray(ids);
        return this.baseMapper.deleteBatchIds(Arrays.asList(idArr));
    }

    /**
     * 删除复检单
     *
     * @param id 主键
     * @return 结果
     */
    @Override
    public int deleteTSampRetestById(Long id) {
        return this.baseMapper.deleteById(id);
    }

    /**
     * 批量回填检验批号/产品名称（关联展示用，复检单本身不冗余）
     *
     * @param list 复检单集合
     */
    private void fillLotInfo(List<TSampRetest> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> lotIds = new HashSet<Long>();
        for (TSampRetest retest : list) {
            if (retest.getLotId() != null) {
                lotIds.add(retest.getLotId());
            }
        }
        if (lotIds.isEmpty()) {
            return;
        }
        List<TSampLot> lots = tSampLotService.selectTSampLotList(new QueryWrapper<TSampLot>()
                .in("id", new ArrayList<Long>(lotIds)));
        for (TSampRetest retest : list) {
            for (TSampLot lot : lots) {
                if (retest.getLotId() != null && retest.getLotId().equals(lot.getId())) {
                    retest.setLotNo(lot.getLotNo());
                    retest.setProductName(lot.getProductName());
                    break;
                }
            }
        }
    }
}
