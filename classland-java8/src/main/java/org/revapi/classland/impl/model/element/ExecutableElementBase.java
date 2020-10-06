package org.revapi.classland.impl.model.element;

import org.revapi.classland.impl.Universe;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.mirror.AnnotationValueImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.model.signature.TypeVariableResolutionContext;
import org.revapi.classland.impl.util.Memoized;
import org.revapi.classland.impl.util.Nullable;

import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public abstract class ExecutableElementBase extends ElementImpl implements ExecutableElement, TypeVariableResolutionContext {
    protected ExecutableElementBase(Universe universe, Memoized<AnnotationSource> annotationSource, AnnotationTargetPath path, Memoized<@Nullable ModuleElementImpl> typeLookupSeed) {
        super(universe, annotationSource, path, typeLookupSeed);
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return v.visitExecutable(this, p);
    }

    @Override
    public abstract List<TypeParameterElementImpl> getTypeParameters();

    @Override
    public abstract TypeMirrorImpl getReturnType();

    @Override
    public abstract List<VariableElementImpl> getParameters();

    @Override
    public abstract TypeMirrorImpl getReceiverType();

    @Override
    public abstract List<TypeMirrorImpl> getThrownTypes();

    @Override
    public abstract AnnotationValueImpl getDefaultValue();
}
