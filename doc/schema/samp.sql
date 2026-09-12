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
  status int DEFAULT '0' COMMENT '状态 0待抽样 1抽样中 2待判定 3已判定 4已关闭',
  conclude varchar(32) DEFAULT NULL COMMENT '判定结论 合格/不合格',
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
