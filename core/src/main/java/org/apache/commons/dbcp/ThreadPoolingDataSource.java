/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2011 Aimluck,Inc.
 * http://www.aipo.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.apache.commons.dbcp;

import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.commons.pool.ObjectPool;

/**
 * @see PoolingDataSource
 */
public class ThreadPoolingDataSource implements DataSource {

  /** Controls access to the underlying connection */
  private boolean accessToUnderlyingConnectionAllowed = false;

  public ThreadPoolingDataSource() {
    this(null);
  }

  public ThreadPoolingDataSource(ObjectPool pool) {
    _pool = pool;
  }

  public void setPool(ObjectPool pool) throws IllegalStateException,
      NullPointerException {
    if (null != _pool) {
      throw new IllegalStateException("Pool already set");
    } else if (null == pool) {
      throw new NullPointerException("Pool must not be null.");
    } else {
      _pool = pool;
    }
  }

  public ObjectPool getPool() {
    return _pool;
  }

  /**
   * Returns the value of the accessToUnderlyingConnectionAllowed property.
   *
   * @return true if access to the underlying is allowed, false otherwise.
   */
  public boolean isAccessToUnderlyingConnectionAllowed() {
    return this.accessToUnderlyingConnectionAllowed;
  }

  /**
   * Sets the value of the accessToUnderlyingConnectionAllowed property. It
   * controls if the PoolGuard allows access to the underlying connection.
   * (Default: false)
   *
   * @param allow
   *          Access to the underlying connection is granted when true.
   */
  public void setAccessToUnderlyingConnectionAllowed(boolean allow) {
    this.accessToUnderlyingConnectionAllowed = allow;
  }

  /* JDBC_4_ANT_KEY_BEGIN */
  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return false;
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new SQLException("PoolingDataSource is not a wrapper.");
  }

  /* JDBC_4_ANT_KEY_END */

  // --- DataSource methods -----------------------------------------

  /**
   * Return a {@link java.sql.Connection} from my pool, according to the
   * contract specified by {@link ObjectPool#borrowObject}.
   */
  @SuppressWarnings("deprecation")
  @Override
  public Connection getConnection() throws SQLException {
    try {
      Connection conn = (Connection) (_pool.borrowObject());
      if (conn != null) {
        conn = new ThreadPoolGuardConnectionWrapper(conn);
      }
      return conn;
    } catch (SQLException e) {
      throw e;
    } catch (NoSuchElementException e) {
      throw new SQLNestedException("Cannot get a connection, pool error "
        + e.getMessage(), e);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new SQLNestedException("Cannot get a connection, general error", e);
    }
  }

  /**
   * Throws {@link UnsupportedOperationException}
   *
   * @throws UnsupportedOperationException
   */
  @Override
  public Connection getConnection(String uname, String passwd)
      throws SQLException {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns my log writer.
   *
   * @return my log writer
   * @see DataSource#getLogWriter
   */
  @Override
  public PrintWriter getLogWriter() {
    return _logWriter;
  }

  /**
   * Throws {@link UnsupportedOperationException}.
   *
   * @throws UnsupportedOperationException
   *           As this implementation does not support this feature.
   */
  @Override
  public int getLoginTimeout() {
    throw new UnsupportedOperationException("Login timeout is not supported.");
  }

  /**
   * Throws {@link UnsupportedOperationException}.
   *
   * @throws UnsupportedOperationException
   *           As this implementation does not support this feature.
   */
  @Override
  public void setLoginTimeout(int seconds) {
    throw new UnsupportedOperationException("Login timeout is not supported.");
  }

  /**
   * Sets my log writer.
   *
   * @see DataSource#setLogWriter
   */
  @Override
  public void setLogWriter(PrintWriter out) {
    _logWriter = out;
  }

  /** My log writer. */
  protected PrintWriter _logWriter = null;

  protected ObjectPool _pool = null;

  /**
   * PoolGuardConnectionWrapper is a Connection wrapper that makes sure a closed
   * connection cannot be used anymore.
   */
  public class ThreadPoolGuardConnectionWrapper extends DelegatingConnection {

    private Connection delegate;

    ThreadPoolGuardConnectionWrapper(Connection delegate) {
      super(delegate);
      this.delegate = delegate;
    }

    @Override
    protected void checkOpen() throws SQLException {
      if (delegate == null) {
        throw new SQLException("Connection is closed.");
      }
    }

    @Override
    public void close() throws SQLException {
      if (delegate != null) {
        this.delegate.close();
        this.delegate = null;
        super.setDelegate(null);
      }
    }

    @Override
    public boolean isClosed() throws SQLException {
      if (delegate == null) {
        return true;
      }
      return delegate.isClosed();
    }

    @Override
    public void clearWarnings() throws SQLException {
      checkOpen();
      delegate.clearWarnings();
    }

    @Override
    public void commit() throws SQLException {
      checkOpen();
      delegate.commit();
    }

    @Override
    public Statement createStatement() throws SQLException {
      checkOpen();
      return new DelegatingStatement(this, delegate.createStatement());
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency)
        throws SQLException {
      checkOpen();
      return new DelegatingStatement(this, delegate.createStatement(
        resultSetType,
        resultSetConcurrency));
    }

    @Override
    public boolean innermostDelegateEquals(Connection c) {
      Connection innerCon = super.getInnermostDelegate();
      if (innerCon == null) {
        return c == null;
      } else {
        return innerCon.equals(c);
      }
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
      checkOpen();
      return delegate.getAutoCommit();
    }

    @Override
    public String getCatalog() throws SQLException {
      checkOpen();
      return delegate.getCatalog();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
      checkOpen();
      return delegate.getMetaData();
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
      checkOpen();
      return delegate.getTransactionIsolation();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Map getTypeMap() throws SQLException {
      checkOpen();
      return delegate.getTypeMap();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
      checkOpen();
      return delegate.getWarnings();
    }

    @Override
    public int hashCode() {
      if (delegate == null) {
        return 0;
      }
      return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (obj == this) {
        return true;
      }
      // Use superclass accessor to skip access test
      Connection conn = super.getInnermostDelegate();
      if (conn == null) {
        return false;
      }
      if (obj instanceof DelegatingConnection) {
        DelegatingConnection c = (DelegatingConnection) obj;
        return c.innermostDelegateEquals(conn);
      } else {
        return conn.equals(obj);
      }
    }

    @Override
    public boolean isReadOnly() throws SQLException {
      checkOpen();
      return delegate.isReadOnly();
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
      checkOpen();
      return delegate.nativeSQL(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
      checkOpen();
      return new DelegatingCallableStatement(this, delegate.prepareCall(sql));
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType,
        int resultSetConcurrency) throws SQLException {
      checkOpen();
      return new DelegatingCallableStatement(this, delegate.prepareCall(
        sql,
        resultSetType,
        resultSetConcurrency));
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
      checkOpen();
      return new DelegatingPreparedStatement(this, delegate
        .prepareStatement(sql));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType,
        int resultSetConcurrency) throws SQLException {
      checkOpen();
      return new DelegatingPreparedStatement(this, delegate.prepareStatement(
        sql,
        resultSetType,
        resultSetConcurrency));
    }

    @Override
    public void rollback() throws SQLException {
      checkOpen();
      delegate.rollback();
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
      checkOpen();
      delegate.setAutoCommit(autoCommit);
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
      checkOpen();
      delegate.setCatalog(catalog);
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
      checkOpen();
      delegate.setReadOnly(readOnly);
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
      checkOpen();
      delegate.setTransactionIsolation(level);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void setTypeMap(Map map) throws SQLException {
      checkOpen();
      delegate.setTypeMap(map);
    }

    @Override
    public String toString() {
      if (delegate == null) {
        return "NULL";
      }
      return delegate.toString();
    }

    @Override
    public int getHoldability() throws SQLException {
      checkOpen();
      return delegate.getHoldability();
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
      checkOpen();
      delegate.setHoldability(holdability);
    }

    @Override
    public java.sql.Savepoint setSavepoint() throws SQLException {
      checkOpen();
      return delegate.setSavepoint();
    }

    @Override
    public java.sql.Savepoint setSavepoint(String name) throws SQLException {
      checkOpen();
      return delegate.setSavepoint(name);
    }

    @Override
    public void releaseSavepoint(java.sql.Savepoint savepoint)
        throws SQLException {
      checkOpen();
      delegate.releaseSavepoint(savepoint);
    }

    @Override
    public void rollback(java.sql.Savepoint savepoint) throws SQLException {
      checkOpen();
      delegate.rollback(savepoint);
    }

    @Override
    public Statement createStatement(int resultSetType,
        int resultSetConcurrency, int resultSetHoldability) throws SQLException {
      checkOpen();
      return new DelegatingStatement(this, delegate.createStatement(
        resultSetType,
        resultSetConcurrency,
        resultSetHoldability));
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType,
        int resultSetConcurrency, int resultSetHoldability) throws SQLException {
      checkOpen();
      return new DelegatingCallableStatement(this, delegate.prepareCall(
        sql,
        resultSetType,
        resultSetConcurrency,
        resultSetHoldability));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
        throws SQLException {
      checkOpen();
      return new DelegatingPreparedStatement(this, delegate.prepareStatement(
        sql,
        autoGeneratedKeys));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType,
        int resultSetConcurrency, int resultSetHoldability) throws SQLException {
      checkOpen();
      return new DelegatingPreparedStatement(this, delegate.prepareStatement(
        sql,
        resultSetType,
        resultSetConcurrency,
        resultSetHoldability));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
        throws SQLException {
      checkOpen();
      return new DelegatingPreparedStatement(this, delegate.prepareStatement(
        sql,
        columnIndexes));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames)
        throws SQLException {
      checkOpen();
      return new DelegatingPreparedStatement(this, delegate.prepareStatement(
        sql,
        columnNames));
    }

    /**
     * @see org.apache.commons.dbcp.DelegatingConnection#getDelegate()
     */
    @Override
    public Connection getDelegate() {
      if (isAccessToUnderlyingConnectionAllowed()) {
        return super.getDelegate();
      } else {
        return null;
      }
    }

    /**
     * @see org.apache.commons.dbcp.DelegatingConnection#getInnermostDelegate()
     */
    @Override
    public Connection getInnermostDelegate() {
      if (isAccessToUnderlyingConnectionAllowed()) {
        return super.getInnermostDelegate();
      } else {
        return null;
      }
    }
  }

  /**
   * @return
   * @throws SQLFeatureNotSupportedException
   */
  @Override
  public Logger getParentLogger() {
    throw new UnsupportedOperationException();
  }
}