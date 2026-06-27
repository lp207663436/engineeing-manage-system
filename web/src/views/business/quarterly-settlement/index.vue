<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { quarterlySettlementApi, contractApi, type QuarterlySettlementDTO, type ContractDTO } from '@/api/business'

interface Row extends QuarterlySettlementDTO {
  createTime?: string
}

const loading = ref(false)
const tableData = ref<Row[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, code: '', contractId: '', status: '' })

const contractOptions = ref<ContractDTO[]>([])

// 生成结算单
const genDialogVisible = ref(false)
const genFormRef = ref<FormInstance>()
const genForm = reactive({ contractId: '' })
const genRules = {
  contractId: [{ required: true, message: '请选择维保主合同', trigger: 'change' }],
}

// 状态流转
const statusDialogVisible = ref(false)
const statusFormRef = ref<FormInstance>()
const statusForm = reactive({ id: '', status: '', remark: '' })
const statusRules = {
  status: [{ required: true, message: '请选择目标状态', trigger: 'change' }],
}

// 调整
const adjustDialogVisible = ref(false)
const adjustFormRef = ref<FormInstance>()
const adjustForm = reactive({ id: '', amount: 0, remark: '' })
const adjustRules = {
  amount: [{ required: true, message: '请输入调整金额', trigger: 'blur' }],
}

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'
const statusMap: Record<string, string> = {
  DRAFT: '草稿', REVIEWED: '已审核', CONFIRMED: '已确认', INVOICED: '已开票', RECEIVED: '已回款', CLOSED: '已关闭',
}
const statusTagType: Record<string, TagType> = {
  DRAFT: 'info', REVIEWED: 'warning', CONFIRMED: 'success', INVOICED: 'primary', RECEIVED: 'success', CLOSED: 'info',
}
// 状态流转可选目标状态
const flowStatusMap: Record<string, string> = {
  REVIEWED: '已审核', CONFIRMED: '已确认', INVOICED: '已开票', RECEIVED: '已回款', CLOSED: '已关闭',
}

async function loadContracts() {
  try {
    const res: any = await contractApi.page({ pageNum: 1, pageSize: 200 })
    contractOptions.value = res.list || []
  } catch {}
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await quarterlySettlementApi.page(query)
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
  query.code = ''
  query.contractId = ''
  query.status = ''
  handleSearch()
}

function formatAmount(v?: number) {
  return v != null ? `¥${Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}` : '-'
}

function contractLabel(id?: string) {
  if (!id) return '-'
  const c = contractOptions.value.find((x) => x.id === id)
  return c ? `${c.code} · ${c.name}` : id
}

// 生成结算单
function openGenDialog() {
  genForm.contractId = ''
  genDialogVisible.value = true
}

async function handleGenSubmit() {
  if (!genFormRef.value) return
  await genFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await quarterlySettlementApi.generate(genForm.contractId)
      ElMessage.success('已生成(已存在期次自动跳过)')
      genDialogVisible.value = false
      loadData()
    } catch {}
  })
}

// 状态流转
function openStatusDialog(row: Row) {
  statusForm.id = row.id || ''
  statusForm.status = ''
  statusForm.remark = ''
  statusDialogVisible.value = true
}

async function handleStatusSubmit() {
  if (!statusFormRef.value) return
  await statusFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await quarterlySettlementApi.updateStatus(statusForm.id, {
        status: statusForm.status,
        remark: statusForm.remark,
      })
      ElMessage.success('状态已更新')
      statusDialogVisible.value = false
      loadData()
    } catch {}
  })
}

// 调整
function openAdjustDialog(row: Row) {
  adjustForm.id = row.id || ''
  adjustForm.amount = row.amount ?? 0
  adjustForm.remark = ''
  adjustDialogVisible.value = true
}

async function handleAdjustSubmit() {
  if (!adjustFormRef.value) return
  await adjustFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await quarterlySettlementApi.adjust(adjustForm.id, {
        amount: adjustForm.amount,
        remark: adjustForm.remark,
      })
      ElMessage.success('调整成功')
      adjustDialogVisible.value = false
      loadData()
    } catch {}
  })
}

async function handleDelete(row: Row) {
  try {
    await ElMessageBox.confirm(`确定删除结算单「${row.code}」吗?`, '提示', { type: 'warning' })
    await quarterlySettlementApi.delete(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

onMounted(() => {
  loadContracts()
  loadData()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>季度结算</h1>
    </div>

    <div class="card search-bar">
      <el-form inline>
        <el-form-item label="编号">
          <el-input v-model="query.code" placeholder="结算单编号" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="合同">
          <el-select v-model="query.contractId" placeholder="全部" clearable filterable style="width: 220px">
            <el-option v-for="c in contractOptions" :key="c.id" :label="`${c.code} · ${c.name}`" :value="c.id!" />
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
        <el-button type="primary" @click="openGenDialog">+ 生成结算单</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" row-key="id" style="width: 100%">
        <el-table-column prop="code" label="结算单编号" min-width="140" />
        <el-table-column label="维保合同" min-width="200">
          <template #default="{ row }">{{ contractLabel(row.contractId) }}</template>
        </el-table-column>
        <el-table-column label="期次" width="90">
          <template #default="{ row }">{{ row.periodNo != null ? `第${row.periodNo}期` : '-' }}</template>
        </el-table-column>
        <el-table-column prop="periodStartDate" label="期起始" min-width="120" />
        <el-table-column prop="periodEndDate" label="期结束" min-width="120" />
        <el-table-column label="金额" min-width="130" align="right">
          <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.status] || 'info'" size="small" effect="light">
              {{ statusMap[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="invoiceNo" label="发票号" min-width="120" />
        <el-table-column label="已回款金额" min-width="130" align="right">
          <template #default="{ row }">{{ formatAmount(row.receivedAmount) }}</template>
        </el-table-column>
        <el-table-column prop="receivedDate" label="回款日期" min-width="120" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openStatusDialog(row)">状态流转</el-button>
            <el-button link type="primary" size="small" @click="openAdjustDialog(row)">调整</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 生成结算单 -->
    <el-dialog v-model="genDialogVisible" title="生成结算单" width="480px">
      <el-form ref="genFormRef" :model="genForm" :rules="genRules" label-width="100px">
        <el-form-item label="维保合同" prop="contractId">
          <el-select v-model="genForm.contractId" placeholder="请选择维保主合同" filterable style="width: 100%">
            <el-option v-for="c in contractOptions" :key="c.id" :label="`${c.code} · ${c.name}`" :value="c.id!" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <div class="tip">系统将根据合同期次自动生成未生成的结算单,已存在期次自动跳过。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="genDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleGenSubmit">确定生成</el-button>
      </template>
    </el-dialog>

    <!-- 状态流转 -->
    <el-dialog v-model="statusDialogVisible" title="状态流转" width="480px">
      <el-form ref="statusFormRef" :model="statusForm" :rules="statusRules" label-width="100px">
        <el-form-item label="目标状态" prop="status">
          <el-select v-model="statusForm.status" placeholder="请选择目标状态" style="width: 100%">
            <el-option v-for="(label, key) in flowStatusMap" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="statusForm.remark" type="textarea" :rows="3" placeholder="流转备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleStatusSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 调整金额 -->
    <el-dialog v-model="adjustDialogVisible" title="调整金额" width="480px">
      <el-form ref="adjustFormRef" :model="adjustForm" :rules="adjustRules" label-width="100px">
        <el-form-item label="调整金额" prop="amount">
          <el-input-number v-model="adjustForm.amount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="adjustForm.remark" type="textarea" :rows="3" placeholder="调整原因说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAdjustSubmit">确定</el-button>
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
.tip { font-size: 12px; color: #9CA3AF; line-height: 1.6; }
</style>
