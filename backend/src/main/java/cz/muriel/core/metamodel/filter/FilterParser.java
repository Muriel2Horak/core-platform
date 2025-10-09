package cz.muriel.core.metamodel.filter;

import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🔍 Filter Parser - Converts filter strings to jOOQ Conditions
 * 
 * Supported operators:
 * - eq: equals (age eq 18)
 * - ne: not equals (status ne 'inactive')
 * - lt: less than (age lt 30)
 * - lte: less than or equal (age lte 30)
 * - gt: greater than (age gt 18)
 * - gte: greater than or equal (age gte 18)
 * - like: pattern match (name like '%John%')
 * - in: in list (status in ('active', 'pending'))
 * - notIn: not in list (status notIn ('deleted'))
 * 
 * Boolean operators: and, or
 * Parentheses: (age gt 18) and (status eq 'active')
 * 
 * Examples:
 * - status eq 'active'
 * - (age gt 18) and (status eq 'active')
 * - name like '%Smith%' or email like '%@example.com'
 * - status in ('active', 'pending', 'approved')
 * - (age gte 18 and age lte 65) and status ne 'inactive'
 */
@Slf4j
public class FilterParser {

    // Regex patterns
    private static final Pattern COMPARISON_PATTERN = Pattern.compile(
        "(\\w+)\\s+(eq|ne|lt|lte|gt|gte|like)\\s+('[^']*'|\\d+|true|false)"
    );
    
    private static final Pattern IN_PATTERN = Pattern.compile(
        "(\\w+)\\s+(in|notIn)\\s+\\(([^)]+)\\)"
    );

    /**
     * Parse filter string to jOOQ Condition
     */
    public static Condition parse(String filterString) {
        if (filterString == null || filterString.isBlank()) {
            return DSL.trueCondition();
        }

        try {
            return parseExpression(filterString.trim());
        } catch (Exception e) {
            log.error("Failed to parse filter: {}", filterString, e);
            throw new IllegalArgumentException("Invalid filter syntax: " + e.getMessage(), e);
        }
    }

    /**
     * Parse expression with AND/OR operators
     */
    private static Condition parseExpression(String expr) {
        // Handle parentheses first
        expr = expr.trim();
        
        // Split by OR (lowest precedence)
        List<String> orParts = splitByOperator(expr, "or");
        if (orParts.size() > 1) {
            Condition result = parseAndExpression(orParts.get(0));
            for (int i = 1; i < orParts.size(); i++) {
                result = result.or(parseAndExpression(orParts.get(i)));
            }
            return result;
        }
        
        return parseAndExpression(expr);
    }

    /**
     * Parse AND expressions
     */
    private static Condition parseAndExpression(String expr) {
        List<String> andParts = splitByOperator(expr, "and");
        if (andParts.size() > 1) {
            Condition result = parseCondition(andParts.get(0));
            for (int i = 1; i < andParts.size(); i++) {
                result = result.and(parseCondition(andParts.get(i)));
            }
            return result;
        }
        
        return parseCondition(expr);
    }

    /**
     * Parse single condition (with parentheses handling)
     */
    private static Condition parseCondition(String expr) {
        expr = expr.trim();
        
        // Remove outer parentheses
        if (expr.startsWith("(") && expr.endsWith(")")) {
            return parseExpression(expr.substring(1, expr.length() - 1));
        }
        
        // Try IN/NOT IN pattern
        Matcher inMatcher = IN_PATTERN.matcher(expr);
        if (inMatcher.matches()) {
            String field = inMatcher.group(1);
            String operator = inMatcher.group(2);
            String values = inMatcher.group(3);
            
            List<Object> valueList = parseValueList(values);
            Field<Object> fieldRef = DSL.field(DSL.name(field));
            
            if ("in".equals(operator)) {
                return fieldRef.in(valueList);
            } else {
                return fieldRef.notIn(valueList);
            }
        }
        
        // Try comparison pattern
        Matcher compMatcher = COMPARISON_PATTERN.matcher(expr);
        if (compMatcher.matches()) {
            String field = compMatcher.group(1);
            String operator = compMatcher.group(2);
            String value = compMatcher.group(3);
            
            return createComparison(field, operator, parseValue(value));
        }
        
        throw new IllegalArgumentException("Invalid condition syntax: " + expr);
    }

    /**
     * Create comparison condition
     */
    private static Condition createComparison(String fieldName, String operator, Object value) {
        Field<Object> field = DSL.field(DSL.name(fieldName));
        
        return switch (operator) {
            case "eq" -> field.eq(value);
            case "ne" -> field.ne(value);
            case "lt" -> field.lt(value);
            case "lte" -> field.le(value);
            case "gt" -> field.gt(value);
            case "gte" -> field.ge(value);
            case "like" -> field.like(value.toString());
            default -> throw new IllegalArgumentException("Unknown operator: " + operator);
        };
    }

    /**
     * Split expression by operator (respecting parentheses)
     */
    private static List<String> splitByOperator(String expr, String operator) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            
            if (depth == 0) {
                String remaining = expr.substring(i);
                if (remaining.toLowerCase().startsWith(" " + operator + " ")) {
                    parts.add(expr.substring(start, i).trim());
                    start = i + operator.length() + 2; // skip " operator "
                    i = start - 1;
                }
            }
        }
        
        if (start == 0) {
            // No operator found
            parts.add(expr);
        } else {
            // Add last part
            parts.add(expr.substring(start).trim());
        }
        
        return parts;
    }

    /**
     * Parse value (string, number, boolean)
     */
    private static Object parseValue(String value) {
        value = value.trim();
        
        // String literal
        if (value.startsWith("'") && value.endsWith("'")) {
            return value.substring(1, value.length() - 1);
        }
        
        // Boolean
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        
        // Number
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            return value;
        }
    }

    /**
     * Parse comma-separated value list
     */
    private static List<Object> parseValueList(String values) {
        String[] parts = values.split(",");
        List<Object> result = new ArrayList<>();
        
        for (String part : parts) {
            result.add(parseValue(part.trim()));
        }
        
        return result;
    }
}
