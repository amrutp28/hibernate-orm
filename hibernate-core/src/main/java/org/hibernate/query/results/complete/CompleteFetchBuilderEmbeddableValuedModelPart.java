/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.complete;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.ResultSetMappingSqlSelection;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.query.results.ResultsHelper.impl;
import static org.hibernate.query.results.ResultsHelper.jdbcPositionToValuesArrayPosition;

/**
 * CompleteFetchBuilder for embeddable-valued ModelParts
 *
 * @author Christian Beikov
 */
public class CompleteFetchBuilderEmbeddableValuedModelPart
		implements CompleteFetchBuilder, ModelPartReferenceEmbeddable {
	private final NavigablePath navigablePath;
	private final EmbeddableValuedModelPart modelPart;
	private final List<String> columnAliases;

	public CompleteFetchBuilderEmbeddableValuedModelPart(
			NavigablePath navigablePath,
			EmbeddableValuedModelPart modelPart,
			List<String> columnAliases) {
		this.navigablePath = navigablePath;
		this.modelPart = modelPart;
		this.columnAliases = columnAliases;
	}

	@Override
	public FetchBuilder cacheKeyInstance() {
		return new CompleteFetchBuilderEmbeddableValuedModelPart(
				navigablePath,
				modelPart,
				List.copyOf( columnAliases )
		);
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public EmbeddableValuedModelPart getReferencedPart() {
		return modelPart;
	}

	@Override
	public List<String> getColumnAliases() {
		return columnAliases;
	}

	@Override
	public Fetch buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		assert fetchPath.equals( navigablePath );
		final DomainResultCreationStateImpl creationStateImpl = impl( domainResultCreationState );

		final TableGroup tableGroup = creationStateImpl.getFromClauseAccess().getTableGroup( navigablePath.getParent() );
		modelPart.forEachSelectable(
				(selectionIndex, selectableMapping) -> {
					final TableReference tableReference = tableGroup.resolveTableReference( navigablePath, selectableMapping.getContainingTableExpression() );
					final String columnAlias = columnAliases.get( selectionIndex );
					creationStateImpl.resolveSqlSelection(
							ResultsHelper.resolveSqlExpression(
									creationStateImpl,
									jdbcResultsMetadata,
									tableReference,
									selectableMapping,
									columnAlias
							),
							selectableMapping.getJdbcMapping().getJdbcJavaType(),
							null,
							creationStateImpl.getSessionFactory().getTypeConfiguration()
					);
				}
		);

		return parent.generateFetchableFetch(
				modelPart,
				fetchPath,
				FetchTiming.IMMEDIATE,
				true,
				null,
				domainResultCreationState
		);
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final CompleteFetchBuilderEmbeddableValuedModelPart that = (CompleteFetchBuilderEmbeddableValuedModelPart) o;
		return navigablePath.equals( that.navigablePath )
				&& modelPart.equals( that.modelPart )
				&& columnAliases.equals( that.columnAliases );
	}

	@Override
	public int hashCode() {
		int result = navigablePath.hashCode();
		result = 31 * result + modelPart.hashCode();
		result = 31 * result + columnAliases.hashCode();
		return result;
	}
}
