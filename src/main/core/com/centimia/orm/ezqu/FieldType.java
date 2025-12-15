package com.centimia.orm.ezqu;

enum FieldType {
	NORMAL, FK, M2M, O2M, M2O;

	boolean isCollectionRelation() {
		return this == O2M || this == M2M;
	}
}