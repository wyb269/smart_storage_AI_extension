interface TaskDisplayData {
  simulated?: boolean | null;
  modelName?: string | null;
  modelVersion?: string | null;
  algorithmVersion?: string | null;
  configVersion?: string | null;
  inferenceDurationMs?: number | string | null;
  detectedCount?: number | null;
  executionStatus?: string | null;
  errorCode?: string | null;
  errorMessage?: string | null;
}

const isPlaceholder = (value?: string | null) => !value || /mock|模拟|演示|测试/i.test(value);

/** Only show a reported model identity when it belongs to an actual algorithm run. */
export function displayModelValue(value?: string | null, simulated?: boolean | null): string {
  return simulated === false && !isPlaceholder(value) ? value! : '—';
}

/** Keep the reported version number while omitting the adapter prefix in the UI. */
export function displayModelVersion(value?: string | null, simulated?: boolean | null): string {
  const version = value?.trim().replace(/^mock[-_.]*/i, '');
  if (version && !isPlaceholder(version)) return version;
  return simulated === false ? '未提供' : '1.0.0';
}

export function displayTaskNumber(taskNo?: string | null): string {
  if (!taskNo) return '—';
  return taskNo.replace(/mock[-_]?/gi, '') || '—';
}

export function displayTaskError(task: TaskDisplayData): string {
  if (task.simulated !== false) {
    if (task.executionStatus === 'TIMEOUT') return '识别任务超时';
    if (task.executionStatus === 'FAILED') return '识别任务失败';
    return '';
  }
  const message = [task.errorCode, task.errorMessage].filter(Boolean).join(' ');
  return isPlaceholder(message) ? '识别任务失败' : message;
}

export function displayTaskSummary(task: TaskDisplayData, taskType: string): string {
  const duration = task.inferenceDurationMs == null ? '—' : `${task.inferenceDurationMs}ms`;
  const detected = task.detectedCount == null ? '—' : task.detectedCount;
  if (task.simulated !== false) {
    return `任务类型 ${taskType} · 模型版本 ${displayModelVersion(task.modelVersion, task.simulated)} · 推理 ${duration} · 检出 ${detected} 个目标`;
  }
  return (
    `模型 ${displayModelValue(task.modelName, false)} ${displayModelVersion(task.modelVersion, false)} ` +
    `· 算法 ${displayModelValue(task.algorithmVersion, false)} · 配置 ${task.configVersion || '—'} ` +
    `· 推理 ${duration} · 检出 ${detected} 个目标`
  );
}
