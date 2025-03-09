package org.example.objects;

import javax.xml.bind.annotation.XmlElement;
import java.util.Objects;

public class Arc {
    private String sourceId;
    private String destinationId;
    private int multiplicity;

    // Default constructor
    public Arc() {
    }

    // Constructor with multiplicity
    public Arc(String sourceId, String destinationId, int multiplicity) {
        this.sourceId = sourceId;
        this.destinationId = destinationId;
        this.multiplicity = multiplicity;
    }

    @XmlElement(name = "sourceId")
    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    @XmlElement(name = "destinationId")
    public String getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(String destinationId) {
        this.destinationId = destinationId;
    }

    @XmlElement(name = "multiplicity")
    public int getMultiplicity() {
        return multiplicity;
    }

    public void setMultiplicity(int multiplicity) {
        this.multiplicity = multiplicity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Arc arc = (Arc) o;
        return multiplicity == arc.multiplicity &&
                sourceId.equals(arc.sourceId) &&
                destinationId.equals(arc.destinationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceId, destinationId, multiplicity);
    }
}
