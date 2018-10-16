/*
 * Copyright 2014 JBoss Inc
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
package org.hibernate.bugs;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using its built-in unit test framework.
 * Although ORMStandaloneTestCase is perfectly acceptable as a reproducer, usage of this class is much preferred.
 * Since we nearly always include a regression test with bug fixes, providing your reproducer using this method
 * simplifies the process.
 * <p>
 * What's even better?  Fork hibernate-orm itself, add your test case directly to a module's unit tests, then
 * submit it as a PR!
 */
public class ORMUnitTestCase extends BaseCoreFunctionalTestCase {

	// Add your entities here.
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Parent.class,
				Child.class
		};
	}

	// Add in any settings that are specific to your test.  See resources/hibernate.properties for the defaults.
	@Override
	protected void configure(Configuration configuration) {
		super.configure(configuration);

		configuration.setProperty(AvailableSettings.SHOW_SQL, Boolean.TRUE.toString());
		configuration.setProperty(AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString());
		//configuration.setProperty( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	// Add your tests, using standard JUnit.
	@Test
	public void hhh12999CriteriaBasedTest() throws Exception {

		checkWithSession(s -> {

					CriteriaBuilder cb = s.getCriteriaBuilder();

					CriteriaQuery<Projection> cq = cb.createQuery(Projection.class);
					Root<Parent> parentRoot = cq.from(Parent.class);
					cq.multiselect(parentRoot.get("id"), parentRoot.get("child"));

					TypedQuery<Projection> typedQuery = s.createQuery(cq);
					return typedQuery.getResultList();
				},
				Projection::getId);
	}

	@Test
	public void hhh12999QlBasedTest() throws Exception {

		checkWithSession(s -> {
					return (List<Object[]>) s.createQuery("select p.id, p.child from Parent p").list();
				},
				(Object[] a) -> a[0]);

	}

	@Test
	public void hhh12999TupleBasedTest() throws Exception {

		checkWithSession(s -> {

					CriteriaBuilder cb = s.getCriteriaBuilder();

					CriteriaQuery<Tuple> cq = cb.createQuery(Tuple.class);
					Root<Parent> parentRoot = cq.from(Parent.class);
					cq.multiselect(parentRoot.get("id"), parentRoot.get("child"));

					TypedQuery<Tuple> typedQuery = s.createQuery(cq);
					return typedQuery.getResultList();
				},
				t -> t.get(0)
		);
	}


	private <T> void checkWithSession(Function<Session, List<T>> query, Function<T, ?> extractor) throws Exception {
		// BaseCoreFunctionalTestCase automatically creates the SessionFactory and provides the Session.
		Session s = openSession();
		Transaction tx = s.beginTransaction();


		Parent parentWithoutChild = new Parent();

		Parent parentWithChild = new Parent();
		parentWithChild.setChild(new Child());

		s.persist(parentWithChild.getChild());
		s.persist(parentWithChild);
		s.persist(parentWithoutChild);

		s.flush();

		List<T> result = query.apply(s);

		assertThat(result)
				.extracting(extractor)
				.containsExactlyInAnyOrder(
						tuple(parentWithChild.getId()),
						tuple(parentWithoutChild.getId())
				);

		tx.rollback();
		s.close();
	}
}
