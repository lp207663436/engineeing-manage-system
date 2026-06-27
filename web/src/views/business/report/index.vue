<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { reportApi } from '@/api/business'

interface ReportItem {
  key: string
  name: string
  desc: string
  icon: string
  color: string
  export: () => Promise<Blob>
}

const reports: ReportItem[] = [
  {
    key: 'project',
    name: '项目台账',
    desc: '导出全部项目基本信息、客户、负责人、状态等台账数据',
    icon: 'FolderKanban',
    color: '#4F6BED',
    export: reportApi.exportProject,
  },
  {
    key: 'contract-payment',
    name: '合同收付款',
    desc: '导出合同计划与实际收付款明细、发票号、对账数据',
    icon: 'Banknote',
    color: '#10B981',
    export: reportApi.exportContractPayment,
  },
  {
    key: 'progress',
    name: '进度执行',
    desc: '导出项目节点进度、计划与实际工期、完成率等执行数据',
    icon: 'ListTree',
    color: '#F59E0B',
    export: reportApi.exportProgress,
  },
  {
    key: 'maintenance-workload',
    name: '维保工作量',
    desc: '导出维保任务处理人工作量、完工数量、响应时长统计',
    icon: 'Wrench',
    color: '#8B5CF6',
    export: reportApi.exportMaintenanceWorkload,
  },
  {
    key: 'settlement',
    name: '双线结算',
    desc: '导出点位结算与季度结算双线结算单、开票与到账数据',
    icon: 'Receipt',
    color: '#EF4444',
    export: reportApi.exportSettlement,
  },
]

const loadingKey = ref<string>('')

function triggerDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

function buildFilename(name: string) {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const stamp = `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}_${pad(d.getHours())}${pad(d.getMinutes())}`
  return `${name}_${stamp}.xlsx`
}

async function handleExport(item: ReportItem) {
  loadingKey.value = item.key
  try {
    const blob = await item.export()
    triggerDownload(blob, buildFilename(item.name))
    ElMessage.success(`${item.name}导出成功`)
  } catch (e: any) {
    ElMessage.error(e?.message || `${item.name}导出失败`)
  } finally {
    loadingKey.value = ''
  }
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>报表中心</h1>
      <p class="page-sub">选择报表类型,一键导出 Excel 文件</p>
    </div>

    <div class="report-grid">
      <el-card
        v-for="item in reports"
        :key="item.key"
        class="report-card"
        shadow="hover"
        :body-style="{ padding: '24px' }"
      >
        <div class="card-top">
          <div class="card-icon" :style="{ background: item.color + '1A', color: item.color }">
            {{ item.name.charAt(0) }}
          </div>
          <div class="card-title">{{ item.name }}</div>
        </div>
        <div class="card-desc">{{ item.desc }}</div>
        <div class="card-footer">
          <el-button
            type="primary"
            :loading="loadingKey === item.key"
            @click="handleExport(item)"
          >
            导出 Excel
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped lang="scss">
.page { padding: 24px; }
.page-header {
  margin-bottom: 20px;
  h1 { font-size: 20px; font-weight: 600; color: #1F2937; }
  .page-sub { font-size: 13px; color: #6B7280; margin-top: 4px; }
}
.report-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}
.report-card {
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
  &:hover { transform: translateY(-2px); }
}
.card-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
}
.card-title { font-size: 16px; font-weight: 600; color: #1F2937; }
.card-desc {
  font-size: 13px;
  color: #6B7280;
  line-height: 1.6;
  min-height: 42px;
  margin-bottom: 16px;
}
.card-footer { display: flex; justify-content: flex-end; }
</style>
