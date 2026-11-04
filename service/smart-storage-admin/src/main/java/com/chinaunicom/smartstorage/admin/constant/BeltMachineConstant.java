package com.chinaunicom.smartstorage.admin.constant;

/**
 * 皮带机下站人监测功能常量。
 *
 * @author aiVision 功能族 · 皮带机下站人监测
 */
public final class BeltMachineConstant {

    private BeltMachineConstant() {
    }

    /**
     * 功能权限操作键。
     *
     * <p>这些值是操作键，不是权限码。本项目授权模型没有权限码列，
     * Spring Security 里的 authority 是 {@code base_rights_info.id} 的字符串形式，
     * 因此每个键必须在 {@code ai-vision.permission.rights.beltMachine} 中
     * 映射到目标环境实际分配的<b>数字权限 ID</b> 才能生效。</p>
     */
    public static final class Permission {

        private Permission() {
        }

        /** 本功能在共享权限配置中的功能标识 */
        public static final String FEATURE = "beltMachine";

        /** 查看：皮带机状态、画面地址、事件列表、事件详情，以及查看后自动标记 */
        public static final String VIEW = "VIEW";

        /** 闲人历史记录导出 */
        public static final String EXPORT = "EXPORT";

        /**
         * 人工处理。产品口径与仓门异动、视频遮挡一致：
         * 页面只走「查看即标记」，因此前端无处理按钮；
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
    public static final String EXPORT_BIZ_TYPE = "beltMachineEvent";

    /** 事件编号前缀 */
    public static final String EVENT_NO_PREFIX = "BC";

    /** 首页最近事件默认条数 */
    public static final int DEFAULT_RECENT_EVENT_LIMIT = 5;

    /** 首页最近事件条数上限，防止 limit 被滥用为全表扫描 */
    public static final int MAX_RECENT_EVENT_LIMIT = 50;

    /**
     * 「未部署」的判定口径。
     *
     * <p>本功能监测对象是<b>皮带机</b>，不是摄像头。皮带机可能已经登记进台账，
     * 但还没接入视频平台，因此 {@code ai_belt_monitor_point.camera_id} 为空。
     * 这类点位既不是「在线」也不是「掉线」，而是<b>从来没有产生过数据</b>，
     * 页面文案叫「未部署」。</p>
     *
     * <p>库里用 {@code device_status = 'UNKNOWN'} 且 {@code camera_id IS NULL}
     * 两个条件共同表达。只有 UNKNOWN 而 camera_id 非空，是「绑了摄像头但设备态未知」，
     * 页面显示「未知」而不是「未部署」——两者运维处置动作完全不同，不能合并。</p>
     */
    public static final String DISPLAY_LABEL_NOT_DEPLOYED = "未部署";

    /**
     * 「查看即已处理」写入审计记录时使用的处理意见文案。
     *
     * <p>必须明确标注这是系统按查看动作自动标记，不代表任何人做过
     * 真实站人/误报的判断，否则审计会被自动记录污染成无法区分的人工结论。</p>
     */
    public static final String AUTO_VIEW_OPINION = "查看事件详情后自动标记为已处理";

    /**
     * 处理审计来源：真人在页面上做出的处理。
     *
     * <p>与 {@link #SOURCE_SYSTEM} 必须区分开：审计记录一旦分不清
     * 哪些结论来自人，就无法评估「查看即标记」这条产品口径的影响面。</p>
     */
    public static final String SOURCE_WEB = "WEB";

    /** 处理审计来源：系统按「查看即标记」口径自动写入 */
    public static final String SOURCE_SYSTEM = "SYSTEM";

    /* ==================== 算法任务（重新识别链路） ==================== */

    /** 任务类型，皮带机下站人识别固定取值（与 05 建表脚本注释一致） */
    public static final String ALGORITHM_TASK_TYPE = "BELT_PERSON_DETECT";

    /** 重新识别提交的配置版本，用于版本追溯（AI_HANDOFF 第 25.2 节） */
    public static final String ALGORITHM_CONFIG_VERSION = "belt-cfg-1.0.0";

    /** 皮带机禁入区域（受控 ROI，JSON 字符串）：画面下沿中央区域 */
    public static final String ALGORITHM_ROI = "[0.10,0.15,0.90,0.85]";

    /** 算法任务媒体类型：单帧图像 */
    public static final String ALGORITHM_MEDIA_TYPE = "SINGLE_IMAGE";

    /** 详情弹窗随包返回的算法任务条数上限 */
    public static final int DETAIL_ALGORITHM_TASK_LIMIT = 20;
}
