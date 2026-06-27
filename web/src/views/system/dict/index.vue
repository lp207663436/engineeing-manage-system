<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { dictApi, type SysDictDTO, type SysDictItemDTO } from '@/api/system'

// 字典主表
const dictLoading = ref(false)
const dictList = ref<SysDictDTO[]>([])
const dictTotal = ref(0)
const dictQuery = reactive({ pageNum: 1, pageSize: 20, name: '', code: '' })
const currentDictId = ref<string>('')

const dictDialogVisible = ref(false)
const dictDialogTitle = ref('')
const dictFormRef = ref<FormInstance>()
const dictForm = reactive<SysDictDTO>({ code: '', name: '', remark: '' })
const dictIsEdit = ref(false)
const dictRules = {
  code: [{ required: true, message: '请输入字典编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
}

// 字典项
const itemLoading = ref(false)
const itemList = ref<SysDictItemDTO[]>([])
const itemDialogVisible = ref(false)
const itemDialogTitle = ref('')
const itemFormRef = ref<FormInstance>()
const itemForm = reactive<SysDictItemDTO>({ dictId: '', label: '', value: '', sort: 1 })
const itemIsEdit = ref(false)
const itemRules = {
  label: [{ required: true, message: '请输入字典项标签', trigger: 'blur' }],
  value: [{ required: true, message: '请输入字典项值', trigger: 'blur' }],
}

async function loadDictList() {
  dictLoading.value = true
  try {
    const res: any = await dictApi.page(dictQuery)
    dictList.value = res.list || []
    dictTotal.value = res.total || 0
  } finally {
    dictLoading.value = false
  }
}

function handleDictSearch() {
  dictQuery.pageNum = 1
  loadDictList()
}

function handleDictReset() {
  dictQuery.name = ''
  dictQuery.code = ''
  handleDictSearch()
}

function handleDictAdd() {
  dictIsEdit.value = false
  dictDialogTitle.value = '新增字典'
  Object.assign(dictForm, { id: undefined, code: '', name: '', remark: '' })
  dictDialogVisible.value = true
}

function handleDictEdit(row: SysDictDTO) {
  dictIsEdit.value = true
  dictDialogTitle.value = '编辑字典'
  Object.assign(dictForm, { id: row.id, code: row.code, name: row.name, remark: row.remark })
  dictDialogVisible.value = true
}

async function handleDictSubmit() {
  if (!dictFormRef.value) return
  await dictFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (dictIsEdit.value) {
        await dictApi.update(dictForm)
        ElMessage.success('更新成功')
      } else {
        await dictApi.create(dictForm)
        ElMessage.success('新增成功')
      }
      dictDialogVisible.value = false
      loadDictList()
    } catch {}
  })
}

async function handleDictDelete(row: SysDictDTO) {
  try {
    await ElMessageBox.confirm(`确定删除字典「${row.name}」吗?其下字典项也会一并删除`, '提示', { type: 'warning' })
    await dictApi.delete(row.id!)
    ElMessage.success('删除成功')
    if (currentDictId.value === row.id) {
      currentDictId.value = ''
      itemList.value = []
    }
    loadDictList()
  } catch {}
}

function handleSelectDict(row: SysDictDTO) {
  currentDictId.value = row.id!
  loadItems()
}

// 字典项管理
async function loadItems() {
  if (!currentDictId.value) return
  itemLoading.value = true
  try {
    const res: any = await dictApi.itemsByDictId(currentDictId.value)
    itemList.value = res || []
  } finally {
    itemLoading.value = false
  }
}

function handleItemAdd() {
  itemIsEdit.value = false
  itemDialogTitle.value = '新增字典项'
  Object.assign(itemForm, { id: undefined, dictId: currentDictId.value, label: '', value: '', sort: itemList.value.length + 1 })
  itemDialogVisible.value = true
}

function handleItemEdit(row: SysDictItemDTO) {
  itemIsEdit.value = true
  itemDialogTitle.value = '编辑字典项'
  Object.assign(itemForm, { id: row.id, dictId: row.dictId, label: row.label, value: row.value, sort: row.sort })
  itemDialogVisible.value = true
}

async function handleItemSubmit() {
  if (!itemFormRef.value) return
  await itemFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (itemIsEdit.value) {
        await dictApi.itemUpdate(itemForm)
        ElMessage.success('更新成功')
      } else {
        await dictApi.itemCreate(itemForm)
        ElMessage.success('新增成功')
      }
      itemDialogVisible.value = false
      loadItems()
    } catch {}
  })
}

async function handleItemDelete(row: SysDictItemDTO) {
  try {
    await ElMessageBox.confirm(`确定删除字典项「${row.label}」吗?`, '提示', { type: 'warning' })
    await dictApi.itemDelete(row.id!)
    ElMessage.success('删除成功')
    loadItems()
  } catch {}
}

onMounted(() => {
  loadDictList()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>数据字典</h1>
    </div>

    <div class="dict-layout">
      <!-- 左侧:字典列表 -->
      <div class="card dict-left">
        <div class="search-bar">
          <el-input v-model="dictQuery.name" placeholder="字典名称" clearable size="small" style="width: 130px" @keyup.enter="handleDictSearch" />
          <el-input v-model="dictQuery.code" placeholder="字典编码" clearable size="small" style="width: 130px; margin-left: 8px" @keyup.enter="handleDictSearch" />
          <el-button type="primary" size="small" style="margin-left: 8px" @click="handleDictSearch">查询</el-button>
          <el-button size="small" @click="handleDictReset">重置</el-button>
        </div>
        <div class="toolbar">
          <el-button type="primary" size="small" @click="handleDictAdd">+ 新增字典</el-button>
        </div>
        <el-table
          v-loading="dictLoading"
          :data="dictList"
          size="small"
          style="width: 100%"
          highlight-current-row
          @current-change="(row: any) => row && handleSelectDict(row as SysDictDTO)"
        >
          <el-table-column prop="code" label="编码" width="140" />
          <el-table-column prop="name" label="名称" min-width="140" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click.stop="handleDictEdit(row as SysDictDTO)">编辑</el-button>
              <el-button link type="danger" size="small" @click.stop="handleDictDelete(row as SysDictDTO)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination">
          <el-pagination
            v-model:current-page="dictQuery.pageNum"
            v-model:page-size="dictQuery.pageSize"
            :total="dictTotal"
            layout="prev, pager, next"
            small
            @current-change="loadDictList"
          />
        </div>
      </div>

      <!-- 右侧:字典项 -->
      <div class="card dict-right">
        <div class="right-header">
          <div class="right-title">
            字典项
            <span v-if="currentDictId" class="right-tip">当前字典ID: {{ currentDictId }}</span>
            <span v-else class="right-tip-empty">请从左侧选择字典</span>
          </div>
          <el-button type="primary" size="small" :disabled="!currentDictId" @click="handleItemAdd">+ 新增字典项</el-button>
        </div>
        <el-table v-loading="itemLoading" :data="itemList" size="small" style="width: 100%">
          <el-table-column prop="label" label="标签" min-width="160" />
          <el-table-column prop="value" label="值" min-width="140" />
          <el-table-column prop="sort" label="排序" width="80" align="center" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="handleItemEdit(row as SysDictItemDTO)">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleItemDelete(row as SysDictItemDTO)">删除</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <div class="empty-tip">{{ currentDictId ? '该字典暂无字典项' : '请先从左侧选择一个字典' }}</div>
          </template>
        </el-table>
      </div>
    </div>

    <!-- 字典主表弹窗 -->
    <el-dialog v-model="dictDialogVisible" :title="dictDialogTitle" width="480px">
      <el-form ref="dictFormRef" :model="dictForm" :rules="dictRules" label-width="80px">
        <el-form-item label="编码" prop="code">
          <el-input v-model="dictForm.code" placeholder="如 contract_type" :disabled="dictIsEdit" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="dictForm.name" placeholder="如 合同类型" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dictForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dictDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDictSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 字典项弹窗 -->
    <el-dialog v-model="itemDialogVisible" :title="itemDialogTitle" width="480px">
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="80px">
        <el-form-item label="标签" prop="label">
          <el-input v-model="itemForm.label" placeholder="显示文字" />
        </el-form-item>
        <el-form-item label="值" prop="value">
          <el-input v-model="itemForm.value" placeholder="存储值" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="itemForm.sort" :min="1" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleItemSubmit">确定</el-button>
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
  padding: 16px;
}
.dict-layout {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 16px;
}
.dict-left { display: flex; flex-direction: column; }
.dict-right {
  display: flex;
  flex-direction: column;
  min-height: 400px;
}
.search-bar { margin-bottom: 12px; }
.toolbar { margin-bottom: 12px; }
.pagination { display: flex; justify-content: flex-end; margin-top: 12px; }
.right-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.right-title {
  font-size: 15px;
  font-weight: 600;
  color: #1F2937;
  .right-tip { font-size: 12px; color: #6B7280; margin-left: 8px; font-weight: 400; }
  .right-tip-empty { font-size: 12px; color: #9CA3AF; margin-left: 8px; font-weight: 400; }
}
.empty-tip {
  padding: 32px 0;
  text-align: center;
  font-size: 13px;
  color: #9CA3AF;
}
</style>
