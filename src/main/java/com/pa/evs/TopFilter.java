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
import com.pa.evs.utils.TimeZoneHolder;

@Order(value = Ordered.HIGHEST_PRECEDENCE)
@Component
public class TopFilter implements Filter {

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

		try {
			String timeZone = request.getParameter("timeZone");
			if (StringUtils.isNotBlank(timeZone)) {
    			TimeZoneHolder.set(timeZone);
    		} 
			chain.doFilter(req, res);			
		} finally {
			TimeZoneHolder.remove();
			CommonController.CMD_DESC.remove();
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
