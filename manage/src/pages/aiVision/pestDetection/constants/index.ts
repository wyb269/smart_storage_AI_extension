/**
 * 粮面虫害智能识别功能 - 枚举常量与显示映射
 *
 * 原则：
 * 1. 组件内不得散落 if-else 判断状态，统一从此处取标签与颜色。
 * 2. 页面显示状态必须由 shared/displayStatus 计算得出，
 *    不得依赖后端返回的单一模糊字符串。
 * 3. 色值、主题、轮询间隔、分页默认值取自共享层，这里只放**本功能特有的文案**。
 *    尤其是 ABNORMAL 在本功能叫「告警」。
 */

import type {
  AlgorithmResult,
  AlgorithmStatus,
  ConclusionType,
  DisplayStatus,
  ProcessStatus,
  StreamProtocol,
} from '../types';

/* ==================== 枚举值集合 ==================== */

export const CONCLUSION_TYPE = {
  GRAIN_INSECT: 'GRAIN_INSECT',
  DENSITY_HIGH: 'DENSITY_HIGH',
  MOLD: 'MOLD',
  OTHER: 'OTHER',
} as const;

export const PROCESS_STATUS = {
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  DONE: 'DONE',
} as const;

/* ==================== 中文标签 ==================== */

/**
 * 页面综合展示状态的中文文案。
 *
 * 与其它 aiVision 功能的差异点：ABNORMAL 在这里显示为「告警」，
 * 与原型右上角图例（正常 / 告警）一致。色值与主题语义相同，因此只有标签需要各自定义。
 */
export const DISPLAY_STATUS_LABEL: Record<DisplayStatus, string> = {
  NORMAL: '正常',
  ABNORMAL: '告警',
  OFFLINE: '离线',
  UNKNOWN: '未知',
};

export const CONCLUSION_TYPE_LABEL: Record<ConclusionType, string> = {
  GRAIN_INSECT: '粮面害虫',
  DENSITY_HIGH: '密度超标',
  MOLD: '霉变',
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
  ANOMALY: '虫情异常',
  UNCERTAIN: '无法判断',
};

/**
 * 人工处理状态标签。兼容库中的旧 PROCESSING 值，页面只展示未处理和已处理。
 */
export const PROCESS_STATUS_LABEL: Record<ProcessStatus, string> = {
  PENDING: '未处理',
  PROCESSING: '未处理',
  DONE: '已处理',
};

/* ==================== TDesign 主题 ==================== */

/** 未处理为警示色，已处理为成功色 */
export const PROCESS_STATUS_THEME = {
  PENDING: 'warning',
  PROCESSING: 'warning',
  DONE: 'success',
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
  | 'NO_POINT_SELECTED'
  | 'LOADING'
  | 'PLAYING'
  | 'ERROR'
  | 'EXPIRED'
  | 'OFFLINE'
  | 'NO_ADDRESS'
  | 'UNSUPPORTED_PROTOCOL',
  string
> = {
  NO_POINT_SELECTED: '请先选择粮面点位',
  LOADING: '正在加载视频…',
  PLAYING: '播放中',
  ERROR: '播放失败',
  EXPIRED: '播放地址已过期',
  OFFLINE: '粮面摄像头离线',
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
  { value: 'DONE' as const, label: '已处理' },
];

export const CONCLUSION_TYPE_OPTIONS = [
  { value: 'GRAIN_INSECT' as const, label: '粮面害虫' },
  { value: 'DENSITY_HIGH' as const, label: '密度超标' },
  { value: 'MOLD' as const, label: '霉变' },
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
