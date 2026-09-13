package com.fc.v2.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;

import com.fc.v2.model.auto.TSampImportBatch;

/**
 * 检验批批量导入Service接口
 *
 * 导入文件为文本行集合：第一行为表头，跳过；其后每行一个检验批，
 * 逗号分隔 6 列：批号、产品编码、批量、检验类型、报检单位、报检人。
 * 逐行校验，通过的入库，不通过的记失败明细（含 Excel 行号与原因）。
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITSampImportService extends IService<TSampImportBatch> {

    /**
     * 批量导入检验批
     *
     * @param fileName 文件名（记入导入批次）
     * @param lines    文件文本行（第一行表头）
     * @return batchNo 导入批次号、total 总条数、success 成功条数、fail 失败条数、
     *         errors 失败明细（List，元素含 rowNo 行号与 reason 原因）
     */
    Map<String, Object> importTSampLot(String fileName, List<String> lines);

    /**
     * 查询导入批次记录列表
     *
     * @param queryWrapper 查询条件
     * @return 导入批次记录集合
     */
    List<TSampImportBatch> selectImportBatchList(Wrapper<TSampImportBatch> queryWrapper);
}
