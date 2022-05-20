/*
 * Copyright 2020-2022 Lukas Krejci
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

import java.util.List;

import javax.lang.model.element.TypeElement;

import org.revapi.classland.archive.Archive;
import org.revapi.classland.impl.TypeLookup;
import org.revapi.classland.impl.model.NameImpl;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.mirror.DeclaredTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.model.signature.TypeVariableResolutionContext;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Nullable;

public abstract class TypeElementBase extends ElementImpl implements TypeElement, TypeVariableResolutionContext {
    // we have to accommodate for the unnamed module and classpath where a single module/package can span across
    // multiple archives. We assume here that there can be no class duplicates and therefore that each type definitely
    // comes from a single archive. A missing type of course comes from no archive and therefore we need to be nullable.
    protected final @Nullable Archive archive;
    protected final String internalName;
    protected final MemoizedValue<PackageElementImpl> pkg;
    protected final MemoizedValue<ModuleElementImpl> module;

    protected TypeElementBase(TypeLookup lookup, @Nullable Archive archive, String internalName,
            MemoizedValue<@Nullable PackageElementImpl> pkg, MemoizedValue<AnnotationSource> annos) {
        super(lookup, annos, AnnotationTargetPath.ROOT, pkg.map(p -> p == null ? null : p.getModule()));
        this.archive = archive;
        this.internalName = internalName;
        this.pkg = pkg;
        this.module = pkg.map(p -> p == null ? null : p.getModule());
    }

    public @Nullable Archive getArchive() {
        return archive;
    }

    public String getInternalName() {
        return internalName;
    }

    public MemoizedValue<PackageElementImpl> getPackage() {
        return pkg;
    }

    public MemoizedValue<ModuleElementImpl> lookupModule() {
        return module;
    }

    @Override
    public abstract NameImpl getQualifiedName();

    @Override
    public abstract DeclaredTypeImpl asType();

    @Override
    public abstract TypeMirrorImpl getSuperclass();

    @Override
    public abstract List<TypeMirrorImpl> getInterfaces();

    @Override
    public abstract List<TypeParameterElementImpl> getTypeParameters();

    public abstract @Nullable ExecutableElementImpl getMethod(String methodName, String methodDescriptor);

    public abstract List<ExecutableElementImpl> getMethod(String methodName);

    public abstract VariableElementImpl.@Nullable Field getField(String name);
}
