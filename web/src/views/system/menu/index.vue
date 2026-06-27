<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { menuApi } from '@/api/system'

const loading = ref(false)
const tableData = ref<any[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const form = reactive<any>({
  id: undefined, parentId: 0, name: '', type: 2, permission: '', path: '', icon: '', sort: 0, status: 1,
})
const isEdit = ref(false)
const menuOptions = ref<any[]>([])

const typeMap: Record<number, { label: string; type: string }> = {
  1: { label: '目录', type: 'info' },
  2: { label: '菜单', type: 'success' },
  3: { label: '按钮', type: 'warning' },
}

async function loadData() {
  loading.value = true
  try {
    tableData.value = await menuApi.list()
  } finally {
    loading.value = false
  }
}

function handleAdd(parentId = 0) {
  isEdit.value = false
  dialogTitle.value = '新增菜单'
  Object.assign(form, { id: undefined, parentId, name: '', type: 2, permission: '', path: '', icon: '', sort: 0, status: 1 })
  menuOptions.value = [{ id: 0, name: '根目录', children: tableData.value }]
  dialogVisible.value = true
}

function handleEdit(row: any) {
  isEdit.value = true
  dialogTitle.value = '编辑菜单'
  Object.assign(form, { ...row })
  menuOptions.value = [{ id: 0, name: '根目录', children: tableData.value }]
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) { await menuApi.update(form); ElMessage.success('更新成功') }
      else { await menuApi.create(form); ElMessage.success('新增成功') }
      dialogVisible.value = false
      loadData()
    } catch {}
  })
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除菜单「${row.name}」吗?`, '提示', { type: 'warning' })
    await menuApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  permission: [{
    validator: (_r: any, v: any, cb: any) => {
      if (form.type === 3 && !v) cb(new Error('按钮类型必须填写权限标识'))
      else cb()
    }, trigger: 'blur',
  }],
  path: [{
    validator: (_r: any, v: any, cb: any) => {
      if ((form.type === 1 || form.type === 2) && !v) cb(new Error('目录/菜单类型必须填写路径'))
      else cb()
    }, trigger: 'blur',
  }],
}

onMounted(loadData)
</script>

<template>
  <div class="page">
    <div class="page-header"><h1>菜单管理</h1></div>
    <div class="card">
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd(0)">+ 新增菜单</el-button>
      </div>
      <el-table
        v-loading="loading"
        :data="tableData"
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
        style="width: 100%"
      >
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="icon" label="图标" width="100" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="typeMap[row.type]?.type" effect="light">{{ typeMap[row.type]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="permission" label="权限标识" min-width="160" />
        <el-table-column prop="path" label="路径" min-width="140" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleAdd(row.id)">新增子项</el-button>
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="menuOptions"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">目录</el-radio>
            <el-radio :value="2">菜单</el-radio>
            <el-radio :value="3">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" placeholder="请输入名称" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" placeholder="Lucide 图标名,如 User" /></el-form-item>
        <el-form-item label="权限标识" prop="permission"><el-input v-model="form.permission" placeholder="如 system:user:list" /></el-form-item>
        <el-form-item label="路径" prop="path"><el-input v-model="form.path" placeholder="如 /system/user" /></el-form-item>
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
