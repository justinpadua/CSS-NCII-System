package com.cssncii.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class TiDBDialectTest {
    @Test
    void pessimisticWriteLockOmitsAliasClause() {
        TiDBDialect dialect = new TiDBDialect();

        String lockClause = dialect.getForUpdateString("s1_0");

        assertEquals("for update", lockClause.trim());
        assertFalse(lockClause.toLowerCase().contains("of s1_0"));
    }
}
