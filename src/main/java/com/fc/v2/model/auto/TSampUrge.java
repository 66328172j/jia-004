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
 * 检验批超期催办记录 t_samp_urge
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_samp_urge")
@ApiModel(value = "TSampUrge", description = "检验批超期催办记录")
public class TSampUrge implements Serializable {

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

    /** 催办单号 */
    @TableField("urge_no")
    @ApiModelProperty(value = "催办单号")
    private String urgeNo;

    /** 催办次数（该批第几次催办） */
    @TableField("urge_count")
    @ApiModelProperty(value = "催办次数")
    private Integer urgeCount;

    /** 超期天数（催办时） */
    @TableField("overdue_days")
    @ApiModelProperty(value = "超期天数")
    private Integer overdueDays;

    /** 催办人 */
    @TableField("urge_by")
    @ApiModelProperty(value = "催办人")
    private String urgeBy;

    /** 本次催办时间 */
    @TableField("urge_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "本次催办时间")
    private Date urgeTime;

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

    /** 催办级别 1一般 2紧急 */
    @TableField("urge_level")
    @ApiModelProperty(value = "催办级别 1一般 2紧急")
    private Integer urgeLevel;

    /** 处理状态 0待处理 1已处理 2已关闭 */
    @TableField("status")
    @ApiModelProperty(value = "处理状态 0待处理 1已处理 2已关闭")
    private Integer status;

    /** 处理人 */
    @TableField("handle_by")
    @ApiModelProperty(value = "处理人")
    private String handleBy;

    /** 处理时间 */
    @TableField("handle_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "处理时间")
    private Date handleTime;

    /** 处理意见 */
    @TableField("handle_remark")
    @ApiModelProperty(value = "处理意见")
    private String handleRemark;

    /** 来源 0人工发起 1定时扫描 */
    @TableField("source")
    @ApiModelProperty(value = "来源 0人工发起 1定时扫描")
    private Integer source;

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

    public String getUrgeNo() {
        return urgeNo;
    }

    public void setUrgeNo(String urgeNo) {
        this.urgeNo = urgeNo;
    }

    public Integer getUrgeCount() {
        return urgeCount;
    }

    public void setUrgeCount(Integer urgeCount) {
        this.urgeCount = urgeCount;
    }

    public Integer getOverdueDays() {
        return overdueDays;
    }

    public void setOverdueDays(Integer overdueDays) {
        this.overdueDays = overdueDays;
    }

    public String getUrgeBy() {
        return urgeBy;
    }

    public void setUrgeBy(String urgeBy) {
        this.urgeBy = urgeBy;
    }

    public Date getUrgeTime() {
        return urgeTime;
    }

    public void setUrgeTime(Date urgeTime) {
        this.urgeTime = urgeTime;
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

    public Integer getUrgeLevel() {
        return urgeLevel;
    }

    public void setUrgeLevel(Integer urgeLevel) {
        this.urgeLevel = urgeLevel;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getHandleBy() {
        return handleBy;
    }

    public void setHandleBy(String handleBy) {
        this.handleBy = handleBy;
    }

    public Date getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(Date handleTime) {
        this.handleTime = handleTime;
    }

    public String getHandleRemark() {
        return handleRemark;
    }

    public void setHandleRemark(String handleRemark) {
        this.handleRemark = handleRemark;
    }

    public Integer getSource() {
        return source;
    }

    public void setSource(Integer source) {
        this.source = source;
    }
}
