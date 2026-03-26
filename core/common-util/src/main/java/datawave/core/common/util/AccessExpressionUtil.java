package datawave.core.common.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Collection;

import org.apache.accumulo.access.Access;
import org.apache.accumulo.access.AccessExpression;
import org.apache.accumulo.access.ParsedAccessExpression;
import org.apache.accumulo.access.examples.ParseExamples;
import org.apache.accumulo.access.impl.AccessExpressionImpl;
import org.apache.accumulo.core.security.ColumnVisibility;

import datawave.marking.AccessExpressionMarkings;
import datawave.marking.MarkingFunctions;
import datawave.marking.Markings;

/**
 * Utility for bridging between Accumulo's {@link ColumnVisibility} (from accumulo-core) and {@link AccessExpression} (from accumulo-access).
 * <p>
 * Many modules still require {@link ColumnVisibility} for the native Accumulo Key/Mutation API, while the marking functions layer has migrated to
 * {@link AccessExpression}. This utility provides zero-copy-where-possible conversions between the two.
 */
public final class AccessExpressionUtil {

    private static final Access ACCESS = ParseExamples.ACCESS;

    private static final AccessExpression EMPTY_EXPRESSION = AccessExpressionImpl.EMPTY;

    private AccessExpressionUtil() {}

    /**
     * Convert a {@link ColumnVisibility} to an {@link AccessExpression}.
     *
     * @param cv
     *            the column visibility
     * @return the equivalent access expression
     */
    public static AccessExpression toAccessExpression(ColumnVisibility cv) {
        byte[] expr = cv.getExpression();
        if (expr.length == 0) {
            return EMPTY_EXPRESSION;
        }
        return ACCESS.newExpression(new String(expr, UTF_8));
    }

    /**
     * Convert a {@link Markings} to an {@link AccessExpression}.
     *
     * @param markings
     *            assumed to be of type AccessExpressionMarkings
     * @return the embedded access expression
     */
    public static AccessExpression toAccessExpression(Markings<?> markings) {
        return (AccessExpression) markings.getMarkings();
    }

    /**
     * Convert a {@link AccessExpression} to an {@link AccessExpressionMarkings}.
     *
     * @param expression
     *            The AccessExpression to embed in the wrapper class
     * @return the wrapped access expression
     */
    public static AccessExpressionMarkings toAccessExpressionMarkings(AccessExpression expression) {
        return AccessExpressionMarkings.builder().accessExpression(expression).build();
    }

    /**
     * Convert an {@link Markings} to a {@link ColumnVisibility}.
     *
     * @param markings
     *            the access expression
     * @return the equivalent column visibility
     */
    public static ColumnVisibility toColumnVisibility(Markings<?> markings) {
        if (markings instanceof AccessExpressionMarkings) {
            return toColumnVisibility((AccessExpression) markings.getMarkings());
        }
        String m = markings.toString();
        if (m.isEmpty()) {
            return new ColumnVisibility();
        }
        return new ColumnVisibility(m);
    }

    /**
     * Convert an {@link AccessExpression} to a {@link ColumnVisibility}.
     *
     * @param ae
     *            the access expression
     * @return the equivalent column visibility
     */
    public static ColumnVisibility toColumnVisibility(AccessExpression ae) {
        String expr = ae.getExpression();
        if (expr.isEmpty()) {
            return new ColumnVisibility();
        }
        return new ColumnVisibility(expr);
    }

    /**
     * Convert a raw visibility byte array (e.g. from {@code Key.getColumnVisibilityData()}) to an {@link AccessExpression}.
     *
     * @param visibilityBytes
     *            the raw visibility bytes
     * @return the equivalent access expression
     */
    public static AccessExpression toAccessExpression(byte[] visibilityBytes) {
        if (visibilityBytes == null || visibilityBytes.length == 0) {
            return EMPTY_EXPRESSION;
        }
        return ACCESS.newExpression(new String(visibilityBytes, UTF_8));
    }

    /**
     * Normalize an {@link AccessExpression} by deduplicating and sorting terms. This is the replacement for the old {@code ColumnVisibility.flatten()}
     * behavior.
     *
     * @param expression
     *            the expression to normalize
     * @return a normalized {@link AccessExpression}
     */
    public static AccessExpression normalize(AccessExpression expression) {
        String expr = expression.getExpression();
        if (expr.isEmpty()) {
            return expression;
        }
        ParsedAccessExpression parsed = ACCESS.newParsedExpression(expr);
        return ACCESS.newExpression(ParseExamples.normalize(parsed).expression);
    }

    /**
     * @return the shared {@link Access} instance used by this utility
     */
    public static Access getAccess() {
        return ACCESS;
    }

    /**
     * @return a cached empty {@link AccessExpression}
     */
    public static AccessExpression emptyExpression() {
        return EMPTY_EXPRESSION;
    }

    /**
     * Helper to call {@link MarkingFunctions#combine(Collection)} with a wildcard-typed {@link MarkingFunctions}. This avoids the wildcard-capture issue that
     * arises when callers hold a {@code MarkingFunctions<?>} reference.
     *
     * @param markingFunctions
     *            the marking functions instance
     * @param markings
     *            the markings to combine
     * @return the combined markings
     * @throws MarkingFunctions.Exception
     *             if the markings cannot be combined
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Markings<?> combine(MarkingFunctions<?> markingFunctions, Collection<? extends Markings<?>> markings) throws MarkingFunctions.Exception {
        return ((MarkingFunctions) markingFunctions).combine(markings);
    }
}
