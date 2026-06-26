import request from '@/utils/request'

// ===== 项目 =====
export interface ProjectDTO {
  id?: number
  code: string
  name: string
  customerName?: string
  managerId?: number
  address?: string
  startDate?: string
  endDate?: string
  type?: string
  status?: string
  description?: string
}

export const projectApi = {
  page: (params: { pageNum: number; pageSize: number; name?: string; type?: string; status?: string }) =>
    request.get('/business/project/page', { params }),
  get: (id: number) => request.get(`/business/project/${id}`),
  create: (data: ProjectDTO) => request.post('/business/project', data),
  update: (data: ProjectDTO) => request.put('/business/project', data),
  delete: (id: number) => request.delete(`/business/project/${id}`),
}

// ===== 合同 =====
export interface ContractDTO {
  id?: number
  code: string
  name: string
  partyA?: string
  partyB?: string
  signDate?: string
  amount?: number
  category?: string
  paymentMethod?: string
  projectId?: number
  status?: string
  startDate?: string
  endDate?: string
  remark?: string
}

export const contractApi = {
  page: (params: { pageNum: number; pageSize: number; name?: string; code?: string; status?: string }) =>
    request.get('/business/contract/page', { params }),
  get: (id: number) => request.get(`/business/contract/${id}`),
  create: (data: ContractDTO) => request.post('/business/contract', data),
  update: (data: ContractDTO) => request.put('/business/contract', data),
  delete: (id: number) => request.delete(`/business/contract/${id}`),
}

// ===== 报价 =====
export interface QuoteDTO {
  id?: number
  code: string
  projectId?: number
  businessType?: string
  businessId?: number
  amount?: number
  quoteDate?: string
  validUntil?: string
  quotePerson?: string
  customerName?: string
  version?: number
  status?: string
  summary?: string
}

export const quoteApi = {
  page: (params: { pageNum: number; pageSize: number; code?: string; customerName?: string; status?: string }) =>
    request.get('/business/quote/page', { params }),
  get: (id: number) => request.get(`/business/quote/${id}`),
  create: (data: QuoteDTO) => request.post('/business/quote', data),
  update: (data: QuoteDTO) => request.put('/business/quote', data),
  delete: (id: number) => request.delete(`/business/quote/${id}`),
}

// ===== 设备台账 =====
export interface EquipmentDTO {
  id?: number
  code: string
  name: string
  brand?: string
  model?: string
  serialNumber?: string
  category?: string
  specs?: string
  commissioningDate?: string
  warrantyExpiry?: string
  status?: string
  projectId?: number
  pointId?: number
}

export const equipmentApi = {
  page: (params: { pageNum: number; pageSize: number; code?: string; name?: string; category?: string; status?: string; projectId?: number }) =>
    request.get('/business/equipment/page', { params }),
  get: (id: number) => request.get(`/business/equipment/${id}`),
  create: (data: EquipmentDTO) => request.post('/business/equipment', data),
  update: (data: EquipmentDTO) => request.put('/business/equipment', data),
  delete: (id: number) => request.delete(`/business/equipment/${id}`),
}
