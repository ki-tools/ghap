package io.ghap.jersey;

import com.google.inject.matcher.AbstractMatcher;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import javax.validation.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

/**
 */
public class ValidationInterceptor implements MethodInterceptor {
    private final Validator validator;

    public static IsValidable isValidable() {
        return new IsValidable();
    }

    public ValidationInterceptor() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }


    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Object argument = null;
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        if (parameterAnnotations != null) {
            for (int i = 0; i < parameterAnnotations.length; i++) {
                Annotation[] annotationsArray  = parameterAnnotations[i];
                for(Annotation annotation : annotationsArray){
                    if (annotation instanceof Valid) {
                        argument = invocation.getArguments()[i];
                    }
                }
            }
        }


        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n");
        if (argument != null ) {
            Set violations = validator.validate(argument);
            if (!violations.isEmpty()) {
                for(Object violation : violations) {
                    stringBuilder.append(((ConstraintViolation)violation).getMessage()).append("\n");
                }
                throw new ConstraintViolationException(
                        String.format("Error when validating method %s due to %s", method.getName(),
                                stringBuilder.toString())
                        ,violations);

            }
        }
        return invocation.proceed();

    }
}

class IsValidable extends AbstractMatcher<Method> {

    @Override
    public boolean matches(Method method) {
        Class[] interfaces = method.getDeclaringClass().getInterfaces();

        if (interfaces.length > 0) {
            Class resource = interfaces[0];

            try {
                Method resourceMethod = resource.getMethod(method.getName(),
                        method.getParameterTypes());

                return Modifier.isPublic(resourceMethod.getModifiers());

            } catch (NoSuchMethodException nsme) {
                return false;
            }
        } else {
            return false;
        }
    }
}
