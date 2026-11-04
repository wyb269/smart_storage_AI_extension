package com.chinaunicom.smartstorage.admin.constant;

/**
 * 地面积水监测功能常量。
 *
 * @author aiVision 功能族 · 地面积水监测
 */
public final class GroundWaterConstant {

    private GroundWaterConstant() {
    }

    /**
     * 功能权限操作键。
     *
     * <p>这些值是操作键，不是权限码。本项目授权模型没有权限码列，
     * Spring Security 里的 authority 是 {@code base_rights_info.id} 的字符串形式，
     * 因此每个键必须在 {@code ai-vision.permission.rights.groundWater} 中
     * 映射到目标环境实际分配的<b>数字权限 ID</b> 才能生效。</p>
     */
    public static final class Permission {

        private Permission() {
        }

        /** 本功能在共享权限配置中的功能标识 */
        public static final String FEATURE = "groundWater";

        /** 查看：监测点位状态、画面地址、事件列表、事件详情，以及查看后自动标记 */
        public static final String VIEW = "VIEW";

        /** 积水记录导出 */
        public static final String EXPORT = "EXPORT";

        /**
         * 人工处理。产品口径与遮挡那版一致：页面不提供人工处理入口，
         * 点开详情即标记为已处理，因此前端无处理按钮；
         * 权限位保留，便于后续开启真实处理链路，
         * 避免留下无权限约束的写接口。
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
     * <p><b>本 key 需要由基座维护者在 ExcelExportConfig 中登记一次</b>，
     * 未登记时导出接口会 NPE，其余 6 个接口不受影响。</p>
     */
    public static final String EXPORT_BIZ_TYPE = "groundWaterEvent";

    /** 事件编号前缀 */
    public static final String EVENT_NO_PREFIX = "WP";

    /** 首页最近事件默认条数 */
    public static final int DEFAULT_RECENT_EVENT_LIMIT = 5;

    /** 首页最近事件条数上限，防止 limit 被滥用为全表扫描 */
    public static final int MAX_RECENT_EVENT_LIMIT = 50;

    /**
     * 「查看即已处理」写入审计记录时使用的处理意见文案。
     *
     * <p>必须明确标注这是系统按查看动作自动标记，不代表任何人做过
     * 真实积水/误报的判断，否则审计会被自动记录污染成无法区分的人工结论。</p>
     */
    public static final String AUTO_VIEW_OPINION = "查看事件详情后自动标记为已处理";

    /** 审计来源：系统按「查看即标记」自动写入 */
    public static final String PROCESS_SOURCE_SYSTEM = "SYSTEM";

    /** 审计来源：页面人工处理写入 */
    public static final String PROCESS_SOURCE_WEB = "WEB";

    /** 算法任务类型，地面积水识别固定值 */
    public static final String ALGORITHM_TASK_TYPE = "GROUND_WATER_DETECT";

    /** 详情接口携带的算法任务条数上限（契约 §1：algorithmTasks 最多 20 条） */
    public static final int DETAIL_ALGORITHM_TASK_LIMIT = 20;

    /** listAlgorithmTasks 端点返回的任务条数上限，防止历史无限增长拖垮弹窗 */
    public static final int LIST_ALGORITHM_TASK_LIMIT = 50;

    /** 重跑任务使用的媒体形态：积水判定基于单帧关键图 */
    public static final String ALGORITHM_MEDIA_TYPE = "SINGLE_IMAGE";

    /** 重跑任务的配置版本标识（进入算法输入与任务行，供版本追溯） */
    public static final String ALGORITHM_CONFIG_VERSION = "gw-web-config-v1";

    /**
     * 「进行中」状态的最短可见时长（毫秒）。
     *
     * <p>Mock 适配器 200ms 即出终态。若查询接口一命中终态就立刻落库，
     * 前端按 800ms 轮询只会看到 QUEUED→SUCCEEDED 一步到位，
     * 「先 RUNNING 后 SUCCEEDED」的生命周期在真实链路上不可观察。
     * 服务层因此做一次节流：QUEUED 的任务首次拿到终态先落 RUNNING，
     * 距任务开始超过本阈值后下一次轮询才采纳终态。
     * 这不是同步长等——每个请求仍然即时返回，只是状态推进变慢了一拍。</p>
     */
    public static final long ALGORITHM_RUNNING_VISIBLE_MIN_MS = 1_000L;
}
