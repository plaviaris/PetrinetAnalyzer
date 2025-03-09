package org.example.objects;

import javax.xml.bind.annotation.XmlElement;
import java.util.Objects;

public class Transition {
    private String id;

    // Constructor
    public Transition() {
    }

    public Transition(String id) {
        this.id = id;
    }

    @XmlElement(name = "id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transition that = (Transition) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
