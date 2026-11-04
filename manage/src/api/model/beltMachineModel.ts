/**
 * 皮带机下站人监测 - 请求与分页模型
 */

import type { EventQueryParams } from '@/pages/aiVision/beltMachine/types';

/** 项目统一分页返回结构（result.data / result.total） */
export interface PageResult<T> {
  data: T[];
  total: number;
}

/** 分页查询请求 */
export interface EventPageRequest extends EventQueryParams {}

/** 导出请求 */
export interface EventExportRequest {
  /** 发起导出的页面，取值 HISTORY / RECENT。不能当模板 key 用 */
  source: 'HISTORY' | 'RECENT';
  eventNo?: string;
  warehouseId?: string;
  pointId?: string;
  deviceName?: string;
  cameraCode?: string;
  conclusionType?: string;
  processStatus?: string;
  startTime?: string;
  endTime?: string;
  limit?: number;
}
