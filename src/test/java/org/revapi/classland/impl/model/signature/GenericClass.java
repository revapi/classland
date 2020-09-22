/*
 * Copyright 2020 Lukas Krejci
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.revapi.classland.impl.model.signature;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class GenericClass<A, B extends String, C extends B, D extends Serializable & Cloneable & Set<? super int[]>, E extends GenericClass<A, B, C, D, E>>
        extends GenericClassParent<E, Comparable<C>[][]> implements Comparable<A>, Set<String[]>, Serializable {

    @Deprecated
    private A f1;
    private B[][] f2;
    private List<C>[] f3;
    private List<? super D[][]> f4;
    private List<? extends E> f5;
    private List<?> f7;
    int[][] f8;
    protected float f9;
    public double f10;
    private Outer<B>.Inner<String[], C> f11;

    public static <T> T[] method(Set<? extends String> p1, Set<? super String> p2) {
        return null;
    }

    @Override
    public int compareTo(A o) {
        return 0;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public Iterator<String[]> iterator() {
        return null;
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return null;
    }

    @Override
    public boolean add(String[] strings) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends String[]> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {

    }
}

class GenericClassParent<A, B> {

}

class Outer<T extends String> {
    class Inner<A, B extends T> {
    }
}
