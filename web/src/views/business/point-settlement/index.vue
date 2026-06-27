<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { pointSettlementApi, type PointSettlementDTO } from '@/api/business'

interface PointSettlement extends PointSettlementDTO {
  createTime?: string
}

const loading = ref(false)
const tableData = ref<PointSettlement[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, code: '', pointId: '', projectId: '', status: '' })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const form = reactive<PointSettlementDTO>({
  code: '', projectId: undefined, pointId: undefined, quoteId: undefined,
  acceptanceId: undefined, amount: 0, status: 'PENDING', invoiceNo: '',
  receivedAmount: 0, receivedDate: '', remark: '',
})
const isEdit = ref(false)

const statusMap: Record<string, string> = {
  PENDING: '待结算', CONFIRMED: '已确认', INVOICED: '已开票',
  RECEIVED: '已回款', CLOSED: '已关闭',
}
const statusTagType: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
  PENDING: 'info', CONFIRMED: 'success', INVOICED: 'primary',
  RECEIVED: 'success', CLOSED: 'info',
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await pointSettlementApi.page(query)
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
  query.pointId = ''
  query.projectId = ''
  query.status = ''
  handleSearch()
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增结算单'
  Object.assign(form, {
    id: undefined, code: '', projectId: undefined, pointId: undefined, quoteId: undefined,
    acceptanceId: undefined, amount: 0, status: 'PENDING', invoiceNo: '',
    receivedAmount: 0, receivedDate: '', remark: '',
  })
  dialogVisible.value = true
}

function handleEdit(row: PointSettlement) {
  isEdit.value = true
  dialogTitle.value = '编辑结算单'
  Object.assign(form, {
    id: row.id, code: row.code, projectId: row.projectId, pointId: row.pointId,
    quoteId: row.quoteId, acceptanceId: row.acceptanceId, amount: row.amount,
    status: row.status, invoiceNo: row.invoiceNo, receivedAmount: row.receivedAmount,
    receivedDate: row.receivedDate, remark: row.remark,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        await pointSettlementApi.update(form)
        ElMessage.success('更新成功')
      } else {
        await pointSettlementApi.create(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch {}
  })
}

async function handleDelete(row: PointSettlement) {
  try {
    await ElMessageBox.confirm(`确定删除结算单「${row.code}」吗?`, '提示', { type: 'warning' })
    await pointSettlementApi.delete(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

function formatAmount(v: number) {
  return v != null ? `¥${Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}` : '-'
}

const rules = {
  code: [{ required: true, message: '请输入结算单编号', trigger: 'blur' }],
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>点位结算</h1>
    </div>

    <div class="card search-bar">
      <el-form inline>
        <el-form-item label="编号">
          <el-input v-model="query.code" placeholder="结算单编号" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="点位ID">
          <el-input v-model="query.pointId" placeholder="点位ID" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="项目ID">
          <el-input v-model="query.projectId" placeholder="项目ID" clearable style="width: 160px" @keyup.enter="handleSearch" />
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
        <el-button type="primary" @click="handleAdd">+ 新增结算单</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" row-key="id" style="width: 100%">
        <el-table-column prop="code" label="结算单编号" min-width="130" />
        <el-table-column prop="pointId" label="点位ID" min-width="120" />
        <el-table-column prop="quoteId" label="报价ID" min-width="120" />
        <el-table-column prop="acceptanceId" label="验收ID" min-width="120" />
        <el-table-column label="结算金额" min-width="140" align="right">
          <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.status] || 'info'" size="small" effect="light">
              {{ statusMap[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="invoiceNo" label="发票号" min-width="140" />
        <el-table-column label="已回款金额" min-width="140" align="right">
          <template #default="{ row }">{{ formatAmount(row.receivedAmount) }}</template>
        </el-table-column>
        <el-table-column prop="receivedDate" label="回款日期" min-width="120" />
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row as PointSettlement)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row as PointSettlement)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="结算单编号" prop="code">
          <el-input v-model="form.code" :disabled="isEdit" placeholder="如 PS2026-001" />
        </el-form-item>
        <el-form-item label="项目ID">
          <el-input v-model="form.projectId" placeholder="项目ID" />
        </el-form-item>
        <el-form-item label="点位ID">
          <el-input v-model="form.pointId" placeholder="点位ID" />
        </el-form-item>
        <el-form-item label="报价ID">
          <el-input v-model="form.quoteId" placeholder="报价ID" />
        </el-form-item>
        <el-form-item label="验收ID">
          <el-input v-model="form.acceptanceId" placeholder="验收ID" />
        </el-form-item>
        <el-form-item label="结算金额">
          <el-input-number v-model="form.amount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="(label, key) in statusMap" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="发票号">
          <el-input v-model="form.invoiceNo" placeholder="发票号" />
        </el-form-item>
        <el-form-item label="已回款金额">
          <el-input-number v-model="form.receivedAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="回款日期">
          <el-date-picker v-model="form.receivedDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
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
</style>
