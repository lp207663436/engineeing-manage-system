package com.ems.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

/**
 * 业务模块表初始化(幂等)。
 * 启动时检查并创建业务表、插入菜单权限,避免依赖外部 mysql 客户端。
 */
@Component
@Order(100)
public class DbInitRunner implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    public DbInitRunner(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public void run(String... args) {
        createProjectTable();
        createContractTable();
        createQuoteTable();
        createEquipmentTable();
        seedBusinessMenus();
        System.out.println("[DbInitRunner] 业务模块表初始化完成");
    }

    private void exec(String sql) {
        jdbc.execute(sql);
    }

    private void createProjectTable() {
        exec("CREATE TABLE IF NOT EXISTS project (" +
                "id BIGINT PRIMARY KEY," +
                "code VARCHAR(50) NOT NULL COMMENT '项目编号'," +
                "name VARCHAR(200) NOT NULL COMMENT '项目名称'," +
                "customer_name VARCHAR(200) COMMENT '客户名称'," +
                "manager_id BIGINT COMMENT '项目经理'," +
                "address VARCHAR(500) COMMENT '项目地址'," +
                "start_date DATE COMMENT '开工日期'," +
                "end_date DATE COMMENT '竣工日期'," +
                "type VARCHAR(20) NOT NULL DEFAULT 'NEW_BUILD' COMMENT 'NEW_BUILD/MAINTENANCE'," +
                "status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/IN_PROGRESS/COMPLETED/ARCHIVED'," +
                "description VARCHAR(1000) COMMENT '项目描述'," +
                "create_by BIGINT," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "UNIQUE KEY uk_code (code)," +
                "INDEX idx_type (type)," +
                "INDEX idx_status (status)" +
                ") COMMENT '项目表'");
    }

    private void createContractTable() {
        exec("CREATE TABLE IF NOT EXISTS contract (" +
                "id BIGINT PRIMARY KEY," +
                "code VARCHAR(50) NOT NULL COMMENT '合同编号'," +
                "name VARCHAR(200) NOT NULL COMMENT '合同名称'," +
                "party_a VARCHAR(200) COMMENT '甲方'," +
                "party_b VARCHAR(200) COMMENT '乙方'," +
                "sign_date DATE COMMENT '签订日期'," +
                "amount DECIMAL(14,2) DEFAULT 0 COMMENT '合同金额'," +
                "category VARCHAR(20) COMMENT 'GENERAL/SUB/PURCHASE/MAINTENANCE'," +
                "payment_method VARCHAR(200) COMMENT '付款方式'," +
                "project_id BIGINT COMMENT '关联项目'," +
                "status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/APPROVING/APPROVED/ARCHIVED'," +
                "start_date DATE COMMENT '合同生效日'," +
                "end_date DATE COMMENT '合同到期日'," +
                "remark VARCHAR(500) COMMENT '备注'," +
                "create_by BIGINT," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "UNIQUE KEY uk_code (code)," +
                "INDEX idx_project (project_id)," +
                "INDEX idx_status (status)" +
                ") COMMENT '合同表'");
    }

    private void createQuoteTable() {
        exec("CREATE TABLE IF NOT EXISTS quote (" +
                "id BIGINT PRIMARY KEY," +
                "code VARCHAR(50) NOT NULL COMMENT '报价编号'," +
                "project_id BIGINT COMMENT '关联项目'," +
                "business_type VARCHAR(30) NOT NULL DEFAULT 'NEW_BUILD' COMMENT 'NEW_BUILD/MAINTENANCE_POINT'," +
                "business_id BIGINT COMMENT '业务ID'," +
                "amount DECIMAL(14,2) DEFAULT 0 COMMENT '报价金额'," +
                "quote_date DATE COMMENT '报价日期'," +
                "valid_until DATE COMMENT '报价有效期'," +
                "quote_person VARCHAR(100) COMMENT '报价人'," +
                "customer_name VARCHAR(200) COMMENT '客户名称'," +
                "version INT DEFAULT 1 COMMENT '版本号'," +
                "status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED/APPROVED/CONFIRMED/VOID'," +
                "summary VARCHAR(1000) COMMENT '报价摘要'," +
                "create_by BIGINT," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "UNIQUE KEY uk_code (code)," +
                "INDEX idx_project (project_id)," +
                "INDEX idx_business (business_type, business_id)" +
                ") COMMENT '报价表'");
    }

    private void createEquipmentTable() {
        exec("CREATE TABLE IF NOT EXISTS equipment (" +
                "id BIGINT PRIMARY KEY," +
                "code VARCHAR(50) NOT NULL COMMENT '设备编号'," +
                "name VARCHAR(200) NOT NULL COMMENT '设备名称'," +
                "brand VARCHAR(100) COMMENT '品牌'," +
                "model VARCHAR(100) COMMENT '型号'," +
                "serial_number VARCHAR(100) COMMENT '序列号'," +
                "category VARCHAR(50) COMMENT '分类'," +
                "specs VARCHAR(500) COMMENT '规格参数'," +
                "commissioning_date DATE COMMENT '投运日期'," +
                "warranty_expiry DATE COMMENT '保修到期日'," +
                "status VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/FAULT/REPAIRING/SCRAPPED'," +
                "project_id BIGINT COMMENT '归属项目'," +
                "point_id BIGINT COMMENT '归属点位'," +
                "create_by BIGINT," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "UNIQUE KEY uk_code (code)," +
                "INDEX idx_project (project_id)," +
                "INDEX idx_status (status)" +
                ") COMMENT '设备台账表'");
    }

    private void seedBusinessMenus() {
        // 目录与菜单(INSERT IGNORE 幂等)
        String[][] menus = {
                {"200", "0", "业务管理", "1", "", "/business", "Briefcase", "2"},
                {"201", "200", "项目管理", "2", "business:project:list", "/business/project", "FolderKanban", "1"},
                {"202", "200", "合同管理", "2", "business:contract:list", "/business/contract", "FileText", "2"},
                {"203", "200", "报价管理", "2", "business:quote:list", "/business/quote", "FileSpreadsheet", "3"},
                {"204", "200", "设备台账", "2", "business:equipment:list", "/business/equipment", "Server", "4"},
                {"2011", "201", "项目新增", "3", "business:project:create", "", "", "1"},
                {"2012", "201", "项目编辑", "3", "business:project:update", "", "", "2"},
                {"2013", "201", "项目删除", "3", "business:project:delete", "", "", "3"},
                {"2021", "202", "合同新增", "3", "business:contract:create", "", "", "1"},
                {"2022", "202", "合同编辑", "3", "business:contract:update", "", "", "2"},
                {"2023", "202", "合同删除", "3", "business:contract:delete", "", "", "3"},
                {"2031", "203", "报价新增", "3", "business:quote:create", "", "", "1"},
                {"2032", "203", "报价编辑", "3", "business:quote:update", "", "", "2"},
                {"2033", "203", "报价删除", "3", "business:quote:delete", "", "", "3"},
                {"2041", "204", "设备新增", "3", "business:equipment:create", "", "", "1"},
                {"2042", "204", "设备编辑", "3", "business:equipment:update", "", "", "2"},
                {"2043", "204", "设备删除", "3", "business:equipment:delete", "", "", "3"}
        };
        for (String[] m : menus) {
            jdbc.update("INSERT IGNORE INTO sys_menu (id, parent_id, name, type, permission, path, icon, sort, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)",
                    Long.parseLong(m[0]), Long.parseLong(m[1]), m[2], Integer.parseInt(m[3]),
                    m[4].isEmpty() ? null : m[4],
                    m[5].isEmpty() ? null : m[5],
                    m[6].isEmpty() ? null : m[6],
                    Integer.parseInt(m[7]));
        }
        // 给 admin(role_id=1)分配权限,幂等
        jdbc.update("INSERT IGNORE INTO sys_role_menu (role_id, menu_id) " +
                "SELECT 1, id FROM sys_menu WHERE id BETWEEN 200 AND 2043");
    }
}
