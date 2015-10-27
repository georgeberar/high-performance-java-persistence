package com.vladmihalcea.book.high_performance_java_persistence.jdbc.transaction;

import com.vladmihalcea.book.high_performance_java_persistence.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.high_performance_java_persistence.util.AbstractTest;
import com.vladmihalcea.book.high_performance_java_persistence.util.providers.BankEntityProvider;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * <code>AutoCommitTest</code> - Auto commit test
 *
 * @author Vlad Mihalcea
 */
public class AutoCommitTest extends AbstractTest {

    public static final String INSERT_ACCOUNT = "INSERT INTO account (id, balance) VALUES (?, ?)";

    private BankEntityProvider entityProvider = new BankEntityProvider();

    private DataSource dataSource = newDataSource();

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Override
    public void init() {
        super.init();
        doInConnection(connection -> {
            try (
                PreparedStatement accountStatement = connection.prepareStatement(INSERT_ACCOUNT);
            ) {
                accountStatement.setLong(1, 1);
                accountStatement.setLong(2, 100);
                accountStatement.executeUpdate();

                accountStatement.setLong(1, 2);
                accountStatement.setLong(2, 0);
                accountStatement.executeUpdate();
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
     public void testAutoCommit() throws SQLException {
        LOGGER.info("Test Auto Commit");

        long cents = 70;
        long fromAccountId = 1;
        long toAccountId = 2;

        DataSource dataSource = newDataSource();

        try(Connection connection = dataSource.getConnection();
            PreparedStatement transferStatement = connection.prepareStatement(
                "UPDATE account SET balance = ? WHERE id = ?"
            )
        ) {
            transferStatement.setLong(1, Math.negateExact(cents));
            transferStatement.setLong(2, fromAccountId);
            transferStatement.executeUpdate();

            transferStatement.setLong(1, cents);
            transferStatement.setLong(2, toAccountId);
            transferStatement.executeUpdate();
        }
    }

    @Test
    public void testManualCommit() throws SQLException {
        LOGGER.info("Test Manual Commit");

        long cents = 70;
        long fromAccountId = 1;
        long toAccountId = 2;

        DataSource dataSource = newDataSource();

        try(Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try(PreparedStatement transferStatement = connection.prepareStatement(
                "UPDATE account SET balance = ? WHERE id = ?"
            )) {
                transferStatement.setLong(1, Math.negateExact(cents));
                transferStatement.setLong(2, fromAccountId);
                transferStatement.executeUpdate();

                transferStatement.setLong(1, cents);
                transferStatement.setLong(2, toAccountId);
                transferStatement.executeUpdate();

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    @Test
    public void testManualCommitWithTemplate() throws SQLException {
        long cents = 70;
        long fromAccountId = 1;
        long toAccountId = 2;

        transact((Connection connection) -> {
            try (PreparedStatement transferStatement = connection.prepareStatement(
                    "UPDATE account SET balance = ? WHERE id = ?"
            )) {
                transferStatement.setLong(1, Math.negateExact(cents));
                transferStatement.setLong(2, fromAccountId);
                transferStatement.executeUpdate();

                transferStatement.setLong(1, cents);
                transferStatement.setLong(2, toAccountId);
                transferStatement.executeUpdate();
            } catch (SQLException e) {
                throw new DataAccessException(e);
            }
        });
    }
}