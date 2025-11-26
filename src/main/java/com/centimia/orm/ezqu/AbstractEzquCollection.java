/*
 * Copyright (c) 2007-2010 Centimia Ltd.
 * All rights reserved.  Unpublished -- rights reserved
 *
 * Use of a copyright notice is precautionary only, and does
 * not imply publication or disclosure.
 *
 * Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 2.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group, Centimia Inc.
 */

/*
 * Update Log
 *
 *  Date			User				Comment
 * ------			-------				--------
 * 02/02/2010		Shai Bentin			 create
 */
package com.centimia.orm.ezqu;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.centimia.orm.ezqu.TableDefinition.FieldDefinition;
import com.centimia.orm.ezqu.util.Utils;

/**
 * A special collection class used to support merging entities into running {@link Db}
 * sessions. It wraps an underlying {@link Collection} and tracks changes so that
 * child relationships can be maintained automatically when the collection is
 * modified. The collection works in two modes:
 * <ul>
 *   <li><b>Active mode</b> – When the {@code Db} session is open, every add,
 *       remove or clear operation immediately updates the database.</li>
 *   <li><b>Deferred mode</b> – When the session is closed, all changes are
 *       recorded locally and applied later when {@link #merge()} is invoked.</li>
 * </ul>
 *
 * <p>This class is abstract because concrete subclasses can be either {@link List} or {@link Set} and
 * must provide the specific type of elements they manage. The {@code originalList} field holds
 * the actual data and may be any {@link Collection} implementation.</p>
 *
 * @param &lt;E&gt; the type of elements held in this collection
 * 
 * @author Shai Bentin
 */
abstract class AbstractEzquCollection<E> implements Serializable, IEzquCollection<E> {

	private static final long serialVersionUID	= 3249922548306321787L;

	protected Collection<E> originalList;
	protected Map<E, E> internalDeleteMapping;

	protected transient WeakReference<Db> db;
	protected transient FieldDefinition definition;
	protected transient Object parentPk;

	/**
     * Constructs a new {@code AbstractEzquCollection} that wraps the given
     * collection and associates it with the supplied database session, field
     * definition and parent primary key.
     *
     * @param origList   the underlying collection that holds the elements
     * @param db         the {@link Db} session in which this collection operates
     * @param definition the field definition that describes the child relation
     * @param parentPk   the primary key of the parent entity
     */
	AbstractEzquCollection(Collection<E> origList, Db db, FieldDefinition definition, Object parentPk) {
		this.originalList = origList;
		this.db = new WeakReference<>(db);
		this.definition = definition;
		this.parentPk = parentPk;
	}

	/**
     * Adds the specified element to this collection. If the database session
     * is still open, the element is first merged into the session via
     * {@link Db#checkSession(Object)} before being added to the underlying
     * collection.
     *
     * @param e the element to be added
     * @return {@code true} if the collection changed as a result of the call
     */
	@Override
	public boolean add(E e) {
		if (!dbClosed()) {
			e = db.get().checkSession(e); // merge the Object into the DB
		}
		return originalList.add(e);
	}

	/**
     * Adds all of the elements in the specified collection to this collection.
     * Each element is processed via {@link #add(Object)} so that the database
     * session is updated accordingly.
     *
     * @param c the collection containing elements to be added
     * @return {@code true} if the collection changed as a result of the call
     */
	@Override
	public boolean addAll(Collection<? extends E> c) {
		return c.stream().anyMatch(this::add);
	}

	/**
     * Removes all elements from the collection. If the database session is
     * still open, each element is deleted from the child relation before the
     * underlying collection is cleared.  In deferred mode, all elements are
     * marked for deletion and the underlying collection is then cleared.
     */
	@Override
	public void clear() {
		if (!dbClosed()) {
			for (E e: originalList) {
				db.get().deleteChildRelation(definition, e, parentPk);
			}
			originalList.clear();
		}
		else {
			if (null == internalDeleteMapping)
				internalDeleteMapping = Utils.newHashMap();
			originalList.forEach(item -> internalDeleteMapping.put(item, null));
			originalList.clear();
		}
	}

	/**
     * Returns {@code true} if this collection contains the specified element.
     *
     * @param o element whose presence in this collection is to be tested
     * @return {@code true} if this collection contains the specified element
     */
	@Override
	public boolean contains(Object o) {
		return originalList.contains(o);
	}

	/**
     * Returns {@code true} if this collection contains all of the elements
     * in the specified collection.
     *
     * @param c collection to be checked for containment
     * @return {@code true} if this collection contains all elements of the
     *         specified collection
     */
	@Override
	public boolean containsAll(Collection<?> c) {
		return originalList.containsAll(c);
	}

	/**
     * Returns {@code true} if this collection contains no elements.
     *
     * @return {@code true} if this collection is empty
     */
	@Override
	public boolean isEmpty() {
		return originalList.isEmpty();
	}

	/**
     * Removes a single instance of the specified element from this collection,
     * if it is present. If the database session is open, the element is also
     * removed from the child relation. In deferred mode the element is
     * marked for deletion.
     *
     * @param o object to be removed from this collection, if present
     * @return {@code true} if this collection contained the specified element
     */
	@Override
	@SuppressWarnings("unchecked")
	public boolean remove(Object o) {
		boolean removed = originalList.remove(o);
		if (!removed)
			return false;

		if (!dbClosed()) {
			if (null != db.get().factory.getPrimaryKey(o))
				db.get().deleteChildRelation(definition, o, parentPk);
		}
		else {
			if (null == internalDeleteMapping)
				internalDeleteMapping = Utils.newHashMap();
			internalDeleteMapping.put((E)o, null);
		}
		return true;
	}

	/**
     * Removes from this collection all of its elements that are contained in
     * the specified collection.  Each removal is processed via {@link #remove(Object)}.
     *
     * @param c collection containing elements to be removed
     * @return {@code true} if this collection changed as a result of the call
     */
	@Override
	public boolean removeAll(Collection<?> c) {
		return c.stream().map(this::remove).reduce(false, (a, b) -> a || b);
	}

	/**
     * Retains only the elements in this collection that are contained in the
     * specified collection.  Elements not contained in the specified collection
     * are removed via {@link #remove(Object)}.
     *
     * @param c collection containing elements to be retained
     * @return {@code true} if this collection changed as a result of the call
     */
	@Override
	public boolean retainAll(Collection<?> c) {
		return this.removeIf(e -> !c.contains(e));
	}

	/**
     * Returns the number of elements in this collection.
     *
     * @return int the number of elements in this collection
     */
	@Override
	public int size() {
		return originalList.size();
	}

	/**
     * Returns an array containing all of the elements in this collection.
     * The returned array is not backed by the underlying collection.
     *
     * @return Object[] an array containing all elements of the collection
     */
	@Override
	public Object[] toArray() {
		return originalList.toArray();
	}

	/**
     * Returns an array containing all of the elements in this collection; the
     * runtime type of the returned array is that of the specified array. The
     * returned array is not backed by the underlying collection.
     *
     * @param a the array into which the elements of the collection are to
     *        be stored, if it is large enough; otherwise a new array of the
     *        same runtime type is allocated
     * @param &lt;T&gt; the component type of the array
     * @return an array containing the elements of this collection
     */
	@Override
	public <T> T[] toArray(T[] a) {
		return originalList.toArray(a);
	}

	/**
     * Returns an iterator over the elements in this collection. The iterator
     * removes elements via {@link EzquIterator#remove()}.
     *
     * @return Iterator&lt;E&gt; an iterator over the elements in this collection
     */
	@Override
	public Iterator<E> iterator() {
		return new EzquIterator(originalList.iterator());
	}

	@Override
	public String toString() {
		return originalList.stream().map(e -> e == this ? "(this Collection)" : 
			Objects.toString(e, "null")).collect(Collectors.joining(", ", "[", "]"));
    }
	
	/**
     * Checks whether the associated {@link Db} session has been closed or
     * is unavailable. In deferred mode this method returns {@code true}.
     *
     * @return {@code true} if the database session is closed or {@code null}
     */
	protected boolean dbClosed() {
		if (null == db)
			// because db is transient, on the client side it may be null.
			return true;
		Db internal = db.get();
		return internal == null || internal.closed();
	}

	/**
     * Iterator implementation that delegates to the underlying iterator
     * while ensuring that element removal updates the child relation when
     * the database session is active.
     *
     * @param &lt;E&gt; the type of elements returned by this iterator
     */
	protected class EzquIterator implements Iterator<E> {

		protected final Iterator<E> delagete;
		protected E current;

		/**
         * Creates a new iterator that wraps the provided underlying iterator.
         *
         * @param iter the underlying iterator
         */
		EzquIterator(Iterator<E> iter) {
			this.delagete = iter;
		}

		/**
         * Returns {@code true} if the iteration has more elements.
         *
         * @return {@code true} if the iteration has more elements
         */
		@Override
		public boolean hasNext() {
			return delagete.hasNext();
		}

		/**
         * Returns the next element in the iteration.
         *
         * @return the next element in the iteration
         */
		@Override
		public E next() {
			this.current = delagete.next();
			return current;
		}

		/**
         * Removes from the underlying collection the last element returned
         * by this iterator.  The removal also updates the child relation
         * if the database session is still active.
         */
		@Override
		public void remove() {
			delagete.remove();
			if (!dbClosed()) {
				if (null != db.get().factory.getPrimaryKey(current))
					db.get().deleteChildRelation(definition, current, parentPk);
			}
			else {
				if (internalDeleteMapping == null)
	                internalDeleteMapping = Utils.newHashMap();
	            internalDeleteMapping.put(current, null);
			}
		}
	}

	/**
     * Sets the {@link Db} session for this collection. The session is
     * wrapped in a {@link WeakReference} so that it can be garbage-collected
     * when no longer needed.
     *
     * @param db the new database session
     */
	void setDb(Db db) {
		this.db = new WeakReference<>(db);
	}

	/**
     * Assigns the field definition that describes the child relation.
     *
     * @param fDef the field definition to associate with this collection
     */
	void setFieldDefinition(FieldDefinition fDef) {
		this.definition = fDef;
	}

	/**
     * Sets the primary key of the parent entity to which this collection
     * belongs.
     *
     * @param parentPk the parent primary key
     */
	void setParentPk(Object parentPk) {
		this.parentPk = parentPk;
	}

	/**
     * Merges the current state of the collection into the open {@link Db}
     * session. All elements that were marked for deletion in deferred mode
     * are processed by {@link #deleteRelationIfChanged(Object, Object)}.
     */
	void merge() {
		if (null != internalDeleteMapping) {
			internalDeleteMapping.forEach(this::deleteRelationIfChanged);
		}
		internalDeleteMapping = null;
	}
	
	/**
     * Deletes the child relation if the entity has changed or been removed.
     *
     * @param oldValue the original entity instance
     * @param newValue the new entity instance; {@code null} indicates removal
     */
	private void deleteRelationIfChanged(E oldValue, E newValue) {
		Db dbRef = db.get();
		if (null == newValue) {
			if (Objects.nonNull(dbRef.factory.getPrimaryKey(oldValue)))
				dbRef.deleteChildRelation(definition, oldValue, parentPk);
		}
		else {
			var oldPk = dbRef.factory.getPrimaryKey(oldValue);
			var newPk = dbRef.factory.getPrimaryKey(newValue);
			if (Objects.nonNull(oldPk) && !oldPk.equals(newPk))
				dbRef.deleteChildRelation(definition, oldValue, parentPk);
		}
	}
}
