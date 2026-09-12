package com.fc.v2.model.auto;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 检验批复检记录 t_samp_retest
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_samp_retest")
@ApiModel(value = "TSampRetest", description = "检验批复检记录")
public class TSampRetest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 检验批ID */
    @TableField("lot_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "检验批ID")
    private Long lotId;

    /** 复检单号 */
    @TableField("retest_no")
    @ApiModelProperty(value = "复检单号")
    private String retestNo;

    /** 复检原因 */
    @TableField("reason")
    @ApiModelProperty(value = "复检原因")
    private String reason;

    /** 复检样本量（加倍复检） */
    @TableField("sample_qty")
    @ApiModelProperty(value = "复检样本量")
    private Integer sampleQty;

    /** 原判定 0合格 1不合格 */
    @TableField("origin_result")
    @ApiModelProperty(value = "原判定 0合格 1不合格")
    private Integer originResult;

    /** 复检判定 0合格 1不合格 */
    @TableField("retest_result")
    @ApiModelProperty(value = "复检判定 0合格 1不合格")
    private Integer retestResult;

    /** 复检人 */
    @TableField("retest_by")
    @ApiModelProperty(value = "复检人")
    private String retestBy;

    /** 复检时间 */
    @TableField("retest_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "复检时间")
    private Date retestTime;

    /** 状态 0待复检 1已完成 2已作废 */
    @TableField("status")
    @ApiModelProperty(value = "状态 0待复检 1已完成 2已作废")
    private Integer status;

    /** 创建者 */
    @TableField(value = "create_by", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = com.baomidou.mybatisplus.annotation.FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = com.baomidou.mybatisplus.annotation.FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    /** 检验批号（关联展示用，不落库） */
    @TableField(exist = false)
    @ApiModelProperty(value = "检验批号")
    private String lotNo;

    /** 产品名称（关联展示用，不落库） */
    @TableField(exist = false)
    @ApiModelProperty(value = "产品名称")
    private String productName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLotId() {
        return lotId;
    }

    public void setLotId(Long lotId) {
        this.lotId = lotId;
    }

    public String getRetestNo() {
        return retestNo;
    }

    public void setRetestNo(String retestNo) {
        this.retestNo = retestNo;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getSampleQty() {
        return sampleQty;
    }

    public void setSampleQty(Integer sampleQty) {
        this.sampleQty = sampleQty;
    }

    public Integer getOriginResult() {
        return originResult;
    }

    public void setOriginResult(Integer originResult) {
        this.originResult = originResult;
    }

    public Integer getRetestResult() {
        return retestResult;
    }

    public void setRetestResult(Integer retestResult) {
        this.retestResult = retestResult;
    }

    public String getRetestBy() {
        return retestBy;
    }

    public void setRetestBy(String retestBy) {
        this.retestBy = retestBy;
    }

    public Date getRetestTime() {
        return retestTime;
    }

    public void setRetestTime(Date retestTime) {
        this.retestTime = retestTime;
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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
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

    public String getLotNo() {
        return lotNo;
    }

    public void setLotNo(String lotNo) {
        this.lotNo = lotNo;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }
}
