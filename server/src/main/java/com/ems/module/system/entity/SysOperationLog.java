package com.ems.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 * 记录用户的关键业务操作,用于安全审计。
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 操作人 ID */
    private Long userId;

    /** 操作人用户名 */
    private String username;

    /** 模块名(如:用户管理、角色管理) */
    private String module;

    /** 操作描述(如:新增、修改、删除) */
    private String operation;

    /** 请求方法(类名.方法名) */
    private String method;

    /** 请求参数(JSON) */
    private String params;

    /** 操作结果(成功/失败+异常信息) */
    private String result;

    /** 请求 IP */
    private String ip;

    /** 耗时(毫秒) */
    private Long costTime;

    /** 操作时间 */
    private LocalDateTime createTime;
}
