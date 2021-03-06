/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.integration.conversion_reactive;

import static org.assertj.core.api.Assertions.assertThat;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.TransactionWork;
import org.neo4j.driver.Values;
import org.neo4j.driver.summary.ResultSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.integration.shared.conversion.PersonWithCustomId;
import org.springframework.data.neo4j.integration.shared.conversion.ThingWithCustomTypes;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Gerrit Meier
 */
@Neo4jIntegrationTest
public class ReactiveCustomTypesIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final AtomicLong customIdValueGenerator = new AtomicLong();

	private final Driver driver;

	private final ReactiveNeo4jOperations neo4jOperations;

	@Autowired
	public ReactiveCustomTypesIT(Driver driver, ReactiveNeo4jOperations neo4jOperations) {
		this.driver = driver;
		this.neo4jOperations = neo4jOperations;
	}

	@BeforeEach
	void setup() {
		try (Session session = driver.session()) {
			session.writeTransaction(transaction -> {
				transaction.run("MATCH (n) detach delete n");
				transaction.run("CREATE (:CustomTypes{customType:'XYZ'})");
				return null;
			});
		}
	}

	SessionConfig getSessionConfig() {
		return SessionConfig.defaultConfig();
	}

	@Test
	void findByConvertedCustomType(@Autowired EntityWithCustomTypePropertyRepository repository) {
		StepVerifier.create(repository.findByCustomType(ThingWithCustomTypes.CustomType.of("XYZ"))).expectNextCount(1)
				.verifyComplete();
	}

	@Test
	void findByConvertedCustomTypeWithCustomQuery(@Autowired EntityWithCustomTypePropertyRepository repository) {
		StepVerifier.create(repository.findByCustomTypeCustomQuery(ThingWithCustomTypes.CustomType.of("XYZ")))
				.expectNextCount(1).verifyComplete();
	}

	@Test
	void findByConvertedCustomTypeWithSpELPropertyAccessQuery(
			@Autowired EntityWithCustomTypePropertyRepository repository) {

		StepVerifier
				.create(repository.findByCustomTypeCustomSpELPropertyAccessQuery(ThingWithCustomTypes.CustomType.of("XYZ")))
				.expectNextCount(1).verifyComplete();
	}

	@Test
	void findByConvertedCustomTypeWithSpELObjectQuery(@Autowired EntityWithCustomTypePropertyRepository repository) {
		StepVerifier.create(repository.findByCustomTypeSpELObjectQuery(ThingWithCustomTypes.CustomType.of("XYZ")))
				.expectNextCount(1).verifyComplete();
	}

	TransactionWork<ResultSummary> createPersonWithCustomId(PersonWithCustomId.PersonId assignedId) {

		return tx -> tx.run("CREATE (n:PersonWithCustomId) SET n.id = $id ", Values.parameters("id", assignedId.getId()))
				.consume();
	}

	@Test
	void deleteByCustomId() {

		PersonWithCustomId.PersonId id = new PersonWithCustomId.PersonId(customIdValueGenerator.incrementAndGet());
		try (Session session = driver.session(getSessionConfig())) {
			session.writeTransaction(createPersonWithCustomId(id));
		}

		StepVerifier.create(neo4jOperations.count(PersonWithCustomId.class)).expectNext(1L).verifyComplete();

		StepVerifier.create(neo4jOperations.deleteById(id, PersonWithCustomId.class)).verifyComplete();

		try (Session session = driver.session(getSessionConfig())) {
			Result result = session.run("MATCH (p:PersonWithCustomId) return count(p) as count");
			assertThat(result.single().get("count").asLong()).isEqualTo(0);
		}
	}

	@Test
	void deleteAllByCustomId() {

		List<PersonWithCustomId.PersonId> ids = Stream.generate(customIdValueGenerator::incrementAndGet)
				.map(PersonWithCustomId.PersonId::new).limit(2).collect(Collectors.toList());
		try (Session session = driver.session(getSessionConfig());) {
			ids.forEach(id -> session.writeTransaction(createPersonWithCustomId(id)));
		}

		StepVerifier.create(neo4jOperations.count(PersonWithCustomId.class)).expectNext(2L).verifyComplete();

		StepVerifier.create(neo4jOperations.deleteAllById(ids, PersonWithCustomId.class)).verifyComplete();

		try (Session session = driver.session(getSessionConfig())) {
			Result result = session.run("MATCH (p:PersonWithCustomId) return count(p) as count");
			assertThat(result.single().get("count").asLong()).isEqualTo(0);
		}
	}

	interface EntityWithCustomTypePropertyRepository extends ReactiveNeo4jRepository<ThingWithCustomTypes, Long> {

		Mono<ThingWithCustomTypes> findByCustomType(ThingWithCustomTypes.CustomType customType);

		@Query("MATCH (c:CustomTypes) WHERE c.customType = $customType return c")
		Mono<ThingWithCustomTypes> findByCustomTypeCustomQuery(
				@Param("customType") ThingWithCustomTypes.CustomType customType);

		@Query("MATCH (c:CustomTypes) WHERE c.customType = :#{#customType.value} return c")
		Mono<ThingWithCustomTypes> findByCustomTypeCustomSpELPropertyAccessQuery(
				@Param("customType") ThingWithCustomTypes.CustomType customType);

		@Query("MATCH (c:CustomTypes) WHERE c.customType = :#{#customType} return c")
		Mono<ThingWithCustomTypes> findByCustomTypeSpELObjectQuery(
				@Param("customType") ThingWithCustomTypes.CustomType customType);
	}

	@Configuration
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	@EnableTransactionManagement
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singletonList(ThingWithCustomTypes.class.getPackage().getName());
		}

		@Override
		public Neo4jConversions neo4jConversions() {
			Set<GenericConverter> additionalConverters = new HashSet<>();
			additionalConverters.add(new ThingWithCustomTypes.CustomTypeConverter());
			additionalConverters.add(new ThingWithCustomTypes.DifferentTypeConverter());
			additionalConverters.add(new PersonWithCustomId.CustomPersonIdConverter());

			return new Neo4jConversions(additionalConverters);
		}

	}
}
