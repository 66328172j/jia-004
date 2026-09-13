package com.fc.v2.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.mapper.auto.TSampQueryLogMapper;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampQueryLog;
import com.fc.v2.service.ITSampScanService;
import com.github.pagehelper.PageHelper;

/**
 * 移动端扫码查询Service实现
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TSampScanServiceImpl implements ITSampScanService {

    @Resource
    private TSampLotMapper tSampLotMapper;

    @Resource
    private TSampQueryLogMapper tSampQueryLogMapper;

    /** 扫码查询结果缓存：批号 -> 查询结果 */
    private final Map<String, Map<String, Object>> scanCache = new ConcurrentHashMap<>();

    @Override
    public Map<String, Object> scanQuery(String lotNo, String source) {
        Map<String, Object> cached = scanCache.get(lotNo);
        if (cached != null) {
            return cached;
        }

        // 按批号查询检验批
        TSampLot lot = tSampLotMapper.selectOne(new QueryWrapper<TSampLot>().eq("lot_no", lotNo));

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("hit", true);
        result.put("lotNo", lot.getLotNo());
        // 对外直接返回库表实体
        result.put("data", lot);

        scanCache.put(lotNo, result);
        return result;
    }

    @Override
    public List<TSampQueryLog> selectQueryLogList(Wrapper<TSampQueryLog> queryWrapper) {
        PageHelper.startPage(1, 10);
        return tSampQueryLogMapper.selectList(new QueryWrapper<TSampQueryLog>());
    }
}
