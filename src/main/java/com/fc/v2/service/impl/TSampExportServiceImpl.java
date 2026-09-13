package com.fc.v2.service.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.shiro.UnavailableSecurityManagerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.conf.V2Config;
import com.fc.v2.mapper.auto.TSampExportMapper;
import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.model.auto.TSampExport;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSysUser;
import com.fc.v2.service.ITSampExportService;
import com.fc.v2.shiro.util.ShiroUtils;
import com.fc.v2.util.DateUtils;
import com.fc.v2.util.StringUtils;

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

    @Autowired
    private V2Config v2Config;

    /**
     * 查询导出记录
     *
     * @param id 主键
     * @return 导出记录
     */
    @Override
    public TSampExport selectTSampExportById(String id) {
        return this.baseMapper.selectOne(new QueryWrapper<TSampExport>()
                .eq("id", id)
                .eq("del_flag", 0));
    }

    /**
     * 查询导出记录列表（分页由调用方 PageHelper 统一处理，这里不再硬编码分页）
     *
     * @param queryWrapper 查询条件
     * @return 导出记录集合
     */
    @Override
    public List<TSampExport> selectTSampExportList(Wrapper<TSampExport> queryWrapper) {
        QueryWrapper<TSampExport> wrapper = (queryWrapper instanceof QueryWrapper)
                ? (QueryWrapper<TSampExport>) queryWrapper
                : new QueryWrapper<TSampExport>();
        wrapper.eq("del_flag", 0).orderByDesc("export_time");
        return this.baseMapper.selectList(wrapper);
    }

    /**
     * 按时间范围与检验类型查询待导出的检验批：台账按报检日期归集，
     * 开始日期取当天 00:00、结束日期取次日 00:00（不含），即起止两天都算在内
     *
     * @param beginDate  开始日期
     * @param endDate   结束日期（含当天）
     * @param checkType 检验类型（出厂/到货），空为全部
     * @return 检验批集合
     */
    @Override
    public List<TSampLot> selectLedgerLots(Date beginDate, Date endDate, String checkType) {
        QueryWrapper<TSampLot> qw = new QueryWrapper<TSampLot>();
        qw.eq("del_flag", 0);
        if (StringUtils.isNotEmpty(checkType)) {
            qw.eq("inspect_type", checkType.trim());
        }
        if (beginDate != null) {
            qw.ge("apply_date", startOfDay(beginDate));
        }
        if (endDate != null) {
            qw.lt("apply_date", nextDayStart(endDate));
        }
        qw.orderByAsc("apply_date").orderByAsc("id");
        return tSampLotMapper.selectList(qw);
    }

    /**
     * 导出检验台账：按条件查询检验批，先落导出记录再写台账文件，
     * 文件写失败时删文件并抛异常触发事务回滚，不留"有记录没文件"或"有文件没记录"
     *
     * @param beginDate  开始日期
     * @param endDate   结束日期（含当天）
     * @param checkType 检验类型（出厂/到货），空为全部
     * @return 导出记录（fileName 为导出的文件名）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TSampExport exportLedger(Date beginDate, Date endDate, String checkType) {
        if (beginDate == null) {
            throw new IllegalArgumentException("导出开始日期不能为空");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("导出结束日期不能为空");
        }
        if (startOfDay(beginDate).after(startOfDay(endDate))) {
            throw new IllegalArgumentException("导出开始日期不能晚于结束日期");
        }
        // 导出人必须是当前实际操作的登录人，取不到就拒绝导出，
        // 避免台账记录上落一个"system"占位人，月底对客户账时说不清是谁导的
        TSysUser exportUser;
        try {
            exportUser = ShiroUtils.getUser();
        } catch (UnavailableSecurityManagerException e) {
            throw new IllegalArgumentException("无法确定导出人，请重新登录后再导出");
        }
        if (exportUser == null || exportUser.getUsername() == null
                || exportUser.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("无法确定导出人，请重新登录后再导出");
        }
        List<TSampLot> lots = selectLedgerLots(beginDate, endDate, checkType);

        TSampExport record = new TSampExport();
        record.setId(UUID.randomUUID().toString().replace("-", ""));
        // 批次号后端生成：时间戳定位到秒 + 随机后缀防并发撞号，页面传值一律忽略
        record.setBatchNo("EXP-" + DateUtils.dateTimeNow(DateUtils.YYYYMMDDHHMMSS)
                + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        record.setBeginDate(startOfDay(beginDate));
        record.setEndDate(startOfDay(endDate));
        record.setCheckType(StringUtils.isNotEmpty(checkType) ? checkType.trim() : null);
        record.setExportCount(lots.size());
        record.setExportBy(exportUser.getUsername());
        record.setExportTime(new Date());
        // fileName 只存文件名不存路径，目录迁移/下载拼接时不受绝对路径影响
        record.setFileName("检验台账_" + record.getBatchNo() + ".csv");
        record.setCreateBy(exportUser.getUsername());
        record.setCreateTime(new Date());
        record.setDelFlag(0);
        this.baseMapper.insert(record);

        File file = new File(getExportDir(), record.getFileName());
        try {
            writeLedgerCsv(file, lots);
        } catch (IOException e) {
            file.delete();
            throw new RuntimeException("台账文件写入失败", e);
        }
        return record;
    }

    /**
     * 导出文件所在目录：文件根目录下的 sampExport 子目录，与上传文件同根，便于备份与清理
     *
     * @return 目录
     */
    @Override
    public File getExportDir() {
        File dir = new File(v2Config.getProfile(), "sampExport");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * 写台账 CSV：UTF-8 带 BOM，Excel 双击打开靠 BOM 识别编码，没有 BOM 中文会乱码；
     * 行尾用 CRLF（RFC4180），各版本 Excel 都能正确分行
     *
     * @param file 目标文件
     * @param lots 检验批集合
     */
    private void writeLedgerCsv(File file, List<TSampLot> lots) throws IOException {
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
        try {
            // BOM(0xFEFF) 必须第一个写，Excel 靠它识别 UTF-8，少了中文会乱码
            writer.write(0xFEFF);
            writer.write("检验批号,产品,批量,抽样方案,应抽样本量,不合格数,合格率,判定结论,质量等级");
            writer.write("\r\n");
            for (TSampLot lot : lots) {
                StringBuilder line = new StringBuilder();
                line.append(csv(lot.getLotNo())).append(',')
                        .append(csv(lot.getProductName())).append(',')
                        .append(lot.getBatchQty() == null ? "" : lot.getBatchQty()).append(',')
                        .append(csv(schemeText(lot))).append(',')
                        .append(lot.getSampleSize() == null ? "" : lot.getSampleSize()).append(',')
                        .append(lot.getDefectCount() == null ? "" : lot.getDefectCount()).append(',')
                        .append(lot.getPassRate() == null ? "" : lot.getPassRate().toPlainString() + "%").append(',')
                        .append(csv(lot.getConclude())).append(',')
                        .append(csv(lot.getGradeName()));
                writer.write(line.toString());
                writer.write("\r\n");
            }
            writer.flush();
        } finally {
            writer.close();
        }
    }

    /**
     * 抽样方案展示文本：样本量字码 + 接收质量限，如 字码D/AQL2.5
     *
     * @param lot 检验批
     * @return 抽样方案文本
     */
    private String schemeText(TSampLot lot) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(lot.getSchemeCode())) {
            sb.append("字码").append(lot.getSchemeCode());
        }
        if (lot.getAql() != null) {
            sb.append(sb.length() > 0 ? "/AQL" : "AQL").append(lot.getAql().toPlainString());
        }
        return sb.toString();
    }

    /**
     * CSV 字段转义：含逗号/双引号/换行的字段用双引号包裹，字段内双引号双写，
     * 否则产品名称里的逗号会把一行的列顶错位
     *
     * @param s 字段值
     * @return 转义后的字段
     */
    private String csv(String s) {
        if (s == null) {
            return "";
        }
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }

    /**
     * 取当天 00:00:00
     *
     * @param date 日期时间
     * @return 当天零点
     */
    private Date startOfDay(Date date) {
        LocalDate day = new Timestamp(date.getTime()).toLocalDateTime().toLocalDate();
        return Timestamp.valueOf(day.atStartOfDay());
    }

    /**
     * 取次日 00:00:00（结束日期含当天的右开边界）
     *
     * @param date 日期时间
     * @return 次日零点
     */
    private Date nextDayStart(Date date) {
        LocalDate day = new Timestamp(date.getTime()).toLocalDateTime().toLocalDate().plusDays(1);
        return Timestamp.valueOf(day.atStartOfDay());
    }
}
