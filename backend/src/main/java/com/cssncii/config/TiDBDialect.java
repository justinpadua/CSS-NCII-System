package com.cssncii.config;

import org.hibernate.dialect.MySQLDialect;

/** MySQL-compatible dialect adjustments for TiDB Cloud. */
public class TiDBDialect extends MySQLDialect {
    @Override
    protected boolean supportsAliasLocks() {
        return false;
    }
}
