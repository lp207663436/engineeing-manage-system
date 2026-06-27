<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { progressApi, projectApi, type ProgressDTO } from '@/api/business'

interface Option { label: string; value: string }

interface GanttRow extends ProgressDTO {
  id: string
  planStart: Date
  planEnd: Date
  actualStart?: Date
  actualEnd?: Date
}

const loading = ref(false)
const projectOptions = ref<Option[]>([])
const projectId = ref<string>('')
const progressList = ref<ProgressDTO[]>([])

// 时间轴范围(天)
const timelineStart = ref<Date>(new Date())
const timelineEnd = ref<Date>(new Date())
const days = computed(() => {
  if (!timelineStart.value || !timelineEnd.value) return 0
  return Math.max(1, Math.ceil((timelineEnd.value.getTime() - timelineStart.value.getTime()) / (24 * 3600 * 1000)))
})

const statusMap: Record<string, string> = {
  PENDING: '待开始',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完工',
  OVERDUE: '延期',
}

function parseDate(s?: string): Date | undefined {
  if (!s) return undefined
  // 后端 LocalDate 序列化为 'YYYY-MM-DD'
  const d = new Date(s)
  return isNaN(d.getTime()) ? undefined : d
}

function computeRange() {
  const list = progressList.value
  if (list.length === 0) {
    const now = new Date()
    timelineStart.value = new Date(now.getFullYear(), now.getMonth(), 1)
    timelineEnd.value = new Date(now.getFullYear(), now.getMonth() + 1, 0)
    return
  }
  let minTime = Infinity
  let maxTime = -Infinity
  list.forEach((p) => {
    const ps = parseDate(p.planStartDate)
    const pe = parseDate(p.planEndDate)
    const as = parseDate(p.actualStartDate)
    const ae = parseDate(p.actualEndDate)
    ;[ps, pe, as, ae].forEach((d) => {
      if (d) {
        if (d.getTime() < minTime) minTime = d.getTime()
        if (d.getTime() > maxTime) maxTime = d.getTime()
      }
    })
  })
  if (minTime === Infinity || maxTime === -Infinity) {
    const now = new Date()
    timelineStart.value = new Date(now.getFullYear(), now.getMonth(), 1)
    timelineEnd.value = new Date(now.getFullYear(), now.getMonth() + 1, 0)
    return
  }
  // 前后留 3 天余量
  timelineStart.value = new Date(minTime - 3 * 24 * 3600 * 1000)
  timelineEnd.value = new Date(maxTime + 3 * 24 * 3600 * 1000)
}

function dayOffset(date?: Date): number {
  if (!date) return 0
  return Math.floor((date.getTime() - timelineStart.value.getTime()) / (24 * 3600 * 1000))
}

function daySpan(start?: Date, end?: Date): number {
  if (!start || !end) return 0
  return Math.max(1, Math.ceil((end.getTime() - start.getTime()) / (24 * 3600 * 1000)))
}

const timelineMarks = computed(() => {
  const marks: { label: string; offset: number }[] = []
  const total = days.value
  // 每隔 7 天打一个刻度
  const step = total > 60 ? 14 : 7
  for (let i = 0; i <= total; i += step) {
    const d = new Date(timelineStart.value.getTime() + i * 24 * 3600 * 1000)
    const pad = (n: number) => String(n).padStart(2, '0')
    marks.push({
      label: `${pad(d.getMonth() + 1)}-${pad(d.getDate())}`,
      offset: (i / total) * 100,
    })
  }
  return marks
})

const ganttRows = computed<GanttRow[]>(() => {
  return progressList.value.map((p) => ({
    ...p,
    id: p.id!,
    planStart: parseDate(p.planStartDate) || timelineStart.value,
    planEnd: parseDate(p.planEndDate) || timelineEnd.value,
    actualStart: parseDate(p.actualStartDate),
    actualEnd: parseDate(p.actualEndDate),
  }))
})

function barStyle(start?: Date, end?: Date, color = '#4F6BED'): Record<string, string> {
  if (!start || !end) return { display: 'none' }
  const offset = dayOffset(start)
  const span = daySpan(start, end)
  const total = days.value || 1
  const left = (offset / total) * 100
  const width = (span / total) * 100
  return {
    left: `${left}%`,
    width: `${Math.max(2, width)}%`,
    background: color,
  }
}

async function loadProjects() {
  try {
    const res: any = await projectApi.page({ pageNum: 1, pageSize: 1000, name: undefined as any, type: undefined as any, status: undefined as any })
    const list = res.list || []
    projectOptions.value = list.map((p: any) => ({ label: `${p.code} - ${p.name}`, value: p.id }))
    if (list.length > 0) {
      projectId.value = list[0].id
      await loadProgress()
    }
  } catch {}
}

async function loadProgress() {
  if (!projectId.value) return
  loading.value = true
  try {
    const res: any = await progressApi.page({ pageNum: 1, pageSize: 1000, projectId: projectId.value })
    progressList.value = res.list || []
    computeRange()
  } catch (e: any) {
    ElMessage.error(e?.message || '加载进度失败')
  } finally {
    loading.value = false
  }
}

async function handleProjectChange() {
  await loadProgress()
}

onMounted(() => {
  loadProjects()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>进度甘特图</h1>
      <p class="page-sub">蓝条=计划工期,绿条=实际工期</p>
    </div>

    <div class="card filter-bar">
      <el-form inline>
        <el-form-item label="项目">
          <el-select v-model="projectId" placeholder="选择项目" filterable clearable style="width: 280px" @change="handleProjectChange">
            <el-option v-for="opt in projectOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadProgress">刷新</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-loading="loading" class="card gantt-card">
      <div v-if="ganttRows.length === 0" class="empty-tip">暂无进度数据</div>
      <div v-else class="gantt">
        <!-- 时间轴表头 -->
        <div class="gantt-header">
          <div class="gantt-label-col">节点名称</div>
          <div class="gantt-timeline">
            <div
              v-for="(mark, idx) in timelineMarks"
              :key="idx"
              class="timeline-mark"
              :style="{ left: mark.offset + '%' }"
            >
              <div class="mark-line"></div>
              <div class="mark-label">{{ mark.label }}</div>
            </div>
          </div>
        </div>

        <!-- 行 -->
        <div v-for="row in ganttRows" :key="row.id" class="gantt-row">
          <div class="gantt-label-col" :title="row.nodeName">
            <div class="node-name">{{ row.nodeName }}</div>
            <div class="node-meta">{{ statusMap[row.status || ''] || row.status }} · {{ row.progressPercent || 0 }}%</div>
          </div>
          <div class="gantt-timeline">
            <!-- 计划条 -->
            <div
              class="gantt-bar plan-bar"
              :style="barStyle(row.planStart, row.planEnd, '#4F6BED')"
              :title="`计划: ${row.planStartDate} ~ ${row.planEndDate}`"
            ></div>
            <!-- 实际条 -->
            <div
              v-if="row.actualStart && row.actualEnd"
              class="gantt-bar actual-bar"
              :style="barStyle(row.actualStart, row.actualEnd, '#10B981')"
              :title="`实际: ${row.actualStartDate} ~ ${row.actualEndDate}`"
            ></div>
          </div>
        </div>
      </div>
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
.filter-bar { padding: 16px 20px 0; }
.empty-tip {
  text-align: center;
  padding: 40px 0;
  font-size: 13px;
  color: #9CA3AF;
}
.gantt {
  width: 100%;
}
.gantt-header {
  display: flex;
  align-items: center;
  border-bottom: 2px solid #E5E7EB;
  padding-bottom: 8px;
  margin-bottom: 8px;
  height: 32px;
}
.gantt-label-col {
  width: 200px;
  flex-shrink: 0;
  padding-right: 12px;
  font-size: 12px;
  font-weight: 600;
  color: #6B7280;
}
.gantt-timeline {
  flex: 1;
  position: relative;
  height: 24px;
}
.timeline-mark {
  position: absolute;
  top: 0;
  bottom: 0;
  .mark-line {
    width: 1px;
    height: 6px;
    background: #D1D5DB;
  }
  .mark-label {
    font-size: 11px;
    color: #9CA3AF;
    margin-top: 2px;
    transform: translateX(-50%);
  }
}
.gantt-row {
  display: flex;
  align-items: center;
  border-bottom: 1px solid #F3F4F6;
  min-height: 48px;
  &:hover { background: #F9FAFB; }
}
.gantt-label-col {
  width: 200px;
  flex-shrink: 0;
  padding-right: 12px;
  .node-name {
    font-size: 13px;
    color: #1F2937;
    font-weight: 500;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .node-meta {
    font-size: 11px;
    color: #9CA3AF;
    margin-top: 2px;
  }
}
.gantt-timeline {
  flex: 1;
  position: relative;
  min-height: 32px;
}
.gantt-bar {
  position: absolute;
  top: 8px;
  height: 16px;
  border-radius: 4px;
  min-width: 6px;
  cursor: pointer;
  transition: opacity 0.15s;
  &:hover { opacity: 0.85; }
}
.plan-bar {
  background: #4F6BED;
}
.actual-bar {
  background: #10B981;
  top: 14px;
  height: 10px;
  opacity: 0.85;
}
</style>
