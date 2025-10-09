package cz.muriel.core.reporting.dsl;

import cz.muriel.core.reporting.app.ReportingProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Validator for QueryRequest.
 * 
 * Validates:
 * - Maximum rows limit
 * - Maximum time interval
 * - Required time range
 */
@Component
@RequiredArgsConstructor
public class QueryRequestValidator implements ConstraintValidator<ValidQuery, QueryRequest> {

    private final ReportingProperties properties;

    @Override
    public boolean isValid(QueryRequest query, ConstraintValidatorContext context) {
        if (query == null) {
            return true; // Let @NotNull handle this
        }

        context.disableDefaultConstraintViolation();
        boolean isValid = true;

        // Validate limit
        if (query.getLimit() != null && query.getLimit() > properties.getMaxRows()) {
            context.buildConstraintViolationWithTemplate(
                    "Limit exceeds maximum allowed rows: " + properties.getMaxRows())
                .addPropertyNode("limit")
                .addConstraintViolation();
            isValid = false;
        }

        // Validate time range
        if (query.getTimeRange() != null) {
            Instant start = query.getTimeRange().getStart();
            Instant end = query.getTimeRange().getEnd();

            if (start != null && end != null) {
                if (end.isBefore(start)) {
                    context.buildConstraintViolationWithTemplate(
                            "Time range end must be after start")
                        .addPropertyNode("timeRange.end")
                        .addConstraintViolation();
                    isValid = false;
                }

                long days = Duration.between(start, end).toDays();
                if (days > properties.getMaxIntervalDays()) {
                    context.buildConstraintViolationWithTemplate(
                            "Time interval exceeds maximum allowed days: " + properties.getMaxIntervalDays())
                        .addPropertyNode("timeRange")
                        .addConstraintViolation();
                    isValid = false;
                }
            }
        }

        // Require time range for most entities (security measure)
        if (query.getTimeRange() == null && !isEntityExemptFromTimeRange(query.getEntity())) {
            context.buildConstraintViolationWithTemplate(
                    "Time range is required for this entity")
                .addPropertyNode("timeRange")
                .addConstraintViolation();
            isValid = false;
        }

        return isValid;
    }

    private boolean isEntityExemptFromTimeRange(String entity) {
        // Small lookup tables can be exempt from time range requirement
        return entity != null && (
            entity.equals("entity_types") ||
            entity.equals("field_types") ||
            entity.equals("enum_values")
        );
    }
}
