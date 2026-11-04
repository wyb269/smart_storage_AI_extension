/**
 * 车辆智能识别 - 请求与分页模型
 */

import type { EventQueryParams } from '@/pages/aiVision/vehicleRecognition/types';

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
  plateNo?: string;
  vehicleType?: string;
  direction?: string;
  gateName?: string;
  verifyStatus?: string;
  warehouseId?: string;
  pointId?: string;
  startTime?: string;
  endTime?: string;
  limit?: number;
}
