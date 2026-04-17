package datawave.marking;

import static java.nio.charset.StandardCharsets.UTF_8;

import static datawave.marking.AccessExpressionMarkings.ACCESS;
import static org.apache.accumulo.access.ParsedAccessExpression.ExpressionType.AND;
import static org.apache.accumulo.access.ParsedAccessExpression.ExpressionType.AUTHORIZATION;

import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.accumulo.access.Access;
import org.apache.accumulo.access.AccessExpression;
import org.apache.accumulo.access.ParsedAccessExpression;
import org.apache.accumulo.core.security.ColumnVisibility;

/**
 * Utility for bridging between Accumulo's {@link ColumnVisibility} (from accumulo-core) and {@link AccessExpression} (from accumulo-access).
 * <p>
 * Many modules still require {@link ColumnVisibility} for the native Accumulo Key/Mutation API, while the marking functions layer has migrated to
 * {@link AccessExpression}. This utility provides zero-copy-where-possible conversions between the two.
 */
public final class AccessExpressionUtil {

    private static final AccessExpression EMPTY_EXPRESSION = ACCESS.newExpression("");
    private static final ColumnVisibility EMPTY_VISIBILITY = new ColumnVisibility();

    private AccessExpressionUtil() {}

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
            return EMPTY_VISIBILITY;
        }
        return new ColumnVisibility(expr);
    }

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
     * Convert a visibility String to an {@link AccessExpression}.
     *
     * @param visibility
     *            the visibility String
     * @return the equivalent access expression
     */
    public static AccessExpression toAccessExpression(String visibility) {
        if (visibility == null || visibility.isEmpty()) {
            return EMPTY_EXPRESSION;
        }
        return ACCESS.newExpression(visibility);
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
        return ACCESS.newExpression(normalize(parsed).expression);
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
    public static AccessExpressionMarkings combine(MarkingFunctions<?> markingFunctions, Collection<? extends Markings<?>> markings)
                    throws MarkingFunctions.Exception {
        return ((MarkingFunctions.Default) markingFunctions).combine((Collection<AccessExpressionMarkings>) markings);
    }

    /**
     * A helper routine to merge markings when merging fields of a NormalizedContentInterface
     *
     * @param markings1
     *            markings
     * @param markings2
     *            different markings
     * @return the merged markings
     */
    public static AccessExpressionMarkings mergeMarkings(MarkingFunctions.Default markingFunctions, AccessExpressionMarkings markings1,
                    AccessExpressionMarkings markings2) {
        if (markings2 != null) {
            if (markings1 == null) {
                markings1 = markings2;
            } else {
                try {
                    return combine(markingFunctions, List.of(markings1, markings2));
                } catch (MarkingFunctions.Exception e) {
                    throw new RuntimeException("Unable to combine markings.", e);
                }
            }
        }
        return markings1;
    }

    /**
     * A helper routine to merge markings when merging fields of a NormalizedContentInterface
     *
     * @param markings1
     *            markings
     * @param markings2
     *            different markings
     * @return the merged markings
     */
    public static AccessExpressionMarkings mergeMarkings(MarkingFunctions<?> markingFunctions, Markings<?> markings1, Markings<?> markings2) {
        if (markings2 != null) {
            if (markings1 == null) {
                markings1 = markings2;
            } else {
                if (markings1 instanceof AccessExpressionMarkings && markings2 instanceof AccessExpressionMarkings) {
                    AccessExpressionMarkings aem1 = (AccessExpressionMarkings) markings1;
                    AccessExpressionMarkings aem2 = (AccessExpressionMarkings) markings2;
                    return mergeMarkings((MarkingFunctions.Default) markingFunctions, aem1, aem2);
                } else {
                    throw new RuntimeException(
                                    String.format("Unknown markings class %s or %s", markings1.getClass().getName(), markings1.getClass().getName()));
                }
            }
        }
        return (AccessExpressionMarkings) markings1;
    }

    /**
     * As part of normalizing access expression this class is used to sort and dedupe sub-expressions in a tree set.
     */
    public static class NormalizedExpression implements Comparable<NormalizedExpression> {
        public final String expression;
        public final ParsedAccessExpression.ExpressionType type;

        NormalizedExpression(String expression, ParsedAccessExpression.ExpressionType type) {
            this.expression = expression;
            this.type = type;
        }

        // determines the sort order of different kinds of subexpressions.
        private static int typeOrder(ParsedAccessExpression.ExpressionType type) {
            switch (type) {
                case AUTHORIZATION:
                    return 1;
                case OR:
                    return 2;
                case AND:
                    return 3;
                case EMPTY:
                default:
                    throw new IllegalArgumentException("Unexpected type " + type);
            }
        }

        @Override
        public int compareTo(NormalizedExpression o) {
            // Changing this comparator would significantly change how expressions are normalized.
            int cmp = typeOrder(type) - typeOrder(o.type);
            if (cmp == 0) {
                if (type == AUTHORIZATION) {
                    // sort based on the unquoted and unescaped form of the authorization
                    cmp = ACCESS.unquote(expression).compareTo(ACCESS.unquote(o.expression));
                } else {
                    cmp = expression.compareTo(o.expression);
                }

            }
            return cmp;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || (o instanceof NormalizedExpression && compareTo((NormalizedExpression) o) == 0);
        }

        @Override
        public int hashCode() {
            return expression.hashCode();
        }
    }

    /**
     * This method helps with the flattening aspect of normalization by recursing down as far as possible the parse tree in the case when the expression type is
     * the same. As long as the type is the same in the sub expression, keep using the same set.
     */
    public static void flatten(ParsedAccessExpression.ExpressionType parentType, ParsedAccessExpression parsed,
                    SortedSet<NormalizedExpression> normalizedExpressions) {
        if (parsed.getType() == parentType) {
            for (var child : parsed.getChildren()) {
                flatten(parentType, child, normalizedExpressions);
            }
        } else {
            // The type changed, so start again on the subexpression.
            normalizedExpressions.add(normalize(parsed));
        }
    }

    /**
     * <p>
     * For a given access expression this example will deduplicate, sort, flatten, and remove unneeded parentheses or quotes in the expressions. The following
     * list gives examples of what each normalization step does.
     *
     * <ul>
     * <li>As an example of flattening, the expression {@code A&(B&C)} flattens to {@code
     * A&B&C}.</li>
     * <li>As an example of sorting, the expression {@code (Z&Y)|(C&B)} sorts to {@code
     * (B&C)|(Y&Z)}</li>
     * <li>As an example of deduplication, the expression {@code X&Y&X} normalizes to {@code X&Y}</li>
     * <li>As an example of unneeded quotes, the expression {@code "ABC"&"XYZ"} normalizes to {@code ABC&XYZ}</li>
     * <li>As an example of unneeded parentheses, the expression {@code (((ABC)|(XYZ)))} normalizes to {@code ABC|XYZ}</li>
     * </ul>
     *
     * <p>
     * This algorithm attempts to have the same behavior as the one in the Accumulo 2.1 ColumnVisibility class. However the implementation is very different.
     */
    public static NormalizedExpression normalize(ParsedAccessExpression parsed) {
        if (parsed.getType() == AUTHORIZATION) {
            // If the authorization is quoted and it does not need to be quoted then the following two
            // lines will remove the unnecessary quoting.
            String unquoted = ACCESS.unquote(parsed.getExpression());
            String quoted = ACCESS.quote(unquoted);
            return new NormalizedExpression(quoted, parsed.getType());
        } else {
            // The tree set does the work of sorting and deduplicating sub expressions.
            TreeSet<NormalizedExpression> normalizedChildren = new TreeSet<>();
            for (var child : parsed.getChildren()) {
                flatten(parsed.getType(), child, normalizedChildren);
            }

            if (normalizedChildren.size() == 1) {
                return normalizedChildren.first();
            } else {
                String operator = parsed.getType() == AND ? "&" : "|";
                String sep = "";

                StringBuilder builder = new StringBuilder();

                for (var child : normalizedChildren) {
                    builder.append(sep);
                    if (child.type == AUTHORIZATION) {
                        builder.append(child.expression);
                    } else {
                        builder.append("(");
                        builder.append(child.expression);
                        builder.append(")");
                    }
                    sep = operator;
                }

                return new NormalizedExpression(builder.toString(), parsed.getType());
            }
        }
    }
}
