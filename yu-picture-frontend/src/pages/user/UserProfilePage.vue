<template>
  <div id="userProfilePage">
    <a-card class="profile-card" title="个人设置">
      <a-row :gutter="24" align="middle">
        <a-col :xs="24" :md="8">
          <div class="avatar-panel">
            <a-avatar :size="120" :src="formState.userAvatar">
              {{ avatarFallbackText }}
            </a-avatar>
            <div class="avatar-tip">头像预览</div>
            <div class="user-name">{{ formState.userName || '未设置用户名' }}</div>
            <div class="user-account">{{ formState.userAccount || '未设置账号' }}</div>
            <a-tag v-if="loginUserStore.loginUser.userRole" color="blue">
              {{ loginUserStore.loginUser.userRole === 'admin' ? '管理员' : '普通用户' }}
            </a-tag>
          </div>
        </a-col>
        <a-col :xs="24" :md="16">
          <a-form layout="vertical" :model="formState" @finish="handleSubmit">
            <a-form-item label="用户 id">
              <a-input v-model:value="formState.id" disabled />
            </a-form-item>
            <a-form-item label="账号" name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
              <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
            </a-form-item>
            <a-form-item label="用户名" name="userName">
              <a-input v-model:value="formState.userName" placeholder="请输入用户名" />
            </a-form-item>
            <a-form-item label="头像地址" name="userAvatar">
              <a-input v-model:value="formState.userAvatar" placeholder="请输入头像地址" />
            </a-form-item>
            <a-form-item label="个人简介" name="userProfile">
              <a-textarea
                v-model:value="formState.userProfile"
                :rows="4"
                placeholder="请输入个人简介"
              />
            </a-form-item>
            <a-form-item>
              <a-space>
                <a-button type="primary" html-type="submit">保存修改</a-button>
                <a-button @click="resetForm">重置</a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </a-col>
      </a-row>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { updateUserUsingPost } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const formState = reactive<API.UserUpdateRequest>({})

const avatarFallbackText = computed(() => {
  return formState.userName?.slice(0, 1) || formState.userAccount?.slice(0, 1) || '用'
})

const syncFormFromLoginUser = () => {
  const loginUser = loginUserStore.loginUser
  if (!loginUser?.id) {
    return false
  }
  formState.id = loginUser.id
  formState.userAccount = loginUser.userAccount
  formState.userName = loginUser.userName
  formState.userAvatar = loginUser.userAvatar
  formState.userProfile = loginUser.userProfile
  formState.userRole = loginUser.userRole
  return true
}

const resetForm = () => {
  syncFormFromLoginUser()
}

const handleSubmit = async () => {
  if (!formState.id) {
    message.error('用户信息不存在，请重新登录')
    await router.push('/user/login')
    return
  }
  const res = await updateUserUsingPost({
    id: formState.id,
    userAccount: formState.userAccount,
    userName: formState.userName,
    userAvatar: formState.userAvatar,
    userProfile: formState.userProfile,
  })
  if (res.data.code === 0) {
    message.success('保存成功')
    await loginUserStore.fetchLoginUser()
    syncFormFromLoginUser()
  } else {
    message.error('保存失败，' + res.data.message)
  }
}

onMounted(async () => {
  await loginUserStore.fetchLoginUser()
  if (!syncFormFromLoginUser()) {
    message.error('请先登录')
    await router.push('/user/login')
  }
})
</script>

<style scoped>
#userProfilePage {
  max-width: 1100px;
  margin: 0 auto;
}

.profile-card {
  border-radius: 16px;
}

.avatar-panel {
  min-height: 360px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 24px 12px;
  background: linear-gradient(180deg, rgba(22, 119, 255, 0.08), rgba(22, 119, 255, 0.02));
  border-radius: 16px;
}

.avatar-tip {
  font-size: 13px;
  color: #8c8c8c;
}

.user-name {
  font-size: 20px;
  font-weight: 600;
}

.user-account {
  color: #595959;
}
</style>
