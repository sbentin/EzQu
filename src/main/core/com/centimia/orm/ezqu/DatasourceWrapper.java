/*
 * Copyright (c) 2025-2030 Centimia Ltd.
 * All rights reserved.  Unpublished -- rights reserved
 *
 * Use of a copyright notice is precautionary only, and does
 * not imply publication or disclosure.
 *
 * Licensed under Eclipse Public License, Version 2.0,
 * Initial Developer: Shai Bentin, Centimia Ltd.
 */

/*
 ISSUE			DATE			AUTHOR
-------		   ------	       --------
Created		   Oct 22, 2012			shai

*/
package com.centimia.orm.ezqu;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Wrapper;
import java.util.logging.Logger;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;


/**
 * Wrapper interface for CommonDataSource.<br>
 * This wrapper analyzes the datasource to answer simple config questions made by the factory
 * 
 * @author shai
 */
class DatasourceWrapper implements DataSource, XADataSource {

	private final CommonDataSource datasource;
	private final boolean isXA;

	public DatasourceWrapper(CommonDataSource datasource) {
		if (DataSource.class.isAssignableFrom(datasource.getClass())) {
			this.datasource = datasource;
			this.isXA = false;
		}
		else if (XADataSource.class.isAssignableFrom(datasource.getClass())) {
			this.datasource = datasource;
			this.isXA = true;
		}
		else
			throw new EzquError("%s Not a legal datasource. Must extend either javax.sql.XADatasource or javax.sql.Datasource", datasource.getClass());
	}

	@Override
	public XAConnection getXAConnection() throws SQLException {
		if (isXA)
			return ((XADataSource)datasource).getXAConnection();
		throw new EzquError("% is not an XADatasource!", datasource.getClass());
	}

	@Override
	public Connection getConnection() throws SQLException {
		if (!isXA)
			return ((DataSource)datasource).getConnection();
		throw new EzquError("% is not a Datasource!", datasource.getClass());
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return datasource.getLogWriter();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		datasource.setLogWriter(out);
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		datasource.setLoginTimeout(seconds);
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return datasource.getLoginTimeout();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return ((Wrapper)datasource).unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException, ClassCastException {
		return ((Wrapper)datasource).isWrapperFor(iface);
	}

	@Override
	public XAConnection getXAConnection(String user, String password) throws SQLException {
		if (isXA)
			return ((XADataSource)datasource).getXAConnection(user, password);
		throw new EzquError("% is not an XADatasource!", datasource.getClass());
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		if (!isXA)
			return ((DataSource)datasource).getConnection(username, password);
		throw new EzquError("% is not a Datasource!", datasource.getClass());
	}

	/**
	 * True if wrapped an XA datasource
	 * @return
	 */
	boolean isXA() {
		return this.isXA;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return this.datasource.getParentLogger();
	}
}
