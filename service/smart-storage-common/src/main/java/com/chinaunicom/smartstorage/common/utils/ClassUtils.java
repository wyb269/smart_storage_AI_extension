package com.chinaunicom.smartstorage.common.utils;

import java.lang.reflect.Field;
import java.util.Date;

import com.chinaunicom.smartstorage.common.exception.bean.CustomException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 类工具
 *
 * @author liuchink
 * @since 2021/3/18 8:43
 */
@Slf4j
@Data
public class ClassUtils {
	/**
	 * 实体类null赋默认值
	 *
	 * @param obj
	 *            obj
	 * @author liuchink
	 * @since 2021/3/18 8:47
	 */
	public static void setFeidValueNotNull(Object obj) {
		for (Field field : obj.getClass().getDeclaredFields()) {
			field.setAccessible(true);
			try {
				if (field.get(obj) == null) {
					switch (field.getGenericType().toString()) {
					case "class java.lang.Boolean":
						field.set(obj, false);
						break;
					case "class java.lang.String":
						field.set(obj, "");
						break;
					case "class java.lang.Integer":
						field.set(obj, 0);
						break;
					case "class java.lang.Double":
						field.set(obj, 0.0);
						break;
					case "class java.lang.Long":
						field.set(obj, 0L);
						break;
					case "class java.lang.Date":
						field.set(obj, new Date());
						break;
					default:
					}
				}
			} catch (IllegalAccessException llegalAccessException) {
				throw new CustomException(llegalAccessException);
			}
		}
	}
}
