package cz.muriel.core.reporting.dsl;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Annotation for validating QueryRequest.
 */
@Target({ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = QueryRequestValidator.class)
@Documented
public @interface ValidQuery {
    String message() default "Invalid query request";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
