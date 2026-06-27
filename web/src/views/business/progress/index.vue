<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { progressApi, projectApi, maintenancePointApi, type ProgressDTO } from '@/api/business'
import { userApi } from '@/api/system'

interface Option { label: string; value: string }

interface Progress extends ProgressDTO {
  createTime?: string
}

const projectOptions = ref<Option[]>([])
const pointOptions = ref<Option[]>([])
const userOptions = ref<Option[]>([])

const loading = ref(false)
const tableData = ref<Progress[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, code: '', nodeName: '', status: '' })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const form = reactive<ProgressDTO>({
  code: '', projectId: undefined, businessType: 'NEW_BUILD', businessId: undefined,
  nodeName: '', planStartDate: '', planEndDate: '', actualStartDate: '', actualEndDate: '',
  progressPercent: 0, managerId: undefined, status: 'PENDING', remark: '',
})
const isEdit = ref(false)

const businessTypeMap: Record<string, string> = { NEW_BUILD: '新建工程', MAINTENANCE_POINT: '维护点位' }
const statusMap: Record<string, string> = {
  PENDING: '待开始', IN_PROGRESS: '进行中', COMPLETED: '已完成', OVERDUE: '延期',
}
const statusTagType: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
  PENDING: 'info', IN_PROGRESS: 'warning', COMPLETED: 'success', OVERDUE: 'danger',
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
    const [proj, points, users] = await Promise.all([
      projectApi.page({ pageNum: 1, pageSize: 200 }),
      maintenancePointApi.page({ pageNum: 1, pageSize: 200 }),
      userApi.page({ pageNum: 1, pageSize: 200 }),
    ]) as any[]
    projectOptions.value = (proj.list || []).map((p: any) => ({ label: `${p.code} ${p.name}`, value: p.id }))
    pointOptions.value = (points.list || []).map((p: any) => ({ label: `${p.code} ${p.name}`, value: p.id }))
    userOptions.value = (users.list || []).map((u: any) => ({ label: u.name, value: u.id }))
  } catch {}
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await progressApi.page(query)
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
  query.nodeName = ''
  query.status = ''
  handleSearch()
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增进度'
  Object.assign(form, {
    id: undefined, code: '', projectId: undefined, businessType: 'NEW_BUILD', businessId: undefined,
    nodeName: '', planStartDate: '', planEndDate: '', actualStartDate: '', actualEndDate: '',
    progressPercent: 0, managerId: undefined, status: 'PENDING', remark: '',
  })
  dialogVisible.value = true
}

function handleEdit(row: Progress) {
  isEdit.value = true
  dialogTitle.value = '编辑进度'
  Object.assign(form, {
    id: row.id, code: row.code, projectId: row.projectId, businessType: row.businessType,
    businessId: row.businessId, nodeName: row.nodeName, planStartDate: row.planStartDate,
    planEndDate: row.planEndDate, actualStartDate: row.actualStartDate, actualEndDate: row.actualEndDate,
    progressPercent: row.progressPercent, managerId: row.managerId, status: row.status, remark: row.remark,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        await progressApi.update(form)
        ElMessage.success('更新成功')
      } else {
        await progressApi.create(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch {}
  })
}

async function handleDelete(row: Progress) {
  try {
    await ElMessageBox.confirm(`确定删除进度「${row.code}」吗?`, '提示', { type: 'warning' })
    await progressApi.delete(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

const rules = {
  code: [{ required: true, message: '请输入进度编号', trigger: 'blur' }],
  nodeName: [{ required: true, message: '请输入节点名称', trigger: 'blur' }],
  projectId: [{ required: true, message: '请选择所属项目', trigger: 'change' }],
  planStartDate: [{ required: true, message: '请选择计划开始日期', trigger: 'change' }],
  planEndDate: [{ required: true, message: '请选择计划结束日期', trigger: 'change' }],
}

onMounted(() => {
  loadOptions()
  loadData()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>进度管理</h1>
    </div>

    <div class="card search-bar">
      <el-form inline>
        <el-form-item label="编号">
          <el-input v-model="query.code" placeholder="进度编号" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="节点名称">
          <el-input v-model="query.nodeName" placeholder="节点名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
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
        <el-button type="primary" @click="handleAdd">+ 新增进度</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" row-key="id" style="width: 100%">
        <el-table-column prop="code" label="进度编号" min-width="130" />
        <el-table-column prop="nodeName" label="节点名称" min-width="150" />
        <el-table-column label="业务类型" width="120">
          <template #default="{ row }">{{ businessTypeMap[row.businessType] || row.businessType || '-' }}</template>
        </el-table-column>
        <el-table-column label="进度百分比" width="180">
          <template #default="{ row }">
            <el-progress :percentage="Number(row.progressPercent || 0)" :stroke-width="10" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.status] || 'info'" size="small" effect="light">
              {{ statusMap[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="planStartDate" label="计划开始" min-width="120" />
        <el-table-column prop="planEndDate" label="计划结束" min-width="120" />
        <el-table-column prop="actualStartDate" label="实际开始" min-width="120" />
        <el-table-column prop="actualEndDate" label="实际结束" min-width="120" />
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row as Progress)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row as Progress)">删除</el-button>
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
        <el-form-item label="进度编号" prop="code">
          <el-input v-model="form.code" :disabled="isEdit" placeholder="如 PG2026-001" />
        </el-form-item>
        <el-form-item label="节点名称" prop="nodeName">
          <el-input v-model="form.nodeName" placeholder="请输入节点名称" />
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
        <el-form-item label="负责人">
          <el-select v-model="form.managerId" placeholder="请选择负责人" clearable filterable style="width: 100%">
            <el-option v-for="opt in userOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="进度百分比">
          <el-input-number v-model="form.progressPercent" :min="0" :max="100" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="(label, key) in statusMap" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划开始" prop="planStartDate">
          <el-date-picker v-model="form.planStartDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="计划结束" prop="planEndDate">
          <el-date-picker v-model="form.planEndDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="实际开始">
          <el-date-picker v-model="form.actualStartDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="实际结束">
          <el-date-picker v-model="form.actualEndDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
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
