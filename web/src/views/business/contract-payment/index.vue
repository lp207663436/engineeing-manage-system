<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  contractPaymentApi,
  contractApi,
  type ContractPaymentDTO,
  type ContractDTO,
} from '@/api/business'

interface Row extends ContractPaymentDTO {
  createTime?: string
}

const loading = ref(false)
const tableData = ref<Row[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  code: '',
  contractId: '',
  type: '',
  status: '',
})

const contractOptions = ref<ContractDTO[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const form = reactive<ContractPaymentDTO>({
  code: '',
  contractId: undefined,
  type: 'RECEIVABLE',
  planDate: '',
  planAmount: 0,
  invoiceNo: '',
  remark: '',
})
const isEdit = ref(false)

const actualDialogVisible = ref(false)
const actualFormRef = ref<FormInstance>()
const actualForm = reactive({
  id: '',
  actualAmount: 0,
  actualDate: '',
  invoiceNo: '',
})

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'
const typeMap: Record<string, string> = { RECEIVABLE: '应收', PAYABLE: '应付' }
const typeTagType: Record<string, TagType> = { RECEIVABLE: 'success', PAYABLE: 'warning' }
const statusMap: Record<string, string> = {
  PENDING: '待收/付',
  RECEIVED: '已收/付',
  OVERDUE: '逾期',
}
const statusTagType: Record<string, TagType> = {
  PENDING: 'info',
  RECEIVED: 'success',
  OVERDUE: 'danger',
}

interface DashboardData {
  receivable: { planned: number; received: number; overdue: number }
  payable: { planned: number; paid: number; overdue: number }
}
const dashboard = ref<DashboardData>({
  receivable: { planned: 0, received: 0, overdue: 0 },
  payable: { planned: 0, paid: 0, overdue: 0 },
})

async function loadContracts() {
  try {
    const res: any = await contractApi.page({ pageNum: 1, pageSize: 200 })
    contractOptions.value = res.list || []
  } catch {}
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await contractPaymentApi.page(query)
    tableData.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
  loadDashboard()
}

async function loadDashboard() {
  try {
    const res: any = await contractPaymentApi.dashboard(query.contractId || '')
    if (res) {
      dashboard.value = {
        receivable: {
          planned: res.receivable?.planned || 0,
          received: res.receivable?.received || 0,
          overdue: res.receivable?.overdue || 0,
        },
        payable: {
          planned: res.payable?.planned || 0,
          paid: res.payable?.paid || 0,
          overdue: res.payable?.overdue || 0,
        },
      }
    }
  } catch {}
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function handleReset() {
  query.code = ''
  query.contractId = ''
  query.type = ''
  query.status = ''
  handleSearch()
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增收付款'
  Object.assign(form, {
    id: undefined,
    code: '',
    contractId: undefined,
    type: 'RECEIVABLE',
    planDate: '',
    planAmount: 0,
    invoiceNo: '',
    remark: '',
  })
  dialogVisible.value = true
}

function handleEdit(row: Row) {
  isEdit.value = true
  dialogTitle.value = '编辑收付款'
  Object.assign(form, {
    id: row.id,
    code: row.code,
    contractId: row.contractId,
    type: row.type || 'RECEIVABLE',
    planDate: row.planDate,
    planAmount: row.planAmount,
    invoiceNo: row.invoiceNo,
    remark: row.remark,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        await contractPaymentApi.update(form)
        ElMessage.success('更新成功')
      } else {
        await contractPaymentApi.create(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch {}
  })
}

async function handleDelete(row: Row) {
  try {
    await ElMessageBox.confirm(`确定删除收付款「${row.code}」吗?`, '提示', { type: 'warning' })
    await contractPaymentApi.delete(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

function handleRecordActual(row: Row) {
  actualForm.id = row.id!
  actualForm.actualAmount = row.actualAmount || row.planAmount || 0
  actualForm.actualDate = row.actualDate || ''
  actualForm.invoiceNo = row.invoiceNo || ''
  actualDialogVisible.value = true
}

async function handleSubmitActual() {
  if (!actualFormRef.value) return
  await actualFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await contractPaymentApi.recordActual(actualForm.id, {
        actualAmount: actualForm.actualAmount,
        actualDate: actualForm.actualDate,
        invoiceNo: actualForm.invoiceNo,
      })
      ElMessage.success('登记成功')
      actualDialogVisible.value = false
      loadData()
    } catch {}
  })
}

function contractName(row: Row) {
  const c = contractOptions.value.find((x) => x.id === row.contractId)
  return c ? `${c.code} ${c.name}` : row.contractId || '-'
}

function formatAmount(v?: number) {
  return v != null ? `¥${Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}` : '-'
}

const rules = {
  code: [{ required: true, message: '请输入单号', trigger: 'blur' }],
  contractId: [{ required: true, message: '请选择合同', trigger: 'change' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  planDate: [{ required: true, message: '请选择计划日期', trigger: 'change' }],
}

const actualRules = {
  actualAmount: [{ required: true, message: '请输入实际金额', trigger: 'blur' }],
  actualDate: [{ required: true, message: '请选择实际日期', trigger: 'change' }],
}

onMounted(async () => {
  await loadContracts()
  loadData()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>合同收付款</h1>
    </div>

    <!-- 看板卡片 -->
    <div class="dashboard-row">
      <el-card class="dashboard-card" shadow="never">
        <template #header>
          <span class="card-title">应收(计划回款)</span>
        </template>
        <div class="stat-row">
          <el-statistic title="计划应收" :value="dashboard.receivable.planned" :precision="2" prefix="¥" />
          <el-statistic title="已收" :value="dashboard.receivable.received" :precision="2" prefix="¥" />
          <el-statistic title="逾期" :value="dashboard.receivable.overdue" :precision="2" prefix="¥" />
        </div>
      </el-card>
      <el-card class="dashboard-card" shadow="never">
        <template #header>
          <span class="card-title">应付(计划付款)</span>
        </template>
        <div class="stat-row">
          <el-statistic title="计划应付" :value="dashboard.payable.planned" :precision="2" prefix="¥" />
          <el-statistic title="已付" :value="dashboard.payable.paid" :precision="2" prefix="¥" />
          <el-statistic title="逾期" :value="dashboard.payable.overdue" :precision="2" prefix="¥" />
        </div>
      </el-card>
    </div>

    <div class="card search-bar">
      <el-form inline>
        <el-form-item label="单号">
          <el-input
            v-model="query.code"
            placeholder="单号"
            clearable
            style="width: 160px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="合同">
          <el-select
            v-model="query.contractId"
            placeholder="全部"
            clearable
            filterable
            style="width: 240px"
          >
            <el-option
              v-for="c in contractOptions"
              :key="c.id"
              :label="`${c.code} ${c.name}`"
              :value="c.id!"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="(label, key) in typeMap" :key="key" :label="label" :value="key" />
          </el-select>
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
        <el-button type="primary" @click="handleAdd">+ 新增收付款</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" row-key="id" style="width: 100%">
        <el-table-column prop="code" label="单号" min-width="130" />
        <el-table-column label="合同" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ contractName(row as Row) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="typeTagType[row.type] || 'info'" size="small" effect="light">
              {{ typeMap[row.type] || row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="planDate" label="计划日期" min-width="120" />
        <el-table-column label="计划金额" min-width="130" align="right">
          <template #default="{ row }">{{ formatAmount(row.planAmount) }}</template>
        </el-table-column>
        <el-table-column label="实际金额" min-width="130" align="right">
          <template #default="{ row }">{{ formatAmount(row.actualAmount) }}</template>
        </el-table-column>
        <el-table-column prop="actualDate" label="实际日期" min-width="120" />
        <el-table-column prop="invoiceNo" label="发票号" min-width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.status] || 'info'" size="small" effect="light">
              {{ statusMap[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row as Row)">编辑</el-button>
            <el-button link type="success" size="small" @click="handleRecordActual(row as Row)">记实际</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row as Row)">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="单号" prop="code">
              <el-input v-model="form.code" :disabled="isEdit" placeholder="如 CP2026-001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型" prop="type">
              <el-select v-model="form.type" style="width: 100%">
                <el-option v-for="(label, key) in typeMap" :key="key" :label="label" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="合同" prop="contractId">
          <el-select v-model="form.contractId" placeholder="请选择合同" clearable filterable style="width: 100%">
            <el-option
              v-for="c in contractOptions"
              :key="c.id"
              :label="`${c.code} ${c.name}`"
              :value="c.id!"
            />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="计划日期" prop="planDate">
              <el-date-picker
                v-model="form.planDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划金额">
              <el-input-number
                v-model="form.planAmount"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="发票号">
          <el-input v-model="form.invoiceNo" placeholder="发票号" />
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

    <!-- 记实际收付款弹窗 -->
    <el-dialog v-model="actualDialogVisible" title="登记实际收付款" width="480px">
      <el-form ref="actualFormRef" :model="actualForm" :rules="actualRules" label-width="100px">
        <el-form-item label="实际金额" prop="actualAmount">
          <el-input-number
            v-model="actualForm.actualAmount"
            :min="0"
            :precision="2"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="实际日期" prop="actualDate">
          <el-date-picker
            v-model="actualForm.actualDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="发票号">
          <el-input v-model="actualForm.invoiceNo" placeholder="发票号(可选)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actualDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitActual">确定</el-button>
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
.dashboard-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}
.dashboard-card {
  flex: 1;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
}
.card-title { font-size: 14px; font-weight: 600; color: #1F2937; }
.stat-row {
  display: flex;
  justify-content: space-around;
  gap: 16px;
}
</style>
