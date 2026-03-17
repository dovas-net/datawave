package datawave.webservice.dictionary.data;

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import datawave.webservice.query.result.event.HasMarkings;
import datawave.webservice.xml.util.StringMapAdapter;
import io.protostuff.Message;

@XmlAccessorType(XmlAccessType.NONE)
public abstract class DescriptionBase<T> implements HasMarkings, Message<T> {

    @XmlElement(name = "description")
    protected String description;

    /**
     * JAXB-serialized markings map. The {@link HasMarkings} interface methods {@link #getMarkings()}/{@link #setMarkings} operate on {@code Markings<?>};
     * subclasses must bridge between this map and the Markings type.
     */
    @XmlElement(name = "markings")
    @XmlJavaTypeAdapter(StringMapAdapter.class)
    protected Map<String,String> markingsMap;

    /** Direct access to the JAXB markings map. */
    public Map<String,String> getMarkingsMap() {
        return markingsMap;
    }

    /** Direct access to set the JAXB markings map. */
    public void setMarkingsMap(Map<String,String> markingsMap) {
        this.markingsMap = markingsMap;
    }

    public abstract String getDescription();

    public abstract void setDescription(String description);

}
