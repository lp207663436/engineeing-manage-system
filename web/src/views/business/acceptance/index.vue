<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  acceptanceApi,
  projectApi,
  maintenancePointApi,
  quoteApi,
  type AcceptanceDTO,
} from '@/api/business'
import { userApi } from '@/api/system'

interface Option { label: string; value: string }

interface Acceptance extends AcceptanceDTO {
  createTime?: string
}

const projectOptions = ref<Option[]>([])
const pointOptions = ref<Option[]>([])
const quoteOptions = ref<Option[]>([])
const userOptions = ref<Option[]>([])

const loading = ref(false)
const tableData = ref<Acceptance[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, code: '', result: '' })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const form = reactive<AcceptanceDTO>({
  code: '', projectId: undefined, businessType: 'NEW_BUILD', businessId: undefined,
  quoteId: undefined, acceptorId: undefined, acceptDate: '', actualQuantity: '',
  result: 'PENDING', rectifyCount: 0, remark: '',
})
const isEdit = ref(false)

// 提交结论弹窗
const resultVisible = ref(false)
const resultFormRef = ref<FormInstance>()
const resultForm = reactive<{ id: string; code: string; result: string; remark: string; rectifyCount: number }>({
  id: '', code: '', result: 'PASS', remark: '', rectifyCount: 0,
})

const businessTypeMap: Record<string, string> = { NEW_BUILD: '新建工程', MAINTENANCE_POINT: '维护点位' }
const resultMap: Record<string, string> = {
  PENDING: '待验收', PASS: '通过', FAIL: '不通过', RECTIFYING: '整改中', ARBITRATION: '仲裁中',
}
const resultTagType: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
  PENDING: 'info', PASS: 'success', FAIL: 'danger', RECTIFYING: 'warning', ARBITRATION: 'danger',
}

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
    const [proj, points, quotes, users] = await Promise.all([
      projectApi.page({ pageNum: 1, pageSize: 200 }),
      maintenancePointApi.page({ pageNum: 1, pageSize: 200 }),
      quoteApi.page({ pageNum: 1, pageSize: 200 }),
      userApi.page({ pageNum: 1, pageSize: 200 }),
    ]) as any[]
    projectOptions.value = (proj.list || []).map((p: any) => ({ label: `${p.code} ${p.name}`, value: p.id }))
    pointOptions.value = (points.list || []).map((p: any) => ({ label: `${p.code} ${p.name}`, value: p.id }))
    quoteOptions.value = (quotes.list || []).map((q: any) => ({ label: q.customerName ? `${q.code} (${q.customerName})` : q.code, value: q.id }))
    userOptions.value = (users.list || []).map((u: any) => ({ label: u.name, value: u.id }))
  } catch {}
}

const MAX_RECTIFY = 3

async function loadData() {
  loading.value = true
  try {
    const res: any = await acceptanceApi.page(query)
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
  query.result = ''
  handleSearch()
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增验收'
  Object.assign(form, {
    id: undefined, code: '', projectId: undefined, businessType: 'NEW_BUILD', businessId: undefined,
    quoteId: undefined, acceptorId: undefined, acceptDate: '', actualQuantity: '',
    result: 'PENDING', rectifyCount: 0, remark: '',
  })
  dialogVisible.value = true
}

function handleEdit(row: Acceptance) {
  isEdit.value = true
  dialogTitle.value = '编辑验收'
  Object.assign(form, {
    id: row.id, code: row.code, projectId: row.projectId, businessType: row.businessType,
    businessId: row.businessId, quoteId: row.quoteId, acceptorId: row.acceptorId,
    acceptDate: row.acceptDate, actualQuantity: row.actualQuantity, result: row.result,
    rectifyCount: row.rectifyCount, remark: row.remark,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        await acceptanceApi.update(form)
        ElMessage.success('更新成功')
      } else {
        await acceptanceApi.create(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch {}
  })
}

async function handleDelete(row: Acceptance) {
  try {
    await ElMessageBox.confirm(`确定删除验收「${row.code}」吗?`, '提示', { type: 'warning' })
    await acceptanceApi.delete(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

function handleOpenResult(row: Acceptance) {
  Object.assign(resultForm, {
    id: row.id || '', code: row.code, result: 'PASS', remark: '',
    rectifyCount: row.rectifyCount || 0,
  })
  resultVisible.value = true
}

async function handleResultSubmit() {
  if (!resultFormRef.value) return
  await resultFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await acceptanceApi.submitResult(resultForm.id, {
        result: resultForm.result,
        remark: resultForm.remark,
      })
      ElMessage.success('结论提交成功')
      resultVisible.value = false
      loadData()
    } catch {}
  })
}

function formatRectify(count: number) {
  const c = count || 0
  return `${c}/${MAX_RECTIFY}`
}

// 根据验收人 ID 查找名称
function acceptorName(id?: string) {
  if (!id) return '-'
  return userOptions.value.find((u) => u.value === id)?.label || id
}

const rules = {
  code: [{ required: true, message: '请输入验收编号', trigger: 'blur' }],
  projectId: [{ required: true, message: '请选择所属项目', trigger: 'change' }],
  acceptorId: [{ required: true, message: '请选择验收人', trigger: 'change' }],
  acceptDate: [{ required: true, message: '请选择验收日期', trigger: 'change' }],
  actualQuantity: [{ required: true, message: '请输入实际工程量', trigger: 'blur' }],
}

const resultRules = {
  result: [{ required: true, message: '请选择验收结论', trigger: 'change' }],
}

onMounted(() => {
  loadOptions()
  loadData()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>验收管理</h1>
    </div>

    <div class="card search-bar">
      <el-form inline>
        <el-form-item label="编号">
          <el-input v-model="query.code" placeholder="验收编号" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="结论">
          <el-select v-model="query.result" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="(label, key) in resultMap" :key="key" :label="label" :value="key" />
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
        <el-button type="primary" @click="handleAdd">+ 新增验收</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" row-key="id" style="width: 100%">
        <el-table-column prop="code" label="验收编号" min-width="130" />
        <el-table-column label="业务类型" width="120">
          <template #default="{ row }">{{ businessTypeMap[row.businessType] || row.businessType || '-' }}</template>
        </el-table-column>
        <el-table-column label="结论" width="100">
          <template #default="{ row }">
            <el-tag :type="resultTagType[row.result] || 'info'" size="small" effect="light">
              {{ resultMap[row.result] || row.result }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="整改次数" width="110" align="center">
          <template #default="{ row }">
            <span :style="{ color: (row.rectifyCount || 0) > MAX_RECTIFY ? '#F56C6C' : '#1F2937', fontWeight: (row.rectifyCount || 0) > MAX_RECTIFY ? 600 : 400 }">
              {{ formatRectify(row.rectifyCount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="acceptDate" label="验收日期" min-width="120" />
        <el-table-column label="验收人" min-width="120">
          <template #default="{ row }">{{ acceptorName(row.acceptorId) }}</template>
        </el-table-column>
        <el-table-column prop="actualQuantity" label="实际工程量" min-width="120" />
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row as Acceptance)">编辑</el-button>
            <el-button link type="success" size="small" @click="handleOpenResult(row as Acceptance)">提交结论</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row as Acceptance)">删除</el-button>
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
        <el-form-item label="验收编号" prop="code">
          <el-input v-model="form.code" :disabled="isEdit" placeholder="如 AC2026-001" />
        </el-form-item>
        <el-form-item label="项目" prop="projectId">
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
        <el-form-item label="报价">
          <el-select v-model="form.quoteId" placeholder="请选择报价" clearable filterable style="width: 100%">
            <el-option v-for="opt in quoteOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="验收人" prop="acceptorId">
          <el-select v-model="form.acceptorId" placeholder="请选择验收人" clearable filterable style="width: 100%">
            <el-option v-for="opt in userOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="验收日期" prop="acceptDate">
          <el-date-picker v-model="form.acceptDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="实际工程量" prop="actualQuantity">
          <el-input v-model="form.actualQuantity" placeholder="实际工程量" />
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

    <el-dialog v-model="resultVisible" title="提交验收结论" width="480px">
      <el-form ref="resultFormRef" :model="resultForm" :rules="resultRules" label-width="90px">
        <el-form-item label="验收编号">
          <el-input v-model="resultForm.code" disabled />
        </el-form-item>
        <el-form-item label="结论" prop="result">
          <el-select v-model="resultForm.result" style="width: 100%">
            <el-option label="通过" value="PASS" />
            <el-option label="不通过(需整改)" value="FAIL" />
            <el-option label="整改中" value="RECTIFYING" />
            <el-option label="转仲裁" value="ARBITRATION" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="resultForm.result === 'FAIL'" label="整改次数">
          <span>{{ resultForm.rectifyCount }} / {{ MAX_RECTIFY }}</span>
          <el-alert
            v-if="resultForm.rectifyCount >= MAX_RECTIFY"
            title="整改次数已达上限，将自动转为仲裁"
            type="warning"
            :closable="false"
            show-icon
            style="margin-left: 12px; flex: 1"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="resultForm.remark" type="textarea" :rows="3" placeholder="结论备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resultVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResultSubmit">提交</el-button>
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
