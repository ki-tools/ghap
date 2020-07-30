package io.ghap.user.model.validation.validators;


import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * validator using a regular expression,
 * based on the jsr303 Pattern constraint annotation.
 *
 * @deprecated Use {@link io.ghap.user.model.validation.validators.PasswordPatternListValidator}
 */
@Deprecated
public class PasswordPatternValidator implements ConstraintValidator<PasswordPattern, String> {
    protected Pattern pattern;

    public void initialize(PasswordPattern annotation) {
        try {
            pattern = Pattern.compile(annotation.regexp(), 0);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid regular expression.", e);
        }
    }


    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || pattern.matcher(value).matches();
    }
}

