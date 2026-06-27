<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { maintenanceContractApi, projectApi, type MaintenanceContractDTO } from '@/api/business'

interface Option { label: string; value: string }

interface MaintenanceContract extends MaintenanceContractDTO {
  createTime?: string
}

const projectOptions = ref<Option[]>([])

const loading = ref(false)
const tableData = ref<MaintenanceContract[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, code: '', name: '', status: '', projectId: '' })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const form = reactive<MaintenanceContractDTO>({
  code: '', name: '', projectId: undefined, partyA: '', partyB: '', signDate: '',
  effectiveDate: '', totalAmount: 0, periodMonths: 6, periodCount: undefined,
  responseSla: '', scope: '', status: 'ACTIVE', endDate: '', remark: '',
})
const isEdit = ref(false)

const statusMap: Record<string, string> = {
  ACTIVE: '生效', TERMINATED: '终止', EXPIRED: '到期',
}
const statusTagType: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
  ACTIVE: 'success', TERMINATED: 'danger', EXPIRED: 'info',
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await maintenanceContractApi.page(query)
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
  query.name = ''
  query.status = ''
  query.projectId = ''
  handleSearch()
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增维保合同'
  Object.assign(form, {
    id: undefined, code: '', name: '', projectId: undefined, partyA: '', partyB: '', signDate: '',
    effectiveDate: '', totalAmount: 0, periodMonths: 6, periodCount: undefined,
    responseSla: '', scope: '', status: 'ACTIVE', endDate: '', remark: '',
  })
  dialogVisible.value = true
}

function handleEdit(row: MaintenanceContract) {
  isEdit.value = true
  dialogTitle.value = '编辑维保合同'
  Object.assign(form, {
    id: row.id, code: row.code, name: row.name, projectId: row.projectId, partyA: row.partyA,
    partyB: row.partyB, signDate: row.signDate, effectiveDate: row.effectiveDate,
    totalAmount: row.totalAmount, periodMonths: row.periodMonths, periodCount: row.periodCount,
    responseSla: row.responseSla, scope: row.scope, status: row.status, endDate: row.endDate,
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
        await maintenanceContractApi.update(form)
        ElMessage.success('更新成功')
      } else {
        await maintenanceContractApi.create(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch {}
  })
}

async function handleDelete(row: MaintenanceContract) {
  try {
    await ElMessageBox.confirm(`确定删除维保合同「${row.name}」吗?`, '提示', { type: 'warning' })
    await maintenanceContractApi.delete(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

function formatAmount(v: number) {
  return v != null ? `¥${Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}` : '-'
}

function formatMonths(v: number) {
  return v != null ? `${v}个月` : '-'
}

function formatPeriodCount(v: number) {
  return v != null ? `${v}期` : '-'
}

const validatePeriodMonths = (rule: any, value: number, callback: any) => {
  if (value == null) {
    callback(new Error('请输入维保周期(月)'))
  } else if (value < 6) {
    callback(new Error('维保周期必须 ≥ 6'))
  } else if (value % 3 !== 0) {
    callback(new Error('维保周期必须为 3 的倍数'))
  } else {
    callback()
  }
}

const validateTotalAmount = (_rule: any, value: number, callback: any) => {
  if (value == null || value <= 0) {
    callback(new Error('合同总金额必须大于0'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  code: [{ required: true, message: '请输入合同编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入合同名称', trigger: 'blur' }],
  effectiveDate: [{ required: true, message: '请选择生效日期', trigger: 'change' }],
  totalAmount: [{ required: true, validator: validateTotalAmount, trigger: 'blur' }],
  periodMonths: [{ required: true, validator: validatePeriodMonths, trigger: 'blur' }],
  projectId: [{ required: true, message: '请选择所属项目', trigger: 'change' }],
  partyA: [{ required: true, message: '请输入甲方名称', trigger: 'blur' }],
  partyB: [{ required: true, message: '请输入乙方名称', trigger: 'blur' }],
  signDate: [{ required: true, message: '请选择签订日期', trigger: 'change' }],
  responseSla: [{ required: true, message: '请输入响应SLA', trigger: 'blur' }],
}

async function loadOptions() {
  try {
    const res: any = await projectApi.page({ pageNum: 1, pageSize: 200 })
    projectOptions.value = (res.list || []).map((p: any) => ({ label: `${p.code} ${p.name}`, value: p.id }))
  } catch {}
}

onMounted(() => {
  loadOptions()
  loadData()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>维保主合同</h1>
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
        <el-form-item label="项目">
          <el-select v-model="query.projectId" placeholder="全部" clearable filterable style="width: 200px">
            <el-option v-for="opt in projectOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
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
        <el-table-column prop="code" label="合同编号" min-width="130" />
        <el-table-column prop="name" label="合同名称" min-width="160" />
        <el-table-column prop="partyA" label="甲方" min-width="140" />
        <el-table-column prop="partyB" label="乙方" min-width="140" />
        <el-table-column label="总金额" min-width="140" align="right">
          <template #default="{ row }">{{ formatAmount(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="维保周期" width="100" align="center">
          <template #default="{ row }">{{ formatMonths(row.periodMonths) }}</template>
        </el-table-column>
        <el-table-column label="期数" width="90" align="center">
          <template #default="{ row }">{{ formatPeriodCount(row.periodCount) }}</template>
        </el-table-column>
        <el-table-column prop="effectiveDate" label="生效日期" min-width="120" />
        <el-table-column prop="endDate" label="到期日期" min-width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.status] || 'info'" size="small" effect="light">
              {{ statusMap[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row as MaintenanceContract)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row as MaintenanceContract)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="合同编号" prop="code">
          <el-input v-model="form.code" :disabled="isEdit" placeholder="如 MC2026-001" />
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
          <el-input v-model="form.partyA" placeholder="甲方名称" />
        </el-form-item>
        <el-form-item label="乙方" prop="partyB">
          <el-input v-model="form.partyB" placeholder="乙方名称" />
        </el-form-item>
        <el-form-item label="签订日期" prop="signDate">
          <el-date-picker v-model="form.signDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="生效日期" prop="effectiveDate">
          <el-date-picker v-model="form.effectiveDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="合同总金额" prop="totalAmount">
          <el-input-number v-model="form.totalAmount" :min="0.01" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="维保周期" prop="periodMonths">
          <el-input-number v-model="form.periodMonths" :min="6" :step="3" controls-position="right" style="width: 100%" />
          <div style="font-size: 12px; color: #909399; line-height: 1.4; margin-top: 4px;">必须为 3 的倍数且 ≥ 6</div>
        </el-form-item>
        <el-form-item label="响应SLA" prop="responseSla">
          <el-input v-model="form.responseSla" placeholder="如 2小时响应" />
        </el-form-item>
        <el-form-item label="服务范围">
          <el-input v-model="form.scope" type="textarea" :rows="3" placeholder="服务范围" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="(label, key) in statusMap" :key="key" :label="label" :value="key" />
          </el-select>
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
