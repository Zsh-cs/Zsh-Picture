<template>
  <div class="picture-list">
    <a-table
      v-if="viewMode === 'table'"
      :columns="tableColumns"
      :data-source="dataList"
      :loading="loading"
      row-key="id"
      :row-selection="rowSelection"
      :pagination="false"
      :scroll="{ x: 900 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'thumbnailUrl'">
          <a-image :src="record.thumbnailUrl ?? record.url ?? ''" :width="100" :height="70" />
        </template>
        <template v-else-if="column.dataIndex === 'tags'">
          <a-space wrap>
            <a-tag v-for="tag in record.tags" :key="tag ?? ''">{{ tag }}</a-tag>
          </a-space>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" @click="doShare(record, $event)">分享</a-button>
            <a-button type="link" @click="doSearch(record, $event)">以图搜图</a-button>
            <a-button v-if="canEdit" type="link" @click="doEdit(record, $event)">编辑</a-button>
            <a-button v-if="canDelete" type="link" danger @click="doDelete(record, $event)">
              删除
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 图片列表 -->
    <a-list
      v-else
      :grid="{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 4, xl: 5, xxl: 6 }"
      :data-source="dataList"
      :loading="loading"
    >
      <template #renderItem="{ item: picture }">
        <a-list-item style="padding: 0">
          <!-- 单张图片 -->
          <a-card hoverable @click="doClickPicture(picture)">
            <template #cover>
              <img
                :alt="picture.name"
                :src="picture.thumbnailUrl ?? picture.url"
                style="height: 180px; object-fit: cover"
              />
            </template>
            <a-card-meta :title="picture.name">
              <template #description>
                <a-flex>
                  <a-tag color="green">
                    {{ picture.category ?? '默认' }}
                  </a-tag>
                  <a-tag v-for="tag in picture.tags" :key="tag ?? ''">
                    {{ tag }}
                  </a-tag>
                </a-flex>
              </template>
            </a-card-meta>
            <template v-if="showOp" #actions>
              <ShareAltOutlined @click="(e) => doShare(picture, e)" />
              <SearchOutlined @click="(e) => doSearch(picture, e)" />
              <EditOutlined v-if="canEdit" @click="(e) => doEdit(picture, e)" />
              <DeleteOutlined v-if="canDelete" @click="(e) => doDelete(picture, e)" />
            </template>
          </a-card>
        </a-list-item>
      </template>
    </a-list>
    <ShareModal ref="shareModalRef" :link="shareLink" />
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import {
  DeleteOutlined,
  EditOutlined,
  SearchOutlined,
  ShareAltOutlined,
} from '@ant-design/icons-vue'
import { deletePictureUsingPost } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import ShareModal from '@/components/ShareModal.vue'
import { computed, ref, watch } from 'vue'

interface Props {
  dataList?: API.PictureVO[]
  loading?: boolean
  showOp?: boolean
  canEdit?: boolean
  canDelete?: boolean
  onReload?: () => void
  viewMode?: 'card' | 'table'
  selectedRowKeys?: (string | number)[]
}

const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
  showOp: false,
  canEdit: false,
  canDelete: false,
  viewMode: 'card',
  selectedRowKeys: () => [],
})

const emit = defineEmits<{
  selectionChange: [selectedRowKeys: (string | number)[], selectedRows: API.PictureVO[]]
}>()

const tableColumns = [
  { title: '图片', dataIndex: 'thumbnailUrl', width: 130 },
  { title: '名称', dataIndex: 'name', width: 160, ellipsis: true },
  { title: '分类', dataIndex: 'category', width: 100 },
  { title: '标签', dataIndex: 'tags', width: 180 },
  { title: '操作', key: 'action', width: 320 },
]

// Keep the checkbox responsive even while the parent synchronizes selections across pages.
const internalSelectedRowKeys = ref<(string | number)[]>([...props.selectedRowKeys])

watch(
  () => props.selectedRowKeys,
  (selectedRowKeys) => {
    internalSelectedRowKeys.value = [...selectedRowKeys]
  },
  { deep: true },
)

const handleRowSelectionChange = (selectedRowKeys: (string | number)[], selectedRows: API.PictureVO[]) => {
  internalSelectedRowKeys.value = [...selectedRowKeys]
  emit('selectionChange', selectedRowKeys, selectedRows.filter(Boolean))
}

const rowSelection = computed(() => ({
  selectedRowKeys: internalSelectedRowKeys.value,
  preserveSelectedRowKeys: true,
  onChange: handleRowSelectionChange,
}))

const router = useRouter()
// 跳转至图片详情页
const doClickPicture = (picture: API.PictureVO) => {
  router.push({
    path: `/picture/${picture.id}`,
  })
}

// 搜索
const doSearch = (picture: API.PictureVO, e: Event) => {
  // 阻止冒泡
  e.stopPropagation()
  // 打开新的页面
  window.open(`/search_picture?pictureId=${picture.id}`)
}

// 编辑
const doEdit = (picture: API.PictureVO, e: Event) => {
  // 阻止冒泡
  e.stopPropagation()
  // 跳转时一定要携带 spaceId
  router.push({
    path: '/add_picture',
    query: {
      id: picture.id,
      spaceId: picture.spaceId,
    },
  })
}

// 删除数据
const doDelete = async (picture: API.PictureVO, e: Event) => {
  // 阻止冒泡
  e.stopPropagation()
  const id = picture.id
  if (!id) {
    return
  }
  const res = await deletePictureUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    props.onReload?.()
  } else {
    message.error('删除失败')
  }
}

// ----- 分享操作 ----
const shareModalRef = ref()
// 分享链接
const shareLink = ref<string>()
// 分享
const doShare = (picture: API.PictureVO, e: Event) => {
  // 阻止冒泡
  e.stopPropagation()
  shareLink.value = `${window.location.protocol}//${window.location.host}/picture/${picture.id}`
  if (shareModalRef.value) {
    shareModalRef.value.openModal()
  }
}
</script>

<style scoped></style>
