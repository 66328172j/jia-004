package com.fc.v2.model.auto;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;

/**
 * 定时任务执行日志
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_samp_job_log")
public class TSampJobLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId
    @ApiModelProperty(value = "主键")
    private String id;

    /** 任务名 */
    @TableField("job_name")
    @ApiModelProperty(value = "任务名")
    private String jobName;

    /** 开始时间 */
    @TableField("start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "开始时间")
    private Date startTime;

    /** 结束时间 */
    @TableField("end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "结束时间")
    private Date endTime;

    /** 扫描条数 */
    @TableField("scan_count")
    @ApiModelProperty(value = "扫描条数")
    private Integer scanCount;

    /** 生成催办条数 */
    @TableField("urge_count")
    @ApiModelProperty(value = "生成催办条数")
    private Integer urgeCount;

    /** 失败条数 */
    @TableField("fail_count")
    @ApiModelProperty(value = "失败条数")
    private Integer failCount;

    /** 执行结果 0成功 1失败 */
    @TableField("status")
    @ApiModelProperty(value = "执行结果 0成功 1失败")
    private Integer status;

    /** 错误信息 */
    @TableField("error_msg")
    @ApiModelProperty(value = "错误信息")
    private String errorMsg;

    /** 创建时间 */
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Integer getScanCount() {
        return scanCount;
    }

    public void setScanCount(Integer scanCount) {
        this.scanCount = scanCount;
    }

    public Integer getUrgeCount() {
        return urgeCount;
    }

    public void setUrgeCount(Integer urgeCount) {
        this.urgeCount = urgeCount;
    }

    public Integer getFailCount() {
        return failCount;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
