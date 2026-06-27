<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { approvalApi, type ApprovalLogDTO } from '@/api/business'

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

const activeTab = ref<'pending' | 'history' | 'all'>('pending')

const pendingLoading = ref(false)
const pendingData = ref<ApprovalLogDTO[]>([])
const historyLoading = ref(false)
const historyData = ref<ApprovalLogDTO[]>([])

const allLoading = ref(false)
const allData = ref<ApprovalLogDTO[]>([])
const allTotal = ref(0)
const allQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  businessType: '',
  businessId: '',
  result: '',
})

const businessTypeMap: Record<string, string> = {
  CONTRACT_APPROVAL: '合同审批',
  QUOTE_APPROVAL: '报价审批',
}
const businessTypeTag: Record<string, TagType> = {
  CONTRACT_APPROVAL: 'primary',
  QUOTE_APPROVAL: 'warning',
}
const resultMap: Record<string, string> = {
  APPROVED: '同意',
  REJECTED: '拒绝',
}
const resultTag: Record<string, TagType> = {
  APPROVED: 'success',
  REJECTED: 'danger',
}

// 审批弹窗
const approveDialogVisible = ref(false)
const approveFormRef = ref<FormInstance>()
const approveForm = reactive({
  logId: '',
  result: 'APPROVED',
  opinion: '',
})
const approveRules = {
  result: [{ required: true, message: '请选择审批结果', trigger: 'change' }],
}

async function loadPending() {
  pendingLoading.value = true
  try {
    const res: any = await approvalApi.pending()
    pendingData.value = res || []
  } finally {
    pendingLoading.value = false
  }
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const res: any = await approvalApi.history()
    historyData.value = res || []
  } finally {
    historyLoading.value = false
  }
}

async function loadAll() {
  allLoading.value = true
  try {
    const res: any = await approvalApi.page(allQuery)
    allData.value = res.list || []
    allTotal.value = res.total || 0
  } finally {
    allLoading.value = false
  }
}

function handleTabChange(tab: string | number) {
  if (tab === 'pending') loadPending()
  else if (tab === 'history') loadHistory()
  else if (tab === 'all') loadAll()
}

function handleAllSearch() {
  allQuery.pageNum = 1
  loadAll()
}

function handleAllReset() {
  allQuery.businessType = ''
  allQuery.businessId = ''
  allQuery.result = ''
  handleAllSearch()
}

function openApprove(row: ApprovalLogDTO, defaultResult: 'APPROVED' | 'REJECTED') {
  approveForm.logId = row.id!
  approveForm.result = defaultResult
  approveForm.opinion = ''
  approveDialogVisible.value = true
}

async function handleSubmitApprove() {
  if (!approveFormRef.value) return
  await approveFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await approvalApi.approve(approveForm.logId, {
        result: approveForm.result,
        opinion: approveForm.opinion,
      })
      ElMessage.success('审批提交成功')
      approveDialogVisible.value = false
      loadPending()
      if (activeTab.value === 'history') loadHistory()
      if (activeTab.value === 'all') loadAll()
    } catch {}
  })
}

onMounted(() => {
  loadPending()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>审批中心</h1>
    </div>

    <div class="card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 待审批 -->
        <el-tab-pane label="待审批" name="pending">
          <el-table v-loading="pendingLoading" :data="pendingData" row-key="id" style="width: 100%">
            <el-table-column label="业务类型" width="140">
              <template #default="{ row }">
                <el-tag :type="businessTypeTag[row.businessType] || 'info'" size="small" effect="light">
                  {{ businessTypeMap[row.businessType] || row.businessType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="businessId" label="业务ID" min-width="160" />
            <el-table-column prop="nodeOrder" label="节点序号" width="100" />
            <el-table-column prop="approverId" label="审批人ID" min-width="140" />
            <el-table-column prop="createTime" label="创建时间" min-width="160" />
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button link type="success" size="small" @click="openApprove(row as ApprovalLogDTO, 'APPROVED')">审批</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 已审批 -->
        <el-tab-pane label="已审批" name="history">
          <el-table v-loading="historyLoading" :data="historyData" row-key="id" style="width: 100%">
            <el-table-column label="业务类型" width="140">
              <template #default="{ row }">
                <el-tag :type="businessTypeTag[row.businessType] || 'info'" size="small" effect="light">
                  {{ businessTypeMap[row.businessType] || row.businessType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="businessId" label="业务ID" min-width="160" />
            <el-table-column prop="nodeOrder" label="节点序号" width="100" />
            <el-table-column label="结果" width="100">
              <template #default="{ row }">
                <el-tag :type="resultTag[row.result] || 'info'" size="small" effect="light">
                  {{ resultMap[row.result] || row.result || '-' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="opinion" label="审批意见" min-width="200" show-overflow-tooltip />
            <el-table-column prop="approveTime" label="审批时间" min-width="160" />
          </el-table>
        </el-tab-pane>

        <!-- 全部 -->
        <el-tab-pane label="全部" name="all">
          <div class="search-bar-inline">
            <el-form inline>
              <el-form-item label="业务类型">
                <el-select
                  v-model="allQuery.businessType"
                  placeholder="全部"
                  clearable
                  filterable
                  style="width: 180px"
                >
                  <el-option
                    v-for="(label, key) in businessTypeMap"
                    :key="key"
                    :label="label"
                    :value="key"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="业务ID">
                <el-input
                  v-model="allQuery.businessId"
                  placeholder="业务ID"
                  clearable
                  style="width: 200px"
                  @keyup.enter="handleAllSearch"
                />
              </el-form-item>
              <el-form-item label="结果">
                <el-select v-model="allQuery.result" placeholder="全部" clearable style="width: 140px">
                  <el-option v-for="(label, key) in resultMap" :key="key" :label="label" :value="key" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleAllSearch">查询</el-button>
                <el-button @click="handleAllReset">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
          <el-table v-loading="allLoading" :data="allData" row-key="id" style="width: 100%">
            <el-table-column label="业务类型" width="140">
              <template #default="{ row }">
                <el-tag :type="businessTypeTag[row.businessType] || 'info'" size="small" effect="light">
                  {{ businessTypeMap[row.businessType] || row.businessType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="businessId" label="业务ID" min-width="160" />
            <el-table-column prop="nodeOrder" label="节点序号" width="100" />
            <el-table-column label="结果" width="100">
              <template #default="{ row }">
                <el-tag :type="resultTag[row.result] || 'info'" size="small" effect="light">
                  {{ resultMap[row.result] || row.result || '-' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="opinion" label="审批意见" min-width="200" show-overflow-tooltip />
            <el-table-column prop="approveTime" label="审批时间" min-width="160" />
            <el-table-column prop="createTime" label="创建时间" min-width="160" />
          </el-table>
          <div class="pagination">
            <el-pagination
              v-model:current-page="allQuery.pageNum"
              v-model:page-size="allQuery.pageSize"
              :total="allTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="loadAll"
              @size-change="loadAll"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 审批弹窗 -->
    <el-dialog v-model="approveDialogVisible" title="审批" width="480px">
      <el-form ref="approveFormRef" :model="approveForm" :rules="approveRules" label-width="100px">
        <el-form-item label="审批结果" prop="result">
          <el-radio-group v-model="approveForm.result">
            <el-radio value="APPROVED">同意</el-radio>
            <el-radio value="REJECTED">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input
            v-model="approveForm.opinion"
            type="textarea"
            :rows="4"
            placeholder="请输入审批意见"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitApprove">提交</el-button>
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
.search-bar-inline { padding: 0 0 12px; }
.pagination { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
