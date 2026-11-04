import { request } from '@/utils/request';

// 倒仓管理 - 列表查询
export function listTransferStorageInfoByCondition(data: Record<string, unknown>) {
  return request.post({
    url: '/transferStorageInfo/listTransferStorageInfoByCondition',
    method: 'post',
    data,
  });
}

// 倒仓管理 - 新增
export function insertTransferStorageInfo(data: Record<string, unknown>) {
  return request.post({
    url: '/transferStorageInfo/saveTransferStorageInfo',
    method: 'post',
    data,
  });
}

// 倒仓管理 - 编辑
export function updateTransferStorageInfo(data: Record<string, unknown>) {
  return request.post({
    url: '/transferStorageInfo/updateTransferStorageInfo',
    method: 'post',
    data,
  });
}

// 倒仓管理 - 详情
export function detailTransferStorageInfo(id: string) {
  return request.post({
    url: `/transferStorageInfo/getTransferStorageInfoByDetail/${id}`,
    method: 'post',
  });
}

// 倒仓管理 - 删除
export function deleteTransferStorageInfo(data: { ids: string[] }) {
  return request.post({
    url: '/transferStorageInfo/deleteTransferStorageInfo',
    method: 'post',
    data,
  });
}

// 倒仓管理 - 提交
export function processPending(id: string) {
  return request.post({
    url: `/transferStorageInfo/processPending/${id}`,
    method: 'post',
  });
}

// 倒仓管理 - 撤回
export function processCancel(id: string) {
  return request.post({
    url: `/transferStorageInfo/processCancel/${id}`,
    method: 'post',
  });
}

// 倒仓管理 - 作废
export function processVoided(id: string) {
  return request.post({
    url: `/transferStorageInfo/processVoided/${id}`,
    method: 'post',
  });
}

// 倒仓审批 - 列表查询
export function listTransferStorageAuditByCondition(data: Record<string, unknown>) {
  return request.post({
    url: '/transferStorageAudit/listTransferStorageInfoByCondition',
    method: 'post',
    data,
  });
}

// 倒仓审批 - 查看审批流
export function listTransferStorageAuditFlowByCondition(data: Record<string, unknown>) {
  return request.post({
    url: '/transferStorageAudit/listTransferStorageAuditByCondition',
    method: 'post',
    data,
  });
}

// 倒仓审批 - 审核
export function auditTransferStorage(data: Record<string, unknown>) {
  return request.post({
    url: '/transferStorageAudit/audit',
    method: 'post',
    data,
  });
}

// 仓库列表查询
export function listWarehouseByConditionOnPage(data: Record<string, unknown>) {
  return request.post({
    url: '/warehouse/listWarehouseByConditionOnPage',
    method: 'post',
    data,
  });
}
