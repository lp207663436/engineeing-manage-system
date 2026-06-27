<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { customerApi, type CustomerDTO } from '@/api/business'

interface Row extends CustomerDTO {
  createTime?: string
}

const loading = ref(false)
const tableData = ref<Row[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, name: '', code: '' })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const form = reactive<CustomerDTO>({
  code: '', name: '', contactPerson: '', contactPhone: '', contactEmail: '',
  address: '', bankAccount: '', bankName: '', remark: '',
})
const isEdit = ref(false)

const rules = {
  code: [{ required: true, message: '请输入客户编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  contactPhone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  contactEmail: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await customerApi.page(query)
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
  query.code = ''
  handleSearch()
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增客户'
  Object.assign(form, {
    id: undefined, code: '', name: '', contactPerson: '', contactPhone: '', contactEmail: '',
    address: '', bankAccount: '', bankName: '', remark: '',
  })
  dialogVisible.value = true
}

function handleEdit(row: Row) {
  isEdit.value = true
  dialogTitle.value = '编辑客户'
  Object.assign(form, {
    id: row.id, code: row.code, name: row.name, contactPerson: row.contactPerson,
    contactPhone: row.contactPhone, contactEmail: row.contactEmail, address: row.address,
    bankAccount: row.bankAccount, bankName: row.bankName, remark: row.remark,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        await customerApi.update(form)
        ElMessage.success('更新成功')
      } else {
        await customerApi.create(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch {}
  })
}

async function handleDelete(row: Row) {
  try {
    await ElMessageBox.confirm(`确定删除客户「${row.name}」吗?`, '提示', { type: 'warning' })
    await customerApi.delete(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>客户档案</h1>
    </div>

    <div class="card search-bar">
      <el-form inline>
        <el-form-item label="名称">
          <el-input v-model="query.name" placeholder="客户名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="编号">
          <el-input v-model="query.code" placeholder="客户编号" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">+ 新增客户</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" row-key="id" style="width: 100%">
        <el-table-column prop="code" label="客户编号" width="140" />
        <el-table-column prop="name" label="客户名称" min-width="200" />
        <el-table-column prop="contactPerson" label="联系人" width="120" />
        <el-table-column prop="contactPhone" label="联系电话" width="140" />
        <el-table-column prop="bankName" label="开户行" min-width="160" />
        <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
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
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="客户编号" prop="code">
              <el-input v-model="form.code" placeholder="如 C001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户名称" prop="name">
              <el-input v-model="form.name" placeholder="客户/公司名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model="form.contactPerson" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系邮箱" prop="contactEmail">
              <el-input v-model="form.contactEmail" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开户行">
              <el-input v-model="form.bankName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="银行账号">
              <el-input v-model="form.bankAccount" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="地址">
              <el-input v-model="form.address" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
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
