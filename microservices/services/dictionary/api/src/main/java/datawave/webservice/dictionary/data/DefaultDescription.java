package datawave.webservice.dictionary.data;

import static org.apache.accumulo.access.examples.ParseExamples.ACCESS;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.accumulo.access.AccessExpression;

import datawave.marking.AccessExpressionMarkings;
import datawave.marking.Markings;
import datawave.webservice.query.result.event.MapSchema;
import io.protostuff.Input;
import io.protostuff.Message;
import io.protostuff.Output;
import io.protostuff.Schema;

@XmlAccessorType(XmlAccessType.NONE)
@XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL)
public class DefaultDescription extends DescriptionBase<DefaultDescription> implements Serializable, Message<DefaultDescription> {
    private static final long serialVersionUID = 6756537335803261872L;

    private static final String CV_KEY = "columnVisibility";

    public DefaultDescription() {}

    public DefaultDescription(String description) {
        this.description = description;
    }

    public DefaultDescription(String description, Map<String,String> markingsMap) {
        this.description = description;
        this.markingsMap = markingsMap;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public void setMarkings(Markings<?> markings) {
        if (markings == null || markings.isEmpty()) {
            this.markingsMap = null;
        } else {
            Object m = markings.getMarkings();
            if (m instanceof AccessExpression) {
                this.markingsMap = new HashMap<>();
                this.markingsMap.put(CV_KEY, ((AccessExpression) m).getExpression());
            }
        }
    }

    @Override
    public Markings<?> getMarkings() {
        if (this.markingsMap != null && this.markingsMap.containsKey(CV_KEY)) {
            String expr = this.markingsMap.get(CV_KEY);
            return AccessExpressionMarkings.builder().accessExpression(ACCESS.newExpression(expr)).build();
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DefaultDescription) {
            DefaultDescription other = (DefaultDescription) o;
            return Objects.equals(this.description, other.description);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.description.hashCode();
    }

    @Override
    public String toString() {
        return getDescription();
    }

    public static Schema<DefaultDescription> getSchema() {
        return SCHEMA;
    }

    @Override
    /*
     * (non-Javadoc)
     *
     * @see io.protostuff.Message#cachedSchema()
     */
    public Schema<DefaultDescription> cachedSchema() {
        return SCHEMA;
    }

    @XmlTransient
    private static final Schema<DefaultDescription> SCHEMA = new Schema<DefaultDescription>() {
        public DefaultDescription newMessage() {
            return new DefaultDescription();
        }

        public Class<DefaultDescription> typeClass() {
            return DefaultDescription.class;
        }

        public String messageName() {
            return DefaultDescription.class.getSimpleName();
        }

        public String messageFullName() {
            return DefaultDescription.class.getName();
        }

        public boolean isInitialized(DefaultDescription message) {
            return true;
        }

        public void writeTo(Output output, DefaultDescription message) throws IOException {
            if (message.description != null) {
                output.writeString(1, message.description, false);
            }
            // markings field 2 no longer written via MapSchema; markingsMap is bridged via Markings<?>
        }

        public void mergeFrom(Input input, DefaultDescription message) throws IOException {
            int number;
            while ((number = input.readFieldNumber(this)) != 0) {
                switch (number) {
                    case 1:
                        message.description = input.readString();
                        break;
                    case 2:
                        // legacy markings map — consume but discard
                        HashMap<String,String> legacyMarkings = new HashMap<>();
                        input.mergeObject(legacyMarkings, MapSchema.SCHEMA);
                        break;
                    default:
                        input.handleUnknownField(number, this);
                        break;
                }
            }
        }

        public String getFieldName(int number) {
            switch (number) {
                case 1:
                    return "description";
                case 2:
                    return "markings";
                default:
                    return null;
            }
        }

        public int getFieldNumber(String name) {
            final Integer number = fieldMap.get(name);
            return number == null ? 0 : number;
        }

        final java.util.HashMap<String,Integer> fieldMap = new java.util.HashMap<String,Integer>();
        {
            fieldMap.put("description", 1);
            fieldMap.put("markings", 2);
        }
    };

}
