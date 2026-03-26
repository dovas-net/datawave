package datawave.marking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.apache.accumulo.access.Access;
import org.apache.accumulo.access.examples.ParseExamples;
import org.junit.jupiter.api.Test;

public class MarkingFunctionsDefaultCombineTest {
    public static final Access ACCESS = ParseExamples.ACCESS;

    @Test
    public void testCombineAnds() throws MarkingFunctions.Exception {
        MarkingFunctions<AccessExpressionMarkings> markingFunctions = new MarkingFunctions.Default();

        AccessExpressionMarkings oneAnna = AccessExpressionMarkings.builder().accessExpression(ACCESS.newExpression("A&B")).build();
        AccessExpressionMarkings twoAnna = AccessExpressionMarkings.builder().accessExpression(ACCESS.newExpression("A&C")).build();

        AccessExpressionMarkings combined = markingFunctions.combine(List.of(oneAnna, twoAnna));
        assertEquals(AccessExpressionMarkings.builder().accessExpression(ACCESS.newExpression("A&B&C")).build(), combined);
    }

    @Test
    public void testCombineOrs() throws MarkingFunctions.Exception {
        MarkingFunctions<AccessExpressionMarkings> markingFunctions = new MarkingFunctions.Default();

        AccessExpressionMarkings oneOr = AccessExpressionMarkings.builder().accessExpression(ACCESS.newExpression("A|B")).build();
        AccessExpressionMarkings twoOr = AccessExpressionMarkings.builder().accessExpression(ACCESS.newExpression("A|C")).build();

        AccessExpressionMarkings combined = markingFunctions.combine(List.of(oneOr, twoOr));
        assertEquals(AccessExpressionMarkings.builder().accessExpression(ACCESS.newExpression("(A|B)&(A|C)")).build(), combined);
    }

    @Test
    public void testCombineMapsOfAnds() throws MarkingFunctions.Exception {
        MarkingFunctions<AccessExpressionMarkings> markingFunctions = new MarkingFunctions.Default();

        AccessExpressionMarkings cv1 = AccessExpressionMarkings.builder().accessExpression(ACCESS.newExpression("A&B")).build();
        AccessExpressionMarkings cv2 = AccessExpressionMarkings.builder().accessExpression(ACCESS.newExpression("A&C")).build();
        AccessExpressionMarkings expected = AccessExpressionMarkings.builder().accessExpression(ACCESS.newExpression("A&B&C")).build();

        assertEquals(expected, markingFunctions.combine(List.of(cv1, cv2)));
    }

    @Test
    public void testCombineMapsOfOrs() throws MarkingFunctions.Exception {
        MarkingFunctions<AccessExpressionMarkings> markingFunctions = new MarkingFunctions.Default();

        AccessExpressionMarkings cv1 = AccessExpressionMarkings.builder().accessExpression(ACCESS.newExpression("A|B")).build();
        AccessExpressionMarkings cv2 = AccessExpressionMarkings.builder().accessExpression(ACCESS.newExpression("A|C")).build();
        AccessExpressionMarkings expected = AccessExpressionMarkings.builder().accessExpression(ACCESS.newExpression("(A|B)&(A|C)")).build();

        assertEquals(expected, markingFunctions.combine(List.of(cv1, cv2)));
    }
}
