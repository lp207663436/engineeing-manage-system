<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { maintenancePointApi, projectApi, type MaintenancePointDTO } from '@/api/business'
import { userApi } from '@/api/system'

interface Option { label: string; value: string }

interface MaintenancePoint extends MaintenancePointDTO {
  createTime?: string
}

const projectOptions = ref<Option[]>([])
const userOptions = ref<Option[]>([])

const loading = ref(false)
const tableData = ref<MaintenancePoint[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, code: '', name: '', status: '', projectId: '' })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const form = reactive<MaintenancePointDTO>({
  code: '', projectId: undefined, name: '', location: '', equipmentList: '',
  managerId: undefined, status: 'WAITING_QUOTE',
})
const isEdit = ref(false)

const statusMap: Record<string, string> = {
  WAITING_QUOTE: '待报价', QUOTED: '已报价', CONSTRUCTING: '施工中',
  WAITING_ACCEPTANCE: '待验收', ACCEPTED: '已验收', SETTLED: '已结算',
}
const statusTagType: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
  WAITING_QUOTE: 'info', QUOTED: 'warning', CONSTRUCTING: 'primary',
  WAITING_ACCEPTANCE: 'warning', ACCEPTED: 'success', SETTLED: 'success',
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await maintenancePointApi.page(query)
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
  dialogTitle.value = '新增维护点位'
  Object.assign(form, {
    id: undefined, code: '', projectId: undefined, name: '', location: '', equipmentList: '',
    managerId: undefined, status: 'WAITING_QUOTE',
  })
  dialogVisible.value = true
}

function handleEdit(row: MaintenancePoint) {
  isEdit.value = true
  dialogTitle.value = '编辑维护点位'
  Object.assign(form, {
    id: row.id, code: row.code, projectId: row.projectId, name: row.name,
    location: row.location, equipmentList: row.equipmentList,
    managerId: row.managerId, status: row.status,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        await maintenancePointApi.update(form)
        ElMessage.success('更新成功')
      } else {
        await maintenancePointApi.create(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch {}
  })
}

async function handleDelete(row: MaintenancePoint) {
  try {
    await ElMessageBox.confirm(`确定删除维护点位「${row.name}」吗?`, '提示', { type: 'warning' })
    await maintenancePointApi.delete(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

const rules = {
  code: [{ required: true, message: '请输入点位编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入点位名称', trigger: 'blur' }],
  projectId: [{ required: true, message: '请选择所属项目', trigger: 'change' }],
  managerId: [{ required: true, message: '请选择负责人', trigger: 'change' }],
}

async function loadOptions() {
  try {
    const [proj, users] = await Promise.all([
      projectApi.page({ pageNum: 1, pageSize: 200 }),
      userApi.page({ pageNum: 1, pageSize: 200 }),
    ]) as any[]
    projectOptions.value = (proj.list || []).map((p: any) => ({ label: `${p.code} ${p.name}`, value: p.id }))
    userOptions.value = (users.list || []).map((u: any) => ({ label: u.name, value: u.id }))
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
      <h1>维护点位</h1>
    </div>

    <div class="card search-bar">
      <el-form inline>
        <el-form-item label="编号">
          <el-input v-model="query.code" placeholder="点位编号" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="query.name" placeholder="点位名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
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
        <el-button type="primary" @click="handleAdd">+ 新增点位</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" row-key="id" style="width: 100%">
        <el-table-column prop="code" label="点位编号" min-width="130" />
        <el-table-column prop="name" label="点位名称" min-width="160" />
        <el-table-column prop="location" label="位置" min-width="160" />
        <el-table-column prop="equipmentList" label="设备清单" min-width="180" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.status] || 'info'" size="small" effect="light">
              {{ statusMap[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="managerId" label="负责人ID" min-width="120" />
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row as MaintenancePoint)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row as MaintenancePoint)">删除</el-button>
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
        <el-form-item label="点位编号" prop="code">
          <el-input v-model="form.code" :disabled="isEdit" placeholder="如 MP2026-001" />
        </el-form-item>
        <el-form-item label="点位名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入点位名称" />
        </el-form-item>
        <el-form-item label="项目" prop="projectId">
          <el-select v-model="form.projectId" placeholder="请选择项目" clearable filterable style="width: 100%">
            <el-option v-for="opt in projectOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="form.location" placeholder="请输入位置" />
        </el-form-item>
        <el-form-item label="设备清单">
          <el-input v-model="form.equipmentList" type="textarea" :rows="3" placeholder="设备清单" />
        </el-form-item>
        <el-form-item label="负责人" prop="managerId">
          <el-select v-model="form.managerId" placeholder="请选择负责人" clearable filterable style="width: 100%">
            <el-option v-for="opt in userOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="(label, key) in statusMap" :key="key" :label="label" :value="key" />
          </el-select>
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
