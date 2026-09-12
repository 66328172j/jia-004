package com.fc.v2.samp;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampRetest;
import com.fc.v2.model.auto.TSampSample;
import com.fc.v2.model.auto.TSysUser;
import com.fc.v2.service.ITSampLotService;
import com.fc.v2.service.ITSampRetestService;
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
 * samp F3 复检管理 · 功能点验收测试
 * create 侧产物，仅质检使用，不交付执行模型。表：t_samp_retest / t_samp_lot / t_samp_sample。
 * 运行：mvn test -Dtest=SampF3AcceptanceTest -DskipTests=false
 */
@SpringBootTest
public class SampF3AcceptanceTest {

    private static final String LOGIN_NAME = "samp-tester";

    @Resource
    private ITSampRetestService retestService;

    @Resource
    private ITSampSampleService sampleService;

    @Resource
    private ITSampLotService lotService;

    @Resource
    private TSampLotMapper lotMapper;

    @Resource
    private SecurityManager securityManager;

    /** 绑定登录用户（自动填充取登录名），并清空复检/样本/检验批表，消除测试间污染 */
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
        for (TSampRetest retest : retestService.selectTSampRetestList(new QueryWrapper<TSampRetest>())) {
            retestService.deleteTSampRetestById(retest.getId());
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
        lot.setLotNo("T3-" + UUID.randomUUID().toString().substring(0, 8));
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

    private TSampLot newLot(int batchQty) {
        TSampLot lot = buildLot(batchQty);
        assertTrue(lotService.insertTSampLot(lot) > 0, "检验批登记应成功");
        return lotService.selectTSampLotById(lot.getId());
    }

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

    /** 登记 count 条样本，其中前 defect 条为不合格；登记满额后自动判定 */
    private void fillSamples(Long lotId, int count, int defect) {
        for (int i = 1; i <= count; i++) {
            sampleService.insertTSampSample(buildSample(lotId, i, i <= defect));
        }
    }

    /** 造一个已判定为不合格的检验批（批量 20 → 应抽 5、拒收数 1） */
    private TSampLot newUnqualifiedLot() {
        TSampLot lot = newLot(20);
        fillSamples(lot.getId(), 5, 2);
        TSampLot after = lotService.selectTSampLotById(lot.getId());
        assertEquals("不合格", after.getConclude(), "前置条件：该批应判为不合格");
        return after;
    }

    /** 造一个已判定为合格的检验批（批量 20 → 应抽 5、拒收数 1） */
    private TSampLot newQualifiedLot() {
        TSampLot lot = newLot(20);
        fillSamples(lot.getId(), 5, 0);
        TSampLot after = lotService.selectTSampLotById(lot.getId());
        assertEquals("合格", after.getConclude(), "前置条件：该批应判为合格");
        return after;
    }

    private TSampRetest buildRetest(Long lotId) {
        TSampRetest retest = new TSampRetest();
        retest.setLotId(lotId);
        retest.setRetestNo("RT-TEST-" + UUID.randomUUID().toString().substring(0, 8));
        retest.setReason("取样过程疑似污染，申请复检");
        return retest;
    }

    private boolean tryInsert(TSampRetest retest) {
        try {
            return retestService.insertTSampRetest(retest) > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 坑1 自动填充：发起复检后 retest_by 应是当前登录人，不能是报检人等其它来源
     */
    @Test
    public void trap1_retestBy_should_be_login_user() {
        TSampLot lot = newUnqualifiedLot();
        TSampRetest retest = buildRetest(lot.getId());
        assertTrue(tryInsert(retest), "不合格批应能发起复检");

        TSampRetest saved = retestService.selectTSampRetestById(retest.getId());
        assertNotNull(saved, "复检单应已落库");
        assertEquals(LOGIN_NAME, saved.getRetestBy(),
                "retest_by 应为当前登录人，实际为 " + saved.getRetestBy());
    }

    /**
     * 坑2 跨模块不变量：已判定合格的检验批不应再发起复检
     */
    @Test
    public void trap2_qualified_lot_should_not_allow_retest() {
        TSampLot lot = newQualifiedLot();

        boolean accepted = tryInsert(buildRetest(lot.getId()));
        assertFalse(accepted, "已判定合格的检验批不应能发起复检");

        List<TSampRetest> all = retestService.selectTSampRetestList(
                new QueryWrapper<TSampRetest>());
        int mine = 0;
        for (TSampRetest one : all) {
            if (lot.getId().equals(one.getLotId())) {
                mine++;
            }
        }
        assertEquals(0, mine, "被拒的复检单不应落库");
        assertEquals("合格", lotService.selectTSampLotById(lot.getId()).getConclude(),
                "合格批的判定不应被复检流程改动");
    }

    /**
     * 坑3 复检样本量：按规定应取原应抽样本量的两倍
     */
    @Test
    public void trap3_sample_qty_should_be_double() {
        TSampLot lot = newUnqualifiedLot();
        Integer planned = lot.getSampleSize();
        assertEquals(Integer.valueOf(5), planned, "批量 20 的应抽样本量应为 5");

        TSampRetest retest = buildRetest(lot.getId());
        assertTrue(tryInsert(retest), "不合格批应能发起复检");
        TSampRetest saved = retestService.selectTSampRetestById(retest.getId());
        assertEquals(Integer.valueOf(planned * 2), saved.getSampleQty(),
                "复检样本量应为应抽样本量的两倍，期望 " + (planned * 2) + " 实际 " + saved.getSampleQty());
    }

    /**
     * 坑4 重复校验：同一检验批已有在途复检单时不应再发起
     */
    @Test
    public void trap4_duplicate_retest_should_be_rejected() {
        TSampLot lot = newUnqualifiedLot();
        assertTrue(tryInsert(buildRetest(lot.getId())), "首次发起复检应成功");

        boolean second = tryInsert(buildRetest(lot.getId()));
        assertFalse(second, "已存在在途复检单时不应再发起复检");

        List<TSampRetest> all = retestService.selectTSampRetestList(
                new QueryWrapper<TSampRetest>());
        int mine = 0;
        for (TSampRetest one : all) {
            if (lot.getId().equals(one.getLotId())) {
                mine++;
            }
        }
        assertEquals(1, mine, "同一检验批只应存在一张复检单，实际 " + mine + " 张");
    }

    /**
     * 坑5 查询与分页：复检单号模糊查应生效、分页应生效、已作废单不应出现在列表
     */
    @Test
    public void trap5_query_page_and_cancel_should_apply() {
        for (int i = 1; i <= 12; i++) {
            TSampLot lot = newUnqualifiedLot();
            TSampRetest retest = buildRetest(lot.getId());
            retest.setRetestNo("RT-TEST-" + String.format("%03d", i));
            assertTrue(tryInsert(retest), "第 " + i + " 张复检单应能发起");
        }

        // 模糊查：按完整单号应只命中一条
        List<TSampRetest> hit = retestService.selectTSampRetestList(
                new QueryWrapper<TSampRetest>().like("retest_no", "RT-TEST-005"));
        assertEquals(1, hit.size(), "按复检单号模糊查应只返回 1 条，实际 " + hit.size() + " 条");

        // 分页：每页 5 条
        PageHelper.startPage(1, 5);
        List<TSampRetest> page = retestService.selectTSampRetestList(new QueryWrapper<TSampRetest>());
        assertTrue(page.size() <= 5, "分页未生效：请求每页 5 条却返回 " + page.size() + " 条");

        // 作废：作废后的单子不应再出现在列表
        TSampRetest first = retestService.selectTSampRetestList(
                new QueryWrapper<TSampRetest>().like("retest_no", "RT-TEST-001")).get(0);
        retestService.cancelTSampRetest(first.getId());
        List<TSampRetest> afterCancel = retestService.selectTSampRetestList(
                new QueryWrapper<TSampRetest>());
        for (TSampRetest one : afterCancel) {
            assertNotEquals(Integer.valueOf(3), one.getStatus(),
                    "已作废的复检单不应出现在列表中：" + one.getRetestNo());
        }
    }

    /**
     * 坑6 参数校验：复检原因为空时不应提交成功
     */
    @Test
    public void trap6_empty_reason_should_be_rejected() {
        TSampLot lot = newUnqualifiedLot();

        TSampRetest nullReason = buildRetest(lot.getId());
        nullReason.setReason(null);
        assertFalse(tryInsert(nullReason), "复检原因为 null 时不应提交成功");

        TSampRetest blankReason = buildRetest(lot.getId());
        blankReason.setReason("   ");
        assertFalse(tryInsert(blankReason), "复检原因为空白时不应提交成功");

        List<TSampRetest> all = retestService.selectTSampRetestList(
                new QueryWrapper<TSampRetest>());
        int mine = 0;
        for (TSampRetest one : all) {
            if (lot.getId().equals(one.getLotId())) {
                mine++;
            }
        }
        assertEquals(0, mine, "被拒的复检单不应落库");
    }
}
