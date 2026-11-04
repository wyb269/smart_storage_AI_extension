/**
 * 视频遮挡检测功能 - 枚举常量与显示映射
 *
 * 原则：
 * 1. 组件内不得散落 if-else 判断状态，统一从此处取标签与颜色。
 * 2. 页面显示状态必须由 shared/displayStatus 计算得出，
 *    不得依赖后端返回的单一模糊字符串。
 * 3. 色值、主题、轮询间隔、分页默认值取自共享层，这里只放**本功能特有的文案**。
 *    尤其是 ABNORMAL 在本功能叫「遮挡」，不叫「异常」。
 */

import type {
  AlgorithmResult,
  AlgorithmStatus,
  DisplayStatus,
  OcclusionType,
  ProcessStatus,
  StreamProtocol,
} from '../types';

/* ==================== 枚举值集合 ==================== */

export const OCCLUSION_TYPE = {
  FULL: 'FULL',
  PARTIAL: 'PARTIAL',
  PAINT: 'PAINT',
  LENS_COVER: 'LENS_COVER',
  OTHER: 'OTHER',
} as const;

export const PROCESS_STATUS = {
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  VERIFIED_TRUE: 'VERIFIED_TRUE',
  VERIFIED_FALSE: 'VERIFIED_FALSE',
  CLOSED: 'CLOSED',
} as const;

/* ==================== 中文标签 ==================== */

/**
 * 页面综合展示状态的中文文案。
 *
 * <p><b>与其它 aiVision 功能的唯一差异点</b>：ABNORMAL 在这里显示为「遮挡」，
 * 仓门异动显示为「异常」，地面积水显示为「积水」。
 * 色值与主题语义相同，因此只有标签需要各自定义。</p>
 */
export const DISPLAY_STATUS_LABEL: Record<DisplayStatus, string> = {
  NORMAL: '正常',
  ABNORMAL: '遮挡',
  OFFLINE: '离线',
  UNKNOWN: '未知',
};

export const OCCLUSION_TYPE_LABEL: Record<OcclusionType, string> = {
  FULL: '全幅遮挡',
  PARTIAL: '部分遮挡',
  PAINT: '镜头污损',
  LENS_COVER: '镜头被遮盖',
  OTHER: '其他或未知',
};

export const ALGORITHM_STATUS_LABEL: Record<AlgorithmStatus, string> = {
  QUEUED: '等待识别',
  RUNNING: '识别中',
  SUCCEEDED: '识别成功',
  FAILED: '识别失败',
  TIMEOUT: '识别超时',
  SKIPPED: '无需识别',
  MOCK: '识别完成',
};

export const ALGORITHM_RESULT_LABEL: Record<AlgorithmResult, string> = {
  NORMAL: '正常',
  ANOMALY: '遮挡',
  UNCERTAIN: '无法判断',
};

/**
 * 人工处理状态标签。
 *
 * <p>页面只展示两种状态：未处理、已处理。
 * 底层五种取值不合并，便于后续追溯与扩展。</p>
 */
export const PROCESS_STATUS_LABEL: Record<ProcessStatus, string> = {
  PENDING: '未处理',
  PROCESSING: '已处理',
  VERIFIED_TRUE: '已处理',
  VERIFIED_FALSE: '已处理',
  CLOSED: '已处理',
};

/** 判断处理状态是否属于「已处理」；只有 PENDING 返回 false */
export function isProcessed(status: ProcessStatus): boolean {
  return status !== 'PENDING';
}

/* ==================== TDesign 主题 ==================== */

/** 未处理为警示色，已处理一律成功色，与两态语义一致 */
export const PROCESS_STATUS_THEME = {
  PENDING: 'warning',
  PROCESSING: 'success',
  VERIFIED_TRUE: 'success',
  VERIFIED_FALSE: 'success',
  CLOSED: 'success',
} as const satisfies Record<ProcessStatus, string>;

export const ALGORITHM_STATUS_THEME = {
  QUEUED: 'default',
  RUNNING: 'primary',
  SUCCEEDED: 'success',
  FAILED: 'danger',
  TIMEOUT: 'danger',
  SKIPPED: 'default',
  MOCK: 'warning',
} as const satisfies Record<AlgorithmStatus, string>;

export const ALGORITHM_RESULT_THEME = {
  NORMAL: 'success',
  ANOMALY: 'danger',
  UNCERTAIN: 'warning',
} as const satisfies Record<AlgorithmResult, string>;

/* ==================== 播放器文案 ==================== */

export const PLAYER_STATE_TEXT: Record<
  | StreamProtocol
  | 'NO_CAMERA_SELECTED'
  | 'LOADING'
  | 'PLAYING'
  | 'ERROR'
  | 'EXPIRED'
  | 'OFFLINE'
  | 'NO_ADDRESS'
  | 'UNSUPPORTED_PROTOCOL',
  string
> = {
  NO_CAMERA_SELECTED: '请先选择摄像头',
  LOADING: '正在加载视频…',
  PLAYING: '播放中',
  ERROR: '播放失败',
  EXPIRED: '播放地址已过期',
  OFFLINE: '摄像头离线',
  NO_ADDRESS: '暂无可用播放地址',
  UNSUPPORTED_PROTOCOL: '该视频协议暂不支持播放',
  MP4: '当前协议可正常播放',
  HLS: '一期仅支持 MP4/WebM，HLS 需引入播放器库或转码方案，本期不提供',
  FLV: '一期仅支持 MP4/WebM，HTTP-FLV 需引入播放器库，本期不提供',
  WEBRTC: '一期仅支持 MP4/WebM，WebRTC 需信令服务与专用播放端，本期不提供',
};

/* ==================== 下拉选项 ==================== */

export const PROCESS_STATUS_OPTIONS = [
  { value: 'PENDING' as const, label: '未处理' },
  { value: 'PROCESSED' as const, label: '已处理' },
];

export const OCCLUSION_TYPE_OPTIONS = [
  { value: 'FULL' as const, label: '全幅遮挡' },
  { value: 'PARTIAL' as const, label: '部分遮挡' },
  { value: 'PAINT' as const, label: '镜头污损' },
  { value: 'LENS_COVER' as const, label: '镜头被遮盖' },
  { value: 'OTHER' as const, label: '其他或未知' },
];

/* ==================== 共享运行参数再导出 ==================== */

export {
  DISPLAY_STATUS_COLOR,
  DISPLAY_STATUS_ICON,
  DISPLAY_STATUS_THEME,
  PAGINATION_DEFAULT,
  POLLING,
  RECENT_EVENT_LIMIT,
} from '../../shared/constants';
