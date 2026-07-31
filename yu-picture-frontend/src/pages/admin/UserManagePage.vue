<template>
  <div id="userManagePage">
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="账号">
        <a-input v-model:value="searchParams.userAccount" placeholder="输入账号" allow-clear />
      </a-form-item>
      <a-form-item label="用户名">
        <a-input v-model:value="searchParams.userName" placeholder="输入用户名" allow-clear />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>
    <div style="margin-bottom: 16px" />
    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="dataList"
      :pagination="pagination"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'userAvatar'">
          <a-image v-if="editingId !== record.id" :src="record.userAvatar" :width="120" />
          <a-input v-else v-model:value="editFormData.userAvatar" placeholder="请输入头像地址" />
        </template>
        <template v-else-if="column.dataIndex === 'userRole'">
          <template v-if="editingId === record.id">
            <a-select
              v-model:value="editFormData.userRole"
              :options="USER_ROLE_OPTIONS"
              style="min-width: 120px"
              :dropdown-match-select-width="false"
            />
          </template>
          <div v-else-if="record.userRole === 'admin'">
            <a-tag color="green">管理员</a-tag>
          </div>
          <div v-else>
            <a-tag color="blue">普通用户</a-tag>
          </div>
        </template>
        <template v-else-if="column.dataIndex === 'userName'">
          <a-input v-if="editingId === record.id" v-model:value="editFormData.userName" />
          <span v-else>{{ record.userName }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'userAccount'">
          <a-input v-if="editingId === record.id" v-model:value="editFormData.userAccount" />
          <span v-else>{{ record.userAccount }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'userProfile'">
          <a-input v-if="editingId === record.id" v-model:value="editFormData.userProfile" />
          <span v-else>{{ record.userProfile }}</span>
        </template>
        <template v-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space :size="8">
            <a-button
              v-if="editingId !== record.id"
              style="color: #1677ff; border-color: #1677ff"
              @click="startEdit(record)"
            >
              编辑
            </a-button>
            <template v-else>
              <a-button type="link" @click="saveEdit">保存</a-button>
              <a-button type="link" @click="cancelEdit">取消</a-button>
            </template>
            <a-button danger @click="doDeleteConfirm(record.id)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  deleteUserUsingPost,
  listUserVoByPageUsingPost,
  updateUserUsingPost,
} from '@/api/userController.ts'
import { Modal, message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { ID_COLUMN_WIDTH } from '@/constants/common'

const USER_ROLE_OPTIONS = [
  { label: '普通用户', value: 'user' },
  { label: '管理员', value: 'admin' },
]

const columns = [
  {
    title: 'id',
    dataIndex: 'id',
    width: ID_COLUMN_WIDTH.NORMAL,
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
  },
  {
    title: '用户名',
    dataIndex: 'userName',
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

// 定义数据
const dataList = ref<API.UserVO[]>([])
const total = ref(0)
const editingId = ref<number>()
const editFormData = reactive<API.UserUpdateRequest>({})

// 搜索条件
const searchParams = reactive<API.UserQueryRequest>({
  current: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'ascend',
})

// 获取数据
const fetchData = async () => {
  const res = await listUserVoByPageUsingPost({
    ...searchParams,
  })
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
}

// 页面加载时获取数据，请求一次
onMounted(() => {
  fetchData()
})

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.current,
    pageSize: searchParams.pageSize,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: number) => `共 ${total} 条`,
  }
})

// 表格变化之后，重新获取数据
const doTableChange = (page: any) => {
  searchParams.current = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 搜索数据
const doSearch = () => {
  // 重置页码
  searchParams.current = 1
  fetchData()
}

// 删除数据
const doDelete = async (id: number) => {
  if (!id) {
    return
  }
  const res = await deleteUserUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    // 刷新数据
    fetchData()
  } else {
    message.error('删除失败')
  }
}

// 确认删除数据
const doDeleteConfirm = (id: number) => {
  Modal.confirm({
    title: '确认删除该用户？',
    content: '删除后将无法恢复，请确认是否继续。',
    okText: '确认删除',
    cancelText: '取消',
    okButtonProps: {
      danger: true,
    },
    onOk: async () => {
      await doDelete(id)
    },
  })
}

const startEdit = (record: API.UserVO) => {
  editingId.value = record.id
  editFormData.id = record.id
  editFormData.userAccount = record.userAccount
  editFormData.userName = record.userName
  editFormData.userAvatar = record.userAvatar
  editFormData.userProfile = record.userProfile
  editFormData.userRole = record.userRole
}

const cancelEdit = () => {
  editingId.value = undefined
  editFormData.id = undefined
  editFormData.userAccount = undefined
  editFormData.userName = undefined
  editFormData.userAvatar = undefined
  editFormData.userProfile = undefined
  editFormData.userRole = undefined
}

const saveEdit = async () => {
  if (!editFormData.id) {
    message.error('用户 id 为空，无法保存')
    return
  }
  const res = await updateUserUsingPost({
    ...editFormData,
    id: editFormData.id,
  })
  if (res.data.code === 0) {
    message.success('修改成功')
    cancelEdit()
    fetchData()
  } else {
    message.error('修改失败，' + res.data.message)
  }
}
</script>
