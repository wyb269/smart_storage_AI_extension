/**
 * 周界入侵识别功能 - 枚举常量与显示映射
 *
 * 原则：
 * 1. 组件内不得散落 if-else 判断状态，统一从此处取标签与颜色。
 * 2. 页面显示状态必须由本功能的 usePerimeterDisplayStatus 计算，
 *    不得依赖后端返回的单一模糊字符串。
 * 3. 色值、主题、轮询间隔、分页默认值取自共享层，这里只放**本功能特有的文案**。
 *    尤其是共享层的四态在这里分别是「正常布防 / 入侵报警 / 离线 / 未知」，
 *    并且本功能多出一个第五态「未布防」。
 */

import type {
  AlgorithmResult,
  AlgorithmStatus,
  DefenseStatus,
  IntrusionType,
  PerimeterDisplayStatus,
  ProcessStatus,
  StreamProtocol,
  Zone,
} from '../types';

/* ==================== 枚举值集合 ==================== */

export const INTRUSION_TYPE = {
  CLIMB: 'CLIMB',
  CROSS: 'CROSS',
  INTRUSION: 'INTRUSION',
  LOITER: 'LOITER',
  OTHER: 'OTHER',
} as const;

export const PROCESS_STATUS = {
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  DONE: 'DONE',
} as const;

export const ZONE = {
  EAST: 'EAST',
  WEST: 'WEST',
  SOUTH: 'SOUTH',
  NORTH: 'NORTH',
} as const;

export const DEFENSE_STATUS = {
  ARMED: 'ARMED',
  DISARMED: 'DISARMED',
} as const;

/* ==================== 中文标签 ==================== */

/**
 * 页面综合展示状态的中文文案。
 *
 * <p><b>与其它 aiVision 功能的差异点</b>：
 * NORMAL 在这里叫「正常布防」（遮挡那版叫「正常」），
 * ABNORMAL 叫「入侵报警」（遮挡那版叫「遮挡」），
 * 并且多出一个共享层没有的 DISARMED = 「未布防」。</p>
 */
export const DISPLAY_STATUS_LABEL: Record<PerimeterDisplayStatus, string> = {
  NORMAL: '正常布防',
  ABNORMAL: '入侵报警',
  DISARMED: '未布防',
  OFFLINE: '离线',
  UNKNOWN: '未知',
};

/**
 * 展示状态的 TDesign tag 主题。
 *
 * <p>共享层的 DISPLAY_STATUS_THEME 只覆盖四个键，直接拿来索引
 * PerimeterDisplayStatus 会缺 DISARMED，因此本功能自带一份完整映射。
 * 「未布防」用 warning（橙）而不是 default（灰）：灰是「离线」的语义色，
 * 把「没设防」画成灰色会让人以为设备掉了，两种风险完全不同。</p>
 */
export const DISPLAY_STATUS_THEME = {
  NORMAL: 'success',
  ABNORMAL: 'danger',
  DISARMED: 'warning',
  OFFLINE: 'default',
  UNKNOWN: 'warning',
} as const satisfies Record<PerimeterDisplayStatus, string>;

/** 展示状态色值，与主题语义一致 */
export const DISPLAY_STATUS_COLOR: Record<PerimeterDisplayStatus, string> = {
  NORMAL: '#00a870',
  ABNORMAL: '#d54941',
  DISARMED: '#e37318',
  OFFLINE: '#86909c',
  UNKNOWN: '#c9cdd4',
};

/** 展示状态图标；DISARMED 用「锁」表达人工撤防，与设备类图标区分开 */
export const DISPLAY_STATUS_ICON: Record<PerimeterDisplayStatus, string> = {
  NORMAL: 'secured',
  ABNORMAL: 'error-circle',
  DISARMED: 'lock-on',
  OFFLINE: 'poweroff',
  UNKNOWN: 'help-circle',
};

export const DEFENSE_STATUS_LABEL: Record<DefenseStatus, string> = {
  ARMED: '已布防',
  DISARMED: '未布防',
};

export const ZONE_LABEL: Record<Zone, string> = {
  EAST: '东区',
  WEST: '西区',
  SOUTH: '南区',
  NORTH: '北区',
};

export const INTRUSION_TYPE_LABEL: Record<IntrusionType, string> = {
  CLIMB: '攀爬',
  CROSS: '翻越',
  INTRUSION: '闯入',
  LOITER: '徘徊',
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
  ANOMALY: '入侵',
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

/** 判断处理状态是否已终态 */
export function isProcessed(status: ProcessStatus): boolean {
  return status === 'DONE';
}

/* ==================== TDesign 主题 ==================== */

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
  | 'DISARMED'
  | 'NO_ADDRESS'
  | 'UNSUPPORTED_PROTOCOL',
  string
> = {
  NO_POINT_SELECTED: '请先选择防区段',
  LOADING: '正在加载视频…',
  PLAYING: '播放中',
  ERROR: '播放失败',
  EXPIRED: '播放地址已过期',
  OFFLINE: '防区段摄像头离线',
  /** 撤防期间不提供实时画面，文案要说明是「人没设防」而不是「设备坏了」 */
  DISARMED: '该防区段已撤防，暂不提供实时画面',
  NO_ADDRESS: '暂无可用播放地址',
  UNSUPPORTED_PROTOCOL: '该视频协议暂不支持播放',
  MP4: '当前协议可正常播放',
  HLS: '一期仅支持 MP4/WebM，HLS 需引入播放器库或转码方案，本期不提供',
  FLV: '一期仅支持 MP4/WebM，HTTP-FLV 需引入播放器库，本期不提供',
  WEBRTC: '一期仅支持 MP4/WebM，WebRTC 需信令服务与专用播放端，本期不提供',
};

/* ==================== 下拉选项 ==================== */

/** 防区筛选下拉：第一项「全部防区」用空串表示，与后端 zone 缺省语义一致 */
export const ZONE_OPTIONS: Array<{ value: Zone | ''; label: string }> = [
  { value: '', label: '全部防区' },
  { value: 'EAST', label: '东区' },
  { value: 'WEST', label: '西区' },
  { value: 'SOUTH', label: '南区' },
  { value: 'NORTH', label: '北区' },
];

export const PROCESS_STATUS_OPTIONS: Array<{ value: ProcessStatus; label: string }> = [
  { value: 'PENDING', label: '未处理' },
  { value: 'DONE', label: '已处理' },
];

export const INTRUSION_TYPE_OPTIONS: Array<{ value: IntrusionType; label: string }> = [
  { value: 'CLIMB', label: '攀爬' },
  { value: 'CROSS', label: '翻越' },
  { value: 'INTRUSION', label: '闯入' },
  { value: 'LOITER', label: '徘徊' },
  { value: 'OTHER', label: '其他' },
];

/* ==================== 共享运行参数再导出 ==================== */

export {
  HEARTBEAT_TIMEOUT_MINUTES,
  HEARTBEAT_TIMEOUT_MS,
  PAGINATION_DEFAULT,
  POLLING,
  RECENT_EVENT_LIMIT,
} from '../../shared/constants';
