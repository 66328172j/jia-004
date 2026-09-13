package com.fc.v2.model.auto;

import java.io.Serializable;

import io.swagger.annotations.ApiModelProperty;

/**
 * 移动端扫码查询·对外视图
 *
 * 对外（客户 / 移动端）只暴露批号、产品、检验状态、判定结论、质量等级五个字段，
 * 抽样方案（AQL、样本量、接收数、拒收数）与内部统计字段不得外泄。
 *
 * @author fuce
 * @date 2026-09-12
 */
public class TSampScanVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 批号 */
    @ApiModelProperty(value = "批号")
    private String lotNo;

    /** 产品编码 */
    @ApiModelProperty(value = "产品编码")
    private String productCode;

    /** 产品名称 */
    @ApiModelProperty(value = "产品名称")
    private String productName;

    /** 检验状态 0待抽样 1抽样中 2待判定 3已判定 4已关闭 */
    @ApiModelProperty(value = "检验状态 0待抽样 1抽样中 2待判定 3已判定 4已关闭")
    private Integer status;

    /** 判定结论 */
    @ApiModelProperty(value = "判定结论")
    private String conclude;

    /** 质量等级名称 */
    @ApiModelProperty(value = "质量等级名称")
    private String gradeName;

    public TSampScanVo() {
    }

    public TSampScanVo(String lotNo, String productCode, String productName,
                       Integer status, String conclude, String gradeName) {
        this.lotNo = lotNo;
        this.productCode = productCode;
        this.productName = productName;
        this.status = status;
        this.conclude = conclude;
        this.gradeName = gradeName;
    }

    public String getLotNo() {
        return lotNo;
    }

    public void setLotNo(String lotNo) {
        this.lotNo = lotNo;
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

    public String getGradeName() {
        return gradeName;
    }

    public void setGradeName(String gradeName) {
        this.gradeName = gradeName;
    }
}
