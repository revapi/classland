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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

import static org.objectweb.asm.Opcodes.ACC_OPEN;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_TRANSITIVE;
import static org.revapi.classland.impl.util.MemoizedValue.memoize;
import static org.revapi.classland.impl.util.MemoizedValue.obtained;

import java.util.List;
import java.util.stream.Stream;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.ModuleExportNode;
import org.objectweb.asm.tree.ModuleOpenNode;
import org.objectweb.asm.tree.ModuleProvideNode;
import org.objectweb.asm.tree.ModuleRequireNode;
import org.revapi.classland.archive.Archive;
import org.revapi.classland.impl.TypeLookup;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Nullable;

public class ModuleElementImpl extends BaseModuleElementImpl implements ModuleElement {
    private final MemoizedValue<List<? extends Directive>> directives;

    public ModuleElementImpl(TypeLookup lookup, Archive archive, @Nullable ClassNode moduleType) {
        super(lookup, archive, moduleType, TypeKind.MODULE);
        this.directives = memoize(() -> {
            Stream<ExportsDirective> exports = module.exports == null ? Stream.empty()
                    : module.exports.stream().map(ExportsDirectiveImpl::new);

            Stream<OpensDirective> opens = module.opens == null ? Stream.empty()
                    : module.opens.stream().map(OpensDirectiveImpl::new);

            Stream<ProvidesDirective> provides = module.provides == null ? Stream.empty()
                    : module.provides.stream().map(ProvidesDirectiveImpl::new);

            Stream<RequiresDirective> requires = module.requires == null ? Stream.empty()
                    : module.requires.stream().map(RequiresDirectiveImpl::new);

            Stream<UsesDirective> uses = module.uses == null ? Stream.empty()
                    : module.uses.stream().map(UsesDirectiveImpl::new);

            return concat(exports, concat(opens, concat(provides, concat(requires, uses)))).collect(toList());
        });
    }

    public ModuleElementImpl(TypeLookup lookup, Archive archive, String moduleName) {
        this(lookup, archive, moduleName, false);
    }

    protected ModuleElementImpl(TypeLookup lookup, @Nullable Archive archive, String moduleName, boolean unused) {
        super(lookup, archive, moduleName, TypeKind.MODULE);
        this.directives = obtained(emptyList());
    }

    @Override
    public Stream<ReachableModule> getReachableModules() {
        return directives.get().stream().filter(d -> d.getKind() == DirectiveKind.REQUIRES)
                .map(d -> (RequiresDirectiveImpl) d);
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.MODULE;
    }

    @Override
    public boolean isOpen() {
        return (module.access & ACC_OPEN) == ACC_OPEN;
    }

    @Override
    public boolean isUnnamed() {
        return module.name.isEmpty();
    }

    @Override
    public List<? extends Directive> getDirectives() {
        return directives.get();
    }

    private class ExportsDirectiveImpl implements ExportsDirective {
        private final MemoizedValue<PackageElement> pkg;
        private final MemoizedValue<List<? extends ModuleElement>> targets;

        public ExportsDirectiveImpl(ModuleExportNode n) {
            pkg = computePackages().map(m -> m.get(n.packaze));
            targets = memoize(() -> n.modules.stream().map(lookup::getModule).collect(toList()));
        }

        @Override
        public DirectiveKind getKind() {
            return DirectiveKind.EXPORTS;
        }

        @Override
        public <R, P> R accept(DirectiveVisitor<R, P> v, P p) {
            return v.visitExports(this, p);
        }

        @Override
        public PackageElement getPackage() {
            return pkg.get();
        }

        @Override
        public List<? extends ModuleElement> getTargetModules() {
            return targets.get();
        }
    }

    private class OpensDirectiveImpl implements OpensDirective {
        private final MemoizedValue<PackageElementImpl> pkg;
        private final MemoizedValue<List<? extends ModuleElement>> targets;

        private OpensDirectiveImpl(ModuleOpenNode n) {
            pkg = computePackages().map(m -> m.get(n.packaze));
            targets = memoize(() -> n.modules.stream().map(lookup::getModule).collect(toList()));
        }

        @Override
        public PackageElement getPackage() {
            return pkg.get();
        }

        @Override
        public List<? extends ModuleElement> getTargetModules() {
            return targets.get();
        }

        @Override
        public DirectiveKind getKind() {
            return DirectiveKind.OPENS;
        }

        @Override
        public <R, P> R accept(DirectiveVisitor<R, P> v, P p) {
            return v.visitOpens(this, p);
        }
    }

    private class ProvidesDirectiveImpl implements ProvidesDirective {
        private final MemoizedValue<TypeElement> service;
        private final MemoizedValue<List<? extends TypeElement>> impls;

        public ProvidesDirectiveImpl(ModuleProvideNode n) {
            service = memoize(() -> lookup.getTypeByInternalNameFromModule(n.service, ModuleElementImpl.this));
            impls = memoize(() -> n.providers.stream()
                    .map(m -> lookup.getTypeByInternalNameFromModule(m, ModuleElementImpl.this)).collect(toList()));
        }

        @Override
        public TypeElement getService() {
            return service.get();
        }

        @Override
        public List<? extends TypeElement> getImplementations() {
            return impls.get();
        }

        @Override
        public DirectiveKind getKind() {
            return DirectiveKind.PROVIDES;
        }

        @Override
        public <R, P> R accept(DirectiveVisitor<R, P> v, P p) {
            return v.visitProvides(this, p);
        }
    }

    private class RequiresDirectiveImpl implements RequiresDirective, ReachableModule {
        private final ModuleElementImpl dep;
        private final ModuleRequireNode n;

        public RequiresDirectiveImpl(ModuleRequireNode n) {
            this.n = n;
            dep = lookup.getModule(n.module);
        }

        @Override
        public boolean isStatic() {
            return (n.access & ACC_STATIC) == ACC_STATIC;
        }

        @Override
        public boolean isTransitive() {
            return (n.access & ACC_TRANSITIVE) == ACC_TRANSITIVE;
        }

        @Override
        public String getModuleName() {
            return n.module;
        }

        @Override
        public ModuleElementImpl getDependency() {
            return dep;
        }

        @Override
        public DirectiveKind getKind() {
            return DirectiveKind.REQUIRES;
        }

        @Override
        public <R, P> R accept(DirectiveVisitor<R, P> v, P p) {
            return v.visitRequires(this, p);
        }
    }

    private class UsesDirectiveImpl implements UsesDirective {
        private final MemoizedValue<TypeElement> service;

        public UsesDirectiveImpl(String n) {
            service = memoize(() -> lookup.getTypeByInternalNameFromModule(n, ModuleElementImpl.this));
        }

        @Override
        public TypeElement getService() {
            return service.get();
        }

        @Override
        public DirectiveKind getKind() {
            return DirectiveKind.USES;
        }

        @Override
        public <R, P> R accept(DirectiveVisitor<R, P> v, P p) {
            return v.visitUses(this, p);
        }
    }
}
