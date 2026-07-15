<template>
  <div class="seller-chat-page">
    <div class="chat-layout">
      <!-- 会话列表 -->
      <div class="session-panel" :class="{ collapsed: !showSessions }">
        <div class="panel-header">
          <h3>会话列表</h3>
          <button class="toggle-btn" @click="showSessions = !showSessions">&#9776;</button>
          <button class="toggle-btn" @click="loadSessions">&#8635;</button>
        </div>
        <div class="session-list">
          <div v-for="s in sessions" :key="s.id" class="session-item" :class="{ active: currentSession?.id === s.id }" @click="selectSession(s)">
            <div class="session-avatar">{{ (s.userName || '用户').charAt(0) }}</div>
            <div class="session-info">
              <div class="session-name">
                {{ s.userName || '用户' }}
                <span v-if="s.productName" class="product-tag">{{ s.productName }}</span>
                <span v-if="s.sellerUnread > 0" class="unread-badge">{{ s.sellerUnread > 99 ? '99+' : s.sellerUnread }}</span>
              </div>
              <div class="session-preview">{{ s.lastMessage || '暂无消息' }}</div>
            </div>
            <div class="session-time">{{ formatSessionTime(s.updatedAt || s.createdAt) }}</div>
          </div>
          <div v-if="sessions.length === 0" class="empty-hint">暂无会话</div>
        </div>
      </div>

      <!-- 聊天区域 -->
      <div class="chat-panel">
        <div v-if="!currentSession" class="no-session">
          <div class="no-session-icon">&#128172;</div>
          <p>选择一个会话开始聊天</p>
        </div>
        <template v-else>
          <div class="chat-header">
            <div class="chat-user">
              <div class="chat-avatar">{{ (currentSession.userName || '用户').charAt(0) }}</div>
              <div>
                <div class="chat-username">{{ currentSession.userName || '用户' }}</div>
                <div class="chat-status" :class="{ online: isOnline }">{{ isOnline ? '在线' : '离线' }}</div>
              </div>
            </div>
            <div class="chat-actions">
              <button class="action-btn" @click="requestIntervention">{{ interventionApplied ? '已申请介入' : '申请介入' }}</button>
              <button class="action-btn" @click="closeChat">关闭</button>
            </div>
          </div>

          <div class="chat-messages" ref="msgContainer">
            <template v-for="(msg, idx) in displayMessages" :key="msg.id">
              <div v-if="idx === 0 || getDateKey(msg.createdAt) !== getDateKey(displayMessages[idx-1].createdAt)" class="date-separator">
                {{ getDateLabel(msg.createdAt) }}
              </div>
              <div :class="['message-row', msg.type]">
                <div :class="['msg-avatar', msg.type]">{{ msg.avatarText }}</div>
                <div :class="['msg-content', msg.type]">
                  <div v-if="msg.type !== 'mine'" class="msg-sender">{{ msg.senderLabel }}</div>
                  <div class="msg-bubble">
                    <img v-if="msg.imageUrl" :src="msg.imageUrl" class="msg-image" />
                    <span v-else>{{ msg.content }}</span>
                  </div>
                  <div class="msg-meta">
                    <span class="msg-time">{{ formatMsgTime(msg.createdAt) }}</span>
                    <span v-if="msg.type === 'mine' && msg.status" :class="['msg-status', msg.status]" @click="msg.status === 'failed' && retryMessage(msg.id)" :style="msg.status === 'failed' ? 'cursor:pointer;text-decoration:underline' : ''">
                      {{ msg.status === 'sending' ? '发送中...' : msg.status === 'sent' ? '已发送' : msg.status === 'delivered' ? '已送达' : msg.status === 'read' ? '已读' : '点击重试' }}
                    </span>
                  </div>
                </div>
              </div>
            </template>
          </div>

          <div class="chat-input">
            <input v-model="inputText" @keyup.enter="sendText" placeholder="输入消息..." class="msg-input" />
            <button class="send-btn" @click="sendText" :disabled="!inputText.trim()">发送</button>
          </div>
        </template>
      </div>
    </div>

    <!-- 介入申请弹窗 -->
    <div v-if="showIntervention" class="modal-overlay" @click.self="showIntervention = false">
      <div class="modal-box">
        <h4>申请平台介入</h4>
        <textarea v-model="interventionReason" placeholder="请描述问题..." rows="4" class="modal-textarea"></textarea>
        <div class="modal-actions">
          <button @click="submitIntervention" :disabled="!interventionReason.trim()">提交申请</button>
          <button @click="showIntervention = false">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import api from '@/utils/api';
import { ElMessage } from 'element-plus';
import { useChatNotification } from '@/composables/useChatNotification';
import { saveMessages, loadMessages as loadCachedMessages, savePendingMessage, getPendingMessages, removePendingMessage, getLastMessageId, setLastMessageId } from '@/utils/chatStorage';

export default {
  name: 'SellerChat',
  setup() {
    const sessions = ref([]);
    const currentSession = ref(null);
    const messages = ref([]);
    const inputText = ref('');
    const showSessions = ref(true);
    const isOnline = ref(false);
    const interventionApplied = ref(false);
    const showIntervention = ref(false);
    const interventionReason = ref('');
    const msgContainer = ref(null);
    const retryQueue = ref([]);

    let stompClient = null;
    let globalStompClient = null;
    let currentSellerId = null;
    let heartbeatTimer = null;
    let globalSubscription = null;
    let retryTimer = null;

    const notification = useChatNotification();

    let readStatusTimer = null;
    const generateId = () => 'msg_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);

    const saveCache = () => {
      if (currentSellerId && currentSession.value) {
        saveMessages(currentSellerId, currentSession.value.id, messages.value)
      }
    }
    const loadCache = () => {
      if (currentSellerId && currentSession.value) {
        const cached = loadCachedMessages(currentSellerId, currentSession.value.id)
        if (cached.length) messages.value = cached
      }
    }
    const flushPendingQueue = async () => {
      const pending = getPendingMessages(currentSellerId)
      if (!pending.length || !currentSession.value) return
      for (const msg of pending) {
        try {
          const res = await api.post('/chat/message', null, {
            params: { sessionId: msg.sessionId || currentSession.value.id, content: msg.content }
          })
          if (res) {
            removePendingMessage(currentSellerId, msg._clientMsgId)
            const idx = messages.value.findIndex(m => m._clientMsgId === msg._clientMsgId || m.id === msg._clientMsgId)
            if (idx !== -1) messages.value[idx] = { ...messages.value[idx], status: 'sent', id: res.id }
          }
        } catch (e) {}
      }
    }

    try {
      const u = JSON.parse(localStorage.getItem('seller_user') || '{}');
      currentSellerId = u.id || u.sellerId || u.userId;
      console.log('[SellerChat] currentSellerId:', currentSellerId, 'from seller_user:', JSON.stringify(u).substring(0, 200));
    } catch(e) {
      console.warn('[SellerChat] failed to read seller_user:', e);
    }

    const sortedMessages = computed(() => {
      return [...messages.value].sort((a, b) => {
        const ta = new Date(a.createdAt || 0).getTime();
        const tb = new Date(b.createdAt || 0).getTime();
        return ta - tb;
      });
    });

    const displayMessages = computed(() => {
      return sortedMessages.value.map(msg => {
        let type = 'customer';

        if (currentSellerId && String(msg.senderId) === String(currentSellerId)) {
          type = 'mine';
        } else if (msg.senderType === 3) {
          type = 'admin';
        }

        return {
          ...msg,
          type,
          avatarText: type === 'mine' ? '我' : type === 'admin' ? '管' : (msg.senderName || '用').charAt(0),
          senderLabel: type === 'admin' ? '平台管理员' : (type === 'customer' ? (msg.senderName || '用户') : ''),
          isRead: msg.isRead === 1,
          status: msg.status || 'sent'
        };
      });
    });

    const pad = (n) => String(n).padStart(2, '0');

    const parseDate = (t) => {
      if (!t) return null;
      const d = new Date(t);
      if (isNaN(d.getTime())) return null;
      return d;
    };

    const formatSessionTime = (t) => {
      const d = parseDate(t);
      if (!d) return '';
      const now = new Date();
      const timeStr = pad(d.getHours()) + ':' + pad(d.getMinutes());
      if (d.toDateString() === now.toDateString()) return timeStr;
      const yest = new Date(now); yest.setDate(yest.getDate() - 1);
      if (d.toDateString() === yest.toDateString()) return '昨天';
      if (d.getFullYear() === now.getFullYear()) return (d.getMonth() + 1) + '/' + d.getDate();
      return d.getFullYear() + '/' + (d.getMonth() + 1) + '/' + d.getDate();
    };

    const formatMsgTime = (t) => {
      const d = parseDate(t);
      if (!d) return '';
      const now = new Date();
      const timeStr = pad(d.getHours()) + ':' + pad(d.getMinutes());
      if (d.toDateString() === now.toDateString()) return timeStr;
      const yest = new Date(now); yest.setDate(yest.getDate() - 1);
      if (d.toDateString() === yest.toDateString()) return '昨天 ' + timeStr;
      if (d.getFullYear() === now.getFullYear()) return pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + timeStr;
      return d.getFullYear() + '/' + pad(d.getMonth() + 1) + '/' + pad(d.getDate()) + ' ' + timeStr;
    };

    const getDateKey = (t) => {
      const d = parseDate(t);
      if (!d) return '';
      return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
    };

    const getDateLabel = (t) => {
      const d = parseDate(t);
      if (!d) return '';
      const now = new Date();
      if (d.toDateString() === now.toDateString()) return '今天';
      const yest = new Date(now); yest.setDate(yest.getDate() - 1);
      if (d.toDateString() === yest.toDateString()) return '昨天';
      return d.getFullYear() + '年' + (d.getMonth() + 1) + '月' + d.getDate() + '日';
    };

    const loadSessions = async () => {
      try {
        const res = await api.get('/chat/seller/sessions');
        sessions.value = res || [];
      } catch (e) {
        console.error('加载会话列表失败:', e);
        ElMessage.error('加载会话列表失败');
      }
    };

    const loadMessages = async (sessionId) => {
      try {
        const res = await api.get('/chat/messages/' + sessionId);
        const all = res || [];
        messages.value = all.filter(m => !m.createdAt || new Date(m.createdAt) >= new Date(Date.now() - 30 * 86400000));
        nextTick(() => scrollToBottom());
      } catch (e) {
        console.error('加载消息失败:', e);
      }
    };

    const scrollToBottom = () => {
      if (msgContainer.value) {
        msgContainer.value.scrollTop = msgContainer.value.scrollHeight;
      }
    };

    const selectSession = (session) => {
      currentSession.value = session;
      interventionApplied.value = session.interventionApplied || false;
      if (session.sellerUnread > 0) {
        session.sellerUnread = 0;
        api.post('/chat/read', null, { params: { sessionId: session.id } }).catch(() => {});
        const undelivered = messages.value.filter(m => m.senderId !== currentSellerId && (m.status == null || m.status < 2) && m.id && !String(m.id).startsWith('msg_'));
        if (undelivered.length > 0) {
          api.put('/chat/messages/delivered/batch', { messageIds: undelivered.map(m => m.id) }).catch(() => {});
        }
      }
      loadMessages(session.id);
      loadCache()
      connectWebSocket(session.id);
    };

    const sendText = async () => {
      if (!inputText.value.trim() || !currentSession.value) return;
      const content = inputText.value;
      const clientMsgId = generateId();
      
      messages.value.push({
        id: clientMsgId, sessionId: currentSession.value.id, content,
        senderId: currentSellerId, senderType: 2, createdAt: new Date().toISOString(),
        isRead: 0, status: 'sending'
      });
      inputText.value = '';
      nextTick(() => scrollToBottom());
      
      try {
        const res = await api.post('/chat/message', null, {
          params: { sessionId: currentSession.value.id, content }
        });
        const idx = messages.value.findIndex(m => m.id === clientMsgId);
        if (idx !== -1) {
          messages.value[idx] = { ...res, status: 'sent' };
        } else {
          messages.value.push({ ...res, status: 'sent' });
        }
        saveCache()
      } catch (e) {
        const idx = messages.value.findIndex(m => m.id === clientMsgId);
        if (idx !== -1) messages.value[idx] = { ...messages.value[idx], status: 'failed' };
        savePendingMessage(currentSellerId, currentSession.value.id, { _clientMsgId: clientMsgId, content });
        retryQueue.value.push({ clientMsgId, content, attempt: 1 });
        scheduleRetry();
        console.error('发送失败:', e);
        ElMessage.error('发送失败，将自动重试');
      }
      nextTick(() => scrollToBottom());
    };

    const connectWebSocket = (sessionId) => {
      if (stompClient?.active) stompClient.deactivate();
      
      const token = localStorage.getItem('seller_token') || '';
      const wsUrl = 'http://localhost:8081/ws-chat';
      
      stompClient = new Client({
        webSocketFactory: () => new SockJS(wsUrl),
        connectHeaders: { Authorization: token ? 'Bearer ' + token : '' },
        reconnectDelay: 5000,
        onConnect: () => {
          console.log('WebSocket connected');
          flushPendingQueue()
          stompClient.subscribe('/topic/chat/' + sessionId, (msg) => {
            const data = JSON.parse(msg.body);
            if (data.type === 'message_status') {
              const idx = messages.value.findIndex(m => m.id === data.messageId);
              if (idx !== -1) messages.value[idx] = { ...messages.value[idx], status: data.status };
              return;
            }
            if (currentSellerId && String(data.senderId) === String(currentSellerId)) return;
            messages.value.push(data);
            nextTick(() => scrollToBottom());
            notification.notify('新消息', data.content || '[图片]');
            saveCache()
          });
          stompClient.subscribe('/topic/chat/session/' + sessionId, (msg) => {
            const data = JSON.parse(msg.body);
            if (data.type === 'message_status') {
              const idx = messages.value.findIndex(m => m.id === data.messageId);
              if (idx !== -1) messages.value[idx] = { ...messages.value[idx], status: data.status };
              return;
            }
            if (currentSellerId && String(data.senderId) === String(currentSellerId)) return;
            const exists = messages.value.some(m => m.id && m.id === data.id);
            if (!exists) {
              messages.value.push(data);
              nextTick(() => scrollToBottom());
              notification.notify('新消息', data.content || '[图片]');
              saveCache()
            }
          });
        },
        onStompError: (err) => console.error('STOMP error:', err)
      });
      stompClient.activate();
    };

    const connectGlobalWebSocket = () => {
      if (!currentSellerId) return;
      if (globalStompClient?.active) {
        globalStompClient.deactivate();
        globalSubscription = null;
      }

      const token = localStorage.getItem('seller_token') || '';
      const wsUrl = 'http://localhost:8081/ws-chat';

      globalStompClient = new Client({
        webSocketFactory: () => new SockJS(wsUrl),
        connectHeaders: { Authorization: token ? 'Bearer ' + token : '' },
        reconnectDelay: 5000,
        onConnect: () => {
          console.log('Global WebSocket connected for seller:', currentSellerId);
          globalStompClient.subscribe('/user/' + currentSellerId + '/queue/notifications', (msg) => {
            const data = JSON.parse(msg.body);
            if (data.type === 'new_message') {
              const s = sessions.value.find(s => s.id === data.sessionId);
              if (s) {
                s.lastMessage = data.content;
                s.updatedAt = data.createdAt;
                if (data.senderType === 1) {
                  s.sellerUnread = (s.sellerUnread || 0) + 1;
                }
              } else {
                loadSessions();
              }
              notification.notify('新消息', data.content || '您有新的客服消息');
              if (currentSession.value && currentSession.value.id === data.sessionId) {
                loadMessages(currentSession.value.id);
              }
            }
            else if (data.type === 'message_status') {
              if (currentSession.value) {
                const idx = messages.value.findIndex(m => m.id === data.messageId);
                if (idx !== -1) messages.value[idx] = { ...messages.value[idx], status: data.status };
              }
            }
          });
        },
        onStompError: (err) => console.error('Global STOMP error:', err)
      });
      globalStompClient.activate();
    };

    const startHeartbeat = () => {
      if (heartbeatTimer) clearInterval(heartbeatTimer);
      api.post('/chat/online').catch(() => {});
      heartbeatTimer = setInterval(() => {
        api.post('/chat/online').catch(() => {});
      }, 5 * 60 * 1000);
    };

    const stopHeartbeat = () => {
      if (heartbeatTimer) {
        clearInterval(heartbeatTimer);
        heartbeatTimer = null;
      }
    };

    const scheduleRetry = () => {
      if (retryTimer) clearTimeout(retryTimer);
      if (retryQueue.value.length === 0) return;
      const next = retryQueue.value[0];
      const delay = Math.min(1000 * Math.pow(2, next.attempt - 1), 30000);
      retryTimer = setTimeout(async () => {
        if (!currentSession.value) { retryQueue.value.shift(); scheduleRetry(); return; }
        try {
          const res = await api.post('/chat/message', null, {
            params: { sessionId: currentSession.value.id, content: next.content }
          });
          const idx = messages.value.findIndex(m => m.id === next.clientMsgId);
          if (idx !== -1) messages.value[idx] = { ...res, status: 'sent' };
          saveCache();
        } catch (e) {
          next.attempt++;
          if (next.attempt <= 5) {
            scheduleRetry();
          } else {
            const idx = messages.value.findIndex(m => m.id === next.clientMsgId);
            if (idx !== -1) messages.value[idx] = { ...messages.value[idx], status: 'failed' };
          }
          return;
        }
        retryQueue.value.shift();
        scheduleRetry();
      }, delay);
    };

    const retryMessage = (msgId) => {
      const idx = messages.value.findIndex(m => m.id === msgId);
      if (idx === -1) return;
      const msg = messages.value[idx];
      if (msg.status !== 'failed') return;
      messages.value[idx] = { ...msg, status: 'sending' };
      retryQueue.value.push({ clientMsgId: msgId, content: msg.content, attempt: 1 });
      scheduleRetry();
    };

    const closeChat = async () => {
      if (!currentSession.value) return;
      try {
        await api.post('/chat/close/' + currentSession.value.id);
        ElMessage.success('会话已关闭');
        currentSession.value = null;
        loadSessions();
      } catch (e) {
        console.error('关闭失败:', e);
      }
    };

    const requestIntervention = () => {
      showIntervention.value = true;
    };

    const submitIntervention = async () => {
      if (!interventionReason.value.trim() || !currentSession.value) return;
      try {
        await api.post('/chat/request-intervention', null, {
          params: { sessionId: currentSession.value.id, reason: interventionReason.value }
        });
        interventionApplied.value = true;
        showIntervention.value = false;
        interventionReason.value = '';
        ElMessage.success('介入申请已提交');
      } catch (e) {
        console.error('申请介入失败:', e);
        ElMessage.error('提交失败');
      }
    };

    watch(() => messages.value.length, () => nextTick(() => scrollToBottom()));

    onMounted(() => {
      notification.requestPermission();
      loadSessions();
      startHeartbeat();
      setTimeout(connectGlobalWebSocket, 1000);
      readStatusTimer = setInterval(() => {
        if (!currentSession.value) return;
        api.get('/chat/messages/' + currentSession.value.id).then(res => {
          if (res) {
            messages.value.forEach(localMsg => {
              if (localMsg.status !== 'sent' && localMsg.status !== 'sending') return;
              if (String(localMsg.senderId) !== String(currentSellerId)) return;
              const serverMsg = res.find(m => m.id === localMsg.id);
              if (serverMsg && serverMsg.isRead === 1) {
                localMsg.isRead = 1;
                localMsg.status = 'read';
              }
            });
          }
        }).catch(() => {});
      }, 5000);
    });

    onUnmounted(() => {
      if (readStatusTimer) clearInterval(readStatusTimer);
      stopHeartbeat();
      if (retryTimer) { clearTimeout(retryTimer); retryTimer = null; }
      if (stompClient?.active) stompClient.deactivate();
      if (globalStompClient?.active) globalStompClient.deactivate();
      globalSubscription = null;
      api.post('/chat/offline').catch(() => {});
    });

    return {
      sessions, currentSession, messages, displayMessages, inputText, showSessions,
      isOnline, interventionApplied, showIntervention, interventionReason,
      msgContainer, formatSessionTime, formatMsgTime, getDateKey, getDateLabel,
      loadSessions, loadMessages, selectSession,
      sendText, closeChat, requestIntervention, submitIntervention, retryMessage
    };
  }
};
</script>

<style scoped>
.seller-chat-page { height: 100%; margin: -24px -28px; display: flex; flex-direction: column; }
.chat-layout { display: flex; flex: 1; overflow: hidden; }
.session-panel { width: 280px; border-right: 1px solid #e5e7eb; display: flex; flex-direction: column; }
.session-panel.collapsed { width: 50px; }
.panel-header { padding: 12px; display: flex; align-items: center; gap: 8px; border-bottom: 1px solid #e5e7eb; }
.panel-header h3 { flex: 1; margin: 0; font-size: 14px; }
.toggle-btn { border: none; background: none; cursor: pointer; font-size: 16px; padding: 4px 8px; }
.session-list { flex: 1; overflow-y: auto; }
.session-item { display: flex; padding: 12px; gap: 10px; cursor: pointer; align-items: center; }
.session-item:hover { background: #f3f4f6; }
.session-item.active { background: #eff6ff; }
.session-avatar { width: 36px; height: 36px; border-radius: 50%; background: #3b82f6; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 14px; }
.session-info { flex: 1; min-width: 0; }
.session-name { font-size: 13px; font-weight: 500; display: flex; align-items: center; gap: 6px; }
.product-tag { font-size: 10px; background: #e8f4fd; color: #1890ff; padding: 1px 6px; border-radius: 8px; font-weight: 400; max-width: 80px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.unread-badge { background: var(--color-error); color: #fff; font-size: 10px; font-weight: 600; min-width: 16px; height: 16px; line-height: 16px; border-radius: 8px; padding: 0 5px; text-align: center; display: inline-block; }
.session-preview { font-size: 12px; color: #6b7280; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.session-time { font-size: 11px; color: #9ca3af; }
.chat-panel { flex: 1; display: flex; flex-direction: column; }
.no-session { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #9ca3af; }
.no-session-icon { font-size: 48px; margin-bottom: 16px; }
.chat-header { padding: 12px 16px; border-bottom: 1px solid #e5e7eb; display: flex; justify-content: space-between; align-items: center; }
.chat-user { display: flex; align-items: center; gap: 10px; }
.chat-avatar { width: 32px; height: 32px; border-radius: 50%; background: #3b82f6; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 13px; }
.chat-username { font-weight: 500; font-size: 14px; }
.chat-status { font-size: 12px; color: #9ca3af; }
.chat-status.online { color: #22c55e; }
.action-btn { padding: 6px 12px; border: 1px solid #d1d5db; border-radius: 6px; background: #fff; cursor: pointer; font-size: 12px; }
.action-btn:hover { background: #f3f4f6; }
.chat-messages { flex: 1; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; }
.date-separator { text-align: center; font-size: 11px; color: #9ca3af; margin: 8px 0; position: relative; }
.date-separator::before, .date-separator::after { content: ''; position: absolute; top: 50%; width: 30%; height: 1px; background: #e5e7eb; }
.date-separator::before { left: 0; } .date-separator::after { right: 0; }

.message-row { display: flex; width: 100%; margin-bottom: 14px; align-items: flex-start; }
.message-row.mine { flex-direction: row-reverse; }
.message-row.customer, .message-row.admin { flex-direction: row; }

.msg-avatar { width: 34px; height: 34px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; font-size: 12px; font-weight: 700; box-shadow: 0 1px 4px rgba(0,0,0,0.08); }
.msg-avatar.mine { background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%); color: #fff; }
.msg-avatar.customer { background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%); color: #fff; }
.msg-avatar.admin { background: linear-gradient(135deg, var(--color-warning) 0%, #fbbf24 100%); color: #fff; }

.msg-content { margin: 0 10px; max-width: 70%; display: flex; flex-direction: column; }
.msg-content.mine { align-items: flex-end; }
.msg-content.customer, .msg-content.admin { align-items: flex-start; }

.msg-sender { font-size: 11px; color: #6b7280; margin-bottom: 3px; font-weight: 500; }

.msg-bubble { padding: 9px 14px; border-radius: 14px; word-break: break-word; font-size: 13.5px; line-height: 1.55; max-width: 100%; }
.msg-content.mine .msg-bubble { background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%); color: #fff; border-bottom-right-radius: 4px; box-shadow: 0 2px 8px rgba(59,130,246,0.18); }
.msg-content.customer .msg-bubble { background: #fff; border: 1px solid #e5e7eb; color: #1f2937; border-bottom-left-radius: 4px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.msg-content.admin .msg-bubble { background: rgba(245,158,11,0.07); border: 1px solid rgba(245,158,11,0.15); color: #1f2937; border-bottom-left-radius: 4px; }

.msg-image { max-width: 180px; max-height: 140px; border-radius: 8px; cursor: pointer; margin-top: 4px; }

.msg-meta { display: flex; align-items: center; gap: 6px; margin-top: 4px; }
.msg-content.mine .msg-meta { justify-content: flex-end; }
.msg-content.customer .msg-meta, .msg-content.admin .msg-meta { justify-content: flex-start; }

.msg-time { font-size: 10px; color: #9ca3af; }

.msg-status {
  font-size: 10px;
  font-weight: 500;
}
.msg-status.sending { color: var(--color-warning); }
.msg-status.sent { color: var(--color-text-tertiary); }
.msg-status.read { color: var(--color-success); }
.msg-status.failed { color: var(--color-error); }
.chat-input { padding: 12px; border-top: 1px solid var(--color-border-light); display: flex; gap: 8px; }
.msg-input { flex: 1; padding: 8px 12px; border: 1px solid var(--color-border-default); border-radius: 8px; outline: none; font-size: 13px; }
.msg-input:focus { border-color: var(--color-info); }
.send-btn { padding: 8px 20px; background: var(--color-info); color: var(--color-text-inverse); border: none; border-radius: 8px; cursor: pointer; font-size: 13px; }
.send-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.empty-hint { padding: 40px; text-align: center; color: var(--color-text-tertiary); font-size: 13px; }
.modal-overlay { position: fixed; inset: 0; background: var(--color-bg-mask); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-box { background: var(--color-bg-container); border-radius: 12px; padding: 24px; width: 400px; max-width: 90vw; }
.modal-box h4 { margin: 0 0 16px; font-size: 16px; }
.modal-textarea { width: 100%; padding: 10px; border: 1px solid var(--color-border-default); border-radius: 8px; resize: vertical; font-size: 13px; box-sizing: border-box; }
.modal-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 16px; }
.modal-actions button { padding: 8px 20px; border-radius: 8px; border: none; cursor: pointer; font-size: 13px; }
.modal-actions button:first-child { background: #3b82f6; color: #fff; }
.modal-actions button:first-child:disabled { opacity: 0.5; }
.modal-actions button:last-child { background: #f3f4f6; }
</style>