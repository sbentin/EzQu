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
package com.centimia.orm.ezqu;

/**
 * An enumeration of compare operations.
 */
enum CompareType {
    EQUAL("=", true),
    BIGGER(">", true),
    BIGGER_EQUAL(">=", true),
    SMALLER("<", true),
    SMALLER_EQUAL("<=", true),
    NOT_EQUAL("<>", true),
    BETWEEN("BETWEEN", true),
    IS_NOT_NULL("IS NOT NULL", false),
    IS_NULL("IS NULL", false),
    LIKE("LIKE", true),
    IN("IN", false),
    NOT_IN("NOT IN", false);

    private String text;
    private boolean hasRightExpression;

    CompareType(String text, boolean hasRightExpression) {
        this.text = text;
        this.hasRightExpression = hasRightExpression;
    }

    String getString() {
        return text;
    }

    boolean hasRightExpression() {
        return hasRightExpression;
    }

}