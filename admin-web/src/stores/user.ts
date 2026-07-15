import { defineStore } from 'pinia'
import { login, logout, getUserInfo } from '@/api/auth'

interface UserState {
  token: string
  userInfo: any
}

export const useUserStore = defineStore('user', {
  state: (): UserState => {
    let userInfo = null
    try {
      const savedUser = localStorage.getItem('admin_user')
      if (savedUser && savedUser !== 'undefined' && savedUser !== 'null') {
        userInfo = JSON.parse(savedUser)
      }
    } catch (e) {
      console.warn('解析用户信息失败:', e)
      localStorage.removeItem('admin_user')
    }

    return {
      token: localStorage.getItem('admin_token') || '',
      userInfo
    }
  },
  
  actions: {
    async loginAction(username: string, password: string, captchaKey: string, captchaCode: string) {
      const { token, user } = await login({ username, password, captchaKey, captchaCode })
      this.token = token
      this.userInfo = user
      localStorage.setItem('admin_token', token)
      localStorage.setItem('admin_user', JSON.stringify(user))
    },
    
    async logoutAction() {
      await logout()
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_user')
    },
    
    async getUserInfoAction() {
      const user = await getUserInfo()
      this.userInfo = user
      localStorage.setItem('admin_user', JSON.stringify(user))
    }
  }
})
