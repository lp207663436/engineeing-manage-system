CREATE DATABASE IF NOT EXISTS engine_manage DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE engine_manage;

CREATE TABLE sys_dept (
    id BIGINT PRIMARY KEY COMMENT '部门ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父部门ID',
    name VARCHAR(50) NOT NULL COMMENT '部门名称',
    sort INT DEFAULT 0 COMMENT '排序',
    leader VARCHAR(50) COMMENT '负责人',
    status TINYINT DEFAULT 1 COMMENT '状态(1正常 0禁用)',
    create_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
) COMMENT '部门表';

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY,
    dept_id BIGINT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL COMMENT 'BCrypt',
    name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    status TINYINT DEFAULT 1,
    create_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_dept_id (dept_id),
    INDEX idx_username (username)
) COMMENT '用户表';

CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    data_scope TINYINT DEFAULT 1 COMMENT '1全部 2本部门 3本部门及以下 4本人',
    sort INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
) COMMENT '角色表';

CREATE TABLE sys_menu (
    id BIGINT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    name VARCHAR(50) NOT NULL,
    type TINYINT NOT NULL COMMENT '1目录 2菜单 3按钮',
    permission VARCHAR(100),
    path VARCHAR(200),
    icon VARCHAR(50),
    sort INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_parent_id (parent_id)
) COMMENT '菜单权限表';

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
) COMMENT '用户角色关联';

CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
) COMMENT '角色菜单关联';

-- 种子数据:管理员密码 admin123 (BCrypt hash 已验证有效)
INSERT INTO sys_dept (id, parent_id, name, sort, leader) VALUES (1, 0, '总公司', 0, '管理员');
INSERT INTO sys_user (id, dept_id, username, password, name) VALUES (1, 1, 'admin', '$2a$10$fDJiNEm5zRwuFEI1feEnz.DOGZQfuI4I6x801vftSANEPgnHgcqD.', '超级管理员');
INSERT INTO sys_role (id, name, code, data_scope) VALUES (1, '超级管理员', 'admin', 1);
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO sys_menu (id, parent_id, name, type, permission, path, icon, sort) VALUES
(100, 0, '系统管理', 1, NULL, '/system', 'Settings', 1),
(101, 100, '用户管理', 2, 'system:user:list', '/system/user', 'User', 1),
(102, 100, '角色管理', 2, 'system:role:list', '/system/role', 'UserFilled', 2),
(103, 100, '菜单管理', 2, 'system:menu:list', '/system/menu', 'Menu', 3),
(104, 100, '部门管理', 2, 'system:dept:list', '/system/dept', 'OfficeBuilding', 4);

-- ===== 业务模块表(2A) =====

-- 项目基类(NEW_BUILD新建工程 / MAINTENANCE维护型)
CREATE TABLE IF NOT EXISTS project (
    id BIGINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL COMMENT '项目编号',
    name VARCHAR(200) NOT NULL COMMENT '项目名称',
    customer_name VARCHAR(200) COMMENT '客户名称',
    manager_id BIGINT COMMENT '项目经理(user.id)',
    address VARCHAR(500) COMMENT '项目地址',
    start_date DATE COMMENT '开工日期',
    end_date DATE COMMENT '竣工日期',
    type VARCHAR(20) NOT NULL DEFAULT 'NEW_BUILD' COMMENT 'NEW_BUILD/MAINTENANCE',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/IN_PROGRESS/COMPLETED/ARCHIVED',
    description VARCHAR(1000) COMMENT '项目描述',
    create_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_code (code),
    INDEX idx_type (type),
    INDEX idx_status (status)
) COMMENT '项目表';

-- 合同管理
CREATE TABLE IF NOT EXISTS contract (
    id BIGINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL COMMENT '合同编号',
    name VARCHAR(200) NOT NULL COMMENT '合同名称',
    party_a VARCHAR(200) COMMENT '甲方',
    party_b VARCHAR(200) COMMENT '乙方',
    sign_date DATE COMMENT '签订日期',
    amount DECIMAL(14,2) DEFAULT 0 COMMENT '合同金额',
    category VARCHAR(20) COMMENT 'GENERAL总包/SUB分包/PURCHASE采购/MAINTENANCE维保',
    payment_method VARCHAR(200) COMMENT '付款方式',
    project_id BIGINT COMMENT '关联项目',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/APPROVING/APPROVED/ARCHIVED',
    start_date DATE COMMENT '合同生效日',
    end_date DATE COMMENT '合同到期日',
    remark VARCHAR(500) COMMENT '备注',
    create_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_code (code),
    INDEX idx_project (project_id),
    INDEX idx_status (status)
) COMMENT '合同表';

-- 报价管理(基础能力层,含 business_type)
CREATE TABLE IF NOT EXISTS quote (
    id BIGINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL COMMENT '报价编号',
    project_id BIGINT COMMENT '关联项目',
    business_type VARCHAR(30) NOT NULL DEFAULT 'NEW_BUILD' COMMENT 'NEW_BUILD/MAINTENANCE_POINT',
    business_id BIGINT COMMENT '业务ID(NEW_BUILD=project_id, MAINTENANCE_POINT=point_id)',
    amount DECIMAL(14,2) DEFAULT 0 COMMENT '报价金额',
    quote_date DATE COMMENT '报价日期',
    valid_until DATE COMMENT '报价有效期',
    quote_person VARCHAR(100) COMMENT '报价人',
    customer_name VARCHAR(200) COMMENT '客户名称',
    version INT DEFAULT 1 COMMENT '版本号',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED/APPROVED/CONFIRMED/VOID',
    summary VARCHAR(1000) COMMENT '报价摘要',
    create_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_code (code),
    INDEX idx_project (project_id),
    INDEX idx_business (business_type, business_id)
) COMMENT '报价表';

-- 设备台账
CREATE TABLE IF NOT EXISTS equipment (
    id BIGINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL COMMENT '设备编号',
    name VARCHAR(200) NOT NULL COMMENT '设备名称',
    brand VARCHAR(100) COMMENT '品牌',
    model VARCHAR(100) COMMENT '型号',
    serial_number VARCHAR(100) COMMENT '序列号',
    category VARCHAR(50) COMMENT '分类:门禁/监控/网络/楼控等',
    specs VARCHAR(500) COMMENT '规格参数',
    commissioning_date DATE COMMENT '投运日期',
    warranty_expiry DATE COMMENT '保修到期日',
    status VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/FAULT/REPAIRING/SCRAPPED',
    project_id BIGINT COMMENT '归属项目',
    point_id BIGINT COMMENT '归属点位(维护型项目,2A不填)',
    create_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_code (code),
    INDEX idx_project (project_id),
    INDEX idx_status (status)
) COMMENT '设备台账表';

-- 业务模块菜单与权限
INSERT INTO sys_menu (id, parent_id, name, type, permission, path, icon, sort, status, create_time) VALUES
(200, 0, '业务管理', 1, NULL, '/business', 'Briefcase', 2, 1, NOW()),
(201, 200, '项目管理', 2, 'business:project:list', '/business/project', 'FolderKanban', 1, 1, NOW()),
(202, 200, '合同管理', 2, 'business:contract:list', '/business/contract', 'FileText', 2, 1, NOW()),
(203, 200, '报价管理', 2, 'business:quote:list', '/business/quote', 'FileSpreadsheet', 3, 1, NOW()),
(204, 200, '设备台账', 2, 'business:equipment:list', '/business/equipment', 'Server', 4, 1, NOW()),
(2011, 201, '项目新增', 3, 'business:project:create', NULL, NULL, 1, 1, NOW()),
(2012, 201, '项目编辑', 3, 'business:project:update', NULL, NULL, 2, 1, NOW()),
(2013, 201, '项目删除', 3, 'business:project:delete', NULL, NULL, 3, 1, NOW()),
(2021, 202, '合同新增', 3, 'business:contract:create', NULL, NULL, 1, 1, NOW()),
(2022, 202, '合同编辑', 3, 'business:contract:update', NULL, NULL, 2, 1, NOW()),
(2023, 202, '合同删除', 3, 'business:contract:delete', NULL, NULL, 3, 1, NOW()),
(2031, 203, '报价新增', 3, 'business:quote:create', NULL, NULL, 1, 1, NOW()),
(2032, 203, '报价编辑', 3, 'business:quote:update', NULL, NULL, 2, 1, NOW()),
(2033, 203, '报价删除', 3, 'business:quote:delete', NULL, NULL, 3, 1, NOW()),
(2041, 204, '设备新增', 3, 'business:equipment:create', NULL, NULL, 1, 1, NOW()),
(2042, 204, '设备编辑', 3, 'business:equipment:update', NULL, NULL, 2, 1, NOW()),
(2043, 204, '设备删除', 3, 'business:equipment:delete', NULL, NULL, 3, 1, NOW());

-- 给 admin 角色分配业务菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id BETWEEN 200 AND 2043;

-- ========== 唯一索引 ==========

-- 季度结算表:合同+季度唯一
CREATE UNIQUE INDEX IF NOT EXISTS uk_quarterly_settlement ON quarterly_settlement(contract_id, period_no);

-- 维保任务表:项目+点位+类型+计划巡检日期唯一
CREATE UNIQUE INDEX IF NOT EXISTS uk_maintenance_task ON maintenance_task(project_id, point_id, type, plan_inspect_date);
