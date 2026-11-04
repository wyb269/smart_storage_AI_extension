package com.chinaunicom.smartstorage.common.controller.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;



import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.extern.slf4j.Slf4j;

/**
 * ResponseWrapper
 *
 * @author liuchink
 * @since 2021/12/21 10:44
 */
@Slf4j
public class ResponseWrapper extends HttpServletResponseWrapper {
	private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
	private HttpServletResponse response;
	private PrintWriter pwrite;

	public ResponseWrapper(HttpServletResponse response) {
		super(response);
		this.response = response;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return new MyServletOutputStream(bytes);
	}

	/**
	 *
	 *
	 * 重写父类的 getWriter() 方法，将响应数据缓存在 PrintWriter 中
	 *
	 * @return java.io.PrintWriter
	 * @author liuchink
	 * @since 2022/7/6 16:29
	 */
	@Override
	public PrintWriter getWriter() throws IOException {
		pwrite = new PrintWriter(
				new OutputStreamWriter(bytes, StandardCharsets.UTF_8));
		return pwrite;
	}

	/**
	 *
	 *
	 * 获取缓存在 PrintWriter 中的响应数据
	 *
	 * @return byte[]
	 * @author liuchink
	 * @since 2022/7/6 16:29
	 */
	public byte[] getBytes() {
		if (null != pwrite) {
			pwrite.close();
			return bytes.toByteArray();
		}
		try {
			bytes.flush();
		} catch (IOException ioException) {
			log.error(SystemConstant.LOG_ERROR_TITLE, ioException.getMessage(),
					ioException);
		}
		return bytes.toByteArray();
	}

	static class MyServletOutputStream extends ServletOutputStream {
		private final ByteArrayOutputStream ostream;

		public MyServletOutputStream(ByteArrayOutputStream ostream) {
			this.ostream = ostream;
		}

		@Override
		public void write(int b) throws IOException {
			ostream.write(b); // 将数据写到 stream 中
		}

		@Override
		public boolean isReady() {
            return true;
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {

		}
	}
}
