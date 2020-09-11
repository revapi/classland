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
package org.revapi.classland.impl.model;

import javax.lang.model.element.Name;

public final class NameImpl implements Name {
    public static final NameImpl EMPTY = new NameImpl("");

    private final String value;

    private NameImpl(String value) {
        this.value = value;
    }

    public static NameImpl of(String value) {
        if (value == null || value.isEmpty()) {
            return EMPTY;
        }

        return new NameImpl(value);
    }

    public String asString() {
        return value;
    }

    @Override
    public boolean contentEquals(CharSequence cs) {
        return value.contentEquals(cs);
    }

    @Override
    public int length() {
        return value.length();
    }

    @Override
    public char charAt(int index) {
        return value.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return value.subSequence(start, end);
    }

    @Override
    public String toString() {
        return value;
    }
}
