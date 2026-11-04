package com.chinaunicom.smartstorage.admin.constant;

/**
 * 周界入侵识别功能常量。
 *
 * @author aiVision 功能族 · 周界入侵识别
 */
public final class PerimeterIntrusionConstant {

    private PerimeterIntrusionConstant() {
    }

    /**
     * 功能权限操作键。
     *
     * <p>这些值是操作键，不是权限码。本项目授权模型没有权限码列，
     * Spring Security 里的 authority 是 {@code base_rights_info.id} 的字符串形式，
     * 因此每个键必须在 {@code ai-vision.permission.rights.perimeterIntrusion} 中
     * 映射到目标环境实际分配的<b>数字权限 ID</b> 才能生效。</p>
     */
    public static final class Permission {

        private Permission() {
        }

        /** 本功能在共享权限配置中的功能标识 */
        public static final String FEATURE = "perimeterIntrusion";

        /** 查看：防区段状态、画面地址、事件列表、事件详情，以及查看后自动标记 */
        public static final String VIEW = "VIEW";

        /** 入侵记录导出 */
        public static final String EXPORT = "EXPORT";

        /**
         * 人工处理。产品口径与遮挡那一版一致：页面详情弹窗打开即标记已处理，
         * 接口保留但受独立权限保护，避免留下无权限约束的写接口。
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
     */
    public static final String EXPORT_BIZ_TYPE = "perimeterIntrusionEvent";

    /** 事件编号前缀 */
    public static final String EVENT_NO_PREFIX = "PD";

    /** 首页最近事件默认条数 */
    public static final int DEFAULT_RECENT_EVENT_LIMIT = 5;

    /** 首页最近事件条数上限，防止 limit 被滥用为全表扫描 */
    public static final int MAX_RECENT_EVENT_LIMIT = 50;

    /**
     * 「查看即已处理」写入审计记录时使用的处理意见文案。
     *
     * <p>必须明确标注这是系统按查看动作自动标记，不代表任何人做过
     * 真实入侵/误报的判断，否则审计会被自动记录污染成无法区分的人工结论。</p>
     */
    public static final String AUTO_VIEW_OPINION = "查看事件详情后自动标记为已处理";

    /* ==================== 算法任务（契约 §1「重新识别」） ==================== */

    /**
     * 算法任务类型。
     *
     * <p>本功能语义名，不与遮挡（VIDEO_OCCLUSION_DETECT）等功能共用；
     * 与 {@code ai_perimeter_algorithm_task.task_type} 及
     * {@code 05_create_perimeter_algorithm_task.sql} 的列注释一致。</p>
     */
    public static final String ALGORITHM_TASK_TYPE = "PERIMETER_INTRUSION_DETECT";

    /** 详情弹窗一次性带回的算法任务条数上限（契约 §1：最多 20 条） */
    public static final int DETAIL_ALGORITHM_TASK_LIMIT = 20;

    /** 算法输入媒体类型：本期用事件关键帧单图 */
    public static final String ALGORITHM_MEDIA_TYPE = "SINGLE_IMAGE";

    /** 算法配置版本：Mock 阶段用代码常量，真实算法接入后由适配器提供 */
    public static final String ALGORITHM_CONFIG_VERSION = "mock-config-v1";

    /** 置信度阈值，与 PerimeterIntrusionMockAlgorithmProperties 默认值 0.75 一致 */
    public static final java.math.BigDecimal ALGORITHM_CONFIDENCE_THRESHOLD =
        new java.math.BigDecimal("0.7500");

    /** 事件发生对应的受控媒体引用前缀（禁止 http / 本地绝对路径） */
    public static final String EVIDENCE_REF_PREFIX = "mock://perimeter/";

    /**
     * 「重新识别」业务错误：撤防段不做算法识别。
     *
     * <p>沿用 getStreamInfo 已有的 POINT_DISARMED 语义码，前缀编码与
     * {@code unavailableReason} 一致，便于前端把两处「撤防不工作」的
     * 提示统一到同一文案。</p>
     */
    public static final String POINT_DISARMED_ERROR =
        "该防区段当前为撤防状态，撤防期间不执行算法识别（POINT_DISARMED）";

    /** 撤防语义码，与 StreamInfo.unavailableReason 的 POINT_DISARMED 同值 */
    public static final String POINT_DISARMED_CODE = "POINT_DISARMED";
}
