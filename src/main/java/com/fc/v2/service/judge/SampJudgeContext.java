package com.fc.v2.service.judge;

/**
 * 判定输入：各判定口径按需取用，与口径无关的字段保持 null
 *
 * @author fuce
 * @date 2026-09-13
 */
public class SampJudgeContext {

    /** 不合格样本数（一次抽样判定口径） */
    private Integer defect;

    /** 接收数Ac（一次抽样判定口径） */
    private Integer accept;

    /** 拒收数Re（一次抽样判定口径） */
    private Integer reject;

    /** 复检结果 0合格 1不合格（复检判定口径） */
    private Integer retestResult;

    public Integer getDefect() {
        return defect;
    }

    public void setDefect(Integer defect) {
        this.defect = defect;
    }

    public Integer getAccept() {
        return accept;
    }

    public void setAccept(Integer accept) {
        this.accept = accept;
    }

    public Integer getReject() {
        return reject;
    }

    public void setReject(Integer reject) {
        this.reject = reject;
    }

    public Integer getRetestResult() {
        return retestResult;
    }

    public void setRetestResult(Integer retestResult) {
        this.retestResult = retestResult;
    }
}
