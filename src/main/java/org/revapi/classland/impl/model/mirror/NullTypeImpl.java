package org.revapi.classland.impl.model.mirror;

import org.revapi.classland.impl.model.Universe;

import javax.lang.model.type.NullType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;
import java.util.Collections;
import java.util.List;

public class NullTypeImpl extends TypeMirrorImpl implements NullType {
    public NullTypeImpl(Universe universe) {
        super(universe);
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.NULL;
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> v, P p) {
        return v.visitNull(this, p);
    }

    @Override
    public List<AnnotationMirrorImpl> getAnnotationMirrors() {
        return Collections.emptyList();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof NullTypeImpl)) return false;

        return universe.equals(((NullTypeImpl) obj).universe);
    }

    @Override
    public String toString() {
        return "Null";
    }
}
