package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TSampImportBatch;
import com.fc.v2.model.auto.TSampImportError;
import com.fc.v2.service.ITSampImportService;
import com.fc.v2.util.StringUtils;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 检验批批量导入 controller
 *
 * 导入文件为 CSV（Excel 另存为 CSV 后上传，与台账导出的 CSV 约定一致），
 * 第一行表头，其后每行一个检验批：批号、产品编码、批量、检验类型、报检单位、报检人。
 *
 * @author fuce
 * @date 2026-09-13
 */
@Controller
@RequestMapping("/SampImportController")
@Api(value = "检验批批量导入")
public class SampImportController extends BaseController {

    private final String prefix = "admin/sampImport";

    @Autowired
    private ITSampImportService tSampImportService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    public String view() {
        return prefix + "/list";
    }

    @Log(title = "检验批导入批次查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @ResponseBody
    public ResultTable list(TSampImportBatch tSampImportBatch) {
        QueryWrapper<TSampImportBatch> queryWrapper = new QueryWrapper<TSampImportBatch>();
        // 按导入批次号模糊查
        queryWrapper.like(StringUtils.isNotEmpty(tSampImportBatch.getBatchNo()), "batch_no",
                tSampImportBatch.getBatchNo());
        startPage();
        PageInfo<TSampImportBatch> page = new PageInfo<TSampImportBatch>(
                tSampImportService.selectImportBatchList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @ApiOperation(value = "导入跳转", notes = "导入跳转")
    @GetMapping("/add")
    public String add() {
        return prefix + "/add";
    }

    @Log(title = "检验批批量导入", action = "import")
    @ApiOperation(value = "批量导入", notes = "上传CSV文件批量导入检验批，逐行校验，通过的入库，失败的记明细")
    @PostMapping("/import")
    @ResponseBody
    public AjaxResult importLots(@RequestParam("file") MultipartFile file) {
        try {
            List<String> lines = readCsvLines(file);
            Map<String, Object> result = tSampImportService.importTSampLot(file.getOriginalFilename(), lines);
            String msg = "导入完成：共" + result.get("total") + "条，成功" + result.get("success")
                    + "条，失败" + result.get("fail") + "条";
            Integer fail = (Integer) result.get("fail");
            if (fail != null && fail > 0) {
                msg += "，失败明细可在列表下载";
            }
            return success(200, msg, result);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        } catch (IOException e) {
            return error("导入文件读取失败，请重新选择文件");
        }
    }

    @Log(title = "导入失败明细下载", action = "downloadError")
    @ApiOperation(value = "下载失败明细", notes = "按导入批次下载失败明细CSV，含行号与失败原因")
    @GetMapping("/downloadError")
    @ResponseBody
    public void downloadError(String id, HttpServletResponse response) throws IOException {
        TSampImportBatch batch = tSampImportService.selectImportBatchById(id);
        if (batch == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "导入批次不存在或已删除");
            return;
        }
        List<TSampImportError> errors = tSampImportService.selectImportErrorList(id);
        StringBuilder content = new StringBuilder();
        content.append("行号,出错字段,失败原因\r\n");
        for (TSampImportError error : errors) {
            content.append(error.getRowNo() == null ? "" : error.getRowNo()).append(',')
                    .append(csv(error.getFieldName())).append(',')
                    .append(csv(error.getErrorMsg())).append("\r\n");
        }
        writeCsvResponse(response, "导入失败明细_" + batch.getBatchNo() + ".csv", content.toString());
    }

    @ApiOperation(value = "下载导入模板", notes = "下载检验批导入CSV模板（表头+示例行）")
    @GetMapping("/template")
    @ResponseBody
    public void template(HttpServletResponse response) throws IOException {
        String content = "检验批号,产品编码,批量,检验类型,报检单位,报检人\r\n"
                + "LOT20260913001,SAMP-P001,100,出厂,示例报检单位,张三\r\n";
        writeCsvResponse(response, "检验批导入模板.csv", content);
    }

    /**
     * 把上传文件读成文本行：只支持 CSV（Excel 另存为 CSV 后上传）。
     * Excel 另存的 CSV 常见两种编码：带 BOM 的 UTF-8、中文 Windows 默认的 ANSI(GBK)，都兼容
     *
     * @param file 上传文件
     * @return 文本行集合（第一行为表头）
     */
    private List<String> readCsvLines(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要导入的CSV文件");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("只支持CSV文件，请先在Excel中另存为CSV（UTF-8）后再上传");
        }
        byte[] bytes = file.getBytes();
        Charset charset = hasUtf8Bom(bytes) ? StandardCharsets.UTF_8 : Charset.forName("GBK");
        String content = new String(bytes, charset);
        // UTF-8 BOM 解码后首字符是 \uFEFF，去掉免得污染表头第一列
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }
        return new ArrayList<String>(Arrays.asList(content.split("\r\n|\r|\n", -1)));
    }

    /**
     * 是否带 UTF-8 BOM（EF BB BF）
     *
     * @param bytes 文件字节
     * @return true=带 BOM
     */
    private boolean hasUtf8Bom(byte[] bytes) {
        return bytes.length >= 3
                && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF;
    }

    /**
     * 写出 CSV 下载响应：UTF-8 带 BOM（Excel 双击打开靠 BOM 识别编码，没有 BOM 中文会乱码），
     * 中文文件名按 UTF-8 编码，浏览器另存时文件名才不乱码
     *
     * @param response 响应
     * @param fileName 下载文件名
     * @param content  CSV 内容
     */
    private void writeCsvResponse(HttpServletResponse response, String fileName, String content)
            throws IOException {
        byte[] body = content.getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[body.length + 3];
        bytes[0] = (byte) 0xEF;
        bytes[1] = (byte) 0xBB;
        bytes[2] = (byte) 0xBF;
        System.arraycopy(body, 0, bytes, 3, body.length);
        String encodedName = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
        response.reset();
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedName + "\"");
        response.addHeader("Content-Length", "" + bytes.length);
        response.setContentType("application/octet-stream; charset=UTF-8");
        IOUtils.write(bytes, response.getOutputStream());
    }

    /**
     * CSV 字段转义：含逗号/双引号/换行的字段用双引号包裹，字段内双引号双写，
     * 否则失败原因里的逗号会把一行的列顶错位
     *
     * @param s 字段值
     * @return 转义后的字段
     */
    private String csv(String s) {
        if (s == null) {
            return "";
        }
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
