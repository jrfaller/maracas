package com.github.maracas.rest.breakbot;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BreakbotConfig {
	private String mvnPom;
	private List<String> mvnGoals = new ArrayList<>();
	private List<String> mvnProperties = new ArrayList<>();
	private String jarLocation;
	private final List<String> excludes = new ArrayList<>();
	private final List<GithubRepositoryConfig> clients = new ArrayList<>();

	private static final Logger logger = LogManager.getLogger(BreakbotConfig.class);

	public String getMvnPom() {
		return mvnPom;
	}

	public List<String> getMvnGoals() {
		return mvnGoals;
	}

	public List<String> getMvnProperties() {
		return mvnProperties;
	}

	public String getJarLocation() {
		return jarLocation;
	}

	public List<String> getExcludes() { return excludes; }

	public List<GithubRepositoryConfig> getClients() {
		return clients;
	}

	@JsonProperty("build")
	private void unpackBuild(Map<String, String> build) {
		if (build.containsKey("pom"))
			mvnPom = build.get("pom");
		if (build.containsKey("goals"))
			mvnGoals = Arrays.asList(build.get("goals").split(" "));
		if (build.containsKey("properties"))
			mvnProperties = Arrays.asList(build.get("properties").split(" "));
		if (build.containsKey("jar"))
			jarLocation = build.get("jar");
	}

	public static BreakbotConfig defaultConfig() {
		return new BreakbotConfig();
	}

	public static BreakbotConfig fromYaml(InputStream in) {
		try {
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			return mapper.readValue(in, BreakbotConfig.class);
		} catch (IOException e) {
			logger.warn("Couldn't parse .breakbot.yml: returning default configuration", e);
			return defaultConfig();
		}
	}

	public static BreakbotConfig fromYaml(String yaml) {
		try {
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			return mapper.readValue(yaml, BreakbotConfig.class);
		} catch (IOException e) {
			logger.warn("Couldn't parse .breakbot.yml: returning default configuration", e);
			return defaultConfig();
		}
	}
}
