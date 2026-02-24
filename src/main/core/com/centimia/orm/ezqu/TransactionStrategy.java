/*
 * All rights reserved.  Unpublished -- rights reserved
 *
 * Use of a copyright notice is precautionary only, and does
 * not imply publication or disclosure.
 *
 * Licensed under Eclipse Public License, Version 2.0,
 * Initial Developer: Shai Bentin, Centimia Ltd.
 */
package com.centimia.orm.ezqu;

/**
 * @author shai
 */
public interface TransactionStrategy {
    
    /**
     * Checks if a transaction is currently active
     * 
     * @param notCommitted - check for transaction not committed
     * @return boolean
     */
    boolean isTransactionActive(boolean notCommitted);

    /**
     * set the transaction to roll back only
     */
	void setRollbackOnly();
}
