/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.repository.cdi;

import org.neo4j.ogm.session.Session;
import org.springframework.data.neo4j.repository.support.GraphRepositoryFactory;
import org.springframework.data.neo4j.template.Neo4jTemplate;
import org.springframework.data.repository.cdi.CdiRepositoryBean;
import org.springframework.data.repository.config.CustomRepositoryImplementationDetector;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * {@link org.springframework.data.repository.cdi.CdiRepositoryBean} to create Neo4j repository instances via CDI.
 *
 * @author Mark Paluch
 * @since 4.2
 */
public class Neo4jCdiRepositoryBean<T> extends CdiRepositoryBean<T> {
    private final Bean<Session> sessionBean;

    /**
     * Creates a new {@link Neo4jCdiRepositoryBean}.
     *
     * @param sessionBean must not be {@literal null}.
     * @param qualifiers must not be {@literal null}.
     * @param repositoryType must not be {@literal null}.
     * @param beanManager must not be {@literal null}.
     * @param detector detector for the custom {@link org.springframework.data.repository.Repository} implementations
     *        {@link CustomRepositoryImplementationDetector}, can be {@literal null}.
     */
    public Neo4jCdiRepositoryBean(Bean<Session> sessionBean, Set<Annotation> qualifiers, Class<T> repositoryType,
                                  BeanManager beanManager, CustomRepositoryImplementationDetector detector) {
        super(qualifiers, repositoryType, beanManager, detector);
        this.sessionBean = sessionBean;
    }

    @Override
    protected T create(CreationalContext<T> creationalContext, Class<T> repositoryType, Object customImplementation) {

        Session session = getDependencyInstance(sessionBean, Session.class);
        Neo4jTemplate neo4jTemplate = new Neo4jTemplate(session);

        GraphRepositoryFactory factory = new GraphRepositoryFactory(session, neo4jTemplate);
        return factory.getRepository(repositoryType, customImplementation);
    }
}
