package io.ghap.user.model.validation.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PasswordPatternListValidator implements ConstraintValidator<PasswordPattern.Several, String> {

    private final List<Pattern> patterns = new ArrayList<>(4);
    private PasswordPattern.Several constraintAnnotation;

    @Override
    public void initialize(PasswordPattern.Several constraintAnnotation) {
        this.constraintAnnotation = constraintAnnotation;
        PasswordPattern[] patternList = constraintAnnotation.value();
        if(patternList != null) for(PasswordPattern pattern:patternList){
            try {
                patterns.add(Pattern.compile(pattern.regexp(), 0));
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("Invalid regular expression.", e);
            }
        }
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if(value == null){
            return true;
        }
        int count = constraintAnnotation.count();
        for(Pattern pattern:patterns){
            if( pattern.matcher(value).matches() ){
                count--;
            }
        }
        return count <= 0;
    }
}
