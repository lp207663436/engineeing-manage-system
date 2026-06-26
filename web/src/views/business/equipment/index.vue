<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { equipmentApi, type EquipmentDTO } from '@/api/business'

interface Equipment extends EquipmentDTO {
  createTime?: string
}

const loading = ref(false)
const tableData = ref<Equipment[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1, pageSize: 10, code: '', name: '', category: '', status: '',
})

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const form = reactive<EquipmentDTO>({
  code: '', name: '', brand: '', model: '', serialNumber: '', category: '',
  specs: '', commissioningDate: '', warrantyExpiry: '', status: 'NORMAL',
  projectId: undefined, pointId: undefined,
})
const isEdit = ref(false)

const statusMap: Record<string, string> = {
  NORMAL: '正常', FAULT: '故障', REPAIRING: '维修中', SCRAPPED: '报废',
}
const statusTagType: Record<string, string> = {
  NORMAL: 'success', FAULT: 'danger', REPAIRING: 'warning', SCRAPPED: 'info',
}
const categoryOptions = ['门禁', '监控', '网络', '楼控', '消防', '广播', '其他']

async function loadData() {
  loading.value = true
  try {
    const res: any = await equipmentApi.page(query)
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
  query.category = ''
  query.status = ''
  handleSearch()
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增设备'
  Object.assign(form, {
    id: undefined, code: '', name: '', brand: '', model: '', serialNumber: '',
    category: '', specs: '', commissioningDate: '', warrantyExpiry: '',
    status: 'NORMAL', projectId: undefined, pointId: undefined,
  })
  dialogVisible.value = true
}

function handleEdit(row: Equipment) {
  isEdit.value = true
  dialogTitle.value = '编辑设备'
  Object.assign(form, {
    id: row.id, code: row.code, name: row.name, brand: row.brand, model: row.model,
    serialNumber: row.serialNumber, category: row.category, specs: row.specs,
    commissioningDate: row.commissioningDate, warrantyExpiry: row.warrantyExpiry,
    status: row.status, projectId: row.projectId, pointId: row.pointId,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        await equipmentApi.update(form)
        ElMessage.success('更新成功')
      } else {
        await equipmentApi.create(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch {}
  })
}

async function handleDelete(row: Equipment) {
  try {
    await ElMessageBox.confirm(`确定删除设备「${row.name}」吗?`, '提示', { type: 'warning' })
    await equipmentApi.delete(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

const rules = {
  code: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>设备台账</h1>
    </div>

    <div class="card search-bar">
      <el-form inline>
        <el-form-item label="编号">
          <el-input v-model="query.code" placeholder="设备编号" clearable style="width: 150px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="query.name" placeholder="设备名称" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.category" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
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
        <el-button type="primary" @click="handleAdd">+ 新增设备</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" row-key="id" style="width: 100%">
        <el-table-column prop="code" label="设备编号" min-width="120" />
        <el-table-column prop="name" label="设备名称" min-width="150" />
        <el-table-column prop="brand" label="品牌" min-width="100" />
        <el-table-column prop="model" label="型号" min-width="130" />
        <el-table-column prop="serialNumber" label="序列号" min-width="130" />
        <el-table-column prop="category" label="分类" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.status] || 'info'" size="small" effect="light">
              {{ statusMap[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="commissioningDate" label="投运日期" min-width="120" />
        <el-table-column prop="warrantyExpiry" label="保修到期" min-width="120" />
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
        <el-form-item label="设备编号" prop="code">
          <el-input v-model="form.code" :disabled="isEdit" placeholder="如 EQ2026-001" />
        </el-form-item>
        <el-form-item label="设备名称" prop="name">
          <el-input v-model="form.name" placeholder="设备名称" />
        </el-form-item>
        <el-form-item label="品牌">
          <el-input v-model="form.brand" placeholder="品牌" />
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="form.model" placeholder="型号" />
        </el-form-item>
        <el-form-item label="序列号">
          <el-input v-model="form.serialNumber" placeholder="序列号" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="请选择" style="width: 100%">
            <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="规格参数">
          <el-input v-model="form.specs" type="textarea" :rows="2" placeholder="规格参数" />
        </el-form-item>
        <el-form-item label="投运日期">
          <el-date-picker v-model="form.commissioningDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="保修到期">
          <el-date-picker v-model="form.warrantyExpiry" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
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
