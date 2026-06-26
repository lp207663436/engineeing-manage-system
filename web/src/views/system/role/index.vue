<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { roleApi, menuApi, type SysRoleDTO } from '@/api/system'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, name: '' })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const form = reactive<SysRoleDTO>({ name: '', code: '', dataScope: 1, sort: 0, status: 1, menuIds: [] })
const isEdit = ref(false)

const menuDialogVisible = ref(false)
const menuTree = ref<any[]>([])
const currentRoleId = ref<number>(0)
const checkedMenuIds = ref<number[]>([])

async function loadData() {
  loading.value = true
  try {
    const res: any = await roleApi.page(query)
    tableData.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.pageNum = 1; loadData() }
function handleReset() { query.name = ''; handleSearch() }

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增角色'
  Object.assign(form, { id: undefined, name: '', code: '', dataScope: 1, sort: 0, status: 1, menuIds: [] })
  dialogVisible.value = true
}

function handleEdit(row: any) {
  isEdit.value = true
  dialogTitle.value = '编辑角色'
  Object.assign(form, { id: row.id, name: row.name, code: row.code, dataScope: row.dataScope, sort: row.sort, status: row.status, menuIds: [] })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) { await roleApi.update(form); ElMessage.success('更新成功') }
      else { await roleApi.create(form); ElMessage.success('新增成功') }
      dialogVisible.value = false
      loadData()
    } catch {}
  })
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除角色「${row.name}」吗?`, '提示', { type: 'warning' })
    await roleApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

async function handleAssignMenus(row: any) {
  currentRoleId.value = row.id
  menuTree.value = await menuApi.tree()
  const ids: number[] = await roleApi.getMenuIds(currentRoleId.value)
  checkedMenuIds.value = ids
  menuDialogVisible.value = true
}

async function handleAssignSubmit() {
  const tree = (treeRef.value as any)?.getCheckedKeys() || []
  const halfChecked = (treeRef.value as any)?.getHalfCheckedKeys() || []
  await roleApi.update({ id: currentRoleId.value, name: '', code: '', menuIds: [...tree, ...halfChecked] } as any)
  ElMessage.success('分配成功')
  menuDialogVisible.value = false
}

const treeRef = ref()
const rules = {
  name: [{ required: true, message: '请输入角色名', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
}

const dataScopeMap: Record<number, string> = { 1: '全部', 2: '本部门', 3: '本人' }

onMounted(loadData)
</script>

<template>
  <div class="page">
    <div class="page-header"><h1>角色管理</h1></div>
    <div class="card search-bar">
      <el-form inline>
        <el-form-item label="角色名">
          <el-input v-model="query.name" placeholder="请输入角色名" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="card">
      <div class="toolbar"><el-button type="primary" @click="handleAdd">+ 新增角色</el-button></div>
      <el-table v-loading="loading" :data="tableData" style="width: 100%">
        <el-table-column prop="name" label="角色名" min-width="140" />
        <el-table-column prop="code" label="编码" min-width="140" />
        <el-table-column label="数据范围" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="light">{{ dataScopeMap[row.dataScope] || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="primary" size="small" @click="handleAssignMenus(row)">分配菜单</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
          :total="total" :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadData" @size-change="loadData"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="角色名" prop="name"><el-input v-model="form.name" placeholder="请输入角色名" /></el-form-item>
        <el-form-item label="编码" prop="code"><el-input v-model="form.code" placeholder="请输入编码" /></el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="form.dataScope" style="width: 100%">
            <el-option :value="1" label="全部数据" />
            <el-option :value="2" label="本部门数据" />
            <el-option :value="3" label="本人数据" />
          </el-select>
        </el-form-item>
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

    <el-dialog v-model="menuDialogVisible" title="分配菜单" width="480px">
      <el-tree
        ref="treeRef"
        :data="menuTree"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        show-checkbox
        :default-checked-keys="checkedMenuIds"
        default-expand-all
      />
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssignSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.page { padding: 24px; }
.page-header { margin-bottom: 16px; h1 { font-size: 20px; font-weight: 600; color: #1F2937; } }
.card { background: #fff; border: 1px solid #E5E7EB; border-radius: 8px; padding: 20px; margin-bottom: 16px; }
.search-bar { padding: 16px 20px 0; }
.toolbar { margin-bottom: 16px; }
.pagination { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
