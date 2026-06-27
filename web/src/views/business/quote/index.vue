<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { quoteApi, projectApi, maintenancePointApi, type QuoteDTO } from '@/api/business'

interface Option { label: string; value: string }

interface Quote extends QuoteDTO {
  createTime?: string
}

const projectOptions = ref<Option[]>([])
const pointOptions = ref<Option[]>([])

const loading = ref(false)
const tableData = ref<Quote[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, code: '', customerName: '', status: '' })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const form = reactive<QuoteDTO>({
  code: '', projectId: undefined, businessType: 'NEW_BUILD', businessId: undefined,
  amount: 0, quoteDate: '', validUntil: '', quotePerson: '', customerName: '',
  version: 1, status: 'DRAFT', summary: '',
})
const isEdit = ref(false)

const statusMap: Record<string, string> = {
  DRAFT: '草稿', SUBMITTED: '已提交', APPROVED: '已审批', CONFIRMED: '已确认', VOID: '已作废',
}
const statusTagType: Record<string, string> = {
  DRAFT: 'info', SUBMITTED: 'warning', APPROVED: 'success', CONFIRMED: 'success', VOID: 'danger',
}
const businessTypeMap: Record<string, string> = { NEW_BUILD: '新建工程', MAINTENANCE_POINT: '维护点位' }

const businessIdOptions = computed(() => {
  if (form.businessType === 'NEW_BUILD') return projectOptions.value
  if (form.businessType === 'MAINTENANCE_POINT') return pointOptions.value
  return []
})

watch(() => form.businessType, () => {
  form.businessId = undefined
})

async function loadOptions() {
  try {
    const [proj, points] = await Promise.all([
      projectApi.page({ pageNum: 1, pageSize: 200 }),
      maintenancePointApi.page({ pageNum: 1, pageSize: 200 }),
    ]) as any[]
    projectOptions.value = (proj.list || []).map((p: any) => ({ label: `${p.code} ${p.name}`, value: p.id }))
    pointOptions.value = (points.list || []).map((p: any) => ({ label: `${p.code} ${p.name}`, value: p.id }))
  } catch {}
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await quoteApi.page(query)
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
  query.customerName = ''
  query.status = ''
  handleSearch()
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增报价'
  Object.assign(form, {
    id: undefined, code: '', projectId: undefined, businessType: 'NEW_BUILD', businessId: undefined,
    amount: 0, quoteDate: '', validUntil: '', quotePerson: '', customerName: '',
    version: 1, status: 'DRAFT', summary: '',
  })
  dialogVisible.value = true
}

function handleEdit(row: Quote) {
  isEdit.value = true
  dialogTitle.value = '编辑报价'
  Object.assign(form, {
    id: row.id, code: row.code, projectId: row.projectId, businessType: row.businessType,
    businessId: row.businessId, amount: row.amount, quoteDate: row.quoteDate,
    validUntil: row.validUntil, quotePerson: row.quotePerson, customerName: row.customerName,
    version: row.version, status: row.status, summary: row.summary,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        await quoteApi.update(form)
        ElMessage.success('更新成功')
      } else {
        await quoteApi.create(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch {}
  })
}

async function handleDelete(row: Quote) {
  try {
    await ElMessageBox.confirm(`确定删除报价「${row.code}」吗?`, '提示', { type: 'warning' })
    await quoteApi.delete(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

function formatAmount(v: number) {
  return v != null ? `¥${Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}` : '-'
}

const rules = {
  code: [{ required: true, message: '请输入报价编号', trigger: 'blur' }],
}

onMounted(() => {
  loadOptions()
  loadData()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>报价管理</h1>
    </div>

    <div class="card search-bar">
      <el-form inline>
        <el-form-item label="编号">
          <el-input v-model="query.code" placeholder="报价编号" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="客户">
          <el-input v-model="query.customerName" placeholder="客户名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
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
        <el-button type="primary" @click="handleAdd">+ 新增报价</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" row-key="id" style="width: 100%">
        <el-table-column prop="code" label="报价编号" min-width="130" />
        <el-table-column prop="customerName" label="客户名称" min-width="150" />
        <el-table-column label="报价金额" min-width="130" align="right">
          <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="quotePerson" label="报价人" min-width="110" />
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.status] || 'info'" size="small" effect="light">
              {{ statusMap[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quoteDate" label="报价日期" min-width="120" />
        <el-table-column prop="validUntil" label="有效期至" min-width="120" />
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="报价编号" prop="code">
          <el-input v-model="form.code" :disabled="isEdit" placeholder="如 Q2026-001" />
        </el-form-item>
        <el-form-item label="项目">
          <el-select v-model="form.projectId" placeholder="请选择项目" clearable filterable style="width: 100%">
            <el-option v-for="opt in projectOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务类型">
          <el-select v-model="form.businessType" placeholder="请选择" style="width: 100%">
            <el-option v-for="(label, key) in businessTypeMap" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务对象">
          <el-select v-model="form.businessId" placeholder="请选择" clearable filterable style="width: 100%" :disabled="!form.businessType">
            <el-option v-for="opt in businessIdOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="客户名称">
          <el-input v-model="form.customerName" placeholder="客户名称" />
        </el-form-item>
        <el-form-item label="报价金额">
          <el-input-number v-model="form.amount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="报价人">
          <el-input v-model="form.quotePerson" placeholder="报价人" />
        </el-form-item>
        <el-form-item label="报价日期">
          <el-date-picker v-model="form.quoteDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="有效期至">
          <el-date-picker v-model="form.validUntil" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="版本号">
          <el-input-number v-model="form.version" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="(label, key) in statusMap" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="报价摘要">
          <el-input v-model="form.summary" type="textarea" :rows="3" placeholder="报价摘要" />
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
