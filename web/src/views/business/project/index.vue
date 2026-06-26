<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { projectApi, type ProjectDTO } from '@/api/business'

interface Project extends ProjectDTO {
  createTime?: string
}

const loading = ref(false)
const tableData = ref<Project[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, name: '', type: '', status: '' })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const form = reactive<ProjectDTO>({
  code: '', name: '', customerName: '', managerId: undefined, address: '',
  startDate: '', endDate: '', type: 'NEW_BUILD', status: 'DRAFT', description: '',
})
const isEdit = ref(false)

const typeMap: Record<string, string> = { NEW_BUILD: '新建工程', MAINTENANCE: '维护型' }
const statusMap: Record<string, string> = {
  DRAFT: '草稿', IN_PROGRESS: '进行中', COMPLETED: '已竣工', ARCHIVED: '已归档',
}
const statusTagType: Record<string, string> = {
  DRAFT: 'info', IN_PROGRESS: 'warning', COMPLETED: 'success', ARCHIVED: 'info',
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await projectApi.page(query)
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
  query.name = ''
  query.type = ''
  query.status = ''
  handleSearch()
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增项目'
  Object.assign(form, {
    id: undefined, code: '', name: '', customerName: '', managerId: undefined, address: '',
    startDate: '', endDate: '', type: 'NEW_BUILD', status: 'DRAFT', description: '',
  })
  dialogVisible.value = true
}

function handleEdit(row: Project) {
  isEdit.value = true
  dialogTitle.value = '编辑项目'
  Object.assign(form, {
    id: row.id, code: row.code, name: row.name, customerName: row.customerName,
    managerId: row.managerId, address: row.address, startDate: row.startDate,
    endDate: row.endDate, type: row.type, status: row.status, description: row.description,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        await projectApi.update(form)
        ElMessage.success('更新成功')
      } else {
        await projectApi.create(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch {}
  })
}

async function handleDelete(row: Project) {
  try {
    await ElMessageBox.confirm(`确定删除项目「${row.name}」吗?`, '提示', { type: 'warning' })
    await projectApi.delete(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

const rules = {
  code: [{ required: true, message: '请输入项目编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>项目管理</h1>
    </div>

    <div class="card search-bar">
      <el-form inline>
        <el-form-item label="名称">
          <el-input v-model="query.name" placeholder="项目名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 140px">
            <el-option label="新建工程" value="NEW_BUILD" />
            <el-option label="维护型" value="MAINTENANCE" />
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
        <el-button type="primary" @click="handleAdd">+ 新增项目</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" row-key="id" style="width: 100%">
        <el-table-column prop="code" label="项目编号" min-width="120" />
        <el-table-column prop="name" label="项目名称" min-width="180" />
        <el-table-column prop="customerName" label="客户" min-width="140" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ typeMap[row.type] || row.type }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.status] || 'info'" size="small" effect="light">
              {{ statusMap[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="开工日期" min-width="120" />
        <el-table-column prop="endDate" label="竣工日期" min-width="120" />
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
        <el-form-item label="项目编号" prop="code">
          <el-input v-model="form.code" :disabled="isEdit" placeholder="如 P2026-001" />
        </el-form-item>
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="客户名称">
          <el-input v-model="form.customerName" placeholder="请输入客户名称" />
        </el-form-item>
        <el-form-item label="项目地址">
          <el-input v-model="form.address" placeholder="请输入项目地址" />
        </el-form-item>
        <el-form-item label="项目类型">
          <el-radio-group v-model="form.type">
            <el-radio value="NEW_BUILD">新建工程</el-radio>
            <el-radio value="MAINTENANCE">维护型</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="项目状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="(label, key) in statusMap" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="开工日期">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="竣工日期">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="项目描述" />
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
