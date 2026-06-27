<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { workbenchApi, type WorkbenchVO } from '@/api/business'
import {
  ClipboardCheck,
  CheckSquare,
  AlertCircle,
  Wrench,
  FileSignature,
  Receipt,
} from 'lucide-vue-next'

const router = useRouter()
const userStore = useUserStore()

const summary = ref<WorkbenchVO>({
  pendingApprovals: 0,
  pendingAcceptances: 0,
  overdueAcceptances: 0,
  overdueTasks: 0,
  expiringContracts: 0,
  pendingSettlements: 0,
})
const loading = ref(false)

interface StatCard {
  key: keyof WorkbenchVO
  title: string
  color: string
  path: string
  icon: typeof ClipboardCheck
}

const cards: StatCard[] = [
  { key: 'pendingApprovals', title: '待审批', color: '#3B82F6', path: '/business/approval', icon: ClipboardCheck },
  { key: 'pendingAcceptances', title: '待验收', color: '#06B6D4', path: '/business/acceptance', icon: CheckSquare },
  { key: 'overdueAcceptances', title: '超期验收', color: '#EF4444', path: '/business/acceptance', icon: AlertCircle },
  { key: 'overdueTasks', title: '超期任务', color: '#F59E0B', path: '/business/maintenance-task', icon: Wrench },
  { key: 'expiringContracts', title: '即将到期合同', color: '#EAB308', path: '/business/maintenance-contract', icon: FileSignature },
  { key: 'pendingSettlements', title: '待回款结算单', color: '#8B5CF6', path: '/business/quarterly-settlement', icon: Receipt },
]

function go(path: string) {
  router.push(path)
}

async function loadSummary() {
  loading.value = true
  try {
    const res: any = await workbenchApi.summary()
    if (res) {
      Object.assign(summary.value, res)
    }
  } catch {
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadSummary()
})
</script>

<template>
  <div v-loading="loading" class="page workbench">
    <div class="welcome">
      <h1>欢迎回来,{{ userStore.name || userStore.username || '用户' }}</h1>
      <p>以下是您当前的待办事项概览</p>
    </div>

    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.key" :span="8">
        <el-card class="stat-card" shadow="hover" @click="go(card.path)">
          <div class="stat-inner">
            <div class="stat-icon" :style="{ background: card.color + '1A', color: card.color }">
              <component :is="card.icon" :size="24" />
            </div>
            <div class="stat-body">
              <div class="stat-value" :style="{ color: card.color }">{{ summary[card.key] }}</div>
              <div class="stat-title">{{ card.title }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped lang="scss">
.workbench { padding: 24px; }
.welcome {
  margin-bottom: 20px;
  h1 { font-size: 22px; font-weight: 600; color: #1F2937; margin: 0 0 4px; }
  p { font-size: 13px; color: #6B7280; margin: 0; }
}
.stat-card {
  margin-bottom: 16px;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
  &:hover { transform: translateY(-2px); }
  :deep(.el-card__body) { padding: 20px; }
}
.stat-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}
.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-body { flex: 1; }
.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}
.stat-title {
  font-size: 13px;
  color: #6B7280;
  margin-top: 2px;
}
</style>
