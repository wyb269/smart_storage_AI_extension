package com.chinaunicom.smartstorage.admin.enumbean;

import lombok.Getter;

/**
 * 仓门异动功能统一状态枚举。
 *
 * <p>严格对齐 AI_HANDOFF.md 第 19 节「统一状态模型」。
 * 每个枚举均提供编码与中文描述，页面标签由前端映射，
 * 后端不得只用布尔值存储人工处理状态（第 19.7 节）。</p>
 *
 * <p>本类为纯枚举定义，不引入任何算法依赖。</p>
 *
 * @author 仓门异动功能
 */
public final class DoorAnomalyStatusEnum {

    private DoorAnomalyStatusEnum() {
    }

    /**
     * 第 19.1 节 设备连接状态。
     */
    @Getter
    public enum DeviceStatusEnum {
        /** 在线 */
        ONLINE("在线"),
        /** 离线 */
        OFFLINE("离线"),
        /** 未知 */
        UNKNOWN("未知");

        private final String description;

        DeviceStatusEnum(String description) {
            this.description = description;
        }

        public static DeviceStatusEnum fromCode(String code) {
            for (DeviceStatusEnum item : values()) {
                if (item.name().equals(code)) {
                    return item;
                }
            }
            return UNKNOWN;
        }
    }

    /**
     * 第 19.2 节 仓门物理状态。
     */
    @Getter
    public enum DoorPhysicalStatusEnum {
        /** 开启 */
        OPEN("开启"),
        /** 关闭 */
        CLOSED("关闭"),
        /** 未知 */
        UNKNOWN("未知");

        private final String description;

        DoorPhysicalStatusEnum(String description) {
            this.description = description;
        }
    }

    /**
     * 第 19.3 节 告警状态。
     */
    @Getter
    public enum AlarmStatusEnum {
        /** 无有效告警 */
        NORMAL("无告警"),
        /** 告警中 */
        ACTIVE("告警中"),
        /** 告警已恢复但事件仍保留 */
        RECOVERED("已恢复");

        private final String description;

        AlarmStatusEnum(String description) {
            this.description = description;
        }
    }

    /**
     * 第 19.5 节 算法任务状态。
     */
    @Getter
    public enum AlgorithmStatusEnum {
        /** 等待执行 */
        QUEUED("等待识别"),
        /** 执行中 */
        RUNNING("识别中"),
        /** 执行成功 */
        SUCCEEDED("识别成功"),
        /** 执行失败 */
        FAILED("识别失败"),
        /** 执行超时 */
        TIMEOUT("识别超时"),
        /** 无需执行 */
        SKIPPED("无需识别"),
        /** 模拟执行结果 */
        MOCK("模拟识别");

        private final String description;

        AlgorithmStatusEnum(String description) {
            this.description = description;
        }
    }

    /**
     * 第 19.6 节 算法结论。
     */
    @Getter
    public enum AlgorithmResultEnum {
        /** 正常 */
        NORMAL("正常"),
        /** 异常 */
        ANOMALY("异常"),
        /** 无法判断 */
        UNCERTAIN("无法判断");

        private final String description;

        AlgorithmResultEnum(String description) {
            this.description = description;
        }
    }

    /**
     * 第 19.7 节 人工处理状态。
     * <p>后端不得只使用布尔值存储。</p>
     */
    @Getter
    public enum ProcessStatusEnum {
        /** 未处理 */
        PENDING("未处理"),
        /** 处理中 */
        PROCESSING("处理中"),
        /** 已核实为真实异常 */
        VERIFIED_TRUE("已核实为真实异常"),
        /** 已核实为误报 */
        VERIFIED_FALSE("已核实为误报"),
        /** 已关闭 */
        CLOSED("已关闭");

        private final String description;

        ProcessStatusEnum(String description) {
            this.description = description;
        }

        /**
         * 是否处于终态（已核实或已关闭）。
         * <p>终态事件不允许再次提交处理（第 16.5 节）。</p>
         */
        public boolean isFinalStatus() {
            return this == VERIFIED_TRUE || this == VERIFIED_FALSE || this == CLOSED;
        }

        /**
         * 是否允许提交人工处理。
         * <p>处理中是否允许继续处理由权限和并发规则决定（第 16.5 节）。</p>
         */
        public boolean isProcessable() {
            return this == PENDING || this == PROCESSING;
        }
    }

    /**
     * 第 19.8 节 异动类型。
     * <p>最终类型编码由算法能力和业务规则确认，
     * 此处为一期占位定义，页面不得以中文字符串作为唯一判断条件。</p>
     */
    @Getter
    public enum AnomalyTypeEnum {
        /** 非法开启 */
        ILLEGAL_OPEN("非法开启"),
        /** 长时间未关闭 */
        LONG_TIME_OPEN("长时间未关闭"),
        /** 异常移动 */
        ABNORMAL_MOVE("异常移动"),
        /** 设备离线 */
        DEVICE_OFFLINE("设备离线"),
        /** 其他或未知 */
        OTHER("其他或未知");

        private final String description;

        AnomalyTypeEnum(String description) {
            this.description = description;
        }

        public static AnomalyTypeEnum fromCode(String code) {
            for (AnomalyTypeEnum item : values()) {
                if (item.name().equals(code)) {
                    return item;
                }
            }
            return OTHER;
        }
    }

    /**
     * 第 20.2 节 视频协议。
     * <p>协议尚未最终确认（第 37 节），本期仅用于承载后端签发结果，
     * 不作为安装播放器依赖的依据。</p>
     */
    @Getter
    public enum StreamProtocolEnum {
        /** HLS，延迟约 3 到 10 秒 */
        HLS("HLS"),
        /** HTTP-FLV，延迟约 1 到 3 秒 */
        FLV("HTTP-FLV"),
        /** WebRTC，通常小于 1 秒 */
        WEBRTC("WebRTC"),
        /** MP4，非实时，用于 Mock 与回放 */
        MP4("MP4");

        private final String description;

        StreamProtocolEnum(String description) {
            this.description = description;
        }
    }

    /**
     * 第 20.5 节 证据类型。
     */
    @Getter
    public enum EvidenceTypeEnum {
        /** 正常参考图 */
        NORMAL_REFERENCE("正常参考图"),
        /** 异常关键帧 */
        ABNORMAL_KEY_FRAME("异常关键帧"),
        /** 异常前一帧 */
        BEFORE_FRAME("异常前一帧"),
        /** 恢复正常图 */
        RECOVERY_IMAGE("恢复正常图"),
        /** 事件短视频 */
        EVENT_VIDEO("事件短视频");

        private final String description;

        EvidenceTypeEnum(String description) {
            this.description = description;
        }
    }
}
