package com.sunny.imessage.push.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sunny.imessage.push.file.FileUtils;
import com.sunny.imessage.push.utils.StringUtils;

/**
 * 
 * 获取电话号码
 * 
 * Create on Dec 21, 2013 12:15:46 PM
 * 
 * @author <a href="mailto:zhouyan@pzoomtech.com">ZhouYan</a>.
 * 
 */
public class GetPhoneNum implements IAction {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public final static GetPhoneNum instance = new GetPhoneNum();

	private final LinkedBlockingQueue<Long> phones = new LinkedBlockingQueue<Long>();

	private String text;

	private ConcurrentHashSet<Long> success = new ConcurrentHashSet<Long>();

	private ConcurrentHashSet<Long> failed = new ConcurrentHashSet<Long>();

	private StyledText outputText;

	private int count = 0;

	private ArrayList<String> files = new ArrayList<String>();

	private GetPhoneNum() {
	}

	/**
	 * @return the size
	 */
	public long getSize() {
		return phones.size();
	}

	public void stop() {
		phones.clear();
	}

	/**
	 * 返回请求URI，匹配成功后处理
	 */
	@Override
	public String getPath() {
		return "/IMessage/api/getphonenumber";
	}

	public void addFile(String file) {
		files.add(file);
	}

	public void start(StyledText outputText, String text) throws IOException {
		this.text = text;
		phones.clear();
		this.outputText = outputText;
		for (String file : files) {
			List<Long> f = FileUtils.readPhones(file);
			for (Long p : f) {
				phones.add(p);
				failed.add(p);
			}
		}
		logger.info("reading files has finished.");
	}

	/**
	 * 处理成功
	 * 
	 * @param phones
	 */
	public void success(String[] phones) {
		String[] p = phones;
		for (String phone : p) {
			success.add(Long.valueOf(phone.replaceAll("\\+", "")));
			failed.remove(Long.valueOf(phone.replaceAll("\\+", "")));
		}
	}

	/**
	 * 处理失败
	 * 
	 * @param phones
	 */
	public void failed(String[] phones) {
		String[] p = phones;
		for (String phone : p) {
			failed.add(Long.valueOf(phone.replaceAll("+", "")));
		}
	}

	public synchronized void print(final String line) {
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				outputText.append(line);
				count++;
				if (count == 1000)
					outputText.setText("");
				else
					outputText.setSelection(outputText.getTextLimit());

			}

		});

	}

	private long getNum() {
		return phones.poll();
		// Long num = null;
		// while (true) {
		// logger.debug("..................");
		// if (iter.hasNext()) {
		// num = iter.next();
		// } else {
		// if (phones.size() > 0) {
		// iter = phones.iterator();
		// num = iter.next();
		// } else {
		// num = -1l;
		// }
		// }
		// if (num != -1) {
		// if (success.contains(num) || failed.contains(num)) {
		// iter.remove();
		// continue;
		// } else {
		// return num;
		// }
		// } else {
		// return num;
		// }
		// }
	}

	public ConcurrentHashSet<Long> getFailed() {
		return failed;
	}

	public ConcurrentHashSet<Long> getSuccess() {
		return success;
	}

	@Override
	public void doing(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
		// String count = request.getParameter("count");
		int max = 1;
		// if (count != null && !count.equals(""))
		// max = Integer.valueOf(count);

		Long num = getNum();
		logger.debug("---------------------" + num);

		if (num == null)
			return;

		JSONArray lstJSON = new JSONArray();
		// for (int i = 0; i < max; i++) {
		JSONObject phone = new JSONObject();
		String strNum = num + "";
		if (!strNum.startsWith("86"))
			strNum = "86" + strNum;
		phone.put("phone", strNum);
		lstJSON.add(phone);

		if (lstJSON.size() == 0)
			return;

		JSONObject resJSON = new JSONObject();
		resJSON.put("text", text);
		resJSON.put("phone", lstJSON);
		logger.debug(resJSON.toJSONString());
		response.getOutputStream().write(resJSON.toJSONString().getBytes("utf-8"));
		response.getOutputStream().flush();
	}

	public void save(String type) {
		try {
			String location = StringUtils.getLocation();
			if (type.equals(Task.SEND)) {
				FileUtils.writePhones(success, location + FileUtils.DIR + FileUtils.successFile);
				FileUtils.writePhones(failed, location + FileUtils.DIR + FileUtils.failedFile);
			} else if (type.equals(Task.SCAN)) {
				FileUtils.writePhones(success, location + FileUtils.DIR + FileUtils.scanFile);
			}
		} catch (IOException e) {
			logger.error("", e);
		}
	}
}
