package com.chinaunicom.smartstorage.admin.constant;

/**
 * 车辆智能识别功能常量。
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
public final class VehicleRecognitionConstant {

    private VehicleRecognitionConstant() {
    }

    /**
     * 功能权限操作键。
     *
     * <p>这些值是操作键，不是权限码。本项目授权模型没有权限码列，
     * Spring Security 里的 authority 是 {@code base_rights_info.id} 的字符串形式，
     * 因此每个键必须在 {@code ai-vision.permission.rights.vehicleRecognition} 中
     * 映射到目标环境实际分配的<b>数字权限 ID</b> 才能生效。</p>
     */
    public static final class Permission {

        private Permission() {
        }

        /** 本功能在共享权限配置中的功能标识 */
        public static final String FEATURE = "vehicleRecognition";

        /** 查看：识别点位画面地址、今日识别、出入事件列表、事件详情，以及查看后自动标记 */
        public static final String VIEW = "VIEW";

        /** 出入事件记录导出 */
        public static final String EXPORT = "EXPORT";

        /**
         * 人工核实。产品口径与遮挡一致：页面走「查看即标记」，
         * 前端不调用独立处理接口，但保留 PROCESS 权限位以便后续开启误报处理链路。
         */
        public static final String PROCESS = "PROCESS";
    }

    /** 播放地址有效期（分钟），短时签名地址 */
    public static final int STREAM_URL_EXPIRE_MINUTES = 5;

    /** 证据图片短时地址有效期（分钟） */
    public static final int EVIDENCE_URL_EXPIRE_MINUTES = 10;

    /** 同步导出单次条数上限，超过约 5000 条应改为异步导出任务 */
    public static final int EXPORT_MAX_ROWS = 5000;

    /**
     * 导出模板在 {@code ExcelExportConfig.config} 中注册的 key。
     *
     * <p>与请求参数 {@code source}（发起导出的页面，取值 HISTORY/RECENT）不是一回事：
     * 把 source 当模板 key 会查不到配置并立刻 NPE。</p>
     *
     * <p>⚠️ 本 key 需在共享的 {@code ExcelExportConfig} 中登记后，真实后端导出才可用；
     * 该文件属禁改共享件，登记动作由共享文件负责人统一处理。</p>
     */
    public static final String EXPORT_BIZ_TYPE = "vehicleRecognitionEvent";

    /** 事件编号前缀 */
    public static final String EVENT_NO_PREFIX = "VC";

    /** 首页最近事件默认条数 */
    public static final int DEFAULT_RECENT_EVENT_LIMIT = 5;

    /** 首页最近事件条数上限，防止 limit 被滥用为全表扫描 */
    public static final int MAX_RECENT_EVENT_LIMIT = 50;

    /**
     * 「今日识别车辆」左侧列表的默认展示上限。
     *
     * <p>左侧是「今天」的识别记录卡片，量大时按识别时间倒序截断，
     * 完整记录走底部分页表与历史页。</p>
     */
    public static final int DEFAULT_TODAY_VEHICLE_LIMIT = 50;

    /** 今日识别条数上限，防止全表扫描 */
    public static final int MAX_TODAY_VEHICLE_LIMIT = 200;

    /**
     * 「查看即已核实」写入审计记录时使用的处理意见文案。
     *
     * <p>必须明确标注这是系统按查看动作自动标记，不代表任何人做过
     * 真实核实/误报的判断，否则审计会被自动记录污染成无法区分的人工结论。</p>
     */
    public static final String AUTO_VIEW_OPINION = "查看识别记录后自动标记为已核实";

    /* ==================== 算法任务（重新识别） ==================== */

    /**
     * 算法任务类型，写入 {@code ai_vehicle_algorithm_task.task_type}。
     *
     * <p>本功能的算法职责是「车牌 + 车型 + 方向」三件事，
     * 因此用功能语义名 {@code VEHICLE_RECOGNIZE}，不与其它功能共用。</p>
     */
    public static final String ALGORITHM_TASK_TYPE = "VEHICLE_RECOGNIZE";

    /**
     * 算法任务编号前缀。
     *
     * <p>「重新识别」链路上，页面可见的任务标识直接沿用算法网关返回的
     * {@code taskId}（落库到 {@code task_no}，唯一约束 {@code uk_vat_task_no}），
     * 这样才能用同一个标识回调 {@code gateway.queryTask()}。
     * 本前缀用于历史种子数据与兜底生成，见 docs/vehicle-recognition/sql/07。</p>
     */
    public static final String ALGORITHM_TASK_NO_PREFIX = "VCAT";

    /** 算法输入媒体类型：车辆全景图 + 车牌特写，故为双图 */
    public static final String ALGORITHM_MEDIA_TYPE = "DUAL_IMAGE";

    /** 算法配置版本（写入 task.config_version，供结论追溯） */
    public static final String ALGORITHM_CONFIG_VERSION = "vehicle-cfg-1.0";

    /**
     * 模型名 / 版本 / 推理程序版本。
     *
     * <p>取值与 {@code MockVehicleRecognitionAlgorithmProperties} 的默认参数一致，
     * 但<b>不在这里引用那个类</b>：真实算法到达后由适配器输出这些字段，
     * 本组常量只给内存兜底模式的历史种子用，两者不一致时以适配器为准。</p>
     */
    public static final String ALGORITHM_MODEL_NAME = "mock-vehicle-recognizer";

    /** 模型版本 */
    public static final String ALGORITHM_MODEL_VERSION = "mock-1.0.0";

    /** 推理程序版本 */
    public static final String ALGORITHM_ADAPTER_VERSION = "mock-adapter-1.0.0";

    /** 算法置信度阈值（写入 task.confidence_threshold；低于阈值时结论为 UNCERTAIN） */
    public static final java.math.BigDecimal ALGORITHM_CONFIDENCE_THRESHOLD =
            new java.math.BigDecimal("0.7500");

    /** 详情响应内联返回的算法任务条数上限（契约：最多 20 条） */
    public static final int MAX_ALGORITHM_TASKS_IN_DETAIL = 20;

    /** {@code listAlgorithmTasks} 单次返回上限，防止历史无限增长拖慢详情 */
    public static final int MAX_ALGORITHM_TASKS_QUERY = 50;
}
