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
 * 检验批对象 t_samp_lot
 *
 * @author fuce
 * @date 2026-09-11
 */
@TableName("t_samp_lot")
@ApiModel(value = "TSampLot", description = "检验批")
public class TSampLot implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 检验批号 */
    @TableField("lot_no")
    @ApiModelProperty(value = "检验批号")
    private String lotNo;

    /** 产品ID */
    @TableField("product_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "产品ID")
    private Long productId;

    /** 产品编号（冗余） */
    @TableField("product_code")
    @ApiModelProperty(value = "产品编号")
    private String productCode;

    /** 产品名称（冗余） */
    @TableField("product_name")
    @ApiModelProperty(value = "产品名称")
    private String productName;

    /** 批量 */
    @TableField("batch_qty")
    @ApiModelProperty(value = "批量")
    private Integer batchQty;

    /** 检验类型 出厂/到货 */
    @TableField("inspect_type")
    @ApiModelProperty(value = "检验类型")
    private String inspectType;

    /** 报检单位 */
    @TableField("apply_unit")
    @ApiModelProperty(value = "报检单位")
    private String applyUnit;

    /** 报检人 */
    @TableField("apply_by")
    @ApiModelProperty(value = "报检人")
    private String applyBy;

    /** 报检日期 */
    @TableField("apply_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "报检日期")
    private Date applyDate;

    /** 样本量字码 */
    @TableField("scheme_code")
    @ApiModelProperty(value = "样本量字码")
    private String schemeCode;

    /** 接收质量限 */
    @TableField("aql")
    @ApiModelProperty(value = "接收质量限")
    private java.math.BigDecimal aql;

    /** 应抽样本量 n */
    @TableField("sample_size")
    @ApiModelProperty(value = "应抽样本量")
    private Integer sampleSize;

    /** 接收数 Ac */
    @TableField("accept_count")
    @ApiModelProperty(value = "接收数")
    private Integer acceptCount;

    /** 拒收数 Re */
    @TableField("reject_count")
    @ApiModelProperty(value = "拒收数")
    private Integer rejectCount;

    /** 不合格样本数（样本检测汇总） */
    @TableField("defect_count")
    @ApiModelProperty(value = "不合格样本数")
    private Integer defectCount;

    /** 合格率（样本检测汇总） */
    @TableField("pass_rate")
    @ApiModelProperty(value = "合格率")
    private java.math.BigDecimal passRate;

    /** 状态 0待抽样 1抽样中 2待判定 3已判定 4已关闭 */
    @TableField("status")
    @ApiModelProperty(value = "状态 0待抽样 1抽样中 2待判定 3已判定 4已关闭")
    private Integer status;

    /** 判定结论 合格/不合格 */
    @TableField("conclude")
    @ApiModelProperty(value = "判定结论")
    private String conclude;

    /** 质量等级字码（质量定等回写） */
    @TableField("grade_code")
    @ApiModelProperty(value = "质量等级字码")
    private String gradeCode;

    /** 质量等级名称（质量定等回写） */
    @TableField("grade_name")
    @ApiModelProperty(value = "质量等级名称")
    private String gradeName;

    /** 要求完成天数 */
    @TableField("require_days")
    @ApiModelProperty(value = "要求完成天数")
    private Integer requireDays;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLotNo() {
        return lotNo;
    }

    public void setLotNo(String lotNo) {
        this.lotNo = lotNo;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getBatchQty() {
        return batchQty;
    }

    public void setBatchQty(Integer batchQty) {
        this.batchQty = batchQty;
    }

    public String getInspectType() {
        return inspectType;
    }

    public void setInspectType(String inspectType) {
        this.inspectType = inspectType;
    }

    public String getApplyUnit() {
        return applyUnit;
    }

    public void setApplyUnit(String applyUnit) {
        this.applyUnit = applyUnit;
    }

    public String getApplyBy() {
        return applyBy;
    }

    public void setApplyBy(String applyBy) {
        this.applyBy = applyBy;
    }

    public Date getApplyDate() {
        return applyDate;
    }

    public void setApplyDate(Date applyDate) {
        this.applyDate = applyDate;
    }

    public String getSchemeCode() {
        return schemeCode;
    }

    public void setSchemeCode(String schemeCode) {
        this.schemeCode = schemeCode;
    }

    public java.math.BigDecimal getAql() {
        return aql;
    }

    public void setAql(java.math.BigDecimal aql) {
        this.aql = aql;
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

    public Integer getDefectCount() {
        return defectCount;
    }

    public void setDefectCount(Integer defectCount) {
        this.defectCount = defectCount;
    }

    public java.math.BigDecimal getPassRate() {
        return passRate;
    }

    public void setPassRate(java.math.BigDecimal passRate) {
        this.passRate = passRate;
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

    public Integer getRequireDays() {
        return requireDays;
    }

    public void setRequireDays(Integer requireDays) {
        this.requireDays = requireDays;
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
}
