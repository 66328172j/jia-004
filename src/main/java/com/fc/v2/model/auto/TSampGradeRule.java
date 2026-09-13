package com.fc.v2.model.auto;

import java.io.Serializable;
import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;

/**
 * 质量定等规则
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_samp_grade_rule")
public class TSampGradeRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId
    @ApiModelProperty(value = "主键")
    private String id;

    /** 等级编码 */
    @TableField("grade_code")
    @ApiModelProperty(value = "等级编码")
    private String gradeCode;

    /** 等级名称 */
    @TableField("grade_name")
    @ApiModelProperty(value = "等级名称")
    private String gradeName;

    /** 合格率下限（含） */
    @TableField("min_pass_rate")
    @ApiModelProperty(value = "合格率下限（含）")
    private BigDecimal minPassRate;

    /** 不合格项数上限（含） */
    @TableField("max_defect_qty")
    @ApiModelProperty(value = "不合格项数上限（含）")
    private Integer maxDefectQty;

    /** 排序（小的优先命中） */
    @TableField("sort")
    @ApiModelProperty(value = "排序（小的优先命中）")
    private Integer sort;

    /** 状态 0启用 1停用 */
    @TableField("status")
    @ApiModelProperty(value = "状态 0启用 1停用")
    private Integer status;

    /** 创建者 */
    @TableField("create_by")
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField("create_time")
    @ApiModelProperty(value = "创建时间")
    private java.util.Date createTime;

    /** 更新者 */
    @TableField("update_by")
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField("update_time")
    @ApiModelProperty(value = "更新时间")
    private java.util.Date updateTime;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGradeCode() {
        return gradeCode;
    }

    public void setGradeCode(String gradeCode) {
        this.gradeCode = gradeCode;
    }

    public String getGradeName() {
        return gradeName;
    }

    public void setGradeName(String gradeName) {
        this.gradeName = gradeName;
    }

    public BigDecimal getMinPassRate() {
        return minPassRate;
    }

    public void setMinPassRate(BigDecimal minPassRate) {
        this.minPassRate = minPassRate;
    }

    public Integer getMaxDefectQty() {
        return maxDefectQty;
    }

    public void setMaxDefectQty(Integer maxDefectQty) {
        this.maxDefectQty = maxDefectQty;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public java.util.Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(java.util.Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public java.util.Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(java.util.Date updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
