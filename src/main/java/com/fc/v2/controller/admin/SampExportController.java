package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TSampExport;
import com.fc.v2.service.ITSampExportService;
import com.fc.v2.util.DateUtils;
import com.fc.v2.util.StringUtils;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 检验台账导出 controller
 *
 * @author fuce
 * @date 2026-09-13
 */
@Controller
@RequestMapping("/SampExportController")
@Api(value = "检验台账导出")
public class SampExportController extends BaseController {

    private final String prefix = "admin/sampExport";

    @Autowired
    private ITSampExportService tSampExportService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    public String view() {
        return prefix + "/list";
    }

    @Log(title = "台账导出记录查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @ResponseBody
    public ResultTable list(TSampExport tSampExport) {
        QueryWrapper<TSampExport> queryWrapper = new QueryWrapper<TSampExport>();
        // 按导出批次号模糊查
        queryWrapper.like(StringUtils.isNotEmpty(tSampExport.getBatchNo()), "batch_no", tSampExport.getBatchNo());
        startPage();
        PageInfo<TSampExport> page = new PageInfo<TSampExport>(tSampExportService.selectTSampExportList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @ApiOperation(value = "导出跳转", notes = "导出跳转")
    @GetMapping("/add")
    public String add() {
        return prefix + "/add";
    }

    @Log(title = "检验台账导出", action = "export")
    @ApiOperation(value = "导出台账", notes = "按时间范围与检验类型导出检验台账，并落一条导出记录")
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(String beginDate, String endDate, String checkType) {
        try {
            TSampExport record = tSampExportService.exportLedger(parseDay(beginDate), parseDay(endDate), checkType);
            AjaxResult result = AjaxResult.success("导出成功，批次号[" + record.getBatchNo() + "]，共 " + record.getExportCount() + " 条");
            result.put("data", record);
            return result;
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @Log(title = "台账文件下载", action = "download")
    @ApiOperation(value = "下载台账文件", notes = "按导出记录下载台账文件")
    @GetMapping("/download")
    @ResponseBody
    public void download(String id, HttpServletResponse response) throws IOException {
        TSampExport record = tSampExportService.selectTSampExportById(id);
        if (record == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "导出记录不存在或已删除");
            return;
        }
        // fileName 是后端生成的纯文件名，直接拼导出目录，不接受页面传路径
        File file = new File(tSampExportService.getExportDir(), record.getFileName());
        if (!file.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "台账文件不存在或已被清理");
            return;
        }
        byte[] bytes = Files.readAllBytes(file.toPath());
        // 兼容修复前导出的旧文件：旧文件没有 BOM，Excel 打开中文乱码，下载时补上，
        // 免得历史导出记录必须全部重导一遍
        if (bytes.length < 3
                || bytes[0] != (byte) 0xEF || bytes[1] != (byte) 0xBB || bytes[2] != (byte) 0xBF) {
            byte[] withBom = new byte[bytes.length + 3];
            withBom[0] = (byte) 0xEF;
            withBom[1] = (byte) 0xBB;
            withBom[2] = (byte) 0xBF;
            System.arraycopy(bytes, 0, withBom, 3, bytes.length);
            bytes = withBom;
        }
        // 中文文件名按 UTF-8 编码，浏览器另存与 Excel 打开时文件名才不乱码
        String fileName = URLEncoder.encode(record.getFileName(), "UTF-8").replace("+", "%20");
        response.reset();
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.addHeader("Content-Length", "" + bytes.length);
        response.setContentType("application/octet-stream; charset=UTF-8");
        IOUtils.write(bytes, response.getOutputStream());
    }

    /**
     * 页面传的日期为 yyyy-MM-dd，格式不对直接拒绝，不做进位解析
     *
     * @param dateStr 日期串
     * @return 日期，空串返回 null 交给后端校验统一报错
     */
    private Date parseDay(String dateStr) {
        if (StringUtils.isEmpty(dateStr)) {
            return null;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DateUtils.YYYY_MM_DD);
            sdf.setLenient(false);
            return sdf.parse(dateStr.trim());
        } catch (ParseException e) {
            throw new IllegalArgumentException("日期格式不正确，应为 yyyy-MM-dd");
        }
    }
}
