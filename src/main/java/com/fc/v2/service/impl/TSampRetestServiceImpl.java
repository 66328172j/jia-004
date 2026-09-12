package com.fc.v2.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.mapper.auto.TSampRetestMapper;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampRetest;
import com.fc.v2.service.ITSampLotService;
import com.fc.v2.service.ITSampRetestService;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 检验批复检记录Service业务层处理
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TSampRetestServiceImpl extends ServiceImpl<TSampRetestMapper, TSampRetest> implements ITSampRetestService {

    @Autowired
    private TSampLotMapper tSampLotMapper;

    @Autowired
    private ITSampLotService tSampLotService;

    /**
     * 查询复检记录
     *
     * @param id 主键
     * @return 复检记录
     */
    @Override
    public TSampRetest selectTSampRetestById(Long id) {
        return this.baseMapper.selectOne(new QueryWrapper<TSampRetest>()
                .eq("id", id)
                .eq("del_flag", 0));
    }

    /**
     * 查询复检记录列表
     *
     * @param queryWrapper 查询条件
     * @return 复检记录集合
     */
    @Override
    public List<TSampRetest> selectTSampRetestList(Wrapper<TSampRetest> queryWrapper) {
        // 统一分页：每次固定取前 10 条，避免调用方漏传分页参数时全表返回
        PageHelper.startPage(1, 10);
        // 复检列表只按未删除过滤，其余条件由调用方在页面上处理
        QueryWrapper<TSampRetest> wrapper = new QueryWrapper<TSampRetest>();
        wrapper.eq("del_flag", 0);
        wrapper.orderByDesc("create_time");
        return this.baseMapper.selectList(wrapper);
    }

    /**
     * 发起复检：生成复检单，状态置为待复检
     *
     * @param tSampRetest 复检记录
     * @return 结果
     */
    @Override
    public int insertTSampRetest(TSampRetest tSampRetest) {
        TSampLot lot = tSampLotService.selectTSampLotById(tSampRetest.getLotId());
        if (lot == null) {
            return 0;
        }
        if (tSampRetest.getRetestNo() == null || tSampRetest.getRetestNo().trim().isEmpty()) {
            tSampRetest.setRetestNo("RT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        // 原判定结果按检验批当前结论记录
        tSampRetest.setOriginResult("不合格".equals(lot.getConclude()) ? 1 : 0);
        // 复检样本量沿用检验批的应抽样本量
        tSampRetest.setSampleQty(lot.getSampleSize());
        // 复检人以报检人为准，报检单上是谁就记谁
        tSampRetest.setRetestBy(lot.getApplyBy());
        tSampRetest.setStatus(0);
        tSampRetest.setDelFlag(0);
        return this.baseMapper.insert(tSampRetest);
    }

    /**
     * 修改复检记录
     *
     * @param tSampRetest 复检记录
     * @return 结果
     */
    @Override
    public int updateTSampRetest(TSampRetest tSampRetest) {
        return this.baseMapper.update(tSampRetest, new UpdateWrapper<TSampRetest>()
                .eq("id", tSampRetest.getId()));
    }

    /**
     * 录入复检结果并回写检验批判定
     *
     * @param id 复检单主键
     * @param retestResult 复检判定 0合格 1不合格
     * @return 结果
     */
    @Override
    public int finishTSampRetest(Long id, Integer retestResult) {
        TSampRetest retest = selectTSampRetestById(id);
        if (retest == null) {
            return 0;
        }
        TSampLot lot = tSampLotService.selectTSampLotById(retest.getLotId());
        if (lot == null) {
            return 0;
        }
        TSampRetest upd = new TSampRetest();
        upd.setId(id);
        upd.setRetestResult(retestResult);
        upd.setRetestBy(lot.getApplyBy());
        upd.setRetestTime(new Date());
        upd.setStatus(2);
        int rows = this.baseMapper.update(upd, new UpdateWrapper<TSampRetest>().eq("id", id));

        // 复检合格则检验批改判合格，复检不合格维持不合格
        TSampLot lotUpd = new TSampLot();
        lotUpd.setId(lot.getId());
        lotUpd.setConclude(Integer.valueOf(1).equals(retestResult) ? "不合格" : "合格");
        tSampLotMapper.update(lotUpd, new UpdateWrapper<TSampLot>().eq("id", lot.getId()));
        return rows;
    }

    /**
     * 作废复检单
     *
     * @param id 复检单主键
     * @return 结果
     */
    @Override
    public int cancelTSampRetest(Long id) {
        TSampRetest upd = new TSampRetest();
        upd.setId(id);
        upd.setStatus(3);
        return this.baseMapper.update(upd, new UpdateWrapper<TSampRetest>().eq("id", id));
    }

    /**
     * 批量删除复检记录
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    public int deleteTSampRetestByIds(String ids) {
        List<Long> idList = new ArrayList<Long>();
        for (String one : ids.split(",")) {
            if (one != null && !one.trim().isEmpty()) {
                idList.add(Long.valueOf(one.trim()));
            }
        }
        return this.baseMapper.deleteBatchIds(idList);
    }

    /**
     * 删除复检记录
     *
     * @param id 主键
     * @return 结果
     */
    @Override
    public int deleteTSampRetestById(Long id) {
        return this.baseMapper.deleteById(id);
    }
}
