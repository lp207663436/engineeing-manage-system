<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  maintenanceTaskApi,
  projectApi,
  maintenancePointApi,
  type MaintenanceTaskDTO,
  type ProjectDTO,
  type MaintenancePointDTO,
} from '@/api/business'
import { userApi } from '@/api/system'

interface Option { label: string; value: string }

interface Row extends MaintenanceTaskDTO {
  createTime?: string
}

const loading = ref(false)
const tableData = ref<Row[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, code: '', status: '', projectId: '', pointId: '' })

const projectOptions = ref<ProjectDTO[]>([])
const pointOptions = ref<MaintenancePointDTO[]>([])
const userOptions = ref<Option[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const form = reactive<MaintenanceTaskDTO>({
  code: '', projectId: undefined, pointId: undefined, type: 'INSPECTION',
  reporterId: undefined, handlerId: undefined, planDate: '', finishDate: '',
  status: 'PENDING', priority: 'MEDIUM', description: '', result: '',
})
const isEdit = ref(false)

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'
const typeMap: Record<string, string> = { INSPECTION: '巡检', REPAIR: '故障维修' }
const typeTagType: Record<string, TagType> = { INSPECTION: 'primary', REPAIR: 'warning' }
const priorityMap: Record<string, string> = { LOW: '低', MEDIUM: '中', HIGH: '高', URGENT: '紧急' }
const priorityTagType: Record<string, TagType> = { LOW: 'info', MEDIUM: 'info', HIGH: 'warning', URGENT: 'danger' }
const statusMap: Record<string, string> = {
  PENDING: '待处理', ASSIGNED: '已派单', IN_PROGRESS: '处理中', COMPLETED: '已完成', CANCELLED: '已取消',
}
const statusTagType: Record<string, TagType> = {
  PENDING: 'info', ASSIGNED: 'primary', IN_PROGRESS: 'warning', COMPLETED: 'success', CANCELLED: 'info',
}

async function loadProjects() {
  try {
    const res: any = await projectApi.page({ pageNum: 1, pageSize: 200 })
    projectOptions.value = res.list || []
  } catch {}
}

async function loadPoints() {
  try {
    const res: any = await maintenancePointApi.page({ pageNum: 1, pageSize: 200 })
    pointOptions.value = res.list || []
  } catch {}
}

async function loadUsers() {
  try {
    const res: any = await userApi.page({ pageNum: 1, pageSize: 200 })
    userOptions.value = (res.list || []).map((u: any) => ({ label: u.name, value: u.id }))
  } catch {}
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await maintenanceTaskApi.page(query)
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
  query.status = ''
  query.projectId = ''
  query.pointId = ''
  handleSearch()
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增维保任务'
  Object.assign(form, {
    id: undefined, code: '', projectId: undefined, pointId: undefined, type: 'INSPECTION',
    reporterId: undefined, handlerId: undefined, planDate: '', finishDate: '',
    status: 'PENDING', priority: 'MEDIUM', description: '', result: '',
  })
  dialogVisible.value = true
}

function handleEdit(row: Row) {
  isEdit.value = true
  dialogTitle.value = '编辑维保任务'
  Object.assign(form, {
    id: row.id, code: row.code, projectId: row.projectId, pointId: row.pointId,
    type: row.type || 'INSPECTION', reporterId: row.reporterId, handlerId: row.handlerId,
    planDate: row.planDate, finishDate: row.finishDate, status: row.status || 'PENDING',
    priority: row.priority || 'MEDIUM', description: row.description, result: row.result,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        await maintenanceTaskApi.update(form)
        ElMessage.success('更新成功')
      } else {
        await maintenanceTaskApi.create(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch {}
  })
}

async function handleDelete(row: Row) {
  try {
    await ElMessageBox.confirm(`确定删除任务「${row.code}」吗?`, '提示', { type: 'warning' })
    await maintenanceTaskApi.delete(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

const rules = {
  code: [{ required: true, message: '请输入任务编号', trigger: 'blur' }],
}

onMounted(() => {
  loadProjects()
  loadPoints()
  loadUsers()
  loadData()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>维保任务</h1>
    </div>

    <div class="card search-bar">
      <el-form inline>
        <el-form-item label="编号">
          <el-input v-model="query.code" placeholder="任务编号" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="(label, key) in statusMap" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目">
          <el-select v-model="query.projectId" placeholder="全部" clearable filterable style="width: 200px">
            <el-option v-for="p in projectOptions" :key="p.id" :label="p.name" :value="p.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="点位">
          <el-select v-model="query.pointId" placeholder="全部" clearable filterable style="width: 180px">
            <el-option v-for="p in pointOptions" :key="p.id" :label="p.name" :value="p.id!" />
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
        <el-button type="primary" @click="handleAdd">+ 新增任务</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" row-key="id" style="width: 100%">
        <el-table-column prop="code" label="任务编号" min-width="130" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeTagType[row.type] || 'info'" size="small" effect="light">
              {{ typeMap[row.type] || row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="90">
          <template #default="{ row }">
            <el-tag :type="priorityTagType[row.priority] || 'info'" size="small" effect="light">
              {{ priorityMap[row.priority] || row.priority }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.status] || 'info'" size="small" effect="light">
              {{ statusMap[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reporterId" label="报修人" min-width="120" />
        <el-table-column prop="handlerId" label="处理人" min-width="120" />
        <el-table-column prop="planDate" label="计划日期" min-width="120" />
        <el-table-column prop="finishDate" label="完成日期" min-width="120" />
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row as Row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="任务编号" prop="code">
          <el-input v-model="form.code" :disabled="isEdit" placeholder="如 T2026-001" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属项目">
              <el-select v-model="form.projectId" placeholder="选择项目" filterable clearable style="width: 100%">
                <el-option v-for="p in projectOptions" :key="p.id" :label="p.name" :value="p.id!" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="维护点位">
              <el-select v-model="form.pointId" placeholder="选择点位" filterable clearable style="width: 100%">
                <el-option v-for="p in pointOptions" :key="p.id" :label="p.name" :value="p.id!" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="任务类型">
              <el-select v-model="form.type" style="width: 100%">
                <el-option v-for="(label, key) in typeMap" :key="key" :label="label" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级">
              <el-select v-model="form.priority" style="width: 100%">
                <el-option v-for="(label, key) in priorityMap" :key="key" :label="label" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="报修人">
              <el-select v-model="form.reporterId" placeholder="请选择报修人" clearable filterable style="width: 100%">
                <el-option v-for="opt in userOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="处理人">
              <el-select v-model="form.handlerId" placeholder="请选择处理人" clearable filterable style="width: 100%">
                <el-option v-for="opt in userOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="计划日期">
              <el-date-picker v-model="form.planDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="完成日期">
              <el-date-picker v-model="form.finishDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="任务状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="(label, key) in statusMap" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="任务描述" />
        </el-form-item>
        <el-form-item label="处理结果">
          <el-input v-model="form.result" type="textarea" :rows="3" placeholder="处理结果" />
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
