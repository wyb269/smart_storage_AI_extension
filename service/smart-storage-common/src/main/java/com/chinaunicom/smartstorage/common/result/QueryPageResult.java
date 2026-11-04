package com.chinaunicom.smartstorage.common.result;

import com.google.common.collect.Lists;
import lombok.Data;


import java.util.List;

/**
 * 查询结果返回
 *
 * @author liuchink
 * @since 2021/8/24 8:50
 */
@Data
public class QueryPageResult<T>  {


	public QueryPageResult() {
		this.total = 0L;
		this.data = Lists.newArrayList();
	}

	private Long total;
	private List<T> data;
}
