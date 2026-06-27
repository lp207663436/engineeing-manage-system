<script setup lang="ts">
import { ref, watch, onBeforeUnmount, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const props = defineProps<{ attachmentId?: string; visible: boolean }>()
const emit = defineEmits<{ 'update:visible': [boolean] }>()

const userStore = useUserStore()

const loading = ref(false)
const objectUrl = ref<string>('')
const previewKind = ref<'pdf' | 'image' | 'office' | 'unknown' | ''>('')

// 水印文本:当前用户名 + 时间戳
const watermarkText = computed(() => {
  const name = userStore.name || userStore.username || '未知用户'
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const stamp = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  return `${name}  ${stamp}`
})

function revokeUrl() {
  if (objectUrl.value) {
    URL.revokeObjectURL(objectUrl.value)
    objectUrl.value = ''
  }
}

function detectKind(blobType: string): 'pdf' | 'image' | 'office' | 'unknown' {
  const t = (blobType || '').toLowerCase()
  if (t === 'application/pdf') return 'pdf'
  if (t.startsWith('image/')) return 'image'
  if (
    t.includes('word') ||
    t.includes('sheet') ||
    t.includes('powerpoint') ||
    t.includes('officedocument') ||
    t.includes('msword') ||
    t.includes('excel')
  ) {
    return 'office'
  }
  return 'unknown'
}

async function loadFile() {
  if (!props.attachmentId) return
  revokeUrl()
  previewKind.value = ''
  loading.value = true
  try {
    const token = localStorage.getItem('ems_token')
    const res = await fetch(`/api/business/preview/${props.attachmentId}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    })
    if (!res.ok) {
      const msg = await res.text().catch(() => '文件加载失败')
      throw new Error(msg || '文件加载失败')
    }
    const blob = await res.blob()
    previewKind.value = detectKind(blob.type)
    objectUrl.value = URL.createObjectURL(blob)
  } catch (e: any) {
    ElMessage.error(e?.message || '文件预览失败')
    previewKind.value = 'unknown'
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.visible, props.attachmentId],
  ([vis, id]) => {
    if (vis && id) {
      loadFile()
    } else if (!vis) {
      revokeUrl()
      previewKind.value = ''
    }
  }
)

function handleClose() {
  emit('update:visible', false)
}

onBeforeUnmount(() => {
  revokeUrl()
})
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="文件预览"
    width="80%"
    top="6vh"
    destroy-on-close
    @update:model-value="handleClose"
  >
    <div v-loading="loading" class="preview-wrap">
      <!-- 水印层 -->
      <div class="watermark-layer" aria-hidden="true">
        <span
          v-for="n in 12"
          :key="n"
          class="watermark-item"
        >{{ watermarkText }}</span>
      </div>

      <!-- 预览内容 -->
      <div class="preview-content">
        <iframe
          v-if="previewKind === 'pdf' && objectUrl"
          :src="objectUrl"
          class="preview-iframe"
          frameborder="0"
        ></iframe>
        <img
          v-else-if="previewKind === 'image' && objectUrl"
          :src="objectUrl"
          class="preview-img"
          alt="预览"
        />
        <div v-else-if="previewKind === 'office'" class="preview-tip">
          <div class="tip-icon">📄</div>
          <div class="tip-text">暂不支持在线预览,请下载后查看</div>
        </div>
        <div v-else-if="previewKind === 'unknown'" class="preview-tip">
          <div class="tip-icon">🚫</div>
          <div class="tip-text">不支持预览此文件类型</div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
.preview-wrap {
  position: relative;
  min-height: 60vh;
  background: #F9FAFB;
  border-radius: 6px;
  overflow: hidden;
}
.preview-content {
  position: relative;
  z-index: 1;
  width: 100%;
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
.preview-iframe {
  width: 100%;
  height: 70vh;
  border: none;
  background: #fff;
}
.preview-img {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
}
.preview-tip {
  text-align: center;
  padding: 60px 20px;
  .tip-icon { font-size: 48px; margin-bottom: 12px; }
  .tip-text { font-size: 14px; color: #6B7280; }
}

/* 水印层:绝对定位、半透明、旋转、不拦截事件 */
.watermark-layer {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
  display: flex;
  flex-wrap: wrap;
  align-content: space-around;
  justify-content: space-around;
  overflow: hidden;
}
.watermark-item {
  display: inline-block;
  width: 33.33%;
  text-align: center;
  font-size: 14px;
  color: rgba(31, 41, 55, 0.18);
  transform: rotate(-25deg);
  white-space: nowrap;
  padding: 28px 0;
  user-select: none;
}
</style>
