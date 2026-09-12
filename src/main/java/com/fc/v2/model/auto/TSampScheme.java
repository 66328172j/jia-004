package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 抽样方案对象 t_samp_scheme（批量区间闭区间，一般检验水平II）
 *
 * @author fuce
 * @date 2026-09-11
 */
@TableName("t_samp_scheme")
@ApiModel(value = "TSampScheme", description = "抽样方案")
public class TSampScheme implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 批量下界（含） */
    @TableField("qty_min")
    @ApiModelProperty(value = "批量下界（含）")
    private Integer qtyMin;

    /** 批量上界（含） */
    @TableField("qty_max")
    @ApiModelProperty(value = "批量上界（含）")
    private Integer qtyMax;

    /** 样本量字码 */
    @TableField("code_letter")
    @ApiModelProperty(value = "样本量字码")
    private String codeLetter;

    /** 样本量 n */
    @TableField("sample_size")
    @ApiModelProperty(value = "样本量")
    private Integer sampleSize;

    /** 接收数 Ac */
    @TableField("accept_count")
    @ApiModelProperty(value = "接收数")
    private Integer acceptCount;

    /** 拒收数 Re */
    @TableField("reject_count")
    @ApiModelProperty(value = "拒收数")
    private Integer rejectCount;

    /** 逻辑删除标记（0正常 1删除） */
    @TableField("del_flag")
    @ApiModelProperty(value = "逻辑删除标记（0正常 1删除）")
    private Integer delFlag;

    /** 创建者 */
    @TableField("create_by")
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField("create_time")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField("update_by")
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField("update_time")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getQtyMin() {
        return qtyMin;
    }

    public void setQtyMin(Integer qtyMin) {
        this.qtyMin = qtyMin;
    }

    public Integer getQtyMax() {
        return qtyMax;
    }

    public void setQtyMax(Integer qtyMax) {
        this.qtyMax = qtyMax;
    }

    public String getCodeLetter() {
        return codeLetter;
    }

    public void setCodeLetter(String codeLetter) {
        this.codeLetter = codeLetter;
    }

    public Integer getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(Integer sampleSize) {
        this.sampleSize = sampleSize;
    }

    public Integer getAcceptCount() {
        return acceptCount;
    }

    public void setAcceptCount(Integer acceptCount) {
        this.acceptCount = acceptCount;
    }

    public Integer getRejectCount() {
        return rejectCount;
    }

    public void setRejectCount(Integer rejectCount) {
        this.rejectCount = rejectCount;
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
}
