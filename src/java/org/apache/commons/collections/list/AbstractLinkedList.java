/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//collections/src/java/org/apache/commons/collections/list/AbstractLinkedList.java,v 1.3 2003/12/28 17:58:54 scolebourne Exp $
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.apache.commons.collections.list;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.apache.commons.collections.OrderedIterator;

/**
 * An abstract implementation of a linked list which provides numerous points for
 * subclasses to override.
 * <p>
 * Overridable methods are provided to change the storage node and to change how
 * nodes are added to and removed. Hopefully, all you need for unusual subclasses
 * is here.
 * 
 * @since Commons Collections 3.0
 * @version $Revision: 1.3 $ $Date: 2003/12/28 17:58:54 $
 *
 * @author Rich Dougherty
 * @author Phil Steitz
 * @author Stephen Colebourne
 */
public abstract class AbstractLinkedList implements List {

    /*
     * Implementation notes:
     * - a standard circular doubly-linked list
     * - a marker node is stored to mark the start and the end of the list
     * - node creation and removal always occurs through createNode() and
     *   removeNode().
     * - a modification count is kept, with the same semantics as
     * {@link java.util.LinkedList}.
     * - respects {@link AbstractList#modCount}
     */

    /**
     * A {@link Node} which indicates the start and end of the list and does not
     * hold a value. The value of <code>next</code> is the first item in the
     * list. The value of of <code>previous</code> is the last item in the list.
     */
    protected transient Node header;
    /** The size of the list */
    protected transient int size;
    /** Modification count for iterators */
    protected transient int modCount;

    /**
     * Constructor that does nothing intended for deserialization.
     * <p>
     * If this constructor is used by a serializable subclass then the init()
     * method must be called.
     */
    protected AbstractLinkedList() {
        super();
    }

    /**
     * Constructs a list copying data from the specified collection.
     * 
     * @param coll  the collection to copy
     */
    protected AbstractLinkedList(Collection coll) {
        super();
        init();
        addAll(coll);
    }

    /**
     * The equivalent of a default constructor, broken out so it can be called
     * by any constructor and by <code>readObject</code>.
     * Subclasses which override this method should make sure they call super,
     * so the list is initialised properly.
     */
    protected void init() {
        header = createHeaderNode();
    }

    //-----------------------------------------------------------------------
    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return (size() == 0);
    }

    public Object get(int index) {
        Node node = getNode(index, false);
        return node.value;
    }

    //-----------------------------------------------------------------------
    public Iterator iterator() {
        return listIterator();
    }

    public ListIterator listIterator() {
        return new LinkedListIterator(this, 0);
    }

    public ListIterator listIterator(int fromIndex) {
        return new LinkedListIterator(this, fromIndex);
    }

    //-----------------------------------------------------------------------
    public int indexOf(Object value) {
        int i = 0;
        for (Node node = header.next; node != header; node = node.next) {
            if (isEqualValue(node.value, value)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public int lastIndexOf(Object value) {
        int i = size - 1;
        for (Node node = header.previous; node != header; node = node.previous) {
            if (isEqualValue(node.value, value)) {
                return i;
            }
            i--;
        }
        return -1;
    }

    public boolean contains(Object value) {
        return indexOf(value) != -1;
    }

    public boolean containsAll(Collection coll) {
        Iterator it = coll.iterator();
        while (it.hasNext()) {
            if (contains(it.next()) == false) {
                return false;
            }
        }
        return true;
    }
    
    //-----------------------------------------------------------------------
    public Object[] toArray() {
        return toArray(new Object[size]);
    }

    public Object[] toArray(Object[] array) {
        // Extend the array if needed
        if (array.length < size) {
            Class componentType = array.getClass().getComponentType();
            array = (Object[]) Array.newInstance(componentType, size);
        }
        // Copy the values into the array
        int i = 0;
        for (Node node = header.next; node != header; node = node.next, i++) {
            array[i] = node.value;
        }
        // Set the value after the last value to null
        if (array.length > size) {
            array[size] = null;
        }
        return array;
    }

    /**
     * Gets a sublist of the main list.
     * 
     * @param fromIndexInclusive  the index to start from
     * @param toIndexExclusive  the index to end at
     * @return the new sublist
     */
    public List subList(int fromIndexInclusive, int toIndexExclusive) {
        return new LinkedSubList(this, fromIndexInclusive, toIndexExclusive);
    }
    
    //-----------------------------------------------------------------------
    public boolean add(Object value) {
        addLast(value);
        return true;
    }
    
    public void add(int index, Object value) {
        Node node = getNode(index, true);
        addNodeBefore(node, value);
    }
    
    public boolean addAll(Collection coll) {
        return addAll(size, coll);
    }

    public boolean addAll(int index, Collection coll) {
        Node node = getNode(index, true);
        for (Iterator itr = coll.iterator(); itr.hasNext();) {
            Object value = itr.next();
            addNodeBefore(node, value);
        }
        return true;
    }

    //-----------------------------------------------------------------------
    public Object remove(int index) {
        Node node = getNode(index, false);
        Object oldValue = node.value;
        removeNode(node);
        return oldValue;
    }

    public boolean remove(Object value) {
        for (Node node = header.next; node != header; node = node.next) {
            if (isEqualValue(node.value, value)) {
                removeNode(node);
                return true;
            }
        }
        return false;
    }

    public boolean removeAll(Collection coll) {
        boolean modified = false;
        Iterator it = iterator();
        while (it.hasNext()) {
            if (coll.contains(it.next())) {
                it.remove();
                modified = true;
            }
        }
        return modified;
    }

    //-----------------------------------------------------------------------
    public boolean retainAll(Collection coll) {
        boolean modified = false;
        Iterator it = iterator();
        while (it.hasNext()) {
            if (coll.contains(it.next()) == false) {
                it.remove();
                modified = true;
            }
        }
        return modified;
    }

    public Object set(int index, Object value) {
        Node node = getNode(index, false);
        Object oldValue = node.value;
        updateNode(node, value);
        return oldValue;
    }

    public void clear() {
        removeAllNodes();
    }
    
    //-----------------------------------------------------------------------
    public Object getFirst() {
        Node node = header.next;
        if (node == header) {
            throw new NoSuchElementException();
        }
        return node.value;
    }

    public Object getLast() {
        Node node = header.previous;
        if (node == header) {
            throw new NoSuchElementException();
        }
        return node.value;
    }

    public boolean addFirst(Object o) {
        addNodeAfter(header, o);
        return true;
    }

    public boolean addLast(Object o) {
        addNodeBefore(header, o);
        return true;
    }

    public Object removeFirst() {
        Node node = header.next;
        if (node == header) {
            throw new NoSuchElementException();
        }
        Object oldValue = node.value;
        removeNode(node);
        return oldValue;
    }

    public Object removeLast() {
        Node node = header.previous;
        if (node == header) {
            throw new NoSuchElementException();
        }
        Object oldValue = node.value;
        removeNode(node);
        return oldValue;
    }

    //-----------------------------------------------------------------------
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof List == false) {
            return false;
        }
        List other = (List) obj;
        if (other.size() != size()) {
            return false;
        }
        ListIterator it1 = listIterator();
        ListIterator it2 = other.listIterator();
        while (it1.hasNext() && it2.hasNext()) {
            Object o1 = it1.next();
            Object o2 = it2.next();
            if (!(o1 == null ? o2 == null : o1.equals(o2)))
                return false;
        }
        return !(it1.hasNext() || it2.hasNext());
    }

    public int hashCode() {
        int hashCode = 1;
        Iterator it = iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            hashCode = 31 * hashCode + (obj == null ? 0 : obj.hashCode());
        }
        return hashCode;
    }

    public String toString() {
        if (size() == 0) {
            return "[]";
        }
        StringBuffer buf = new StringBuffer(16 * size());
        buf.append("[");

        Iterator it = iterator();
        boolean hasNext = it.hasNext();
        while (hasNext) {
            Object value = it.next();
            buf.append(value == this ? "(this Collection)" : value);
            hasNext = it.hasNext();
            if (hasNext) {
                buf.append(", ");
            }
        }
        buf.append("]");
        return buf.toString();
    }

    //-----------------------------------------------------------------------
    /**
     * Compares two values for equals.
     * This implementation uses the equals method.
     * Subclasses can override this to match differently.
     * 
     * @param value1  the first value to compare, may be null
     * @param value2  the second value to compare, may be null
     * @return true if equal
     */
    protected boolean isEqualValue(Object value1, Object value2) {
        return (value1 == value2 || (value1 == null ? false : value1.equals(value2)));
    }
    
    /**
     * Updates the node with a new value.
     * This implementation sets the value on the node.
     * Subclasses can override this to record the change.
     * 
     * @param node  node to update
     * @param value  new value of the node
     */
    protected void updateNode(Node node, Object value) {
        node.value = value;
    }

    /**
     * Creates a new node with previous, next and element all set to null.
     * This implementation creates a new empty Node.
     * Subclasses can override this to create a different class.
     * 
     * @return  newly created node
     */
    protected Node createHeaderNode() {
        return new Node();
    }

    /**
     * Creates a new node with the specified properties.
     * This implementation creates a new Node with data.
     * Subclasses can override this to create a different class.
     * 
     * @param value  value of the new node
     */
    protected Node createNode(Object value) {
        return new Node(value);
    }

    /**
     * Creates a new node with the specified object as its 
     * <code>value</code> and inserts it before <code>node</code>.
     * <p>
     * This implementation uses {@link #createNode(Object)} and {@link #addNode(Node,Node)}.
     *
     * @param node  node to insert before
     * @param value  value of the newly added node
     * @throws NullPointerException if <code>node</code> is null
     */
    protected void addNodeBefore(Node node, Object value) {
        Node newNode = createNode(value);
        addNode(newNode, node);
    }

    /**
     * Creates a new node with the specified object as its 
     * <code>value</code> and inserts it after <code>node</code>.
     * <p>
     * This implementation uses {@link #createNode(Object)} and {@link #addNode(Node,Node)}.
     * 
     * @param node  node to insert after
     * @param value  value of the newly added node
     * @throws NullPointerException if <code>node</code> is null
     */
    protected void addNodeAfter(Node node, Object value) {
        Node newNode = createNode(value);
        addNode(newNode, node.next);
    }

    /**
     * Inserts a new node into the list.
     *
     * @param nodeToInsert  new node to insert
     * @param insertBeforeNode  node to insert before
     * @throws NullPointerException if either node is null
     */
    protected void addNode(Node nodeToInsert, Node insertBeforeNode) {
        nodeToInsert.next = insertBeforeNode;
        nodeToInsert.previous = insertBeforeNode.previous;
        insertBeforeNode.previous.next = nodeToInsert;
        insertBeforeNode.previous = nodeToInsert;
        size++;
        modCount++;
    }

    /**
     * Removes the specified node from the list.
     *
     * @param node  the node to remove
     * @throws NullPointerException if <code>node</code> is null
     */
    protected void removeNode(Node node) {
        node.previous.next = node.next;
        node.next.previous = node.previous;
        size--;
        modCount++;
    }

    /**
     * Removes all nodes by resetting the circular list marker.
     */
    protected void removeAllNodes() {
        header.next = header;
        header.previous = header;
        size = 0;
        modCount++;
    }

    /**
     * Gets the node at a particular index.
     * 
     * @param index  the index, starting from 0
     * @param endMarkerAllowed  whether or not the end marker can be returned if
     * startIndex is set to the list's size
     * @throws IndexOutOfBoundsException if the index is less than 0; equal to
     * the size of the list and endMakerAllowed is false; or greater than the
     * size of the list
     */
    protected Node getNode(int index, boolean endMarkerAllowed) throws IndexOutOfBoundsException {
        // Check the index is within the bounds
        if (index < 0) {
            throw new IndexOutOfBoundsException("Couldn't get the node: " +
                    "index (" + index + ") less than zero.");
        }
        if (!endMarkerAllowed && index == size) {
            throw new IndexOutOfBoundsException("Couldn't get the node: " +
                    "index (" + index + ") is the size of the list.");
        }
        if (index > size) {
            throw new IndexOutOfBoundsException("Couldn't get the node: " +
                    "index (" + index + ") greater than the size of the " +
                    "list (" + size + ").");
        }
        // Search the list and get the node
        Node node;
        if (index < (size / 2)) {
            // Search forwards
            node = header.next;
            for (int currentIndex = 0; currentIndex < index; currentIndex++) {
                node = node.next;
            }
        } else {
            // Search backwards
            node = header;
            for (int currentIndex = size; currentIndex > index; currentIndex--) {
                node = node.previous;
            }
        }
        return node;
    }

    //-----------------------------------------------------------------------
    /**
     * Creates an iterator for the sublist.
     * 
     * @param subList  the sublist to get an iterator for
     */
    protected Iterator createSubListIterator(LinkedSubList subList) {
        return createSubListListIterator(subList, 0);
    }

    /**
     * Creates a list iterator for the sublist.
     * 
     * @param subList  the sublist to get an iterator for
     * @param fromIndex  the index to start from, relative to the sublist
     */
    protected ListIterator createSubListListIterator(LinkedSubList subList, int fromIndex) {
        return new LinkedSubListIterator(subList, fromIndex);
    }

    //-----------------------------------------------------------------------
    /**
     * Serializes the data held in this object to the stream specified.
     * <p>
     * The first serializable subclass must call this method from
     * <code>writeObject</code>.
     */
    protected void doWriteObject(ObjectOutputStream outputStream) throws IOException {
        // Write the size so we know how many nodes to read back
        outputStream.writeInt(size());
        for (Iterator itr = iterator(); itr.hasNext();) {
            outputStream.writeObject(itr.next());
        }
    }

    /**
     * Deserializes the data held in this object to the stream specified.
     * <p>
     * The first serializable subclass must call this method from
     * <code>readObject</code>.
     */
    protected void doReadObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        init();
        int size = inputStream.readInt();
        for (int i = 0; i < size; i++) {
            add(inputStream.readObject());
        }
    }

    //-----------------------------------------------------------------------
    /**
     * A node within the linked list.
     */
    protected static class Node {

        /** A pointer to the node before this node */
        protected Node previous;
        /** A pointer to the node after this node */
        protected Node next;
        /** The object contained within this node */
        protected Object value;

        /**
         * Constructs a new header node.
         */
        protected Node() {
            super();
            previous = this;
            next = this;
        }

        /**
         * Constructs a new node.
         * 
         * @param value  the value to store
         */
        protected Node(Object value) {
            super();
            this.value = value;
        }
        
        /**
         * Constructs a new node.
         * 
         * @param previous  the previous node in the list
         * @param next  the next node in the list
         * @param value  the value to store
         */
        protected Node(Node previous, Node next, Object value) {
            super();
            this.previous = previous;
            this.next = next;
            this.value = value;
        }
    }

    //-----------------------------------------------------------------------
    /**
     * A list iterator over the linked list.
     */
    protected static class LinkedListIterator implements ListIterator, OrderedIterator {
        
        /** The parent list */
        protected final AbstractLinkedList list;

        /**
         * The node that will be returned by {@link #next()}. If this is equal
         * to {@link AbstractLinkedList#header} then there are no more values to return.
         */
        protected Node next;

        /**
         * The index of {@link #next}.
         */
        protected int nextIndex;

        /**
         * The last node that was returned by {@link #next()} or {@link
         * #previous()}. Set to <code>null</code> if {@link #next()} or {@link
         * #previous()} haven't been called, or if the node has been removed
         * with {@link #remove()} or a new node added with {@link #add(Object)}.
         * Should be accesed through {@link #getLastNodeReturned()} to enforce
         * this behaviour.
         */
        protected Node current;

        /**
         * The modification count that the list is expected to have. If the list
         * doesn't have this count, then a
         * {@link java.util.ConcurrentModificationException} may be thrown by
         * the operations.
         */
        protected int expectedModCount;

        /**
         * Create a ListIterator for a list.
         * 
         * @param parent  the parent list
         * @param fromIndex  the index to start at
         */
        public LinkedListIterator(AbstractLinkedList parent, int fromIndex) throws IndexOutOfBoundsException {
            super();
            this.list = parent;
            this.expectedModCount = list.modCount;
            this.next = list.getNode(fromIndex, true);
            this.nextIndex = fromIndex;
        }

        /**
         * Checks the modification count of the list is the value that this
         * object expects.
         * 
         * @throws ConcurrentModificationException If the list's modification
         * count isn't the value that was expected.
         */
        protected void checkModCount() {
            if (list.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }

        /**
         * Gets the last node returned.
         * 
         * @throws IllegalStateException If {@link #next()} or
         * {@link #previous()} haven't been called, or if the node has been removed
         * with {@link #remove()} or a new node added with {@link #add(Object)}.
         */
        protected Node getLastNodeReturned() throws IllegalStateException {
            if (current == null) {
                throw new IllegalStateException();
            }
            return current;
        }

        public boolean hasNext() {
            return next != list.header;
        }

        public Object next() {
            checkModCount();
            if (!hasNext()) {
                throw new NoSuchElementException("No element at index " +
                        nextIndex + ".");
            }
            Object value = next.value;
            current = next;
            next = next.next;
            nextIndex++;
            return value;
        }

        public boolean hasPrevious() {
            return next.previous != list.header;
        }

        public Object previous() {
            checkModCount();
            if (!hasPrevious()) {
                throw new NoSuchElementException("Already at start of list.");
            }
            next = next.previous;
            Object value = next.value;
            current = next;
            nextIndex--;
            return value;
        }

        public int nextIndex() {
            return nextIndex;
        }

        public int previousIndex() {
            // not normally overridden, as relative to nextIndex()
            return nextIndex() - 1;
        }

        public void remove() {
            checkModCount();
            list.removeNode(getLastNodeReturned());
            current = null;
            nextIndex--;
            expectedModCount++;
        }

        public void set(Object obj) {
            checkModCount();
            getLastNodeReturned().value = obj;
        }

        public void add(Object obj) {
            checkModCount();
            list.addNodeBefore(next, obj);
            current = null;
            nextIndex++;
            expectedModCount++;
        }

    }

    //-----------------------------------------------------------------------
    /**
     * A list iterator over the linked sub list.
     */
    protected static class LinkedSubListIterator extends LinkedListIterator {
        
        /** The parent list */
        protected final LinkedSubList sub;
        
        protected LinkedSubListIterator(LinkedSubList sub, int startIndex) {
            super(sub.list, startIndex + sub.offset);
            this.sub = sub;
        }

        public boolean hasNext() {
            return (nextIndex() < sub.size);
        }

        public boolean hasPrevious() {
            return (previousIndex() >= 0);
        }

        public int nextIndex() {
            return (super.nextIndex() - sub.offset);
        }

        public void add(Object obj) {
            super.add(obj);
            sub.expectedModCount = list.modCount;
            sub.size++;
        }
        
        public void remove() {
            super.remove();
            sub.expectedModCount = list.modCount;
            sub.size--;
        }
    }
    
    //-----------------------------------------------------------------------
    /**
     * The sublist implementation for AbstractLinkedList.
     */
    protected static class LinkedSubList extends AbstractList {
        /** The main list */
        private AbstractLinkedList list;
        /** Offset from the main list */
        private int offset;
        /** Sublist size */
        private int size;
        /** Sublist modCount */
        private int expectedModCount;

        protected LinkedSubList(AbstractLinkedList list, int fromIndex, int toIndex) {
            if (fromIndex < 0) {
                throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
            }
            if (toIndex > list.size()) {
                throw new IndexOutOfBoundsException("toIndex = " + toIndex);
            }
            if (fromIndex > toIndex) {
                throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
            }
            this.list = list;
            this.offset = fromIndex;
            this.size = toIndex - fromIndex;
            this.expectedModCount = list.modCount;
        }

        public int size() {
            checkModCount();
            return size;
        }

        public Object get(int index) {
            rangeCheck(index, size);
            checkModCount();
            return list.get(index + offset);
        }

        public void add(int index, Object obj) {
            rangeCheck(index, size + 1);
            checkModCount();
            list.add(index + offset, obj);
            expectedModCount = list.modCount;
            size++;
            LinkedSubList.this.modCount++;
        }

        public Object remove(int index) {
            rangeCheck(index, size);
            checkModCount();
            Object result = list.remove(index + offset);
            expectedModCount = list.modCount;
            size--;
            LinkedSubList.this.modCount++;
            return result;
        }

        public boolean addAll(Collection coll) {
            return addAll(size, coll);
        }

        public boolean addAll(int index, Collection coll) {
            rangeCheck(index, size + 1);
            int cSize = coll.size();
            if (cSize == 0) {
                return false;
            }

            checkModCount();
            list.addAll(offset + index, coll);
            expectedModCount = list.modCount;
            size += cSize;
            LinkedSubList.this.modCount++;
            return true;
        }

        public Object set(int index, Object obj) {
            rangeCheck(index, size);
            checkModCount();
            return list.set(index + offset, obj);
        }

        public void clear() {
            checkModCount();
            Iterator it = iterator();
            while (it.hasNext()) {
                it.next();
                it.remove();
            }
        }

        public Iterator iterator() {
            checkModCount();
            return list.createSubListIterator(this);
        }

        public ListIterator listIterator(final int index) {
            rangeCheck(index, size + 1);
            checkModCount();
            return list.createSubListListIterator(this, index);
        }

        public List subList(int fromIndexInclusive, int toIndexExclusive) {
            return new LinkedSubList(list, fromIndexInclusive + offset, toIndexExclusive + offset);
        }

        protected void rangeCheck(int index, int beyond) {
            if (index < 0 || index >= beyond) {
                throw new IndexOutOfBoundsException("Index '" + index + "' out of bounds for size '" + size + "'");
            }
        }

        protected void checkModCount() {
            if (list.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }
    }
    
}
