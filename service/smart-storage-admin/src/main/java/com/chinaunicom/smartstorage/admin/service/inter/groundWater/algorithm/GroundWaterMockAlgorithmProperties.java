package com.chinaunicom.smartstorage.admin.service.inter.groundWater.algorithm;

/**
 * 地面积水模拟算法参数。
 *
 * <p>依据 AI_HANDOFF.md 第 24.4 节，模拟适配器必须支持：
 * 正常结果、异常结果、无法判断、执行失败、执行超时、可配置延迟、
 * 干燥参考图和积水证据图。</p>
 *
 * <p><b>注意</b>：默认值全部使用代码常量，
 * <b>不写入 application.yml</b>，避免修改共享配置。
 * 需要调整时通过 setter 或后续配置项实现。</p>
 *
 * @author aiVision 功能族 · 地面积水监测
 */
public class GroundWaterMockAlgorithmProperties {

    /** 模拟基础延迟（毫秒），用于演示等待与运行中状态 */
    private long baseDelayMs = 200L;

    /** 强制模拟为异常结论 */
    private boolean forceAnomaly = false;

    /** 强制模拟为无法判断 */
    private boolean forceUncertain = false;

    /** 强制模拟为执行失败 */
    private boolean forceFailed = false;

    /** 强制模拟为执行超时 */
    private boolean forceTimeout = false;

    /** 模拟置信度阈值 */
    private double confidenceThreshold = 0.75d;

    /** 模拟使用的模型名称 */
    private String modelName = "mock-ground-water-detector";

    /** 模拟使用的模型版本 */
    private String modelVersion = "mock-1.0.0";

    /** 模拟使用的算法程序版本 */
    private String algorithmVersion = "mock-adapter-1.0.0";

    public long getBaseDelayMs() {
        return baseDelayMs;
    }

    public GroundWaterMockAlgorithmProperties setBaseDelayMs(long baseDelayMs) {
        this.baseDelayMs = baseDelayMs;
        return this;
    }

    public boolean isForceAnomaly() {
        return forceAnomaly;
    }

    public GroundWaterMockAlgorithmProperties setForceAnomaly(boolean forceAnomaly) {
        this.forceAnomaly = forceAnomaly;
        return this;
    }

    public boolean isForceUncertain() {
        return forceUncertain;
    }

    public GroundWaterMockAlgorithmProperties setForceUncertain(boolean forceUncertain) {
        this.forceUncertain = forceUncertain;
        return this;
    }

    public boolean isForceFailed() {
        return forceFailed;
    }

    public GroundWaterMockAlgorithmProperties setForceFailed(boolean forceFailed) {
        this.forceFailed = forceFailed;
        return this;
    }

    public boolean isForceTimeout() {
        return forceTimeout;
    }

    public GroundWaterMockAlgorithmProperties setForceTimeout(boolean forceTimeout) {
        this.forceTimeout = forceTimeout;
        return this;
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public GroundWaterMockAlgorithmProperties setConfidenceThreshold(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
        return this;
    }

    public String getModelName() {
        return modelName;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public String getAlgorithmVersion() {
        return algorithmVersion;
    }
}
