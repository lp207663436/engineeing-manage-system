<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { projectApi, quoteVersionApi, type QuoteCompareVO, type QuoteVersionDTO } from '@/api/business'

interface Option { label: string; value: string }

const projectOptions = ref<Option[]>([])
const versionOptions = ref<Option[]>([])
const loading = ref(false)
const compareResult = ref<QuoteCompareVO | null>(null)

const form = reactive({
  projectId: '',
  v1Id: '',
  v2Id: '',
})

async function loadProjects() {
  try {
    const res: any = await projectApi.page({ pageNum: 1, pageSize: 200, name: undefined as any, type: undefined as any, status: undefined as any })
    const list = res.list || []
    projectOptions.value = list.map((p: any) => ({ label: `${p.code} - ${p.name}`, value: p.id }))
  } catch {}
}

async function loadVersions() {
  versionOptions.value = []
  form.v1Id = ''
  form.v2Id = ''
  compareResult.value = null
  if (!form.projectId) return
  try {
    const res: any = await quoteVersionApi.versions(form.projectId)
    const list: QuoteVersionDTO[] = res || []
    versionOptions.value = list.map((v) => ({
      label: `v${v.version} - ${v.code} (${v.amount ?? 0})`,
      value: v.id,
    }))
  } catch (e: any) {
    ElMessage.error(e?.message || '加载版本失败')
  }
}

async function handleCompare() {
  if (!form.projectId || !form.v1Id || !form.v2Id) {
    ElMessage.warning('请选择项目和两个版本')
    return
  }
  if (form.v1Id === form.v2Id) {
    ElMessage.warning('请选择两个不同的版本')
    return
  }
  loading.value = true
  try {
    const res: any = await quoteVersionApi.compare(form.projectId, form.v1Id, form.v2Id)
    compareResult.value = res as QuoteCompareVO
  } catch (e: any) {
    ElMessage.error(e?.message || '对比失败')
  } finally {
    loading.value = false
  }
}

const amountDiffClass = computed(() => {
  const diff = compareResult.value?.amountDiff ?? 0
  if (diff > 0) return 'diff-up'
  if (diff < 0) return 'diff-down'
  return 'diff-zero'
})

const amountDiffText = computed(() => {
  const diff = compareResult.value?.amountDiff ?? 0
  if (diff === 0) return '无变化'
  const sign = diff > 0 ? '+' : ''
  return `${sign}${diff.toFixed(2)}`
})

onMounted(() => {
  loadProjects()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>报价版本对比</h1>
      <p class="page-sub">选择项目下两个报价版本,对比金额、有效期、摘要差异</p>
    </div>

    <div class="card">
      <el-form inline>
        <el-form-item label="项目">
          <el-select
            v-model="form.projectId"
            placeholder="选择项目"
            filterable
            clearable
            style="width: 280px"
            @change="loadVersions"
          >
            <el-option v-for="opt in projectOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本1">
          <el-select v-model="form.v1Id" placeholder="选择版本" filterable clearable style="width: 220px" :disabled="!form.projectId">
            <el-option v-for="opt in versionOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本2">
          <el-select v-model="form.v2Id" placeholder="选择版本" filterable clearable style="width: 220px" :disabled="!form.projectId">
            <el-option v-for="opt in versionOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleCompare">对比</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-if="compareResult" v-loading="loading" class="card compare-result">
      <!-- 差异摘要 -->
      <div class="diff-summary">
        <div class="diff-item">
          <div class="diff-label">金额差异</div>
          <div class="diff-value" :class="amountDiffClass">{{ amountDiffText }}</div>
        </div>
        <div class="diff-item">
          <div class="diff-label">有效期差异</div>
          <div class="diff-value">{{ compareResult.validUntilDiff || '无变化' }}</div>
        </div>
        <div class="diff-item">
          <div class="diff-label">摘要变化</div>
          <div class="diff-value">
            <el-tag size="small" :type="compareResult.summaryDiff ? 'warning' : 'info'">
              {{ compareResult.summaryDiff ? '已变化' : '无变化' }}
            </el-tag>
          </div>
        </div>
      </div>

      <!-- 左右分栏 -->
      <div class="compare-grid">
        <div class="version-card v1">
          <div class="version-title">版本1 (v{{ compareResult.v1?.version ?? '-' }})</div>
          <div class="version-row"><span class="row-label">编号:</span>{{ compareResult.v1?.code || '-' }}</div>
          <div class="version-row"><span class="row-label">金额:</span>¥{{ compareResult.v1?.amount ?? 0 }}</div>
          <div class="version-row"><span class="row-label">报价日期:</span>{{ compareResult.v1?.quoteDate || '-' }}</div>
          <div class="version-row"><span class="row-label">有效期至:</span>{{ compareResult.v1?.validUntil || '-' }}</div>
          <div class="version-row"><span class="row-label">报价人:</span>{{ compareResult.v1?.quotePerson || '-' }}</div>
          <div class="version-row"><span class="row-label">客户:</span>{{ compareResult.v1?.customerName || '-' }}</div>
          <div class="version-row"><span class="row-label">状态:</span>{{ compareResult.v1?.status || '-' }}</div>
          <div class="version-row summary-row"><span class="row-label">摘要:</span>{{ compareResult.v1?.summary || '-' }}</div>
        </div>
        <div class="version-card v2">
          <div class="version-title">版本2 (v{{ compareResult.v2?.version ?? '-' }})</div>
          <div class="version-row"><span class="row-label">编号:</span>{{ compareResult.v2?.code || '-' }}</div>
          <div class="version-row"><span class="row-label">金额:</span>¥{{ compareResult.v2?.amount ?? 0 }}</div>
          <div class="version-row"><span class="row-label">报价日期:</span>{{ compareResult.v2?.quoteDate || '-' }}</div>
          <div class="version-row"><span class="row-label">有效期至:</span>{{ compareResult.v2?.validUntil || '-' }}</div>
          <div class="version-row"><span class="row-label">报价人:</span>{{ compareResult.v2?.quotePerson || '-' }}</div>
          <div class="version-row"><span class="row-label">客户:</span>{{ compareResult.v2?.customerName || '-' }}</div>
          <div class="version-row"><span class="row-label">状态:</span>{{ compareResult.v2?.status || '-' }}</div>
          <div class="version-row summary-row"><span class="row-label">摘要:</span>{{ compareResult.v2?.summary || '-' }}</div>
        </div>
      </div>
    </div>

    <div v-else class="card empty-card">
      <div class="empty-text">请选择项目与版本后点击「对比」</div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.page { padding: 24px; }
.page-header {
  margin-bottom: 16px;
  h1 { font-size: 20px; font-weight: 600; color: #1F2937; }
  .page-sub { font-size: 13px; color: #6B7280; margin-top: 4px; }
}
.card {
  background: #fff;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
}
.empty-card { padding: 60px 20px; }
.empty-text {
  text-align: center;
  font-size: 14px;
  color: #9CA3AF;
}
.diff-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px dashed #E5E7EB;
}
.diff-item {
  background: #F9FAFB;
  border-radius: 6px;
  padding: 16px;
  text-align: center;
  .diff-label { font-size: 12px; color: #6B7280; margin-bottom: 8px; }
  .diff-value { font-size: 18px; font-weight: 600; color: #1F2937; }
  .diff-up { color: #EF4444; }
  .diff-down { color: #10B981; }
  .diff-zero { color: #6B7280; }
}
.compare-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.version-card {
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  padding: 20px;
  background: #FCFCFD;
  &.v1 { border-left: 4px solid #4F6BED; }
  &.v2 { border-left: 4px solid #10B981; }
}
.version-title {
  font-size: 15px;
  font-weight: 600;
  color: #1F2937;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #F3F4F6;
}
.version-row {
  font-size: 13px;
  color: #4B5563;
  margin-bottom: 10px;
  .row-label {
    display: inline-block;
    width: 80px;
    color: #9CA3AF;
    margin-right: 8px;
  }
  &.summary-row {
    display: flex;
    .row-label { flex-shrink: 0; }
  }
}
</style>
