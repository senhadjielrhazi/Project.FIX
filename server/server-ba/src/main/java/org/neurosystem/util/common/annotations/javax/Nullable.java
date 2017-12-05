package org.neurosystem.util.common.annotations.javax;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.neurosystem.util.common.annotations.javax.meta.TypeQualifierNickname;
import org.neurosystem.util.common.annotations.javax.meta.When;

@Documented
@TypeQualifierNickname
@Nonnull(when = When.UNKNOWN)
@Retention(RetentionPolicy.RUNTIME)
public @interface Nullable {

}
