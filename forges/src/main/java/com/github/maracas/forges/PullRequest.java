package com.github.maracas.forges;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public record PullRequest(
  Repository repository,
  int number,
  Commit base,
  Commit head,
  String baseBranch,
  String headBranch
) {
  public PullRequest {
    Objects.requireNonNull(repository);
    Objects.requireNonNull(number);
    Objects.requireNonNull(base);
    Objects.requireNonNull(head);
    Objects.requireNonNull(baseBranch);
    Objects.requireNonNull(headBranch);
  }

  public String buildGitHubDiffUrl(String file, int line) {
    return "https://github.com/%s/%s/pull/%d/files#diff-%sL%d".formatted(
      repository.owner(),
      repository.name(),
      number,
      Hashing.sha256().hashString(file, StandardCharsets.UTF_8),
      line);
  }
}
