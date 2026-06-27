<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { contractChangeApi, contractApi, type ContractChangeDTO } from '@/api/business'

interface Option { label: string; value: string }
interface Row extends ContractChangeDTO {}

const contractOptions = ref<Option[]>([])
const loading = ref(false)
const tableData = ref<Row[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, contractId: '', changeType: '', status: '' })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const form = reactive<ContractChangeDTO>({
  contractId: '', changeType: 'AMOUNT_CHANGE', changeDesc: '', supplementFileId: '',
  approverId: '', status: 'NONE', remark: '',
})
const isEdit = ref(false)

const rules = {
  contractId: [{ required: true, message: '请选择合同', trigger: 'change' }],
  changeType: [{ required: true, message: '请选择变更类型', trigger: 'change' }],
  changeDesc: [{ required: true, message: '请输入变更描述', trigger: 'blur' }],
}

const changeTypeMap: Record<string, string> = {
  AMOUNT_CHANGE: '金额变更',
  SCOPE_CHANGE: '范围变更',
  TERM_CHANGE: '期限变更',
  OTHER: '其他',
}
const statusMap: Record<string, string> = {
  NONE: '未审核',
  PENDING: '审核中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
}
const statusTagType: Record<string, string> = {
  NONE: 'info',
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
}

async function loadContracts() {
  try {
    const res: any = await contractApi.page({ pageNum: 1, pageSize: 1000 })
    const list = res.list || []
    contractOptions.value = list.map((c: any) => ({ label: `${c.code} - ${c.name}`, value: c.id }))
  } catch {}
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await contractChangeApi.page(query)
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
  query.contractId = ''
  query.changeType = ''
  query.status = ''
  handleSearch()
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增合同变更'
  Object.assign(form, {
    id: undefined, contractId: '', changeType: 'AMOUNT_CHANGE', changeDesc: '',
    supplementFileId: '', approverId: '', status: 'NONE', remark: '',
  })
  dialogVisible.value = true
}

function handleEdit(row: Row) {
  isEdit.value = true
  dialogTitle.value = '编辑合同变更'
  Object.assign(form, {
    id: row.id, contractId: row.contractId, changeType: row.changeType,
    changeDesc: row.changeDesc, supplementFileId: row.supplementFileId,
    approverId: row.approverId, status: row.status, remark: row.remark,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        await contractChangeApi.update(form)
        ElMessage.success('更新成功')
      } else {
        await contractChangeApi.create(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch {}
  })
}

async function handleDelete(row: Row) {
  try {
    await ElMessageBox.confirm('确定删除该变更记录吗?', '提示', { type: 'warning' })
    await contractChangeApi.delete(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

async function handleAudit(row: Row, status: 'APPROVED' | 'REJECTED') {
  const action = status === 'APPROVED' ? '通过' : '驳回'
  try {
    const { value } = await ElMessageBox.prompt(`请输入${action}意见`, '审核', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '审核意见(可选)',
    })
    await contractChangeApi.audit(row.id!, status, value || '')
    ElMessage.success(`已${action}`)
    loadData()
  } catch {}
}

onMounted(() => {
  loadContracts()
  loadData()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>合同变更</h1>
    </div>

    <div class="card search-bar">
      <el-form inline>
        <el-form-item label="合同">
          <el-select v-model="query.contractId" placeholder="选择合同" clearable filterable style="width: 220px">
            <el-option v-for="opt in contractOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="变更类型">
          <el-select v-model="query.changeType" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="(label, key) in changeTypeMap" :key="key" :label="label" :value="key" />
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
        <el-button type="primary" @click="handleAdd">+ 新增变更</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" row-key="id" style="width: 100%">
        <el-table-column label="合同" min-width="200">
          <template #default="{ row }">
            {{ contractOptions.find(c => c.value === row.contractId)?.label || row.contractId }}
          </template>
        </el-table-column>
        <el-table-column label="变更类型" width="120">
          <template #default="{ row }">{{ changeTypeMap[row.changeType] || row.changeType }}</template>
        </el-table-column>
        <el-table-column prop="changeDesc" label="变更描述" min-width="240" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="(statusTagType[row.status] as any) || 'info'">{{ statusMap[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" link type="success" size="small" @click="handleAudit(row as Row, 'APPROVED')">通过</el-button>
            <el-button v-if="row.status === 'PENDING'" link type="danger" size="small" @click="handleAudit(row as Row, 'REJECTED')">驳回</el-button>
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
        <el-form-item label="合同" prop="contractId">
          <el-select v-model="form.contractId" placeholder="选择合同" filterable clearable style="width: 100%">
            <el-option v-for="opt in contractOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="变更类型" prop="changeType">
          <el-select v-model="form.changeType" style="width: 100%">
            <el-option v-for="(label, key) in changeTypeMap" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="变更描述" prop="changeDesc">
          <el-input v-model="form.changeDesc" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="补充文件ID">
          <el-input v-model="form.supplementFileId" placeholder="补充协议附件ID(可选)" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
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
