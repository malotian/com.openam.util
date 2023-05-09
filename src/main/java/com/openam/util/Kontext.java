package com.openam.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.thymeleaf.util.StringUtils;

import com.jcabi.aspects.Loggable;

@Component
public class Kontext {

	public static Pattern patternPROD = Pattern.compile("prd|prod|production");
	private static final Logger logger = LoggerFactory.getLogger(Kontext.class);

	boolean useProd = true;

	public File file(final String filename) {
		return Paths.get(getEnvironment() + "/" + filename).toFile();
	}

	public String getEnvironment() {
		return useProd ? "prod" : "stage";
	}

	@Loggable(Loggable.DEBUG)
	public void initilize(final String environment) throws IOException {
		useProd = !StringUtils.isEmptyOrWhitespace(environment) && Kontext.patternPROD.matcher(environment.toLowerCase()).find();

		if (!Files.exists(Paths.get(getEnvironment()))) {
			Files.createDirectories(Paths.get(getEnvironment()));
		}
	}

}