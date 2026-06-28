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
        createQuarterlySettlementTable();
        createMaintenanceTaskTable();
        createMaintenanceRecordTable();
        createAttachmentTable();
        createContractPaymentTable();
        createApprovalFlowTable();
        createApprovalNodeTable();
        createApprovalLogTable();
        createSysNotificationTable();
        createPreviewLogTable();
        createCustomerTable();
        createSupplierTable();
        createSysDictTable();
        createSysDictItemTable();
        createContractChangeTable();
        ensureAuditColumns();
        seedBusinessMenus();
        System.out.println("[DbInitRunner] 业务模块表初始化完成");
    }

    private void exec(String sql) {
        jdbc.execute(sql);
    }

    /**
     * 为所有继承 BaseEntity 的表补充 create_by / update_by 列(幂等,列已存在则跳过)。
     */
    private void ensureAuditColumns() {
        String[] tables = {
                "project", "contract", "quote", "equipment", "progress", "acceptance",
                "maintenance_point", "maintenance_contract", "point_settlement", "quarterly_settlement",
                "maintenance_task", "maintenance_record", "attachment", "contract_payment",
                "approval_flow", "approval_node", "approval_log", "customer", "supplier",
                "contract_change", "sys_user", "sys_role", "sys_dept", "sys_menu"
        };
        for (String table : tables) {
            try { exec("ALTER TABLE " + table + " ADD COLUMN create_by BIGINT COMMENT '创建人'"); } catch (Exception ignore) {}
            try { exec("ALTER TABLE " + table + " ADD COLUMN update_by BIGINT COMMENT '更新人'"); } catch (Exception ignore) {}
        }
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
        // 补充 version 列(已有表兼容)
        try { exec("ALTER TABLE quote ADD COLUMN version INT DEFAULT 1 COMMENT '版本号' AFTER status"); } catch (Exception ignore) {}
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
                "rectify_count INT DEFAULT 0 COMMENT '整改次数上限3'," +
                "remark VARCHAR(500) COMMENT '验收结论说明'," +
                "create_by BIGINT," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "UNIQUE KEY uk_code (code)," +
                "INDEX idx_business (business_type, business_id)," +
                "INDEX idx_project (project_id)" +
                ") COMMENT '项目验收表'");
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
                "contract_id BIGINT COMMENT '关联合同'," +
                "period_no VARCHAR(20) COMMENT '季度编号如2026-Q2'," +
                "amount DECIMAL(14,2) NOT NULL COMMENT '结算金额(=报价金额)'," +
                "status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CONFIRMED/INVOICED/PARTIAL/RECEIVED/CLOSED'," +
                "invoice_no VARCHAR(100) COMMENT '发票号'," +
                "received_amount DECIMAL(14,2) COMMENT '已回款金额'," +
                "received_date DATE COMMENT '回款日期'," +
                "remark VARCHAR(500) COMMENT '备注'," +
                "create_by BIGINT," +
                "update_by BIGINT," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "UNIQUE KEY uk_code (code)," +
                "UNIQUE KEY uk_quarterly_settlement (contract_id, period_no)," +
                "INDEX idx_point (point_id)," +
                "INDEX idx_project (project_id)" +
                ") COMMENT '点位结算单表'");
    }

    private void createQuarterlySettlementTable() {
        exec("CREATE TABLE IF NOT EXISTS quarterly_settlement (" +
                "id BIGINT PRIMARY KEY," +
                "code VARCHAR(50) NOT NULL COMMENT '结算单编号'," +
                "contract_id BIGINT NOT NULL COMMENT '关联维保主合同'," +
                "project_id BIGINT COMMENT '关联项目'," +
                "period_no INT NOT NULL COMMENT '第几期(1-based)'," +
                "period_start_date DATE NOT NULL COMMENT '本期开始日'," +
                "period_end_date DATE NOT NULL COMMENT '本期结束日'," +
                "amount DECIMAL(14,2) NOT NULL COMMENT '本期维保费金额'," +
                "amount_version INT DEFAULT 1 COMMENT '适用金额版本'," +
                "status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/REVIEWED/CONFIRMED/INVOICED/RECEIVED/CLOSED'," +
                "invoice_no VARCHAR(100) COMMENT '发票号'," +
                "received_amount DECIMAL(14,2) COMMENT '已回款金额'," +
                "received_date DATE COMMENT '回款日期'," +
                "remark VARCHAR(500) COMMENT '备注'," +
                "create_by BIGINT," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "UNIQUE KEY uk_code (code)," +
                "INDEX idx_contract (contract_id)," +
                "INDEX idx_project (project_id)," +
                "INDEX idx_period (contract_id, period_no)" +
                ") COMMENT '季度结算单表'");
    }

    private void createMaintenanceTaskTable() {
        exec("CREATE TABLE IF NOT EXISTS maintenance_task (" +
                "id BIGINT PRIMARY KEY," +
                "code VARCHAR(50) NOT NULL COMMENT '工单编号'," +
                "project_id BIGINT COMMENT '关联项目'," +
                "point_id BIGINT COMMENT '关联点位(维护型项目)'," +
                "equipment_id BIGINT COMMENT '关联设备'," +
                "type VARCHAR(20) NOT NULL COMMENT '任务类型 INSPECTION巡检/REPAIR故障报修'," +
                "title VARCHAR(200) NOT NULL COMMENT '标题'," +
                "description VARCHAR(1000) COMMENT '故障现象/任务描述'," +
                "reporter_id BIGINT COMMENT '报修人(故障报修)'," +
                "handler_id BIGINT COMMENT '处理人(派单)'," +
                "handle_method VARCHAR(1000) COMMENT '处理方法'," +
                "parts_used VARCHAR(500) COMMENT '更换配件'," +
                "status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待派单/ASSIGNED已派单/PROCESSING处理中/WAITING_ACCEPTANCE待验收/COMPLETED已完成/CLOSED已关闭'," +
                "plan_date DATE COMMENT '计划日期(巡检)'," +
                "complete_date DATE COMMENT '完工日期'," +
                "remark VARCHAR(500) COMMENT '备注'," +
                "create_by BIGINT," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "UNIQUE KEY uk_code (code)," +
                "INDEX idx_project (project_id)," +
                "INDEX idx_equipment (equipment_id)," +
                "INDEX idx_status (status)" +
                ") COMMENT '维保任务/工单表'");
        // 补充 plan_inspect_date 列(已有表兼容)
        try {
            exec("ALTER TABLE maintenance_task ADD COLUMN plan_inspect_date DATE COMMENT '计划巡检日期' AFTER plan_date");
        } catch (Exception ignore) {
            // 列已存在则忽略
        }
    }

    private void createMaintenanceRecordTable() {
        exec("CREATE TABLE IF NOT EXISTS maintenance_record (" +
                "id BIGINT PRIMARY KEY," +
                "code VARCHAR(50) NOT NULL COMMENT '记录编号'," +
                "task_id BIGINT COMMENT '关联工单(可空,巡检可直接建记录)'," +
                "project_id BIGINT COMMENT '关联项目'," +
                "point_id BIGINT COMMENT '关联点位'," +
                "equipment_id BIGINT COMMENT '关联设备'," +
                "record_type VARCHAR(20) NOT NULL COMMENT 'INSPECTION巡检/REPAIR维修/MAINTENANCE保养'," +
                "content VARCHAR(2000) NOT NULL COMMENT '维保内容/处理过程'," +
                "parts_used VARCHAR(500) COMMENT '更换配件'," +
                "recorder_id BIGINT COMMENT '记录人'," +
                "record_date DATE NOT NULL COMMENT '记录日期'," +
                "remark VARCHAR(500) COMMENT '备注'," +
                "create_by BIGINT," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "UNIQUE KEY uk_code (code)," +
                "INDEX idx_project (project_id)," +
                "INDEX idx_equipment (equipment_id)," +
                "INDEX idx_task (task_id)" +
                ") COMMENT '维保记录表'");
    }

    private void createAttachmentTable() {
        exec("CREATE TABLE IF NOT EXISTS attachment (" +
                "id BIGINT PRIMARY KEY," +
                "name VARCHAR(200) NOT NULL COMMENT '原始文件名'," +
                "file_path VARCHAR(500) NOT NULL COMMENT '存储相对路径'," +
                "file_size BIGINT COMMENT '文件大小(字节)'," +
                "file_type VARCHAR(100) COMMENT '文件MIME类型'," +
                "business_type VARCHAR(50) NOT NULL COMMENT '业务类型(CONTRACT/QUOTE/EQUIPMENT/ACCEPTANCE/MAINTENANCE等)'," +
                "business_id BIGINT COMMENT '业务ID'," +
                "create_by BIGINT," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "INDEX idx_business (business_type, business_id)" +
                ") COMMENT '附件表'");
    }

    private void createContractPaymentTable() {
        exec("CREATE TABLE IF NOT EXISTS contract_payment (" +
                "id BIGINT PRIMARY KEY," +
                "code VARCHAR(50) NOT NULL COMMENT '单号'," +
                "contract_id BIGINT NOT NULL COMMENT '合同ID'," +
                "type VARCHAR(20) NOT NULL COMMENT 'RECEIVABLE应收/PAYABLE应付'," +
                "plan_date DATE COMMENT '计划日期'," +
                "plan_amount DECIMAL(14,2) COMMENT '计划金额'," +
                "actual_amount DECIMAL(14,2) COMMENT '实际金额'," +
                "actual_date DATE COMMENT '实际日期'," +
                "invoice_no VARCHAR(100) COMMENT '发票号'," +
                "status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/RECEIVED/OVERDUE'," +
                "remark VARCHAR(500)," +
                "create_by BIGINT," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "UNIQUE KEY uk_code (code)," +
                "INDEX idx_contract (contract_id)," +
                "INDEX idx_type (type)," +
                "INDEX idx_status (status)" +
                ") COMMENT '合同收付款表'");
    }

    private void createApprovalFlowTable() {
        exec("CREATE TABLE IF NOT EXISTS approval_flow (" +
                "id BIGINT PRIMARY KEY," +
                "code VARCHAR(50) NOT NULL COMMENT '流程编码'," +
                "name VARCHAR(200) NOT NULL COMMENT '流程名称'," +
                "business_type VARCHAR(30) NOT NULL COMMENT 'CONTRACT_APPROVAL/QUOTE_APPROVAL'," +
                "enabled INT DEFAULT 1 COMMENT '1启用 0禁用'," +
                "remark VARCHAR(500)," +
                "create_by BIGINT," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "UNIQUE KEY uk_code (code)," +
                "INDEX idx_business (business_type)" +
                ") COMMENT '审批流定义表'");
    }

    private void createApprovalNodeTable() {
        exec("CREATE TABLE IF NOT EXISTS approval_node (" +
                "id BIGINT PRIMARY KEY," +
                "flow_id BIGINT NOT NULL COMMENT '流程ID'," +
                "node_order INT NOT NULL COMMENT '节点顺序'," +
                "node_name VARCHAR(200) NOT NULL COMMENT '节点名称'," +
                "approver_role_id BIGINT COMMENT '审批角色ID'," +
                "amount_threshold DECIMAL(14,2) COMMENT '金额阈值(合同金额>=阈值才走该节点)'," +
                "create_by BIGINT," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "INDEX idx_flow (flow_id)," +
                "INDEX idx_order (flow_id, node_order)" +
                ") COMMENT '审批节点表'");
    }

    private void createApprovalLogTable() {
        exec("CREATE TABLE IF NOT EXISTS approval_log (" +
                "id BIGINT PRIMARY KEY," +
                "flow_id BIGINT NOT NULL COMMENT '流程ID'," +
                "business_type VARCHAR(30) NOT NULL COMMENT 'CONTRACT_APPROVAL/QUOTE_APPROVAL'," +
                "business_id BIGINT NOT NULL COMMENT '业务ID'," +
                "node_order INT NOT NULL COMMENT '当前节点序号'," +
                "approver_id BIGINT COMMENT '审批人ID'," +
                "result VARCHAR(20) COMMENT 'APPROVED/REJECTED(null=待审批)'," +
                "opinion VARCHAR(500) COMMENT '审批意见'," +
                "approve_time DATETIME COMMENT '审批时间'," +
                "create_by BIGINT," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "INDEX idx_business (business_type, business_id)," +
                "INDEX idx_approver (approver_id)," +
                "INDEX idx_result (result)" +
                ") COMMENT '审批记录表'");
        // 补充 contract/quote 表 approval_status 列(已有表兼容)
        try { exec("ALTER TABLE contract ADD COLUMN approval_status VARCHAR(20) DEFAULT 'NONE' COMMENT 'NONE/PENDING/APPROVED/REJECTED' AFTER status"); } catch (Exception ignore) {}
        try { exec("ALTER TABLE quote ADD COLUMN approval_status VARCHAR(20) DEFAULT 'NONE' COMMENT 'NONE/PENDING/APPROVED/REJECTED' AFTER status"); } catch (Exception ignore) {}
    }

    private void createSysNotificationTable() {
        exec("CREATE TABLE IF NOT EXISTS sys_notification (" +
                "id BIGINT PRIMARY KEY," +
                "user_id BIGINT NOT NULL COMMENT '接收人'," +
                "title VARCHAR(200) NOT NULL COMMENT '标题'," +
                "content VARCHAR(1000) COMMENT '内容'," +
                "type VARCHAR(30) COMMENT 'SETTLEMENT/APPROVAL/OVERDUE/WARRANTY/ACCEPTANCE'," +
                "business_type VARCHAR(30) COMMENT '业务类型'," +
                "business_id BIGINT COMMENT '业务ID'," +
                "is_read TINYINT DEFAULT 0 COMMENT '0未读 1已读'," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "INDEX idx_user (user_id, is_read)," +
                "INDEX idx_type (type)" +
                ") COMMENT '站内通知表'");
    }

    private void createPreviewLogTable() {
        exec("CREATE TABLE IF NOT EXISTS preview_log (" +
                "id BIGINT PRIMARY KEY," +
                "user_id BIGINT COMMENT '预览人'," +
                "attachment_id BIGINT NOT NULL COMMENT '附件ID'," +
                "attachment_name VARCHAR(255) COMMENT '附件名称'," +
                "business_type VARCHAR(30) COMMENT '业务类型'," +
                "business_id BIGINT COMMENT '业务ID'," +
                "ip VARCHAR(64) COMMENT 'IP地址'," +
                "preview_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '预览时间'," +
                "INDEX idx_user (user_id)," +
                "INDEX idx_attachment (attachment_id)," +
                "INDEX idx_preview_time (preview_time)" +
                ") COMMENT '文件预览日志表'");
    }

    private void createCustomerTable() {
        exec("CREATE TABLE IF NOT EXISTS customer (" +
                "id BIGINT PRIMARY KEY," +
                "code VARCHAR(50) NOT NULL COMMENT '客户编号'," +
                "name VARCHAR(200) NOT NULL COMMENT '客户名称'," +
                "contact_person VARCHAR(100) COMMENT '联系人'," +
                "contact_phone VARCHAR(50) COMMENT '联系电话'," +
                "contact_email VARCHAR(100) COMMENT '联系邮箱'," +
                "address VARCHAR(500) COMMENT '地址'," +
                "bank_account VARCHAR(100) COMMENT '银行账号'," +
                "bank_name VARCHAR(100) COMMENT '开户行'," +
                "remark VARCHAR(500) COMMENT '备注'," +
                "create_by BIGINT," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "UNIQUE KEY uk_code (code)," +
                "INDEX idx_name (name)" +
                ") COMMENT '客户档案表'");
    }

    private void createSupplierTable() {
        exec("CREATE TABLE IF NOT EXISTS supplier (" +
                "id BIGINT PRIMARY KEY," +
                "code VARCHAR(50) NOT NULL COMMENT '供应商编号'," +
                "name VARCHAR(200) NOT NULL COMMENT '供应商名称'," +
                "contact_person VARCHAR(100) COMMENT '联系人'," +
                "contact_phone VARCHAR(50) COMMENT '联系电话'," +
                "contact_email VARCHAR(100) COMMENT '联系邮箱'," +
                "address VARCHAR(500) COMMENT '地址'," +
                "bank_account VARCHAR(100) COMMENT '银行账号'," +
                "bank_name VARCHAR(100) COMMENT '开户行'," +
                "remark VARCHAR(500) COMMENT '备注'," +
                "create_by BIGINT," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "UNIQUE KEY uk_code (code)," +
                "INDEX idx_name (name)" +
                ") COMMENT '供应商档案表'");
    }

    private void createSysDictTable() {
        exec("CREATE TABLE IF NOT EXISTS sys_dict (" +
                "id BIGINT PRIMARY KEY," +
                "code VARCHAR(50) NOT NULL COMMENT '字典编码'," +
                "name VARCHAR(100) NOT NULL COMMENT '字典名称'," +
                "remark VARCHAR(500) COMMENT '备注'," +
                "UNIQUE KEY uk_code (code)" +
                ") COMMENT '数据字典表'");
    }

    private void createSysDictItemTable() {
        exec("CREATE TABLE IF NOT EXISTS sys_dict_item (" +
                "id BIGINT PRIMARY KEY," +
                "dict_id BIGINT NOT NULL COMMENT '字典ID'," +
                "label VARCHAR(100) NOT NULL COMMENT '字典项标签'," +
                "value VARCHAR(100) NOT NULL COMMENT '字典项值'," +
                "sort INT DEFAULT 0 COMMENT '排序'," +
                "INDEX idx_dict (dict_id)" +
                ") COMMENT '数据字典项表'");
    }

    private void createContractChangeTable() {
        exec("CREATE TABLE IF NOT EXISTS contract_change (" +
                "id BIGINT PRIMARY KEY," +
                "contract_id BIGINT NOT NULL COMMENT '合同ID'," +
                "change_type VARCHAR(30) NOT NULL COMMENT 'AMOUNT_CHANGE/SCOPE_CHANGE/TERM_CHANGE/OTHER'," +
                "change_field VARCHAR(30) COMMENT '变更字段:AMOUNT/START_DATE/END_DATE'," +
                "new_amount DECIMAL(14,2) COMMENT '变更后金额'," +
                "new_date DATE COMMENT '变更后日期'," +
                "change_desc VARCHAR(1000) COMMENT '变更说明'," +
                "supplement_file_id BIGINT COMMENT '补充附件ID'," +
                "approver_id BIGINT COMMENT '审核人ID'," +
                "approve_time DATETIME COMMENT '审核时间'," +
                "status VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/PENDING/APPROVED/REJECTED'," +
                "remark VARCHAR(500) COMMENT '备注'," +
                "create_by BIGINT," +
                "update_by BIGINT," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "INDEX idx_contract (contract_id)," +
                "INDEX idx_status (status)" +
                ") COMMENT '合同变更记录表'");
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
                {"2103", "210", "结算单删除", "3", "business:pointSettlement:delete", "", "", "3"},
                {"211", "207", "季度结算", "2", "business:quarterlySettlement:list", "/business/quarterly-settlement", "CalendarClock", "4"},
                {"2111", "211", "生成结算单", "3", "business:quarterlySettlement:create", "", "", "1"},
                {"2112", "211", "调整结算单", "3", "business:quarterlySettlement:update", "", "", "2"},
                {"2113", "211", "删除结算单", "3", "business:quarterlySettlement:delete", "", "", "3"},
                {"212", "0", "附件管理", "2", "business:attachment:list", "/business/attachment", "Paperclip", "4"},
                {"2121", "212", "上传", "3", "business:attachment:upload", "", "", "1"},
                {"2122", "212", "删除", "3", "business:attachment:delete", "", "", "2"},
                {"213", "207", "维保任务", "2", "business:maintenanceTask:list", "/business/maintenance-task", "Wrench", "5"},
                {"214", "207", "维保记录", "2", "business:maintenanceRecord:list", "/business/maintenance-record", "ClipboardList", "6"},
                {"215", "0", "结算看板", "2", "business:dashboard:list", "/business/dashboard/settlement", "BarChart3", "5"},
                {"2131", "213", "工单新增", "3", "business:maintenanceTask:create", "", "", "1"},
                {"2132", "213", "工单编辑", "3", "business:maintenanceTask:update", "", "", "2"},
                {"2133", "213", "工单删除", "3", "business:maintenanceTask:delete", "", "", "3"},
                {"2141", "214", "记录新增", "3", "business:maintenanceRecord:create", "", "", "1"},
                {"2142", "214", "记录编辑", "3", "business:maintenanceRecord:update", "", "", "2"},
                {"2143", "214", "记录删除", "3", "business:maintenanceRecord:delete", "", "", "3"},
                {"2151", "215", "查询", "3", "business:dashboard:list", "", "", "1"},
                {"216", "200", "合同收付款", "2", "business:contractPayment:list", "/business/contract-payment", "Money", "7"},
                {"2161", "216", "新增", "3", "business:contractPayment:create", "", "", "1"},
                {"2162", "216", "编辑", "3", "business:contractPayment:update", "", "", "2"},
                {"2163", "216", "删除", "3", "business:contractPayment:delete", "", "", "3"},
                {"217", "200", "审批中心", "2", "business:approval:list", "/business/approval", "Stamp", "8"},
                {"2171", "217", "发起审批", "3", "business:approval:start", "", "", "1"},
                {"2172", "217", "审批操作", "3", "business:approval:approve", "", "", "2"},
                {"218", "200", "审批流配置", "2", "business:approval:config", "/business/approval/flow-config", "Setting", "9"},
                {"219", "0", "消息通知", "2", "system:notification:list", "", "", "99"},
                {"220", "0", "报表中心", "2", "business:report:export", "/business/report", "FileSpreadsheet", "6"},
                {"221", "0", "维保统计", "2", "business:maintenanceStat:list", "/business/maintenance-stat", "BarChart3", "7"},
                {"222", "0", "客户档案", "2", "business:customer:list", "/business/customer", "Users", "8"},
                {"2221", "222", "新增", "3", "business:customer:create", "", "", "1"},
                {"2222", "222", "编辑", "3", "business:customer:update", "", "", "2"},
                {"2223", "222", "删除", "3", "business:customer:delete", "", "", "3"},
                {"223", "0", "供应商档案", "2", "business:supplier:list", "/business/supplier", "Truck", "9"},
                {"2231", "223", "新增", "3", "business:supplier:create", "", "", "1"},
                {"2232", "223", "编辑", "3", "business:supplier:update", "", "", "2"},
                {"2233", "223", "删除", "3", "business:supplier:delete", "", "", "3"},
                {"224", "0", "数据字典", "2", "system:dict:list", "/system/dict", "BookMarked", "5"},
                {"2241", "224", "新增", "3", "system:dict:create", "", "", "1"},
                {"2242", "224", "编辑", "3", "system:dict:update", "", "", "2"},
                {"2243", "224", "删除", "3", "system:dict:delete", "", "", "3"},
                {"225", "0", "合同变更", "2", "business:contractChange:list", "/business/contract-change", "FilePenLine", "10"},
                {"2251", "225", "新增", "3", "business:contractChange:create", "", "", "1"},
                {"2252", "225", "编辑", "3", "business:contractChange:update", "", "", "2"},
                {"2253", "225", "删除", "3", "business:contractChange:delete", "", "", "3"},
                {"2254", "225", "审核", "3", "business:contractChange:audit", "", "", "4"}
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
                "SELECT 1, id FROM sys_menu WHERE id BETWEEN 200 AND 2254");
    }
}
