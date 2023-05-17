package com.openam.util.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.jcabi.aspects.Loggable;

@Configuration
public class RestInterceptorConfig implements WebMvcConfigurer {

	@Autowired
	private RestInterceptor restInterceptor;

	@Override
	@Loggable(Loggable.DEBUG)
	public void addInterceptors(final InterceptorRegistry registry) {
		registry.addInterceptor(restInterceptor).addPathPatterns("/rest/**");
	}

}