package datawave.marking;

import org.apache.accumulo.access.Access;
import org.apache.accumulo.access.AccessExpression;
import org.apache.accumulo.access.examples.ParseExamples;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class AccessExpressionMarkings implements Markings<AccessExpression> {

    private static final Access ACCESS = ParseExamples.ACCESS;
    public static final String COLUMN_VISIBILITY_KEY = "columnVisibility";

    private final AccessExpression accessExpression;

    @Override
    public AccessExpression getMarkings() {
        return getAccessExpression();
    }

    @Override
    public boolean isEmpty() {
        return accessExpression == null || accessExpression.getExpression().isEmpty();
    }

    @JsonProperty(COLUMN_VISIBILITY_KEY)
    public String getColumnVisibilityString() {
        return accessExpression != null ? accessExpression.getExpression() : "";
    }

    @JsonCreator
    public static AccessExpressionMarkings create(@JsonProperty(COLUMN_VISIBILITY_KEY) String cv) {
        return AccessExpressionMarkings.builder().accessExpression(ACCESS.newExpression(cv != null ? cv : "")).build();
    }
}
