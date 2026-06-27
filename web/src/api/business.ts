import request from '@/utils/request'

// 后端 Long 已全局序列化为 String,所有 id 用 string 避免 JS 精度丢失

// ===== 项目 =====
export interface ProjectDTO {
  id?: string
  code: string
  name: string
  customerName?: string
  managerId?: string
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
  get: (id: string) => request.get(`/business/project/${id}`),
  create: (data: ProjectDTO) => request.post('/business/project', data),
  update: (data: ProjectDTO) => request.put('/business/project', data),
  delete: (id: string) => request.delete(`/business/project/${id}`),
}

// ===== 合同 =====
export interface ContractDTO {
  id?: string
  code: string
  name: string
  partyA?: string
  partyB?: string
  signDate?: string
  amount?: number
  category?: string
  paymentMethod?: string
  projectId?: string
  status?: string
  startDate?: string
  endDate?: string
  remark?: string
}

export const contractApi = {
  page: (params: { pageNum: number; pageSize: number; name?: string; code?: string; status?: string }) =>
    request.get('/business/contract/page', { params }),
  get: (id: string) => request.get(`/business/contract/${id}`),
  create: (data: ContractDTO) => request.post('/business/contract', data),
  update: (data: ContractDTO) => request.put('/business/contract', data),
  delete: (id: string) => request.delete(`/business/contract/${id}`),
}

// ===== 报价 =====
export interface QuoteDTO {
  id?: string
  code: string
  projectId?: string
  businessType?: string
  businessId?: string
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
  get: (id: string) => request.get(`/business/quote/${id}`),
  create: (data: QuoteDTO) => request.post('/business/quote', data),
  update: (data: QuoteDTO) => request.put('/business/quote', data),
  delete: (id: string) => request.delete(`/business/quote/${id}`),
}

// ===== 设备台账 =====
export interface EquipmentDTO {
  id?: string
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
  projectId?: string
  pointId?: string
}

export const equipmentApi = {
  page: (params: { pageNum: number; pageSize: number; code?: string; name?: string; category?: string; status?: string; projectId?: string }) =>
    request.get('/business/equipment/page', { params }),
  get: (id: string) => request.get(`/business/equipment/${id}`),
  create: (data: EquipmentDTO) => request.post('/business/equipment', data),
  update: (data: EquipmentDTO) => request.put('/business/equipment', data),
  delete: (id: string) => request.delete(`/business/equipment/${id}`),
}

// ===== 项目进度 =====
export interface ProgressDTO {
  id?: string
  code: string
  projectId?: string
  businessType?: string
  businessId?: string
  nodeName: string
  planStartDate?: string
  planEndDate?: string
  actualStartDate?: string
  actualEndDate?: string
  progressPercent?: number
  managerId?: string
  status?: string
  remark?: string
}

export const progressApi = {
  page: (params: { pageNum: number; pageSize: number; code?: string; nodeName?: string; status?: string; businessType?: string; businessId?: string }) =>
    request.get('/business/progress/page', { params }),
  get: (id: string) => request.get(`/business/progress/${id}`),
  create: (data: ProgressDTO) => request.post('/business/progress', data),
  update: (data: ProgressDTO) => request.put('/business/progress', data),
  delete: (id: string) => request.delete(`/business/progress/${id}`),
}

// ===== 项目验收 =====
export interface AcceptanceDTO {
  id?: string
  code: string
  projectId?: string
  businessType?: string
  businessId?: string
  quoteId?: string
  acceptorId?: string
  acceptDate?: string
  actualQuantity?: string
  result?: string
  rectifyCount?: number
  remark?: string
}

export const acceptanceApi = {
  page: (params: { pageNum: number; pageSize: number; code?: string; result?: string; businessType?: string; businessId?: string }) =>
    request.get('/business/acceptance/page', { params }),
  get: (id: string) => request.get(`/business/acceptance/${id}`),
  create: (data: AcceptanceDTO) => request.post('/business/acceptance', data),
  update: (data: AcceptanceDTO) => request.put('/business/acceptance', data),
  delete: (id: string) => request.delete(`/business/acceptance/${id}`),
  submitResult: (id: string, data: { result: string; remark?: string }) =>
    request.post(`/business/acceptance/${id}/result`, data),
}

// ===== 维护点位 =====
export interface MaintenancePointDTO {
  id?: string
  code: string
  projectId?: string
  name: string
  location?: string
  equipmentList?: string
  managerId?: string
  status?: string
}

export const maintenancePointApi = {
  page: (params: { pageNum: number; pageSize: number; code?: string; name?: string; status?: string; projectId?: string }) =>
    request.get('/business/maintenance-point/page', { params }),
  get: (id: string) => request.get(`/business/maintenance-point/${id}`),
  create: (data: MaintenancePointDTO) => request.post('/business/maintenance-point', data),
  update: (data: MaintenancePointDTO) => request.put('/business/maintenance-point', data),
  delete: (id: string) => request.delete(`/business/maintenance-point/${id}`),
}

// ===== 维保主合同 =====
export interface MaintenanceContractDTO {
  id?: string
  code: string
  name: string
  projectId?: string
  partyA?: string
  partyB?: string
  signDate?: string
  effectiveDate: string
  totalAmount: number
  periodMonths: number
  periodCount?: number
  responseSla?: string
  scope?: string
  status?: string
  endDate?: string
  remark?: string
}

export const maintenanceContractApi = {
  page: (params: { pageNum: number; pageSize: number; code?: string; name?: string; status?: string; projectId?: string }) =>
    request.get('/business/maintenance-contract/page', { params }),
  get: (id: string) => request.get(`/business/maintenance-contract/${id}`),
  create: (data: MaintenanceContractDTO) => request.post('/business/maintenance-contract', data),
  update: (data: MaintenanceContractDTO) => request.put('/business/maintenance-contract', data),
  delete: (id: string) => request.delete(`/business/maintenance-contract/${id}`),
}

// ===== 点位结算单 =====
export interface PointSettlementDTO {
  id?: string
  code: string
  projectId?: string
  pointId?: string
  quoteId?: string
  acceptanceId?: string
  amount?: number
  status?: string
  invoiceNo?: string
  receivedAmount?: number
  receivedDate?: string
  remark?: string
}

export const pointSettlementApi = {
  page: (params: { pageNum: number; pageSize: number; code?: string; pointId?: string; projectId?: string; status?: string }) =>
    request.get('/business/point-settlement/page', { params }),
  get: (id: string) => request.get(`/business/point-settlement/${id}`),
  create: (data: PointSettlementDTO) => request.post('/business/point-settlement', data),
  update: (data: PointSettlementDTO) => request.put('/business/point-settlement', data),
  delete: (id: string) => request.delete(`/business/point-settlement/${id}`),
}

// ===== 季度结算单 =====
export interface QuarterlySettlementDTO {
  id?: string
  code?: string
  contractId?: string
  projectId?: string
  periodNo?: number
  periodStartDate?: string
  periodEndDate?: string
  amount?: number
  amountVersion?: number
  status?: string
  invoiceNo?: string
  receivedAmount?: number
  receivedDate?: string
  remark?: string
}

export const quarterlySettlementApi = {
  page: (params: { pageNum: number; pageSize: number; code?: string; contractId?: string; projectId?: string; status?: string }) =>
    request.get('/business/quarterly-settlement/page', { params }),
  get: (id: string) => request.get(`/business/quarterly-settlement/${id}`),
  listByContract: (contractId: string) => request.get(`/business/quarterly-settlement/contract/${contractId}`),
  updateStatus: (id: string, data: { status: string; remark?: string }) =>
    request.put(`/business/quarterly-settlement/${id}/status`, data),
  adjust: (id: string, data: { amount: number; remark?: string }) =>
    request.put(`/business/quarterly-settlement/${id}/adjust`, data),
  delete: (id: string) => request.delete(`/business/quarterly-settlement/${id}`),
  generate: (contractId: string) => request.post(`/business/quarterly-settlement/contract/${contractId}/generate`),
}

// ===== 维保任务 =====
export interface MaintenanceTaskDTO {
  id?: string
  code: string
  projectId?: string
  pointId?: string
  equipmentId?: string
  type?: string
  title?: string
  description?: string
  reporterId?: string
  handlerId?: string
  handleMethod?: string
  partsUsed?: string
  status?: string
  planDate?: string
  completeDate?: string
  planInspectDate?: string
  remark?: string
}

export const maintenanceTaskApi = {
  page: (params: { pageNum: number; pageSize: number; code?: string; type?: string; status?: string; projectId?: string; equipmentId?: string }) =>
    request.get('/business/maintenance-task/page', { params }),
  get: (id: string) => request.get(`/business/maintenance-task/${id}`),
  create: (data: MaintenanceTaskDTO) => request.post('/business/maintenance-task', data),
  update: (data: MaintenanceTaskDTO) => request.put('/business/maintenance-task', data),
  delete: (id: string) => request.delete(`/business/maintenance-task/${id}`),
}

// ===== 维保记录 =====
export interface MaintenanceRecordDTO {
  id?: string
  code: string
  projectId?: string
  pointId?: string
  taskId?: string
  recordType?: string
  recordDate?: string
  recorderId?: string
  content?: string
  result?: string
  remark?: string
}

export const maintenanceRecordApi = {
  page: (params: { pageNum: number; pageSize: number; code?: string; projectId?: string; pointId?: string; recordType?: string }) =>
    request.get('/business/maintenance-record/page', { params }),
  get: (id: string) => request.get(`/business/maintenance-record/${id}`),
  create: (data: MaintenanceRecordDTO) => request.post('/business/maintenance-record', data),
  update: (data: MaintenanceRecordDTO) => request.put('/business/maintenance-record', data),
  delete: (id: string) => request.delete(`/business/maintenance-record/${id}`),
}

// ===== 附件 =====
export interface AttachmentDTO {
  id?: string
  name?: string
  filePath?: string
  fileSize?: number
  fileType?: string
  businessType?: string
  businessId?: string
  createTime?: string
}

export const attachmentApi = {
  page: (params: { pageNum: number; pageSize: number; name?: string; businessType?: string; businessId?: string }) =>
    request.get('/business/attachment/page', { params }),
  listByBusiness: (businessType: string, businessId: string) =>
    request.get('/business/attachment/list', { params: { businessType, businessId } }),
  get: (id: string) => request.get(`/business/attachment/${id}`),
  upload: (file: File, businessType: string, businessId: string) => {
    const form = new FormData()
    form.append('file', file)
    form.append('businessType', businessType)
    form.append('businessId', businessId)
    return request.post('/business/attachment/upload', form, { headers: { 'Content-Type': 'multipart/form-data' } })
  },
  delete: (id: string) => request.delete(`/business/attachment/${id}`),
  downloadUrl: (id: string) => `/api/business/attachment/download/${id}`,
}

// ===== 结算看板 =====
export const dashboardApi = {
  projectDashboard: (projectId: string) => request.get(`/business/dashboard/settlement/project/${projectId}`),
  projectDetail: (projectId: string) => request.get(`/business/dashboard/settlement/project/${projectId}/detail`),
}

// ===== 合同收付款 =====
export interface ContractPaymentDTO {
  id?: string
  code: string
  contractId?: string
  type?: string
  planDate?: string
  planAmount?: number
  actualAmount?: number
  actualDate?: string
  invoiceNo?: string
  status?: string
  remark?: string
  createTime?: string
}

export const contractPaymentApi = {
  page: (params: { pageNum: number; pageSize: number; code?: string; contractId?: string; type?: string; status?: string }) =>
    request.get('/business/contract-payment/page', { params }),
  get: (id: string) => request.get(`/business/contract-payment/${id}`),
  create: (data: ContractPaymentDTO) => request.post('/business/contract-payment', data),
  update: (data: ContractPaymentDTO) => request.put('/business/contract-payment', data),
  delete: (id: string) => request.delete(`/business/contract-payment/${id}`),
  recordActual: (id: string, data: { actualAmount: number; actualDate: string; invoiceNo?: string }) =>
    request.put(`/business/contract-payment/${id}/actual`, data),
  dashboard: (contractId: string) => request.get('/business/contract-payment/dashboard', { params: { contractId } }),
}

// ===== 审批流 =====
export interface ApprovalLogDTO {
  id?: string
  flowId?: string
  businessType?: string
  businessId?: string
  nodeOrder?: number
  approverId?: string
  result?: string
  opinion?: string
  approveTime?: string
  createTime?: string
}

export interface ApprovalFlowDTO {
  id?: string
  code: string
  name: string
  businessType?: string
  enabled?: number
  remark?: string
}

export interface ApprovalNodeDTO {
  id?: string
  flowId?: string
  nodeOrder: number
  nodeName: string
  approverRoleId?: string
  amountThreshold?: number
}

export const approvalApi = {
  start: (data: { businessType: string; businessId: string }) =>
    request.post('/business/approval/start', data),
  approve: (logId: string, data: { result: string; opinion?: string }) =>
    request.post(`/business/approval/${logId}/approve`, data),
  pending: () => request.get('/business/approval/pending'),
  history: () => request.get('/business/approval/history'),
  page: (params: { pageNum: number; pageSize: number; businessType?: string; businessId?: string; result?: string }) =>
    request.get('/business/approval/page', { params }),
  progress: (businessType: string, businessId: string) =>
    request.get('/business/approval/progress', { params: { businessType, businessId } }),
  flowList: () => request.get('/business/approval/flow/list'),
  flowSave: (data: ApprovalFlowDTO) => request.post('/business/approval/flow', data),
  flowDelete: (id: string) => request.delete(`/business/approval/flow/${id}`),
  flowNodes: (flowId: string) => request.get(`/business/approval/flow/${flowId}/nodes`),
  flowSaveNodes: (flowId: string, nodes: ApprovalNodeDTO[]) =>
    request.post(`/business/approval/flow/${flowId}/nodes`, nodes),
}

// ===== 工作台 =====
export interface WorkbenchVO {
  pendingApprovals: number
  pendingAcceptances: number
  overdueAcceptances: number
  overdueTasks: number
  expiringContracts: number
  pendingSettlements: number
}

export const workbenchApi = {
  summary: () => request.get('/business/workbench/summary'),
}

// ===== 报表中心 =====
// 注意:导出走 blob 下载,但 request 实例的响应拦截器会解包 response.data.data,
// 对 Blob 响应会失效,因此用 fetch 直接请求(带 token)绕过拦截器。
async function exportBlob(url: string): Promise<Blob> {
  const token = localStorage.getItem('ems_token')
  const res = await fetch('/api' + url, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  if (!res.ok) {
    const msg = await res.text().catch(() => '导出失败')
    throw new Error(msg || '导出失败')
  }
  return res.blob()
}

export const reportApi = {
  exportProject: () => exportBlob('/business/report/export/project'),
  exportContractPayment: () => exportBlob('/business/report/export/contract-payment'),
  exportProgress: () => exportBlob('/business/report/export/progress'),
  exportMaintenanceWorkload: () => exportBlob('/business/report/export/maintenance-workload'),
  exportSettlement: () => exportBlob('/business/report/export/settlement'),
}

// ===== 维保统计 =====
export interface FaultTypeStat {
  type: string
  count: number
}

export interface ResponseTimeStat {
  avgResponseHours: number
  totalCount: number
}

export interface EquipmentRank {
  equipmentId: string
  equipmentName: string
  faultCount: number
}

export interface WorkloadStat {
  handlerId: string
  taskCount: number
  completedCount: number
}

export interface EquipmentHealth {
  equipmentId: string
  equipmentName: string
  healthScore: number
}

export interface MaintenanceStatVO {
  faultTypeStats: FaultTypeStat[]
  responseTimeStat: ResponseTimeStat
  equipmentRanks: EquipmentRank[]
  workloadStats: WorkloadStat[]
  equipmentHealth: EquipmentHealth[]
}

export const maintenanceStatApi = {
  summary: () => request.get('/business/maintenance-stat/summary'),
}
