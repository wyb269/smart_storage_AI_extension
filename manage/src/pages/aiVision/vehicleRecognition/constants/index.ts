/**
 * 车辆智能识别功能 - 枚举常量与显示映射
 *
 * 原则：
 * 1. 组件内不得散落 if-else 判断状态，统一从此处取标签与颜色。
 * 2. 页面显示状态必须由 shared/displayStatus 计算得出，
 *    不得依赖后端返回的单一模糊字符串。
 * 3. 色值、主题、轮询间隔、分页默认值取自共享层，这里只放**本功能特有的文案**。
 *    车辆功能的展示态是「入库 / 出库」方向与「待核实 / 已核实 / 误报」核实态。
 */

import type { AlgorithmStatus, Direction, EvidenceType, StreamProtocol, VerifyStatus } from '../types';

/* ==================== 枚举值集合 ==================== */

export const DIRECTION = {
  IN: 'IN',
  OUT: 'OUT',
} as const;

export const VERIFY_STATUS = {
  PENDING: 'PENDING',
  VERIFIED: 'VERIFIED',
  REJECTED: 'REJECTED',
} as const;

/* ==================== 中文标签 ==================== */

export const DIRECTION_LABEL: Record<Direction, string> = {
  IN: '入库',
  OUT: '出库',
};

/**
 * 核实状态标签。
 *
 * 原型只画了「已核实 / 待核实」两态；REJECTED（误报）是为幂等与后续误报处理链路
 * 预留的第三个取值，本期没有写入路径，但标签与主题都补齐，避免将来出现空文案。
 */
export const VERIFY_STATUS_LABEL: Record<VerifyStatus, string> = {
  PENDING: '未处理',
  VERIFIED: '已处理',
  REJECTED: '已处理',
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

export const EVIDENCE_TYPE_LABEL: Record<EvidenceType, string> = {
  VEHICLE_IMAGE: '车辆全景图',
  PLATE_CROP: '车牌特写图',
  ANNOTATED: '算法标注图',
};

/* ==================== TDesign 主题 ==================== */

/**
 * 行驶方向主题：入库成功色、出库警示色，与原型卡片的绿/橙 tag 一致。
 */
export const DIRECTION_THEME = {
  IN: 'success',
  OUT: 'warning',
} as const satisfies Record<Direction, string>;

/**
 * 处理状态主题：未处理为警示色，已处理为成功色。
 */
export const VERIFY_STATUS_THEME = {
  PENDING: 'warning',
  VERIFIED: 'success',
  REJECTED: 'success',
} as const satisfies Record<VerifyStatus, string>;

export const ALGORITHM_STATUS_THEME = {
  QUEUED: 'default',
  RUNNING: 'primary',
  SUCCEEDED: 'success',
  FAILED: 'danger',
  TIMEOUT: 'danger',
  SKIPPED: 'default',
  MOCK: 'warning',
} as const satisfies Record<AlgorithmStatus, string>;

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
  NO_POINT_SELECTED: '请先选择一条识别记录',
  LOADING: '正在加载视频…',
  PLAYING: '播放中',
  ERROR: '播放失败',
  EXPIRED: '播放地址已过期',
  OFFLINE: '识别点位摄像头离线',
  NO_ADDRESS: '暂无可用播放地址',
  UNSUPPORTED_PROTOCOL: '该视频协议暂不支持播放',
  MP4: '当前协议可正常播放',
  HLS: '一期仅支持 MP4/WebM，HLS 需引入播放器库或转码方案，本期不提供',
  FLV: '一期仅支持 MP4/WebM，HTTP-FLV 需引入播放器库，本期不提供',
  WEBRTC: '一期仅支持 MP4/WebM，WebRTC 需信令服务与专用播放端，本期不提供',
};

/* ==================== 下拉选项 ==================== */

export const DIRECTION_OPTIONS = [
  { value: 'IN' as const, label: '入库' },
  { value: 'OUT' as const, label: '出库' },
];

export const VERIFY_STATUS_OPTIONS = [
  { value: 'PENDING' as const, label: '未处理' },
  { value: 'VERIFIED' as const, label: '已处理' },
];

export const VEHICLE_TYPE_OPTIONS = [
  { value: '柴油货车', label: '柴油货车' },
  { value: '空车', label: '空车' },
  { value: '客车车型', label: '客车车型' },
  { value: '新能源轿车', label: '新能源轿车' },
  { value: '未识别车型', label: '未识别车型' },
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
