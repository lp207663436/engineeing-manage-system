package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.entity.*;
import com.ems.module.business.mapper.*;
import com.ems.module.system.entity.SysUserRole;
import com.ems.module.system.mapper.SysUserMapper;
import com.ems.module.system.mapper.SysUserRoleMapper;
import com.ems.module.system.service.SysNotificationService;
import com.ems.security.context.CurrentUser;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalFlowMapper approvalFlowMapper;
    private final ApprovalNodeMapper approvalNodeMapper;
    private final ApprovalLogMapper approvalLogMapper;
    private final ContractMapper contractMapper;
    private final QuoteMapper quoteMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysNotificationService notificationService;

    /**
     * 启动审批流程
     */
    @Transactional(rollbackFor = Exception.class)
    public void startApproval(String businessType, Long businessId) {
        // businessType 白名单校验,防止任意类型注入
        if (!"CONTRACT_APPROVAL".equals(businessType) && !"QUOTE_APPROVAL".equals(businessType)) {
            throw new BusinessException("不支持的审批业务类型: " + businessType);
        }
        // 重复发起审批校验:根据业务实体 approvalStatus 判断
        String currentStatus = getBusinessApprovalStatus(businessType, businessId);
        if ("PENDING".equals(currentStatus)) {
            throw new BusinessException("审批进行中,无法重复发起");
        }
        if ("APPROVED".equals(currentStatus)) {
            throw new BusinessException("已通过,无法重新发起");
        }
        // REJECTED 或 NONE 允许发起

        ApprovalFlow flow = approvalFlowMapper.selectOne(
                new LambdaQueryWrapper<ApprovalFlow>()
                        .eq(ApprovalFlow::getBusinessType, businessType)
                        .eq(ApprovalFlow::getEnabled, 1));
        if (flow == null) {
            throw new BusinessException("未找到启用的审批流程: " + businessType);
        }

        // 金额阈值路由:根据业务金额查找 amount_threshold >= 业务金额 的第一个节点作为起始节点
        BigDecimal businessAmount = getBusinessAmount(businessType, businessId);
        List<ApprovalNode> nodes = approvalNodeMapper.selectList(
                new LambdaQueryWrapper<ApprovalNode>()
                        .eq(ApprovalNode::getFlowId, flow.getId())
                        .orderByAsc(ApprovalNode::getNodeOrder));
        if (nodes.isEmpty()) {
            throw new BusinessException("审批流程未配置节点");
        }

        ApprovalNode firstNode = null;
        for (ApprovalNode node : nodes) {
            // amount_threshold 为 NULL 表示无下限,始终满足;业务金额 >= 阈值时命中该节点
            if (node.getAmountThreshold() == null
                    || node.getAmountThreshold().compareTo(businessAmount) <= 0) {
                firstNode = node;
                break;
            }
        }
        // 如果所有节点阈值都大于业务金额,取第一个节点作为兜底(最小金额走第一个节点)
        if (firstNode == null) {
            firstNode = nodes.get(0);
        }
        ApprovalLog log = new ApprovalLog();
        log.setFlowId(flow.getId());
        log.setBusinessType(businessType);
        log.setBusinessId(businessId);
        log.setNodeOrder(firstNode.getNodeOrder());
        log.setCreateBy(SecurityContext.getUserId());
        approvalLogMapper.insert(log);

        // 通知首节点审批人(事务提交后发送,避免事务回滚后误发通知)
        final ApprovalNode firstNodeForNotify = firstNode;
        final String btForNotify = businessType;
        final Long bidForNotify = businessId;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notifyApprovers(firstNodeForNotify, btForNotify, bidForNotify, "待审批:业务ID " + bidForNotify);
            }
        });

        updateBusinessApprovalStatus(businessType, businessId, "PENDING");
    }

    /**
     * 审批
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long logId, String result, String opinion) {
        ApprovalLog log = approvalLogMapper.selectById(logId);
        if (log == null) {
            throw new BusinessException("审批记录不存在");
        }
        if (log.getResult() != null) {
            throw new BusinessException("该审批记录已处理");
        }

        CurrentUser user = SecurityContext.get();
        if (user == null || user.getUserId() == null) {
            throw new BusinessException(401, "未登录");
        }

        // 审批人越权校验:当前用户角色需匹配节点 approverRoleId,超管(userId==1)可绕过
        ApprovalNode currentNode = approvalNodeMapper.selectOne(
                new LambdaQueryWrapper<ApprovalNode>()
                        .eq(ApprovalNode::getFlowId, log.getFlowId())
                        .eq(ApprovalNode::getNodeOrder, log.getNodeOrder()));
        if (currentNode != null && currentNode.getApproverRoleId() != null
                && (user.getUserId() == null || !SecurityContext.isAdmin())) {
            List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                    new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getUserId()));
            Set<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
            if (!roleIds.contains(currentNode.getApproverRoleId())) {
                throw new BusinessException("无权审批该节点");
            }
        }

        // 审批结果校验:必须为 APPROVED 或 REJECTED(校验在 DB 更新之前)
        if (!"APPROVED".equals(result) && !"REJECTED".equals(result)) {
            throw new BusinessException("无效的审批结果");
        }

        // 并发审批竞态条件:用 SQL 条件更新(result IS NULL)避免并发重复审批
        int updated = approvalLogMapper.update(null,
                new LambdaUpdateWrapper<ApprovalLog>()
                        .eq(ApprovalLog::getId, logId)
                        .isNull(ApprovalLog::getResult)
                        .set(ApprovalLog::getResult, result)
                        .set(ApprovalLog::getApproverId, user.getUserId())
                        .set(ApprovalLog::getApproveTime, LocalDateTime.now())
                        .set(ApprovalLog::getOpinion, opinion));
        if (updated == 0) {
            throw new BusinessException("该审批已被处理或不存在");
        }
        // 重新查询 log 获取完整数据用于后续流程
        log = approvalLogMapper.selectById(logId);

        if ("REJECTED".equals(result)) {
            updateBusinessApprovalStatus(log.getBusinessType(), log.getBusinessId(), "REJECTED");
            return;
        }

        // 金额阈值路由:审批通过后查找下一节点,跳过业务金额未达到阈值的节点
        BigDecimal businessAmount = getBusinessAmount(log.getBusinessType(), log.getBusinessId());
        List<ApprovalNode> candidateNodes = approvalNodeMapper.selectList(
                new LambdaQueryWrapper<ApprovalNode>()
                        .eq(ApprovalNode::getFlowId, log.getFlowId())
                        .gt(ApprovalNode::getNodeOrder, log.getNodeOrder())
                        .orderByAsc(ApprovalNode::getNodeOrder));

        ApprovalNode nextNode = null;
        for (ApprovalNode candidate : candidateNodes) {
            // 如果节点有阈值且业务金额未达到阈值,则跳过该节点
            if (candidate.getAmountThreshold() != null
                    && businessAmount.compareTo(candidate.getAmountThreshold()) < 0) {
                continue;
            }
            nextNode = candidate;
            break;
        }

        if (nextNode != null) {
            ApprovalLog newLog = new ApprovalLog();
            newLog.setFlowId(log.getFlowId());
            newLog.setBusinessType(log.getBusinessType());
            newLog.setBusinessId(log.getBusinessId());
            newLog.setNodeOrder(nextNode.getNodeOrder());
            newLog.setCreateBy(user.getUserId());
            approvalLogMapper.insert(newLog);

            // 通知下一节点审批人(事务提交后发送,避免事务回滚后误发通知)
            final ApprovalNode nextNodeForNotify = nextNode;
            final String btForNotify = log.getBusinessType();
            final Long bidForNotify = log.getBusinessId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notifyApprovers(nextNodeForNotify, btForNotify, bidForNotify, "待审批:业务ID " + bidForNotify);
                }
            });
        } else {
            updateBusinessApprovalStatus(log.getBusinessType(), log.getBusinessId(), "APPROVED");
        }
    }

    /**
     * 通知节点的审批人(按 approverRoleId 查找角色用户发通知)
     */
    private void notifyApprovers(ApprovalNode node, String businessType, Long businessId, String title) {
        Long approverRoleId = node.getApproverRoleId();
        if (approverRoleId == null) {
            return;
        }
        List<SysUserRole> roleUsers = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, approverRoleId));
        for (SysUserRole ur : roleUsers) {
            notificationService.send(ur.getUserId(),
                    title,
                    "您有新的审批任务待处理,业务类型: " + businessType
                            + ",业务ID: " + businessId,
                    "APPROVAL", businessType, businessId);
        }
    }

    /**
     * 查询当前用户的待审批列表(按角色匹配)
     */
    public List<ApprovalLog> getPending(Long approverId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, approverId));
        Set<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<ApprovalLog> pendingLogs = approvalLogMapper.selectList(
                new LambdaQueryWrapper<ApprovalLog>()
                        .isNull(ApprovalLog::getResult)
                        .orderByDesc(ApprovalLog::getCreateTime));
        if (pendingLogs.isEmpty()) {
            return Collections.emptyList();
        }

        List<ApprovalLog> result = new ArrayList<>();
        for (ApprovalLog log : pendingLogs) {
            ApprovalNode node = approvalNodeMapper.selectOne(
                    new LambdaQueryWrapper<ApprovalNode>()
                            .eq(ApprovalNode::getFlowId, log.getFlowId())
                            .eq(ApprovalNode::getNodeOrder, log.getNodeOrder()));
            if (node != null && roleIds.contains(node.getApproverRoleId())) {
                result.add(log);
            }
        }
        return result;
    }

    /**
     * 查询当前用户已审批记录
     */
    public List<ApprovalLog> getHistory(Long approverId) {
        return approvalLogMapper.selectList(
                new LambdaQueryWrapper<ApprovalLog>()
                        .eq(ApprovalLog::getApproverId, approverId)
                        .isNotNull(ApprovalLog::getResult)
                        .orderByDesc(ApprovalLog::getApproveTime));
    }

    /**
     * 分页查询
     */
    public PageResult<ApprovalLog> page(long pageNum, long pageSize, String businessType, Long businessId, String result) {
        LambdaQueryWrapper<ApprovalLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(businessType)) wrapper.eq(ApprovalLog::getBusinessType, businessType);
        if (businessId != null) wrapper.eq(ApprovalLog::getBusinessId, businessId);
        if (StringUtils.hasText(result)) wrapper.eq(ApprovalLog::getResult, result);
        wrapper.orderByDesc(ApprovalLog::getCreateTime);
        Page<ApprovalLog> page = approvalLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    /**
     * 查单条
     */
    public ApprovalLog get(Long id) {
        ApprovalLog log = approvalLogMapper.selectById(id);
        if (log == null) throw new BusinessException("审批记录不存在");
        return log;
    }

    /**
     * 查某业务的审批进度
     */
    public List<ApprovalLog> listByBusiness(String businessType, Long businessId) {
        return approvalLogMapper.selectList(
                new LambdaQueryWrapper<ApprovalLog>()
                        .eq(ApprovalLog::getBusinessType, businessType)
                        .eq(ApprovalLog::getBusinessId, businessId)
                        .orderByAsc(ApprovalLog::getNodeOrder));
    }

    // ==================== 审批流配置 CRUD ====================

    public ApprovalFlow createFlow(ApprovalFlow flow) {
        flow.setCreateBy(SecurityContext.getUserId());
        approvalFlowMapper.insert(flow);
        return flow;
    }

    public void updateFlow(ApprovalFlow flow) {
        ApprovalFlow existing = approvalFlowMapper.selectById(flow.getId());
        if (existing == null) throw new BusinessException("审批流程不存在");
        BeanUtils.copyProperties(flow, existing);
        approvalFlowMapper.updateById(existing);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteFlow(Long id) {
        approvalFlowMapper.deleteById(id);
        approvalNodeMapper.delete(new LambdaQueryWrapper<ApprovalNode>().eq(ApprovalNode::getFlowId, id));
    }

    public List<ApprovalFlow> listFlows() {
        return approvalFlowMapper.selectList(
                new LambdaQueryWrapper<ApprovalFlow>().orderByDesc(ApprovalFlow::getCreateTime));
    }

    public List<ApprovalNode> listNodes(Long flowId) {
        return approvalNodeMapper.selectList(
                new LambdaQueryWrapper<ApprovalNode>()
                        .eq(ApprovalNode::getFlowId, flowId)
                        .orderByAsc(ApprovalNode::getNodeOrder));
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveNodes(Long flowId, List<ApprovalNode> nodes) {
        approvalNodeMapper.delete(new LambdaQueryWrapper<ApprovalNode>().eq(ApprovalNode::getFlowId, flowId));
        if (nodes == null) return;
        Long userId = SecurityContext.getUserId();
        for (ApprovalNode node : nodes) {
            node.setId(null);
            node.setFlowId(flowId);
            node.setCreateBy(userId);
            approvalNodeMapper.insert(node);
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 获取业务实体的金额(用于金额阈值路由)
     */
    private BigDecimal getBusinessAmount(String businessType, Long businessId) {
        if ("CONTRACT_APPROVAL".equals(businessType)) {
            Contract contract = contractMapper.selectById(businessId);
            if (contract != null && contract.getAmount() != null) {
                return contract.getAmount();
            }
        } else if ("QUOTE_APPROVAL".equals(businessType)) {
            Quote quote = quoteMapper.selectById(businessId);
            if (quote != null && quote.getAmount() != null) {
                return quote.getAmount();
            }
        }
        return BigDecimal.ZERO;
    }

    private void updateBusinessApprovalStatus(String businessType, Long businessId, String status) {
        if ("CONTRACT_APPROVAL".equals(businessType)) {
            Contract contract = contractMapper.selectById(businessId);
            if (contract == null) {
                throw new BusinessException("业务实体不存在");
            }
            contract.setApprovalStatus(status);
            contractMapper.updateById(contract);
        } else if ("QUOTE_APPROVAL".equals(businessType)) {
            Quote quote = quoteMapper.selectById(businessId);
            if (quote == null) {
                throw new BusinessException("业务实体不存在");
            }
            quote.setApprovalStatus(status);
            quoteMapper.updateById(quote);
        }
    }

    /**
     * 查询业务实体的当前审批状态
     */
    private String getBusinessApprovalStatus(String businessType, Long businessId) {
        if ("CONTRACT_APPROVAL".equals(businessType)) {
            Contract contract = contractMapper.selectById(businessId);
            if (contract == null) {
                throw new BusinessException("业务实体不存在");
            }
            return contract.getApprovalStatus();
        } else if ("QUOTE_APPROVAL".equals(businessType)) {
            Quote quote = quoteMapper.selectById(businessId);
            if (quote == null) {
                throw new BusinessException("业务实体不存在");
            }
            return quote.getApprovalStatus();
        }
        return null;
    }
}
