/*
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.integration.movies;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 * @soundtrack Body Count - Manslaughter
 */
@Neo4jIntegrationTest
class AdvancedMappingIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeAll
	static void setupData(@Autowired Driver driver) throws IOException {

		try (BufferedReader moviesReader = new BufferedReader(
				new InputStreamReader(AdvancedMappingIT.class.getClass().getResourceAsStream("/data/movies.cypher")));
				Session session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n");
			String moviesCypher = moviesReader.lines().collect(Collectors.joining(" "));
			session.run(moviesCypher);
		}
	}

	@Test
	void pathMappingShouldWork(@Autowired PeopleRepository peopleRepository) {

		List<Person> people = peopleRepository.findAllOnShortestPathBetween("Kevin Bacon", "Meg Ryan");
		Assertions.assertThat(people)
				.hasSize(3);
	}

	interface MovieRepository extends Neo4jRepository<Movie, String> {

	}

	interface PeopleRepository extends Neo4jRepository<Person, Long> {

		@Query(""
			   + "MATCH p=shortestPath(\n"
			   + "(bacon:Person {name: $person1})-[*]-(meg:Person {name: $person2}))\n"
			   + "RETURN p"
		)
		List<Person> findAllOnShortestPathBetween(@Param("person1") String person1, @Param("person2") String person2);
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {
			System.out.println("what?");
			return neo4jConnectionSupport.getDriver();
		}
	}
}
