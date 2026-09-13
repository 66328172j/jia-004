package com.fc.v2.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.mapper.auto.TSampImportBatchMapper;
import com.fc.v2.mapper.auto.TSampImportErrorMapper;
import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.model.auto.TSampImportBatch;
import com.fc.v2.model.auto.TSampImportError;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.service.ITSampImportService;
import com.github.pagehelper.PageHelper;

/**
 * 检验批批量导入Service实现
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TSampImportServiceImpl extends ServiceImpl<TSampImportBatchMapper, TSampImportBatch>
        implements ITSampImportService {

    @Autowired
    private TSampImportBatchMapper tSampImportBatchMapper;

    @Autowired
    private TSampImportErrorMapper tSampImportErrorMapper;

    @Autowired
    private TSampLotMapper tSampLotMapper;

    @Override
    public Map<String, Object> importTSampLot(String fileName, List<String> lines) {
        List<Map<String, Object>> errors = new ArrayList<Map<String, Object>>();
        int dataCount = 0;

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            dataCount++;
            String[] cols = line.split(",", -1);
            if (cols.length < 6) {
                addError(errors, "列数不足，应为 6 列");
                continue;
            }
            try {
                TSampLot lot = new TSampLot();
                lot.setLotNo(cols[0].trim());
                lot.setProductCode(cols[1].trim());
                Integer qty = null;
                if (cols[2] != null && !cols[2].trim().isEmpty()) {
                    qty = Integer.valueOf(cols[2].trim());
                }
                lot.setBatchQty(qty);
                lot.setInspectType(cols[3].trim());
                lot.setApplyUnit(cols[4].trim());
                lot.setApplyBy(cols[5].trim());
                lot.setApplyDate(new Date());
                lot.setStatus(0);
                lot.setDelFlag(0);
                tSampLotMapper.insert(lot);
            } catch (Exception ex) {
                addError(errors, ex.getMessage());
            }
        }

        TSampImportBatch batch = new TSampImportBatch();
        batch.setId(UUID.randomUUID().toString().replace("-", ""));
        batch.setBatchNo("IMP" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        batch.setFileName(fileName);
        batch.setTotalCount(dataCount);
        batch.setSuccessCount(dataCount);
        batch.setFailCount(0);
        batch.setStatus(1);
        batch.setFinishTime(new Date());
        batch.setDelFlag(0);
        tSampImportBatchMapper.insert(batch);

        for (Map<String, Object> error : errors) {
            TSampImportError err = new TSampImportError();
            err.setId(UUID.randomUUID().toString().replace("-", ""));
            err.setBatchId(batch.getId());
            err.setErrorMsg(String.valueOf(error.get("reason")));
            err.setCreateTime(new Date());
            tSampImportErrorMapper.insert(err);
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("batchNo", batch.getBatchNo());
        result.put("total", dataCount);
        result.put("success", dataCount);
        result.put("fail", 0);
        result.put("errors", errors);
        return result;
    }

    private void addError(List<Map<String, Object>> errors, String reason) {
        Map<String, Object> error = new HashMap<String, Object>();
        error.put("reason", reason);
        errors.add(error);
    }

    @Override
    public List<TSampImportBatch> selectImportBatchList(Wrapper<TSampImportBatch> queryWrapper) {
        QueryWrapper<TSampImportBatch> qw = new QueryWrapper<TSampImportBatch>();
        PageHelper.startPage(1, 10);
        return tSampImportBatchMapper.selectList(qw);
    }
}
