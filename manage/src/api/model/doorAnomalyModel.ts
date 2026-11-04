/**
 * 仓门异动功能 - 接口请求模型
 *
 * 依据 AI_HANDOFF.md 第 21.2 节「查询参数」与项目既有写法整理。
 * 请求体统一以 POST 提交，与项目现有 Controller 风格一致（第21节）。
 */

/** 分页查询请求（继承项目既有分页字段名 pageNo/pageSize） */
export interface EventPageRequest {
  pageNo?: number;
  pageSize?: number;
  eventNo?: string;
  warehouseId?: string;
  doorId?: string;
  cameraCode?: string;
  anomalyType?: string;
  processStatus?: string;
  startTime?: string;
  endTime?: string;
}

/** 项目统一分页返回结构（对应后端 QueryPageResult） */
export interface PageResult<T> {
  data: T[];
  total: number;
}

/** 项目统一返回体外壳（对应后端 ActionResult） */
export interface ActionResultWrapper<T> {
  status: {
    success: boolean;
    code: string;
    message: string;
    requestId?: string;
    timestamp: string;
  };
  result: T;
  attached?: string;
}

/** 导出请求 */
export interface EventExportRequest {
  source: 'RECENT' | 'HISTORY';
  eventNo?: string;
  warehouseId?: string;
  doorId?: string;
  cameraCode?: string;
  anomalyType?: string;
  processStatus?: string;
  startTime?: string;
  endTime?: string;
  /** 导出上限，防止一次拉取过多（第18.5节约5000条阈值） */
  limit?: number;
}
