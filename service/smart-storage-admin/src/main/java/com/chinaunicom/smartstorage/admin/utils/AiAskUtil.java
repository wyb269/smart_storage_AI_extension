package com.chinaunicom.smartstorage.admin.utils;

import java.util.List;

/**
 * AI 对话式追问工具
 * <p>四个 AI 辅助接口对外模拟大模型：入参缺失时不做兜底计算，由算法入口统一检查
 * 参数完整性，缺失时返回 AI 口吻的追问文案（一次列全所有缺失项），由前端流式回显。</p>
 *
 * @author smart-storage
 * @since 2026/10/29
 */
public final class AiAskUtil {

    private static final String CIRCLED = "①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳";

    private AiAskUtil() {
    }

    /**
     * 生成追问文案，如：为了给您生成精准的通风方案，还需要补充以下信息：① 粮堆高度 ② 平均粮温。补充完整后我再为您重新计算~
     *
     * @param taskName      任务名称，如「通风方案」「熏蒸方案」
     * @param missingParams 缺失参数中文名列表
     */
    public static String buildAskMessage(String taskName, List<String> missingParams) {
        StringBuilder sb = new StringBuilder("为了给您生成精准的")
            .append(taskName)
            .append("，还需要补充以下信息：");
        for (int i = 0; i < missingParams.size(); i++) {
            sb.append(i > 0 ? " " : "").append(circled(i)).append(' ').append(missingParams.get(i));
        }
        sb.append("。补充完整后我再为您重新计算~");
        return sb.toString();
    }

    private static String circled(int index) {
        return index < CIRCLED.length() ? String.valueOf(CIRCLED.charAt(index)) : "(" + (index + 1) + ")";
    }
}
