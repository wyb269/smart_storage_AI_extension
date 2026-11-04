package com.chinaunicom.smartstorage.admin.enumbean;

/**
 * 地面积水监测功能状态枚举。
 *
 * <p>三类状态相互独立，不得互相推导：
 * 设备连接状态、积水告警状态、人工处理状态各管一段。
 * 页面综合展示状态由<b>前端</b> {@code computeDisplayStatus} 计算，
 * 后端不返回合并后的模糊字符串。</p>
 *
 * @author aiVision 功能族 · 地面积水监测
 */
public final class GroundWaterStatusEnum {

    private GroundWaterStatusEnum() {
    }

    /** 设备连接状态 */
    public enum DeviceStatusEnum {
        /** 在线 */
        ONLINE,
        /** 离线 */
        OFFLINE,
        /** 无法判定，页面显示为「未知」而非「离线」 */
        UNKNOWN
    }

    /**
     * 积水告警状态。
     *
     * <p><b>取值必须是 ACTIVE 而不是 ALARM</b>：前端共享层
     * {@code aiVision/shared/displayStatus.ts} 的 {@code isActiveAlarm}
     * 硬编码判定 {@code 'ACTIVE'}，共享层不允许修改。
     * 若这里存 ALARM，左侧网格永远算不出「积水」态。</p>
     */
    public enum WaterAlarmStatusEnum {
        /** 地面干燥，无积水告警 */
        NORMAL,
        /** 积水告警中 */
        ACTIVE,
        /** 已恢复（水已排除） */
        RECOVERED
    }

    /** 积水结论类型 */
    public enum WaterTypeEnum {
        /** 地面积水 */
        PONDING("积水"),
        /** 渗漏 */
        LEAK("渗漏"),
        /** 结冰 */
        ICE("结冰"),
        /** 其他或未知 */
        OTHER("其他");

        private final String label;

        WaterTypeEnum(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /** 算法任务状态 */
    public enum AlgorithmStatusEnum {
        /** 等待识别 */
        QUEUED,
        /** 识别中 */
        RUNNING,
        /** 识别成功 */
        SUCCEEDED,
        /** 识别失败 */
        FAILED,
        /** 识别超时 */
        TIMEOUT,
        /** 无需识别 */
        SKIPPED,
        /** 模拟识别 */
        MOCK
    }

    /** 算法结论；未产出时为 null，不得伪造为 NORMAL */
    public enum AlgorithmResultEnum {
        /** 地面干燥 */
        NORMAL,
        /** 积水异常 */
        ANOMALY,
        /** 无法判断 */
        UNCERTAIN
    }

    /**
     * 人工处理状态。
     *
     * <p>与遮挡那版（库里 PENDING/CLOSED 两态）不同，
     * 本功能按原型建三值：未核实 / 处理中 / 已处理。</p>
     */
    public enum ProcessStatusEnum {
        /** 未核实 */
        PENDING,
        /** 处理中 */
        PROCESSING,
        /** 已处理（终态） */
        DONE;

        /** 是否仍允许被处理；DONE 是终态，不允许重复处理 */
        public boolean isProcessable() {
            return this != DONE;
        }
    }

    /** 视频协议 */
    public enum StreamProtocolEnum {
        HLS, FLV, WEBRTC, MP4
    }

    /** 证据类型 */
    public enum EvidenceTypeEnum {
        /** 积水关键帧 */
        WATER_FRAME,
        /** 干燥参考图 */
        NORMAL_REFERENCE,
        /** 恢复图（水已排除） */
        RECOVERY_IMAGE,
        /** 事件短视频 */
        EVENT_VIDEO
    }
}
