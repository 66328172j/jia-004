package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 检验样本检测记录对象 t_samp_sample
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_samp_sample")
@ApiModel(value = "TSampSample", description = "检验样本检测记录")
public class TSampSample implements Serializable {
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

    /** 样本编号（S01..Sn） */
    @TableField("sample_no")
    @ApiModelProperty(value = "样本编号")
    private String sampleNo;

    /** 检测项编码 */
    @TableField("item_code")
    @ApiModelProperty(value = "检测项编码")
    private String itemCode;

    /** 检测项名称 */
    @TableField("item_name")
    @ApiModelProperty(value = "检测项名称")
    private String itemName;

    /** 标准值/判定依据 */
    @TableField("std_value")
    @ApiModelProperty(value = "标准值")
    private String stdValue;

    /** 实测值 */
    @TableField("measured_value")
    @ApiModelProperty(value = "实测值")
    private String measuredValue;

    /** 单项判定 0合格 1不合格 */
    @TableField("item_result")
    @ApiModelProperty(value = "单项判定 0合格 1不合格")
    private Integer itemResult;

    /** 检测人 */
    @TableField("check_by")
    @ApiModelProperty(value = "检测人")
    private String checkBy;

    /** 检测时间 */
    @TableField("check_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "检测时间")
    private Date checkTime;

    /** 逻辑删除标记（0正常 1删除） */
    @TableField("del_flag")
    @ApiModelProperty(value = "逻辑删除标记（0正常 1删除）")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    /** 检验批号（关联展示用，不落库） */
    @TableField(exist = false)
    @ApiModelProperty(value = "检验批号")
    private String lotNo;

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

    public String getSampleNo() {
        return sampleNo;
    }

    public void setSampleNo(String sampleNo) {
        this.sampleNo = sampleNo;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getStdValue() {
        return stdValue;
    }

    public void setStdValue(String stdValue) {
        this.stdValue = stdValue;
    }

    public String getMeasuredValue() {
        return measuredValue;
    }

    public void setMeasuredValue(String measuredValue) {
        this.measuredValue = measuredValue;
    }

    public Integer getItemResult() {
        return itemResult;
    }

    public void setItemResult(Integer itemResult) {
        this.itemResult = itemResult;
    }

    public String getCheckBy() {
        return checkBy;
    }

    public void setCheckBy(String checkBy) {
        this.checkBy = checkBy;
    }

    public Date getCheckTime() {
        return checkTime;
    }

    public void setCheckTime(Date checkTime) {
        this.checkTime = checkTime;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
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
}
