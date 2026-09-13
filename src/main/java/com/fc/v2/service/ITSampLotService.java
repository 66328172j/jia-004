package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;

import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampScheme;

/**
 * 检验批Service接口
 *
 * @author fuce
 * @date 2026-09-11
 */
public interface ITSampLotService extends IService<TSampLot> {

    /**
     * 查询检验批
     *
     * @param id 检验批ID
     * @return 检验批
     */
    public TSampLot selectTSampLotById(Long id);

    /**
     * 查询检验批列表
     *
     * @param queryWrapper 查询条件
     * @return 检验批集合
     */
    public List<TSampLot> selectTSampLotList(Wrapper<TSampLot> queryWrapper);

    /**
     * 新增检验批
     *
     * @param tSampLot 检验批
     * @return 结果
     */
    public int insertTSampLot(TSampLot tSampLot);

    /**
     * 修改检验批
     *
     * @param tSampLot 检验批
     * @return 结果
     */
    public int updateTSampLot(TSampLot tSampLot);

    /**
     * 判定完成（已判定/已关闭）后批量是否锁定
     *
     * @param dbLot 库中检验批
     * @return true=批量不可再改
     */
    public boolean isBatchQtyLocked(TSampLot dbLot);

    /**
     * 关闭检验批：仅已判定的检验批可关闭，关闭后状态流转为已关闭
     *
     * @param id 检验批ID
     * @return 结果
     */
    public int closeTSampLot(Long id);

    /**
     * 批量删除检验批
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteTSampLotByIds(String ids);

    /**
     * 删除检验批信息
     *
     * @param id 检验批ID
     * @return 结果
     */
    public int deleteTSampLotById(Long id);

    /**
     * 按批量匹配抽样方案（批量区间为闭区间）
     *
     * @param batchQty 批量
     * @return 抽样方案
     */
    public TSampScheme matchScheme(Integer batchQty);

    /**
     * 计算检验批当前超期天数：报检日期（未填取创建时间）+ 要求完成天数为要求完成期限，
     * 超过期限且尚未判定（状态小于已判定）算超期，刚好到期当天不算超期
     *
     * @param lot 检验批
     * @return 超期天数，未超期返回 0
     */
    public int calcOverdueDays(TSampLot lot);

    /**
     * 查询当前仍在超期的检验批（未判定且已超过要求完成天数）
     *
     * @return 超期检验批集合
     */
    public List<TSampLot> selectOverdueLotList();
}
