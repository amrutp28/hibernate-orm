/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstWalker;

import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * Represents a reference to a "named" table in a query's from clause.
 *
 * @author Steve Ebersole
 */
public class NamedTableReference extends AbstractTableReference {
	private final String tableExpression;

	private String prunedTableExpression;

	public NamedTableReference(
			String tableExpression,
			String identificationVariable) {
		this( tableExpression, identificationVariable, false );
	}

	public NamedTableReference(
			String tableExpression,
			String identificationVariable,
			boolean isOptional) {
		super( identificationVariable, isOptional );
		assert tableExpression != null;
		this.tableExpression = tableExpression;
	}

	public String getTableExpression() {
		return prunedTableExpression == null ? tableExpression : prunedTableExpression;
	}

	@Override
	public String getTableId() {
		return getTableExpression();
	}

	public void setPrunedTableExpression(String prunedTableExpression) {
		this.prunedTableExpression = prunedTableExpression;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitNamedTableReference( this );
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {
		nameCollector.accept( getTableExpression() );
	}

	@Override
	public List<String> getAffectedTableNames() {
		return Collections.singletonList( getTableExpression() );
	}

	@Override
	public boolean containsAffectedTableName(String requestedName) {
		return isEmpty( requestedName ) || getTableExpression().equals( requestedName );
	}

	@Override
	public Boolean visitAffectedTableNames(Function<String, Boolean> nameCollector) {
		return nameCollector.apply( getTableExpression() );
	}

	@Override
	public TableReference resolveTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean allowFkOptimization) {
		if ( tableExpression.equals( getTableExpression() ) ) {
			return this;
		}

		throw new UnknownTableReferenceException(
				tableExpression,
				String.format(
						Locale.ROOT,
						"Unable to determine TableReference (`%s`) for `%s`",
						tableExpression,
						navigablePath
				)
		);
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean allowFkOptimization,
			boolean resolve) {
		if ( this.tableExpression.equals( tableExpression ) ) {
			return this;
		}
		return null;
	}

	@Override
	public String toString() {
		return getTableExpression() + "(" + getIdentificationVariable() + ')';
	}

}
