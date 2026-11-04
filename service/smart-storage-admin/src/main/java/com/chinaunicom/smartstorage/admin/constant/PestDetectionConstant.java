package com.chinaunicom.smartstorage.admin.constant;

import java.math.BigDecimal;

/**
 * 粮面虫害智能识别功能常量。
 *
 * @author aiVision 功能族 · 粮面虫害智能识别
 */
public final class PestDetectionConstant {

    private PestDetectionConstant() {
    }

    /**
     * 功能权限操作键。
     *
     * <p>这些值是操作键，不是权限码。本项目授权模型没有权限码列，
     * Spring Security 里的 authority 是 {@code base_rights_info.id} 的字符串形式，
     * 因此每个键必须在 {@code ai-vision.permission.rights.pestDetection} 中
     * 映射到目标环境实际分配的<b>数字权限 ID</b> 才能生效。</p>
     */
    public static final class Permission {

        private Permission() {
        }

        /** 本功能在共享权限配置中的功能标识 */
        public static final String FEATURE = "pestDetection";

        /** 查看：粮面点位状态、画面地址、检测记录列表、记录详情，以及查看后自动标记 */
        public static final String VIEW = "VIEW";

        /** 虫害检测记录导出 */
        public static final String EXPORT = "EXPORT";

        /**
         * 人工处理。产品口径与遮挡一致：前端无独立处理入口（查看即标记），
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
    public static final String EXPORT_BIZ_TYPE = "pestDetectionEvent";

    /** 采样检测事件编号前缀 */
    public static final String EVENT_NO_PREFIX = "PT";

    /** 首页最近检测记录默认条数 */
    public static final int DEFAULT_RECENT_EVENT_LIMIT = 5;

    /** 首页最近检测记录条数上限，防止 limit 被滥用为全表扫描 */
    public static final int MAX_RECENT_EVENT_LIMIT = 50;

    /**
     * 虫情密度判定阈值默认值（单位：头/kg）。
     *
     * <p><b>阈值的唯一权威来源。</b>{@code density > 本值} 即判为密度超标告警。
     * 事件表 {@code density_threshold} 列存的是<b>该条采样当时生效</b>的阈值快照，
     * 允许逐条不同；但「新事件缺省用哪个阈值」「Mock 算法用哪个阈值判超标」
     * 一律取本常量，禁止把数值写进 SQL 或前端页面，否则三处会各自漂移。</p>
     */
    public static final BigDecimal DEFAULT_DENSITY_THRESHOLD = new BigDecimal("5.00");

    /**
     * 「查看即已处理」写入审计记录时使用的处理意见文案。
     *
     * <p>必须明确标注这是系统按查看动作自动标记，不代表任何人做过
     * 真实虫情的判断，否则审计会被自动记录污染成无法区分的人工结论。</p>
     */
    public static final String AUTO_VIEW_OPINION = "查看检测记录详情后自动标记为已处理";

    /**
     * 算法任务类型固定值（本功能语义名，不与其它 aiVision 功能共用）。
     * 与建表注释 {@code ai_pest_algorithm_task.task_type} 一致。
     */
    public static final String ALGORITHM_TASK_TYPE = "PEST_DETECT";

    /** 算法任务媒体类型：虫害按单帧采样识别 */
    public static final String ALGORITHM_MEDIA_TYPE = "SINGLE_IMAGE";

    /** 「重新识别」写入算法任务行的配置版本标识 */
    public static final String ALGORITHM_CONFIG_VERSION = "mock-config-1";

    /** 详情弹窗「算法任务」表最多返回的历史任务条数 */
    public static final int MAX_ALGORITHM_TASKS_PER_EVENT = 20;
}
