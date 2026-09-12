package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;

import com.fc.v2.model.auto.TSampExport;
import com.fc.v2.model.auto.TSampLot;

/**
 * 检验台账导出Service接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITSampExportService extends IService<TSampExport> {

    /**
     * 查询导出记录
     *
     * @param id 主键
     * @return 导出记录
     */
    public TSampExport selectTSampExportById(String id);

    /**
     * 查询导出记录列表
     *
     * @param queryWrapper 查询条件
     * @return 导出记录集合
     */
    public List<TSampExport> selectTSampExportList(Wrapper<TSampExport> queryWrapper);

    /**
     * 按时间范围与检验类型查询待导出的检验批
     *
     * @param beginDate  开始日期
     * @param endDate   结束日期（含当天）
     * @param checkType 检验类型
     * @return 检验批集合
     */
    public List<TSampLot> selectLedgerLots(Date beginDate, Date endDate, String checkType);

    /**
     * 导出检验台账：按条件查询、写出文件并落一条导出记录
     *
     * @param beginDate  开始日期
     * @param endDate   结束日期（含当天）
     * @param checkType 检验类型
     * @return 导出记录（fileName 为导出的文件名）
     */
    public TSampExport exportLedger(Date beginDate, Date endDate, String checkType);

    /**
     * 导出文件所在目录
     *
     * @return 目录
     */
    public java.io.File getExportDir();
}
