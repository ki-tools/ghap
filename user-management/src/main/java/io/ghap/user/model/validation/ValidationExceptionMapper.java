package io.ghap.user.model.validation;


import io.ghap.user.model.AbstractModel;
import io.ghap.user.model.validation.validators.EnglishNumberToWords;
import io.ghap.user.model.validation.validators.PasswordLength;
import io.ghap.user.model.validation.validators.PasswordPattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.metadata.ConstraintDescriptor;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.util.ResourceBundle;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.status;

@Provider
@Singleton
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Inject
    @Named("defaultMessages")
    private ResourceBundle messages;


    @Override
    public Response toResponse(ConstraintViolationException e) {
        //groovyL e.constraintViolations*.message
        AbstractModel model = null;
        for(ConstraintViolation v:e.getConstraintViolations()){
            if(v.getLeafBean() instanceof AbstractModel) {
                model = (AbstractModel)v.getLeafBean();

                final String code = getCode(v);
                final String message = getMessage(v);

                model.addError(v.getPropertyPath().toString(), code, message);
            }
        }

        if(model != null){
            return status(BAD_REQUEST).entity(model).build();
        }
        else {
            return status(BAD_REQUEST).entity(e).build();
        }
    }

    private String getMessage(ConstraintViolation v) {
        ConstraintDescriptor descriptor = v.getConstraintDescriptor();
        if(descriptor.getAnnotation() instanceof PasswordPattern){
            return String.valueOf( descriptor.getAttributes().get("info") );
        }
        else if(descriptor.getAnnotation() instanceof PasswordPattern.Several){
            PasswordPattern.Several a = (PasswordPattern.Several) descriptor.getAnnotation();
            return String.format(a.message(), EnglishNumberToWords.convert(a.value().length));
        }
        else
            return v.getMessage();
    }

    private String getCode(ConstraintViolation v) {
        ConstraintDescriptor descriptor = v.getConstraintDescriptor();
        Annotation annotation = descriptor.getAnnotation();
        if(annotation instanceof PasswordLength){
            return annotation.annotationType().getSimpleName();
        }
        else if(annotation instanceof PasswordPattern.Several){
            return "PasswordPattern";
        }
        else {
            final String messageTemplate = v.getMessageTemplate();
            // Get rid of braces: {javax.validation.constraints.NotNull.message} --> javax.validation.constraints.NotNull.message
            if(messageTemplate.startsWith("{") && messageTemplate.endsWith("}")) {
                final String messageTemplateWithoutBraces = messageTemplate.substring(1, messageTemplate.length() - 1);
                // javax.validation.constraints.NotNull.message --> NotNull
                if( messages.containsKey(messageTemplateWithoutBraces) ) {
                    return messages.getString(messageTemplateWithoutBraces);
                }
                else {
                    return messageTemplateWithoutBraces;
                }
            }
            else {
                return messageTemplate;
            }
        }
    }
}
