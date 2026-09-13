package com.fc.v2.service.impl;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.support.ConvertUtil;
import com.fc.v2.mapper.auto.TSampUrgeMapper;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampUrge;
import com.fc.v2.model.auto.TSysUser;
import com.fc.v2.service.ITSampLotService;
import com.fc.v2.service.ITSampUrgeService;
import com.fc.v2.shiro.util.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 检验批超期催办记录Service业务层处理
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TSampUrgeServiceImpl extends ServiceImpl<TSampUrgeMapper, TSampUrge> implements ITSampUrgeService {

    @Autowired
    private ITSampLotService tSampLotService;

    /**
     * 查询催办记录
     *
     * @param id 主键
     * @return 催办记录
     */
    @Override
    public TSampUrge selectTSampUrgeById(Long id) {
        TSampUrge urge = this.baseMapper.selectOne(new QueryWrapper<TSampUrge>()
                .eq("id", id)
                .eq("del_flag", 0));
        if (urge != null) {
            fillLotInfo(Arrays.asList(urge));
        }
        return urge;
    }

    /**
     * 查询催办记录列表
     *
     * @param queryWrapper 查询条件
     * @return 催办记录集合
     */
    @Override
    public List<TSampUrge> selectTSampUrgeList(Wrapper<TSampUrge> queryWrapper) {
        QueryWrapper<TSampUrge> wrapper = (queryWrapper instanceof QueryWrapper)
                ? (QueryWrapper<TSampUrge>) queryWrapper
                : new QueryWrapper<TSampUrge>();
        wrapper.eq("del_flag", 0).orderByDesc("create_time");
        List<TSampUrge> list = this.baseMapper.selectList(wrapper);
        fillLotInfo(list);
        return list;
    }

    /**
     * 发起催办：只有当前仍在超期的检验批允许催办；催办单号后端生成、催办次数按批递增、
     * 超期天数取催办时的计算值、催办人取当前登录人、催办时间取当前时间，前端传值一律忽略；
     * 同一催办人同一天对同一检验批只记一次
     *
     * @param tSampUrge 催办记录（lotId 必填）
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertTSampUrge(TSampUrge tSampUrge) {
        if (tSampUrge.getLotId() == null) {
            throw new IllegalArgumentException("检验批不能为空");
        }
        TSampLot lot = tSampLotService.selectTSampLotById(tSampUrge.getLotId());
        if (lot == null) {
            throw new IllegalArgumentException("检验批不存在或已删除");
        }
        // 未超期的批不需要催办：已判定/已关闭或仍在要求完成期限内
        int overdueDays = tSampLotService.calcOverdueDays(lot);
        if (overdueDays <= 0) {
            throw new IllegalArgumentException("检验批[" + lot.getLotNo() + "]未超期，无需催办");
        }
        // 催办人必须是当前实际发起催办的登录人：不能取检验批上的报检人(lot.apply_by)，
        // 也不能用 ShiroUtils 在无登录上下文时返回的固定兜底值"Task"。
        // 这里直接取登录主体并显式校验，取不到登录人就拒绝催办，避免把错误的人写进催办记录
        TSysUser urgeUser;
        try {
            urgeUser = ShiroUtils.getUser();
        } catch (org.apache.shiro.UnavailableSecurityManagerException e) {
            throw new IllegalArgumentException("无法确定催办人，请重新登录后再发起催办");
        }
        if (urgeUser == null || urgeUser.getUsername() == null
                || urgeUser.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("无法确定催办人，请重新登录后再发起催办");
        }
        // 同一催办人同一天对同一检验批只记一次，防止重复催办刷屏
        Date dayStart = Timestamp.valueOf(LocalDate.now().atStartOfDay());
        Integer urgedToday = this.baseMapper.selectCount(new QueryWrapper<TSampUrge>()
                .eq("lot_id", lot.getId())
                .eq("urge_by", urgeUser.getUsername())
                .ge("urge_time", dayStart)
                .eq("del_flag", 0));
        if (urgedToday != null && urgedToday > 0) {
            throw new IllegalArgumentException("今天已对该检验批发起过催办，请勿重复催办");
        }
        // 催办次数按批递增：取该批已有催办的最大次数 + 1
        List<TSampUrge> lastList = this.baseMapper.selectList(new QueryWrapper<TSampUrge>()
                .select("urge_count")
                .eq("lot_id", lot.getId())
                .eq("del_flag", 0)
                .orderByDesc("urge_count")
                .last("limit 1"));
        int nextCount = 1;
        if (!lastList.isEmpty() && lastList.get(0).getUrgeCount() != null) {
            nextCount = lastList.get(0).getUrgeCount() + 1;
        }
        // 催办单号后端生成，前端传值一律忽略
        tSampUrge.setUrgeNo("CB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        tSampUrge.setUrgeCount(nextCount);
        tSampUrge.setOverdueDays(overdueDays);
        tSampUrge.setUrgeBy(urgeUser.getUsername());
        tSampUrge.setUrgeTime(new Date());
        tSampUrge.setDelFlag(0);
        return this.baseMapper.insert(tSampUrge);
    }

    /**
     * 批量删除催办记录
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    public int deleteTSampUrgeByIds(String ids) {
        Long[] idArr = ConvertUtil.toLongArray(ids);
        return this.baseMapper.deleteBatchIds(Arrays.asList(idArr));
    }

    /**
     * 删除催办记录
     *
     * @param id 主键
     * @return 结果
     */
    @Override
    public int deleteTSampUrgeById(Long id) {
        return this.baseMapper.deleteById(id);
    }

    /**
     * 批量回填检验批号/产品名称（关联展示用，催办记录本身不冗余）
     *
     * @param list 催办记录集合
     */
    private void fillLotInfo(List<TSampUrge> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> lotIds = new HashSet<Long>();
        for (TSampUrge urge : list) {
            if (urge.getLotId() != null) {
                lotIds.add(urge.getLotId());
            }
        }
        if (lotIds.isEmpty()) {
            return;
        }
        List<TSampLot> lots = tSampLotService.selectTSampLotList(new QueryWrapper<TSampLot>()
                .in("id", new ArrayList<Long>(lotIds)));
        for (TSampUrge urge : list) {
            for (TSampLot lot : lots) {
                if (urge.getLotId() != null && urge.getLotId().equals(lot.getId())) {
                    urge.setLotNo(lot.getLotNo());
                    urge.setProductName(lot.getProductName());
                    break;
                }
            }
        }
    }
}
