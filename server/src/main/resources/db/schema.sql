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
