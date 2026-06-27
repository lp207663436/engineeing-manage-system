package com.ems.module.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ems.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_node")
public class ApprovalNode extends BaseEntity {
    private Long flowId;
    private Integer nodeOrder;
    private String nodeName;
    private Long approverRoleId;
    private BigDecimal amountThreshold;
}
