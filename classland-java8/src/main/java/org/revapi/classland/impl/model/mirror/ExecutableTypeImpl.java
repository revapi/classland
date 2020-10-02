package org.revapi.classland.impl.model.mirror;

import org.revapi.classland.impl.model.element.ElementImpl;
import org.revapi.classland.impl.model.element.ExecutableElementImpl;

import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.revapi.classland.impl.util.Memoized.memoize;

public class ExecutableTypeImpl extends TypeMirrorImpl implements ExecutableType {
    private final List<TypeVariableImpl> typeVars;
    private final TypeMirrorImpl returnType;
    private final List<TypeMirrorImpl> parameterTypes;
    private final TypeMirrorImpl receiverType;
    private final List<TypeMirrorImpl> throwsTypes;

    public ExecutableTypeImpl(ExecutableElementImpl source) {
        super(source.getUniverse(), memoize(source::getAnnotationMirrors));
        typeVars = source.getTypeParameters().stream().map(TypeVariableImpl::new).collect(toList());
        returnType = source.getReturnType();
        parameterTypes = source.getParameters().stream().map(ElementImpl::asType).collect(toList());
        receiverType = source.getReceiverType();
        throwsTypes = source.getThrownTypes();
    }

    @Override
    public List<TypeVariableImpl> getTypeVariables() {
        return typeVars;
    }

    @Override
    public TypeMirrorImpl getReturnType() {
        return returnType;
    }

    @Override
    public List<TypeMirrorImpl> getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public TypeMirrorImpl getReceiverType() {
        return receiverType;
    }

    @Override
    public List<TypeMirrorImpl> getThrownTypes() {
        return throwsTypes;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.EXECUTABLE;
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> v, P p) {
        return v.visitExecutable(this, p);
    }
}
