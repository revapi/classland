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
package org.revapi.classland.impl.model.element;

import static java.util.Collections.emptySet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.revapi.classland.impl.model.NameImpl;
import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.impl.model.mirror.AnnotationMirrorImpl;
import org.revapi.classland.impl.model.mirror.DeclaredTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.util.Memoized;
import org.revapi.classland.impl.util.Modifiers;
import org.revapi.classland.impl.util.Nullable;

public class TypeElementImpl extends ElementImpl implements TypeElement {
    private final String internalName;
    private final PackageElementImpl pkg;
    private final Memoized<NameImpl> qualifiedName;
    private final Memoized<NameImpl> simpleName;
    private final Memoized<Element> enclosingElement;
    private final Memoized<NestingKind> nestingKind;
    private final Memoized<TypeMirrorImpl> superClass;
    private final Memoized<ElementKind> elementKind;
    private final Memoized<Set<Modifier>> modifiers;

    public TypeElementImpl(Universe universe, String internalName, Memoized<ClassNode> node, PackageElementImpl pkg) {
        super(universe);
        this.internalName = internalName;
        this.pkg = pkg;

        Memoized<ScanningResult> scan = node.map(cls -> {
            ScanningResult ret = new ScanningResult();
            ret.classNode = cls;
            ret.nestingKind = NestingKind.TOP_LEVEL;
            ret.effectiveAccess = cls.access;

            if (cls.innerClasses.isEmpty()) {
                ret.simpleName = cls.name.replace('/', '.');
            } else {
                int classNameLength = cls.name.length();
                ret.qualifiedNameParts = new HashMap<>(cls.innerClasses.size(), 1);
                for (InnerClassNode icn : cls.innerClasses) {
                    // if any inner class up the chain is local, this class is local, too.
                    // otherwise the nesting kind is based on the inner and outer name of the inner class node.
                    // The list of the inner classes recorded on a type seems to contain all the containing classes +
                    // all the directly contained classes + the type itself. Therefore we can rely simply on the length
                    // of the name to distinguish between them.
                    if (icn.name.length() <= classNameLength) {
                        ret.qualifiedNameParts.put(icn.name, icn);

                        if (icn.name.length() == classNameLength) {
                            // we probably could detect this using outerClass and outerMethod but that would
                            // make it difficult to detect anonymous classes anyway. So let's just use this
                            // logic instead that is able to detect all the cases.

                            if (icn.innerName == null) {
                                ret.nestingKind = NestingKind.ANONYMOUS;
                            } else if (icn.outerName == null) {
                                ret.nestingKind = NestingKind.LOCAL;
                            } else {
                                ret.nestingKind = NestingKind.MEMBER;
                            }
                            ret.simpleName = icn.innerName == null ? "" : icn.innerName;
                            ret.effectiveAccess = icn.access;
                        }
                    }
                }

                if (ret.simpleName == null) {
                    // we're parsing a top-level class with inner classes
                    ret.simpleName = cls.name.replace('/', '.');
                }
            }

            return ret;
        });

        nestingKind = scan.map(r -> r.nestingKind);

        simpleName = scan.map(r -> {
            if (r.nestingKind == NestingKind.TOP_LEVEL) {
                return NameImpl.of(r.simpleName.substring(r.simpleName.lastIndexOf('.') + 1));
            }
            return NameImpl.of(r.simpleName);
        });

        qualifiedName = scan.map(r -> {
            if (r.nestingKind == NestingKind.TOP_LEVEL) {
                return NameImpl.of(r.simpleName);
            }

            if (r.nestingKind == NestingKind.LOCAL || r.nestingKind == NestingKind.ANONYMOUS) {
                return NameImpl.EMPTY;
            }

            String name = r.classNode.name;
            InnerClassNode icn = r.qualifiedNameParts.get(name);
            List<String> parts = new ArrayList<>(r.qualifiedNameParts.size());
            while (icn != null) {
                parts.add(0, icn.innerName);
                InnerClassNode next = r.qualifiedNameParts.get(icn.outerName);
                if (next == null) {
                    String outerClass = icn.outerName;
                    if (outerClass == null) {
                        // we're embedded in an anonymous class, let's bail...
                        return NameImpl.EMPTY;
                    } else {
                        parts.add(0, outerClass.replace('/', '.'));
                    }
                    break;
                } else {
                    icn = next;
                }
            }

            return NameImpl.of(String.join(".", parts));
        });

        elementKind = node.map(n -> Modifiers.toTypeElementKind(n.access));
        modifiers = scan.map(r -> Modifiers.toTypeModifiers(r.effectiveAccess));

        enclosingElement = scan.map(r -> {
            switch (r.nestingKind) {
            case TOP_LEVEL:
                return pkg;
            case MEMBER:
            case ANONYMOUS:
                return universe.getTypeByInternalName(r.classNode.outerClass).orElse(null);
            case LOCAL:
                return universe.getTypeByInternalName(r.classNode.outerClass)
                        .map(c -> c.getMethod(r.classNode.outerMethod, r.classNode.outerMethodDesc)).orElse(null);
            default:
                throw new IllegalStateException("Unhandled nesting kind, " + r.nestingKind
                        + ", while determining the enclosing element of class " + internalName);
            }
        });

        // TODO not right -
        superClass = node.map(n -> universe.getDeclaredTypeByInternalName(n.superName));
    }

    public boolean isInPackage(PackageElementImpl pkg) {
        return getNestingKind() == NestingKind.TOP_LEVEL && this.pkg.equals(pkg);
    }

    public @Nullable ExecutableElementImpl getMethod(String methodName, String methodDescriptor) {
        // TODO implement
        return null;
    }

    @Override
    public List<AnnotationMirrorImpl> getAnnotationMirrors() {
        // TODO implement
        return Collections.emptyList();
    }

    @Override
    public NestingKind getNestingKind() {
        return nestingKind.get();
    }

    @Override
    public Name getQualifiedName() {
        return qualifiedName.get();
    }

    @Override
    public TypeMirrorImpl getSuperclass() {
        return superClass.get();
    }

    @Override
    public List<? extends TypeMirror> getInterfaces() {
        return null;
    }

    @Override
    public List<? extends TypeParameterElement> getTypeParameters() {
        return null;
    }

    @Override
    public DeclaredTypeImpl asType() {
        return null;
    }

    @Override
    public ElementKind getKind() {
        return elementKind.get();
    }

    @Override
    public Set<Modifier> getModifiers() {
        return modifiers.get();
    }

    @Override
    public Name getSimpleName() {
        return simpleName.get();
    }

    @Override
    public Element getEnclosingElement() {
        return enclosingElement.get();
    }

    @Override
    public List<? extends Element> getEnclosedElements() {
        // TODO implement
        return Collections.emptyList();
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return v.visitType(this, p);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TypeElementImpl that = (TypeElementImpl) o;
        return internalName.equals(that.internalName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(internalName);
    }

    @Override
    public String toString() {
        return "TypeElementImpl{" + internalName + "}";
    }

    private static final class ScanningResult {
        ClassNode classNode;
        int effectiveAccess;
        NestingKind nestingKind;
        String simpleName;
        Map<String, InnerClassNode> qualifiedNameParts;
    }
}
