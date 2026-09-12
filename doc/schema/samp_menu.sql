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
