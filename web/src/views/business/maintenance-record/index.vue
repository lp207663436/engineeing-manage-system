<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  maintenanceRecordApi,
  projectApi,
  maintenancePointApi,
  type MaintenanceRecordDTO,
  type ProjectDTO,
  type MaintenancePointDTO,
} from '@/api/business'

interface Row extends MaintenanceRecordDTO {
  createTime?: string
}

const loading = ref(false)
const tableData = ref<Row[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, code: '', projectId: '', pointId: '', recordType: '' })

const projectOptions = ref<ProjectDTO[]>([])
const pointOptions = ref<MaintenancePointDTO[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const form = reactive<MaintenanceRecordDTO>({
  code: '', projectId: undefined, pointId: undefined, taskId: undefined,
  recordType: 'INSPECTION', recordDate: '', recorderId: undefined,
  content: '', result: '', remark: '',
})
const isEdit = ref(false)

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'
const recordTypeMap: Record<string, string> = {
  INSPECTION: '巡检记录', REPAIR: '维修记录', TEST: '测试记录',
}
const recordTypeTagType: Record<string, TagType> = {
  INSPECTION: 'primary', REPAIR: 'warning', TEST: 'info',
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

async function loadData() {
  loading.value = true
  try {
    const res: any = await maintenanceRecordApi.page(query)
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
  query.projectId = ''
  query.pointId = ''
  query.recordType = ''
  handleSearch()
}

function truncate(s?: string, n = 30) {
  if (!s) return '-'
  return s.length > n ? s.slice(0, n) + '…' : s
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增维保记录'
  Object.assign(form, {
    id: undefined, code: '', projectId: undefined, pointId: undefined, taskId: undefined,
    recordType: 'INSPECTION', recordDate: '', recorderId: undefined,
    content: '', result: '', remark: '',
  })
  dialogVisible.value = true
}

function handleEdit(row: Row) {
  isEdit.value = true
  dialogTitle.value = '编辑维保记录'
  Object.assign(form, {
    id: row.id, code: row.code, projectId: row.projectId, pointId: row.pointId,
    taskId: row.taskId, recordType: row.recordType || 'INSPECTION',
    recordDate: row.recordDate, recorderId: row.recorderId,
    content: row.content, result: row.result, remark: row.remark,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        await maintenanceRecordApi.update(form)
        ElMessage.success('更新成功')
      } else {
        await maintenanceRecordApi.create(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch {}
  })
}

async function handleDelete(row: Row) {
  try {
    await ElMessageBox.confirm(`确定删除记录「${row.code}」吗?`, '提示', { type: 'warning' })
    await maintenanceRecordApi.delete(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

const rules = {
  code: [{ required: true, message: '请输入记录编号', trigger: 'blur' }],
}

onMounted(() => {
  loadProjects()
  loadPoints()
  loadData()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>维保记录</h1>
    </div>

    <div class="card search-bar">
      <el-form inline>
        <el-form-item label="编号">
          <el-input v-model="query.code" placeholder="记录编号" clearable style="width: 160px" @keyup.enter="handleSearch" />
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
        <el-form-item label="类型">
          <el-select v-model="query.recordType" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="(label, key) in recordTypeMap" :key="key" :label="label" :value="key" />
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
        <el-button type="primary" @click="handleAdd">+ 新增记录</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" row-key="id" style="width: 100%">
        <el-table-column prop="code" label="记录编号" min-width="130" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="recordTypeTagType[row.recordType] || 'info'" size="small" effect="light">
              {{ recordTypeMap[row.recordType] || row.recordType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="recordDate" label="记录日期" min-width="120" />
        <el-table-column prop="recorderId" label="记录人" min-width="120" />
        <el-table-column label="内容" min-width="220">
          <template #default="{ row }">{{ truncate(row.content) }}</template>
        </el-table-column>
        <el-table-column prop="result" label="结果" min-width="140" />
        <el-table-column prop="taskId" label="关联任务" min-width="130" />
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
        <el-form-item label="记录编号" prop="code">
          <el-input v-model="form.code" :disabled="isEdit" placeholder="如 R2026-001" />
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
            <el-form-item label="关联任务">
              <el-input v-model="form.taskId" placeholder="任务ID" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="记录类型">
              <el-select v-model="form.recordType" style="width: 100%">
                <el-option v-for="(label, key) in recordTypeMap" :key="key" :label="label" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="记录日期">
              <el-date-picker v-model="form.recordDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="记录人">
              <el-input v-model="form.recorderId" placeholder="记录人ID" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="记录内容">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="记录内容" />
        </el-form-item>
        <el-form-item label="处理结果">
          <el-input v-model="form.result" type="textarea" :rows="3" placeholder="处理结果" />
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
