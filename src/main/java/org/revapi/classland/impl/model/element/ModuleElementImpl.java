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
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

import static org.objectweb.asm.Opcodes.ACC_OPEN;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_TRANSITIVE;
import static org.revapi.classland.impl.util.Memoized.memoize;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.ModuleExportNode;
import org.objectweb.asm.tree.ModuleNode;
import org.objectweb.asm.tree.ModuleOpenNode;
import org.objectweb.asm.tree.ModuleProvideNode;
import org.objectweb.asm.tree.ModuleRequireNode;
import org.revapi.classland.impl.model.NameImpl;
import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.impl.model.mirror.AnnotationMirrorImpl;
import org.revapi.classland.impl.model.mirror.NoTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.util.Memoized;

public final class ModuleElementImpl extends ElementImpl implements ModuleElement {
    private final NameImpl name;
    private final ModuleNode module;
    private final Memoized<List<AnnotationMirrorImpl>> annos;
    private final Memoized<NoTypeImpl> type;
    private final Memoized<List<PackageElementImpl>> packages;
    private final Memoized<List<? extends Directive>> directives;

    public ModuleElementImpl(Universe universe, ClassNode moduleType) {
        super(universe);
        this.module = moduleType.module;
        this.name = NameImpl.of(module.name);
        this.type = memoize(
                () -> new NoTypeImpl(universe, memoize(() -> parseAnnotations(moduleType)), TypeKind.MODULE));
        this.packages = memoize(() -> universe.computePackagesForModule(this).collect(toList()));
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

        this.annos = memoize(() -> parseAnnotations(moduleType));
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

    @Override
    public TypeMirrorImpl asType() {
        return type.get();
    }

    @Override
    public NameImpl getQualifiedName() {
        return name;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.MODULE;
    }

    @Override
    public Set<Modifier> getModifiers() {
        return emptySet();
    }

    @Override
    public Name getSimpleName() {
        return getQualifiedName();
    }

    @Override
    public Element getEnclosingElement() {
        return null;
    }

    @Override
    public List<PackageElementImpl> getEnclosedElements() {
        return packages.get();
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return v.visitModule(this, p);
    }

    @Override
    public List<AnnotationMirrorImpl> getAnnotationMirrors() {
        return annos.get();
    }

    private class ExportsDirectiveImpl implements ExportsDirective {
        private final Memoized<PackageElement> pkg;
        private final Memoized<List<? extends ModuleElement>> targets;

        public ExportsDirectiveImpl(ModuleExportNode n) {
            pkg = memoize(() -> universe.getPackage(n.packaze));
            targets = memoize(() -> universe.getModules().stream()
                    .filter(m -> n.modules.contains(m.getQualifiedName().asString())).collect(toList()));
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
        private final Memoized<PackageElementImpl> pkg;
        private final Memoized<List<? extends ModuleElement>> targets;

        private OpensDirectiveImpl(ModuleOpenNode n) {
            pkg = memoize(() -> universe.getPackage(n.packaze));
            targets = memoize(() -> universe.getModules().stream()
                    .filter(m -> n.modules.contains(m.getQualifiedName().asString())).collect(toList()));
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
        private final Memoized<TypeElement> service;
        private final Memoized<List<? extends TypeElement>> impls;

        public ProvidesDirectiveImpl(ModuleProvideNode n) {
            service = memoize(() -> universe.getTypeByInternalName(n.service));
            impls = memoize(() -> n.providers.stream().map(universe::getTypeByInternalName)
                    .collect(toList()));
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

    private class RequiresDirectiveImpl implements RequiresDirective {
        private final Memoized<ModuleElementImpl> dep;
        private final ModuleRequireNode n;

        public RequiresDirectiveImpl(ModuleRequireNode n) {
            this.n = n;
            dep = memoize(() -> universe.getModules().stream().filter(m -> m.module.name.equals(n.module)).findFirst()
                    .orElseThrow());
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
        public ModuleElement getDependency() {
            return dep.get();
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
        private final Memoized<TypeElement> service;

        public UsesDirectiveImpl(String n) {
            service = memoize(() -> universe.getTypeByInternalName(n));
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
