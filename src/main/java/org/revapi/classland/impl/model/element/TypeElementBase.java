package org.revapi.classland.impl.model.element;

import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.impl.model.mirror.DeclaredTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.util.Memoized;
import org.revapi.classland.impl.util.Nullable;

import javax.lang.model.element.TypeElement;
import java.util.List;

public abstract class TypeElementBase extends ElementImpl implements TypeElement {
    protected final String internalName;
    protected final Memoized<PackageElementImpl> pkg;

    protected TypeElementBase(Universe universe, String internalName, Memoized<PackageElementImpl> pkg) {
        super(universe);
        this.internalName = internalName;
        this.pkg = pkg;
    }

    @Override
    public abstract DeclaredTypeImpl asType();

    @Override
    public abstract TypeMirrorImpl getSuperclass();

    @Override
    public abstract List<TypeMirrorImpl> getInterfaces();

    public abstract @Nullable ExecutableElementImpl getMethod(String methodName, String methodDescriptor);
}
