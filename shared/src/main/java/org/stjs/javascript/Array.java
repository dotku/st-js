/**
 *  Copyright 2011 Alexandru Craciun, Eyal Kaspi
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.stjs.javascript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.SortedMap;
import java.util.TreeMap;

import org.stjs.javascript.annotation.BrowserCompatibility;
import org.stjs.javascript.annotation.Template;
import org.stjs.javascript.functions.Callback1;
import org.stjs.javascript.functions.Callback3;
import org.stjs.javascript.functions.Function3;
import org.stjs.javascript.functions.Function4;

/**
 * This interface represents an array from Javascript.The value may be typed. The iteration is done on the indexes to
 * have the javascript equivalent of <br>
 * <b>for(var key in array)</b> <br>
 * The methods are prefixed with $ to let the generator know that is should generate braket access instead, i.e <br>
 * array.$get(key) => array[key] <br>
 * array.$set(key, value) => array[key]=value
 * 
 * <p>
 * The documentation of this class is mostly adapted from the ECMAScript 5.1 Specification:
 * http://www.ecma-international.org/ecma-262/5.1/
 * <p>
 * Browser compatibility information comes from: http://kangax.github.io/es5-compat-table
 * 
 * @author acraciun, npiguet
 */
public class Array<V> implements Iterable<String> {

	private static final Object UNSET = new Object();

	private ArrayStore<V> array = new PackedArrayStore<V>();
	private long length = 0;
	private long setElements = 0;
	private java.util.Map<String, V> nonArrayElements = new LinkedHashMap<String, V>();

	/**
	 * Constructs a new empty <tt>Array</tt>.
	 */
	public Array() {
		// nothing
	}

	/**
	 * Constructs a new empty <tt>Array</tt> and sets it's <tt>length</tt> property to <tt>len</tt>.
	 * 
	 * @param len
	 *            the length of this new array
	 */
	public Array(Number len) {
		double signedLen = len.doubleValue();
		double unsignedLen = JSAbstractOperations.ToUInt32(signedLen).doubleValue();
		if (signedLen != unsignedLen) {
			throw new Error("RangeError", len + " is out of range for Array length");
		}
		this.$length();
	}

	/**
	 * Constructs a new <tt>Array</tt> containing all the specified elements in the order in which they appear in the
	 * argument list. If the specified values contain exactly one element, and that element is a Number, then this
	 * constructor behaves like <tt>Array(Number)</tt>.
	 * 
	 * @param values
	 *            the values to add to this array, in the order in which they appear in the argument list
	 */
	public Array(V... values) {
		// special case when there is a single Number argument: Should behave like Array(Number)
		if (values.length == 1 && values[0] instanceof Number) {
			double signedLen = ((Number) values[0]).doubleValue();
			double unsignedLen = JSAbstractOperations.ToUInt32(signedLen).doubleValue();
			if (signedLen != unsignedLen) {
				throw new Error("RangeError", signedLen + " is out of range for Array length");
			}
			this.$length();
		} else {
			this.push(values);
		}
	}

	/**
	 * Returns an <tt>Iterator</tt> that allow this <tt>Array</tt> to be used in foreach statements. The returned
	 * iterator is designed to make Java for-each statements on <tt>Arrays</tt> match the corresponding JavaScript
	 * for-in statement. As a result, the returned iterator will iterate over the indices of the <tt>Array</tt>
	 * converted to <tt>String</tt> (the JavaScript behavior) instead of iterating directly over the values (the Java
	 * behavior).
	 * 
	 * <p>
	 * This method is marked as Deprecated because you should never call this method directly in code that will be
	 * translated to JavaScript; the generated JavaScript code will not work because the prototype of Array in
	 * JavaScript does not contain any <tt>iterator</tt> method.
	 * <p>
	 * You may safely call this method from a Java runtime if necessary.
	 */
	@Override
	@Deprecated
	@BrowserCompatibility("none")
	public Iterator<String> iterator() {
		return new Iterator<String>() {
			private Iterator<Entry<V>> arrayIter = array.entryIterator();
			private Iterator<String> nonArrayIter = nonArrayElements.keySet().iterator();

			@Override
			public boolean hasNext() {
				return arrayIter.hasNext() || nonArrayIter.hasNext();
			}

			@Override
			public String next() {
				if (arrayIter.hasNext()) {
					return Long.toString(arrayIter.next().key);
				}
				return nonArrayIter.next();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	/**
	 * Translated to <tt>array[index]</tt> in JavaScript, returns the element at the specified index in this
	 * <tt>Array</tt>.
	 * 
	 * @param index
	 *            the index
	 * @return the element a the specified index
	 */
	@Template("get")
	public V $get(int index) {
		if (index < 0) {
			// index is not an array index, so we'll look into the non-array element values
			return this.nonArrayElements.get(Integer.toString(index));
		}

		// index is an array element, so let's ask the array store
		return this.array.get(index);
	}

	/**
	 * Translated to <tt>array[index]</tt> in JavaScript, returns the element at the specified index in this
	 * <tt>Array</tt>.
	 * 
	 * @param index
	 *            the index
	 * @return the element a the specified index
	 */
	@Template("get")
	public V $get(String index) {
		Long i = toArrayIndex(index);

		if (i == null) {
			// index is not an array Index , look in the non-array elements
			return this.nonArrayElements.get(index);
		}

		return array.get(i);
	}

	private Long toArrayIndex(String index) {
		if (index == null) {
			return null;
		}
		Double asNum = JSAbstractOperations.ToNumber(index);
		Double asInt = JSAbstractOperations.ToInteger(asNum);

		if (Double.isNaN(asNum) || asInt < 0 || asInt >= JSAbstractOperations.UINT_MAX_VALUE_D - 1
				|| !asInt.equals(asNum)) {
			// index is not an array Index , look in the non-array elements
			return null;
		}
		return asInt.longValue();
	}

	/**
	 * Translated to <tt>array[index] = value</tt> in JavaScript, sets the element at the specified index in this
	 * <tt>Array</tt> to the specified value.
	 * 
	 * @param index
	 *            the index
	 * @param value
	 *            the value
	 */
	@Template("set")
	public void $set(int index, V value) {
		if (index < 0) {
			// not an array index. Set the value in the non-array properties
			this.nonArrayElements.put(JSAbstractOperations.ToString(index), value);

		} else {
			this.$set((long) index, value);
		}
	}

	/**
	 * Translated to <tt>array[index] = value</tt> in JavaScript, sets the element at the specified index in this
	 * <tt>Array</tt> to the specified value.
	 * 
	 * @param index
	 *            the index
	 * @param value
	 *            the value
	 */
	@Template("set")
	public void $set(String index, V value) {
		Long i = this.toArrayIndex(index);
		if (i == null) {
			this.nonArrayElements.put(JSAbstractOperations.ToString(index), value);
		} else {
			this.$set(i, value);
		}
	}

	private void $set(long index, V value) {
		// check if store type is correct for setting this value
		boolean isSet = array.isSet(index);
		long newLength = (long) Math.max(this.length, index + 1);
		long newSetElements = this.setElements;
		if (!isSet) {
			// unsetElement, we may need to convert between stores
			newSetElements++;
			this.switchStoreIfNeeded(newLength, newSetElements);
		}

		// store type is now correct, let's do the actual setting
		if (newLength > this.length) {
			array.padTo(newLength);
		}
		array.set(index, value);
		this.length = newLength;
		this.setElements = newSetElements;
	}

	/**
	 * Translated to <tt>array.length</tt> in JavaScript, returns the length of this <tt>Array</tt>. The returned value
	 * is always greater than the highest index in this <tt>Array</tt>.
	 * 
	 * @return the length of this <tt>Array</tt>.
	 */
	@Template("toProperty")
	public int $length() {
		return (int) this.length;
	}

	/**
	 * Translated to <tt>array.length = newLength</tt> in JavaScript, sets the length of this <tt>Array</tt>.
	 * 
	 * <p>
	 * Attempting to set the length property of this <tt>Array</tt> to a value that is numerically less than or equal to
	 * the largest index contained within this <tt>Array</tt> will result in the <tt>Array</tt> being truncated to the
	 * new length.
	 * 
	 * @param newLength
	 *            the new length of this <tt>Array</tt>
	 */
	@Template("toProperty")
	public void $length(int newLength) {
		if (newLength == this.length) {
			// nothing to do, we are not changing the length of the array
			return;

		} else if (newLength > this.length) {
			// growing the array
			this.switchStoreIfNeeded(newLength, this.setElements);
			this.array.padTo(newLength);
			this.length = newLength;

		} else {
			// truncating the array
			long newSetElements = 0;
			if (newLength > 0) {
				newSetElements = this.setElements - this.array.getSetElements(newLength - 1, this.length);
			}
			this.switchStoreIfNeeded(newLength, newSetElements);
			this.array.truncateFrom(newLength);
			this.length = newLength;
			this.setElements = newSetElements;
		}
	}

	private void switchStoreIfNeeded(long newLength, long newSetElements) {
		if (!this.array.isEfficientStoreFor(newLength, newSetElements)) {
			this.array = this.array.switchStoreType();
		}
	}

	/**
	 * Appends all the elements of each of the specified <tt>Arrays</tt> in order to the elements of this <tt>Array</tt>
	 * . This method does not change the existing arrays, but returns a new array, containing the values of the joined
	 * arrays.
	 * 
	 * @param arrays
	 *            the <tt>Arrays</tt> to be concatenated to this <tt>Array</tt>
	 * @return a new <tt>Array</tt>, containing all the elements of the joined <tt>Arrays</tt>.
	 */
	public Array<V> concat(Array<V>... arrays) {
		// Array<V> concat = new Array<V>();
		// for (Array<V> a : arrays) {
		// concat.splice(concat.$length(), 0, a.toArray());
		// }
		// this.splice(this.$length(), 0, concat.toArray());
		// return concat;
		return null;
	}

	/**
	 * Appends all the specified elements in the order they appear in the arguments list to the elements of this
	 * <tt>Array</tt>. This method does not change the existing arrays, but returns a new array, containing the values
	 * of the joined arrays.
	 * 
	 * @param values
	 *            the elements to be concatenated to this <tt>Array</tt>
	 * @return a new <tt>Array</tt>, containing all the elements of the joined <tt>Arrays</tt>.
	 */
	public Array<V> concat(V... values) {
		Array<V> concat = new Array<V>();
		concat.splice(0, 0, values);
		this.splice(this.$length(), 0, values);
		return concat;
	}

	/**
	 * Searches this <tt>Array</tt> for the specified item, and returns its position. Items are compared for equality
	 * using the === operator.
	 * 
	 * <p>
	 * The search will start at the beginning and end at the end of the <tt>Array</tt>.
	 * 
	 * <p>
	 * Returns -1 if the item is not found.
	 * 
	 * <p>
	 * If the item is present more than once, <tt>indexOf</tt> returns the position of the first occurrence.
	 * 
	 * @param element
	 *            the item to search for
	 * @return the index at which the element was found, -1 if not found
	 */
	@BrowserCompatibility("IE:9+")
	public int indexOf(V element) {
		// return array.indexOf(element);
		return 0;
	}

	/**
	 * Searches this <tt>Array</tt> for the specified item, and returns its position. Items are compared for equality
	 * using the === operator.
	 * 
	 * <p>
	 * The search will start at the specified position and end the search at the end of the <tt>Array</tt>.
	 * 
	 * <p>
	 * Returns -1 if the item is not found.
	 * 
	 * <p>
	 * If the specified position is greater than or equal to the length of this <tt>array</tt>, this <tt>Array</tt> is
	 * not searched and -1 is returned. If <tt>start</tt> is negative, it will be treated as <tt>length+start</tt>. If
	 * the computed starting element is less than 0, this whole <tt>Array</tt> will be searched.
	 * 
	 * <p>
	 * If the item is present more than once, <tt>indexOf</tt> method returns the position of the first occurrence.
	 * 
	 * @param element
	 *            the item to search for
	 * @param start
	 *            where to start the search. Negative values will start at the given position counting from the end
	 * @return the index at which the element was found, -1 if not found
	 */
	@BrowserCompatibility("IE:9+")
	public int indexOf(V element, int start) {
		// int pos = array.subList(start, array.size()).indexOf(element);
		// if (pos < 0) {
		// return pos;
		// }
		// return pos + start;
		return 0;
	}

	/**
	 * Converts all of the elements of this <tt>Array</tt> to <tt>Strings</tt>, and concatenates these <tt>Strings</tt>
	 * using a single comma (",") as a separator.
	 * 
	 * <p>
	 * If this <tt>Array</tt> is empty, the empty string is returned.
	 * 
	 * @return the string representation of the values in this <tt>Array</tt>, separated by a comma.
	 */
	public String join() {
		return join(",");
	}

	/**
	 * Converts all of the elements of this <tt>Array</tt> to <tt>Strings</tt>, and concatenates these <tt>Strings</tt>
	 * using the specified separator.
	 * 
	 * <p>
	 * If this <tt>Array</tt> is empty, the empty string is returned.
	 * 
	 * @return the string representation of the values in this <tt>Array</tt>, separated by the specified separator.
	 */
	public String join(String separator) {
		StringBuilder builder = new StringBuilder();
		String realSeparator = "";
		for (int i = 0; i < this.length; i++) {
			builder.append(realSeparator);
			realSeparator = separator;
			V item = this.array.get(i);
			if (item != null) {
				builder.append(JSAbstractOperations.ToString(item));
			}
		}
		return builder.toString();
	}

	/**
	 * Removes the last element from this <tt>Array</tt> and returns it.
	 * 
	 * <p>
	 * If this <tt>Array</tt> is empty, <tt>undefined</tt> is returned.
	 * 
	 * @return the last element from this <tt>Array</tt>
	 */
	public V pop() {
		if (this.length == 0) {
			return null;
		}
		Array<V> removed = this.splice((int) (this.length - 1), 1);
		return removed.$get(0);
	}

	/**
	 * The specified elements are appended at the end of this <tt>Array</tt> in the order in which they appear in the
	 * arguments list and the new length of this <tt>Array</tt> is returned.
	 * 
	 * @param values
	 *            the values to be appended
	 * @return the new length of this <tt>Array</tt>
	 */
	public int push(V... values) {
		this.splice(this.$length(), 0, values);
		return (int) this.length;
	}

	/**
	 * The elements of this <tt>Array</tt> are rearranged so as to reverse their order, and this <tt>Array</tt> is
	 * returned.
	 * 
	 * @return this <tt>Array</tt>
	 */
	public Array<V> reverse() {
		// Collections.reverse(array);
		// return this;
		return this;
	}

	/**
	 * Removes the first element from this <tt>Array</tt> and returns it.
	 * 
	 * <p>
	 * If this <tt>Array</tt> is empty, <tt>null</tt> is returned.
	 * 
	 * @return the first element of this <tt>Array</tt>.
	 */
	public V shift() {
		Array<V> removed = this.splice(0, 1);
		if (removed.$length() == 1) {
			return removed.$get(0);
		}
		return null;
	}

	/**
	 * Returns a new <tt>Array</tt> containing all the elements of this <tt>Array</tt> starting from the given start
	 * index until the end of this <tt>Array</tt>.
	 * 
	 * <p>
	 * If <tt>start</tt> is negative, it is treated as <tt>length+start</tt>.
	 * 
	 * <p>
	 * If the selection range falls outside of this <tt>array</tt>, an empty <tt>Array</tt> is returned.
	 * 
	 * @param start
	 *            the index from which to start the selection. Use negative values to specified an index starting from
	 *            the end of this <tt>Array</tt>.
	 * @return a new <tt>Array</tt> containing all the elements of this <tt>Array</tt> starting from the given start
	 *         index until the end of the <tt>Array</tt>.
	 */
	public Array<V> slice(int start) {
		return slice(start, this.$length());
	}

	/**
	 * Returns a new <tt>Array</tt> containing all the elements of this <tt>Array</tt> starting from the given start
	 * index and ending at (but not including) the given end index.
	 * 
	 * <p>
	 * If <tt>start</tt> is negative, it is treated as <tt>length+start</tt>. If <tt>end</tt> is negative, it is treated
	 * as <tt>length+end</tt>.
	 * 
	 * <p>
	 * If the selection range falls outside of this <tt>Array</tt>, an empty <tt>Array</tt> is returned.
	 * 
	 * @param start
	 *            the index from which to start the selection. Use negative values to specified an index starting from
	 *            the end
	 * @param end
	 *            the index at which to end the selection (the element at this index is excluded). Use negative values
	 *            to specified an index starting from the end
	 * @return a new <tt>Array</tt> containing all the elements of this <tt>Array</tt> starting from the given start
	 *         index and ending at the given end index
	 */
	public Array<V> slice(int start, int end) {
		long actualStart;
		if (start < 0) {
			actualStart = (long) Math.max(this.$length() + start, 0);
		} else {
			actualStart = (long) Math.min(start, this.$length());
		}

		long actualEnd;
		if (end < 0) {
			actualEnd = (long) Math.max(this.$length() + end, 0);
		} else {
			actualEnd = (long) Math.min(end, this.$length());
		}

		return this.array.slice(actualStart, actualEnd);
	}

	/**
	 * Deletes the specified number of elements from this <tt>Array</tt> starting at specified start index, and returns
	 * a new <tt>Array</tt> containing the deleted elements (if any).
	 * 
	 * <p>
	 * If <tt>start</tt> is negative, it is treated as <tt>length+start</tt>.
	 * 
	 * @param start
	 *            the index at which to start deleting elements. Use negative values to specified an index starting from
	 *            the end
	 * @param deleteCount
	 *            the number of elements to be deleted
	 * @return a new <tt>Array</tt> containing the deleted elements (if any)
	 */
	public Array<V> splice(int start, int deleteCount) {
		return this.splice(start, deleteCount, (V[]) null);
	}

	/**
	 * Deletes the specified number of elements from this <tt>Array</tt> starting at specified start index, replaces the
	 * deleted elements with the specified values, and returns a new <tt>Array</tt> containing the deleted elements (if
	 * any).
	 * 
	 * <p>
	 * If <tt>start</tt> is negative, it is treated as <tt>length+start</tt>.
	 * 
	 * @param start
	 *            the index at which to start deleting elements. Use negative values to specify an index starting from
	 *            the end.
	 * @param deleteCount
	 *            the number of elements to be deleted
	 * @param values
	 *            the elements with which the deleted elements must be replaced
	 * @return a new <tt>Array</tt> containing the deleted elements (if any)
	 */
	public Array<V> splice(int start, int deleteCount, V... values) {
		long actualStart;
		if (start < 0) {
			actualStart = (long) Math.max(this.length + start, 0);
		} else {
			actualStart = (long) Math.min(this.length, start);
		}

		long actualDeleteCount = (long) Math.min(Math.max(deleteCount, 0), this.length - actualStart);

		if (values == null) {
			@SuppressWarnings("unchecked")
			V[] tmp = (V[]) new Object[0];
			values = tmp;
		}

		if (actualDeleteCount == 0 && values.length == 0) {
			// we are not deleting or adding anything. This is basically a NOP
			return new Array<V>();
		}

		long newLength = this.length - actualDeleteCount + values.length;
		long newSetElements = this.setElements + values.length
				- this.array.getSetElements(actualStart, actualStart + deleteCount);

		this.switchStoreIfNeeded(newLength, newSetElements);

		Array<V> deleted = this.slice((int) actualStart, (int) (actualStart + actualDeleteCount));
		this.array.splice(actualStart, actualDeleteCount, values);

		this.length = newLength;
		this.setElements = newSetElements;
		return deleted;
	}

	/**
	 * Sorts the elements of this <tt>Array</tt> using the natural order of their string representations.
	 * 
	 * <p>
	 * The sort is not necessarily stable (that is, elements that compare equal do not necessarily remain in their
	 * original order).
	 * 
	 * <p>
	 * <tt>sort</tt> returns this <tt>Array</tt>.
	 * 
	 * <p>
	 * <strong>Note1:</strong> Because non-existent property values always compare greater than undefined property
	 * values, and undefined always compares greater than any other value, undefined property values always sort to the
	 * end of the result, followed by non-existent property values.
	 * 
	 * @return this array
	 */
	public Array<V> sort() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Sorts the elements of this <tt>Array</tt> using the specified SortFunction to determine ordering.
	 * 
	 * <p>
	 * The sort is not necessarily stable (that is, elements that compare equal do not necessarily remain in their
	 * original order).
	 * 
	 * <p>
	 * This methods return this <tt>Array</tt>.
	 * 
	 * <p>
	 * If the specified SortFunction is null, then this method is equivalent to <tt>sort()</tt>.
	 * 
	 * <p>
	 * If <tt>comparefn</tt> is not a consistent comparison function for the elements of this <tt>Array</tt>, the
	 * behaviour of sort is implementation-defined.
	 * 
	 * @param comparefn
	 *            a sort function that can compare elements of this <tt>Array</tt>.
	 * @return this <tt>Array</tt>
	 */
	public Array<V> sort(final SortFunction<V> comparefn) {
		// Collections.sort(array, new Comparator<V>() {
		// @Override
		// public int compare(V a, V b) {
		// return comparefn.$invoke(a, b);
		// }
		// });
		// return this;
		return null;
	}

	/**
	 * Prepends the specified values to the start of this <tt>Array</tt>, such that their order within this
	 * <tt>Array</tt> is the same as the order in which they appear in the argument list. Returns the new length of this
	 * <tt>Array</tt>.
	 * 
	 * @param values
	 *            the values to the prepended to the start of this <tt>Array</tt>
	 * @return the new length of this <tt>Array</tt>
	 */
	public int unshift(V... values) {
		this.splice(0, 0, values);
		return this.$length();
	}

	/**
	 * Calls the specified callback function once for each element present in this <tt>Array</tt>, in ascending order.
	 * <tt>callbackfn</tt> is called only for elements of this <tt>Array</tt> which actually exist; it is not called for
	 * missing elements of this <tt>Array</tt>.
	 * 
	 * <p>
	 * <tt>callbackfn</tt> is called with the value of the element as its argument.
	 * 
	 * <p>
	 * <tt>forEach</tt> does not directly mutate this <tt>Array</tt> but this <tt>Array</tt> may be mutated by the calls
	 * to <tt>callbackfn</tt>.
	 * 
	 * <p>
	 * The range of elements processed by <tt>forEach</tt> is set before the first call to <tt>callbackfn</tt>. Elements
	 * which are appended to this <tt>Array</tt> after the call to <tt>forEach</tt> begins will not be visited by
	 * <tt>callbackfn</tt>. If existing elements of this <tt>Array</tt> are changed, their value as passed to callback
	 * will be the value at the time <tt>forEach</tt> visits them; elements that are deleted after the call to
	 * <tt>forEach</tt> begins and before being visited are not visited.
	 * 
	 * @param callbackfn
	 *            the callback function to be called for each element
	 */
	@BrowserCompatibility("IE:9+")
	public void forEach(Callback1<V> callbackfn) {
		// for (V value : array) {
		// callbackfn.$invoke(value);
		// }
	}

	/**
	 * Calls the specified callback function once for each element present in this <tt>Array</tt>, in ascending order.
	 * <tt>callbackfn</tt> is called only for elements of this <tt>Array</tt> which actually exist; it is not called for
	 * missing elements of this <tt>Array</tt>.
	 * 
	 * <p>
	 * <tt>callbackfn</tt> is called with three arguments: the value of the element, the index of the element, and the
	 * <tt>Array</tt> being traversed (this <tt>Array</tt>).
	 * 
	 * <p>
	 * <tt>forEach</tt> does not directly mutate this <tt>Array</tt> but this <tt>Array</tt> may be mutated by the calls
	 * to <tt>callbackfn</tt>.
	 * 
	 * <p>
	 * The range of elements processed by <tt>forEach</tt> is set before the first call to <tt>callbackfn</tt>. Elements
	 * which are appended to this <tt>Array</tt> after the call to <tt>forEach</tt> begins will not be visited by
	 * <tt>callbackfn</tt>. If existing elements of this <tt>Array</tt> are changed, their value as passed to callback
	 * will be the value at the time <tt>forEach</tt> visits them; elements that are deleted after the call to
	 * <tt>forEach</tt> begins and before being visited are not visited.
	 * 
	 * @param callbackfn
	 *            the callback function to be called for each element
	 */
	@BrowserCompatibility("IE:9+")
	public void forEach(Callback3<V, Integer, Array<V>> callbackfn) {
		// TODO Auto-generated method stub

	}

	/**
	 * Converts all the elements of this <tt>Array</tt> to their String representation, concatenates them using a comma
	 * as a separator. Calling this method is equivalent to calling <tt>join()</tt>.
	 * 
	 * @return the string representation of the values in this <tt>Array</tt>, separated by a comma.
	 */
	@Override
	public String toString() {
		return join();
	}

	/**
	 * Converts all the elements of this <tt>Array</tt> to Strings using their toLocaleString method, and concatenate
	 * the resulting Strings by a separator String that has been derived in an implementation-defined locale-specific
	 * way. The result of calling this function is intended to be analogous to the result of <tt>toString()</tt>, except
	 * that the result of this function is intended to be locale-specific.
	 * 
	 * @return a string representation of all the elements in this <tt>Array</tt>, separated by a locale-specific
	 *         separator
	 */
	public String toLocaleString() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Searches the elements of this <tt>Array</tt> in descending order for the specified element, and returns the index
	 * of the last position at which the element was found, or -1 if not found.
	 * 
	 * <p>
	 * Elements are compared for equality using the <tt>===</tt> operator.
	 * 
	 * <p>
	 * If there are multiple occurrences of the specified element in this <tt>Array</tt>, then the largest index among
	 * occurrences is returned.
	 * 
	 * @param searchElement
	 *            the element to search for
	 * @return the last index where this element is in this <tt>Array</tt>, or -1 if not found
	 */
	@BrowserCompatibility("IE:9+")
	public int lastIndexOf(V searchElement) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * Searches the elements of this <tt>Array</tt> in descending order starting at the specified index for the
	 * specified element, and returns the index of the last index at which the element was found, or -1 if not found.
	 * 
	 * <p>
	 * Elements are compared for equality using the === operator.
	 * 
	 * <p>
	 * If there are multiple occurrences of the specified element in this <tt>Array</tt>, then the largest index among
	 * occurrences is returned.
	 * 
	 * <p>
	 * If <tt>fromIndex</tt> is greater than or equal to the length of this <tt>Array</tt>, the whole array will be
	 * searched. If <tt>fromIndex</tt> is negative, then it is treated as <tt>length+fromIndex</tt>. If the computed
	 * index is less than 0, -1 is returned.
	 * 
	 * @param searchElement
	 *            The element to search for
	 * @param fromIndex
	 *            the index from which to start searching
	 * @return the last index where this element is in this <tt>Array</tt>, or -1 if not found
	 */
	@BrowserCompatibility("IE:9+")
	public int lastIndexOf(V searchElement, int fromIndex) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * Returns <tt>true</tt> if the specified callback function returns <tt>true</tt> for every element in this
	 * <tt>Array</tt>.
	 * 
	 * <p>
	 * <tt>callbackfn</tt> is called on each element in ascending order until one of the calls returns <tt>false</tt>.
	 * Once <tt>callbackfn</tt> has returned false for the first time, the search in this <tt>Array</tt> is interrupted,
	 * <tt>callbackfn</tt> is never called on any more elements and <tt>every</tt> returns <tt>false</tt>. If
	 * <tt>callbackfn</tt> returned <tt>true</tt> for all the elements, then <tt>every</tt> returns <tt>true</tt>.
	 * <tt>callbackfn</tt> is called only for elements of this <tt>Array</tt> which actually exist; it is not called for
	 * missing elements of this <tt>Array</tt>.
	 * 
	 * <p>
	 * <tt>callbackfn</tt> has 3 arguments: the value of the element, the index of the element, and the <tt>Array</tt>
	 * being traversed (this <tt>Array</tt>).
	 * 
	 * <p>
	 * <tt>every</tt> does not directly mutate this <tt>Array</tt>, but this <tt>Array</tt> may be mutated by the calls
	 * to <tt>callbackfn</tt>.
	 * 
	 * <p>
	 * The range of elements processed by <tt>every</tt> is set before the first call to <tt>callbackfn</tt>. Elements
	 * which are appended to this <tt>Array</tt> after the call to <tt>every</tt> begins will not be visited by
	 * <tt>callbackfn</tt>. If existing elements of this <tt>Array</tt> are changed, their value as passed to
	 * <tt>callbackfn</tt> will be the value at the time <tt>every</tt> visits them; elements that are deleted after the
	 * call to <tt>every</tt> begins and before being visited are not visited. <tt>every</tt> acts like the "for all"
	 * quantifier in mathematics. In particular, for an empty <tt>Array</tt>, it returns <tt>true</tt>.
	 * 
	 * @param callbackfn
	 *            the callback function to call
	 * @return <tt>true</tt> if </tt>callbackfn</tt> returns <tt>true</tt> for ALL the elements in this <tt>Array</tt>,
	 *         <tt>false</tt> if not
	 */
	@BrowserCompatibility("IE:9+")
	public boolean every(Function3<V, Integer, Array<V>, Boolean> callbackfn) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Returns <tt>true</tt> if the specified callback function returns <tt>true</tt> for at least one element in this
	 * <tt>Array</tt>.
	 * 
	 * <p>
	 * <tt>some</tt> calls <tt>callbackfn</tt> once for each element present in this <tt>Array</tt>, in ascending order,
	 * until it finds one where <tt>callbackfn</tt> returns <tt>true</tt>. If such an element is found, <tt>some</tt>
	 * immediately returns <tt>true</tt>. Otherwise, <tt>some</tt> returns <tt>false</tt>. <tt>callbackfn</tt> is called
	 * only for elements of this <tt>Array</tt> which actually exist; it is not called for missing elements of this
	 * <tt>Array</tt>.
	 * 
	 * <p>
	 * <tt>callbackfn</tt> has three arguments: the value of the element, the index of the element, and the
	 * <tt>Array</tt> being traversed (this <tt>Array</tt>).
	 * 
	 * <tt>some</tt> does not directly mutate this <tt>Array</tt> but this <tt>Array</tt> may be mutated by the calls to
	 * <tt>callbackfn</tt>.
	 * 
	 * <p>
	 * The range of elements processed by <tt>some</tt> is set before the first call to <tt>callbackfn</tt>. Elements
	 * that are appended to this <tt>Array</tt> after the call to <tt>some</tt> begins will not be visited by
	 * <tt>callbackfn</tt>. If existing elements of this <tt>Array</tt> are changed, their value as passed to
	 * <tt>callbackfn</tt> will be the value at the time that <tt>some</tt> visits them; elements that are deleted after
	 * the call to <tt>some</tt> begins and before being visited are not visited. <tt>some</tt> acts like the "exists"
	 * quantifier in mathematics. In particular, for an empty <tt>Array</tt>, it returns <tt>false</tt>.
	 * 
	 * @param callbackfn
	 *            the callback function to call
	 * @return <tt>true</tt> if <tt>callbackfn</tt> returns <tt>true</tt> for at least one element in this
	 *         <tt>Array</tt>, <tt>false</tt> otherwise
	 */
	@BrowserCompatibility("IE:9+")
	public boolean some(Function3<V, Integer, Array<V>, Boolean> callbackfn) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Constructs a new <tt>Array</tt>, such that the value at each index in the new <tt>Array</tt> is the result of
	 * calling the specified callback function on the value at the corresponding index in this <tt>Array</tt>.
	 * 
	 * <p>
	 * <tt>map</tt> calls <tt>callbackfn</tt> once for each element in this <tt>Array</tt>, in ascending order, and
	 * constructs a new <tt>Array</tt> from the results. <tt>callbackfn</tt> is called only for elements of the array
	 * which actually exist; it is not called for missing elements of this <tt>Array</tt>.
	 * 
	 * <p>
	 * <tt>callbackfn</tt> has three arguments: the value of the element, the index of the element, and the
	 * <tt>Array</tt> being traversed (this <tt>Array</tt>).
	 * 
	 * <p>
	 * <tt>map</tt> does not directly mutate this <tt>Array</tt> but this <tt>Array</tt> may be mutated by the calls to
	 * <tt>callbackfn</tt>.
	 * 
	 * <p>
	 * The range of elements processed by <tt>map</tt> is set before the first call to <tt>callbackfn</tt>. Elements
	 * which are appended to this <tt>Array</tt> after the call to <tt>map</tt> begins will not be visited by
	 * <tt>callbackfn</tt>. If existing elements of this <tt>Array</tt> are changed, their value as passed to
	 * <tt>callbackfn</tt> will be the value at the time <tt>map</tt> visits them; elements that are deleted after the
	 * call to <tt>map</tt> begins and before being visited are not visited.
	 * 
	 * @param callbackfn
	 *            the callback used to create the elements of the new array
	 * @return a new <tt>Array</tt> containing new elements as returned by the specified callback function
	 */
	@BrowserCompatibility("IE:9+")
	public <T> Array<T> map(Function3<V, Integer, Array<V>, T> callbackfn) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Constructs a new <tt>Array</tt> containing only the elements of this <tt>Array</tt> for which the specified
	 * callback function returns <tt>true</tt>.
	 * 
	 * <p>
	 * <tt>filter</tt> calls <tt>callbackfn</tt> once for each element in this <tt>Array</tt>, in ascending order, and
	 * constructs a new <tt>Array</tt> of all the values for which <tt>callbackfn</tt> returns <tt>true</tt>.
	 * <tt>callbackfn</tt> is called only for elements of this <tt>Array</tt> which actually exist; it is not called for
	 * missing elements of this <tt>Array</tt>.
	 * 
	 * <p>
	 * <tt>callbackfn</tt> has three arguments: the value of the element, the index of the element, and the
	 * <tt>Array</tt> being traversed (this <tt>Array</tt>).
	 * 
	 * <p>
	 * <tt>filter</tt> does not directly mutate this <tt>Array</tt>, but this <tt>Array</tt> may be mutated by the calls
	 * to <tt>callbackfn</tt>.
	 * 
	 * <p>
	 * The range of elements processed by <tt>filter</tt> is set before the first call to <tt>callbackfn</tt>. Elements
	 * which are appended to this <tt>Array</tt> after the call to <tt>filter</tt> begins will not be visited by
	 * <tt>callbackfn</tt>. If existing elements of this <tt>Array</tt> are changed their value as passed to
	 * <tt>callbackfn</tt> will be the value at the time <tt>filter</tt> visits them; elements that are deleted after
	 * the call to <tt>filter</tt> begins and before being visited are not visited.
	 * 
	 * @param callbackfn
	 *            the callback function used to decide if an element should be appended to the new <tt>Array</tt>
	 * @return a new <tt>Array</tt> containing only the elements of this <tt>Array</tt> for which the specified callback
	 *         function returns <tt>true</tt>.
	 */
	@BrowserCompatibility("IE:9+")
	public Array<V> filter(Function3<V, Integer, Array<V>, Boolean> callbackfn) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Applies the specified function against an accumulator and each value of this <tt>Array</tt> omitting the first
	 * element (from left-to-right), as to reduce it to a single value.
	 * 
	 * <p>
	 * <tt>reduce</tt> calls the callback, as a function, once for each element present in this <tt>Array</tt> except
	 * for the first one, in ascending order.
	 * 
	 * <p>
	 * <tt>callbackfn</tt> has four arguments: the <tt>previousValue</tt> (or value from the previous call to
	 * <tt>callbackfn</tt>), the <tt>currentValue</tt> (value of the current element), the <tt>currentIndex</tt>, and
	 * the <tt>Array</tt> being traversed (this <tt>Array</tt>). The first time that callback is called (that is, on the
	 * second element element of this <tt>array</tt>) <tt>previousValue</tt> will be equal to the first value in this
	 * <tt>Array</tt> and <tt>currentValue</tt> will be equal to the second. It is a TypeError if this <tt>Array</tt>
	 * contains no elements.
	 * 
	 * <p>
	 * <tt>reduce</tt> does not directly mutate this <tt>Array</tt> but this <tt>Array</tt> may be mutated by the calls
	 * to <tt>callbackfn</tt>.
	 * 
	 * <p>
	 * The range of elements processed by <tt>reduce</tt> is set before the first call to <tt>callbackfn</tt>. Elements
	 * that are appended to this <tt>Array</tt> after the call to <tt>reduce</tt> begins will not be visited by
	 * <tt>callbackfn</tt>. If existing elements of this <tt>Array</tt> are changed, their value as passed to
	 * <tt>callbackfn</tt> will be the value at the time <tt>reduce</tt> visits them; elements that are deleted after
	 * the call to <tt>reduce</tt> begins and before being visited are not visited.
	 * 
	 * @param callbackfn
	 *            the function used to reduce the array to a single value
	 * @return a single value derived from calling the callback function on all the elements of this <tt>Array</tt>
	 */
	@BrowserCompatibility("IE:9+, Safari:4+, Opera:10.50+")
	public V reduce(Function4<V, V, Integer, Array<V>, V> callbackfn) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Applies the specified function against an accumulator and each value of this <tt>Array</tt> (from left-to-right)
	 * as to reduce it to a single value.
	 * 
	 * <p>
	 * <tt>reduce</tt> calls the callback, as a function, once for each element present in this <tt>Array</tt>, in
	 * ascending order.
	 * 
	 * <p>
	 * <tt>callbackfn</tt> has four arguments: the <tt>previousValue</tt> (or value from the previous call to
	 * <tt>callbackfn</tt>), the <tt>currentValue</tt> (value of the current element), the <tt>currentIndex</tt>, and
	 * the <tt>Array</tt> being traversed (this <tt>Array</tt>). The first time that callback is called
	 * <tt>previousValue</tt> will be equal to <tt>initialValue</tt> and <tt>currentValue</tt> will be equal to the
	 * first value in this <tt>Array</tt>.
	 * 
	 * <p>
	 * <tt>reduce</tt> does not directly mutate this <tt>Array</tt> but this <tt>Array</tt> may be mutated by the calls
	 * to <tt>callbackfn</tt>.
	 * 
	 * <p>
	 * The range of elements processed by <tt>reduce</tt> is set before the first call to <tt>callbackfn</tt>. Elements
	 * that are appended to this <tt>Array</tt> after the call to <tt>reduce</tt> begins will not be visited by
	 * <tt>callbackfn</tt>. If existing elements of this <tt>Array</tt> are changed, their value as passed to
	 * <tt>callbackfn</tt> will be the value at the time <tt>reduce</tt> visits them; elements that are deleted after
	 * the call to <tt>reduce</tt> begins and before being visited are not visited.
	 * 
	 * @param callbackfn
	 *            the function used to reduce this <tt>Array</tt> to a single value
	 * @return a single value derived from calling the callback function on all the elements of this <tt>Array</tt>
	 */
	@BrowserCompatibility("IE:9+, Safari:4+, Opera:10.50+")
	public <T> T reduce(Function4<T, V, Integer, Array<V>, T> callbackfn, T initialValue) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Applies the specified function against an accumulator and each value of this <tt>Array</tt> omitting the first
	 * element (from right-to-left), as to reduce it to a single value.
	 * 
	 * <p>
	 * <tt>reduceRight</tt> calls the callback, as a function, once for each element present in this <tt>Array</tt>
	 * except for the first one, in descending order.
	 * 
	 * <p>
	 * <tt>callbackfn</tt> has four arguments: the <tt>previousValue</tt> (or value from the previous call to
	 * <tt>callbackfn</tt>), the <tt>currentValue</tt> (value of the current element), the <tt>currentIndex</tt>, and
	 * the <tt>Array</tt> being traversed (this <tt>Array</tt>). The first time that callback is called (that is, on the
	 * second-to-last element element of this <tt>array</tt>) <tt>previousValue</tt> will be equal to the last value in
	 * this <tt>Array</tt> and <tt>currentValue</tt> will be equal to the second-to-last. It is a TypeError if this
	 * <tt>Array</tt> contains no elements.
	 * 
	 * <p>
	 * <tt>reduceRight</tt> does not directly mutate this <tt>Array</tt> but this <tt>Array</tt> may be mutated by the
	 * calls to <tt>callbackfn</tt>.
	 * 
	 * <p>
	 * The range of elements processed by <tt>reduceRight</tt> is set before the first call to <tt>callbackfn</tt>.
	 * Elements that are appended to this <tt>Array</tt> after the call to <tt>reduceRight</tt> begins will not be
	 * visited by <tt>callbackfn</tt>. If existing elements of this <tt>Array</tt> are changed, their value as passed to
	 * <tt>callbackfn</tt> will be the value at the time <tt>reduceRight</tt> visits them; elements that are deleted
	 * after the call to <tt>reduceRight</tt> begins and before being visited are not visited.
	 * 
	 * @param callbackfn
	 *            the function used to reduce the array to a single value
	 * @return a single value derived from calling the callback function on all the elements of this <tt>Array</tt>
	 */
	@BrowserCompatibility("IE:9+, Safari:4+, Opera:10.50+")
	public V reduceRight(Function4<V, V, Integer, Array<V>, V> callbackfn) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Applies the specified function against an accumulator and each value of this <tt>Array</tt> (from right-to-left)
	 * as to reduce it to a single value.
	 * 
	 * <p>
	 * <tt>reduceRight</tt> calls the callback, as a function, once for each element present in this <tt>Array</tt>, in
	 * descending order.
	 * 
	 * <p>
	 * <tt>callbackfn</tt> has four arguments: the <tt>previousValue</tt> (or value from the previous call to
	 * <tt>callbackfn</tt>), the <tt>currentValue</tt> (value of the current element), the <tt>currentIndex</tt>, and
	 * the <tt>Array</tt> being traversed (this <tt>Array</tt>). The first time that callback is called
	 * <tt>previousValue</tt> will be equal to <tt>initialValue</tt> and <tt>currentValue</tt> will be equal to the last
	 * value in this <tt>Array</tt>.
	 * 
	 * <p>
	 * <tt>reduceRight</tt> does not directly mutate this <tt>Array</tt> but this <tt>Array</tt> may be mutated by the
	 * calls to <tt>callbackfn</tt>.
	 * 
	 * <p>
	 * The range of elements processed by <tt>reduceRight</tt> is set before the first call to <tt>callbackfn</tt>.
	 * Elements that are appended to this <tt>Array</tt> after the call to <tt>reduceRight</tt> begins will not be
	 * visited by <tt>callbackfn</tt>. If existing elements of this <tt>Array</tt> are changed, their value as passed to
	 * <tt>callbackfn</tt> will be the value at the time <tt>reduceRight</tt> visits them; elements that are deleted
	 * after the call to <tt>reduceRight</tt> begins and before being visited are not visited.
	 * 
	 * @param callbackfn
	 *            the function used to reduce this <tt>Array</tt> to a single value
	 * @return a single value derived from calling the callback function on all the elements of this <tt>Array</tt>
	 */
	@BrowserCompatibility("IE:9+, Safari:4+, Opera:10.50+")
	public <T> T reduceRight(Function4<T, V, Integer, Array<V>, T> callbackfn, T initialValue) {
		// TODO Auto-generated method stub
		return null;
	}

	private abstract class ArrayStore<E> {

		ArrayStore<E> switchStoreType() {
			ArrayStore<E> that;
			if (this instanceof PackedArrayStore) {
				that = new SparseArrayStore<E>();
			} else {
				that = new PackedArrayStore<E>();
			}

			Iterator<Entry<E>> entries = this.entryIterator();
			while (entries.hasNext()) {
				Entry<E> entry = entries.next();
				that.set(entry.key, entry.value);
			}

			return that;
		}

		abstract void truncateFrom(long newLength);

		abstract void padTo(long newLength);

		abstract void splice(long actualStart, long actualDeleteCount, E[] values);

		abstract long getSetElements(long firstIncluded, long lastExcluded);

		abstract Iterator<Entry<E>> entryIterator();

		abstract boolean isEfficientStoreFor(long newLength, long newElementCount);

		abstract void set(long index, E value);

		abstract boolean isSet(long index);

		abstract E get(long index);

		abstract Array<E> slice(long fromIncluded, long toExcluded);
	}

	private final class PackedArrayStore<E> extends ArrayStore<E> {

		/**
		 * We can't use <E> instead of <Object> here, because we must be able to make a difference between elements set
		 * to null, and unset elements (represented by UNSET).
		 */
		private ArrayList<Object> elements = new ArrayList<Object>();

		@Override
		boolean isEfficientStoreFor(long newLength, long newElementCount) {
			if (newLength > Integer.MAX_VALUE) {
				// PackedArrayStore cannot store values with index > Integer.MAX_VALUE
				return false;
			}
			if (newLength < 120) {
				// for small arrays, PackedArrayStore is always better
				// note that the threshold (120) intentionally doesn't match with the threshold defined in
				// SparseArrayStore.isEfficientStoreFor(), to make sure that we don't convert back and forth
				// between both types of ArrayStore
				return true;
			}
			if (newElementCount == 0 || (newLength / newElementCount) >= 6) {
				// if we have more than 5/6 unset elements, we're better off with SparseArrayStore
				// thresholds between the two ArrayStore types don't match: see length condition
				return false;
			}
			// we're good enough with the current store
			return true;
		}

		@Override
		void set(long index, E value) {
			this.elements.set((int) index, value);
		}

		@Override
		public void padTo(long newLength) {
			while (this.elements.size() < newLength) {
				this.elements.add(UNSET);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		E get(long index) {
			if (index > Integer.MAX_VALUE || index >= elements.size()) {
				return null;
			}

			Object value = this.elements.get((int) index);
			if (value == UNSET) {
				return null;
			}
			return (E) value;
		}

		@SuppressWarnings("unchecked")
		@Override
		Array<E> slice(long fromIncluded, long toExcluded) {
			Array<E> result = new Array<E>();
			for (int i = (int) fromIncluded, n = 0; i < toExcluded && i < this.elements.size(); i++, n++) {
				Object value = this.elements.get(i);
				if (value != UNSET) {
					result.$set(n, (E) this.elements.get(i));
				}
			}
			return result;
		}

		@Override
		boolean isSet(long index) {
			return index < this.elements.size() && this.elements.get((int) index) != UNSET;
		}

		@Override
		Iterator<Entry<E>> entryIterator() {
			return new Iterator<Entry<E>>() {
				private int nextIndex = 0;

				@Override
				public boolean hasNext() {
					skipToNext();
					return nextIndex < elements.size();
				}

				@Override
				public Entry<E> next() {
					skipToNext();
					Entry<E> entry = new Entry<E>();
					entry.key = nextIndex;
					entry.value = get(nextIndex);
					nextIndex++;
					return entry;
				}

				private void skipToNext() {
					while (nextIndex < elements.size() && !isSet(nextIndex)) {
						nextIndex++;
					}
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		@Override
		long getSetElements(long firstIncluded, long lastExcluded) {
			int setElements = 0;
			for (int i = (int) firstIncluded; i < this.elements.size() && i < lastExcluded; i++) {
				if (this.elements.get(i) != UNSET) {
					setElements++;
				}
			}
			return setElements;
		}

		@Override
		void splice(long actualStart, long actualDeleteCount, E[] values) {

			// we must try to avoid splicing in 0(n^2) if possible
			// let's compute the cost of two different methods of splicing in number of moves
			long trailingElements = this.elements.size() - actualStart - actualDeleteCount;
			long inplaceCost = (long) Math.abs(actualDeleteCount - values.length) * trailingElements;
			long rebuildCost = this.elements.size() + values.length - actualDeleteCount;

			// execute the cheapest strategy. We give an unfair advantage to inplace, because rebuild has
			// uses more memory (2n), so we only want to pick it when it's really worth it
			if (inplaceCost > 2 * rebuildCost) {
				// Rebuild the underlying ArrayList from scratch.

				// add the head untouched elements
				ArrayList<Object> newElements = new ArrayList<Object>((int) rebuildCost);
				newElements.addAll(this.elements.subList(0, (int) actualStart));

				// add the inserted elements
				newElements.addAll(Arrays.asList(values));

				// add the trailing untouched elements
				newElements
						.addAll(this.elements.subList((int) (actualStart + actualDeleteCount), this.elements.size()));

				this.elements = newElements;

			} else {
				// add/remove/set the elements in place, trying to add or remove as few elements as possible

				// as long as the deleted and added elements overlap, we can use Set instead of insert + delete
				// it's a lot cheaper to do it that way on an ArrayList
				int overlap = (int) Math.min(values.length, actualDeleteCount);
				for (int i = 0; i < overlap; i++) {
					this.elements.set((int) actualStart + i, values[i]);
				}

				// more deletions than insertions, we have a few more elements to delete
				// remove elements starting from the end, so we have to move fewer elements
				// in the backing array every time one element is removed
				for (int i = (int) (actualStart + actualDeleteCount - 1); i >= actualStart + overlap; i--) {
					this.elements.remove(i);
				}

				// more insertions than deletions, we have a few more element to insert
				// (this loop is mutually exclusive with the loop above)
				for (int i = overlap; i < values.length; i++) {
					this.elements.add((int) actualStart + i, values[i]);
				}
			}

		}

		@Override
		void truncateFrom(long newLength) {
			if (newLength < this.elements.size() / 4) {
				// if newLength is small enough, it's faster to just copy the remaining values instead of deleting the
				// truncated ones
				this.elements = new ArrayList<Object>(this.elements.subList(0, (int) newLength));

			} else {
				for (int i = this.elements.size() - 1; i >= newLength; i--) {
					this.elements.remove(i);
				}
			}
		}
	}

	private final class SparseArrayStore<E> extends ArrayStore<E> {

		TreeMap<Long, E> elements = new TreeMap<Long, E>();

		@Override
		boolean isEfficientStoreFor(long newLength, long newElementCount) {
			if (newLength > Integer.MAX_VALUE) {
				// SparseArrayStore is the only one that can store values with index > Integer.MAX_VALUE
				return true;
			}
			if (newLength < 80) {
				// for small arrays, PackedArrayStore is always better
				// note that the threshold (80) intentionally doesn't match with the threshold defined in
				// PackedArrayStore.isEfficientStoreFor(), to make sure that we don't convert back and forth
				// between both types of ArrayStore
				return false;
			}
			if ((newLength / newElementCount) < 3) {
				// if we have less than 2/3 empty elements, we're better off with PackedArrayStore.
				// thresholds between the two ArrayStore types don't match: see length condition
				return false;
			}
			// we're happy enough with the current store
			return true;
		}

		@Override
		void set(long index, E value) {
			this.elements.put(index, value);
		}

		@Override
		E get(long index) {
			return this.elements.get(index);
		}

		@Override
		Array<E> slice(long fromIncluded, long toExcluded) {
			Array<E> result = new Array<E>();

			Long firstKey = this.elements.ceilingKey(fromIncluded);
			Long lastKey = this.elements.lowerKey(toExcluded);

			if (firstKey != null && lastKey != null) {
				// only add stuff to the array if the selected range actually contains something
				// if we don't have a key that is higher than fromIncluded, it means the slice is empty
				// if we don't have a key that is smaller than toExcluded, it means the slice is empty
				java.util.Map<Long, E> selected = this.elements.subMap(firstKey, lastKey);

				for (java.util.Map.Entry<Long, E> entry : selected.entrySet()) {
					result.$set(entry.getKey(), entry.getValue());
				}
			}

			// we must also set the length, just in case the last element that was requested was unset
			result.$length((int) (toExcluded - fromIncluded));
			return result;
		}

		@Override
		boolean isSet(long index) {
			return elements.containsKey(index);
		}

		@Override
		Iterator<Entry<E>> entryIterator() {
			final Iterator<java.util.Map.Entry<Long, E>> it = elements.entrySet().iterator();
			return new Iterator<Entry<E>>() {
				@Override
				public boolean hasNext() {
					return it.hasNext();
				}

				@Override
				public Entry<E> next() {
					java.util.Map.Entry<Long, E> utilEntry = it.next();
					Entry<E> entry = new Entry<E>();
					entry.key = utilEntry.getKey();
					entry.value = utilEntry.getValue();
					return entry;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		@Override
		long getSetElements(long firstIncluded, long lastExcluded) {
			Long actualFirstIncluded = this.elements.ceilingKey(firstIncluded);
			Long actualLastExcluded = this.elements.lowerKey(lastExcluded);
			if (actualFirstIncluded == null || actualLastExcluded == null) {
				// there are no keys greater than first included OR
				// there are no keys smaller than last included
				// => the corresponding submap would be empty
				return 0;
			}
			return this.elements.subMap(actualFirstIncluded, actualLastExcluded).size();
		}

		@Override
		void splice(long actualStart, long actualDeleteCount, E[] values) {

			// as long as the deleted and added elements overlap, we can use put() instead of insert + delete
			// this gives only one lookup in the tree instead of two
			long overlap = (long) Math.min(values.length, actualDeleteCount);
			for (int i = 0; i < overlap; i++) {
				this.elements.put(actualStart + i, values[i]);
			}

			// more deletions than insertions, we have a few more elements to delete
			for (int i = (int) actualDeleteCount - 1; i >= overlap; i--) {
				this.elements.remove((int) actualStart + overlap + i);
			}

			// more insertions than deletions, we have a few more element to insert
			// (this loop is mutually exclusive with the loop above)
			for (int i = (int) overlap; i < values.length; i++) {
				this.elements.put(actualStart + i, values[i]);
			}
		}

		@Override
		void truncateFrom(long newLength) {
			Long ceil = this.elements.ceilingKey(newLength);
			if (ceil != null) {
				// there are keys larger than newLength
				SortedMap<Long, E> toRemove = this.elements.subMap(ceil, true, this.elements.lastKey(), true);
				toRemove.clear();
			}
		}

		@Override
		void padTo(long newLength) {
			// no padding needed
		}
	}

	private static class Entry<E> {
		long key;
		E value;
	}
}
