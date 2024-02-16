package com.pa.evs;
import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.pa.evs.ctrl.CommonController;
import com.pa.evs.utils.AppCodeSelectedHolder;
import com.pa.evs.utils.TimeZoneHolder;
import com.pa.evs.utils.Version;

@Order(value = Ordered.HIGHEST_PRECEDENCE)
@Component
public class TopFilter implements Filter {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TopFilter.class);
	
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {

		HttpServletResponse response = (HttpServletResponse) res;
		HttpServletRequest request = (HttpServletRequest) req;

		System.out.println(request.getRequestURI());
		
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Credentials", "true");
		response.setHeader("Access-Control-Allow-Methods", "*");
		response.setHeader("Access-Control-Allow-Headers", "*");
		response.setHeader("-vs-", Version.TEXT);

		try {
			try {
				String timeZone = request.getParameter("timeZone");
				if (StringUtils.isNotBlank(timeZone)) {
	    			TimeZoneHolder.set(timeZone);
	    		} 
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
			if (StringUtils.isNotBlank(request.getHeader("Authorization"))) {
				String ac = request.getHeader("A_C");
				if (StringUtils.isBlank(ac)) {
					AppCodeSelectedHolder.set("MMS");
				} else {
					AppCodeSelectedHolder.set(ac);	
				}
			}
			chain.doFilter(req, res);
		} finally {
			TimeZoneHolder.remove();
			CommonController.CMD_DESC.remove();
			CommonController.CMD_OPTIONS.remove();
			AppCodeSelectedHolder.remove();
		}
	}

	@Override
	public void init(FilterConfig filterConfig) {
		//
	}

	@Override
	public void destroy() {
		//
	}

}
