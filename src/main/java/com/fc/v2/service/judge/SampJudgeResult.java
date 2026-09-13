package com.fc.v2.service.judge;

/**
 * 判定输出：只携带判定结论与目标状态，是否落库、何时落库由编排方负责
 *
 * @author fuce
 * @date 2026-09-13
 */
public class SampJudgeResult {

    /** 目标状态，null 表示本次判定不引起状态流转 */
    private Integer status;

    /** 判定结论，null 表示暂无结论（清空原结论） */
    private String conclude;

    public SampJudgeResult() {
    }

    public SampJudgeResult(Integer status, String conclude) {
        this.status = status;
        this.conclude = conclude;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getConclude() {
        return conclude;
    }

    public void setConclude(String conclude) {
        this.conclude = conclude;
    }
}
