package com.fc.v2.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.shiro.UnavailableSecurityManagerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.mapper.auto.TSampImportBatchMapper;
import com.fc.v2.mapper.auto.TSampImportErrorMapper;
import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.mapper.auto.TSampProductMapper;
import com.fc.v2.model.auto.TSampImportBatch;
import com.fc.v2.model.auto.TSampImportError;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampProduct;
import com.fc.v2.model.auto.TSysUser;
import com.fc.v2.service.ITSampImportService;
import com.fc.v2.service.ITSampLotService;
import com.fc.v2.shiro.util.ShiroUtils;
import com.fc.v2.util.DateUtils;
import com.fc.v2.util.StringUtils;

/**
 * 检验批批量导入Service实现
 *
 * 逐行校验：通过的行走 ITSampLotService.insertTSampLot 入库（与手工新增同一套
 * 产品回填、抽样方案匹配、批量校验规则），不通过的行记失败明细（Excel 行号+原因），
 * 最后落一条导入批次记录（批次号、总/成功/失败条数、导入人）。
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TSampImportServiceImpl extends ServiceImpl<TSampImportBatchMapper, TSampImportBatch>
        implements ITSampImportService {

    /** 导入文件列数：检验批号、产品编码、批量、检验类型、报检单位、报检人 */
    private static final int COLUMN_COUNT = 6;

    /** 检验类型取值（与检验批新增页面一致） */
    private static final String INSPECT_TYPE_FACTORY = "出厂";
    private static final String INSPECT_TYPE_ARRIVAL = "到货";

    @Autowired
    private TSampImportErrorMapper tSampImportErrorMapper;

    @Autowired
    private TSampLotMapper tSampLotMapper;

    @Autowired
    private TSampProductMapper tSampProductMapper;

    @Autowired
    private ITSampLotService tSampLotService;

    /**
     * 批量导入检验批：第一行为表头跳过，其后每行一个检验批。
     * 业务校验失败的行记失败明细继续处理下一行；数据库异常等不可预期错误
     * 直接抛出触发整体回滚，不留"导了一半"的中间状态
     *
     * @param fileName 文件名（记入导入批次）
     * @param lines    文件文本行（第一行表头）
     * @return batchNo 导入批次号、total 总条数、success 成功条数、fail 失败条数、
     *         errors 失败明细（List，元素含 rowNo 行号与 reason 原因）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importTSampLot(String fileName, List<String> lines) {
        // 导入人必须是当前实际操作的登录人，取不到就拒绝导入，
        // 避免批次记录上落一个"system"占位人，之后对账时说不清是谁导的
        TSysUser operator;
        try {
            operator = ShiroUtils.getUser();
        } catch (UnavailableSecurityManagerException e) {
            throw new IllegalArgumentException("无法确定导入人，请重新登录后再导入");
        }
        if (operator == null || operator.getUsername() == null
                || operator.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("无法确定导入人，请重新登录后再导入");
        }
        if (lines == null || lines.size() <= 1) {
            throw new IllegalArgumentException("导入文件没有数据行，第一行为表头，从第二行起每行一个检验批");
        }

        String batchId = UUID.randomUUID().toString().replace("-", "");
        List<TSampImportError> errors = new ArrayList<TSampImportError>();
        // 文件内批号查重：同一文件里批号重复只放行第一行，其余记失败
        Set<String> seenLotNos = new HashSet<String>();
        int total = 0;
        int success = 0;

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            total++;
            // Excel 行号：表头占第 1 行，数据从第 2 行起，失败明细里的行号直接对应 Excel
            int rowNo = i + 1;
            try {
                TSampLot lot = buildLot(parseCsvLine(line), seenLotNos);
                tSampLotService.insertTSampLot(lot);
                success++;
            } catch (ImportRowException e) {
                errors.add(newError(batchId, rowNo, e.getFieldName(), e.getMessage()));
            } catch (IllegalArgumentException e) {
                // insertTSampLot 抛出的业务校验（如批量不合法）也记为该行失败
                errors.add(newError(batchId, rowNo, null, e.getMessage()));
            }
        }

        Date now = new Date();
        TSampImportBatch batch = new TSampImportBatch();
        batch.setId(batchId);
        // 批次号后端生成：时间戳定位到秒 + 随机后缀防并发撞号，页面传值一律忽略
        batch.setBatchNo("IMP-" + DateUtils.dateTimeNow(DateUtils.YYYYMMDDHHMMSS)
                + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        batch.setFileName(fileName);
        batch.setTotalCount(total);
        batch.setSuccessCount(success);
        batch.setFailCount(errors.size());
        // 同步导入，跑完即完成
        batch.setStatus(1);
        batch.setOperator(operator.getUsername());
        batch.setFinishTime(now);
        batch.setCreateBy(operator.getUsername());
        batch.setCreateTime(now);
        batch.setDelFlag(0);
        this.baseMapper.insert(batch);

        for (TSampImportError error : errors) {
            tSampImportErrorMapper.insert(error);
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("batchNo", batch.getBatchNo());
        result.put("total", total);
        result.put("success", success);
        result.put("fail", errors.size());
        List<Map<String, Object>> errorList = new ArrayList<Map<String, Object>>();
        for (TSampImportError error : errors) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("rowNo", error.getRowNo());
            item.put("reason", error.getErrorMsg());
            errorList.add(item);
        }
        result.put("errors", errorList);
        return result;
    }

    /**
     * 把一行数据校验并装配成检验批，任一校验不通过直接抛 ImportRowException（带出错字段）
     *
     * @param cols       行内各列（已按 CSV 规则切分）
     * @param seenLotNos 文件内已出现的批号
     * @return 检验批（产品ID已按产品编码解析，派生字段由 insertTSampLot 统一回填）
     */
    private TSampLot buildLot(String[] cols, Set<String> seenLotNos) {
        if (cols.length < COLUMN_COUNT) {
            throw new ImportRowException(null, "列数不足，应为 6 列：检验批号,产品编码,批量,检验类型,报检单位,报检人");
        }
        String lotNo = cols[0].trim();
        String productCode = cols[1].trim();
        String batchQtyText = cols[2].trim();
        String inspectType = cols[3].trim();
        String applyUnit = cols[4].trim();
        String applyBy = cols[5].trim();

        if (StringUtils.isEmpty(lotNo)) {
            throw new ImportRowException("检验批号", "检验批号不能为空");
        }
        if (!seenLotNos.add(lotNo)) {
            throw new ImportRowException("检验批号", "检验批号[" + lotNo + "]在文件内重复");
        }
        Integer lotCount = tSampLotMapper.selectCount(new QueryWrapper<TSampLot>()
                .eq("lot_no", lotNo)
                .eq("del_flag", 0));
        if (lotCount != null && lotCount > 0) {
            throw new ImportRowException("检验批号", "检验批号[" + lotNo + "]已存在");
        }

        if (StringUtils.isEmpty(productCode)) {
            throw new ImportRowException("产品编码", "产品编码不能为空");
        }
        TSampProduct product = tSampProductMapper.selectOne(new QueryWrapper<TSampProduct>()
                .eq("code", productCode)
                .eq("del_flag", 0));
        if (product == null) {
            throw new ImportRowException("产品编码", "产品编码[" + productCode + "]不存在或已删除");
        }
        // 与手工新增一致：停用产品不允许报检
        if (product.getStatus() == null || product.getStatus() != 0) {
            throw new ImportRowException("产品编码", "产品[" + productCode + "]已停用，不允许报检");
        }

        if (StringUtils.isEmpty(batchQtyText)) {
            throw new ImportRowException("批量", "批量不能为空");
        }
        Integer batchQty;
        try {
            batchQty = Integer.valueOf(batchQtyText);
        } catch (NumberFormatException e) {
            throw new ImportRowException("批量", "批量必须为正整数");
        }
        if (batchQty <= 0) {
            throw new ImportRowException("批量", "批量必须为正整数，不能为0或负数");
        }

        if (StringUtils.isEmpty(inspectType)) {
            throw new ImportRowException("检验类型", "检验类型不能为空");
        }
        if (!INSPECT_TYPE_FACTORY.equals(inspectType) && !INSPECT_TYPE_ARRIVAL.equals(inspectType)) {
            throw new ImportRowException("检验类型", "检验类型只能为出厂或到货");
        }

        if (StringUtils.isEmpty(applyUnit)) {
            throw new ImportRowException("报检单位", "报检单位不能为空");
        }
        if (StringUtils.isEmpty(applyBy)) {
            throw new ImportRowException("报检人", "报检人不能为空");
        }

        TSampLot lot = new TSampLot();
        lot.setLotNo(lotNo);
        // 产品ID按编码解析，产品名称/AQL 等派生字段由 insertTSampLot 以产品档案为准回填
        lot.setProductId(product.getId());
        lot.setBatchQty(batchQty);
        lot.setInspectType(inspectType);
        lot.setApplyUnit(applyUnit);
        lot.setApplyBy(applyBy);
        lot.setApplyDate(new Date());
        return lot;
    }

    /**
     * 按 CSV 规则切分一行：双引号包裹的字段内逗号不当分隔符，字段内两个双引号还原为一个，
     * 与台账导出端写出的引号转义规则对应，导出的文件改完能直接导回来
     *
     * @param line 一行文本
     * @return 各列原始值（未 trim）
     */
    private String[] parseCsvLine(String line) {
        List<String> cols = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                cols.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        cols.add(cur.toString());
        return cols.toArray(new String[0]);
    }

    /**
     * 装配一条失败明细
     *
     * @param batchId   导入批次ID
     * @param rowNo     Excel 行号
     * @param fieldName 出错字段
     * @param errorMsg  失败原因
     * @return 失败明细
     */
    private TSampImportError newError(String batchId, int rowNo, String fieldName, String errorMsg) {
        TSampImportError error = new TSampImportError();
        error.setId(UUID.randomUUID().toString().replace("-", ""));
        error.setBatchId(batchId);
        error.setRowNo(rowNo);
        error.setFieldName(fieldName);
        error.setErrorMsg(errorMsg);
        error.setCreateTime(new Date());
        return error;
    }

    /**
     * 查询导入批次记录
     *
     * @param id 导入批次ID
     * @return 导入批次记录
     */
    @Override
    public TSampImportBatch selectImportBatchById(String id) {
        return this.baseMapper.selectOne(new QueryWrapper<TSampImportBatch>()
                .eq("id", id)
                .eq("del_flag", 0));
    }

    /**
     * 查询导入批次记录列表（分页由调用方 PageHelper 统一处理，这里不再硬编码分页）
     *
     * @param queryWrapper 查询条件
     * @return 导入批次记录集合
     */
    @Override
    public List<TSampImportBatch> selectImportBatchList(Wrapper<TSampImportBatch> queryWrapper) {
        QueryWrapper<TSampImportBatch> wrapper = (queryWrapper instanceof QueryWrapper)
                ? (QueryWrapper<TSampImportBatch>) queryWrapper
                : new QueryWrapper<TSampImportBatch>();
        wrapper.eq("del_flag", 0).orderByDesc("create_time");
        return this.baseMapper.selectList(wrapper);
    }

    /**
     * 查询某导入批次的失败明细（按行号升序，与 Excel 行序一致）
     *
     * @param batchId 导入批次ID
     * @return 失败明细集合
     */
    @Override
    public List<TSampImportError> selectImportErrorList(String batchId) {
        return tSampImportErrorMapper.selectList(new QueryWrapper<TSampImportError>()
                .eq("batch_id", batchId)
                .orderByAsc("row_no"));
    }

    /**
     * 行级校验异常：带出错字段，导入循环内捕获后记为该行失败明细
     */
    private static class ImportRowException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private final String fieldName;

        ImportRowException(String fieldName, String message) {
            super(message);
            this.fieldName = fieldName;
        }

        String getFieldName() {
            return fieldName;
        }
    }
}
