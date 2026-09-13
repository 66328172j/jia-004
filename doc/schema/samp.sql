-- samp 检验抽样批次追踪与质量定等 —— schema (jia-004)
-- 列名与基线实体契约（@TableName / @TableField）逐列对齐，改列必须同步实体。
-- 库：jia_004

CREATE TABLE IF NOT EXISTS t_samp_product (
  id bigint NOT NULL COMMENT '主键',
  code varchar(64) NOT NULL COMMENT '产品编号',
  name varchar(128) DEFAULT NULL COMMENT '产品名称',
  spec varchar(128) DEFAULT NULL COMMENT '规格型号',
  unit varchar(32) DEFAULT NULL COMMENT '计量单位',
  category varchar(64) DEFAULT NULL COMMENT '产品类别',
  aql decimal(5,2) DEFAULT '2.50' COMMENT '默认接收质量限 AQL',
  status int DEFAULT '0' COMMENT '状态 0启用 1停用',
  del_flag int DEFAULT '0' COMMENT '0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_sp_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='受检产品档案';

CREATE TABLE IF NOT EXISTS t_samp_lot (
  id bigint NOT NULL COMMENT '主键',
  lot_no varchar(64) NOT NULL COMMENT '检验批号',
  product_id bigint DEFAULT NULL COMMENT '产品ID',
  product_code varchar(64) DEFAULT NULL COMMENT '产品编号（冗余）',
  product_name varchar(128) DEFAULT NULL COMMENT '产品名称（冗余）',
  batch_qty int DEFAULT NULL COMMENT '批量',
  inspect_type varchar(32) DEFAULT NULL COMMENT '检验类型 出厂/到货',
  apply_unit varchar(128) DEFAULT NULL COMMENT '报检单位',
  apply_by varchar(64) DEFAULT NULL COMMENT '报检人',
  apply_date datetime DEFAULT NULL COMMENT '报检日期',
  scheme_code varchar(8) DEFAULT NULL COMMENT '样本量字码',
  aql decimal(5,2) DEFAULT NULL COMMENT '接收质量限',
  sample_size int DEFAULT NULL COMMENT '应抽样本量 n',
  accept_count int DEFAULT NULL COMMENT '接收数 Ac',
  reject_count int DEFAULT NULL COMMENT '拒收数 Re',
  defect_count int DEFAULT '0' COMMENT '不合格样本数（样本检测汇总）',
  pass_rate decimal(5,2) DEFAULT NULL COMMENT '合格率（百分比，样本检测汇总）',
  status int DEFAULT '0' COMMENT '状态 0待抽样 1抽样中 2待判定 3已判定 4已关闭',
  conclude varchar(32) DEFAULT NULL COMMENT '判定结论 合格/不合格',
  require_days int DEFAULT '7' COMMENT '要求完成天数（报检日期+该天数内未判定即超期）',
  del_flag int DEFAULT '0' COMMENT '0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_sl_no (lot_no),
  KEY idx_sl_product (product_id),
  KEY idx_sl_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='检验批';

CREATE TABLE IF NOT EXISTS t_samp_sample (
  id bigint NOT NULL COMMENT '主键',
  lot_id bigint NOT NULL COMMENT '检验批ID',
  sample_no varchar(16) NOT NULL COMMENT '样本编号（S01..Sn）',
  item_code varchar(64) DEFAULT NULL COMMENT '检测项编码',
  item_name varchar(128) DEFAULT NULL COMMENT '检测项名称',
  std_value varchar(255) DEFAULT NULL COMMENT '标准值/判定依据',
  measured_value varchar(255) DEFAULT NULL COMMENT '实测值',
  item_result int DEFAULT '0' COMMENT '单项判定 0合格 1不合格',
  check_by varchar(64) DEFAULT NULL COMMENT '检测人',
  check_time datetime DEFAULT NULL COMMENT '检测时间',
  del_flag int DEFAULT '0' COMMENT '0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ss_lot_no (lot_id, sample_no),
  KEY idx_ss_lot (lot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='检验样本检测记录';

CREATE TABLE IF NOT EXISTS t_samp_scheme (
  id bigint NOT NULL COMMENT '主键',
  qty_min int NOT NULL COMMENT '批量下界（含）',
  qty_max int NOT NULL COMMENT '批量上界（含）',
  code_letter varchar(8) DEFAULT NULL COMMENT '样本量字码',
  sample_size int DEFAULT NULL COMMENT '样本量 n',
  accept_count int DEFAULT NULL COMMENT '接收数 Ac',
  reject_count int DEFAULT NULL COMMENT '拒收数 Re',
  del_flag int DEFAULT '0' COMMENT '0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_ss_range (qty_min, qty_max)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='抽样方案（一般检验水平II，AQL2.5，正常检验一次抽样）';

-- 预置抽样方案：批量区间闭区间 [qty_min, qty_max]
INSERT INTO t_samp_scheme (id, qty_min, qty_max, code_letter, sample_size, accept_count, reject_count, del_flag, create_time) VALUES
 (1, 2, 8, 'A', 2, 0, 1, 0, NOW()),
 (2, 9, 15, 'B', 3, 0, 1, 0, NOW()),
 (3, 16, 25, 'C', 5, 0, 1, 0, NOW()),
 (4, 26, 50, 'D', 8, 1, 2, 0, NOW()),
 (5, 51, 90, 'E', 13, 1, 2, 0, NOW()),
 (6, 91, 150, 'F', 20, 1, 2, 0, NOW()),
 (7, 151, 280, 'G', 32, 2, 3, 0, NOW()),
 (8, 281, 500, 'H', 50, 3, 4, 0, NOW()),
 (9, 501, 1200, 'J', 80, 5, 6, 0, NOW()),
 (10, 1201, 3200, 'K', 125, 7, 8, 0, NOW()),
 (11, 3201, 10000, 'L', 200, 10, 11, 0, NOW()),
 (12, 10001, 35000, 'M', 315, 14, 15, 0, NOW()),
 (13, 35001, 150000, 'N', 500, 21, 22, 0, NOW()),
 (14, 150001, 500000, 'P', 800, 21, 22, 0, NOW()),
 (15, 500001, 2147483647, 'Q', 1250, 21, 22, 0, NOW())
ON DUPLICATE KEY UPDATE sample_size = VALUES(sample_size), accept_count = VALUES(accept_count), reject_count = VALUES(reject_count);

-- 预置产品档案（供 F1 基线演示与验收测试，编号前缀 SAMP-P）
INSERT INTO t_samp_product (id, code, name, spec, unit, category, aql, status, del_flag, create_time) VALUES
 (1, 'SAMP-P001', '示例产品甲', 'A型', '件', '结构件', 2.50, 0, 0, NOW()),
 (2, 'SAMP-P002', '示例产品乙', 'B型', '箱', '结构件', 2.50, 0, 0, NOW()),
 (3, 'SAMP-P003', '停用产品丙', 'C型', '件', '结构件', 2.50, 1, 0, NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), status = VALUES(status);

-- 已按早期版本建过 t_samp_lot 的库，补样本检测汇总两列（新库无需执行）：
-- ALTER TABLE t_samp_lot ADD COLUMN defect_count int DEFAULT '0' COMMENT '不合格样本数（样本检测汇总）' AFTER reject_count;
-- ALTER TABLE t_samp_lot ADD COLUMN pass_rate decimal(5,2) DEFAULT NULL COMMENT '合格率（百分比，样本检测汇总）' AFTER defect_count;
-- 已按早期版本建过 t_samp_lot 的库，补要求完成天数列（新库无需执行）：
-- ALTER TABLE t_samp_lot ADD COLUMN require_days int DEFAULT '7' COMMENT '要求完成天数（报检日期+该天数内未判定即超期）' AFTER conclude;

CREATE TABLE IF NOT EXISTS t_samp_retest (
  id bigint NOT NULL COMMENT '主键',
  lot_id bigint NOT NULL COMMENT '检验批ID',
  retest_no varchar(64) NOT NULL COMMENT '复检单号',
  reason varchar(500) DEFAULT NULL COMMENT '复检原因',
  sample_qty int DEFAULT NULL COMMENT '复检样本量（应抽样本量的两倍）',
  origin_result int DEFAULT NULL COMMENT '原判定 0合格 1不合格',
  retest_result int DEFAULT NULL COMMENT '复检判定 0合格 1不合格',
  retest_by varchar(64) DEFAULT NULL COMMENT '复检人',
  retest_time datetime DEFAULT NULL COMMENT '复检时间',
  status int DEFAULT '0' COMMENT '状态 0待复检 1已完成 2已作废',
  del_flag int DEFAULT '0' COMMENT '0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sr_no (retest_no),
  KEY idx_sr_lot (lot_id),
  KEY idx_sr_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='检验批复检记录';

CREATE TABLE IF NOT EXISTS t_samp_export (
  id varchar(32) NOT NULL COMMENT '主键',
  batch_no varchar(64) NOT NULL COMMENT '导出批次号',
  begin_date datetime DEFAULT NULL COMMENT '导出开始日期',
  end_date datetime DEFAULT NULL COMMENT '导出结束日期',
  check_type varchar(32) DEFAULT NULL COMMENT '检验类型',
  file_name varchar(255) DEFAULT NULL COMMENT '导出文件名',
  export_count int DEFAULT '0' COMMENT '导出条数',
  export_by varchar(64) DEFAULT NULL COMMENT '导出人',
  export_time datetime DEFAULT NULL COMMENT '导出时间',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_se_batch_no (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='检验台账导出记录';

CREATE TABLE IF NOT EXISTS t_samp_urge (
  id bigint NOT NULL COMMENT '主键',
  lot_id bigint NOT NULL COMMENT '检验批ID',
  urge_no varchar(64) NOT NULL COMMENT '催办单号',
  urge_count int DEFAULT '1' COMMENT '催办次数（该批第几次催办）',
  overdue_days int DEFAULT '0' COMMENT '超期天数（催办时）',
  urge_by varchar(64) DEFAULT NULL COMMENT '催办人',
  urge_time datetime DEFAULT NULL COMMENT '本次催办时间',
  del_flag int DEFAULT '0' COMMENT '0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_su_no (urge_no),
  KEY idx_su_lot (lot_id),
  KEY idx_su_by_time (urge_by, urge_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='检验批超期催办记录';
