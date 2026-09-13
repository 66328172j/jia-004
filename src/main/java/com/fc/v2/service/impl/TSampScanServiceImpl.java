package com.fc.v2.service.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.mapper.auto.TSampQueryLogMapper;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampQueryLog;
import com.fc.v2.model.auto.TSampScanVo;
import com.fc.v2.service.ITSampScanService;
import com.fc.v2.util.StringUtils;

/**
 * 移动端扫码查询Service实现
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TSampScanServiceImpl implements ITSampScanService {

    private static final Logger logger = LoggerFactory.getLogger(TSampScanServiceImpl.class);

    /** 查询来源缺省值（扫码/手输页面未上报来源时使用） */
    private static final String DEFAULT_SOURCE = "H5";

    @Resource
    private TSampLotMapper tSampLotMapper;

    @Resource
    private TSampQueryLogMapper tSampQueryLogMapper;

    /**
     * 扫码查询检验批：
     * 命中只返回对外脱敏视图 TSampScanVo（批号/产品/检验状态/判定结论/质量等级），
     * 抽样方案与内部统计字段一律不带出；未命中返回 hit=false 和明确提示，不抛异常；
     * 每次查询都落一条查询记录，记录落库失败只记日志，不影响查询主流程。
     *
     * @param lotNo  批号
     * @param source 查询来源（如 APP / WECHAT / WEB）
     * @return hit 是否命中、message 提示语、data 对外脱敏视图（未命中为 null）
     */
    @Override
    public Map<String, Object> scanQuery(String lotNo, String source) {
        String queryLotNo = lotNo == null ? "" : lotNo.trim();
        String querySource = StringUtils.isNotEmpty(source) ? source.trim() : DEFAULT_SOURCE;

        TSampLot lot = null;
        if (StringUtils.isNotEmpty(queryLotNo)) {
            // 对外查询只能看到正常批，逻辑删除的批按不存在处理
            lot = tSampLotMapper.selectOne(new QueryWrapper<TSampLot>()
                    .eq("lot_no", queryLotNo)
                    .eq("del_flag", 0)
                    .last("limit 1"));
        }

        Map<String, Object> result = new java.util.HashMap<String, Object>();
        if (lot == null) {
            result.put("hit", false);
            result.put("message", StringUtils.isNotEmpty(queryLotNo)
                    ? "未查询到批号[" + queryLotNo + "]的检验信息，请核对批号后重试"
                    : "请输入或扫描批号后再查询");
            result.put("data", null);
        } else {
            result.put("hit", true);
            result.put("message", "查询成功");
            result.put("data", toScanVo(lot));
        }

        // 无论命中与否都记录一次查询，便于追踪客户查了哪些批号
        saveQueryLog(queryLotNo, lot, querySource);
        return result;
    }

    /**
     * 查询移动端查询记录列表（后台管理分页用，分页由调用方 PageHelper.startPage 控制）
     *
     * @param queryWrapper 查询条件（按批号模糊查、来源、命中标志、时间范围等）
     * @return 查询记录集合，按查询时间倒序
     */
    @Override
    public List<TSampQueryLog> selectQueryLogList(Wrapper<TSampQueryLog> queryWrapper) {
        QueryWrapper<TSampQueryLog> wrapper = (queryWrapper instanceof QueryWrapper)
                ? (QueryWrapper<TSampQueryLog>) queryWrapper
                : new QueryWrapper<TSampQueryLog>();
        wrapper.orderByDesc("query_time");
        return tSampQueryLogMapper.selectList(wrapper);
    }

    /**
     * 批量删除查询记录
     *
     * @param ids 需要删除的数据主键，逗号分隔
     * @return 删除条数
     */
    @Override
    public int deleteQueryLogByIds(String ids) {
        if (StringUtils.isEmpty(ids) || ids.trim().isEmpty()) {
            return 0;
        }
        return tSampQueryLogMapper.deleteBatchIds(Arrays.asList(ids.split(",")));
    }

    /**
     * 实体转对外脱敏视图：只挑客户可见字段，AQL、样本量、接收/拒收数、
     * 不合格数、合格率等内部字段不进入视图
     */
    private TSampScanVo toScanVo(TSampLot lot) {
        return new TSampScanVo(lot.getLotNo(), lot.getProductCode(), lot.getProductName(),
                lot.getStatus(), lot.getConclude(), lot.getGradeName());
    }

    /**
     * 落查询记录：批号、来源、查询时间、命中标志。
     * 查询记录是旁路审计信息，落库失败不能影响客户查询结果。
     */
    private void saveQueryLog(String lotNo, TSampLot lot, String source) {
        try {
            Date now = new Date();
            TSampQueryLog queryLog = new TSampQueryLog();
            queryLog.setId(UUID.randomUUID().toString().replace("-", ""));
            queryLog.setLotNo(lotNo);
            queryLog.setLotId(lot != null && lot.getId() != null ? String.valueOf(lot.getId()) : null);
            queryLog.setQuerySource(source);
            queryLog.setResultFlag(lot != null ? 1 : 0);
            queryLog.setQueryTime(now);
            queryLog.setCreateTime(now);
            tSampQueryLogMapper.insert(queryLog);
        } catch (Exception e) {
            logger.warn("移动端扫码查询记录落库失败，lotNo={}, source={}", lotNo, source, e);
        }
    }
}
