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
        createProgressTable();
        createAcceptanceTable();
        createMaintenancePointTable();
        createMaintenanceContractTable();
        createPointSettlementTable();
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

    private void createProgressTable() {
        exec("CREATE TABLE IF NOT EXISTS progress (" +
                "id BIGINT PRIMARY KEY," +
                "code VARCHAR(50) NOT NULL COMMENT '进度编号'," +
                "project_id BIGINT COMMENT '关联项目'," +
                "business_type VARCHAR(30) NOT NULL DEFAULT 'NEW_BUILD' COMMENT 'NEW_BUILD/MAINTENANCE_POINT'," +
                "business_id BIGINT COMMENT '业务ID'," +
                "node_name VARCHAR(200) NOT NULL COMMENT '节点名称'," +
                "plan_start_date DATE COMMENT '计划开始'," +
                "plan_end_date DATE COMMENT '计划结束'," +
                "actual_start_date DATE COMMENT '实际开始'," +
                "actual_end_date DATE COMMENT '实际结束'," +
                "progress_percent INT DEFAULT 0 COMMENT '进度百分比'," +
                "manager_id BIGINT COMMENT '负责人'," +
                "status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/COMPLETED/OVERDUE'," +
                "remark VARCHAR(500) COMMENT '备注'," +
                "create_by BIGINT," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "UNIQUE KEY uk_code (code)," +
                "INDEX idx_business (business_type, business_id)," +
                "INDEX idx_project (project_id)" +
                ") COMMENT '项目进度表'");
    }

    private void createAcceptanceTable() {
        exec("CREATE TABLE IF NOT EXISTS acceptance (" +
                "id BIGINT PRIMARY KEY," +
                "code VARCHAR(50) NOT NULL COMMENT '验收单编号'," +
                "project_id BIGINT COMMENT '关联项目'," +
                "business_type VARCHAR(30) NOT NULL DEFAULT 'NEW_BUILD' COMMENT 'NEW_BUILD/MAINTENANCE_POINT'," +
                "business_id BIGINT COMMENT '业务ID'," +
                "quote_id BIGINT COMMENT '关联报价'," +
                "acceptor_id BIGINT COMMENT '验收人'," +
                "accept_date DATE COMMENT '验收日期'," +
                "actual_quantity VARCHAR(500) COMMENT '实际工程量'," +
                "result VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PASS/FAIL/RECTIFYING/ARBITRATION'," +
                "rectify_count INT DEFAULT 0 COMMENT '整改次数(上限3)'," +
                "remark VARCHAR(500) COMMENT '验收结论说明'," +
                "create_by BIGINT," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "UNIQUE KEY uk_code (code)," +
                "INDEX idx_business (business_type, business_id)," +
                "INDEX idx_project (project_id)" +
                ") COMMENT '项目验收表')");
    }

    private void createMaintenancePointTable() {
        exec("CREATE TABLE IF NOT EXISTS maintenance_point (" +
                "id BIGINT PRIMARY KEY," +
                "code VARCHAR(50) NOT NULL COMMENT '点位编号'," +
                "project_id BIGINT NOT NULL COMMENT '关联维护型项目'," +
                "name VARCHAR(200) NOT NULL COMMENT '点位名称'," +
                "location VARCHAR(500) COMMENT '位置描述'," +
                "equipment_list VARCHAR(1000) COMMENT '设备/系统清单'," +
                "manager_id BIGINT COMMENT '负责人'," +
                "status VARCHAR(20) NOT NULL DEFAULT 'WAITING_QUOTE' COMMENT 'WAITING_QUOTE/QUOTED/CONSTRUCTING/WAITING_ACCEPTANCE/ACCEPTED/SETTLED'," +
                "create_by BIGINT," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "UNIQUE KEY uk_code (code)," +
                "INDEX idx_project (project_id)" +
                ") COMMENT '维护点位表'");
    }

    private void createMaintenanceContractTable() {
        exec("CREATE TABLE IF NOT EXISTS maintenance_contract (" +
                "id BIGINT PRIMARY KEY," +
                "code VARCHAR(50) NOT NULL COMMENT '合同编号'," +
                "name VARCHAR(200) NOT NULL COMMENT '合同名称'," +
                "project_id BIGINT NOT NULL COMMENT '关联维护型项目'," +
                "party_a VARCHAR(200) COMMENT '甲方'," +
                "party_b VARCHAR(200) COMMENT '乙方'," +
                "sign_date DATE COMMENT '签订日期'," +
                "effective_date DATE NOT NULL COMMENT '合同生效日(季度起算点)'," +
                "total_amount DECIMAL(14,2) NOT NULL COMMENT '合同总额'," +
                "period_months INT NOT NULL COMMENT '合同期月数(必须%3==0且≥6)'," +
                "period_count INT COMMENT '合同期数(=period_months/3)'," +
                "response_sla VARCHAR(200) COMMENT '响应时效'," +
                "scope VARCHAR(1000) COMMENT '维保服务范围'," +
                "status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/TERMINATED/EXPIRED'," +
                "end_date DATE COMMENT '合同到期日(=effective_date+period_months-1天)'," +
                "remark VARCHAR(500) COMMENT '备注'," +
                "create_by BIGINT," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "UNIQUE KEY uk_code (code)," +
                "INDEX idx_project (project_id)" +
                ") COMMENT '维保主合同表'");
    }

    private void createPointSettlementTable() {
        exec("CREATE TABLE IF NOT EXISTS point_settlement (" +
                "id BIGINT PRIMARY KEY," +
                "code VARCHAR(50) NOT NULL COMMENT '结算单编号'," +
                "project_id BIGINT COMMENT '关联项目'," +
                "point_id BIGINT NOT NULL COMMENT '关联点位'," +
                "quote_id BIGINT NOT NULL COMMENT '关联报价'," +
                "acceptance_id BIGINT COMMENT '关联验收单'," +
                "amount DECIMAL(14,2) NOT NULL COMMENT '结算金额(=报价金额)'," +
                "status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CONFIRMED/INVOICED/RECEIVED/CLOSED'," +
                "invoice_no VARCHAR(100) COMMENT '发票号'," +
                "received_amount DECIMAL(14,2) COMMENT '已回款金额'," +
                "received_date DATE COMMENT '回款日期'," +
                "remark VARCHAR(500) COMMENT '备注'," +
                "create_by BIGINT," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "UNIQUE KEY uk_code (code)," +
                "INDEX idx_point (point_id)," +
                "INDEX idx_project (project_id)" +
                ") COMMENT '点位结算单表'");
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
                {"2043", "204", "设备删除", "3", "business:equipment:delete", "", "", "3"},
                {"205", "200", "进度管理", "2", "business:progress:list", "/business/progress", "ListTree", "5"},
                {"206", "200", "验收管理", "2", "business:acceptance:list", "/business/acceptance", "CheckSquare", "6"},
                {"2051", "205", "进度新增", "3", "business:progress:create", "", "", "1"},
                {"2052", "205", "进度编辑", "3", "business:progress:update", "", "", "2"},
                {"2053", "205", "进度删除", "3", "business:progress:delete", "", "", "3"},
                {"2061", "206", "验收新增", "3", "business:acceptance:create", "", "", "1"},
                {"2062", "206", "验收编辑", "3", "business:acceptance:update", "", "", "2"},
                {"2063", "206", "验收删除", "3", "business:acceptance:delete", "", "", "3"},
                {"207", "0", "维护型项目", "1", "", "/business/maintenance", "Briefcase", "3"},
                {"208", "207", "点位管理", "2", "business:maintenancePoint:list", "/business/maintenance-point", "MapPin", "1"},
                {"209", "207", "维保主合同", "2", "business:maintenanceContract:list", "/business/maintenance-contract", "FileSignature", "2"},
                {"210", "207", "点位结算", "2", "business:pointSettlement:list", "/business/point-settlement", "Receipt", "3"},
                {"2081", "208", "点位新增", "3", "business:maintenancePoint:create", "", "", "1"},
                {"2082", "208", "点位编辑", "3", "business:maintenancePoint:update", "", "", "2"},
                {"2083", "208", "点位删除", "3", "business:maintenancePoint:delete", "", "", "3"},
                {"2091", "209", "主合同新增", "3", "business:maintenanceContract:create", "", "", "1"},
                {"2092", "209", "主合同编辑", "3", "business:maintenanceContract:update", "", "", "2"},
                {"2093", "209", "主合同删除", "3", "business:maintenanceContract:delete", "", "", "3"},
                {"2101", "210", "结算单新增", "3", "business:pointSettlement:create", "", "", "1"},
                {"2102", "210", "结算单编辑", "3", "business:pointSettlement:update", "", "", "2"},
                {"2103", "210", "结算单删除", "3", "business:pointSettlement:delete", "", "", "3"}
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
                "SELECT 1, id FROM sys_menu WHERE id BETWEEN 200 AND 2103");
    }
}
