package com.chinaunicom.smartstorage.admin.constant;

/**
 * 视频遮挡检测功能常量。
 *
 * @author aiVision 功能族 · 视频遮挡检测
 */
public final class VideoOcclusionConstant {

    private VideoOcclusionConstant() {
    }

    /**
     * 功能权限操作键。
     *
     * <p>这些值是操作键，不是权限码。本项目授权模型没有权限码列，
     * Spring Security 里的 authority 是 {@code base_rights_info.id} 的字符串形式，
     * 因此每个键必须在 {@code ai-vision.permission.rights.videoOcclusion} 中
     * 映射到目标环境实际分配的<b>数字权限 ID</b> 才能生效。</p>
     */
    public static final class Permission {

        private Permission() {
        }

        /** 本功能在共享权限配置中的功能标识 */
        public static final String FEATURE = "videoOcclusion";

        /** 查看：摄像头状态、画面地址、事件列表、事件详情，以及查看后自动标记 */
        public static final String VIEW = "VIEW";

        /** 遮挡记录导出 */
        public static final String EXPORT = "EXPORT";

        /**
         * 人工处理。产品口径与仓门异动一致：页面只保留未处理/已处理两态，
         * 点开详情即标记已处理，因此前端无处理按钮；
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
    public static final String EXPORT_BIZ_TYPE = "videoOcclusionEvent";

    /** 事件编号前缀 */
    public static final String EVENT_NO_PREFIX = "VD";

    /** 首页最近事件默认条数 */
    public static final int DEFAULT_RECENT_EVENT_LIMIT = 5;

    /** 首页最近事件条数上限，防止 limit 被滥用为全表扫描 */
    public static final int MAX_RECENT_EVENT_LIMIT = 50;

    /**
     * 「查看即已处理」写入审计记录时使用的处理意见文案。
     *
     * <p>必须明确标注这是系统按查看动作自动标记，不代表任何人做过
     * 真实遮挡/误报的判断，否则审计会被自动记录污染成无法区分的人工结论。</p>
     */
    public static final String AUTO_VIEW_OPINION = "查看事件详情后自动标记为已处理";

    /* ==================== 算法任务（契约 §1：数据与「重新识别」交互） ==================== */

    /** 算法任务类型，本功能语义名，不与其它功能共用 */
    public static final String ALGORITHM_TASK_TYPE = "VIDEO_OCCLUSION_DETECT";

    /** 算法输入媒体类型：遮挡判定基于单帧关键图 */
    public static final String ALGORITHM_MEDIA_TYPE = "SINGLE_IMAGE";

    /** 算法运行配置版本，落任务表 config_version 列，供版本追溯 */
    public static final String ALGORITHM_CONFIG_VERSION = "occlusion-config-v1";

    /** 算法置信度阈值（0-1），落任务表 confidence_threshold 列 */
    public static final String ALGORITHM_CONFIDENCE_THRESHOLD = "0.7500";

    /** 详情弹窗「算法任务」表最多返回条数（契约 §1：最多 20 条） */
    public static final int ALGORITHM_TASKS_MAX_PER_EVENT = 20;

    /** 重新识别提交后的初始执行状态（契约 §1：算法不得在页面请求里同步长等） */
    public static final String ALGORITHM_INITIAL_STATUS = "QUEUED";
}
