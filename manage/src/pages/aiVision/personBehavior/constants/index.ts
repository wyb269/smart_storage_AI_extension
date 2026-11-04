/**
 * 人员行为监测功能 - 枚举常量与显示映射
 *
 * 原则：
 * 1. 组件内不得散落 if-else 判断状态，统一从此处取标签与颜色。
 * 2. 页面显示状态必须由 shared/displayStatus 计算得出，
 *    不得依赖后端返回的单一模糊字符串。
 * 3. 色值、主题、轮询间隔、分页默认值取自共享层，这里只放**本功能特有的文案**。
 *    尤其是 ABNORMAL 在本功能要按告警行为细分成「人员聚集」「未戴安全帽」等。
 */

import type {
  AlgorithmResult,
  AlgorithmStatus,
  BehaviorType,
  EvidenceType,
  ProcessStatus,
  StreamProtocol,
} from '../types';

/* ==================== 枚举值集合 ==================== */

export const BEHAVIOR_TYPE = {
  CROWD: 'CROWD',
  NO_HELMET: 'NO_HELMET',
  FALL: 'FALL',
  INTRUSION: 'INTRUSION',
  OTHER: 'OTHER',
} as const;

export const PROCESS_STATUS = {
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  DONE: 'DONE',
} as const;

/* ==================== 中文标签 ==================== */

/**
 * 共享基座算出的四态兜底文案。
 *
 * <p><b>与其它 aiVision 功能的差异点</b>：ABNORMAL 在这里不直接展示，
 * 而是先尝试用 {@link BEHAVIOR_TYPE_LABEL[alarmBehaviorType]} 还原成
 * 「人员聚集」「未戴安全帽」这类具体行为，取不到才退回本表的「行为异常」。
 * 基座只有一个 ABNORMAL，区分不了原型上的多种异常文案。</p>
 */
export const DISPLAY_STATUS_LABEL: Record<'NORMAL' | 'ABNORMAL' | 'OFFLINE' | 'UNKNOWN', string> = {
  NORMAL: '正常',
  ABNORMAL: '行为异常',
  OFFLINE: '离线',
  UNKNOWN: '未知',
};

export const BEHAVIOR_TYPE_LABEL: Record<BehaviorType, string> = {
  CROWD: '人员聚集',
  NO_HELMET: '未戴安全帽',
  FALL: '倒地',
  INTRUSION: '区域闯入',
  OTHER: '其他',
};

/** 证据类型文案 */
export const EVIDENCE_TYPE_LABEL: Record<EvidenceType, string> = {
  BEHAVIOR_FRAME: '行为异常帧',
  NORMAL_REFERENCE: '正常参考图',
  RECOVERY_IMAGE: '恢复图',
  EVENT_VIDEO: '事件短视频',
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
  ANOMALY: '行为异常',
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

/** 判断处理状态是否已终结；只有 DONE 返回 true */
export function isTerminal(status: ProcessStatus): boolean {
  return status === 'DONE';
}

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

/**
 * 异常卡片按行为类型着色。
 *
 * <p>原型上「人员聚集」是橙色、「未戴安全帽」是红色，两者都是 ABNORMAL，
 * 基座给不了这个区分，只能在本功能自己的常量里补。</p>
 */
export const BEHAVIOR_TYPE_CARD_CLASS: Record<BehaviorType, string> = {
  CROWD: 'CROWD',
  NO_HELMET: 'NO_HELMET',
  FALL: 'NO_HELMET',
  INTRUSION: 'NO_HELMET',
  OTHER: 'CROWD',
};

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
  NO_POINT_SELECTED: '请先选择监测区域',
  LOADING: '正在加载视频…',
  PLAYING: '播放中',
  ERROR: '播放失败',
  EXPIRED: '播放地址已过期',
  OFFLINE: '区域摄像头离线',
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

export const BEHAVIOR_TYPE_OPTIONS = [
  { value: 'CROWD' as const, label: '人员聚集' },
  { value: 'NO_HELMET' as const, label: '未戴安全帽' },
  { value: 'FALL' as const, label: '倒地' },
  { value: 'INTRUSION' as const, label: '区域闯入' },
  { value: 'OTHER' as const, label: '其他' },
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
