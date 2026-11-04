/**
 * 皮带机下站人监测功能 - 枚举常量与显示映射
 *
 * 原则：
 * 1. 组件内不得散落 if-else 判断状态，统一从此处取标签与颜色。
 * 2. 页面显示状态必须由 shared/displayStatus 计算得出，
 *    不得依赖后端返回的单一模糊字符串。
 * 3. 色值、主题、轮询间隔、分页默认值取自共享层，这里只放**本功能特有的文案**。
 *    尤其是 ABNORMAL 在本功能叫「报警」，不叫「异常」也不叫「遮挡」。
 */

import { computeDisplayStatus } from '../../shared/displayStatus';
import type {
  AlgorithmResult,
  AlgorithmStatus,
  ConclusionType,
  DisplayStatus,
  MonitorPoint,
  ProcessStatus,
  StreamProtocol,
} from '../types';

/* ==================== 枚举值集合 ==================== */

export const CONCLUSION_TYPE = {
  STANDING_UNDER: 'STANDING_UNDER',
  CROSSING: 'CROSSING',
  LOITERING: 'LOITERING',
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
 * <p><b>与其它 aiVision 功能的差异点</b>：ABNORMAL 在这里显示为「报警」；
 * UNKNOWN 在共享层只有「未知」一档，而本功能的原型把它拆成
 * 「未部署」（没有绑定摄像头）与「未知」（绑了但状态判不出来）两态，
 * 因此卡片文案一律走 {@link pointDisplayLabel}，不要直接索引本表。</p>
 */
export const DISPLAY_STATUS_LABEL: Record<DisplayStatus, string> = {
  NORMAL: '正常',
  ABNORMAL: '报警',
  OFFLINE: '离线',
  UNKNOWN: '未知',
};

/** 「未部署」——共享层的 DisplayStatus 只有 UNKNOWN 一档，拆不出这个语义 */
export const NOT_DEPLOYED_LABEL = '未部署';

export const CONCLUSION_TYPE_LABEL: Record<ConclusionType, string> = {
  STANDING_UNDER: '皮带机下站人',
  CROSSING: '跨越运行设备',
  LOITERING: '区域徘徊',
  OTHER: '其他',
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

/* ==================== TDesign 主题 ==================== */

/**
 * 处理状态主题。
 *
 * 未处理为红色，已处理为绿色；旧 PROCESSING 值按未处理显示。
 */
export const PROCESS_STATUS_THEME = {
  PENDING: 'danger',
  PROCESSING: 'danger',
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
  | 'NOT_DEPLOYED'
  | 'NO_ADDRESS'
  | 'UNSUPPORTED_PROTOCOL',
  string
> = {
  NO_POINT_SELECTED: '请先选择皮带机',
  LOADING: '正在加载视频…',
  PLAYING: '播放中',
  ERROR: '播放失败',
  EXPIRED: '播放地址已过期',
  OFFLINE: '摄像头离线',
  /** 未部署点位没有可播对象，提示要做的是绑摄像头而不是查网络 */
  NOT_DEPLOYED: '该皮带机尚未部署监测摄像头',
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
  { value: 'STANDING_UNDER' as const, label: '皮带机下站人' },
  { value: 'CROSSING' as const, label: '跨越运行设备' },
  { value: 'LOITERING' as const, label: '区域徘徊' },
  { value: 'OTHER' as const, label: '其他' },
];

/* ==================== 展示态派生（共享层不够用的地方） ==================== */

/**
 * 卡片展示态。
 *
 * 共享层的 computeDisplayStatus 只输出 NORMAL/ABNORMAL/OFFLINE/UNKNOWN 四档，
 * 而本功能的原型要求把 UNKNOWN 再拆成「未部署」与「未知」。
 * 共享层禁止修改，因此拆分逻辑放在本功能内：
 * UNKNOWN 且未绑定摄像头 → NOT_DEPLOYED，其余原样透传。
 */
export type BeltCardStatus = DisplayStatus | 'NOT_DEPLOYED';

export function pointDisplayStatus(point: MonitorPoint): BeltCardStatus {
  const status = computeDisplayStatus({
    deviceStatus: point.deviceStatus,
    alarmStatus: point.alarmStatus,
    lastHeartbeatAt: point.lastHeartbeatAt,
  });
  if (status === 'UNKNOWN' && !point.cameraId) return 'NOT_DEPLOYED';
  return status;
}

export function pointDisplayLabel(point: MonitorPoint): string {
  const status = pointDisplayStatus(point);
  return status === 'NOT_DEPLOYED' ? NOT_DEPLOYED_LABEL : DISPLAY_STATUS_LABEL[status];
}

/* ==================== 共享运行参数再导出 ==================== */

export {
  DISPLAY_STATUS_COLOR,
  DISPLAY_STATUS_ICON,
  DISPLAY_STATUS_THEME,
  PAGINATION_DEFAULT,
  POLLING,
  RECENT_EVENT_LIMIT,
} from '../../shared/constants';
