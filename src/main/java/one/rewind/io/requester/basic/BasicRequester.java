package one.rewind.io.requester.basic;

import com.typesafe.config.Config;
import one.rewind.io.requester.proxy.ProxyAuthenticator;
import one.rewind.io.requester.task.Task;
import one.rewind.io.requester.util.CertAutoInstaller;
import one.rewind.txt.URLUtil;
import one.rewind.util.Configs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mozilla.universalchardet.UniversalDetector;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import java.io.*;
import java.net.*;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * 基本HTTP内容请求器
 *
 * @author karajan
 *
 */
public class BasicRequester {

	protected static BasicRequester instance;

	public static int CONNECT_TIMEOUT;
	public static int READ_TIMEOUT;

	private static final Logger logger = LogManager.getLogger(BasicRequester.class.getName());

	private static final Pattern charsetPattern = Pattern.compile("(?i)\\bcharset=\\s*\"?([^\\s;\"]*)");

	static {

		// read config
		Config ioConfig = Configs.getConfig(BasicRequester.class);
		CONNECT_TIMEOUT = ioConfig.getInt("connectTimeout");
		READ_TIMEOUT = ioConfig.getInt("readTimeout");

		System.setProperty("http.keepAlive", "false");
		System.setProperty("http.maxConnections", "100");
		System.setProperty("sun.net.http.errorstream.enableBuffering", "true");

		// 信任证书
		System.setProperty("javax.net.ssl.trustStore", "cacerts");
		/*// Cookie 接收策略
		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));*/

	}

	Cookies cookiesManager = null;

	public Map<String, Map<String, String>> headers = new HashMap<>();

	/**
	 *
	 * @param domain
	 * @param header
	 */
	public void setHeaders(String domain, Map<String, String> header) {
		headers.put(domain, header);
	}

	/**
	 *
	 * @param domain
	 */
	public Map<String, String> getHeaders(String domain) {
		return headers.get(domain);
	}
	/**
	 * 单例模式
	 * @return
	 */
	public static BasicRequester getInstance() {

		if (instance == null) {
			synchronized (BasicRequester.class) {
				if (instance == null) {
					instance = new BasicRequester();
				}
			}
		}

		return instance;
	}

	/**
	 *
	 */
	private BasicRequester() {
		cookiesManager = new Cookies();
	}

	/**
	 * 同步请求
	 * @param task
	 */
	public void submit(Task task) {
		Wrapper wrapper = new Wrapper(task);
		wrapper.run();
		wrapper.close();
	}

	/**
	 * 异步请求
	 * TODO 根本用不着Wrapper 和 Future
	 * @param task
	 * @param timeout 可以手工设定超时时间
	 */
	public void submit(Task task, long timeout) {

		Wrapper wrapper = new Wrapper(task);

		final ExecutorService executor = Executors.newSingleThreadExecutor();
		final Future<?> future = executor.submit(wrapper);
		executor.shutdown();

		try {

			future.get(timeout, TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException e){
			logger.error("Task {}, was interrupted, ", task.url, e);
		}
		catch (TimeoutException e){
			future.cancel(true);
			logger.error("Task {}, ", task.url, e);
			task.exception = e;
		}
		catch (ExecutionException e) {
			logger.error("Task {}, ", task.url, e.getCause());
		}

		wrapper.close();

		if (!executor.isTerminated()){
			executor.shutdownNow();
		}
	}

	/**
	 * 解压缩GZIP输入流
	 *
	 * @param input
	 * @return
	 */
	public static InputStream decompress_stream(InputStream input) {

		// 使用 PushbackInputStream 进行预查
		PushbackInputStream pb = new PushbackInputStream(input, 2);

		byte[] signature = new byte[2];

		try {
			// 读取 signature
			pb.read(signature);
			// 放回 signature
			pb.unread(signature);

		} catch (IOException e) {
			logger.warn(e.toString());
		}

		if (signature[0] == (byte) 0x1f && signature[1] == (byte) 0x8b)

			try {
				return new GZIPInputStream(pb);
			} catch (IOException e) {
				logger.warn(e.toString());
				return pb;
			}

		else
			return pb;
	}

	/**
	 * 读取 ContentType 中的 charset 设定
	 * Parse out a charset from a content type headers.
	 *
	 * @param contentType e.g. "text/html; charset=EUC-JP"
	 * @return "EUC-JP", or null if not found. Charset is trimmed and
	 * uppercased.
	 */
	public static String getCharsetFromContentType(String contentType) {

		if (contentType == null)
			return null;

		Matcher m = charsetPattern.matcher(contentType);
		if (m.find()) {
			return m.group(1).trim().toUpperCase();
		}

		return null;
	}

	/**
	 * 文本内容自动解码方法
	 * @param src 输入内容
	 * @param preferredEncoding 优先编码
	 * @return 返回解码后的结果
	 * @throws UnsupportedEncodingException 异常
	 */
	public static String autoDecode(byte[] src, String preferredEncoding) throws UnsupportedEncodingException {

		String text;

		if(preferredEncoding != null){

			logger.trace("charset detected = {}", preferredEncoding);
			try {
				text = new String(src, preferredEncoding);
			} catch (UnsupportedEncodingException err) {
				logger.trace("decoding using {}", "utf-8");
				text = new String(src, "utf-8");
			}

		} else {

			text = new String(src, "utf-8");
			Pattern pattern = Pattern.compile("meta.*?charset\\s*?=\\s*?([^\"' ]+)", Pattern.MULTILINE);
			Matcher matcher = pattern.matcher(text);

			if (matcher.find()) {
				String encode = matcher.group(1);

				try {
					logger.trace("try decoding using {}", encode);
					text = new String(src, encode);
				} catch (Throwable ignored) {
					//src = new String(srcBin);
					logger.info("decoding error: {}", ignored.getMessage());
				}

			} else {

				UniversalDetector detector = new UniversalDetector(null);
				detector.handleData(src, 0, src.length);
				detector.dataEnd();
				String charset = detector.getDetectedCharset();

				if (charset != null) {
					logger.trace("charset detected = {}", charset);
					try {
						text = new String(src, charset);
					} catch (UnsupportedEncodingException err) {
						logger.trace("decoding using {}", "utf-8");
						text = new String(src, "utf-8");
					}
				} else {
					text = new String(src, "utf-8");
				}
			}
		}

		/*try {
			text = ChineseChar.unicode2utf8(text);
		} catch (Exception | Error e){
			logger.error("Error convert unicode to utf8", e);
		}*/

		return text;
	}

	/**
	 * HttpURLConnection工厂类
	 *
	 * @author karajan
	 *
	 */
	public static class ConnectionBuilder {

		HttpURLConnection conn;

		/**
		 *
		 * @param url
		 * @param proxy
		 * @throws MalformedURLException
		 * @throws IOException
		 * @throws CertificateException
		 * @throws KeyStoreException
		 * @throws NoSuchAlgorithmException
		 * @throws KeyManagementException
		 */
		public ConnectionBuilder(String url, one.rewind.io.requester.proxy.Proxy proxy, String method) throws MalformedURLException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException {

			if (proxy != null) {

				Authenticator.setDefault(new ProxyAuthenticator(proxy.getUsername(), proxy.getPassword()));

				if(url.matches("https://.*?") && proxy.getUsername() != null && proxy.getPassword() != null) {
					conn = (HttpsURLConnection) new URL(url).openConnection(proxy.toProxy());
				} else {
					conn = (HttpURLConnection) new URL(url).openConnection(proxy.toProxy());
				}

				if (proxy.needAuth()) {
					String headerKey = "Proxy-Authorization";
					conn.addRequestProperty(headerKey, proxy.getAuthenticationHeader());
				}

			} else {
				conn = (HttpURLConnection) new URL(url).openConnection();
			}

			if(url.matches("^https.+?$")) {
				((HttpsURLConnection) conn).setSSLSocketFactory(CertAutoInstaller.getSSLFactory());
			}

			conn.setConnectTimeout(CONNECT_TIMEOUT);

			conn.setReadTimeout(READ_TIMEOUT);

			conn.setDoOutput(true);

			conn.setRequestMethod(method);

			if(method.equals("POST")) {
				conn.setDoInput(true);
			}
		}

		/**
		 * 定义Header
		 * @param header
		 */
		public void withHeader(Map<String, String> header) {

			if(header != null) {
				for(String key: header.keySet()) {
					conn.setRequestProperty(key, header.get(key));
				}
			}
		}

		/**
		 * 定义Post Data
		 * @param postData
		 * @throws IOException
		 */
		public void withPostData (String postData) throws IOException {

			//logger.info(postData);

			if (postData != null && !postData.isEmpty()) {
				conn.setDoInput(true);
				PrintWriter out = new PrintWriter(conn.getOutputStream());
				out.print(postData);
				out.flush();
			}
		}

		/**
		 *
		 * @return
		 */
		public HttpURLConnection build() {

			//conn.setAllowUserInteraction(false);
			return conn;
		}
	}

	/**
	 * HeaderBuilder
	 *
	 * 构建请求所需的Header字段
	 */
	public static class HeaderBuilder {

		static String UserAgent = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36";
		static String Accept = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
		static String AcceptLanguage = "zh-CN,zh;q=0.8";
		static String AcceptEncoding = "zip"; //, deflate, sdch
		static String AcceptCharset = "utf-8,gb2312;q=0.8,*;q=0.8";
		static String Connection = "Keep-Alive";

		/**
		 * Build headers from url, cookies and ref url
		 * @param url
		 * @param cookie
		 * @param ref
		 * @return
		 * @throws URISyntaxException
		 * @throws MalformedURLException
		 */
		public static Map<String, String> build(String url, String cookie, String ref)
				throws URISyntaxException, MalformedURLException
		{

			String host = URLUtil.getDomainName(url);

			Map<String, String> header = new TreeMap<String, String>();
			header.put("Host", host);
			header.put("User-Agent", UserAgent);
			header.put("Accept", Accept);
			header.put("Accept-Language", AcceptLanguage);
			header.put("Accept-Encoding", AcceptEncoding);
			header.put("Accept-Charset", AcceptCharset);
			header.put("Cache-Control", "no-cache");
			header.put("Connection", Connection);
			header.put("Upgrade-Insecure-Requests", "1");
			header.put("Pragma", "no-cache");
			if(cookie != null)
				header.put("Cookie", cookie);
			if(ref != null)
				header.put("Referer", ref);

			return header;
		}
	}

	/**
	 * Request Wraper Object
	 *
	 * TODO 应将异常放在Wrapper外部捕获
	 *
	 * @author karajan
	 *
	 */
	public class Wrapper implements Runnable {

		Task<Task> task;

		Map<String, String> headers = null;
		HttpURLConnection conn = null;
		BufferedInputStream inStream = null;
		ByteArrayOutputStream bOutStream = null;

		boolean retry = false;
		int retry_count = 0;

		/**
		 *
		 * @param task
		 */
		public Wrapper(Task task) {
			this.task = task;
			task.start_time = new Date();
		}

		/**
		 *
		 */
		public void run() {

			retry_count ++;

			if(retry_count > 2) return;

			logger.info(task.url + (task.getProxy() == null? "" : " via " + task.getProxy().getInfo()));

			try {

				String cookies = null;

				String proxyHost = task.getProxy() == null ? "" : task.getProxy().getHost();

				if(task.getCookies() != null) {
					cookies = task.getCookies();
				} else {
					cookies = cookiesManager.getCookie(proxyHost, task.url);
				}

				if(task.getHeaders() != null) {
					headers = task.getHeaders();
				} else {
					headers = HeaderBuilder.build(task.url, cookies, task.getRef());
				}

				if(task.getRequestMethod().matches("POST|PUT")) {
					headers.remove("content-length");
					headers.put("Content-Length", String.valueOf(task.getPost_data().length()));
				}

				/*System.err.println("=========================================");
				System.err.println(JSON.toPrettyJson(headers));
				System.err.println(task.getPost_data());
				System.err.println("=========================================");*/

				ConnectionBuilder connBuilder =
						new ConnectionBuilder(task.url, task.getProxy(), task.getRequestMethod());

				connBuilder.withHeader(headers);
				connBuilder.withPostData(task.getPost_data());
				conn = connBuilder.build();

				int code = 0;
				try {

					inStream = new BufferedInputStream(conn.getInputStream());
					code = conn.getResponseCode();
				}
				catch (NoSuchElementException | SocketException | SocketTimeoutException e) {

					logger.error("Error Code: {}", code);
					throw e;
				}
				catch (SSLException e){

					logger.warn("Encounter: {}", e.getMessage());

					try {

						CertAutoInstaller.installCert(task.getDomain(), URLUtil.getPort(task.url));
						// 重新获取
						connBuilder = new ConnectionBuilder(task.url, task.getProxy(), task.getRequestMethod());
						connBuilder.withHeader(headers);
						connBuilder.withPostData(task.getPost_data());
						conn = connBuilder.build();
						inStream = new BufferedInputStream(conn.getInputStream());

					} catch (Exception e1){
						task.exception = e1;
					}
				}
				catch (IOException e) {
					logger.error("Error Code: {}", code);
					task.exception = e;
					inStream = new BufferedInputStream(conn.getErrorStream());
				}

				task.getResponse().setHeader(conn.getHeaderFields());

				for (Map.Entry<String, List<String>> entry : task.getResponse().getHeader().entrySet()) {

					/**
					 * 解压缩
					 */
					if (entry.getKey() != null && entry.getKey().equals("Content-Encoding")) {

						if (entry.getValue().get(0).equals("gzip")) {
							inStream = new BufferedInputStream(decompress_stream(inStream));
						}

						if (entry.getValue().get(0).equals("deflate")) {
							inStream = new BufferedInputStream(new InflaterInputStream(inStream, new Inflater(true)));
						}
					}
					/**
					 * SET ENCODE
					 */
					if (entry.getKey() != null && entry.getKey().equals("Content-Type")) {

						for (String val : entry.getValue()) {
							if (val.matches(".*?charset=.+?")) {
								task.getResponse().setEncoding(
										val.replaceAll(".*?charset=", "").replaceAll(";", "").toUpperCase()
								);
							}
						}
					}

					/**
					 * Set-Cookie
					 */
					if (entry.getKey() != null && entry.getKey().equals("Set-Cookie")) {

						Cookies.Store store = new Cookies.Store(task.url, entry.getValue());

						/*logger.info(JSON.toPrettyJson(store.store));*/

						task.getResponse().setCookies(store);

						if(task.getCookies() == null)
							cookiesManager.addCookiesHolder(proxyHost, store);
					}
				}

				byte[] buf = new byte[1024];
				bOutStream = new ByteArrayOutputStream();

				// possible read timeout here
				int size;
				while ((size = inStream.read(buf)) > 0) {
					bOutStream.write(buf, 0, size);
				}

				task.getResponse().setSrc(bOutStream.toByteArray());

				if(task.getResponse().isText()) {
					task.getResponse().setText();
				}

				if(task.buildDom()){
					task.getResponse().buildDom();
				}

				if(task.getResponse().getText() != null) {
					handleRefreshRequest(task);
				}
			}
			catch (Exception e){
				task.exception = e;
			}
			finally {

			}

			if(retry) {
				close();
				run();
			}
		}

		/**
		 * 处理内容跳转页面
		 * @param task
		 * @throws SocketTimeoutException
		 * @throws IOException
		 * @throws Exception
		 */
		private void handleRefreshRequest(Task task) throws SocketTimeoutException, IOException, Exception {

			Pattern p = Pattern.compile("(?is)<META HTTP-EQUIV=REFRESH CONTENT=['\"]\\d+;URL=(?<T>[^>]+?)['\"]>");
			Matcher m = p.matcher(task.getResponse().getText());

			if(m.find()){
				task.url = m.group("T");
				retry = true;
			}
		}

		/**
		 *
		 */
		public void close() {

			if(task != null) {
				if (bOutStream != null) {
					try {
						bOutStream.close();
					} catch (IOException e) {
						task.exception = e;
					}
				}
				if (inStream != null) {
					try {
						inStream.close();
					} catch (IOException e) {
						task.exception = e;
					}
				}
				try {
					if (conn != null) {
						conn.disconnect();
					}
				} catch (Exception e) {
					task.exception = e;
				}

				task.setDuration();
			}
		}
	}

	/**
	 * 辅助方法 终端打印 Cookies
	 * @param cookies
	 */
	public static void printCookies(String cookies) {

		Map<String, String> map = new TreeMap<String, String>();

		if(cookies != null && cookies.length() > 0) {
			String[] cookie_items = cookies.split(";");
			for(String cookie_item : cookie_items) {
				cookie_item = cookie_item.trim();
				String[] kv = cookie_item.split("=", 2);
				if(kv.length > 1) {

					map.put(kv[0], kv[1]);
				}
			}
		}

		for(String k: map.keySet()){
			System.out.println(k + "=" + map.get(k) + "; ");
		}

	}
}