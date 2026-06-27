<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { attachmentApi, type AttachmentDTO } from '@/api/business'
import type { UploadRawFile } from 'element-plus'
import FilePreview from '@/components/FilePreview.vue'

interface Row extends AttachmentDTO {}

const loading = ref(false)
const tableData = ref<Row[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, name: '', businessType: '' })

const businessTypeMap: Record<string, string> = {
  CONTRACT: '合同', QUOTE: '报价', EQUIPMENT: '设备', ACCEPTANCE: '验收', MAINTENANCE: '维保',
}

// 上传
const uploadDialogVisible = ref(false)
const uploadFormRef = ref<FormInstance>()
const uploadForm = reactive({ businessType: 'CONTRACT', businessId: '' })
const uploadRules = {
  businessType: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
  businessId: [{ required: true, message: '请输入业务ID', trigger: 'blur' }],
}
const fileList = ref<File[]>([])
const uploading = ref(false)

// 预览
const previewVisible = ref(false)
const currentAttachmentId = ref<string>('')

async function loadData() {
  loading.value = true
  try {
    const res: any = await attachmentApi.page(query)
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
  query.businessType = ''
  handleSearch()
}

function formatSize(v?: number) {
  if (v == null) return '-'
  const kb = v / 1024
  if (kb < 1024) return `${kb.toFixed(2)} KB`
  return `${(kb / 1024).toFixed(2)} MB`
}

function handleFileChange(file: { raw: File }) {
  fileList.value.push(file.raw)
}

function handleFileRemove(file: { raw: File }) {
  fileList.value = fileList.value.filter((f) => f !== file.raw)
}

function openUploadDialog() {
  uploadForm.businessType = 'CONTRACT'
  uploadForm.businessId = ''
  fileList.value = []
  uploadDialogVisible.value = true
}

async function handleUploadSubmit() {
  if (!uploadFormRef.value) return
  await uploadFormRef.value.validate(async (valid) => {
    if (!valid) return
    if (fileList.value.length === 0) {
      ElMessage.warning('请选择至少一个文件')
      return
    }
    uploading.value = true
    try {
      for (const file of fileList.value) {
        await attachmentApi.upload(file, uploadForm.businessType, uploadForm.businessId)
      }
      ElMessage.success(`已上传 ${fileList.value.length} 个文件`)
      uploadDialogVisible.value = false
      loadData()
    } catch {
    } finally {
      uploading.value = false
    }
  })
}

function handleDownload(row: Row) {
  if (!row.id) return
  window.open(attachmentApi.downloadUrl(row.id), '_blank')
}

function handlePreview(row: Row) {
  if (!row.id) return
  currentAttachmentId.value = row.id
  previewVisible.value = true
}

async function handleDelete(row: Row) {
  try {
    await ElMessageBox.confirm(`确定删除附件「${row.name}」吗?`, '提示', { type: 'warning' })
    await attachmentApi.delete(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

// 阻止 el-upload 自动上传,改为手动
function beforeUploadHandler(_file: UploadRawFile) {
  return false
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>附件管理</h1>
    </div>

    <div class="card search-bar">
      <el-form inline>
        <el-form-item label="名称">
          <el-input v-model="query.name" placeholder="附件名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="业务类型">
          <el-select v-model="query.businessType" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="(label, key) in businessTypeMap" :key="key" :label="label" :value="key" />
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
        <el-button type="primary" @click="openUploadDialog">+ 上传附件</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" row-key="id" style="width: 100%">
        <el-table-column prop="name" label="附件名称" min-width="220" />
        <el-table-column label="业务类型" width="120">
          <template #default="{ row }">{{ businessTypeMap[row.businessType] || row.businessType }}</template>
        </el-table-column>
        <el-table-column label="文件大小" width="120" align="right">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="fileType" label="文件类型" width="120" />
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handlePreview(row)">预览</el-button>
            <el-button link type="primary" size="small" @click="handleDownload(row)">下载</el-button>
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

    <el-dialog v-model="uploadDialogVisible" title="上传附件" width="520px">
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="100px">
        <el-form-item label="业务类型" prop="businessType">
          <el-select v-model="uploadForm.businessType" style="width: 100%">
            <el-option v-for="(label, key) in businessTypeMap" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务ID" prop="businessId">
          <el-input v-model="uploadForm.businessId" placeholder="关联业务对象的ID" />
        </el-form-item>
        <el-form-item label="选择文件">
          <el-upload
            :auto-upload="false"
            :before-upload="beforeUploadHandler"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            multiple
            drag
          >
            <div class="upload-inner">
              <div class="upload-tip">点击或拖拽文件到此处</div>
              <div class="upload-sub">支持多文件上传</div>
            </div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUploadSubmit">开始上传</el-button>
      </template>
    </el-dialog>

    <FilePreview v-model:visible="previewVisible" :attachment-id="currentAttachmentId" />
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
.upload-inner { padding: 20px 0; }
.upload-tip { font-size: 14px; color: #4B5563; }
.upload-sub { font-size: 12px; color: #9CA3AF; margin-top: 4px; }
</style>
