package com.ems.module.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ems.common.PageResult;
import com.ems.common.exception.BusinessException;
import com.ems.module.business.entity.*;
import com.ems.module.business.mapper.*;
import com.ems.module.system.entity.SysUserRole;
import com.ems.module.system.mapper.SysUserMapper;
import com.ems.module.system.mapper.SysUserRoleMapper;
import com.ems.security.context.CurrentUser;
import com.ems.security.context.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    /**
     * 启动审批流程
     */
    @Transactional(rollbackFor = Exception.class)
    public void startApproval(String businessType, Long businessId) {
        ApprovalFlow flow = approvalFlowMapper.selectOne(
                new LambdaQueryWrapper<ApprovalFlow>()
                        .eq(ApprovalFlow::getBusinessType, businessType)
                        .eq(ApprovalFlow::getEnabled, 1));
        if (flow == null) {
            throw new BusinessException("未找到启用的审批流程: " + businessType);
        }

        List<ApprovalNode> nodes = approvalNodeMapper.selectList(
                new LambdaQueryWrapper<ApprovalNode>()
                        .eq(ApprovalNode::getFlowId, flow.getId())
                        .orderByAsc(ApprovalNode::getNodeOrder));
        if (nodes.isEmpty()) {
            throw new BusinessException("审批流程未配置节点");
        }

        ApprovalNode firstNode = nodes.get(0);
        ApprovalLog log = new ApprovalLog();
        log.setFlowId(flow.getId());
        log.setBusinessType(businessType);
        log.setBusinessId(businessId);
        log.setNodeOrder(firstNode.getNodeOrder());
        log.setCreateBy(SecurityContext.getUserId());
        approvalLogMapper.insert(log);

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
        if (user == null) {
            throw new RuntimeException("未登录");
        }

        log.setApproverId(user.getUserId());
        log.setApproveTime(LocalDateTime.now());
        log.setResult(result);
        log.setOpinion(opinion);
        approvalLogMapper.updateById(log);

        if ("REJECTED".equals(result)) {
            updateBusinessApprovalStatus(log.getBusinessType(), log.getBusinessId(), "REJECTED");
            return;
        }

        // APPROVED: 查下一节点
        ApprovalNode nextNode = approvalNodeMapper.selectOne(
                new LambdaQueryWrapper<ApprovalNode>()
                        .eq(ApprovalNode::getFlowId, log.getFlowId())
                        .gt(ApprovalNode::getNodeOrder, log.getNodeOrder())
                        .orderByAsc(ApprovalNode::getNodeOrder)
                        .last("LIMIT 1"));

        if (nextNode != null) {
            ApprovalLog newLog = new ApprovalLog();
            newLog.setFlowId(log.getFlowId());
            newLog.setBusinessType(log.getBusinessType());
            newLog.setBusinessId(log.getBusinessId());
            newLog.setNodeOrder(nextNode.getNodeOrder());
            newLog.setCreateBy(user.getUserId());
            approvalLogMapper.insert(newLog);
        } else {
            updateBusinessApprovalStatus(log.getBusinessType(), log.getBusinessId(), "APPROVED");
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

    private void updateBusinessApprovalStatus(String businessType, Long businessId, String status) {
        if ("CONTRACT_APPROVAL".equals(businessType)) {
            Contract contract = contractMapper.selectById(businessId);
            if (contract != null) {
                contract.setApprovalStatus(status);
                contractMapper.updateById(contract);
            }
        } else if ("QUOTE_APPROVAL".equals(businessType)) {
            Quote quote = quoteMapper.selectById(businessId);
            if (quote != null) {
                quote.setApprovalStatus(status);
                quoteMapper.updateById(quote);
            }
        }
    }
}
