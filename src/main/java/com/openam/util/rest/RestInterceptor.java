package com.openam.util.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.jcabi.aspects.Loggable;
import com.openam.util.Kontext;
import com.openam.util.entity.Entity;

@Component
public class RestInterceptor implements HandlerInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(RestInterceptor.class);

	@Autowired
	Kontext kontext;

	@Override
	@Loggable(Loggable.DEBUG)
	public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) throws Exception {
		Entity.initialize();
		kontext.initilize(request.getParameter("env"));
		return true;
	}
}
