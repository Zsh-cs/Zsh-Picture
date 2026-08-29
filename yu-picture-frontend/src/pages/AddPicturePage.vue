<template>
  <div id="addPicturePage">
    <h2 style="margin-bottom: 16px">
      {{ route.query?.id ? '修改图片' : '创建图片' }}
    </h2>
    <a-typography-paragraph v-if="spaceId" type="secondary">
      保存至空间：<a :href="`/space/${spaceId}`" target="_blank">{{ spaceId }}</a>
    </a-typography-paragraph>
    <!-- 选择上传方式 -->
    <a-tabs v-model:activeKey="uploadType">
      <a-tab-pane key="file" tab="文件上传">
        <!-- 图片上传组件 -->
        <PictureUpload
          :picture="pictureUploadType === 'file' ? picture : undefined"
          :spaceId="spaceId"
          :onSuccess="(newPicture) => onSuccess(newPicture, 'file')"
        />
      </a-tab-pane>
      <a-tab-pane key="url" tab="URL 上传" force-render>
        <!-- URL 图片上传组件 -->
        <UrlPictureUpload
          ref="urlPictureUploadRef"
          :picture="pictureUploadType === 'url' ? picture : undefined"
          :spaceId="spaceId"
          :onSuccess="(newPicture) => onSuccess(newPicture, 'url')"
        />
      </a-tab-pane>
      <a-tab-pane key="ai" tab="AI 文生图">
        <AIText2Image
          v-if="!isAiResultPage"
          :onApplyResult="applyAiResultByUrl"
        />
      </a-tab-pane>
    </a-tabs>
    <div v-if="isAiResultPage && picture && pictureUploadType === uploadType" class="ai-result-preview">
      <img :src="picture.url" :alt="picture.name || 'AI生成图片'" />
    </div>
    <!-- 图片编辑 -->
    <div v-if="picture && pictureUploadType === uploadType" class="edit-bar">
      <a-space size="middle">
        <a-button :icon="h(EditOutlined)" @click="doEditPicture">编辑图片</a-button>
        <a-button type="primary" :icon="h(FullscreenOutlined)" @click="doImagePainting">
          AI 扩图
        </a-button>
      </a-space>
      <ImageCropper
        ref="imageCropperRef"
        :imageUrl="picture?.url"
        :picture="picture"
        :spaceId="spaceId"
        :space="space"
        :onSuccess="onCropSuccess"
      />
      <ImageOutPainting
        ref="imageOutPaintingRef"
        :picture="picture"
        :spaceId="spaceId"
        :onSuccess="onImageOutPaintingSuccess"
      />
    </div>
    <!-- 图片信息表单 -->
    <a-form
      v-if="picture && pictureUploadType === uploadType"
      name="pictureForm"
      layout="vertical"
      :model="pictureForm"
      @finish="handleSubmit"
    >
      <a-form-item name="name" label="名称">
        <a-input v-model:value="pictureForm.name" placeholder="请输入名称" allow-clear />
      </a-form-item>
      <a-form-item name="introduction" label="简介">
        <a-textarea
          v-model:value="pictureForm.introduction"
          placeholder="请输入简介"
          :auto-size="{ minRows: 2, maxRows: 5 }"
          allow-clear
        />
      </a-form-item>
      <a-form-item name="category" label="分类">
        <a-auto-complete
          v-model:value="pictureForm.category"
          placeholder="请输入分类"
          :options="categoryOptions"
          allow-clear
        />
      </a-form-item>
      <a-form-item name="tags" label="标签">
        <a-select
          v-model:value="pictureForm.tags"
          mode="tags"
          placeholder="请输入标签"
          :options="tagOptions"
          allow-clear
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">创建</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import PictureUpload from '@/components/PictureUpload.vue'
import { computed, h, onMounted, reactive, ref, watch, watchEffect } from 'vue'
import { message } from 'ant-design-vue'
import {
  editPictureUsingPost,
  getPictureVoByIdUsingGet,
  listPictureTagCategoryUsingGet,
} from '@/api/pictureController.ts'
import { useRoute, useRouter } from 'vue-router'
import UrlPictureUpload from '@/components/UrlPictureUpload.vue'
import AIText2Image from '@/components/AIText2Image.vue'
import ImageCropper from '@/components/ImageCropper.vue'
import { EditOutlined, FullscreenOutlined } from '@ant-design/icons-vue'
import ImageOutPainting from '@/components/ImageOutPainting.vue'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController.ts'

const router = useRouter()
const route = useRoute()

const picture = ref<API.PictureVO>()
const pictureForm = reactive<API.PictureEditRequest>({})
type UploadType = 'file' | 'url' | 'ai'
const uploadType = ref<UploadType>('file')
const pictureUploadType = ref<UploadType>()
const urlPictureUploadRef = ref<{ uploadByUrl: (url: string) => Promise<boolean> }>()
const isAiResultPage = computed(() => route.query?.resultOnly === 'true')
// 空间 id
const spaceId = computed(() => {
  return route.query?.spaceId
})

/**
 * 图片上传成功
 * @param newPicture
 */
const onSuccess = (newPicture: API.PictureVO, source: UploadType) => {
  picture.value = newPicture
  pictureUploadType.value = source
  pictureForm.name = newPicture.name
}

const applyAiResultByUrl = async (url: string): Promise<boolean> => {
  if (!urlPictureUploadRef.value) {
    message.error('URL 图片上传组件未初始化')
    return false
  }
  const uploaded = await urlPictureUploadRef.value.uploadByUrl(url)
  if (!uploaded || !picture.value?.id) {
    return false
  }
  const resultPage = router.resolve({
    path: '/add_picture/ai',
    query: {
      ...route.query,
      id: picture.value.id.toString(),
      uploadType: 'ai',
      resultOnly: 'true',
    },
  })
  const resultWindow = window.open(resultPage.href, '_blank')
  if (!resultWindow) {
    message.error('浏览器拦截了新标签页，请允许弹出窗口后重试')
    return false
  }

  const initialAiPage = router.resolve({
    path: '/add_picture/ai',
    query: {
      spaceId: route.query.spaceId,
      uploadType: 'ai',
    },
  })
  window.location.assign(initialAiPage.href)
  return true
}

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  console.log(values)
  const pictureId = picture.value.id
  if (!pictureId) {
    return
  }
  const res = await editPictureUsingPost({
    id: pictureId,
    spaceId: spaceId.value,
    ...values,
  })
  // 操作成功
  if (res.data.code === 0 && res.data.data) {
    message.success('创建成功')
    // 跳转到图片详情页
    router.push({
      path: `/picture/${pictureId}`,
    })
  } else {
    message.error('创建失败，' + res.data.message)
  }
}

const categoryOptions = ref<string[]>([])
const tagOptions = ref<string[]>([])

/**
 * 获取标签和分类选项
 * @param values
 */
const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagOptions.value = (res.data.data.tagList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
    categoryOptions.value = (res.data.data.categoryList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
  } else {
    message.error('获取标签分类列表失败，' + res.data.message)
  }
}

onMounted(() => {
  getTagCategoryOptions()
})

// 获取老数据
const getOldPicture = async () => {
  const routeUploadType = route.query?.uploadType
  let source: UploadType = 'file'
  if (routeUploadType === 'url' || routeUploadType === 'ai') {
    source = routeUploadType
  } else if (route.path === '/add_picture/ai') {
    source = 'ai'
  }
  uploadType.value = source

  // 获取到 id
  const id = route.query?.id
  if (id) {
    const res = await getPictureVoByIdUsingGet({
      id,
    })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      picture.value = data
      pictureUploadType.value = source
      pictureForm.name = data.name
      pictureForm.introduction = data.introduction
      pictureForm.category = data.category
      pictureForm.tags = data.tags
    }
  }
}

watch(
  () => route.fullPath,
  () => {
    void getOldPicture()
  },
  { immediate: true },
)

// ----- 图片编辑器引用 ------
const imageCropperRef = ref()

// 编辑图片
const doEditPicture = async () => {
  imageCropperRef.value?.openModal()
}

// 编辑成功事件
const onCropSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}

// ----- AI 扩图引用 -----
const imageOutPaintingRef = ref()

// 打开 AI 扩图弹窗
const doImagePainting = async () => {
  imageOutPaintingRef.value?.openModal()
}

// AI 扩图保存事件
const onImageOutPaintingSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}

// 获取空间信息
const space = ref<API.SpaceVO>()

// 获取空间信息
const fetchSpace = async () => {
  // 获取数据
  if (spaceId.value) {
    const res = await getSpaceVoByIdUsingGet({
      id: spaceId.value,
    })
    if (res.data.code === 0 && res.data.data) {
      space.value = res.data.data
    }
  }
}

watchEffect(() => {
  fetchSpace()
})
</script>

<style scoped>
#addPicturePage {
  max-width: 720px;
  margin: 0 auto;
}

#addPicturePage .edit-bar {
  text-align: center;
  margin: 16px 0;
}

#addPicturePage .ai-result-preview {
  margin: 16px 0;
  text-align: center;
}

#addPicturePage .ai-result-preview img {
  max-width: 100%;
  max-height: 480px;
}
</style>
