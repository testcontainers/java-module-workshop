package org.testcontainers.workshop;

import org.springframework.graphql.client.GraphQlClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class GHService {

	private final GraphQlClient ghGraphQlClient;

	public GHService(GraphQlClient ghGraphQlClient) {
		this.ghGraphQlClient = ghGraphQlClient;
	}

//	private static final String GRAPHQL_STATS_QUERY =
//			"""
//			query Stats($owner: String!, $name: String!) {
//				repository(owner: $owner, name: $name) {
//					issues(states: OPEN) {
//						totalCount
//					}
//					pullRequests(states: OPEN) {
//						totalCount
//					}
//					stargazers {
//						totalCount
//					}
//					watchers {
//						totalCount
//					}
//					forks {
//						totalCount
//					}
//				}
//			}
//			""";

	public Mono<GitHubResponse> getStats(Map<String, Object> variables) {
		return this.ghGraphQlClient.documentName("githubStats")
						.operationName("Stats")
						.variables(variables)
						.retrieve("repository")
						.toEntity(GitHubResponse.class);

	}

}
