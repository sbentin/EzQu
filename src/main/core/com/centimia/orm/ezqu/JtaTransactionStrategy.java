/*
 * Copyright (c) 2025-2030 Shai Bentin & Centimia Inc..
 * All rights reserved.  Unpublished -- rights reserved
 *
 * Use of a copyright notice is precautionary only, and does
 * not imply publication or disclosure.
 *
 * THIS SOFTWARE CONTAINS CONFIDENTIAL INFORMATION AND TRADE
 * SECRETS OF Shai Bentin USE, DISCLOSURE, OR
 * REPRODUCTION IS PROHIBITED WITHOUT THE PRIOR EXPRESS
 * WRITTEN PERMISSION OF Shai Bentin & CENTIMIA, INC.
 */
package com.centimia.orm.ezqu;

/**
 * a JTA implementation of the transaction strategy.
 * We use a strategy so that we don't need to include JTA in our dependency code unless the user wants to
 */
import javax.transaction.TransactionManager;
import javax.transaction.Status;
import javax.transaction.SystemException;

public class JtaTransactionStrategy implements TransactionStrategy {
    private final TransactionManager tm;

    public JtaTransactionStrategy(TransactionManager tm) {
        this.tm = tm;
    }

    @Override
    public boolean isTransactionActive(boolean notCommitted) {
        try {        	
        	int status = tm.getStatus();
        	if (notCommitted)
        		return null != tm.getTransaction() && status != Status.STATUS_NO_TRANSACTION 
        			&& status != Status.STATUS_UNKNOWN && status != Status.STATUS_COMMITTED;
        	
            return null != tm.getTransaction() && status != Status.STATUS_NO_TRANSACTION && status != Status.STATUS_UNKNOWN;
        }
        catch (SystemException e) {
			StatementLogger.error("unable to read transaction status for an unknown reason. [" + e.getMessage() + "]");
			// we return true, so that the orm will not attempt to do anything, hoping that the transaction manager will know
			// how to take care of its problem
			return true;
		}
    }

	@Override
	public void setRollbackOnly() {
		try {
			tm.getTransaction().setRollbackOnly();
		}
		catch (IllegalStateException e) {
			String msg = "trying to roll back transaction when it is not allowed [" + e.getMessage() + "]";
			StatementLogger.error(msg);
			throw new EzquError(msg, e);
		}
		catch (SystemException e) {
			String msg = "unable to mark connection for rollback for an unknown reason. [" + e.getMessage() + "]";
			StatementLogger.error(msg);
			throw new EzquError(msg, e);
		}
	}
}
