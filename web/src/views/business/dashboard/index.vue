<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { dashboardApi, projectApi, type ProjectDTO } from '@/api/business'

interface SettlementLine {
  totalAmount?: number
  settledAmount?: number
  invoicedAmount?: number
  receivedAmount?: number
}
interface DashboardVO {
  projectName?: string
  maintenanceLine?: SettlementLine
  pointLine?: SettlementLine
  totalReceivedAmount?: number
}

const loading = ref(false)
const projectOptions = ref<ProjectDTO[]>([])
const projectId = ref<string>('')
const dashboard = ref<DashboardVO>({})

// 未收金额 = 总额 - 已回款
const maintenanceUncollected = computed(() => {
  const line = dashboard.value.maintenanceLine
  if (!line) return 0
  return (line.totalAmount || 0) - (line.receivedAmount || 0)
})
const pointUncollected = computed(() => {
  const line = dashboard.value.pointLine
  if (!line) return 0
  return (line.totalAmount || 0) - (line.receivedAmount || 0)
})
// 总未收金额
const totalUncollected = computed(() => maintenanceUncollected.value + pointUncollected.value)
// 逾期金额(已开票但未回款的部分,简化估算)
const maintenanceOverdue = computed(() => {
  const line = dashboard.value.maintenanceLine
  if (!line) return 0
  return Math.max(0, (line.invoicedAmount || 0) - (line.receivedAmount || 0))
})
const pointOverdue = computed(() => {
  const line = dashboard.value.pointLine
  if (!line) return 0
  return Math.max(0, (line.invoicedAmount || 0) - (line.receivedAmount || 0))
})
const totalOverdue = computed(() => maintenanceOverdue.value + pointOverdue.value)

async function loadProjects() {
  try {
    const res: any = await projectApi.page({ pageNum: 1, pageSize: 100 })
    projectOptions.value = res.list || []
    if (projectOptions.value.length > 0) {
      projectId.value = projectOptions.value[0].id!
      loadDashboard()
    }
  } catch {}
}

async function loadDashboard() {
  if (!projectId.value) return
  loading.value = true
  try {
    const res: any = await dashboardApi.projectDashboard(projectId.value)
    dashboard.value = res || {}
  } catch {
    dashboard.value = {}
  } finally {
    loading.value = false
  }
}

function formatAmount(v?: number) {
  return v != null ? `¥${Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}` : '¥0.00'
}

onMounted(() => {
  loadProjects()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>结算看板</h1>
      <div class="header-extra">
        <el-select
          v-model="projectId"
          placeholder="请选择项目"
          filterable
          style="width: 320px"
          @change="loadDashboard"
        >
          <el-option v-for="p in projectOptions" :key="p.id" :label="p.name" :value="p.id!" />
        </el-select>
      </div>
    </div>

    <div v-loading="loading" class="dashboard-wrap">
      <template v-if="projectId">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card shadow="never" class="metric-card">
              <div class="card-title">维保费结算线</div>
              <div class="card-sub">{{ dashboard.projectName || '当前项目' }}</div>
              <el-row :gutter="16" class="metric-row">
                <el-col :span="6">
                  <div class="metric">
                    <div class="metric-label">总额</div>
                    <div class="metric-value">{{ formatAmount(dashboard.maintenanceLine?.totalAmount) }}</div>
                  </div>
                </el-col>
                <el-col :span="6">
                  <div class="metric">
                    <div class="metric-label">已结算</div>
                    <div class="metric-value">{{ formatAmount(dashboard.maintenanceLine?.settledAmount) }}</div>
                  </div>
                </el-col>
                <el-col :span="6">
                  <div class="metric">
                    <div class="metric-label">已开票</div>
                    <div class="metric-value">{{ formatAmount(dashboard.maintenanceLine?.invoicedAmount) }}</div>
                  </div>
                </el-col>
                <el-col :span="6">
                  <div class="metric">
                    <div class="metric-label">已回款</div>
                    <div class="metric-value accent">{{ formatAmount(dashboard.maintenanceLine?.receivedAmount) }}</div>
                  </div>
                </el-col>
              </el-row>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never" class="metric-card">
              <div class="card-title">报价费结算线</div>
              <div class="card-sub">{{ dashboard.projectName || '当前项目' }}</div>
              <el-row :gutter="16" class="metric-row">
                <el-col :span="6">
                  <div class="metric">
                    <div class="metric-label">总额</div>
                    <div class="metric-value">{{ formatAmount(dashboard.pointLine?.totalAmount) }}</div>
                  </div>
                </el-col>
                <el-col :span="6">
                  <div class="metric">
                    <div class="metric-label">已结算</div>
                    <div class="metric-value">{{ formatAmount(dashboard.pointLine?.settledAmount) }}</div>
                  </div>
                </el-col>
                <el-col :span="6">
                  <div class="metric">
                    <div class="metric-label">已开票</div>
                    <div class="metric-value">{{ formatAmount(dashboard.pointLine?.invoicedAmount) }}</div>
                  </div>
                </el-col>
                <el-col :span="6">
                  <div class="metric">
                    <div class="metric-label">已回款</div>
                    <div class="metric-value accent">{{ formatAmount(dashboard.pointLine?.receivedAmount) }}</div>
                  </div>
                </el-col>
              </el-row>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-bottom: 16px;">
          <el-col :span="12">
            <el-card shadow="never" class="metric-card uncollected-card">
              <div class="card-title">未收金额</div>
              <div class="card-sub">总额 - 已回款</div>
              <el-row :gutter="16" class="metric-row">
                <el-col :span="8">
                  <div class="metric">
                    <div class="metric-label">维保费未收</div>
                    <div class="metric-value warning">{{ formatAmount(maintenanceUncollected) }}</div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="metric">
                    <div class="metric-label">报价费未收</div>
                    <div class="metric-value warning">{{ formatAmount(pointUncollected) }}</div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="metric">
                    <div class="metric-label">合计未收</div>
                    <div class="metric-value warning">{{ formatAmount(totalUncollected) }}</div>
                  </div>
                </el-col>
              </el-row>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never" class="metric-card overdue-card">
              <div class="card-title">逾期金额</div>
              <div class="card-sub">已开票未回款(估算)</div>
              <el-row :gutter="16" class="metric-row">
                <el-col :span="8">
                  <div class="metric">
                    <div class="metric-label">维保费逾期</div>
                    <div class="metric-value danger">{{ formatAmount(maintenanceOverdue) }}</div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="metric">
                    <div class="metric-label">报价费逾期</div>
                    <div class="metric-value danger">{{ formatAmount(pointOverdue) }}</div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="metric">
                    <div class="metric-label">合计逾期</div>
                    <div class="metric-value danger">{{ formatAmount(totalOverdue) }}</div>
                  </div>
                </el-col>
              </el-row>
            </el-card>
          </el-col>
        </el-row>

        <el-card shadow="never" class="metric-card summary-card">
          <div class="summary-wrap">
            <div class="summary-label">累计已回款</div>
            <div class="summary-value">{{ formatAmount(dashboard.totalReceivedAmount) }}</div>
          </div>
        </el-card>
      </template>
      <el-empty v-else description="请先选择项目" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.page { padding: 24px; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  h1 { font-size: 20px; font-weight: 600; color: #1F2937; }
}
.dashboard-wrap { min-height: 320px; }
.metric-card {
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  margin-bottom: 16px;
  :deep(.el-card__body) { padding: 20px; }
}
.card-title { font-size: 15px; font-weight: 600; color: #1F2937; }
.card-sub { font-size: 12px; color: #9CA3AF; margin-top: 4px; margin-bottom: 16px; }
.metric-row { margin-top: 4px; }
.metric {
  padding: 8px 0;
  .metric-label { font-size: 12px; color: #9CA3AF; margin-bottom: 6px; }
  .metric-value {
    font-size: 18px;
    font-weight: 600;
    color: #1F2937;
    letter-spacing: -0.01em;
    &.accent { color: #4F6BED; }
    &.warning { color: #F59E0B; }
    &.danger { color: #EF4444; }
  }
}
.summary-card {
  background: linear-gradient(135deg, #4F6BED 0%, #6D83F2 100%);
  border: none;
  :deep(.el-card__body) { padding: 24px; }
}
.summary-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  .summary-label { font-size: 14px; color: rgba(255,255,255,0.85); font-weight: 500; }
  .summary-value { font-size: 28px; font-weight: 700; color: #fff; letter-spacing: -0.01em; }
}
</style>
