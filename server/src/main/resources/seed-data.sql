-- ============================================================
-- engine-manage-system 测试数据种子脚本
-- 设计原则:
--   1. 使用固定 BIGINT ID(9000+ 开头)便于外键关联可读
--   2. 可重复执行:先按 ID 范围清理,再 INSERT
--   3. 覆盖两条主线:新建型项目(P1001)+ 维护型项目(P1002)
--   4. 所有 create_by=1(admin),deleted=0
--   5. 关联关系:project→contract/quote/equipment/progress/acceptance
--              contract→payment/change/approval
--              maintenance_contract→quarterly_settlement
--              maintenance_point→task/record
-- ============================================================

-- ===== 0. 清理旧测试数据(范围 9000000000000000000 ~ 9099999999999999999) =====
DELETE FROM customer              WHERE id >= 9000000000000000000 AND id < 9100000000000000000;
DELETE FROM supplier              WHERE id >= 9000000000000000000 AND id < 9100000000000000000;
DELETE FROM sys_dict_item         WHERE id >= 9000000000000000000 AND id < 9100000000000000000;
DELETE FROM sys_dict              WHERE id >= 9000000000000000000 AND id < 9100000000000000000;
DELETE FROM project               WHERE id >= 9000000000000000000 AND id < 9100000000000000000;
DELETE FROM contract              WHERE id >= 9000000000000000000 AND id < 9100000000000000000;
DELETE FROM quote                 WHERE id >= 9000000000000000000 AND id < 9100000000000000000;
DELETE FROM equipment             WHERE id >= 9000000000000000000 AND id < 9100000000000000000;
DELETE FROM progress              WHERE id >= 9000000000000000000 AND id < 9100000000000000000;
DELETE FROM acceptance            WHERE id >= 9000000000000000000 AND id < 9100000000000000000;
DELETE FROM contract_payment      WHERE id >= 9000000000000000000 AND id < 9100000000000000000;
DELETE FROM contract_change       WHERE id >= 9000000000000000000 AND id < 9100000000000000000;
DELETE FROM approval_log          WHERE id >= 9000000000000000000 AND id < 9100000000000000000;
DELETE FROM approval_node         WHERE id >= 9000000000000000000 AND id < 9100000000000000000;
DELETE FROM approval_flow         WHERE id >= 9000000000000000000 AND id < 9100000000000000000;
DELETE FROM attachment            WHERE id >= 9000000000000000000 AND id < 9100000000000000000;
DELETE FROM maintenance_point     WHERE id >= 9000000000000000000 AND id < 9100000000000000000;
DELETE FROM maintenance_contract  WHERE id >= 9000000000000000000 AND id < 9100000000000000000;
DELETE FROM point_settlement      WHERE id >= 9000000000000000000 AND id < 9100000000000000000;
DELETE FROM quarterly_settlement  WHERE id >= 9000000000000000000 AND id < 9100000000000000000;
DELETE FROM maintenance_task      WHERE id >= 9000000000000000000 AND id < 9100000000000000000;
DELETE FROM maintenance_record    WHERE id >= 9000000000000000000 AND id < 9100000000000000000;

-- ===== 1. 基础数据:客户档案 =====
INSERT INTO customer (id, code, name, contact_person, contact_phone, contact_email, address, bank_account, bank_name, remark, create_by, create_time, update_time, deleted) VALUES
(9001000000000000001, 'CUST-001', '上海智云科技有限公司',       '王经理', '13800138001', 'wang@zhiyun.com', '上海市浦东新区张江高科技园区博云路123号', '6225880134567890', '招商银行上海分行',     '战略客户', 1, NOW(), NOW(), 0),
(9001000000000000002, 'CUST-002', '北京华信电子集团',           '李总监', '13800138002', 'li@huaxin.com',  '北京市海淀区中关村软件园9号楼',           '6217000112345678', '中国工商银行北京海淀支行', '重点客户', 1, NOW(), NOW(), 0),
(9001000000000000003, 'CUST-003', '深圳前海能源服务公司',       '张工',   '13800138003', 'zhang@qianhai.com','深圳市南山区科技园南区T3栋',             '6228480123456789012', '中国农业银行深圳分行', NULL, 1, NOW(), NOW(), 0);

-- ===== 2. 基础数据:供应商档案 =====
INSERT INTO supplier (id, code, name, contact_person, contact_phone, contact_email, address, bank_account, bank_name, remark, create_by, create_time, update_time, deleted) VALUES
(9002000000000000001, 'SUP-001', '西门子(中国)有限公司',     '陈销售', '13900139001', 'chen@siemens.com',  '上海市杨浦区大连路500号',           '6225880099988877665', '浦发银行上海分行',     '主推品牌供应商', 1, NOW(), NOW(), 0),
(9002000000000000002, 'SUP-002', 'ABB电气设备有限公司',       '刘商务', '13900139002', 'liu@abb.com',       '北京市朝阳区建国门外大街8号',       '6217000011223344556', '中国建设银行北京分行', '高压设备供应商', 1, NOW(), NOW(), 0),
(9002000000000000003, 'SUP-003', '施耐德电气(中国)',         '赵经理', '13900139003', 'zhao@schneider.com','广州市天河区珠江新城花城大道85号', '6228480000112233445', '中国银行广州分行',     '低压配电供应商', 1, NOW(), NOW(), 0);

-- ===== 3. 基础数据:数据字典 =====
INSERT INTO sys_dict (id, code, name, remark) VALUES
(9003000000000000001, 'equipment_category', '设备类别', '设备分类字典'),
(9003000000000000002, 'project_type',       '项目类型', '项目类型字典'),
(9003000000000000003, 'fault_type',         '故障类型', '维保故障分类');

INSERT INTO sys_dict_item (id, dict_id, label, value, sort) VALUES
(9003010000000000001, 9003000000000000001, '发电机组',     'GENERATOR',  1),
(9003010000000000002, 9003000000000000001, '配电柜',       'DISTRIBUTION', 2),
(9003010000000000003, 9003000000000000001, 'UPS电源',      'UPS',        3),
(9003010000000000004, 9003000000000000001, '变压器',       'TRANSFORMER',4),
(9003010000000000005, 9003000000000000002, '新建项目',     'NEW_BUILD',  1),
(9003010000000000006, 9003000000000000002, '维护型项目',   'MAINTENANCE',2),
(9003010000000000007, 9003000000000000003, '无法启动',     'NO_START',   1),
(9003010000000000008, 9003000000000000003, '异常噪音',     'ABNORMAL_NOISE', 2),
(9003010000000000009, 9003000000000000003, '温度过高',     'OVERHEAT',   3),
(9003010000000000010, 9003000000000000003, '输出异常',     'OUTPUT_ABNORMAL', 4);

-- ===== 4. 项目(2 个:新建型 + 维护型) =====
INSERT INTO project (id, code, name, customer_name, manager_id, address, start_date, end_date, type, status, description, create_by, create_time, update_time, deleted) VALUES
(9004000000000000001, 'PRJ-2026-001', '智云科技数据中心电力工程',     '上海智云科技有限公司', 1, '上海市浦东新区张江高科技园区', '2026-01-15', '2026-08-30', 'NEW_BUILD',   'IN_PROGRESS', '新建型项目:含设备采购、安装、调试、验收全流程', 1, NOW(), NOW(), 0),
(9004000000000000002, 'PRJ-2026-002', '华信电子集团年度维保服务',     '北京华信电子集团',    1, '北京市海淀区中关村软件园',     '2026-01-01', '2026-12-31', 'MAINTENANCE', 'IN_PROGRESS', '维护型项目:12个月4季度维保服务',                1, NOW(), NOW(), 0);

-- ===== 5. 合同(关联 PRJ-001) =====
INSERT INTO contract (id, code, name, party_a, party_b, sign_date, amount, category, payment_method, project_id, status, approval_status, start_date, end_date, remark, create_by, create_time, update_time, deleted) VALUES
(9005000000000000001, 'CON-2026-001', '智云科技数据中心电力工程总承包合同', '上海智云科技有限公司', 'EMS工程服务有限公司', '2026-01-10', 2580000.00, 'GENERAL', '按进度分期付款', 9004000000000000001, 'APPROVED', 'APPROVED', '2026-01-15', '2026-08-30', '总包合同,含设备采购与安装', 1, NOW(), NOW(), 0);

-- ===== 6. 报价(关联 PRJ-001,2 个版本用于版本对比) =====
INSERT INTO quote (id, code, project_id, business_type, business_id, amount, quote_date, valid_until, quote_person, customer_name, version, status, approval_status, summary, create_by, create_time, update_time, deleted) VALUES
(9006000000000000001, 'QUO-2026-001-V1', 9004000000000000001, 'NEW_BUILD', 9004000000000000001, 2350000.00, '2026-01-05', '2026-02-05', '王工', '上海智云科技有限公司', 1, 'ARCHIVED', 'APPROVED', 'v1 初版报价:含基础设备配置',          1, NOW(), NOW(), 0),
(9006000000000000002, 'QUO-2026-001-V2', 9004000000000000001, 'NEW_BUILD', 9004000000000000001, 2580000.00, '2026-01-08', '2026-02-08', '王工', '上海智云科技有限公司', 2, 'APPROVED', 'APPROVED', 'v2 终版报价:升级UPS容量并增加备品备件', 1, NOW(), NOW(), 0);

-- ===== 7. 设备台账(关联 PRJ-001) =====
INSERT INTO equipment (id, code, name, brand, model, serial_number, category, specs, commissioning_date, warranty_expiry, status, project_id, point_id, create_by, create_time, update_time, deleted) VALUES
(9007000000000000001, 'EQP-001', '主用发电机组',     'Cummins', 'C2200D5',  'SN20260115001', 'GENERATOR',     '2000kW 柴油发电机组', '2026-03-01', '2027-03-01', 'RUNNING', 9004000000000000001, NULL, 1, NOW(), NOW(), 0),
(9007000000000000002, 'EQP-002', '高压配电柜',       'ABB',     'UniSec-12', 'SN20260115002', 'DISTRIBUTION', '12kV 中压开关柜',     '2026-03-10', '2028-03-10', 'RUNNING', 9004000000000000001, NULL, 1, NOW(), NOW(), 0),
(9007000000000000003, 'EQP-003', '在线式UPS电源',    'Schneider','Galaxy-VM','SN20260115003', 'UPS',           '160kVA 在线式',       '2026-03-15', '2028-03-15', 'RUNNING', 9004000000000000001, NULL, 1, NOW(), NOW(), 0);

-- ===== 8. 进度(关联 PRJ-001,覆盖4种状态用于甘特图/看板) =====
INSERT INTO progress (id, code, project_id, business_type, business_id, node_name, plan_start_date, plan_end_date, actual_start_date, actual_end_date, progress_percent, manager_id, status, remark, create_by, create_time, update_time, deleted) VALUES
(9008000000000000001, 'PRG-001', 9004000000000000001, 'NEW_BUILD', 9004000000000000001, '设备采购',     '2026-01-20', '2026-02-28', '2026-01-20', '2026-02-25', 100, 1, 'COMPLETED',   '提前3天完成', 1, NOW(), NOW(), 0),
(9008000000000000002, 'PRG-002', 9004000000000000001, 'NEW_BUILD', 9004000000000000001, '设备安装',     '2026-03-01', '2026-04-15', '2026-03-01', NULL,         70,  1, 'IN_PROGRESS', '安装中,已完成70%', 1, NOW(), NOW(), 0),
(9008000000000000003, 'PRG-003', 9004000000000000001, 'NEW_BUILD', 9004000000000000001, '系统调试',     '2026-04-16', '2026-05-20', NULL,         NULL,         0,   1, 'PENDING',     '待开始', 1, NOW(), NOW(), 0),
(9008000000000000004, 'PRG-004', 9004000000000000001, 'NEW_BUILD', 9004000000000000001, '竣工验收',     '2026-05-21', '2026-05-10', NULL,         NULL,         0,   1, 'OVERDUE',     '计划已过期未开始(演示延期)', 1, NOW(), NOW(), 0),
(9008000000000000005, 'PRG-005', 9004000000000000001, 'NEW_BUILD', 9004000000000000001, '资料移交',     '2026-06-01', '2026-06-15', NULL,         NULL,         0,   1, 'PENDING',     '待开始', 1, NOW(), NOW(), 0),
(9008000000000000006, 'PRG-006', 9004000000000000001, 'NEW_BUILD', 9004000000000000001, '质保期开始',   '2026-06-16', '2027-06-16', NULL,         NULL,         0,   1, 'PENDING',     '待开始', 1, NOW(), NOW(), 0);

-- ===== 9. 验收(关联 PRJ-001 + QUO-V1) =====
INSERT INTO acceptance (id, code, project_id, business_type, business_id, quote_id, acceptor_id, accept_date, actual_quantity, result, rectify_count, remark, create_by, create_time, update_time, deleted) VALUES
(9009000000000000001, 'ACC-001', 9004000000000000001, 'NEW_BUILD', 9004000000000000001, 9006000000000000001, 1, '2026-05-25', '设备采购+安装(已完成部分)', 'PASS',         0,   '阶段性验收通过', 1, NOW(), NOW(), 0);

-- ===== 10. 合同收付款(关联 CON-001,覆盖应收/应付/已收/待收/逾期) =====
INSERT INTO contract_payment (id, code, contract_id, type, plan_date, plan_amount, actual_amount, actual_date, invoice_no, status, remark, create_by, create_time, update_time, deleted) VALUES
(9010000000000000001, 'PAY-001', 9005000000000000001, 'RECEIVABLE', '2026-01-20', 774000.00,  774000.00,  '2026-01-22', 'INV-2026-001', 'RECEIVED', '预付款30%,已收', 1, NOW(), NOW(), 0),
(9010000000000000002, 'PAY-002', 9005000000000000001, 'RECEIVABLE', '2026-04-15', 774000.00,  774000.00,  '2026-04-16', 'INV-2026-002', 'RECEIVED', '进度款30%,已收', 1, NOW(), NOW(), 0),
(9010000000000000003, 'PAY-003', 9005000000000000001, 'RECEIVABLE', '2026-06-01', 516000.00,  NULL,       NULL,         NULL,           'OVERDUE',  '验收款20%,已逾期', 1, NOW(), NOW(), 0),
(9010000000000000004, 'PAY-004', 9005000000000000001, 'PAYABLE',    '2026-03-01', 516000.00,  NULL,       NULL,         NULL,           'PENDING',  '设备采购款20%,待付', 1, NOW(), NOW(), 0);

-- ===== 11. 合同变更(关联 CON-001,2条:已通过+待审核) =====
INSERT INTO contract_change (id, contract_id, change_type, change_desc, supplement_file_id, approver_id, approve_time, status, remark, create_by, create_time, update_time, deleted) VALUES
(9011000000000000001, 9005000000000000001, 'AMOUNT_CHANGE', '因UPS升级,合同金额从235万调整为258万', NULL, 1, '2026-01-12 10:30:00', 'APPROVED', '已审批通过', 1, NOW(), NOW(), 0),
(9011000000000000002, 9005000000000000001, 'SCOPE_CHANGE',  '增加备品备件供应范围',                  NULL, NULL, NULL,                'PENDING',  '待审核',     1, NOW(), NOW(), 0);

-- ===== 12. 审批流(关联合同审批) =====
INSERT INTO approval_flow (id, code, name, business_type, enabled, remark, create_by, create_time, update_time, deleted) VALUES
(9012000000000000001, 'AF-CONTRACT-001', '合同审批流(2级)', 'CONTRACT_APPROVAL', 1, '金额<100万项目经理审,>=100万总监审', 1, NOW(), NOW(), 0);

INSERT INTO approval_node (id, flow_id, node_order, node_name, approver_role_id, amount_threshold, create_by, create_time, update_time, deleted) VALUES
(9012010000000000001, 9012000000000000001, 1, '项目经理审批', 1,                   1000000.00, 1, NOW(), NOW(), 0),
(9012010000000000002, 9012000000000000001, 2, '总监审批',     2070711265028440065, NULL,        1, NOW(), NOW(), 0);

-- 审批日志(关联合同 CON-001)
INSERT INTO approval_log (id, flow_id, business_type, business_id, node_order, approver_id, result, opinion, approve_time, create_by, create_time, update_time, deleted) VALUES
(9012020000000000001, 9012000000000000001, 'CONTRACT_APPROVAL', 9005000000000000001, 1, 1,                    'APPROVED', '同意,金额合理', '2026-01-11 14:20:00', 1, NOW(), NOW(), 0),
(9012020000000000002, 9012000000000000001, 'CONTRACT_APPROVAL', 9005000000000000001, 2, 2070711261006102529,  'APPROVED', '同意',           '2026-01-12 10:30:00', 1, NOW(), NOW(), 0);

-- ===== 13. 附件(关联合同) =====
INSERT INTO attachment (id, name, file_path, file_size, file_type, business_type, business_id, create_by, create_time, update_time, deleted) VALUES
(9013000000000000001, '智云科技数据中心电力工程合同.pdf', '/upload/2026/01/contract-001.pdf', 524288,  'pdf',  'CONTRACT', 9005000000000000001, 1, NOW(), NOW(), 0);

-- ============================================================
-- 维护型项目链路 (PRJ-002)
-- ============================================================

-- ===== 14. 维护点位(关联 PRJ-002) =====
INSERT INTO maintenance_point (id, code, project_id, name, location, equipment_list, manager_id, status, create_by, create_time, update_time, deleted) VALUES
(9014000000000000001, 'MP-001', 9004000000000000002, 'A栋机房维保点位',   'A栋3层主机房',   '主用发电机组,配电柜', 1, 'ACCEPTED', 1, NOW(), NOW(), 0),
(9014000000000000002, 'MP-002', 9004000000000000002, 'B栋配电室维保点位', 'B栋1层配电室',   '高压配电柜,UPS',       1, 'CONSTRUCTING', 1, NOW(), NOW(), 0);

-- ===== 15. 维保主合同(关联 PRJ-002,12个月/4季度) =====
INSERT INTO maintenance_contract (id, code, name, project_id, party_a, party_b, sign_date, effective_date, total_amount, period_months, period_count, response_sla, scope, status, end_date, remark, create_by, create_time, update_time, deleted) VALUES
(9015000000000000001, 'MC-2026-001', '华信电子集团2026年度维保合同', 9004000000000000002, '北京华信电子集团', 'EMS工程服务有限公司', '2025-12-25', '2026-01-01', 360000.00, 12, 4, '4小时响应,24小时到场', 'A栋机房+B栋配电室全年维保', 'ACTIVE', '2026-12-31', '年度维保合同', 1, NOW(), NOW(), 0);

-- ===== 16. 点位结算单(关联 PRJ-002 + MP-001 + QUO-V1 + ACC-001) =====
INSERT INTO point_settlement (id, code, project_id, point_id, quote_id, acceptance_id, amount, status, invoice_no, received_amount, received_date, remark, create_by, create_time, update_time, deleted) VALUES
(9016000000000000001, 'PS-001', 9004000000000000002, 9014000000000000001, 9006000000000000001, 9009000000000000001, 58000.00, 'RECEIVED', 'INV-PS-001', 58000.00, '2026-04-10', 'A栋点位结算,已回款', 1, NOW(), NOW(), 0);

-- ===== 17. 季度结算单(关联 MC-001 + PRJ-002,4期) =====
INSERT INTO quarterly_settlement (id, code, contract_id, project_id, period_no, period_start_date, period_end_date, amount, amount_version, status, invoice_no, received_amount, received_date, remark, create_by, create_time, update_time, deleted) VALUES
(9017000000000000001, 'QS-2026-Q1', 9015000000000000001, 9004000000000000002, 1, '2026-01-01', '2026-03-31', 90000.00,  1, 'RECEIVED',  'INV-QS-Q1', 90000.00, '2026-04-05', 'Q1季度结算,已回款', 1, NOW(), NOW(), 0),
(9017000000000000002, 'QS-2026-Q2', 9015000000000000001, 9004000000000000002, 2, '2026-04-01', '2026-06-30', 90000.00,  1, 'INVOICED', 'INV-QS-Q2', NULL,      NULL,         'Q2季度结算,已开票待回款', 1, NOW(), NOW(), 0),
(9017000000000000003, 'QS-2026-Q3', 9015000000000000001, 9004000000000000002, 3, '2026-07-01', '2026-09-30', 90000.00,  1, 'CONFIRMED', NULL,        NULL,      NULL,         'Q3季度结算,已确认待开票', 1, NOW(), NOW(), 0),
(9017000000000000004, 'QS-2026-Q4', 9015000000000000001, 9004000000000000002, 4, '2026-10-01', '2026-12-31', 90000.00,  1, 'DRAFT',     NULL,        NULL,      NULL,         'Q4季度结算,草稿', 1, NOW(), NOW(), 0);

-- ===== 18. 维保任务(关联 PRJ-002 + MP-001 + EQP-001/002/003,覆盖各状态) =====
INSERT INTO maintenance_task (id, code, project_id, point_id, equipment_id, type, title, description, reporter_id, handler_id, handle_method, parts_used, status, plan_date, complete_date, plan_inspect_date, remark, create_by, create_time, update_time, deleted) VALUES
(9018000000000000001, 'MT-001', 9004000000000000002, 9014000000000000001, 9007000000000000001, 'INSPECTION', 'A栋机房3月巡检',   '发电机组季度巡检',           1, 1, '完成巡检,各项指标正常',     NULL,                       'COMPLETED', '2026-03-15', '2026-03-15', '2026-03-15', '例行巡检', 1, NOW(), NOW(), 0),
(9018000000000000002, 'MT-002', 9004000000000000002, 9014000000000000001, 9007000000000000002, 'REPAIR',     '配电柜异常噪音报修', '2号配电柜出现间歇性异响',     1, 1, '紧固松动的母排连接螺栓',     'M10螺栓x4',                'COMPLETED', '2026-04-08', '2026-04-09', NULL,         '故障已排除', 1, NOW(), NOW(), 0),
(9018000000000000003, 'MT-003', 9004000000000000002, 9014000000000000001, 9007000000000000003, 'INSPECTION', 'A栋机房6月巡检',   'UPS电源季度巡检',           1, 1, NULL,                          NULL,                       'PROCESSING','2026-06-20', NULL,         '2026-06-20', '巡检中',     1, NOW(), NOW(), 0),
(9018000000000000004, 'MT-004', 9004000000000000002, 9014000000000000001, 9007000000000000001, 'INSPECTION', 'A栋机房9月巡检',   '发电机组季度巡检',           1, NULL, NULL,                       NULL,                       'PENDING',   NULL,         NULL,         '2026-09-15', '待派单',     1, NOW(), NOW(), 0);

-- ===== 19. 维保记录(关联 MT-001/002/003) =====
INSERT INTO maintenance_record (id, code, task_id, project_id, point_id, equipment_id, record_type, content, parts_used, recorder_id, record_date, remark, create_by, create_time, update_time, deleted) VALUES
(9019000000000000001, 'MR-001', 9018000000000000001, 9004000000000000002, 9014000000000000001, 9007000000000000001, 'INSPECTION', '发电机组巡检:机油液位正常,冷却液正常,启动测试通过,负载测试正常', NULL,          1, '2026-03-15', '巡检结果合格', 1, NOW(), NOW(), 0),
(9019000000000000002, 'MR-002', 9018000000000000002, 9004000000000000002, 9014000000000000001, 9007000000000000002, 'REPAIR',     '配电柜异响排查:发现2号柜母排连接螺栓松动,已紧固处理,复测无异常', 'M10螺栓x4',  1, '2026-04-09', '维修完成',     1, NOW(), NOW(), 0),
(9019000000000000003, 'MR-003', 9018000000000000003, 9004000000000000002, 9014000000000000001, 9007000000000000003, 'INSPECTION', 'UPS巡检:输入电压正常,输出电压正常,电池组电压正常,风扇运转正常', NULL,          1, '2026-06-20', '巡检中',       1, NOW(), NOW(), 0);

-- ===== 完成 =====
SELECT 'SEED_DATA_INSERTED' AS result;
