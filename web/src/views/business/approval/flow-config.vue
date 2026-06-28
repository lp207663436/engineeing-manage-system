<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  approvalApi,
  type ApprovalFlowDTO,
  type ApprovalNodeDTO,
} from '@/api/business'
import { roleApi } from '@/api/system'

interface RoleOption { label: string; value: string }

const flowLoading = ref(false)
const flowList = ref<ApprovalFlowDTO[]>([])
const currentFlow = ref<ApprovalFlowDTO | null>(null)

const nodeLoading = ref(false)
const nodeList = ref<ApprovalNodeDTO[]>([])
const roleOptions = ref<RoleOption[]>([])

const flowDialogVisible = ref(false)
const flowDialogTitle = ref('')
const flowFormRef = ref<FormInstance>()
const flowForm = reactive<ApprovalFlowDTO>({
  code: '',
  name: '',
  businessType: 'CONTRACT_APPROVAL',
  enabled: 1,
  remark: '',
})
const flowIsEdit = ref(false)

const businessTypeMap: Record<string, string> = {
  CONTRACT_APPROVAL: '合同审批',
  QUOTE_APPROVAL: '报价审批',
}

const flowRules = {
  code: [{ required: true, message: '请输入流程编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入流程名称', trigger: 'blur' }],
  businessType: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
}

async function loadFlows() {
  flowLoading.value = true
  try {
    const res: any = await approvalApi.flowList()
    flowList.value = res || []
    if (flowList.value.length > 0 && !currentFlow.value) {
      handleSelectFlow(flowList.value[0])
    }
  } finally {
    flowLoading.value = false
  }
}

async function loadRoles() {
  try {
    const res: any = await roleApi.page({ pageNum: 1, pageSize: 200 })
    roleOptions.value = (res.list || []).map((r: any) => ({
      label: r.name,
      value: r.id,
    }))
  } catch {}
}

async function loadNodes(flowId: string) {
  nodeLoading.value = true
  try {
    const res: any = await approvalApi.flowNodes(flowId)
    nodeList.value = res || []
  } finally {
    nodeLoading.value = false
  }
}

function handleSelectFlow(row: ApprovalFlowDTO | null) {
  if (!row) {
    currentFlow.value = null
    nodeList.value = []
    return
  }
  currentFlow.value = row
  if (row.id) loadNodes(row.id)
  else nodeList.value = []
}

function handleAddFlow() {
  flowIsEdit.value = false
  flowDialogTitle.value = '新增审批流'
  Object.assign(flowForm, {
    id: undefined,
    code: '',
    name: '',
    businessType: 'CONTRACT_APPROVAL',
    enabled: 1,
    remark: '',
  })
  flowDialogVisible.value = true
}

function handleEditFlow(row: ApprovalFlowDTO) {
  flowIsEdit.value = true
  flowDialogTitle.value = '编辑审批流'
  Object.assign(flowForm, {
    id: row.id,
    code: row.code,
    name: row.name,
    businessType: row.businessType || 'CONTRACT_APPROVAL',
    enabled: row.enabled ?? 1,
    remark: row.remark,
  })
  flowDialogVisible.value = true
}

async function handleSubmitFlow() {
  if (!flowFormRef.value) return
  await flowFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await approvalApi.flowSave(flowForm)
      ElMessage.success('保存成功')
      flowDialogVisible.value = false
      loadFlows()
    } catch {}
  })
}

async function handleDeleteFlow(row: ApprovalFlowDTO) {
  try {
    await ElMessageBox.confirm(`确定删除审批流「${row.name}」吗?`, '提示', { type: 'warning' })
    await approvalApi.flowDelete(row.id!)
    ElMessage.success('删除成功')
    if (currentFlow.value?.id === row.id) {
      currentFlow.value = null
      nodeList.value = []
    }
    loadFlows()
  } catch {}
}

function handleAddNode() {
  if (!currentFlow.value?.id) {
    ElMessage.warning('请先选择左侧审批流')
    return
  }
  const nextOrder = nodeList.value.length > 0
    ? Math.max(...nodeList.value.map((n) => n.nodeOrder || 0)) + 1
    : 1
  nodeList.value.push({
    flowId: currentFlow.value.id,
    nodeOrder: nextOrder,
    nodeName: '',
    approverRoleId: undefined,
    amountThreshold: undefined,
  })
}

function handleRemoveNode(index: number) {
  nodeList.value.splice(index, 1)
}

async function handleSaveNodes() {
  if (!currentFlow.value?.id) return
  if (nodeList.value.length === 0) {
    ElMessage.warning('请至少添加一个节点')
    return
  }
  for (const n of nodeList.value) {
    if (!n.nodeName) {
      ElMessage.warning('节点名称不能为空')
      return
    }
  }
  try {
    await approvalApi.flowSaveNodes(currentFlow.value.id, nodeList.value)
    ElMessage.success('节点保存成功')
    loadNodes(currentFlow.value.id)
  } catch {}
}

onMounted(() => {
  loadRoles()
  loadFlows()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>审批流配置</h1>
    </div>

    <div class="flow-layout">
      <!-- 左侧:审批流列表 -->
      <div class="card flow-list-pane">
        <div class="toolbar">
          <el-button type="primary" @click="handleAddFlow">+ 新增流程</el-button>
        </div>
        <el-table
          v-loading="flowLoading"
          :data="flowList"
          row-key="id"
          highlight-current-row
          style="width: 100%"
          @current-change="handleSelectFlow"
        >
          <el-table-column prop="code" label="编码" min-width="120" />
          <el-table-column prop="name" label="名称" min-width="140" />
          <el-table-column label="业务类型" width="120">
            <template #default="{ row }">
              {{ businessTypeMap[row.businessType] || row.businessType }}
            </template>
          </el-table-column>
          <el-table-column label="启用" width="80">
            <template #default="{ row }">
              <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small" effect="light">
                {{ row.enabled === 1 ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click.stop="handleEditFlow(row as ApprovalFlowDTO)">编辑</el-button>
              <el-button link type="danger" size="small" @click.stop="handleDeleteFlow(row as ApprovalFlowDTO)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 右侧:节点配置 -->
      <div class="card node-pane">
        <div v-if="!currentFlow" class="empty-tip">请选择左侧审批流</div>
        <template v-else>
          <div class="node-header">
            <div class="node-title">
              <span class="title-text">{{ currentFlow.name }}</span>
              <el-tag size="small" effect="light" style="margin-left: 8px">
                {{ businessTypeMap[currentFlow.businessType || ''] || currentFlow.businessType }}
              </el-tag>
            </div>
            <div>
              <el-button type="primary" size="small" @click="handleAddNode">+ 添加节点</el-button>
              <el-button type="success" size="small" @click="handleSaveNodes">保存节点</el-button>
            </div>
          </div>
          <el-table v-loading="nodeLoading" :data="nodeList" row-key="id" style="width: 100%">
            <el-table-column label="序号" width="100">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.nodeOrder"
                  :min="1"
                  :precision="0"
                  controls-position="right"
                  style="width: 90px"
                />
              </template>
            </el-table-column>
            <el-table-column label="节点名称" min-width="160">
              <template #default="{ row }">
                <el-input v-model="row.nodeName" placeholder="如:部门审批" />
              </template>
            </el-table-column>
            <el-table-column label="审批角色" min-width="180">
              <template #default="{ row }">
                <el-select
                  v-model="row.approverRoleId"
                  placeholder="请选择角色"
                  clearable
                  filterable
                  style="width: 100%"
                >
                  <el-option
                    v-for="r in roleOptions"
                    :key="r.value"
                    :label="r.label"
                    :value="r.value"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="金额阈值" width="160">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.amountThreshold"
                  :min="0"
                  :precision="2"
                  controls-position="right"
                  style="width: 100%"
                  placeholder="可选"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ $index }">
                <el-button link type="danger" size="small" @click="handleRemoveNode($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </div>

    <!-- 流程新增/编辑弹窗 -->
    <el-dialog v-model="flowDialogVisible" :title="flowDialogTitle" width="520px">
      <el-form ref="flowFormRef" :model="flowForm" :rules="flowRules" label-width="100px">
        <el-form-item label="流程编码" prop="code">
          <el-input v-model="flowForm.code" :disabled="flowIsEdit" placeholder="如 CONTRACT_FLOW_01" />
        </el-form-item>
        <el-form-item label="流程名称" prop="name">
          <el-input v-model="flowForm.name" placeholder="如 合同审批流" />
        </el-form-item>
        <el-form-item label="业务类型" prop="businessType">
          <el-select v-model="flowForm.businessType" style="width: 100%">
            <el-option
              v-for="(label, key) in businessTypeMap"
              :key="key"
              :label="label"
              :value="key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="flowForm.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="flowForm.remark" type="textarea" :rows="2" placeholder="备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="flowDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitFlow">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.page { padding: 24px; }
.page-header { margin-bottom: 16px; h1 { font-size: 20px; font-weight: 600; color: #1F2937; } }
.card {
  background: #fff;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
}
.toolbar { margin-bottom: 16px; }
.flow-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.flow-list-pane { flex: 1; min-width: 0; }
.node-pane { flex: 1; min-width: 0; }
.empty-tip {
  padding: 60px 0;
  text-align: center;
  color: #9CA3AF;
  font-size: 14px;
}
.node-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.node-title {
  display: flex;
  align-items: center;
  .title-text { font-size: 16px; font-weight: 600; color: #1F2937; }
}
</style>
