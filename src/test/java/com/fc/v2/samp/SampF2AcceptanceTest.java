package com.fc.v2.samp;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampSample;
import com.fc.v2.model.auto.TSysUser;
import com.fc.v2.service.ITSampLotService;
import com.fc.v2.service.ITSampSampleService;
import com.github.pagehelper.PageHelper;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * samp F2 样本检测登记与合格判定 · 功能点验收测试
 * create 侧产物，仅质检使用，不交付执行模型。表：t_samp_sample / t_samp_lot / t_samp_scheme。
 * 运行：mvn test -Dtest=SampF2AcceptanceTest -DskipTests=false
 */
@SpringBootTest
public class SampF2AcceptanceTest {

    private static final String LOGIN_NAME = "samp-tester";

    @Resource
    private ITSampSampleService sampleService;

    @Resource
    private ITSampLotService lotService;

    @Resource
    private TSampLotMapper lotMapper;

    @Resource
    private SecurityManager securityManager;

    /** 绑定登录用户（自动填充取登录名），并清空样本与检验批表，消除测试间污染 */
    @BeforeEach
    public void setUp() {
        TSysUser user = new TSysUser();
        user.setUsername(LOGIN_NAME);
        Subject subject = new Subject.Builder(securityManager)
                .principals(new SimplePrincipalCollection(user, "sampRealm"))
                .authenticated(true)
                .buildSubject();
        ThreadContext.bind(subject);

        // 清库前先把检验批状态复位为「待抽样」：模型可能实现「已判定批的检测记录不允许改动」，
        // 直接删样本会被业务校验拦下（走 Mapper 直改绕过 Service 守卫），否则每个用例都在 setUp 崩
        for (TSampLot lot : lotService.selectTSampLotList(new QueryWrapper<TSampLot>())) {
            if (lot.getStatus() != null && lot.getStatus() != 0) {
                TSampLot reset = new TSampLot();
                reset.setId(lot.getId());
                reset.setStatus(0);
                lotMapper.update(reset, new QueryWrapper<TSampLot>().eq("id", lot.getId()));
            }
        }
        for (TSampSample sample : sampleService.selectTSampSampleList(new QueryWrapper<TSampSample>())) {
            sampleService.deleteTSampSampleById(sample.getId());
        }
        for (TSampLot lot : lotService.selectTSampLotList(new QueryWrapper<TSampLot>())) {
            lotService.deleteTSampLotById(lot.getId());
        }
    }

    private TSampLot buildLot(int batchQty) {
        TSampLot lot = new TSampLot();
        lot.setLotNo("T2-" + UUID.randomUUID().toString().substring(0, 8));
        lot.setProductId(1L);
        lot.setProductCode("SAMP-P001");
        lot.setProductName("示例产品甲");
        lot.setBatchQty(batchQty);
        lot.setInspectType("出厂");
        lot.setApplyUnit("一车间");
        lot.setApplyBy("报检员甲");
        lot.setApplyDate(new Date());
        lot.setStatus(0);
        return lot;
    }

    /** 建批并返回库中的检验批（带抽样方案回写结果） */
    private TSampLot newLot(int batchQty) {
        TSampLot lot = buildLot(batchQty);
        assertTrue(lotService.insertTSampLot(lot) > 0, "检验批登记应成功");
        return lotService.selectTSampLotById(lot.getId());
    }

    /** 造一条样本 */
    private TSampSample buildSample(Long lotId, int seq, boolean defect) {
        TSampSample sample = new TSampSample();
        sample.setLotId(lotId);
        sample.setSampleNo("S" + seq);
        sample.setItemCode("IT-W");
        sample.setItemName("重量偏差");
        sample.setStdValue("100±2");
        sample.setMeasuredValue(defect ? "105" : "100");
        sample.setItemResult(defect ? 1 : 0);
        return sample;
    }

    private boolean tryInsert(TSampSample sample) {
        try {
            return sampleService.insertTSampSample(sample) > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    /** 登记 count 条样本，其中前 defect 条为不合格 */
    private void fillSamples(Long lotId, int count, int defect) {
        for (int i = 1; i <= count; i++) {
            assertTrue(tryInsert(buildSample(lotId, i, i <= defect)),
                    "第 " + i + " 条样本应能正常登记");
        }
    }

    /**
     * 坑1 自动填充：新增样本检测记录后 check_by 应是当前登录人，不能是报检人等其它来源
     */
    @Test
    public void trap1_checkBy_should_be_login_user() {
        TSampLot lot = newLot(20);
        assertTrue(tryInsert(buildSample(lot.getId(), 1, false)), "样本应能登记成功");
        List<TSampSample> list = sampleService.selectTSampSampleList(
                new QueryWrapper<TSampSample>().eq("lot_id", lot.getId()));
        assertEquals(1, list.size(), "应查到刚登记的样本");
        assertEquals(LOGIN_NAME, list.get(0).getCheckBy(),
                "check_by 应为当前登录人，实际为 " + list.get(0).getCheckBy());
    }

    /**
     * 坑2 判定边界：不合格样本数恰好等于拒收数时应判不合格（不能漏成合格）
     */
    @Test
    public void trap2_defect_equal_reject_should_be_unqualified() {
        // 批量 20 → 应抽 5、拒收数 1；批量 30 → 应抽 8、拒收数 2
        TSampLot lotA = newLot(20);
        assertEquals(Integer.valueOf(1), lotA.getRejectCount(), "批量 20 的拒收数应为 1");
        fillSamples(lotA.getId(), 5, 1);
        assertEquals("不合格", lotService.selectTSampLotById(lotA.getId()).getConclude(),
                "不合格数等于拒收数时应判不合格");

        TSampLot lotB = newLot(30);
        assertEquals(Integer.valueOf(2), lotB.getRejectCount(), "批量 30 的拒收数应为 2");
        fillSamples(lotB.getId(), 8, 2);
        assertEquals("不合格", lotService.selectTSampLotById(lotB.getId()).getConclude(),
                "不合格数等于拒收数时应判不合格");

        // 对照：全部合格应判合格
        TSampLot lotC = newLot(20);
        fillSamples(lotC.getId(), 5, 0);
        assertEquals("合格", lotService.selectTSampLotById(lotC.getId()).getConclude(),
                "全部合格时应判合格");
    }

    /**
     * 坑3 跨模块不变量：已登记样本数达到应抽样本量后不应再新增
     */
    @Test
    public void trap3_sample_qty_should_not_exceed_sample_size() {
        TSampLot lot = newLot(20);
        Integer planned = lot.getSampleSize();
        assertEquals(Integer.valueOf(5), planned, "批量 20 的应抽样本量应为 5");
        fillSamples(lot.getId(), planned, 0);

        boolean extra = tryInsert(buildSample(lot.getId(), planned + 1, false));
        List<TSampSample> all = sampleService.selectTSampSampleList(
                new QueryWrapper<TSampSample>().eq("lot_id", lot.getId()));
        assertEquals(planned.intValue(), all.size(),
                extra ? "超出应抽样本量的样本被登记进去了" : "样本条数应保持在应抽样本量");
    }

    /**
     * 坑4 参数校验：实测值为空时不应保存（也不能产生脏数据）
     */
    @Test
    public void trap4_empty_measured_value_should_be_rejected() {
        TSampLot lot = newLot(200);

        TSampSample nullValue = buildSample(lot.getId(), 1, false);
        nullValue.setMeasuredValue(null);
        assertFalse(tryInsert(nullValue), "实测值为 null 时不应登记成功");

        TSampSample blankValue = buildSample(lot.getId(), 2, false);
        blankValue.setMeasuredValue("   ");
        assertFalse(tryInsert(blankValue), "实测值为空白时不应登记成功");

        List<TSampSample> all = sampleService.selectTSampSampleList(
                new QueryWrapper<TSampSample>().eq("lot_id", lot.getId()));
        assertEquals(0, all.size(), "被拒的样本不应落库");
    }

    /**
     * 坑5 分页与排序：分页参数应生效；返回的样本应按样本编号升序排列。
     * 编号由服务端按题面口径生成（S01..Sn），测试不自行指定编号值，
     * 否则会与「按应抽数量生成编号」的题面要求冲突、把正确实现判成掉坑。
     */
    @Test
    public void trap5_page_and_order_should_apply() {
        TSampLot lot = newLot(200);
        for (int i = 1; i <= 12; i++) {
            assertTrue(tryInsert(buildSample(lot.getId(), i, false)), "第 " + i + " 条样本应能登记");
        }

        PageHelper.startPage(1, 5);
        List<TSampSample> page = sampleService.selectTSampSampleList(
                new QueryWrapper<TSampSample>().eq("lot_id", lot.getId()));
        assertTrue(page.size() <= 5, "分页未生效：请求每页 5 条却返回 " + page.size() + " 条");

        PageHelper.clearPage();
        List<TSampSample> all = sampleService.selectTSampSampleList(
                new QueryWrapper<TSampSample>().eq("lot_id", lot.getId()));
        assertEquals(12, all.size(), "应能查到该批下的全部 12 条样本");
        for (int i = 1; i < all.size(); i++) {
            String prev = all.get(i - 1).getSampleNo();
            String cur = all.get(i).getSampleNo();
            assertTrue(prev != null && cur != null && prev.compareTo(cur) <= 0,
                    "样本应按编号升序排列，实际 " + prev + " 排在 " + cur + " 之前");
        }
    }

    /**
     * 坑6 状态约束：已判定的检验批不应再新增样本，且判定结论不应被改动
     */
    @Test
    public void trap6_judged_lot_should_not_accept_sample() {
        TSampLot lot = newLot(200);
        // 直接将状态置为已判定
        TSampLot mark = new TSampLot();
        mark.setId(lot.getId());
        mark.setStatus(3);
        mark.setConclude("合格");
        lotMapper.update(mark, new QueryWrapper<TSampLot>().eq("id", lot.getId()));

        assertFalse(tryInsert(buildSample(lot.getId(), 1, true)), "已判定的检验批不应再新增样本");

        TSampLot after = lotService.selectTSampLotById(lot.getId());
        assertEquals("合格", after.getConclude(), "已判定检验批的结论不应变化");
        assertEquals(Integer.valueOf(3), after.getStatus(), "已判定检验批的状态不应变化");
        List<TSampSample> all = sampleService.selectTSampSampleList(
                new QueryWrapper<TSampSample>().eq("lot_id", lot.getId()));
        assertEquals(0, all.size(), "被拒的样本不应落库");
    }
}
