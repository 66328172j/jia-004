-- samp 检验批模块菜单与按钮权限（jia-004）
-- 顶级目录"质量管理" + 检验批管理菜单 + 列表/添加/删除/修改按钮，并授权给管理员角色

INSERT INTO `t_sys_permission` VALUES
 (930000000000000001, '质量管理', NULL, '', 0, 0, '', 0, 'layui-icon layui-icon-vercode', 2, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000002, '检验批管理', '检验批展示', '/SampLotController/view', 0, 930000000000000001, 'system:sampLot:view', 1, 'layui-icon layui-icon-form', 1, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000003, '检验批集合', '检验批集合', '/SampLotController/list', 0, 930000000000000002, 'system:sampLot:list', 2, '', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000004, '检验批添加', '检验批添加', '/SampLotController/add', 0, 930000000000000002, 'system:sampLot:add', 2, 'layui-icon layui-icon-add-1', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000005, '检验批删除', '检验批删除', '/SampLotController/remove', 0, 930000000000000002, 'system:sampLot:remove', 2, 'layui-icon layui-icon-delete', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000006, '检验批修改', '检验批修改', '/SampLotController/edit', 0, 930000000000000002, 'system:sampLot:edit', 2, 'layui-icon layui-icon-edit', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000010, '样本检测记录', '样本检测记录展示', '/SampSampleController/view', 0, 930000000000000001, 'system:sampSample:view', 1, 'layui-icon layui-icon-template-1', 2, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000011, '样本记录集合', '样本记录集合', '/SampSampleController/list', 0, 930000000000000010, 'system:sampSample:list', 2, '', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000012, '样本记录添加', '样本记录添加', '/SampSampleController/add', 0, 930000000000000010, 'system:sampSample:add', 2, 'layui-icon layui-icon-add-1', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000013, '样本记录删除', '样本记录删除', '/SampSampleController/remove', 0, 930000000000000010, 'system:sampSample:remove', 2, 'layui-icon layui-icon-delete', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000014, '样本记录修改', '样本记录修改', '/SampSampleController/edit', 0, 930000000000000010, 'system:sampSample:edit', 2, 'layui-icon layui-icon-edit', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000020, '复检管理', '检验批复检单展示', '/SampRetestController/view', 0, 930000000000000001, 'system:sampRetest:view', 1, 'layui-icon layui-icon-refresh-3', 3, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000021, '复检单集合', '复检单集合', '/SampRetestController/list', 0, 930000000000000020, 'system:sampRetest:list', 2, '', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000022, '复检单发起', '复检单发起', '/SampRetestController/add', 0, 930000000000000020, 'system:sampRetest:add', 2, 'layui-icon layui-icon-add-1', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000023, '复检结果录入', '复检结果录入', '/SampRetestController/finish', 0, 930000000000000020, 'system:sampRetest:finish', 2, 'layui-icon layui-icon-ok-circle', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000024, '复检单作废', '复检单作废', '/SampRetestController/cancel', 0, 930000000000000020, 'system:sampRetest:cancel', 2, 'layui-icon layui-icon-close-fill', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000025, '复检单删除', '复检单删除', '/SampRetestController/remove', 0, 930000000000000020, 'system:sampRetest:remove', 2, 'layui-icon layui-icon-delete', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL);

-- 授权给管理员角色（488243256161730560）
INSERT INTO `t_sys_permission_role` VALUES
 (930000000000000101, 488243256161730560, 930000000000000001, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000102, 488243256161730560, 930000000000000002, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000103, 488243256161730560, 930000000000000003, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000104, 488243256161730560, 930000000000000004, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000105, 488243256161730560, 930000000000000005, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000106, 488243256161730560, 930000000000000006, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000110, 488243256161730560, 930000000000000010, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000111, 488243256161730560, 930000000000000011, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000112, 488243256161730560, 930000000000000012, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000113, 488243256161730560, 930000000000000013, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000114, 488243256161730560, 930000000000000014, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000120, 488243256161730560, 930000000000000020, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000121, 488243256161730560, 930000000000000021, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000122, 488243256161730560, 930000000000000022, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000123, 488243256161730560, 930000000000000023, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000124, 488243256161730560, 930000000000000024, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000125, 488243256161730560, 930000000000000025, 'admin', sysdate(), NULL, NULL, NULL);

-- 超期催办菜单与按钮权限（jia-004 催办）
INSERT INTO `t_sys_permission` VALUES
 (930000000000000030, '催办管理', '超期催办记录展示', '/SampUrgeController/view', 0, 930000000000000001, 'system:sampUrge:view', 1, 'layui-icon layui-icon-notice', 4, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000031, '催办记录集合', '催办记录集合', '/SampUrgeController/list', 0, 930000000000000030, 'system:sampUrge:list', 2, '', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000032, '发起催办', '发起催办', '/SampUrgeController/add', 0, 930000000000000030, 'system:sampUrge:add', 2, 'layui-icon layui-icon-add-1', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000033, '催办记录删除', '催办记录删除', '/SampUrgeController/remove', 0, 930000000000000030, 'system:sampUrge:remove', 2, 'layui-icon layui-icon-delete', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL);

-- 授权给管理员角色（488243256161730560）
INSERT INTO `t_sys_permission_role` VALUES
 (930000000000000130, 488243256161730560, 930000000000000030, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000131, 488243256161730560, 930000000000000031, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000132, 488243256161730560, 930000000000000032, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000133, 488243256161730560, 930000000000000033, 'admin', sysdate(), NULL, NULL, NULL);

-- 检验台账导出菜单与按钮权限（jia-004 台账导出）
INSERT INTO `t_sys_permission` VALUES
 (930000000000000040, '台账导出', '检验台账导出记录展示', '/SampExportController/view', 0, 930000000000000001, 'system:sampExport:view', 1, 'layui-icon layui-icon-export', 5, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000041, '导出记录集合', '导出记录集合', '/SampExportController/list', 0, 930000000000000040, 'system:sampExport:list', 2, '', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000042, '导出台账', '导出台账', '/SampExportController/export', 0, 930000000000000040, 'system:sampExport:export', 2, 'layui-icon layui-icon-export', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000043, '台账文件下载', '台账文件下载', '/SampExportController/download', 0, 930000000000000040, 'system:sampExport:download', 2, 'layui-icon layui-icon-download-circle', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL);

-- 授权给管理员角色（488243256161730560）
INSERT INTO `t_sys_permission_role` VALUES
 (930000000000000140, 488243256161730560, 930000000000000040, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000141, 488243256161730560, 930000000000000041, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000142, 488243256161730560, 930000000000000042, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000143, 488243256161730560, 930000000000000043, 'admin', sysdate(), NULL, NULL, NULL);

-- 超期扫描定时任务菜单与按钮权限（jia-004 定时催办扫描）
INSERT INTO `t_sys_permission` VALUES
 (930000000000000044, '超期扫描任务', '超期扫描定时任务展示', '/SampJobController/view', 0, 930000000000000001, 'system:sampJob:view', 1, 'layui-icon layui-icon-time', 6, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000045, '任务集合', '任务集合', '/SampJobController/list', 0, 930000000000000044, 'system:sampJob:list', 2, '', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000046, '任务启停', '任务启停', '/SampJobController/changeStatus', 0, 930000000000000044, 'system:sampJob:changeStatus', 2, '', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000047, '手动执行一次', '手动执行一次', '/SampJobController/run', 0, 930000000000000044, 'system:sampJob:run', 2, 'layui-icon layui-icon-triangle-r', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000048, '任务日志', '任务日志', '/SampJobController/logView', 0, 930000000000000044, 'system:sampJob:logView', 2, 'layui-icon layui-icon-log', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL);

-- 授权给管理员角色（488243256161730560）
INSERT INTO `t_sys_permission_role` VALUES
 (930000000000000144, 488243256161730560, 930000000000000044, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000145, 488243256161730560, 930000000000000045, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000146, 488243256161730560, 930000000000000046, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000147, 488243256161730560, 930000000000000047, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000148, 488243256161730560, 930000000000000048, 'admin', sysdate(), NULL, NULL, NULL);

-- 质量统计菜单与按钮权限（jia-004 质量统计）
INSERT INTO `t_sys_permission` VALUES
 (930000000000000050, '质量统计', '按月份/产品检验质量统计展示', '/SampStatisticsController/view', 0, 930000000000000001, 'system:sampStatistics:view', 1, 'layui-icon layui-icon-chart-screen', 7, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000051, '按产品统计集合', '按产品统计集合', '/SampStatisticsController/byProduct', 0, 930000000000000050, 'system:sampStatistics:byProduct', 2, '', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000052, '月度趋势集合', '月度趋势集合', '/SampStatisticsController/trendMonth', 0, 930000000000000050, 'system:sampStatistics:trendMonth', 2, '', NULL, 0, 'admin', sysdate(), NULL, NULL, NULL);

-- 授权给管理员角色（488243256161730560）
INSERT INTO `t_sys_permission_role` VALUES
 (930000000000000150, 488243256161730560, 930000000000000050, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000151, 488243256161730560, 930000000000000051, 'admin', sysdate(), NULL, NULL, NULL),
 (930000000000000152, 488243256161730560, 930000000000000052, 'admin', sysdate(), NULL, NULL, NULL);
