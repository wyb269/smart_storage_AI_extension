<template>
  <div class="search-form-wrapper forNewclass">
    <t-form
      ref="formRef"
      :data="modelValue"
      class="search-form"
    >
      <!-- 基础字段 -->
      <template
        v-for="field in baseFields"
        :key="field.name"
      >
        <t-form-item
          :label="field.label"
          :name="field.name"
          class="form-item"
        >
          <!-- input 类型 -->
          <t-input
            v-if="field.type === 'input'"
            :model-value="modelValue[field.name]"
            @update:model-value="updateField(field.name, $event)"
            :placeholder="field.placeholder || `请输入${field.label}`"
          />

          <!-- select 类型 -->
          <t-select
            v-else-if="field.type === 'select'"
            :model-value="modelValue[field.name]"
            @update:model-value="updateField(field.name, $event)"
            :options="field.options || []"
            :placeholder="field.placeholder || `请选择${field.label}`"
            style="width: 100%;"
          />

          <!-- date-range 类型 -->
          <t-date-range-picker
            v-else-if="field.type === 'date-range'"
            :model-value="modelValue[field.name]"
            @update:model-value="updateField(field.name, $event)"
            :placeholder="field.placeholder || ['开始日期', '结束日期']"
            type="date"
            clearable
            style="width: 100%;"
          />

          <!-- date 类型 -->
          <t-date-picker
            v-else-if="field.type === 'date'"
            :model-value="modelValue[field.name]"
            @update:model-value="updateField(field.name, $event)"
            :placeholder="field.placeholder || `请选择${field.label}`"
            type="date"
            clearable
            style="width: 100%;"
          />

          <!-- textarea 类型 -->
          <t-textarea
            v-else-if="field.type === 'textarea'"
            :model-value="modelValue[field.name]"
            @update:model-value="updateField(field.name, $event)"
            :placeholder="field.placeholder || `请输入${field.label}`"
          />

          <!-- 自定义插槽 -->
          <slot
            v-else
            :name="`field-${field.name}`"
            :field="field"
            :value="modelValue[field.name]"
            :update="updateField"
          />
        </t-form-item>
      </template>

      <!-- 展开字段 -->
      <template v-if="isExpanded">
        <template
          v-for="field in expandedFields"
          :key="field.name"
        >
          <t-form-item
            :label="field.label"
            :name="field.name"
            class="form-item"
          >
            <!-- input 类型 -->
            <t-input
              v-if="field.type === 'input'"
              :model-value="modelValue[field.name]"
              @update:model-value="updateField(field.name, $event)"
              :placeholder="field.placeholder || `请输入${field.label}`"
            />

            <!-- select 类型 -->
            <t-select
              v-else-if="field.type === 'select'"
              :model-value="modelValue[field.name]"
              @update:model-value="updateField(field.name, $event)"
              :options="field.options || []"
              :placeholder="field.placeholder || `请选择${field.label}`"
              style="width: 100%;"
            />

            <!-- date-range 类型 -->
            <t-date-range-picker
              v-else-if="field.type === 'date-range'"
              :model-value="modelValue[field.name]"
              @update:model-value="updateField(field.name, $event)"
              :placeholder="field.placeholder || ['开始日期', '结束日期']"
              type="date"
              clearable
              style="width: 100%;"
            />

            <!-- date 类型 -->
            <t-date-picker
              v-else-if="field.type === 'date'"
              :model-value="modelValue[field.name]"
              @update:model-value="updateField(field.name, $event)"
              :placeholder="field.placeholder || `请选择${field.label}`"
              type="date"
              clearable
              style="width: 100%;"
            />

            <!-- textarea 类型 -->
            <t-textarea
              v-else-if="field.type === 'textarea'"
              :model-value="modelValue[field.name]"
              @update:model-value="updateField(field.name, $event)"
              :placeholder="field.placeholder || `请输入${field.label}`"
            />

            <!-- 自定义插槽 -->
            <slot
              v-else
              :name="`field-${field.name}`"
              :field="field"
              :value="modelValue[field.name]"
              :update="updateField"
            />
          </t-form-item>
        </template>
      </template>

      <!-- 按钮组 -->
      <t-form-item class="form-item button-group-item">
        <div class="button-group">
          <t-button
            theme="primary"
            variant="base"
            @click="handleSearch"
            class="search-btn my-icon-btn"
          >
            <search-icon slot="icon" />查询
          </t-button>
          <t-button
            theme="primary"
            variant="outline"
            @click="handleReset"
            class="reset-btn my-icon-btn"
          >
            <refresh-icon slot="icon" />重置
          </t-button>
          <t-button
            v-if="showExpand && expandedFields.length > 0"
            theme="primary"
            variant="outline"
            @click="toggleExpand"
            :class="isExpanded ? 'collapse-btn my-icon-btn' : 'expand-btn my-icon-btn'"
          >
            <chevron-up-icon
              v-if="isExpanded"
              slot="icon"
            />
            <chevron-down-icon
              v-else
              slot="icon"
            />
            {{ isExpanded ? '收起' : '展开' }}
          </t-button>

          <!-- 自定义按钮插槽 -->
          <slot name="buttons" />
        </div>
      </t-form-item>
    </t-form>
  </div>
</template>

<script setup name="SearchForm">
import { ref, computed } from "vue";
import {
  SearchIcon,
  RefreshIcon,
  ChevronUpIcon,
  ChevronDownIcon,
} from "tdesign-icons-vue-next";

const props = defineProps({
  /**
   * 表单数据，通过 v-model 双向绑定
   */
  modelValue: {
    type: Object,
    required: true,
  },
  /**
   * 字段配置数组
   */
  fields: {
    type: Array,
    required: true,
    default: () => [],
  },
  /**
   * 是否显示展开/收起功能
   */
  showExpand: {
    type: Boolean,
    default: true,
  },
  /**
   * 自定义初始化的表单数据
   */
  initialValue: {
    type: Object,
    default: () => ({}),
  },
});

const emit = defineEmits(["search", "reset", "update:modelValue"]);

// 当 showExpand 为 true 时默认展开
const isExpanded = ref(
  props.showExpand &&
    props.fields &&
    Array.isArray(props.fields) &&
    props.fields.some((f) => f && f.expandable)
);

/**
 * 基础字段（始终显示）
 */
const baseFields = computed(() => {
  if (!props.fields || !Array.isArray(props.fields)) {
    return [];
  }
  return props.fields.filter((f) => f && !f.expandable);
});

/**
 * 展开字段（需要展开才显示）
 */
const expandedFields = computed(() => {
  if (!props.showExpand) {
    return [];
  }
  if (!props.fields || !Array.isArray(props.fields)) {
    return [];
  }
  return props.fields.filter((f) => f && f.expandable);
});

/**
 * 更新单个字段值
 */
const updateField = (fieldName, value) => {
  emit("update:modelValue", {
    ...props.modelValue,
    [fieldName]: value,
  });
};

/**
 * 搜索
 */
const handleSearch = () => {
  emit("search", {
    ...props.modelValue,
  });
};

/**
 * 重置
 */
const handleReset = () => {
  const resetData = {};
  props.fields.forEach((field) => {
    // 如果有 defaultValue，使用 defaultValue
    if (field.defaultValue !== undefined) {
      resetData[field.name] = field.defaultValue;
    } else {
      // 根据字段类型设置合适的默认值
      if (field.type === "date-range") {
        resetData[field.name] = [];
      } else if (field.type === "date") {
        resetData[field.name] = null;
      } else {
        resetData[field.name] = undefined;
      }
    }
  });
  emit("update:modelValue", resetData);
  emit("reset");
};

/**
 * 展开/收起切换
 */
const toggleExpand = () => {
  isExpanded.value = !isExpanded.value;
};
</script>

<style lang="less" scoped>
.search-form-wrapper {
  padding: 20px 15px;
  margin-bottom: 25px;
  background-color: #fff;
  border-radius: 4px;

  .search-form {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;

    .form-item {
      flex: 0 0 calc(25% - 16px);
      margin-bottom: 0;
      display: flex;
      flex-direction: row;
      align-items: center;
      box-sizing: border-box;
      min-width: 0;

      // 标签样式：固定宽度，右对齐
      :deep(.t-form__label) {
        width: 70px !important;
        flex-shrink: 0;
        text-align: right;
        padding-right: 8px;
        font-size: 14px;
        color: #333;
      }

      // 输入控件区域：占据剩余宽度
      :deep(.t-form__controls) {
        flex: 1;
        margin-left: 0 !important;
        min-width: 0;

        // 确保输入框和选择框不超出容器宽度
        :deep(input),
        :deep(.t-select),
        :deep(.t-input),
        :deep(.t-date-picker),
        :deep(.t-date-range-picker),
        :deep(.t-textarea) {
          width: 100%;
          box-sizing: border-box;
        }
      }

      // 按钮组表单项
      &.button-group-item {
        flex: 1 0 auto;
        min-width: fit-content;

        :deep(.t-form__label) {
          display: none;
        }

        :deep(.t-form__controls) {
          width: 100%;
        }

        .button-group {
          display: flex;
          width: 100%;
          justify-content: flex-end;
          gap: 8px;

          .t-button {
            flex: 0 0 auto;
          }
        }
      }
    }

    // 按钮图标样式
    :deep(.my-icon-btn) {
      .t-button__text {
        display: flex;
        align-items: center;
        justify-content: center;
        .t-icon {
          margin-right: 6px;
        }
      }
    }
  }
}
</style>

