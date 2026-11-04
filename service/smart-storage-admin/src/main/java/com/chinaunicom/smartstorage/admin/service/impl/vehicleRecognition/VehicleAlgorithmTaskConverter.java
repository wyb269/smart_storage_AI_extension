package com.chinaunicom.smartstorage.admin.service.impl.vehicleRecognition;

import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.algorithm.VehicleRecognitionAlgorithmTaskOutputDTO;
import com.chinaunicom.smartstorage.admin.entity.vehicleRecognition.VehicleRecognitionAlgorithmTask;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 算法任务实体 ⇄ 响应 DTO 的转换与算法 JSON 落地工具。
 *
 * <p>之所以单独成类：{@code MyBatisVehicleRecognitionRepository} 与
 * {@code InMemoryVehicleRecognitionRepository} 两条数据通路都要做同样的转换，
 * 复制一份会让真实库与内存兜底模式返回不同形状（这正是页面「切数据源就出错」的来源）。</p>
 *
 * <p><b>PII</b>：{@code detections} / {@code raw_result_ref} 里绝不写完整车牌原文，
 * 只写掩码（{@link #maskPlate}）与事件引用；本类是唯一的写入口，
 * 车牌原文在这里就被替换掉。日志同样只打掩码。</p>
 *
 * <p>JSON 一律用 Jackson（Spring Boot 已带）生成，不手拼字符串：
 * 掩码与检测框都可能含分隔符，手拼容易写出非法 JSON 让页面解析失败。</p>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
@Slf4j
final class VehicleAlgorithmTaskConverter {

    /** 与 Mapper 里 to_char 输出、Mock 响应一致的时间格式 */
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 检出目标数：按 detections 数组元素计数 */
    private static final String KEY_CATEGORY = "category";

    private VehicleAlgorithmTaskConverter() {
    }

    /* ==================== 状态推进 ==================== */

    /** 是否终态：终态任务不再向算法适配层查询 */
    static boolean isTerminal(String executionStatus) {
        if (executionStatus == null) {
            return false;
        }
        return switch (executionStatus) {
            case "SUCCEEDED", "FAILED", "TIMEOUT", "SKIPPED", "MOCK" -> true;
            default -> false;
        };
    }

    /**
     * 状态等级，用于<b>禁止状态回退</b>。
     *
     * <p>真实场景：历史种子里的 RUNNING 任务标识不在算法适配层的内存任务表中，
     * {@code queryTask} 会返回兜底的 QUEUED。若无条件覆盖，页面上「识别中」
     * 会退回「等待识别」，看起来像轮询把数据改坏了。</p>
     */
    static int rank(String executionStatus) {
        if (executionStatus == null) {
            return 0;
        }
        return switch (executionStatus) {
            case "QUEUED" -> 1;
            case "RUNNING" -> 2;
            // 三种结束态之间不存在先后，统一给同一等级，由「不回退」规则挡住
            case "SUCCEEDED", "FAILED", "TIMEOUT", "SKIPPED", "MOCK" -> 3;
            default -> 0;
        };
    }

    /** 判断能否把状态从 current 推进到 next（只进不退） */
    static boolean canAdvance(String current, String next) {
        return next != null && rank(next) >= rank(current);
    }

    /* ==================== 车牌脱敏 ==================== */

    /**
     * 车牌掩码：保留前 2 位与后 2 位，中间以 {@code ***} 代替。
     *
     * <p>与 {@code VehicleRecognitionServiceImpl#maskPlate} 同一口径（本方法是唯一实现，
     * Service 委托过来），保证日志、算法任务落库、接口响应三处掩码一致。
     * 过短车牌（脏数据）返回 {@code ***}，绝不回退成原值。</p>
     */
    static String maskPlate(String plateNo) {
        if (plateNo == null || plateNo.length() < 5) {
            return "***";
        }
        return plateNo.substring(0, 2) + "***" + plateNo.substring(plateNo.length() - 2);
    }

    /* ==================== 时间 ==================== */

    static String format(LocalDateTime value) {
        return value == null ? null : TS.format(value);
    }

    /** 解析算法适配层回传的时间字符串；解析失败返回 null（宁可少一个时间戳也不要 500） */
    static LocalDateTime parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.replace(' ', 'T'), TS);
        } catch (Exception e) {
            log.warn("[VehicleRecognition] 算法任务时间格式无法解析，已忽略该字段");
            return null;
        }
    }

    /* ==================== JSON 落地 ==================== */

    /** 序列化为 JSON 字符串；失败返回 null，由调用方按「该列留空」处理 */
    static String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("[VehicleRecognition] 算法任务 JSON 序列化失败，已留空: {}", e.getMessage());
            return null;
        }
    }

    /** 媒体引用数组 JSON；元素必须是受控内部引用（由调用方保证） */
    static String mediaRefsJson(List<String> refs) {
        return toJson(refs == null ? List.of() : refs);
    }

    /**
     * 把算法输出转成 {@code detections} JSON。
     *
     * <p>本功能的算法职责是三件事，因此产物固定包含三类条目：
     * 车辆目标框（含车型与方向）+ 车牌条目。车牌条目<b>只写掩码与事件引用</b>，
     * 不写原文；车牌读不出来时写 {@code readable=false} 且不带掩码。</p>
     *
     * @param output  算法统一输出（其 {@code plateNo} 原文在此处被掩码替换，不外泄）
     * @param eventId 关联识别记录标识，用作车牌引用（替代车牌原文做定位）
     */
    static String detectionsJson(VehicleRecognitionAlgorithmTaskOutputDTO output, String eventId) {
        if (output == null) {
            return null;
        }
        List<Map<String, Object>> items = new ArrayList<>();

        if (output.getDetections() != null) {
            for (VehicleRecognitionAlgorithmTaskOutputDTO.DetectionDTO detection : output.getDetections()) {
                if (detection == null) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put(KEY_CATEGORY, detection.getCategory());
                item.put("confidence", detection.getConfidence());
                item.put("bbox", bboxNode(detection.getBbox()));
                item.put("track_id", detection.getTrackId());
                // 车型与方向挂在车辆目标上，便于「三件事」在一张表里自洽核对
                item.put("vehicle_type", output.getVehicleType());
                item.put("direction", output.getDirection());
                items.add(item);
            }
        }

        // 车牌条目：始终产出，读不出来也要留下「尝试过但失败」的痕迹
        Map<String, Object> plate = new LinkedHashMap<>();
        plate.put(KEY_CATEGORY, "plate");
        boolean plateReadable = output.getPlateNo() != null && !output.getPlateNo().isBlank();
        plate.put("confidence", plateReadable ? output.getConfidence() : null);
        plate.put("bbox", List.of(210, 300, 352, 346));
        plate.put("readable", plateReadable);
        plate.put("plate_ref", "event:" + eventId);
        if (plateReadable) {
            plate.put("plate_masked", maskPlate(output.getPlateNo()));
        }
        items.add(plate);

        return toJson(items);
    }

    /** 证据引用 JSON：只落受控对象存储键，前端永不直接解析 */
    static String evidenceRefsJson(VehicleRecognitionAlgorithmTaskOutputDTO output) {
        if (output == null || output.getEvidenceRefs() == null) {
            return null;
        }
        List<Map<String, Object>> refs = new ArrayList<>();
        for (VehicleRecognitionAlgorithmTaskOutputDTO.EvidenceRefDTO ref : output.getEvidenceRefs()) {
            if (ref == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("evidence_type", ref.getEvidenceType());
            item.put("object_key", ref.getObjectKey());
            item.put("captured_at", ref.getCapturedAt());
            refs.add(item);
        }
        return toJson(refs);
    }

    /**
     * 构造历史任务的 {@code detections} JSON（内存兜底模式的种子任务用）。
     *
     * <p>与真实库的 {@code 07_seed_algorithm_tasks.sql} 输出同形：
     * 车辆目标框（挂车型与方向）+ 车牌条目，共 2 个检出目标。
     * <b>车牌只写掩码与事件引用</b>，与 SQL 侧的 {@code left()/right()} 掩码一致。</p>
     *
     * @param plateReadable false 时对应 {@code PLATE_UNREADABLE} 场景，不写掩码
     */
    static String historyDetectionsJson(String eventId, String vehicleType, String direction,
                                        BigDecimal vehicleConfidence, String plateMasked,
                                        BigDecimal plateConfidence, boolean plateReadable) {
        Map<String, Object> vehicle = new LinkedHashMap<>();
        vehicle.put(KEY_CATEGORY, "vehicle");
        vehicle.put("confidence", vehicleConfidence);
        vehicle.put("bbox", List.of(120, 80, 460, 420));
        vehicle.put("track_id", "TRK-" + eventId);
        vehicle.put("vehicle_type", vehicleType);
        vehicle.put("direction", direction);

        Map<String, Object> plate = new LinkedHashMap<>();
        plate.put(KEY_CATEGORY, "plate");
        plate.put("confidence", plateReadable ? plateConfidence : null);
        plate.put("bbox", List.of(210, 300, 352, 346));
        plate.put("readable", plateReadable);
        plate.put("plate_ref", "event:" + eventId);
        if (plateReadable) {
            plate.put("plate_masked", plateMasked);
        }

        return toJson(List.of(vehicle, plate));
    }

    /** 检测框：适配层给的是字符串，尽量还原成 JSON 数组；失败则原样保留字符串 */
    private static Object bboxNode(String bbox) {
        if (bbox == null || bbox.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(bbox);
        } catch (Exception e) {
            return bbox;
        }
    }

    /* ==================== 实体 → 响应 ==================== */

    /**
     * 实体转响应 DTO。
     *
     * <p>{@code detectedCount}、{@code plateMasked}、{@code plateRef} 从
     * {@code detections} JSON 现算，不落冗余列；解析失败按 0 / null 处理，
     * 不让一条脏 JSON 把整个详情打挂。</p>
     */
    static VehicleAlgorithmTaskRespDTO toResp(VehicleRecognitionAlgorithmTask task) {
        if (task == null) {
            return null;
        }
        VehicleAlgorithmTaskRespDTO dto = new VehicleAlgorithmTaskRespDTO();
        dto.setTaskId(task.getTaskNo());
        dto.setTaskNo(task.getTaskNo());
        dto.setEventId(task.getEventId() == null ? null : String.valueOf(task.getEventId()));
        dto.setPointId(task.getPointId() == null ? null : String.valueOf(task.getPointId()));
        dto.setTaskType(task.getTaskType());
        dto.setExecutionStatus(task.getExecutionStatus());
        dto.setConclusion(task.getConclusion());
        dto.setConfidence(task.getConfidence());
        dto.setModelName(task.getModelName());
        dto.setModelVersion(task.getModelVersion());
        dto.setAlgorithmVersion(task.getAlgorithmVersion());
        dto.setConfigVersion(task.getConfigVersion());
        dto.setInferenceDurationMs(task.getInferenceDurationMs());
        dto.setStartedAt(format(task.getStartedAt()));
        dto.setFinishedAt(format(task.getFinishedAt()));
        dto.setSimulated(task.getIsSimulated() == null ? Boolean.TRUE : task.getIsSimulated());
        dto.setErrorCode(task.getErrorCode());
        dto.setErrorMessage(task.getErrorMessage());
        dto.setVehicleType(task.getVehicleType());

        JsonNode root = readTree(task.getDetections());
        dto.setDetectedCount(root == null ? 0 : root.size());
        if (root != null) {
            for (JsonNode item : root) {
                if ("plate".equals(item.path(KEY_CATEGORY).asText())) {
                    dto.setPlateRef(text(item, "plate_ref"));
                    dto.setPlateMasked(text(item, "plate_masked"));
                }
                if (dto.getDirection() == null) {
                    dto.setDirection(text(item, "direction"));
                }
            }
        }
        return dto;
    }

    /** 详情内联用：批量转换并过滤 null */
    static List<VehicleAlgorithmTaskRespDTO> toRespList(List<VehicleRecognitionAlgorithmTask> tasks) {
        List<VehicleAlgorithmTaskRespDTO> list = new ArrayList<>();
        if (tasks == null) {
            return list;
        }
        for (VehicleRecognitionAlgorithmTask task : tasks) {
            VehicleAlgorithmTaskRespDTO dto = toResp(task);
            if (dto != null) {
                list.add(dto);
            }
        }
        return list;
    }

    private static JsonNode readTree(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            log.warn("[VehicleRecognition] 算法任务 detections JSON 解析失败，检出数按 0 处理");
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : null;
    }
}
