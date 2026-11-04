package com.chinaunicom.smartstorage.admin.constant;

/**
 * 人员行为监测功能常量。
 *
 * @author aiVision 功能族 · 人员行为监测
 */
public final class PersonBehaviorConstant {

    private PersonBehaviorConstant() {
    }

    /**
     * 功能权限操作键。
     *
     * <p>这些值是操作键，不是权限码。本项目授权模型没有权限码列，
     * Spring Security 里的 authority 是 {@code base_rights_info.id} 的字符串形式，
     * 因此每个键必须在 {@code ai-vision.permission.rights.personBehavior} 中
     * 映射到目标环境实际分配的<b>数字权限 ID</b> 才能生效。</p>
     */
    public static final class Permission {

        private Permission() {
        }

        /** 本功能在共享权限配置中的功能标识 */
        public static final String FEATURE = "personBehavior";

        /** 查看：区域状态、画面地址、事件列表、事件详情，以及查看后自动标记 */
        public static final String VIEW = "VIEW";

        /** 违规记录导出 */
        public static final String EXPORT = "EXPORT";

        /**
         * 人工处理。接口层本期不暴露（与遮挡一致，查看即标记），
         * 但 Service 与仓储的写入链路已备好，权限位保留以便后续开启。
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
    public static final String EXPORT_BIZ_TYPE = "personBehaviorEvent";

    /** 事件编号前缀 */
    public static final String EVENT_NO_PREFIX = "PR";

    /** 首页最近事件默认条数 */
    public static final int DEFAULT_RECENT_EVENT_LIMIT = 5;

    /** 首页最近事件条数上限，防止 limit 被滥用为全表扫描 */
    public static final int MAX_RECENT_EVENT_LIMIT = 50;

    /**
     * 「查看即已处理」写入审计记录时使用的处理意见文案。
     *
     * <p>必须明确标注这是系统按查看动作自动标记，不代表任何人做过
     * 真实违规/误报的判断，否则审计会被自动记录污染成无法区分的人工结论。</p>
     */
    public static final String AUTO_VIEW_OPINION = "查看事件详情后自动标记为已处理";

    /** 审计来源：真人在页面上做的处理 */
    public static final String PROCESS_SOURCE_WEB = "WEB";

    /** 审计来源：系统按「查看即标记」自动写入。
     *
     * <p>与 {@link #PROCESS_SOURCE_WEB} 分开是审计可追溯的前提：
     * 两者都记成 WEB 的话，事后无法区分「有人判断过这条是误报」
     * 和「有人点开看了一眼」。</p>
     */
    public static final String PROCESS_SOURCE_SYSTEM = "SYSTEM";

    /* ==================== 算法任务（重新识别） ==================== */

    /** 任务类型：本功能固定值，与种子脚本 07 与建表注释一致 */
    public static final String ALGORITHM_TASK_TYPE = "PERSON_BEHAVIOR_DETECT";

    /** 媒体类型：重跑按事件关键帧单图识别 */
    public static final String ALGORITHM_MEDIA_TYPE = "SINGLE_IMAGE";

    /** 配置版本：提交算法任务时携带，便于版本追溯 */
    public static final String ALGORITHM_CONFIG_VERSION = "cfg-person-v1";

    /** 详情弹窗与任务列表接口单次返回的最大任务条数 */
    public static final int MAX_ALGORITHM_TASKS = 20;

    /** 算法任务创建人标识（系统代发，非人工处理人） */
    public static final String ALGORITHM_TASK_CREATOR = "SYSTEM-RERUN";
}
