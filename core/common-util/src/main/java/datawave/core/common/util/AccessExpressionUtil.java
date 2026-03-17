package datawave.core.common.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import datawave.marking.AccessExpressionMarkings;
import datawave.marking.Markings;
import org.apache.accumulo.access.Access;
import org.apache.accumulo.access.AccessExpression;
import org.apache.accumulo.core.security.ColumnVisibility;

/**
 * Utility for bridging between Accumulo's {@link ColumnVisibility} (from accumulo-core) and {@link AccessExpression} (from accumulo-access).
 * <p>
 * Many modules still require {@link ColumnVisibility} for the native Accumulo Key/Mutation API, while the marking functions layer has migrated to
 * {@link AccessExpression}. This utility provides zero-copy-where-possible conversions between the two.
 */
public final class AccessExpressionUtil {

    private static final Access ACCESS = Access.builder().build();

    private static final AccessExpression EMPTY_EXPRESSION = ACCESS.newExpression("");

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
}
