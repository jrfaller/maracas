package com.github.maracas.rest;

import com.github.maracas.rest.breakbot.BreakbotConfig;
import com.github.maracas.rest.breakbot.BreakbotException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class BreakbotConfigTests {
	@Test
	void testDefaultConfiguration() {
		BreakbotConfig c = BreakbotConfig.defaultConfig();
		assertThat(c.excludes(), is(empty()));
		assertThat(c.build().pom(), is("pom.xml"));
		assertThat(c.build().sources(), is(emptyString()));
		assertThat(c.build().goals(), both(hasSize(1)).and(contains("package")));
		assertThat(c.build().properties(), both(hasSize(1)).and(contains("skipTests")));
		assertThat(c.build().jar(), nullValue());
		assertThat(c.clients(), empty());
	}

	@Test
	void testInvalidConfiguration() {
		String s = "nope";
		Assertions.assertThrows(BreakbotException.class, () -> BreakbotConfig.fromYaml(s));
	}

	@Test
	void testOneClient() {
		String s = """
			clients:
			  - repository: a/b""";
		BreakbotConfig c = BreakbotConfig.fromYaml(s);
		assertThat(c.clients(), hasSize(1));

		BreakbotConfig.GitHubRepository r = c.clients().get(0);
		assertThat(r.owner(), is("a"));
		assertThat(r.name(), is("b"));
		assertThat(r.sources(), nullValue());
		assertThat(r.branch(), nullValue());
		assertThat(r.sha(), nullValue());
	}

	@Test
	void testSeveralClients() {
		String s = """
			clients:
			  - repository: a/b
			  - repository: a/c
			  - repository: b/d""";
		BreakbotConfig c = BreakbotConfig.fromYaml(s);
		assertThat(c.clients(), hasSize(3));

		BreakbotConfig.GitHubRepository r1 = c.clients().get(0);
		assertThat(r1.owner(), is("a"));
		assertThat(r1.name(), is("b"));
		assertThat(r1.sources(), nullValue());
		assertThat(r1.branch(), nullValue());
		assertThat(r1.sha(), nullValue());

		BreakbotConfig.GitHubRepository r2 = c.clients().get(1);
		assertThat(r2.owner(), is("a"));
		assertThat(r2.name(), is("c"));
		assertThat(r2.sources(), nullValue());
		assertThat(r2.branch(), nullValue());
		assertThat(r2.sha(), nullValue());

		BreakbotConfig.GitHubRepository r3 = c.clients().get(2);
		assertThat(r3.owner(), is("b"));
		assertThat(r3.name(), is("d"));
		assertThat(r3.sources(), nullValue());
		assertThat(r3.branch(), nullValue());
		assertThat(r3.sha(), nullValue());
	}

	@Test
	void testClientsWithSources() {
		String s = """
			clients:
			  - repository: a/b
			    sources: src
			  - repository: a/c""";
		BreakbotConfig c = BreakbotConfig.fromYaml(s);
		assertThat(c.clients(), hasSize(2));

		BreakbotConfig.GitHubRepository r1 = c.clients().get(0);
		assertThat(r1.owner(), is("a"));
		assertThat(r1.name(), is("b"));
		assertThat(r1.sources(), is("src"));
		assertThat(r1.branch(), nullValue());
		assertThat(r1.sha(), nullValue());

		BreakbotConfig.GitHubRepository r2 = c.clients().get(1);
		assertThat(r2.owner(), is("a"));
		assertThat(r2.name(), is("c"));
		assertThat(r2.sources(), nullValue());
		assertThat(r2.branch(), nullValue());
		assertThat(r2.sha(), nullValue());
	}

	@Test
	void testCustomBuild() {
		String s = """
			build:
			  pom: anotherpom.xml
			  goals: [a, b]
			  properties: [skipTests, skipDepClean]""";
		BreakbotConfig c = BreakbotConfig.fromYaml(s);
		assertThat(c.build().pom(), is("anotherpom.xml"));
		assertThat(c.build().sources(), is(emptyString()));
		assertThat(c.build().goals(), allOf(iterableWithSize(2), hasItem("a"), hasItem("b")));
		assertThat(c.build().properties(), allOf(iterableWithSize(2), hasItem("skipTests"), hasItem("skipDepClean")));
		assertThat(c.build().jar(), nullValue());
	}

	@Test
	void testCustomSources() {
		String s = """
			build:
			  pom: module/pom.xml
			  sources: module/src/main/java""";
		BreakbotConfig c = BreakbotConfig.fromYaml(s);
		assertThat(c.build().pom(), is("module/pom.xml"));
		assertThat(c.build().sources(), is("module/src/main/java"));
	}

	@Test
	void testCustomOutput() {
		String s = """
			build:
			  jar: build/out.jar""";
		BreakbotConfig c = BreakbotConfig.fromYaml(s);
		assertThat(c.build().pom(), is("pom.xml"));
		assertThat(c.build().goals(), hasItem("package"));
		assertThat(c.build().properties(), hasItem("skipTests"));
		assertThat(c.build().jar(), is("build/out.jar"));
	}

	@Test
	void testCustomBuildOutput() {
		String s = """
			build:
			  pom: anotherpom.xml
			  goals: [custom]
			  properties: [prop]
			  jar: build/out.jar""";
		BreakbotConfig c = BreakbotConfig.fromYaml(s);
		assertThat(c.build().pom(), is("anotherpom.xml"));
		assertThat(c.build().goals(), allOf(iterableWithSize(1), hasItem("custom")));
		assertThat(c.build().properties(), allOf(iterableWithSize(1), hasItem("prop")));
		assertThat(c.build().jar(), is("build/out.jar"));
	}

	@Test
	void testClientWithCommitOrBranch() {
		String s = """
			clients:
			  - repository: a/b
			    sources: src
			    sha: a3b98f
			  - repository: a/c
			    sha: 52f1aa
			  - repository: b/d
			  - repository: b/e
			    branch: dev""";
		BreakbotConfig c = BreakbotConfig.fromYaml(s);
		assertThat(c.clients(), hasSize(4));
		assertThat(c.clients().get(0).sha(), is("a3b98f"));
		assertThat(c.clients().get(0).branch(), nullValue());
		assertThat(c.clients().get(1).sha(), is("52f1aa"));
		assertThat(c.clients().get(1).branch(), nullValue());
		assertThat(c.clients().get(2).sha(), nullValue());
		assertThat(c.clients().get(2).branch(), nullValue());
		assertThat(c.clients().get(3).sha(), nullValue());
		assertThat(c.clients().get(3).branch(), is("dev"));
	}

	@Test
	void testWithExcludes() {
		String s = """
			excludes:
			  # '@' and '*' cannot start a YAML token, we have to quote
			  - '@Beta'
			  - '*internal*'""";
		BreakbotConfig c = BreakbotConfig.fromYaml(s);
		assertThat(c.excludes(), hasSize(2));
		assertThat(c.excludes(), hasItems("@Beta", "*internal*"));
	}

	@Test
	void testIgnoreUnknownProperties() {
		String s = """
			a: b
			excludes:
			  # '@' and '*' cannot start a YAML token, we have to quote
			  - '@Beta'
			  - '*internal*'
			b: c""";
		BreakbotConfig c = BreakbotConfig.fromYaml(s);
		assertThat(c.excludes(), hasSize(2));
		assertThat(c.excludes(), hasItems("@Beta", "*internal*"));
	}
}
