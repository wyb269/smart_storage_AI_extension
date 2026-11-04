/**
 * 仓门异动功能 - 枚举常量与显示映射
 *
 * 基线依据：AI_HANDOFF.md 第 19 节「统一状态模型」、
 *          第 16.3 节「颜色语义」、第 17.5 节「算法状态展示」。
 *
 * 原则：
 * 1. 组件内不得散落 if-else 判断状态，统一从此处取标签与颜色。
 * 2. 页面显示状态（第19.4节）必须由 displayStatus.ts 计算得出，
 *    不得依赖后端返回的单一模糊字符串。
 */

import type {
  AlarmStatus,
  AlgorithmResult,
  AlgorithmStatus,
  AnomalyType,
  DeviceStatus,
  DisplayStatus,
  DoorPhysicalStatus,
  EvidenceType,
  PlayerVisibleState,
  ProcessStatus,
  StreamProtocol,
} from '../types';

/* ==================== 枚举值集合 ==================== */

export const DEVICE_STATUS = {
  ONLINE: 'ONLINE',
  OFFLINE: 'OFFLINE',
  UNKNOWN: 'UNKNOWN',
} as const;

export const DOOR_PHYSICAL_STATUS = {
  OPEN: 'OPEN',
  CLOSED: 'CLOSED',
  UNKNOWN: 'UNKNOWN',
} as const;

export const ALARM_STATUS = {
  NORMAL: 'NORMAL',
  ACTIVE: 'ACTIVE',
  RECOVERED: 'RECOVERED',
} as const;

export const ALGORITHM_STATUS = {
  QUEUED: 'QUEUED',
  RUNNING: 'RUNNING',
  SUCCEEDED: 'SUCCEEDED',
  FAILED: 'FAILED',
  TIMEOUT: 'TIMEOUT',
  SKIPPED: 'SKIPPED',
  MOCK: 'MOCK',
} as const;

export const ALGORITHM_RESULT = {
  NORMAL: 'NORMAL',
  ANOMALY: 'ANOMALY',
  UNCERTAIN: 'UNCERTAIN',
} as const;

export const PROCESS_STATUS = {
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  VERIFIED_TRUE: 'VERIFIED_TRUE',
  VERIFIED_FALSE: 'VERIFIED_FALSE',
  CLOSED: 'CLOSED',
} as const;

export const ANOMALY_TYPE = {
  ILLEGAL_OPEN: 'ILLEGAL_OPEN',
  LONG_TIME_OPEN: 'LONG_TIME_OPEN',
  ABNORMAL_MOVE: 'ABNORMAL_MOVE',
  DEVICE_OFFLINE: 'DEVICE_OFFLINE',
  OTHER: 'OTHER',
} as const;

export const STREAM_PROTOCOL = {
  HLS: 'HLS',
  FLV: 'FLV',
  WEBRTC: 'WEBRTC',
  MP4: 'MP4',
} as const;

/* ==================== 中文标签 ==================== */

export const DEVICE_STATUS_LABEL: Record<DeviceStatus, string> = {
  ONLINE: '在线',
  OFFLINE: '离线',
  UNKNOWN: '未知',
};

export const DOOR_PHYSICAL_STATUS_LABEL: Record<DoorPhysicalStatus, string> = {
  OPEN: '开启',
  CLOSED: '关闭',
  UNKNOWN: '未知',
};

export const ALARM_STATUS_LABEL: Record<AlarmStatus, string> = {
  NORMAL: '无告警',
  ACTIVE: '告警中',
  RECOVERED: '已恢复',
};

/** 算法任务状态标签（第17.5节表格「页面表现」列） */
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
  ANOMALY: '异常',
  UNCERTAIN: '无法判断',
};

/**
 * 人工处理状态标签。
 *
 * <p><b>页面只展示两种状态</b>：未处理、已处理。</p>
 *
 * <p>数据库仍保留原有的五种取值（PENDING / PROCESSING / VERIFIED_TRUE /
 * VERIFIED_FALSE / CLOSED），此处仅在<b>显示层</b>归并，
 * 底层值不合并，便于后续追溯与扩展。</p>
 */
export const PROCESS_STATUS_LABEL: Record<ProcessStatus, string> = {
  PENDING: '未处理',
  PROCESSING: '已处理',
  VERIFIED_TRUE: '已处理',
  VERIFIED_FALSE: '已处理',
  CLOSED: '已处理',
};

export const ANOMALY_TYPE_LABEL: Record<AnomalyType, string> = {
  ILLEGAL_OPEN: '非法开启',
  LONG_TIME_OPEN: '长时间未关闭',
  ABNORMAL_MOVE: '异常移动',
  DEVICE_OFFLINE: '设备离线',
  OTHER: '其他或未知',
};

export const EVIDENCE_TYPE_LABEL: Record<EvidenceType, string> = {
  NORMAL_REFERENCE: '正常参考图',
  ABNORMAL_KEY_FRAME: '异常关键帧',
  BEFORE_FRAME: '异常前一帧',
  RECOVERY_IMAGE: '恢复正常图',
  EVENT_VIDEO: '事件短视频',
};

/**
 * 表格中使用的处理状态短标签。
 * 同样只展示两种：未处理、已处理。
 */
export const PROCESS_STATUS_SHORT_LABEL: Record<ProcessStatus, string> = {
  PENDING: '未处理',
  PROCESSING: '已处理',
  VERIFIED_TRUE: '已处理',
  VERIFIED_FALSE: '已处理',
  CLOSED: '已处理',
};

/**
 * 判断处理状态是否属于「已处理」。
 *
 * <p>只有 PENDING（未处理）返回 false，其余一律视为已处理。</p>
 *
 * <p>查看详情后会自动置为已处理，因此仅剩这一处判断入口。</p>
 */
export function isProcessed(status: ProcessStatus): boolean {
  return status !== 'PENDING';
}

/* ==================== 颜色语义（第16.3节） ==================== */

/** 页面综合展示状态的中文文案 */
export const DISPLAY_STATUS_LABEL: Record<DisplayStatus, string> = {
  NORMAL: '正常',
  ABNORMAL: '异常',
  OFFLINE: '离线',
  UNKNOWN: '未知',
};

/**
 * 展示状态的色值与 TDesign 主题跨功能一致，
 * 定义在 aiVision/shared/constants.ts，这里只做再导出。
 * 中文标签（DISPLAY_STATUS_LABEL）留在本模块，
 * 因为同一个 ABNORMAL 在仓门异动叫「异常」、在视频遮挡叫「遮挡」。
 */
export { DISPLAY_STATUS_COLOR, DISPLAY_STATUS_ICON, DISPLAY_STATUS_THEME } from '../../shared/constants';

/**
 * 处理状态标签主题色。
 * 只有未处理是警示色，已处理一律为成功色，与两种状态的语义保持一致。
 */
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

/* ==================== 播放器状态文案（第16.4节） ==================== */

/**
 * 播放器可见状态文案。
 *
 * <p>一期使用原生 {@code <video>}，状态由元素事件驱动，
 * 文案与 {@link PlayerVisibleState} 保持一一对应。</p>
 */
export const PLAYER_STATE_TEXT: Record<PlayerVisibleState, string> = {
  NO_DOOR_SELECTED: '请先选择仓门',
  LOADING: '正在加载视频…',
  PLAYING: '播放中',
  ERROR: '播放失败',
  EXPIRED: '播放地址已过期',
  OFFLINE: '摄像头离线',
  NO_ADDRESS: '暂无可用播放地址',
  UNSUPPORTED_PROTOCOL: '该视频协议暂不支持播放',
};

/**
 * 不支持协议的明确说明（第26.1节）。
 *
 * <p>一期只支持浏览器原生可播放的 MP4 / WebM，
 * 不引入任何播放器依赖；HLS / FLV / WebRTC 需播放器库或转码方案，
 * 属于后续单独确认范围。</p>
 */
export const STREAM_UNSUPPORTED_HINT: Record<StreamProtocol, string> = {
  MP4: '当前协议可正常播放',
  HLS: '一期仅支持 MP4/WebM，HLS 需引入播放器库或转码方案，本期不提供',
  FLV: '一期仅支持 MP4/WebM，HTTP-FLV 需引入播放器库，本期不提供',
  WEBRTC: '一期仅支持 MP4/WebM，WebRTC 需信令服务与专用播放端，本期不提供',
};

/* ==================== 协议说明 ==================== */

export const STREAM_PROTOCOL_LABEL: Record<StreamProtocol, string> = {
  HLS: 'HLS',
  FLV: 'HTTP-FLV',
  WEBRTC: 'WebRTC',
  MP4: 'MP4',
};

/* ==================== 轮询与分页（共享层再导出） ==================== */

/**
 * 轮询间隔、退避参数、分页默认值、最近事件条数
 * 都是跨功能统一的运行参数，定义在 aiVision/shared/constants.ts。
 *
 * 注意键名：POLLING.DOOR_INTERVAL 已泛化为 POLLING.ITEM_INTERVAL，
 * 因为不同功能轮询的对象不叫「仓门」。
 */
export { PAGINATION_DEFAULT, POLLING, RECENT_EVENT_LIMIT } from '../../shared/constants';

/** Mock 阶段仓门数量（第16.3节 / 第37节，仅测试约定） */
export const MOCK_DOOR_COUNT = 16;

/* ==================== 权限标识（第28节） ==================== */
/**
 * 与后端权限编码一一对应。
 * 注意：前端按钮隐藏不能替代后端鉴权（第28节）。
 */
export const PERMISSION_CODE = {
  MONITOR_VIEW: 'doorAnomaly:monitor:view',
  EVENT_VIEW: 'doorAnomaly:event:view',
  EVENT_PROCESS: 'doorAnomaly:event:process',
  EVENT_EXPORT: 'doorAnomaly:event:export',
  ALGORITHM_MANAGE: 'doorAnomaly:algorithm:manage',
} as const;

/* ==================== 处理结果选项 ==================== */

export const PROCESS_RESULT_OPTIONS = [
  { value: 'VERIFIED_TRUE' as const, label: '真实异常' },
  { value: 'VERIFIED_FALSE' as const, label: '误报' },
];
