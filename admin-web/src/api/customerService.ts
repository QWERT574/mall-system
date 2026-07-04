import request from '@/utils/request'
import type { CustomerServiceAgent } from '@/stores/chat'

export function listAgents(): Promise<CustomerServiceAgent[]> {
  return request.get<CustomerServiceAgent[]>('/cs/agents') as any
}

export function findAvailableAgent(skillGroup = 'pre_sale'): Promise<CustomerServiceAgent> {
  return request.get<CustomerServiceAgent>('/cs/agents/available', {
    params: { skillGroup }
  }) as any
}

export function updateAgentStatus(agentId: number, status: number): Promise<void> {
  return request.put<void>(`/cs/agent/${agentId}/status`, null, {
    params: { status }
  }) as any
}

export function assignAgent(sessionId: number, agentId: number): Promise<void> {
  return request.post<void>(`/cs/session/${sessionId}/assign`, null, {
    params: { agentId }
  }) as any
}

export function transferSession(params: {
  sessionId: number
  fromAgentId: number
  toAgentId: number
  reason?: string
  contextSummary?: string
}): Promise<any> {
  return request.post('/cs/transfer', null, { params }) as any
}

export function getTransferLogs(sessionId: number): Promise<any> {
  return request.get(`/cs/transfer/${sessionId}/logs`) as any
}

export function matchFaq(query: string): Promise<{ matched: boolean; answer: string | null }> {
  return request.post<{ matched: boolean; answer: string | null }>('/faq/match', null, {
    params: { query }
  }) as any
}

export function listFaqTemplates(): Promise<any> {
  return request.get('/faq/templates') as any
}

export function createFaqTemplate(template: any): Promise<any> {
  return request.post('/faq/template', template) as any
}

export function updateFaqTemplate(id: number, template: any): Promise<any> {
  return request.put(`/faq/template/${id}`, template) as any
}

export function deleteFaqTemplate(id: number): Promise<any> {
  return request.delete(`/faq/template/${id}`) as any
}
