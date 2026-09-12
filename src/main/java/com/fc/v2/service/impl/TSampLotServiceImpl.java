package com.fc.v2.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.support.ConvertUtil;
import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.mapper.auto.TSampProductMapper;
import com.fc.v2.mapper.auto.TSampSampleMapper;
import com.fc.v2.mapper.auto.TSampSchemeMapper;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampProduct;
import com.fc.v2.model.auto.TSampSample;
import com.fc.v2.model.auto.TSampScheme;
import com.fc.v2.service.ITSampLotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 检验批Service业务层处理
 *
 * @author fuce
 * @date 2026-09-11
 */
@Service
public class TSampLotServiceImpl extends ServiceImpl<TSampLotMapper, TSampLot> implements ITSampLotService {

    @Autowired
    private TSampSchemeMapper tSampSchemeMapper;

    @Autowired
    private TSampProductMapper tSampProductMapper;

    @Autowired
    private TSampSampleMapper tSampSampleMapper;

    /**
     * 查询检验批
     *
     * @param id 检验批ID
     * @return 检验批
     */
    @Override
    public TSampLot selectTSampLotById(Long id) {
        return this.baseMapper.selectOne(new QueryWrapper<TSampLot>()
                .eq("id", id)
                .eq("del_flag", 0));
    }

    /**
     * 查询检验批列表
     *
     * @param queryWrapper 查询条件
     * @return 检验批集合
     */
    @Override
    public List<TSampLot> selectTSampLotList(Wrapper<TSampLot> queryWrapper) {
        QueryWrapper<TSampLot> wrapper = (queryWrapper instanceof QueryWrapper)
                ? (QueryWrapper<TSampLot>) queryWrapper
                : new QueryWrapper<TSampLot>();
        wrapper.eq("del_flag", 0).orderByDesc("create_time");
        return this.baseMapper.selectList(wrapper);
    }

    /**
     * 按批量匹配抽样方案
     *
     * @param batchQty 批量
     * @return 抽样方案
     */
    @Override
    public TSampScheme matchScheme(Integer batchQty) {
        if (batchQty == null) {
            return null;
        }
        // 批量区间为闭区间 [qty_min, qty_max]，取批量落入的第一档
        List<TSampScheme> list = tSampSchemeMapper.selectList(new QueryWrapper<TSampScheme>()
                .le("qty_min", batchQty)
                .ge("qty_max", batchQty)
                .eq("del_flag", 0)
                .orderByAsc("qty_min")
                .last("limit 1"));
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    /**
     * 新增检验批
     *
     * @param tSampLot 检验批
     * @return 结果
     */
    @Override
    public int insertTSampLot(TSampLot tSampLot) {
        validateBatchQty(tSampLot);
        tSampLot.setDelFlag(0);
        // 新登记的检验批为待抽样
        tSampLot.setStatus(0);
        // 新增报检要求产品为启用状态，停用产品不允许报检
        fillDerivedFields(tSampLot, true);
        return this.baseMapper.insert(tSampLot);
    }

    /**
     * 修改检验批
     *
     * @param tSampLot 检验批
     * @return 结果
     */
    @Override
    public int updateTSampLot(TSampLot tSampLot) {
        TSampLot dbLot = selectTSampLotById(tSampLot.getId());
        if (dbLot == null) {
            return 0;
        }
        validateBatchQty(tSampLot);
        if (isBatchQtyLocked(dbLot)) {
            // 抽样中(1)/待判定(2)/已判定(3)/已关闭(4)的检验批批量与抽样方案冻结：
            // 样本编号按应抽样本量 S01..Sn 生成，判定结论基于抽样方案的 Ac/Re，
            // 抽样开始后再改批量会导致样本量、编号与方案对不上，此处直接拒绝更新
            if (!Objects.equals(dbLot.getBatchQty(), tSampLot.getBatchQty())) {
                return 0;
            }
            // 产品与抽样方案随抽样冻结：置 null 让 MyBatis-Plus 跳过这些列，
            // 避免编辑其他字段时被篡改请求换绑产品、或按新方案表重刷已确定的方案
            tSampLot.setProductId(null);
            tSampLot.setProductCode(null);
            tSampLot.setProductName(null);
            tSampLot.setAql(null);
            tSampLot.setSchemeCode(null);
            tSampLot.setSampleSize(null);
            tSampLot.setAcceptCount(null);
            tSampLot.setRejectCount(null);
        } else {
            // 换绑产品时才强制要求启用：未换产品的普通编辑不受产品后续停用影响
            boolean requireEnabled = !Objects.equals(dbLot.getProductId(), tSampLot.getProductId());
            fillDerivedFields(tSampLot, requireEnabled);
        }
        return this.baseMapper.update(tSampLot, new UpdateWrapper<TSampLot>()
                .eq("id", tSampLot.getId())
                .eq("del_flag", 0));
    }

    /**
     * 抽样开始（抽样中及之后状态）后批量是否锁定
     *
     * @param dbLot 库中检验批
     * @return true=批量不可再改
     */
    @Override
    public boolean isBatchQtyLocked(TSampLot dbLot) {
        return dbLot.getStatus() != null && dbLot.getStatus() >= 1;
    }

    /**
     * 关闭检验批：只有已判定(3)的批可以关闭，关闭后状态为已关闭(4)且检测记录冻结
     *
     * @param id 检验批ID
     * @return 结果
     */
    @Override
    public int closeTSampLot(Long id) {
        TSampLot dbLot = selectTSampLotById(id);
        if (dbLot == null) {
            return 0;
        }
        if (dbLot.getStatus() == null || dbLot.getStatus() != 3) {
            throw new IllegalArgumentException("只有已判定的检验批才允许关闭");
        }
        return this.baseMapper.update(null, new UpdateWrapper<TSampLot>()
                .eq("id", id)
                .eq("del_flag", 0)
                .set("status", 4));
    }

    /**
     * 批量删除检验批
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    public int deleteTSampLotByIds(String ids) {
        Long[] idArr = ConvertUtil.toLongArray(ids);
        // 已录入样本的检验批不允许直接删除，避免样本检测记录失去归属；先删样本再删批
        Integer sampleCount = tSampSampleMapper.selectCount(new QueryWrapper<TSampSample>()
                .in("lot_id", Arrays.asList(idArr))
                .eq("del_flag", 0));
        if (sampleCount != null && sampleCount > 0) {
            throw new IllegalArgumentException("该检验批下存在样本检测记录，不允许删除");
        }
        return this.baseMapper.deleteBatchIds(Arrays.asList(idArr));
    }

    /**
     * 删除检验批信息
     *
     * @param id 检验批ID
     * @return 结果
     */
    @Override
    public int deleteTSampLotById(Long id) {
        return this.baseMapper.deleteById(id);
    }

    /**
     * 校验批量：必填且必须为正整数。批量为 0/负数时没有任何抽样方案可以匹配，
     * 存进去只会得到一条样本量为空的脏数据，因此在落库前直接拒绝
     *
     * @param tSampLot 检验批
     */
    private void validateBatchQty(TSampLot tSampLot) {
        Integer batchQty = tSampLot.getBatchQty();
        if (batchQty == null) {
            throw new IllegalArgumentException("批量不能为空");
        }
        if (batchQty <= 0) {
            throw new IllegalArgumentException("批量必须为正整数，不能为0或负数");
        }
    }

    /**
     * 填充派生字段：冗余产品编号/名称/AQL，并按批量匹配抽样方案填充字码、样本量、接收数、拒收数
     *
     * @param tSampLot       检验批
     * @param requireEnabled 是否要求产品为启用状态（新增报检/换绑产品时为 true）
     */
    private void fillDerivedFields(TSampLot tSampLot, boolean requireEnabled) {
        fillProductFields(tSampLot, requireEnabled);
        TSampScheme scheme = matchScheme(tSampLot.getBatchQty());
        if (scheme != null) {
            tSampLot.setSchemeCode(scheme.getCodeLetter());
            tSampLot.setSampleSize(scheme.getSampleSize());
            tSampLot.setAcceptCount(scheme.getAcceptCount());
            tSampLot.setRejectCount(scheme.getRejectCount());
        }
    }

    /**
     * 填充产品派生字段：产品编号/名称/AQL 一律以产品档案为准。
     * 页面提交的 productCode/productName/aql 可被篡改成任意值，此处强制用档案值覆盖，
     * 保证检验批上的产品信息与档案一致；产品不存在/已删除/已停用时直接拒绝落库
     *
     * @param tSampLot       检验批
     * @param requireEnabled 是否要求产品为启用状态（新增报检/换绑产品时为 true）
     */
    private void fillProductFields(TSampLot tSampLot, boolean requireEnabled) {
        if (tSampLot.getProductId() == null) {
            throw new IllegalArgumentException("报检产品不能为空");
        }
        TSampProduct product = tSampProductMapper.selectOne(new QueryWrapper<TSampProduct>()
                .eq("id", tSampLot.getProductId())
                .eq("del_flag", 0));
        if (product == null) {
            throw new IllegalArgumentException("报检产品不存在或已删除");
        }
        if (requireEnabled && (product.getStatus() == null || product.getStatus() != 0)) {
            throw new IllegalArgumentException("产品[" + product.getCode() + " " + product.getName() + "]已停用，不允许报检");
        }
        tSampLot.setProductCode(product.getCode());
        tSampLot.setProductName(product.getName());
        tSampLot.setAql(product.getAql());
    }
}
