package com.chinaunicom.smartstorage.admin.constant;

/**
 * 仓门异动功能常量。
 *
 * @author 仓门异动功能
 */
public final class DoorAnomalyConstant {

    private DoorAnomalyConstant() {
    }

    /**
     * 第 28 节 功能权限操作键。
     *
     * <p>权限必须同时实施在后端接口、前端按钮可见性、
     * 视频与证据短时地址签发、以及仓库/库区/仓房/摄像头数据范围。
     * 不能只隐藏前端按钮而不验证后端权限。</p>
     *
     * <p><b>这些值是操作键，不是权限码</b>。本项目授权模型没有权限码列，
     * Spring Security 里的 authority 是 {@code base_rights_info.id} 的字符串形式，
     * 因此每个键必须在 {@code ai-vision.permission.rights} 中映射到目标环境
     * 实际分配的<b>数字权限 ID</b> 才能生效，见
     * {@code AiVisionPermissionProperties}。</p>
     */
    public static final class Permission {

        private Permission() {
        }

        /**
         * 本功能在 {@code ai-vision.permission.rights} 中的功能标识。
         *
         * <p>权限判定由共享的 {@code AiVisionPermissionChecker} 承担，
         * 配置结构是「功能 → 操作 → 数字权限 ID」两层，因此每个功能
         * 都要声明自己这一层的键。</p>
         */
        public static final String FEATURE = "doorAnomaly";

        /**
         * 查看：仓门状态、实时画面地址、事件列表、事件详情，
         * 以及「查看详情后自动标记已处理」这一写入动作。
         */
        public static final String VIEW = "VIEW";

        /** 异动记录导出 */
        public static final String EXPORT = "EXPORT";

        /**
         * 人工处理。产品口径已取消人工处理入口（前端无按钮），
         * 但 {@code /processEvent} 接口仍保留，因此仍受独立权限保护，
         * 避免留下一个无权限约束的写接口。
         */
        public static final String PROCESS = "PROCESS";
    }

    /**
     * 第 26.2 节 播放地址有效期（分钟）。
     * <p>短时签名地址，建议 2 到 10 分钟。</p>
     */
    public static final int STREAM_URL_EXPIRE_MINUTES = 5;

    /**
     * 证据图片短时地址有效期（分钟）。
     */
    public static final int EVIDENCE_URL_EXPIRE_MINUTES = 10;

    /**
     * 第 18.5 节 同步导出单次条数上限。
     * <p>超过约 5000 条应改为异步导出任务。</p>
     */
    public static final int EXPORT_MAX_ROWS = 5000;

    /**
     * 导出模板在 {@code ExcelExportConfig.config} 中注册的 key。
     *
     * <p>与请求参数 {@code source}（发起导出的页面，取值 HISTORY/RECENT）不是一回事：
     * 把 source 当模板 key 会查不到配置并在 {@code ExcelExportConfig.ExportConfig}
     * 上抛 NPE，导出必然失败。</p>
     */
    public static final String EXPORT_BIZ_TYPE = "doorAnomalyEvent";

    /**
     * 第 39.6 节 模拟数据标识字段语义常量。
     */
    public static final String SIMULATION_FLAG_FIELD = "isSimulated";

    /**
     * 首页最近事件默认条数。
     * <p>第 16.5 节：首页只展示最近 5 条。</p>
     */
    public static final int DEFAULT_RECENT_EVENT_LIMIT = 5;

    /**
     * 首页最近事件条数上限。
     * <p>服务端限制上限，防止 limit 被滥用为全表扫描。</p>
     */
    public static final int MAX_RECENT_EVENT_LIMIT = 50;

    /**
     * 「查看即已处理」写入审计记录时使用的处理意见文案。
     *
     * <p>本功能已取消人工处理入口：查看详情即把未处理事件标记为已处理。
     * 审计记录仍然追加，但必须明确标注这是<b>系统按查看动作自动标记</b>，
     * 不代表任何人做过真实异常/误报的判断，否则第 29.4 节要求的
     * 「谁处理、为何这样处理」会被自动记录污染成无法区分的人工结论。</p>
     */
    public static final String AUTO_VIEW_OPINION = "查看事件详情后自动标记为已处理";

    /**
     * 详情弹窗「算法任务」表随详情一次性返回的最大条数（契约 §1）。
     */
    public static final int ALGORITHM_TASK_HISTORY_LIMIT = 20;

    /**
     * 「重新识别」落库时使用的任务类型（契约 §1：本功能语义名，不与其他功能共用）。
     */
    public static final String ALGORITHM_TASK_TYPE = "DOOR_ANOMALY_DETECT";

    /**
     * 「重新识别」受理时写入的种子配置版本，与 07 补种脚本保持一致。
     */
    public static final String ALGORITHM_CONFIG_VERSION = "cfg-20261001";
}
