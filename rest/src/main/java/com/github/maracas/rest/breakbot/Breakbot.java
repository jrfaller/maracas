package com.github.maracas.rest.breakbot;

import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import com.github.maracas.rest.data.MaracasReport;
import com.github.maracas.rest.data.PullRequestResponse;

public class Breakbot {
	private final URI callbackUri;
	private final String installationId;
	private static final Logger logger = LogManager.getLogger(Breakbot.class);

	public Breakbot(URI callbackUri, String installationId) {
		this.callbackUri = callbackUri;
		this.installationId = installationId;
	}

	public boolean sendPullRequestResponse(PullRequestResponse pr) {
		try {
			RestTemplate rest = new RestTemplate();

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			if (installationId != null && !installationId.isEmpty())
				headers.set("installationId", installationId);

			HttpEntity<String> request = new HttpEntity<>(pr.toJson(), headers);
			String res = rest.postForObject(callbackUri, request, String.class);

			logger.info("Sent delta back to BreakBot ({}): {}", callbackUri, res);
			return true;
		} catch (Exception e) {
			logger.error(e);
			return false;
		}
	}
}
