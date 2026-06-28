<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  maintenanceStatApi,
  type MaintenanceStatVO,
  type FaultTypeStat,
  type EquipmentRank,
  type WorkloadStat,
  type EquipmentHealth,
} from '@/api/business'
import { userApi } from '@/api/system'

interface Option { label: string; value: string }

const loading = ref(false)
const stat = ref<MaintenanceStatVO | null>(null)
const userOptions = ref<Option[]>([])

// 根据用户 ID 查找名称
function userName(id?: string) {
  if (!id) return '-'
  return userOptions.value.find((u) => u.value === id)?.label || id
}

const faultTypeMap: Record<string, string> = {
  INSPECTION: '巡检',
  REPAIR: '故障维修',
  MAINTENANCE: '保养',
}
const faultColors: Record<string, string> = {
  INSPECTION: '#4F6BED',
  REPAIR: '#EF4444',
  MAINTENANCE: '#10B981',
}

const faultStats = computed<FaultTypeStat[]>(() => stat.value?.faultTypeStats || [])
const faultTotal = computed(() => faultStats.value.reduce((s, i) => s + (i.count || 0), 0))
const equipmentRanks = computed<EquipmentRank[]>(() => {
  const list = stat.value?.equipmentRanks || []
  return [...list].sort((a, b) => (b.faultCount || 0) - (a.faultCount || 0)).slice(0, 10)
})
const rankMax = computed(() => equipmentRanks.value.reduce((m, i) => Math.max(m, i.faultCount || 0), 1))
const workloadStats = computed<WorkloadStat[]>(() => stat.value?.workloadStats || [])
const equipmentHealth = computed<EquipmentHealth[]>(() => stat.value?.equipmentHealth || [])

const responseHours = computed(() => stat.value?.responseTimeStat?.avgResponseHours ?? 0)
const responseTotal = computed(() => stat.value?.responseTimeStat?.totalCount ?? 0)

function faultLabel(type: string) {
  return faultTypeMap[type] || type
}
function faultColor(type: string) {
  return faultColors[type] || '#6B7280'
}
function faultPercent(item: FaultTypeStat) {
  if (faultTotal.value === 0) return 0
  return Math.round(((item.count || 0) / faultTotal.value) * 1000) / 10
}
function rankWidth(item: EquipmentRank) {
  if (rankMax.value === 0) return 0
  return Math.round(((item.faultCount || 0) / rankMax.value) * 100)
}

function healthColor(score: number): string {
  if (score >= 80) return '#10B981'
  if (score >= 60) return '#F59E0B'
  return '#EF4444'
}
function healthStatus(score: number): string {
  if (score >= 80) return '健康'
  if (score >= 60) return '一般'
  return '较差'
}

function workloadRate(row: WorkloadStat) {
  if (!row.taskCount) return 0
  return Math.round(((row.completedCount || 0) / row.taskCount) * 1000) / 10
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await maintenanceStatApi.summary()
    stat.value = (res || null) as MaintenanceStatVO | null
  } catch (e: any) {
    ElMessage.error(e?.message || '加载统计数据失败')
  } finally {
    loading.value = false
  }
}

// 加载用户列表用于处理人名称展示
async function loadUsers() {
  try {
    const res: any = await userApi.page({ pageNum: 1, pageSize: 200 })
    userOptions.value = (res.list || []).map((u: any) => ({ label: u.name || u.username, value: u.id }))
  } catch {}
}

onMounted(() => {
  loadUsers()
  loadData()
})
</script>

<template>
  <div class="page" v-loading="loading">
    <div class="page-header">
      <h1>维保统计</h1>
      <el-button link type="primary" @click="loadData">刷新</el-button>
    </div>

    <!-- 顶部:响应时长卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-label">平均响应时长</div>
        <div class="stat-value">
          {{ responseHours.toFixed(2) }}
          <span class="stat-unit">小时</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-label">统计任务总数</div>
        <div class="stat-value">
          {{ responseTotal }}
          <span class="stat-unit">条</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-label">故障类型数</div>
        <div class="stat-value">
          {{ faultStats.length }}
          <span class="stat-unit">类</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-label">设备健康监测数</div>
        <div class="stat-value">
          {{ equipmentHealth.length }}
          <span class="stat-unit">台</span>
        </div>
      </div>
    </div>

    <div class="grid-2">
      <!-- 故障类型分布 -->
      <div class="card">
        <div class="card-title">故障类型分布</div>
        <div v-if="faultStats.length === 0" class="empty">暂无数据</div>
        <div v-else class="fault-list">
          <div v-for="item in faultStats" :key="item.type" class="fault-row">
            <div class="fault-head">
              <span class="fault-dot" :style="{ background: faultColor(item.type) }"></span>
              <span class="fault-name">{{ faultLabel(item.type) }}</span>
              <span class="fault-count">{{ item.count }} 次</span>
              <span class="fault-percent">{{ faultPercent(item) }}%</span>
            </div>
            <div class="fault-bar">
              <div
                class="fault-bar-inner"
                :style="{ width: faultPercent(item) + '%', background: faultColor(item.type) }"
              ></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 设备故障频次排行 -->
      <div class="card">
        <div class="card-title">设备故障频次排行(TOP10)</div>
        <div v-if="equipmentRanks.length === 0" class="empty">暂无数据</div>
        <div v-else class="rank-list">
          <div v-for="(item, idx) in equipmentRanks" :key="item.equipmentId" class="rank-row">
            <span class="rank-no" :class="{ top: idx < 3 }">{{ idx + 1 }}</span>
            <span class="rank-name" :title="item.equipmentName">{{ item.equipmentName || item.equipmentId }}</span>
            <div class="rank-bar">
              <div class="rank-bar-inner" :style="{ width: rankWidth(item) + '%' }"></div>
            </div>
            <span class="rank-count">{{ item.faultCount }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="grid-2">
      <!-- 维保人员工作量 -->
      <div class="card">
        <div class="card-title">维保人员工作量</div>
        <el-table :data="workloadStats" style="width: 100%" empty-text="暂无数据">
          <el-table-column label="处理人" min-width="120">
            <template #default="{ row }">{{ userName(row.handlerId) }}</template>
          </el-table-column>
          <el-table-column prop="taskCount" label="任务数" width="100" align="right" />
          <el-table-column prop="completedCount" label="已完成" width="100" align="right" />
          <el-table-column label="完成率" min-width="180">
            <template #default="{ row }">
              <el-progress
                :percentage="workloadRate(row as WorkloadStat)"
                :stroke-width="12"
                :color="workloadRate(row as WorkloadStat) >= 80 ? '#10B981' : workloadRate(row as WorkloadStat) >= 60 ? '#F59E0B' : '#EF4444'"
              />
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 设备健康度评分 -->
      <div class="card">
        <div class="card-title">设备健康度评分</div>
        <el-table :data="equipmentHealth" style="width: 100%" empty-text="暂无数据">
          <el-table-column label="设备名称" min-width="160">
            <template #default="{ row }">{{ row.equipmentName || row.equipmentId }}</template>
          </el-table-column>
          <el-table-column label="健康度" min-width="220">
            <template #default="{ row }">
              <el-progress
                :percentage="Math.max(0, Math.min(100, Number(row.healthScore) || 0))"
                :stroke-width="14"
                :color="healthColor(Number(row.healthScore) || 0)"
              />
            </template>
          </el-table-column>
          <el-table-column label="评分" width="90" align="right">
            <template #default="{ row }">{{ Number(row.healthScore || 0).toFixed(1) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag
                size="small"
                effect="light"
                :type="(healthColor(Number(row.healthScore) || 0) === '#10B981' ? 'success' : healthColor(Number(row.healthScore) || 0) === '#F59E0B' ? 'warning' : 'danger') as any"
              >
                {{ healthStatus(Number(row.healthScore) || 0) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.page { padding: 24px; }
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  h1 { font-size: 20px; font-weight: 600; color: #1F2937; }
}
.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}
.stat-card {
  background: #fff;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  padding: 18px 20px;
  .stat-label { font-size: 13px; color: #6B7280; margin-bottom: 8px; }
  .stat-value { font-size: 26px; font-weight: 600; color: #1F2937; }
  .stat-unit { font-size: 13px; font-weight: 400; color: #9CA3AF; margin-left: 4px; }
}
.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}
.card {
  background: #fff;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  padding: 20px;
}
.card-title { font-size: 15px; font-weight: 600; color: #1F2937; margin-bottom: 16px; }
.empty { font-size: 13px; color: #9CA3AF; text-align: center; padding: 32px 0; }

.fault-list { display: flex; flex-direction: column; gap: 14px; }
.fault-row { width: 100%; }
.fault-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 13px;
  .fault-dot { width: 8px; height: 8px; border-radius: 50%; }
  .fault-name { color: #1F2937; font-weight: 500; }
  .fault-count { color: #6B7280; }
  .fault-percent { margin-left: auto; color: #4F6BED; font-weight: 600; }
}
.fault-bar {
  height: 10px;
  background: #F3F4F6;
  border-radius: 5px;
  overflow: hidden;
}
.fault-bar-inner { height: 100%; border-radius: 5px; transition: width 0.3s ease; }

.rank-list { display: flex; flex-direction: column; gap: 10px; }
.rank-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}
.rank-no {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #F3F4F6;
  color: #6B7280;
  text-align: center;
  line-height: 20px;
  font-size: 12px;
  flex-shrink: 0;
  &.top { background: #4F6BED; color: #fff; }
}
.rank-name {
  width: 130px;
  flex-shrink: 0;
  color: #1F2937;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.rank-bar {
  flex: 1;
  height: 14px;
  background: #F3F4F6;
  border-radius: 7px;
  overflow: hidden;
}
.rank-bar-inner {
  height: 100%;
  background: linear-gradient(90deg, #4F6BED, #8B5CF6);
  border-radius: 7px;
  transition: width 0.3s ease;
}
.rank-count {
  width: 36px;
  text-align: right;
  color: #1F2937;
  font-weight: 600;
  flex-shrink: 0;
}
</style>
