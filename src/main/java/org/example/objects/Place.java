package org.example.objects;

import javax.xml.bind.annotation.XmlElement;
import java.util.Objects;

public class Place {
    private String id;
    private int tokens;

    // Constructor
    public Place() {
    }

    public Place(String id, int tokens) {
        this.id = id;
        this.tokens = tokens;
    }

    @XmlElement(name = "id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlElement(name = "tokens")
    public int getTokens() {
        return tokens;
    }

    public void setTokens(int tokens) {
        this.tokens = tokens;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Place place = (Place) o;
        return id.equals(place.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
