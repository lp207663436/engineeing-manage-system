<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { deptApi } from '@/api/system'

const loading = ref(false)
const tableData = ref<any[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const form = reactive<any>({
  id: undefined, parentId: 0, name: '', leader: '', phone: '', sort: 0, status: 1,
})
const isEdit = ref(false)
const deptOptions = ref<any[]>([])
// el-tree-select 节点字段映射(TreeOptionProps 类型未声明 value,用变量引用规避多余属性检查)
const deptTreeProps = { label: 'name', value: 'id', children: 'children' }

async function loadData() {
  loading.value = true
  try {
    tableData.value = await deptApi.tree()
  } finally {
    loading.value = false
  }
}

function handleAdd(parentId = 0) {
  isEdit.value = false
  dialogTitle.value = '新增部门'
  Object.assign(form, { id: undefined, parentId, name: '', leader: '', phone: '', sort: 0, status: 1 })
  deptOptions.value = [{ id: 0, name: '根部门', children: tableData.value }]
  dialogVisible.value = true
}

function handleEdit(row: any) {
  isEdit.value = true
  dialogTitle.value = '编辑部门'
  Object.assign(form, { ...row })
  deptOptions.value = [{ id: 0, name: '根部门', children: tableData.value }]
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) { await deptApi.update(form); ElMessage.success('更新成功') }
      else { await deptApi.create(form); ElMessage.success('新增成功') }
      dialogVisible.value = false
      loadData()
    } catch {}
  })
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除部门「${row.name}」吗?`, '提示', { type: 'warning' })
    await deptApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

const rules = { name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }] }

onMounted(loadData)
</script>

<template>
  <div class="page">
    <div class="page-header"><h1>部门管理</h1></div>
    <div class="card">
      <div class="toolbar"><el-button type="primary" @click="handleAdd(0)">+ 新增部门</el-button></div>
      <el-table
        v-loading="loading"
        :data="tableData"
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
        style="width: 100%"
      >
        <el-table-column prop="name" label="部门名称" min-width="200" />
        <el-table-column prop="leader" label="负责人" min-width="120" />
        <el-table-column prop="phone" label="联系电话" min-width="140" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleAdd(row.id)">新增子部门</el-button>
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="form.parentId"
            :data="deptOptions"
            :props="deptTreeProps"
            check-strictly
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="name"><el-input v-model="form.name" placeholder="请输入部门名称" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model="form.leader" placeholder="请输入负责人" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.phone" placeholder="请输入联系电话" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
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
.card { background: #fff; border: 1px solid #E5E7EB; border-radius: 8px; padding: 20px; }
.toolbar { margin-bottom: 16px; }
</style>
