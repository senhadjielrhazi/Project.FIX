package org.neurosystem.util.common.annotations.javax;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.neurosystem.util.common.annotations.javax.meta.TypeQualifier;
import org.neurosystem.util.common.annotations.javax.meta.TypeQualifierValidator;
import org.neurosystem.util.common.annotations.javax.meta.When;

@Documented
@TypeQualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface Nonnull {
    When when() default When.ALWAYS;

    static class Checker implements TypeQualifierValidator<Nonnull> {

        public When forConstantValue(Nonnull qualifierqualifierArgument,
                Object value) {
            if (value == null)
                return When.NEVER;
            return When.ALWAYS;
        }
    }
}
