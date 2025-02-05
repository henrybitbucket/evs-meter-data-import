package com.pa.evs.utils;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.hibernate.dialect.ColumnAliasExtractor;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/**
 * PostgreSQL returns the raw (and possibly duplicated) column labels, so here we fix them to be more unique.
 */
public class PostgresDuplicationAwareDialect extends PostgreSQLDialect {

    private static final ColumnAliasExtractor ALIAS_EXTRACTOR = new TableAwareColumnAliasExtractor();

    public PostgresDuplicationAwareDialect() {
        super();
    }

    public PostgresDuplicationAwareDialect(DialectResolutionInfo info) {
        super(info);
    }

    public PostgresDuplicationAwareDialect(DatabaseVersion version) {
        super(version);
    }

    @Override
    public ColumnAliasExtractor getColumnAliasExtractor() {
        return ALIAS_EXTRACTOR;
    }

    private static final class TableAwareColumnAliasExtractor implements ColumnAliasExtractor {

        @Override
        public String extractColumnAlias(ResultSetMetaData metaData, int position) throws SQLException {
            return metaData.getTableName(position) + "_" + metaData.getColumnLabel(position) + "_" + position;
        }
    }
}