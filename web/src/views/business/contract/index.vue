<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  contractApi,
  projectApi,
  approvalApi,
  customerApi,
  supplierApi,
  type ContractDTO,
  type ApprovalLogDTO,
} from '@/api/business'
import { userApi } from '@/api/system'

interface Option { label: string; value: string }

interface Contract extends ContractDTO {
  createTime?: string
}

const projectOptions = ref<Option[]>([])
const customerOptions = ref<Option[]>([])
const supplierOptions = ref<Option[]>([])
const userOptions = ref<Option[]>([])

const loading = ref(false)
const tableData = ref<Contract[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, name: '', code: '', status: '' })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const form = reactive<ContractDTO>({
  code: '', name: '', partyA: '', partyB: '', signDate: '', amount: 0,
  category: 'GENERAL', paymentMethod: '', projectId: undefined, status: 'DRAFT',
  startDate: '', endDate: '', remark: '',
})
const isEdit = ref(false)

const categoryMap: Record<string, string> = {
  GENERAL: '总包', SUB: '分包', PURCHASE: '采购', MAINTENANCE: '维保',
}
const statusMap: Record<string, string> = {
  DRAFT: '草稿', APPROVING: '审批中', APPROVED: '已审批', ARCHIVED: '已归档',
}
const statusTagType: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
  DRAFT: 'info', APPROVING: 'warning', APPROVED: 'success', ARCHIVED: 'info',
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await contractApi.page(query)
    tableData.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function handleReset() {
  query.name = ''
  query.code = ''
  query.status = ''
  handleSearch()
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增合同'
  Object.assign(form, {
    id: undefined, code: '', name: '', partyA: '', partyB: '', signDate: '', amount: 0,
    category: 'GENERAL', paymentMethod: '', projectId: undefined, status: 'DRAFT',
    startDate: '', endDate: '', remark: '',
  })
  dialogVisible.value = true
}

function handleEdit(row: Contract) {
  isEdit.value = true
  dialogTitle.value = '编辑合同'
  Object.assign(form, {
    id: row.id, code: row.code, name: row.name, partyA: row.partyA, partyB: row.partyB,
    signDate: row.signDate, amount: row.amount, category: row.category,
    paymentMethod: row.paymentMethod, projectId: row.projectId, status: row.status,
    startDate: row.startDate, endDate: row.endDate, remark: row.remark,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        await contractApi.update(form)
        ElMessage.success('更新成功')
      } else {
        await contractApi.create(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch {}
  })
}

async function handleDelete(row: Contract) {
  try {
    await ElMessageBox.confirm(`确定删除合同「${row.name}」吗?`, '提示', { type: 'warning' })
    await contractApi.delete(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

function formatAmount(v: number) {
  return v != null ? `¥${Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}` : '-'
}

const rules = {
  code: [{ required: true, message: '请输入合同编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入合同名称', trigger: 'blur' }],
  partyA: [{ required: true, message: '请选择甲方', trigger: 'change' }],
  partyB: [{ required: true, message: '请选择乙方', trigger: 'change' }],
  amount: [
    { required: true, message: '请输入合同金额', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '合同金额必须大于0', trigger: 'blur' },
  ],
  signDate: [{ required: true, message: '请选择签订日期', trigger: 'change' }],
  projectId: [{ required: true, message: '请选择关联项目', trigger: 'change' }],
}

// ===== 审批相关 =====
type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'
const progressDialogVisible = ref(false)
const progressLoading = ref(false)
const progressList = ref<ApprovalLogDTO[]>([])
const progressContractName = ref('')

const resultMap: Record<string, string> = {
  APPROVED: '同意',
  REJECTED: '拒绝',
}
const resultTagType: Record<string, TagType> = {
  APPROVED: 'success',
  REJECTED: 'danger',
  PENDING: 'warning',
}

async function handleStartApproval(row: Contract) {
  try {
    await ElMessageBox.confirm(`确定对合同「${row.name}」发起审批吗?`, '提示', { type: 'warning' })
    await approvalApi.start({ businessType: 'CONTRACT_APPROVAL', businessId: row.id! })
    ElMessage.success('审批已发起')
    loadData()
  } catch {}
}

async function handleViewProgress(row: Contract) {
  progressContractName.value = row.name || ''
  progressDialogVisible.value = true
  progressLoading.value = true
  try {
    const res: any = await approvalApi.progress('CONTRACT_APPROVAL', row.id!)
    progressList.value = res || []
  } finally {
    progressLoading.value = false
  }
}

async function loadOptions() {
  try {
    const [proj, customers, suppliers, users] = await Promise.all([
      projectApi.page({ pageNum: 1, pageSize: 200 }),
      customerApi.list(),
      supplierApi.list(),
      userApi.page({ pageNum: 1, pageSize: 999 }),
    ]) as any[]
    projectOptions.value = (proj.list || []).map((p: any) => ({ label: `${p.code} ${p.name}`, value: p.id }))
    customerOptions.value = (customers || []).map((c: any) => ({ label: c.name, value: c.id }))
    supplierOptions.value = (suppliers || []).map((s: any) => ({ label: s.name, value: s.id }))
    userOptions.value = (users.list || []).map((u: any) => ({ label: u.name || u.username, value: u.id }))
  } catch {}
}

// 根据客户 ID 查找名称
function customerName(id?: string) {
  if (!id) return '-'
  return customerOptions.value.find((c) => c.value === id)?.label || id
}

// 根据供应商 ID 查找名称
function supplierName(id?: string) {
  if (!id) return '-'
  return supplierOptions.value.find((s) => s.value === id)?.label || id
}

// 根据用户 ID 查找名称(审批人)
function userName(id?: string) {
  if (!id) return '-'
  return userOptions.value.find((u) => u.value === id)?.label || id
}

onMounted(() => {
  loadOptions()
  loadData()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>合同管理</h1>
    </div>

    <div class="card search-bar">
      <el-form inline>
        <el-form-item label="编号">
          <el-input v-model="query.code" placeholder="合同编号" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="query.name" placeholder="合同名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="(label, key) in statusMap" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">+ 新增合同</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" row-key="id" style="width: 100%">
        <el-table-column prop="code" label="合同编号" min-width="120" />
        <el-table-column prop="name" label="合同名称" min-width="180" />
        <el-table-column label="甲方" min-width="140">
          <template #default="{ row }">{{ customerName(row.partyA) }}</template>
        </el-table-column>
        <el-table-column label="乙方" min-width="140">
          <template #default="{ row }">{{ supplierName(row.partyB) }}</template>
        </el-table-column>
        <el-table-column label="金额" min-width="130" align="right">
          <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="类别" width="90">
          <template #default="{ row }">{{ categoryMap[row.category] || row.category }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.status] || 'info'" size="small" effect="light">
              {{ statusMap[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="signDate" label="签订日期" min-width="120" />
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row as Contract)">编辑</el-button>
            <el-button
              v-if="row.status !== 'APPROVED'"
              link
              type="warning"
              size="small"
              @click="handleStartApproval(row as Contract)"
            >发起审批</el-button>
            <el-button link type="info" size="small" @click="handleViewProgress(row as Contract)">审批进度</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row as Contract)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadData"
          @size-change="loadData"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="合同编号" prop="code">
          <el-input v-model="form.code" :disabled="isEdit" placeholder="如 C2026-001" />
        </el-form-item>
        <el-form-item label="合同名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入合同名称" />
        </el-form-item>
        <el-form-item label="项目" prop="projectId">
          <el-select v-model="form.projectId" placeholder="请选择项目" clearable filterable style="width: 100%">
            <el-option v-for="opt in projectOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="甲方" prop="partyA">
          <el-select v-model="form.partyA" placeholder="请选择甲方(客户)" clearable filterable style="width: 100%">
            <el-option v-for="opt in customerOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="乙方" prop="partyB">
          <el-select v-model="form.partyB" placeholder="请选择乙方(供应商)" clearable filterable style="width: 100%">
            <el-option v-for="opt in supplierOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="合同金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="合同类别">
          <el-select v-model="form.category" style="width: 100%">
            <el-option v-for="(label, key) in categoryMap" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="付款方式">
          <el-input v-model="form.paymentMethod" placeholder="如:按进度付款" />
        </el-form-item>
        <el-form-item label="签订日期" prop="signDate">
          <el-date-picker v-model="form.signDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="生效日期">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="到期日期">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="(label, key) in statusMap" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 审批进度弹窗 -->
    <el-dialog v-model="progressDialogVisible" title="审批进度" width="680px">
      <div class="progress-title">合同:{{ progressContractName }}</div>
      <el-table v-loading="progressLoading" :data="progressList" row-key="id" style="width: 100%">
        <el-table-column prop="nodeOrder" label="节点序号" width="100" />
        <el-table-column label="审批人" min-width="140">
          <template #default="{ row }">{{ userName(row.approverId) }}</template>
        </el-table-column>
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag
              v-if="row.result"
              :type="resultTagType[row.result] || 'info'"
              size="small"
              effect="light"
            >
              {{ resultMap[row.result] || row.result }}
            </el-tag>
            <el-tag v-else type="warning" size="small" effect="light">待审批</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="opinion" label="审批意见" min-width="200" show-overflow-tooltip />
        <el-table-column prop="approveTime" label="审批时间" min-width="160" />
      </el-table>
      <template #footer>
        <el-button @click="progressDialogVisible = false">关闭</el-button>
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
.search-bar { padding: 16px 20px 0; }
.toolbar { margin-bottom: 16px; }
.pagination { display: flex; justify-content: flex-end; margin-top: 16px; }
.progress-title { font-size: 14px; font-weight: 600; color: #1F2937; margin-bottom: 12px; }
</style>
