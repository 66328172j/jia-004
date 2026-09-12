package com.fc.v2.service.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.mapper.auto.TSampExportMapper;
import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.model.auto.TSampExport;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.service.ITSampExportService;
import com.github.pagehelper.PageHelper;

/**
 * 检验台账导出Service实现
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TSampExportServiceImpl extends ServiceImpl<TSampExportMapper, TSampExport> implements ITSampExportService {

    @Autowired
    private TSampLotMapper tSampLotMapper;

    @Override
    public TSampExport selectTSampExportById(String id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<TSampExport> selectTSampExportList(Wrapper<TSampExport> queryWrapper) {
        QueryWrapper<TSampExport> qw = new QueryWrapper<TSampExport>();
        qw.eq("del_flag", 0);
        PageHelper.startPage(1, 10);
        return baseMapper.selectList(qw);
    }

    @Override
    public List<TSampLot> selectLedgerLots(Date beginDate, Date endDate, String checkType) {
        QueryWrapper<TSampLot> qw = new QueryWrapper<TSampLot>();
        qw.eq("del_flag", 0);
        if (beginDate != null) {
            qw.ge("create_time", beginDate);
        }
        if (endDate != null) {
            qw.lt("create_time", endDate);
        }
        return tSampLotMapper.selectList(qw);
    }

    @Override
    public TSampExport exportLedger(Date beginDate, Date endDate, String checkType) {
        List<TSampLot> lots = selectLedgerLots(beginDate, endDate, checkType);

        TSampExport record = new TSampExport();
        record.setId(UUID.randomUUID().toString().replace("-", ""));
        record.setBatchNo("EXP" + System.currentTimeMillis());
        record.setBeginDate(beginDate);
        record.setEndDate(endDate);
        record.setCheckType(checkType);
        record.setExportCount(lots.size());
        record.setExportBy("system");
        record.setExportTime(new Date());
        record.setDelFlag(0);

        File dir = getExportDir();
        File file = new File(dir, record.getBatchNo() + ".csv");
        record.setFileName(file.getAbsolutePath());

        try {
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
            writer.write("检验批号,产品,批量,抽样方案,应抽样本量,不合格数,合格率,判定结论,质量等级");
            writer.newLine();
            writer.write("导出范围," + nvl(lots.get(0).getLotNo()) + " 至 " + nvl(lots.get(lots.size() - 1).getLotNo()));
            writer.newLine();
            for (TSampLot lot : lots) {
                writer.write(join(lot));
                writer.newLine();
            }
            writer.flush();
            writer.close();
        } catch (Exception ex) {
            throw new RuntimeException("导出失败", ex);
        }
        baseMapper.insert(record);
        return record;
    }

    @Override
    public File getExportDir() {
        File dir = new File(System.getProperty("java.io.tmpdir"), "samp-export");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private String join(TSampLot lot) {
        StringBuilder sb = new StringBuilder();
        sb.append(nvl(lot.getLotNo())).append(",")
                .append(nvl(lot.getProductName())).append(",")
                .append(lot.getBatchQty() == null ? "" : lot.getBatchQty()).append(",")
                .append(nvl(lot.getSchemeCode())).append(",")
                .append(lot.getSampleSize() == null ? "" : lot.getSampleSize()).append(",")
                .append(lot.getDefectCount() == null ? "" : lot.getDefectCount()).append(",")
                .append(lot.getPassRate() == null ? "" : lot.getPassRate()).append(",")
                .append(nvl(lot.getConclude())).append(",")
                .append(nvl(lot.getGradeName()));
        return sb.toString();
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}
