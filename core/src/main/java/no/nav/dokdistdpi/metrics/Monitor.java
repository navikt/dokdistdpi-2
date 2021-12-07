package no.nav.dokdistdpi.metrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Monitor {
    String value() default "";

    String[] extraTags() default {};

    double[] percentiles() default {};

    String description() default "";

    boolean histogram() default false;

    boolean logExceptions() default true;

    boolean createErrorMetric() default false;
}