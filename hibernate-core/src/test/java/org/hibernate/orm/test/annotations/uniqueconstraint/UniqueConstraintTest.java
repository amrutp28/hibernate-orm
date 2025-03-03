/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.uniqueconstraint;

import jakarta.persistence.PersistenceException;

import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.fail;

/**
 * @author Manuel Bernhardt
 * @author Brett Meyer
 */
@SkipForDialect(value = SybaseDialect.class,
        comment = "Sybase does not properly support unique constraints on nullable columns")
public class UniqueConstraintTest extends BaseCoreFunctionalTestCase {
	
	protected Class[] getAnnotatedClasses() {
        return new Class[]{
                Room.class,
                Building.class,
                House.class
        };
    }

	@Test
	public void testUniquenessConstraintWithSuperclassProperty() {
        Session s = openSession();
        Transaction tx = s.beginTransaction();
        Room livingRoom = new Room();
        livingRoom.setId(1l);
        livingRoom.setName("livingRoom");
        s.persist(livingRoom);
        s.flush();
        House house = new House();
        house.setId(1l);
        house.setCost(100);
        house.setHeight(1000l);
        house.setRoom(livingRoom);
        s.persist(house);
        s.flush();
        House house2 = new House();
        house2.setId(2l);
        house2.setCost(100);
        house2.setHeight(1001l);
        house2.setRoom(livingRoom);
        s.persist(house2);
        try {
            s.flush();
            fail( "Database constraint non-existent" );
        }
        catch (PersistenceException e) {
            assertTyping( JDBCException.class, e );
            //success
        }
        finally {
            tx.rollback();
            s.close();
        }
    }
    
}
