<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell } from 'lucide-vue-next'
import { notificationApi, type SysNotification, type NotificationType } from '@/api/system'

const unreadCount = ref(0)
const list = ref<SysNotification[]>([])
const loading = ref(false)
const showAll = ref(false)

const typeMap: Record<NotificationType, { label: string; color: string }> = {
  SETTLEMENT: { label: '结算', color: '#3B82F6' },
  APPROVAL: { label: '审批', color: '#F59E0B' },
  OVERDUE: { label: '超期', color: '#EF4444' },
  WARRANTY: { label: '质保', color: '#10B981' },
  ACCEPTANCE: { label: '验收', color: '#8B5CF6' },
}

function typeMeta(t: NotificationType) {
  return typeMap[t] || { label: '通知', color: '#6B7280' }
}

async function loadCount() {
  try {
    const res: any = await notificationApi.unreadCount()
    unreadCount.value = Number(res) || 0
  } catch {}
}

async function loadList() {
  loading.value = true
  try {
    if (showAll.value) {
      const res: any = await notificationApi.page({ pageNum: 1, pageSize: 20 })
      list.value = res.list || []
    } else {
      const res: any = await notificationApi.unread()
      list.value = (res || []).slice(0, 5)
    }
  } catch {
  } finally {
    loading.value = false
  }
}

async function handleMarkRead(item: SysNotification) {
  if (item.isRead === 1) return
  try {
    await notificationApi.markRead(item.id)
    ElMessage.success('已标记为已读')
    await Promise.all([loadCount(), loadList()])
  } catch {}
}

async function handleMarkAllRead() {
  if (unreadCount.value === 0) {
    ElMessage.info('没有未读通知')
    return
  }
  try {
    await notificationApi.markAllRead()
    ElMessage.success('全部已读')
    await Promise.all([loadCount(), loadList()])
  } catch {}
}

function toggleShowAll() {
  showAll.value = !showAll.value
  loadList()
}

function formatTime(t?: string) {
  if (!t) return ''
  return t.replace('T', ' ').slice(0, 16)
}

onMounted(() => {
  loadCount()
})
</script>

<template>
  <el-popover placement="bottom-end" :width="380" trigger="click" @show="loadList">
    <template #reference>
      <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="bell-badge">
        <button class="bell-btn" title="消息通知">
          <Bell :size="20" />
        </button>
      </el-badge>
    </template>

    <div class="notif-panel">
      <div class="notif-header">
        <span class="notif-title">消息通知</span>
        <span v-if="unreadCount > 0" class="notif-unread">{{ unreadCount }} 条未读</span>
      </div>

      <div v-loading="loading" class="notif-list">
        <div v-if="list.length === 0 && !loading" class="notif-empty">暂无通知</div>
        <div
          v-for="item in list"
          :key="item.id"
          class="notif-item"
          :class="{ 'is-unread': item.isRead === 0 }"
          @click="handleMarkRead(item)"
        >
          <div class="notif-item-head">
            <span class="notif-item-title">{{ item.title }}</span>
            <span
              class="notif-type-tag"
              :style="{
                background: typeMeta(item.type).color + '1A',
                color: typeMeta(item.type).color,
              }"
            >
              {{ typeMeta(item.type).label }}
            </span>
          </div>
          <div class="notif-item-content">{{ item.content }}</div>
          <div class="notif-item-time">{{ formatTime(item.createTime) }}</div>
        </div>
      </div>

      <div class="notif-footer">
        <el-button link type="primary" size="small" @click="toggleShowAll">
          {{ showAll ? '只看未读' : '查看全部' }}
        </el-button>
        <el-button link type="primary" size="small" @click="handleMarkAllRead">
          全部已读
        </el-button>
      </div>
    </div>
  </el-popover>
</template>

<style scoped lang="scss">
.bell-badge {
  display: inline-flex;
  align-items: center;
}
.bell-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  color: #4B5563;
  display: flex;
  align-items: center;
  &:hover { background: #F3F4F6; color: #1F2937; }
}
.notif-panel { margin: -12px; }
.notif-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #E5E7EB;
}
.notif-title { font-size: 14px; font-weight: 600; color: #1F2937; }
.notif-unread { font-size: 12px; color: #6B7280; }
.notif-list {
  max-height: 360px;
  overflow-y: auto;
}
.notif-empty {
  padding: 40px 0;
  text-align: center;
  color: #9CA3AF;
  font-size: 13px;
}
.notif-item {
  padding: 12px 16px;
  border-bottom: 1px solid #F3F4F6;
  cursor: pointer;
  transition: background 0.15s;
  &:hover { background: #F9FAFB; }
  &.is-unread { background: #EFF6FF; &:hover { background: #DBEAFE; } }
}
.notif-item-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.notif-item-title {
  font-size: 13px;
  font-weight: 500;
  color: #1F2937;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notif-type-tag {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
  white-space: nowrap;
}
.notif-item-content {
  font-size: 12px;
  color: #6B7280;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.notif-item-time {
  font-size: 11px;
  color: #9CA3AF;
  margin-top: 4px;
}
.notif-footer {
  display: flex;
  justify-content: space-between;
  padding: 8px 16px;
  border-top: 1px solid #E5E7EB;
}
</style>
