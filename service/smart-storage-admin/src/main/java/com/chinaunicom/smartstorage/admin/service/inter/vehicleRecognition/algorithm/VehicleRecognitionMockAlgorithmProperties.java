package com.chinaunicom.smartstorage.admin.service.inter.vehicleRecognition.algorithm;

/**
 * 车辆识别模拟算法参数。
 *
 * <p>模拟适配器必须支持：正常识别结果、未识别到车辆、无法判断、执行失败、执行超时、
 * 可配置延迟、车辆全景图与车牌特写证据。</p>
 *
 * <p><b>注意</b>：默认值全部使用代码常量，<b>不写入 application.yml</b>，避免修改配置。
 * 需要调整时通过 setter 或后续配置项实现。</p>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
public class VehicleRecognitionMockAlgorithmProperties {

    /** 模拟基础延迟（毫秒），用于演示等待与运行中状态 */
    private long baseDelayMs = 200L;

    /** 强制模拟为「未识别到车辆」 */
    private boolean forceNoVehicle = false;

    /** 强制模拟为无法判断 */
    private boolean forceUncertain = false;

    /** 强制模拟为执行失败 */
    private boolean forceFailed = false;

    /** 强制模拟为执行超时 */
    private boolean forceTimeout = false;

    /** 模拟置信度阈值 */
    private double confidenceThreshold = 0.75d;

    /** 模拟使用的模型名称 */
    private String modelName = "mock-vehicle-recognizer";

    /** 模拟使用的模型版本 */
    private String modelVersion = "mock-1.0.0";

    /** 模拟使用的算法程序版本 */
    private String algorithmVersion = "mock-adapter-1.0.0";

    public long getBaseDelayMs() {
        return baseDelayMs;
    }

    public VehicleRecognitionMockAlgorithmProperties setBaseDelayMs(long baseDelayMs) {
        this.baseDelayMs = baseDelayMs;
        return this;
    }

    public boolean isForceNoVehicle() {
        return forceNoVehicle;
    }

    public VehicleRecognitionMockAlgorithmProperties setForceNoVehicle(boolean forceNoVehicle) {
        this.forceNoVehicle = forceNoVehicle;
        return this;
    }

    public boolean isForceUncertain() {
        return forceUncertain;
    }

    public VehicleRecognitionMockAlgorithmProperties setForceUncertain(boolean forceUncertain) {
        this.forceUncertain = forceUncertain;
        return this;
    }

    public boolean isForceFailed() {
        return forceFailed;
    }

    public VehicleRecognitionMockAlgorithmProperties setForceFailed(boolean forceFailed) {
        this.forceFailed = forceFailed;
        return this;
    }

    public boolean isForceTimeout() {
        return forceTimeout;
    }

    public VehicleRecognitionMockAlgorithmProperties setForceTimeout(boolean forceTimeout) {
        this.forceTimeout = forceTimeout;
        return this;
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public VehicleRecognitionMockAlgorithmProperties setConfidenceThreshold(double confidenceThreshold) {
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
