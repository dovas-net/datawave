package datawave.marking;

import org.apache.accumulo.access.AccessExpression;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccessExpressionMarkings implements Markings<AccessExpression> {

    private final AccessExpression accessExpression;

    @Override
    public AccessExpression getMarkings() {
        return getAccessExpression();
    }
}
