<template>
  <div class="ai-text2-image">
    <a-form layout="vertical" :model="form">
      <a-form-item label="描述文本" required>
        <a-textarea
          v-model:value="form.text"
          :maxlength="2000"
          :auto-size="{ minRows: 4, maxRows: 8 }"
          show-count
          placeholder="描述你想生成的图片，例如：一只戴着宇航员头盔的猫，坐在月球上看地球"
        />
      </a-form-item>
      <a-form-item label="反向提示词">
        <a-textarea
          v-model:value="form.negativePrompt"
          :maxlength="500"
          :auto-size="{ minRows: 2, maxRows: 4 }"
          show-count
          placeholder="不希望出现在图片中的内容，可选"
        />
      </a-form-item>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item label="图片尺寸">
            <a-select v-model:value="form.size" style="width: 100%">
              <a-select-option v-for="option in sizeOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item label="生成数量">
            <a-select v-model:value="form.n" style="width: 100%">
              <a-select-option v-for="count in 4" :key="count" :value="count">
                {{ count }} 张
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-space direction="vertical" size="small" class="options">
        <a-checkbox v-model:checked="form.promptExtend">
          开启提示词智能改写
          <a-tooltip placement="top" overlay-class-name="prompt-extend-tooltip">
            <template #title>
              <div>开启后将使用大模型优化正向提示词，对较短提示词提升明显，但会增加约 3-4 秒耗时。</div>
              <div>默认开启，若AI文生图失败，可关闭后重试。</div>
              <div>提示词直接包含受版权保护的角色名或作品名时，关闭后仍可能失败，需要修改提示词。</div>
            </template>
            <InfoCircleOutlined class="prompt-extend-help" @click.prevent />
          </a-tooltip>
        </a-checkbox>
        <a-checkbox v-model:checked="form.watermark">添加“AI生成”水印</a-checkbox>
      </a-space>
      <a-button type="primary" html-type="button" block :loading="creating || !!taskId" @click="createTask">
        {{ taskId ? '正在生成...' : '生成图片' }}
      </a-button>
    </a-form>

    <a-alert
      v-if="taskId"
      class="task-status"
      type="info"
      show-icon
      message="图片生成中，请耐心等待"
      description="生成完成后可以选择一张应用到图库。"
    />

    <div v-if="resultUrls.length" class="results">
      <div class="results-title">生成结果</div>
      <div class="result-grid">
        <div v-for="url in resultUrls" :key="url" class="result-item">
          <img :src="url" alt="AI生成结果" />
          <a-button type="primary" size="small" :loading="uploadingUrl === url" @click="applyResult(url)">
            应用到图库
          </a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { message } from 'ant-design-vue'
import { InfoCircleOutlined } from '@ant-design/icons-vue'
import { onUnmounted, reactive, ref } from 'vue'
import {
  createText2ImageTaskUsingPost,
  queryText2ImageTaskUsingGet,
} from '@/api/pictureController.ts'

interface Props {
  onApplyResult?: (url: string) => Promise<boolean>
}

const props = defineProps<Props>()

const form = reactive<API.AIText2ImageParameters & { text: string }>({
  text: '',
  negativePrompt: '',
  size: '1280*1280',
  n: 1,
  promptExtend: true,
  watermark: false,
})
const sizeOptions = [
  { value: '1280*1280', label: '1:1（1280 × 1280）' },
  { value: '1104*1472', label: '3:4（1104 × 1472）' },
  { value: '1472*1104', label: '4:3（1472 × 1104）' },
  { value: '960*1696', label: '9:16（960 × 1696）' },
  { value: '1696*960', label: '16:9（1696 × 960）' },
]

const creating = ref(false)
const taskId = ref<string>()
const resultUrls = ref<string[]>([])
const uploadingUrl = ref<string>()
let pollingTimer: ReturnType<typeof setInterval> | undefined
let pollingCount = 0

const getTaskId = (output?: API.ImageGenerationOutput) => output?.taskId || output?.task_id
const getTaskStatus = (output?: API.ImageGenerationOutput) => output?.taskStatus || output?.task_status

const createTask = async () => {
  if (!form.text.trim()) {
    message.warning('请输入描述文本')
    return
  }
  creating.value = true
  resultUrls.value = []
  try {
    const res = await createText2ImageTaskUsingPost({
      text: form.text.trim(),
      parameters: {
        negativePrompt: form.negativePrompt?.trim() || undefined,
        size: form.size,
        n: form.n,
        promptExtend: form.promptExtend,
        watermark: form.watermark,
      },
    })
    if (res.data.code !== 0 || !res.data.data) {
      message.error('创建文生图任务失败，' + res.data.message)
      return
    }
    const newTaskId = getTaskId(res.data.data.output)
    if (!newTaskId) {
      message.error('创建文生图任务失败，未获取到任务编号')
      return
    }
    taskId.value = newTaskId
    pollingCount = 0
    startPolling()
  } catch (error) {
    message.error('创建文生图任务失败，' + getErrorMessage(error))
  } finally {
    creating.value = false
  }
}

const startPolling = () => {
  clearTimer()
  pollingTimer = setInterval(queryTask, 3000)
  void queryTask()
}

const queryTask = async () => {
  if (!taskId.value) return
  pollingCount += 1
  if (pollingCount > 60) {
    message.error('文生图任务等待超时，请稍后重试')
    finishPolling()
    return
  }
  try {
    const res = await queryText2ImageTaskUsingGet({ taskId: taskId.value })
    if (res.data.code !== 0 || !res.data.data) {
      message.error('查询文生图任务失败，' + res.data.message)
      finishPolling()
      return
    }
    const output = res.data.data.output
    const status = getTaskStatus(output)
    if (status === 'SUCCEEDED') {
      resultUrls.value = (output?.choices ?? [])
        .flatMap((choice) => choice.message?.content ?? [])
        .map((content) => content.image)
        .filter(Boolean) as string[]
      message[resultUrls.value.length ? 'success' : 'error'](
        resultUrls.value.length ? '图片生成成功，请选择要应用的结果' : '任务已完成，但未获取到图片结果'
      )
      finishPolling()
    } else if (status === 'FAILED' || status === 'CANCELED') {
      message.error('文生图任务执行失败')
      finishPolling()
    }
  } catch (error) {
    message.error('查询文生图任务失败，' + getErrorMessage(error))
    finishPolling()
  }
}

const clearTimer = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = undefined
  }
}

const finishPolling = () => {
  clearTimer()
  taskId.value = undefined
}

const applyResult = async (url: string) => {
  if (!props.onApplyResult) {
    message.error('URL 图片上传组件未初始化')
    return
  }
  uploadingUrl.value = url
  try {
    await props.onApplyResult(url)
  } catch (error) {
    message.error('图片上传失败，' + getErrorMessage(error))
  } finally {
    uploadingUrl.value = undefined
  }
}

const getErrorMessage = (error: unknown) => (error instanceof Error ? error.message : String(error))

onUnmounted(clearTimer)
</script>

<style scoped>
.ai-text2-image { margin-bottom: 16px; }
.options { margin-bottom: 20px; }
.prompt-extend-help { margin-left: 4px; color: #8c8c8c; vertical-align: super; cursor: help; }
:global(.prompt-extend-tooltip .ant-tooltip-inner) {
  width: 490px;
  max-width: calc(100vw - 32px);
  padding: 6px 10px;
  font-size: 12px;
  line-height: 1.45;
}
.task-status { margin-top: 16px; }
.results { margin-top: 24px; }
.results-title { margin-bottom: 12px; font-weight: 600; }
.result-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; }
.result-item { display: flex; flex-direction: column; gap: 8px; }
.result-item img { width: 100%; aspect-ratio: 1; object-fit: contain; border: 1px solid #f0f0f0; }
</style>
