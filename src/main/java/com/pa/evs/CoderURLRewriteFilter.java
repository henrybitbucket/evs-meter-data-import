package com.pa.evs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.ProxyHost;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.tuckey.web.filters.urlrewrite.CatchElem;
import org.tuckey.web.filters.urlrewrite.ClassRule;
import org.tuckey.web.filters.urlrewrite.Condition;
import org.tuckey.web.filters.urlrewrite.Conf;
import org.tuckey.web.filters.urlrewrite.NormalRewrittenUrl;
import org.tuckey.web.filters.urlrewrite.NormalRule;
import org.tuckey.web.filters.urlrewrite.OutboundRule;
import org.tuckey.web.filters.urlrewrite.RequestProxy;
import org.tuckey.web.filters.urlrewrite.RewrittenUrl;
import org.tuckey.web.filters.urlrewrite.RuleBase;
import org.tuckey.web.filters.urlrewrite.RuleChain;
import org.tuckey.web.filters.urlrewrite.RuleExecutionOutput;
import org.tuckey.web.filters.urlrewrite.Run;
import org.tuckey.web.filters.urlrewrite.Runnable;
import org.tuckey.web.filters.urlrewrite.SetAttribute;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;
import org.tuckey.web.filters.urlrewrite.UrlRewriter;
import org.tuckey.web.filters.urlrewrite.extend.RewriteMatch;
import org.tuckey.web.filters.urlrewrite.gzip.GzipFilter;
import org.tuckey.web.filters.urlrewrite.utils.Log;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.pa.evs.utils.ApiUtils;

/**
 * <pre>
 * 		<dependency>
			<groupId>org.tuckey</groupId>
			<artifactId>urlrewritefilter</artifactId>
			<version>4.0.3</version>
		</dependency>
		<!-- https://mvnrepository.com/artifact/commons-httpclient/commons-httpclient -->
		<dependency>
			<groupId>commons-httpclient</groupId>
			<artifactId>commons-httpclient</artifactId>
			<version>3.1.0.redhat-7</version>
			<scope>system</scope>
			<systemPath>${project.basedir}/external-jar/commons-httpclient-3.1.0.redhat-7.jar</systemPath>
		</dependency>
		
		classpath file urlrewrite.xml
		<urlrewrite>
			<rule>
				<name>Structured Homedirs</name>
				<note>
					RewriteRule ^/assets/_.* http://localhost:8080/repository/default$1
				</note>
				<from>^/assets/_(.*)</from>
				<set type="response-header" name="X-Lol">1001</set>
				<to type="proxy">http://localhost:8080/repository/default$1</to>
		<!-- 		<to type="rewrite">http://localhost:8080/repository/default$1</to> -->
			</rule>
		</urlrewrite>
		
		# Apache2
		# Access Jackrabbit file
        <Location /assets>
                Header always set "cache-control" "max-age=315360000"
                #RewriteRule ^.*[/](assets/.*)$  http://localhost:81/$1 [P,L]
                RequestHeader add "Authorization" "Basic YWRtaW46YWRtaW4="
                RewriteRule ^.*[/]assets/_(.*)$  http://localhost:8080/repository/default$1 [P,L]
        </Location>
 * </pre>
 * 
 * @author tonyk
 *
 */
//@Component
//@Order(value = Integer.MIN_VALUE + 1)
public class CoderURLRewriteFilter extends UrlRewriteFilter {

	private static final String CONFIG_LOCATION = "classpath:/urlrewrite.xml";

	/**
	 * //@Value(CONFIG_LOCATION) //private Resource resource;
	 */

	private static final Map<String, String> DETAIL_ZABBIZ = new ConcurrentHashMap<>();

	private StringBuilder config = new StringBuilder()
			.append("    <urlrewrite> ")
			.append("		<rule> ")
			.append("			<name>Structured Homedirs</name> ")
			.append("			<note> ")
			.append("				RewriteRule ^/api/v1/zabbix/(.*) http://18.140.228.95$1 ")
			.append("			</note> ")
			.append("			<from>^/api/v1/zabbix/(.*)</from> ")
			.append("			<set type=\"response-header\" name=\"X-Lol\">1001</set> ")
			.append("			<to type=\"proxy\">http://18.140.228.95/$1</to> ")
			.append("		</rule> ")
			.append("	</urlrewrite> ");

	private Resource resource = new ByteArrayResource(config.toString().getBytes());

	@Override
	protected void loadUrlRewriter(FilterConfig filterConfig) throws ServletException {
		
		try {
			checkConf(new ConfExt(filterConfig.getServletContext(), resource.getInputStream(), resource.getFilename(), ""));
		} catch (IOException ex) {
			throw new ServletException("Unable to load URL-rewrite configuration file from " + CONFIG_LOCATION, ex);
		}
	}

	@Override
	protected UrlRewriter getUrlRewriter(ServletRequest request, ServletResponse response, FilterChain chain) {

		return new UrlRewriterExt(super.getUrlRewriter(request, response, chain).getConf());
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		final HttpServletRequest hsRequest = (HttpServletRequest) request;

		if (hsRequest.getRequestURI().startsWith("/api/v1/zabbix")) {
			super.doFilter(new AddParamsToHeader(hsRequest), response, chain);
			return;
		} else {
			super.doFilter(request, response, chain);
		}

	}

	/**
	 * @see boolean
	 *      org.tuckey.web.filters.urlrewrite.NormalRewrittenUrl.doRewrite(HttpServletRequest
	 *      hsRequest, HttpServletResponse hsResponse, FilterChain chain) throws
	 *      IOException, ServletException
	 * @see HttpMethod
	 *      org.tuckey.web.filters.urlrewrite.RequestProxy.setupProxyRequest(HttpServletRequest
	 *      hsRequest, URL targetUrl) throws IOException
	 */
	public static class AddParamsToHeader extends HttpServletRequestWrapper {
		public AddParamsToHeader(HttpServletRequest request) {
			super(request);
		}

		/**
		 * @see boolean
		 *      org.tuckey.web.filters.urlrewrite.NormalRewrittenUrl.doRewrite(HttpServletRequest
		 *      hsRequest, HttpServletResponse hsResponse, FilterChain chain) throws
		 *      IOException, ServletException
		 * @see HttpMethod
		 *      org.tuckey.web.filters.urlrewrite.RequestProxy.setupProxyRequest(HttpServletRequest
		 *      hsRequest, URL targetUrl) throws IOException
		 */
		@Override
		public Enumeration<String> getHeaders(String name) {

			if (HttpHeaders.COOKIE.equalsIgnoreCase(name)) {

				return new Enumeration<String>() {
					private final Iterator<String> i = Arrays.asList(DETAIL_ZABBIZ.get("cookie")).iterator();

					public boolean hasMoreElements() {
						return i.hasNext();
					}

					public String nextElement() {
						return i.next();
					}
				};
			}

			return super.getHeaders(name);
		}

		@Override
		public String getHeader(String name) {
			String header = super.getHeader(name);
			return (header != null) ? header : super.getParameter(name);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public Enumeration getHeaderNames() {
			List<String> names = Collections.list(super.getHeaderNames());
			names.addAll(Collections.list(super.getParameterNames()));
			if (names.indexOf(HttpHeaders.AUTHORIZATION) == -1
					&& names.indexOf(HttpHeaders.AUTHORIZATION.toLowerCase()) == -1) {
				names.add(HttpHeaders.AUTHORIZATION);
			}

			if (names.indexOf(HttpHeaders.COOKIE) == -1 && names.indexOf(HttpHeaders.COOKIE.toLowerCase()) == -1) {
				names.add(HttpHeaders.COOKIE);
			}

			return Collections.enumeration(names);
		}

		@Override
		// String
		// org.tuckey.web.filters.urlrewrite.UrlRewriter.decodeRequestString(HttpServletRequest
		// request, String source)
		public String getRequestURI() {
			String requestURI = super.getRequestURI();
			try {
				String[] subs = requestURI.split("/");
				StringBuilder uri = new StringBuilder();
				for (int i = 0; i < subs.length; i++) {
					String sub = subs[i];
					sub = URLEncoder.encode(sub, "utf-8");
					/** sub = URLEncoder.encode(sub, "utf-8"); */
					uri.append(i == 0 ? "" : "/").append(sub);
				}
				return uri.toString();
			} catch (UnsupportedEncodingException e) {
				//
			}
			return requestURI;
		}

		@Override
		public StringBuffer getRequestURL() {
			StringBuffer requestURL = super.getRequestURL();
			requestURL = new StringBuffer(requestURL.toString().replace(" ", "%20"));
			return requestURL;
		}
	}

	@SuppressWarnings("rawtypes")
	private static void loginZabbiz() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
		payload.add("name", "prov-portal");
		payload.add("pas" + "sword", "pr0v-p0rtal-passw0rd");
		payload.add("enter", "Sign in");
		HttpEntity<Map> entity = new HttpEntity<>(payload, headers);
		ResponseEntity<String> response = ApiUtils.getRestTemplate().exchange("http://18.140.228." + "95/index.php",
				org.springframework.http.HttpMethod.POST, entity, String.class);

		StringBuilder ck = new StringBuilder();
		response.getHeaders().get("Set-Cookie").forEach(c -> ck.append(c.split(";")[0]).append("; "));
		DETAIL_ZABBIZ.put("cookie", ck.toString());
		System.out.println(DETAIL_ZABBIZ);
	}

//	@PostConstruct
	public void init() {
		
		try {
			new Timer().schedule(new TimerTask() {

				@Override
				public void run() {
					loginZabbiz();
				}
			}, 60l * 1000l);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static class UrlRewriterExt extends UrlRewriter {

		public UrlRewriterExt(Conf conf) {
			super(conf);
		}

	}

	public static class ConfExt extends Conf {

		private static Log log = Log.getLog(ConfExt.class);

		public ConfExt() {
			super();
		}

		public ConfExt(ServletContext context, final InputStream inputStream, String fileName, String systemId) {
			super(context, inputStream, fileName, systemId);
		}

		@SuppressWarnings("unchecked")
		protected void processConfDoc(Document doc) {
			Element rootElement = doc.getDocumentElement();

			if ("true".equalsIgnoreCase(getAttrValue(rootElement, "use-query-string")))
				setUseQueryString(true);
			if ("true".equalsIgnoreCase(getAttrValue(rootElement, "use-context"))) {
				log.debug("use-context set to true");
				setUseContext(true);
			}
			setDecodeUsing(getAttrValue(rootElement, "decode-using"));
			setDefaultMatchType(getAttrValue(rootElement, "default-match-type"));

			NodeList rootElementList = rootElement.getChildNodes();
			for (int i = 0; i < rootElementList.getLength(); i++) {
				Node node = rootElementList.item(i);

				if (node.getNodeType() == Node.ELEMENT_NODE && ((Element) node).getTagName().equals("rule")) {
					Element ruleElement = (Element) node;
					// we have a rule node
					NormalRule rule = new NormalRuleExt();

					processRuleBasics(ruleElement, rule);
					procesConditions(ruleElement, rule);
					processRuns(ruleElement, rule);

					Node toNode = ruleElement.getElementsByTagName("to").item(0);
					rule.setTo(getNodeValue(toNode));
					rule.setToType(getAttrValue(toNode, "type"));
					rule.setToContextStr(getAttrValue(toNode, "context"));
					rule.setToLast(getAttrValue(toNode, "last"));
					rule.setQueryStringAppend(getAttrValue(toNode, "qsappend"));
					if ("true".equalsIgnoreCase(getAttrValue(toNode, "encode")))
						rule.setEncodeToUrl(true);

					processSetAttributes(ruleElement, rule);

					addRule(rule);

				} else if (node.getNodeType() == Node.ELEMENT_NODE
						&& ((Element) node).getTagName().equals("class-rule")) {
					Element ruleElement = (Element) node;

					ClassRule classRule = new ClassRule();
					if ("false".equalsIgnoreCase(getAttrValue(ruleElement, "enabled")))
						classRule.setEnabled(false);
					if ("false".equalsIgnoreCase(getAttrValue(ruleElement, "last")))
						classRule.setLast(false);
					classRule.setClassStr(getAttrValue(ruleElement, "class"));
					classRule.setMethodStr(getAttrValue(ruleElement, "method"));

					addRule(classRule);

				} else if (node.getNodeType() == Node.ELEMENT_NODE
						&& ((Element) node).getTagName().equals("outbound-rule")) {

					Element ruleElement = (Element) node;
					// we have a rule node
					OutboundRule rule = new OutboundRule();

					processRuleBasics(ruleElement, rule);
					if ("true".equalsIgnoreCase(getAttrValue(ruleElement, "encodefirst")))
						rule.setEncodeFirst(true);

					procesConditions(ruleElement, rule);
					processRuns(ruleElement, rule);

					Node toNode = ruleElement.getElementsByTagName("to").item(0);
					rule.setTo(getNodeValue(toNode));
					rule.setToLast(getAttrValue(toNode, "last"));
					if ("false".equalsIgnoreCase(getAttrValue(toNode, "encode")))
						rule.setEncodeToUrl(false);

					processSetAttributes(ruleElement, rule);

					addOutboundRule(rule);

				} else if (node.getNodeType() == Node.ELEMENT_NODE && ((Element) node).getTagName().equals("catch")) {

					Element catchXMLElement = (Element) node;
					// we have a rule node
					CatchElem catchElem = new CatchElem();

					catchElem.setClassStr(getAttrValue(catchXMLElement, "class"));

					processRuns(catchXMLElement, catchElem);

					getCatchElems().add(catchElem);

				}
			}

			set(Conf.class, "docProcessed", this, Boolean.TRUE);
		}

		public static String getNodeValue(Node node) {
			if (node == null)
				return null;
			NodeList nodeList = node.getChildNodes();
			if (nodeList == null)
				return null;
			Node child = nodeList.item(0);
			if (child == null)
				return null;
			if ((child.getNodeType() == Node.TEXT_NODE)) {
				String value = ((Text) child).getData();
				return value.trim();
			}
			return null;
		}

		public static String getAttrValue(Node n, String attrName) {
			if (n == null)
				return null;
			NamedNodeMap attrs = n.getAttributes();
			if (attrs == null)
				return null;
			Node attr = attrs.getNamedItem(attrName);
			if (attr == null)
				return null;
			String val = attr.getNodeValue();
			if (val == null)
				return null;
			return val.trim();
		}

		private void processRuleBasics(Element ruleElement, RuleBase rule) {
			if ("false".equalsIgnoreCase(getAttrValue(ruleElement, "enabled")))
				rule.setEnabled(false);

			String ruleMatchType = getAttrValue(ruleElement, "match-type");
			if (StringUtils.isBlank(ruleMatchType))
				ruleMatchType = defaultMatchType;
			rule.setMatchType(ruleMatchType);

			Node nameNode = ruleElement.getElementsByTagName("name").item(0);
			rule.setName(getNodeValue(nameNode));

			Node noteNode = ruleElement.getElementsByTagName("note").item(0);
			rule.setNote(getNodeValue(noteNode));

			Node fromNode = ruleElement.getElementsByTagName("from").item(0);
			rule.setFrom(getNodeValue(fromNode));
			if ("true".equalsIgnoreCase(getAttrValue(fromNode, "casesensitive")))
				rule.setFromCaseSensitive(true);
		}

		private static void procesConditions(Element ruleElement, RuleBase rule) {
			NodeList conditionNodes = ruleElement.getElementsByTagName("condition");
			for (int j = 0; j < conditionNodes.getLength(); j++) {
				Node conditionNode = conditionNodes.item(j);
				if (conditionNode == null)
					continue;
				Condition condition = new Condition();
				condition.setValue(getNodeValue(conditionNode));
				condition.setType(getAttrValue(conditionNode, "type"));
				condition.setName(getAttrValue(conditionNode, "name"));
				condition.setNext(getAttrValue(conditionNode, "next"));
				condition.setCaseSensitive("true".equalsIgnoreCase(getAttrValue(conditionNode, "casesensitive")));
				condition.setOperator(getAttrValue(conditionNode, "operator"));
				rule.addCondition(condition);
			}
		}

		private static void processRuns(Element ruleElement, Runnable runnable) {
			NodeList runNodes = ruleElement.getElementsByTagName("run");
			for (int j = 0; j < runNodes.getLength(); j++) {
				Node runNode = runNodes.item(j);
				if (runNode == null)
					continue;
				Run run = new Run();
				processInitParams(runNode, run);
				run.setClassStr(getAttrValue(runNode, "class"));
				run.setMethodStr(getAttrValue(runNode, "method"));
				run.setJsonHandler("true".equalsIgnoreCase(getAttrValue(runNode, "jsonhandler")));
				run.setNewEachTime("true".equalsIgnoreCase(getAttrValue(runNode, "neweachtime")));
				runnable.addRun(run);
			}

			// gzip element is just a shortcut to run:
			// org.tuckey.web.filters.urlrewrite.gzip.GzipFilter
			NodeList gzipNodes = ruleElement.getElementsByTagName("gzip");
			for (int j = 0; j < gzipNodes.getLength(); j++) {
				Node runNode = gzipNodes.item(j);
				if (runNode == null)
					continue;
				Run run = new Run();
				run.setClassStr(GzipFilter.class.getName());
				run.setMethodStr("doFilter(ServletRequest, ServletResponse, FilterChain)");
				processInitParams(runNode, run);
				runnable.addRun(run);
			}
		}

		private static void processSetAttributes(Element ruleElement, RuleBase rule) {
			NodeList setNodes = ruleElement.getElementsByTagName("set");
			for (int j = 0; j < setNodes.getLength(); j++) {
				Node setNode = setNodes.item(j);
				if (setNode == null)
					continue;
				SetAttribute setAttribute = new SetAttribute();
				setAttribute.setValue(getNodeValue(setNode));
				setAttribute.setType(getAttrValue(setNode, "type"));
				setAttribute.setName(getAttrValue(setNode, "name"));
				rule.addSetAttribute(setAttribute);
			}
		}

		private static void processInitParams(Node runNode, Run run) {
			if (runNode.getNodeType() == Node.ELEMENT_NODE) {
				Element runElement = (Element) runNode;
				NodeList initParamsNodeList = runElement.getElementsByTagName("init-param");
				for (int k = 0; k < initParamsNodeList.getLength(); k++) {
					Node initParamNode = initParamsNodeList.item(k);
					if (initParamNode == null)
						continue;
					if (initParamNode.getNodeType() != Node.ELEMENT_NODE)
						continue;
					Element initParamElement = (Element) initParamNode;
					Node paramNameNode = initParamElement.getElementsByTagName("param-name").item(0);
					Node paramValueNode = initParamElement.getElementsByTagName("param-value").item(0);
					run.addInitParam(getNodeValue(paramNameNode), getNodeValue(paramValueNode));
				}
			}
		}
	}

	public static class NormalRuleExt extends NormalRule {

		private static Log log = Log.getLog(RuleExecutionOutput.class);

		public NormalRuleExt() {
			super();
		}

		public RewrittenUrl matches(final String url, final HttpServletRequest hsRequest,
				final HttpServletResponse hsResponse, RuleChain chain)
				throws IOException, ServletException, InvocationTargetException {
			RuleExecutionOutput ruleExecutionOutput = super.matchesBase(url, hsRequest, hsResponse, chain);
			if (ruleExecutionOutput == null || !ruleExecutionOutput.isRuleMatched()) {
				// no match, or run/set only match
				return null;
			}
			boolean queryStringAppend = (boolean) get(NormalRule.class, "queryStringAppend", this);
			if (queryStringAppend && hsRequest.getQueryString() != null) {
				String target = ruleExecutionOutput.getReplacedUrl();
				ruleExecutionOutput.setReplacedUrl(target + "&" + hsRequest.getQueryString());
			}

			ServletContext toServletContext = (ServletContext) get(NormalRule.class, "toServletContext", this);
			if (toServletContext != null) {
				ruleExecutionOutput.setReplacedUrlContext(toServletContext);
			}

			boolean encodeToUrl = (boolean) get(NormalRule.class, "encodeToUrl", this);
			return getRewritenUrl(toType, encodeToUrl, ruleExecutionOutput);
		}

		public static RewrittenUrl getRewritenUrl(short toType, boolean encodeToUrl,
				RuleExecutionOutput ruleExecutionOutput) {

			NormalRewrittenUrl rewrittenRequest = new NormalRewrittenUrlExt(ruleExecutionOutput);
			String toUrl = ruleExecutionOutput.getReplacedUrl();

			if (ruleExecutionOutput.isNoSubstitution()) {
				if (log.isDebugEnabled()) {
					log.debug("needs no substitution");
				}
			} else if (toType == NormalRule.TO_TYPE_REDIRECT) {
				if (log.isDebugEnabled()) {
					log.debug("needs to be redirected to " + toUrl);
				}
				rewrittenRequest.setRedirect(true);

			} else if (toType == NormalRule.TO_TYPE_PERMANENT_REDIRECT) {
				if (log.isDebugEnabled()) {
					log.debug("needs to be permanentely redirected to " + toUrl);
				}
				rewrittenRequest.setPermanentRedirect(true);

			} else if (toType == NormalRule.TO_TYPE_TEMPORARY_REDIRECT) {
				if (log.isDebugEnabled()) {
					log.debug("needs to be temporarily redirected to " + toUrl);
				}
				rewrittenRequest.setTemporaryRedirect(true);

			} else if (toType == NormalRule.TO_TYPE_PRE_INCLUDE) {
				if (log.isDebugEnabled()) {
					log.debug(toUrl + " needs to be pre included");
				}
				rewrittenRequest.setPreInclude(true);

			} else if (toType == NormalRule.TO_TYPE_POST_INCLUDE) {
				if (log.isDebugEnabled()) {
					log.debug(toUrl + " needs to be post included");
				}
				rewrittenRequest.setPostInclude(true);

			} else if (toType == NormalRule.TO_TYPE_FORWARD) {

				// pass the request to the "to" url
				if (log.isDebugEnabled()) {
					log.debug("needs to be forwarded to " + toUrl);
				}
				rewrittenRequest.setForward(true);
			} else if (toType == NormalRule.TO_TYPE_PROXY) {
				// pass the request to the "to" url
				if (log.isDebugEnabled()) {
					log.debug("needs to be proxied from " + toUrl);
				}
				rewrittenRequest.setProxy(true);
			}

			if (encodeToUrl) {
				rewrittenRequest.setEncode(true);
			} else {
				rewrittenRequest.setEncode(false);
			}

			return rewrittenRequest;
		}
	}
	
	public static class NormalRewrittenUrlExt extends NormalRewrittenUrl {

		private static Log log = Log.getLog(RewrittenUrl.class);
		
		public NormalRewrittenUrlExt(RuleExecutionOutput ruleExecutionOutput) {
			super(ruleExecutionOutput);
		}
		@Override
	    public boolean doRewrite(final HttpServletRequest hsRequest,
                final HttpServletResponse hsResponse, final FilterChain chain)
                		throws IOException, ServletException {
			
			if (isProxy()) {
				boolean requestRewritten = false;
		        String target = getTarget();
		        if (log.isTraceEnabled()) {
		            log.trace("doRewrite called");
		        }
		        
		        RewriteMatch rewriteMatch = (RewriteMatch) get(NormalRewrittenUrl.class, "rewriteMatch", this);
		        if (rewriteMatch != null) {
		            // todo: exception handling?
		            rewriteMatch.execute(hsRequest, hsResponse);
		        }
		        
		        if (hsResponse.isCommitted()) {
	                log.error("response is committed. cannot proxy " + target + ". Check that you havn't written to the response before.");
	            } else {
	            	RequestProxyExt.execute(target, hsRequest, hsResponse);
	                if (log.isTraceEnabled()) {
	                    log.trace("Proxied request to " + target);
	                }
	            }
	            requestRewritten = true;
	            return requestRewritten;
				
			}
	    	return super.doRewrite(hsRequest, hsResponse, chain);
	    }
	}
	
	public static class RequestProxyExt {
		private static final Log log = Log.getLog(RequestProxy.class);

	    /**
	     * This method performs the proxying of the request to the target address.
	     *
	     * @param target     The target address. Has to be a fully qualified address. The request is send as-is to this address.
	     * @param hsRequest  The request data which should be send to the
	     * @param hsResponse The response data which will contain the data returned by the proxied request to target.
	     * @throws java.io.IOException Passed on from the connection logic.
	     */
	    public static void execute(final String target, final HttpServletRequest hsRequest, final HttpServletResponse hsResponse) throws IOException {
	        log.info("execute, target is " + target);
	        log.info("response commit state: " + hsResponse.isCommitted());

	        if (StringUtils.isBlank(target)) {
	            log.error("The target address is not given. Please provide a target address.");
	            return;
	        }

	        log.info("checking url");
	        final URL url;
	        try {
	            url = new URL(target + (StringUtils.isBlank(hsRequest.getQueryString()) ? "" : ("?" + hsRequest.getQueryString())));
	        } catch (MalformedURLException e) {
	            log.error("The provided target url is not valid.", e);
	            return;
	        }

	        log.info("seting up the host configuration");

	        final HostConfiguration config = new HostConfiguration();

	        ProxyHost proxyHost = getUseProxyServer((String) hsRequest.getAttribute("use-proxy"));
	        if (proxyHost != null) config.setProxyHost(proxyHost);

	        final int port = url.getPort() != -1 ? url.getPort() : url.getDefaultPort();
	        config.setHost(url.getHost(), port, url.getProtocol());

	        log.info("config is " + config.toString());

	        final org.apache.commons.httpclient.HttpMethod targetRequest = setupProxyRequest(hsRequest, url);
	        if (targetRequest == null) {
	            log.error("Unsupported request method found: " + hsRequest.getMethod());
	            return;
	        }

	        //perform the reqeust to the target server
	        final HttpClient client = new HttpClient(new SimpleHttpConnectionManager());
	        if (log.isInfoEnabled()) {
	            log.info("client state" + client.getState());
	            log.info("client params" + client.getParams().toString());
	            log.info("executeMethod / fetching data ...");
	        }

	        final int result;
	        if (targetRequest instanceof EntityEnclosingMethod) {
	            final RequestProxyCustomRequestEntity requestEntity = new RequestProxyCustomRequestEntity(
	                    hsRequest.getInputStream(), hsRequest.getContentLength(), hsRequest.getContentType());
	            final EntityEnclosingMethod entityEnclosingMethod = (EntityEnclosingMethod) targetRequest;
	            entityEnclosingMethod.setRequestEntity(requestEntity);
	            result = client.executeMethod(config, entityEnclosingMethod);

	        } else {
	            result = client.executeMethod(config, targetRequest);
	        }

	        //copy the target response headers to our response
	        setupResponseHeaders(targetRequest, hsResponse);

	        InputStream originalResponseStream = targetRequest.getResponseBodyAsStream();
	        //the body might be null, i.e. for responses with cache-headers which leave out the body
	        if (originalResponseStream != null) {
	            OutputStream responseStream = hsResponse.getOutputStream();
	            copyStream(originalResponseStream, responseStream);
	        }

	        log.info("set up response, result code was " + result);
	    }

	    public static void copyStream(InputStream in, OutputStream out) throws IOException {
	        byte[] buf = new byte[65536];
	        int count;
	        while ((count = in.read(buf)) != -1) {
	            out.write(buf, 0, count);
	        }
	    }


	    public static ProxyHost getUseProxyServer(String useProxyServer) {
	        ProxyHost proxyHost = null;
	        if (useProxyServer != null) {
	            String proxyHostStr = useProxyServer;
	            int colonIdx = proxyHostStr.indexOf(':');
	            if (colonIdx != -1) {
	                proxyHostStr = proxyHostStr.substring(0, colonIdx);
	                String proxyPortStr = useProxyServer.substring(colonIdx + 1);
	                if (proxyPortStr != null && proxyPortStr.length() > 0 && proxyPortStr.matches("[0-9]+")) {
	                    int proxyPort = Integer.parseInt(proxyPortStr);
	                    proxyHost = new ProxyHost(proxyHostStr, proxyPort);
	                } else {
	                    proxyHost = new ProxyHost(proxyHostStr);
	                }
	            } else {
	                proxyHost = new ProxyHost(proxyHostStr);
	            }
	        }
	        return proxyHost;
	    }

	    @SuppressWarnings("rawtypes")
		private static org.apache.commons.httpclient.HttpMethod setupProxyRequest(final HttpServletRequest hsRequest, final URL targetUrl) throws IOException {
	        final String methodName = hsRequest.getMethod();
	        final org.apache.commons.httpclient.HttpMethod method;
	        if ("POST".equalsIgnoreCase(methodName)) {
	            PostMethod postMethod = new PostMethod();
	            InputStreamRequestEntity inputStreamRequestEntity = new InputStreamRequestEntity(hsRequest.getInputStream());
	            postMethod.setRequestEntity(inputStreamRequestEntity);
	            method = postMethod;
	        } else if ("GET".equalsIgnoreCase(methodName)) {
	            method = new GetMethod();
	        } else {
	            log.warn("Unsupported HTTP method requested: " + hsRequest.getMethod());
	            return null;
	        }

	        method.setFollowRedirects(false);
	        method.setPath(targetUrl.getPath());
	        method.setQueryString(targetUrl.getQuery());

	        Enumeration e = hsRequest.getHeaderNames();
	        if (e != null) {
	            while (e.hasMoreElements()) {
	                String headerName = (String) e.nextElement();
	                if ("host".equalsIgnoreCase(headerName)) {
	                    //the host value is set by the http client
	                    continue;
	                } else if ("content-length".equalsIgnoreCase(headerName)) {
	                    //the content-length is managed by the http client
	                    continue;
	                } else if ("accept-encoding".equalsIgnoreCase(headerName)) {
	                    //the accepted encoding should only be those accepted by the http client.
	                    //The response stream should (afaik) be deflated. If our http client does not support
	                    //gzip then the response can not be unzipped and is delivered wrong.
	                    continue;
	                }/** else if (headerName.toLowerCase().startsWith("cookie")) {
	                    //fixme : don't set any cookies in the proxied request, this needs a cleaner solution
	                    continue;
	                }*/

	                Enumeration values = hsRequest.getHeaders(headerName);
	                while (values.hasMoreElements()) {
	                    String headerValue = (String) values.nextElement();
	                    log.info("setting proxy request parameter:" + headerName + ", value: " + headerValue);
	                    method.addRequestHeader(headerName, headerValue);
	                }
	            }
	        }

	        log.info("proxy query string " + method.getQueryString());
	        return method;
	    }

	    private static void setupResponseHeaders(org.apache.commons.httpclient.HttpMethod httpMethod, HttpServletResponse hsResponse) {
	        if ( log.isInfoEnabled() ) {
	            log.info("setupResponseHeaders");
	            log.info("status text: " + httpMethod.getStatusText());
	            log.info("status line: " + httpMethod.getStatusLine());
	        }

	        //filter the headers, which are copied from the proxy response. The http lib handles those itself.
	        //Filtered out: the content encoding, the content length and cookies
	        for (int i = 0; i < httpMethod.getResponseHeaders().length; i++) {
	            Header h = httpMethod.getResponseHeaders()[i];
	            if ("content-encoding".equalsIgnoreCase(h.getName())) {
	                continue;
	            } else if ("content-length".equalsIgnoreCase(h.getName())) {
	                continue;
	            } else if ("transfer-encoding".equalsIgnoreCase(h.getName())) {
	                continue;
	            } else if (h.getName().toLowerCase().startsWith("cookie")) {
	                //retrieving a cookie which sets the session id will change the calling session: bad! So we skip this header.
	                continue;
	            } else if (h.getName().toLowerCase().startsWith("set-cookie")) {
	                //retrieving a cookie which sets the session id will change the calling session: bad! So we skip this header.
	                continue;
	            }

	            hsResponse.addHeader(h.getName(), h.getValue());
	            log.info("setting response parameter:" + h.getName() + ", value: " + h.getValue());
	        }
	        //fixme what about the response footers? (httpMethod.getResponseFooters())

	        if (httpMethod.getStatusCode() != 200) {
	            hsResponse.setStatus(httpMethod.getStatusCode());
	        }
	    }
	    
	    static class RequestProxyCustomRequestEntity  implements RequestEntity {

	     	private InputStream is = null;
	    	private long contentLength = 0;
	    	private String contentType;

	        public RequestProxyCustomRequestEntity(InputStream is, long contentLength, String contentType) {
	            super();
	            this.is = is;
	            this.contentLength = contentLength;
	            this.contentType = contentType;
	        }

	        public boolean isRepeatable() {
	            return true;
	        }

	        public String getContentType() {
	            return this.contentType;
	        }

	        public void writeRequest(OutputStream out) throws IOException {

	            try {
	                int l;
	                byte[] buffer = new byte[10240];
	                while ((l = is.read(buffer)) != -1) {
	                    out.write(buffer, 0, l);
	                }
	            } finally {
	                is.close();
	            }
	        }

	        public long getContentLength() {
	            return this.contentLength;
	        }
	    }
	}

	public static void set(Class<?> clz, String field, Object instance, Object value) {
		try {
			Field f = clz.getDeclaredField(field);
			f.setAccessible(true);
			f.set(instance, value);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Object get(Class<?> clz, String field, Object instance) {
		try {
			Field f = clz.getDeclaredField(field);
			f.setAccessible(true);
			return f.get(instance);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static Object invoke(Class<?> clz, String method, Class<?>[] argTypes, Object instance, Object... args) {
		try {
			Method m = clz.getDeclaredMethod(method, argTypes);
			m.setAccessible(true);
			return m.invoke(instance, args);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}

//http://localhost:8080/api/v1/zabbix/chart2.php?graphid=1573&from=now%2Fd&to=now&height=201&width=1083&profileIdx=web.charts.filter&_=1574

///curl -X GET http://localhost:8080/api/v1/zabbix/prov-portal-api.php?host=00000000c6d59f1c-6cdffb900038
///{"graphs":[{"graph_name":"System load","graph_id":"1573"},{"graph_name":"CPU utilization","graph_id":"1574"},{"graph_name":"Memory utilization","graph_id":"1582"},{"graph_name":"Interface wlan0: Network traffic","graph_id":"1584"},{"graph_name":"Interface eth0: Network traffic","graph_id":"1585"},{"graph_name":"mmcblk0: Disk read\/write rates","graph_id":"1586"},{"graph_name":"\/: Disk space usage","graph_id":"1588"},{"graph_name":"Prosyst - Time since last log","graph_id":"1594"},{"graph_name":"USB Devices","graph_id":"1679"}],"connected usb devices":[{"timestamp":"2021-03-14 12:59:04","value":"DWC OTG Controller\nSTM32 Virtual ComPort"},{"timestamp":"2021-03-14 12:58:03","value":"DWC OTG Controller\nSTM32 Virtual ComPort"},{"timestamp":"2021-03-14 12:57:02","value":"DWC OTG Controller\nSTM32 Virtual ComPort"},{"timestamp":"2021-03-14 12:56:01","value":"DWC OTG Controller\nSTM32 Virtual ComPort"},{"timestamp":"2021-03-14 12:55:00","value":"DWC OTG Controller\nSTM32 Virtual ComPort"},{"timestamp":"2021-03-14 12:53:59","value":"DWC OTG Controller\nSTM32 Virtual ComPort"},{"timestamp":"2021-03-14 12:52:59","value":"DWC OTG Controller\nSTM32 Virtual ComPort"}],"Failed Logins":[{"timestamp":"2021-03-14 12:59:02","value":""},{"timestamp":"2021-03-14 12:58:02","value":""},{"timestamp":"2021-03-14 12:57:01","value":""},{"timestamp":"2021-03-14 12:56:01","value":""},{"timestamp":"2021-03-14 12:55:01","value":""},{"timestamp":"2021-03-14 12:54:01","value":""},{"timestamp":"2021-03-14 12:53:01","value":""}]}