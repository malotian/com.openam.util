package com.openam.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import groovyjarjarpicocli.CommandLine.Command;
import groovyjarjarpicocli.CommandLine.Model.CommandSpec;
import groovyjarjarpicocli.CommandLine.ParameterException;
import groovyjarjarpicocli.CommandLine.Spec;

@SpringBootApplication
@ComponentScan(basePackages = "com.openam.*")
@Command(name = "com.openam.util", synopsisSubcommandLabel = "COMMAND", subcommands = {})
public class Application implements CommandLineRunner, Runnable {
	static Logger LOG = LoggerFactory.getLogger(Application.class);

	public static void main(final String[] args) {
		final var app = new SpringApplication(Application.class);
		app.setBannerMode(Banner.Mode.OFF);
		app.run(args);
	}

	@Spec
	CommandSpec spec;

	@Override
	public void run() {
		throw new ParameterException(spec.commandLine(), "Missing required subcommand");
	}

	@Override
	public void run(final String... args) {
	}
}
