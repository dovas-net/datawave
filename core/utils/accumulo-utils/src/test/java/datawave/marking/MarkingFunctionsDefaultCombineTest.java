package datawave.marking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.apache.accumulo.access.Access;
import org.apache.accumulo.access.AccessExpression;
import org.junit.jupiter.api.Test;

public class MarkingFunctionsDefaultCombineTest {
    public static final Access ACCESS = Access.builder().build();

    @Test
    public void testCombineAnds() throws MarkingFunctions.Exception {
        MarkingFunctions<AccessExpressionMarkings> markingFunctions = new MarkingFunctions.Default();

        AccessExpression oneAnna = ACCESS.newExpression("A&B");
        AccessExpression twoAnna = ACCESS.newExpression("A&C");

        AccessExpression combined = markingFunctions.combine(List.of(oneAnna, twoAnna));
        assertEquals(ACCESS.newExpression("A&B&C"), combined);
    }

    @Test
    public void testCombineOrs() throws MarkingFunctions.Exception {
        MarkingFunctions<AccessExpressionMarkings> markingFunctions = new MarkingFunctions.Default();

        AccessExpression oneOr = ACCESS.newExpression("A|B");
        AccessExpression twoOr = ACCESS.newExpression("A|C");

        AccessExpression combined = markingFunctions.combine(List.of(oneOr, twoOr));
        assertEquals(ACCESS.newExpression("(A|B)&(A|C)"), combined);
    }

    @Test
    public void testCombineMapsOfAnds() throws MarkingFunctions.Exception {
        MarkingFunctions<AccessExpressionMarkings> markingFunctions = new MarkingFunctions.Default();

        AccessExpressionMarkings cv1 = AccessExpressionMarkings.builder().accessExpression(ACCESS.newExpression("A&B")).build();
        AccessExpressionMarkings cv2 = AccessExpressionMarkings.builder().accessExpression(ACCESS.newExpression("A&C")).build();
        AccessExpressionMarkings expected = AccessExpressionMarkings.builder().accessExpression(ACCESS.newExpression("A&B&C")).build();

        assertEquals(expected, markingFunctions.combine(cv1, cv2));
    }

    @Test
    public void testCombineMapsOfOrs() throws MarkingFunctions.Exception {
        MarkingFunctions<AccessExpressionMarkings> markingFunctions = new MarkingFunctions.Default();

        AccessExpressionMarkings cv1 = AccessExpressionMarkings.builder().accessExpression(ACCESS.newExpression("A|B")).build();
        AccessExpressionMarkings cv2 = AccessExpressionMarkings.builder().accessExpression(ACCESS.newExpression("A|C")).build();
        AccessExpressionMarkings expected = AccessExpressionMarkings.builder().accessExpression(ACCESS.newExpression("(A|B)&(A|C)")).build();

        assertEquals(expected, markingFunctions.combine(cv1, cv2));
    }
}
