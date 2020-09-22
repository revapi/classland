package org.revapi.classland.impl.model.element;

import org.revapi.classland.impl.model.NameImpl;
import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.impl.model.mirror.AnnotationMirrorImpl;
import org.revapi.classland.impl.model.mirror.DeclaredTypeImpl;
import org.revapi.classland.impl.model.mirror.ErrorTypeImpl;
import org.revapi.classland.impl.model.mirror.NoTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.util.Memoized;
import org.revapi.classland.impl.util.Nullable;
import org.revapi.classland.impl.util.Packages;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.type.TypeKind;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;

public class MissingTypeImpl extends TypeElementBase {
    private final ErrorTypeImpl type;
    private final NameImpl qualifiedName;
    private final NameImpl simpleName;

    public MissingTypeImpl(Universe universe, String internalName) {
        super(universe, internalName, Memoized.memoize(() -> {
            String packageName = Packages.getPackageNameFromInternalName(internalName);
            return universe.getPackage(packageName);
        }));
        type = new ErrorTypeImpl(universe, this);

        qualifiedName = NameImpl.of(internalName.replace('/', '.'));
        int lastSlash = internalName.lastIndexOf('/');
        simpleName = lastSlash == -1 ? qualifiedName : NameImpl.of(internalName.substring(lastSlash + 1));
    }

    @Override
    public DeclaredTypeImpl asType() {
        return type;
    }

    @Override
    public @Nullable ExecutableElementImpl getMethod(String methodName, String methodDescriptor) {
        return null;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.CLASS;
    }

    @Override
    public Set<Modifier> getModifiers() {
        return Collections.emptySet();
    }

    @Override
    public NameImpl getSimpleName() {
        return simpleName;
    }

    @Override
    public TypeMirrorImpl getSuperclass() {
        return new NoTypeImpl(universe, Collections::emptyList, TypeKind.NONE);
    }

    @Override
    public List<TypeMirrorImpl> getInterfaces() {
        return emptyList();
    }

    @Override
    public List<TypeParameterElementImpl> getTypeParameters() {
        return emptyList();
    }

    @Override
    public ElementImpl getEnclosingElement() {
        return pkg.get();
    }

    @Override
    public List<ElementImpl> getEnclosedElements() {
        return emptyList();
    }

    @Override
    public NestingKind getNestingKind() {
        return NestingKind.TOP_LEVEL;
    }

    @Override
    public NameImpl getQualifiedName() {
        return qualifiedName;
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return v.visitType(this, p);
    }

    @Override
    public List<AnnotationMirrorImpl> getAnnotationMirrors() {
        return emptyList();
    }
}
