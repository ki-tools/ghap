package io.ghap.user.model.validation.validators;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The annotated {@code CharSequence} must match the specified regular expression.
 * The regular expression follows the Java regular expression conventions
 * see {@link java.util.regex.Pattern}.
 * <p/>
 * Accepts {@code CharSequence}. {@code null} elements are considered valid.
 *
 * @author Emmanuel Bernard
 */
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = PasswordPatternValidator.class)
public @interface PasswordPattern {

    /**
     * @return the regular expression to match
     */
    String regexp();

    /**
     * @return the error message template
     */
    String message() default "{javax.validation.constraints.PasswordPattern.message}";

    String info() default "";;

    /**
     * @return the groups the constraint belongs to
     */
    Class<?>[] groups() default { };

    /**
     * @return the payload associated to the constraint
     */
    Class<? extends Payload>[] payload() default { };

    /**
     * Defines several {@link PasswordPattern} annotations on the same element.
     *
     * @see PasswordPattern
     */
    @Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
    @Retention(RUNTIME)
    @Documented
    @Constraint(validatedBy = PasswordPatternListValidator.class)
    @interface Several {

        PasswordPattern[] value() default { };

        int count() default 3;

        Class<?>[] groups() default { };

        /**
         * @return the payload associated to the constraint
         */
        Class<? extends Payload>[] payload() default { };

        String message() default "Password should contain at least one character from three of the %s categories";
    }

    /**
     * Defines several {@link PasswordPattern} annotations on the same element.
     *
     * @see PasswordPattern
     */
    @Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
    @Retention(RUNTIME)
    @Documented
    @interface List {

        PasswordPattern[] value();
    }
}

