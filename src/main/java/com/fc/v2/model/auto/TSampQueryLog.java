package com.fc.v2.model.auto;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;

/**
 * 移动端扫码查询记录
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_samp_query_log")
public class TSampQueryLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId
    @ApiModelProperty(value = "主键")
    private String id;

    /** 查询的批号 */
    @TableField("lot_no")
    @ApiModelProperty(value = "查询的批号")
    private String lotNo;

    /** 命中的检验批ID */
    @TableField("lot_id")
    @ApiModelProperty(value = "命中的检验批ID")
    private String lotId;

    /** 查询来源 */
    @TableField("query_source")
    @ApiModelProperty(value = "查询来源")
    private String querySource;

    /** 查询时间 */
    @TableField("query_time")
    @ApiModelProperty(value = "查询时间")
    private Date queryTime;

    /** 是否命中 0未命中 1命中 */
    @TableField("result_flag")
    @ApiModelProperty(value = "是否命中 0未命中 1命中")
    private Integer resultFlag;

    /** 创建时间 */
    @TableField("create_time")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLotNo() {
        return lotNo;
    }

    public void setLotNo(String lotNo) {
        this.lotNo = lotNo;
    }

    public String getLotId() {
        return lotId;
    }

    public void setLotId(String lotId) {
        this.lotId = lotId;
    }

    public String getQuerySource() {
        return querySource;
    }

    public void setQuerySource(String querySource) {
        this.querySource = querySource;
    }

    public Date getQueryTime() {
        return queryTime;
    }

    public void setQueryTime(Date queryTime) {
        this.queryTime = queryTime;
    }

    public Integer getResultFlag() {
        return resultFlag;
    }

    public void setResultFlag(Integer resultFlag) {
        this.resultFlag = resultFlag;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
