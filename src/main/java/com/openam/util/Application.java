package com.openam.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import groovyjarjarpicocli.CommandLine.Command;
import groovyjarjarpicocli.CommandLine.Model.CommandSpec;
import groovyjarjarpicocli.CommandLine.ParameterException;
import groovyjarjarpicocli.CommandLine.Spec;

@SpringBootApplication
@Command(name = "com.openam.util", synopsisSubcommandLabel = "COMMAND", subcommands = {})
public class Application implements CommandLineRunner, Runnable {
	static Logger LOG = LoggerFactory.getLogger(Application.class);

	public static void main(final String[] args) {
		final var app = new SpringApplication(Application.class);
		app.setBannerMode(Banner.Mode.OFF);
		app.run(args);
	}

	@Autowired
	Configuration configuration;

	@Autowired
	RestKontroller kontroller;

	@Spec
	CommandSpec spec;

	@Override
	public void run() {
		throw new ParameterException(this.spec.commandLine(), "Missing required subcommand");
	}

	@Override
	public void run(final String... args) {
		Configuration.setInstance(this.configuration);
	}
}
