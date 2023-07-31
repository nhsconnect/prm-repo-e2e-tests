package uk.nhs.prm.e2etests.annotation;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(value = { ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
public @interface Debt {
    String comment() default "This has been marked as technical debt to be addressed.";
    Priority priority() default Priority.LOW;
    String ticket() default "Not assigned.";;

    enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}
