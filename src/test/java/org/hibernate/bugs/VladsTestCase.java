/*
 * Copyright 2018 the original author or authors.
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

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Jens Schauder
 */
public class VladsTestCase extends BaseCoreFunctionalTestCase {

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

		@Test
		public void hhh12999Tuple() throws Exception {
			// BaseCoreFunctionalTestCase automatically creates the SessionFactory and provides the Session.
			Session s = openSession();
			Transaction tx = s.beginTransaction();


			Parent parentWithoutChild = new Parent();
			parentWithoutChild.setId( 1L );

			Parent parentWithChild = new Parent();
			parentWithChild.setId( 2L );
			Child child = new Child();
			child.id = 123L;
			parentWithChild.setChild(child);

			s.persist(parentWithChild.getChild());
			s.persist(parentWithChild);
			s.persist(parentWithoutChild);

			s.flush();

			CriteriaBuilder cb = s.getCriteriaBuilder();

			CriteriaQuery<Tuple> cq = cb.createQuery( Tuple.class );
			Root<Parent> parentRoot = cq.from( Parent.class );
			cq.multiselect( parentRoot.get( "id" ), parentRoot.get( "child" ) );

			TypedQuery<Tuple> typedQuery = s.createQuery( cq );
			List<Tuple> result = typedQuery.getResultList();

			assertEquals(2, result.size());

			assertEquals(2L, result.get( 0 ).get( 0 ));
			Child childEntity = (Child) result.get( 0 ).get( 1 );

			assertEquals(123L, (long) childEntity.id);

			tx.rollback();
			s.close();
		}

		@Entity(name = "Child")
		public static class Child {

			@Id
			Long id;

		}

		@Entity(name = "Parent")
		public static class Parent {

			@Id
			private Long id;

			@OneToOne(fetch = FetchType.LAZY)
			private Child child;

			public Child getChild() {
				return child;
			}

			public void setChild(Child child) {
				this.child = child;
			}


			public Long getId() {
				return id;
			}

			public void setId(Long id) {
				this.id = id;
			}
		}
}
