<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { progressApi, projectApi, type ProgressDTO } from '@/api/business'

interface Option { label: string; value: string }

const loading = ref(false)
const projectOptions = ref<Option[]>([])
const projectId = ref<string>('')
const allList = ref<ProgressDTO[]>([])

const statusMap: Record<string, string> = {
  PENDING: '待开始',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完工',
  OVERDUE: '延期',
}

// 看板分列
const columns = computed(() => {
  const groups: Record<string, ProgressDTO[]> = {
    PENDING: [],
    IN_PROGRESS: [],
    OVERDUE: [],
    COMPLETED: [],
  }
  allList.value.forEach((p) => {
    const status = p.status || 'PENDING'
    if (groups[status]) {
      groups[status].push(p)
    } else {
      groups.PENDING.push(p)
    }
  })
  return [
    { key: 'PENDING', title: '待开始', color: '#6B7280', items: groups.PENDING },
    { key: 'IN_PROGRESS', title: '进行中', color: '#4F6BED', items: groups.IN_PROGRESS },
    { key: 'OVERDUE', title: '延期', color: '#EF4444', items: groups.OVERDUE },
    { key: 'COMPLETED', title: '已完工', color: '#10B981', items: groups.COMPLETED },
  ]
})

async function loadProjects() {
  try {
    const res: any = await projectApi.page({ pageNum: 1, pageSize: 200, name: undefined as any, type: undefined as any, status: undefined as any })
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
    const res: any = await progressApi.page({ pageNum: 1, pageSize: 200, projectId: projectId.value })
    allList.value = res.list || []
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
      <h1>进度看板</h1>
      <p class="page-sub">按状态分类展示进度节点(待开始/进行中/延期/已完工)</p>
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

    <div v-loading="loading" class="kanban">
      <div v-for="col in columns" :key="col.key" class="kanban-col">
        <div class="col-header">
          <span class="col-title">
            <span class="col-dot" :style="{ background: col.color }"></span>
            {{ col.title }}
          </span>
          <span class="col-count">{{ col.items.length }}</span>
        </div>
        <div class="col-body">
          <div v-if="col.items.length === 0" class="col-empty">暂无节点</div>
          <div v-for="item in col.items" :key="item.id" class="kanban-card" :style="{ borderLeftColor: col.color }">
            <div class="card-title">{{ item.nodeName }}</div>
            <div class="card-meta">
              <span class="meta-label">计划:</span>
              <span class="meta-value">{{ item.planStartDate || '-' }} ~ {{ item.planEndDate || '-' }}</span>
            </div>
            <div v-if="item.actualStartDate" class="card-meta">
              <span class="meta-label">实际:</span>
              <span class="meta-value">{{ item.actualStartDate }} ~ {{ item.actualEndDate || '进行中' }}</span>
            </div>
            <div class="card-footer">
              <span class="progress-text">{{ item.progressPercent || 0 }}%</span>
              <el-progress
                :percentage="Number(item.progressPercent) || 0"
                :stroke-width="6"
                :color="col.color"
                :show-text="false"
                style="flex: 1"
              />
            </div>
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
.kanban {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.kanban-col {
  background: #fff;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  min-height: 400px;
}
.col-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #F3F4F6;
  .col-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    font-weight: 600;
    color: #1F2937;
  }
  .col-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }
  .col-count {
    font-size: 12px;
    color: #6B7280;
    background: #F3F4F6;
    padding: 2px 8px;
    border-radius: 10px;
  }
}
.col-body {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
}
.col-empty {
  text-align: center;
  padding: 24px 0;
  font-size: 12px;
  color: #D1D5DB;
}
.kanban-card {
  background: #FCFCFD;
  border: 1px solid #E5E7EB;
  border-left: 3px solid #4F6BED;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 10px;
  transition: box-shadow 0.15s;
  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
}
.card-title {
  font-size: 13px;
  font-weight: 600;
  color: #1F2937;
  margin-bottom: 8px;
  line-height: 1.4;
}
.card-meta {
  font-size: 11px;
  color: #6B7280;
  margin-bottom: 4px;
  .meta-label { color: #9CA3AF; margin-right: 4px; }
  .meta-value { color: #4B5563; }
}
.card-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed #F3F4F6;
  .progress-text {
    font-size: 11px;
    color: #6B7280;
    width: 32px;
  }
}

@media (max-width: 1100px) {
  .kanban {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 640px) {
  .kanban {
    grid-template-columns: 1fr;
  }
}
</style>
