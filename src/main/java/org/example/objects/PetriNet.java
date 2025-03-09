package org.example.objects;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Objects;

@XmlRootElement(name = "document")
public class PetriNet {
    private List<Place> places;
    private List<Transition> transitions;
    private List<Arc> arcs;

    public PetriNet() {
    }

    @XmlElement(name = "place")
    public List<Place> getPlaces() {
        return places;
    }

    public void setPlaces(List<Place> places) {
        this.places = places;
    }

    @XmlElement(name = "transition")
    public List<Transition> getTransitions() {
        return transitions;
    }

    public void setTransitions(List<Transition> transitions) {
        this.transitions = transitions;
    }

    @XmlElement(name = "arc")
    public List<Arc> getArcs() {
        return arcs;
    }

    public void setArcs(List<Arc> arcs) {
        this.arcs = arcs;
    }

    public boolean isSubsetOf(PetriNet other) {
        for (Place place : this.places) {
            if (!other.places.contains(place)) {
                return false;
            }
        }

        for (Transition transition : this.transitions) {
            if (!other.transitions.contains(transition)) {
                return false;
            }
        }

        for (Arc arc : this.arcs) {
            if (!other.arcs.contains(arc)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PetriNet petriNet = (PetriNet) o;
        return Objects.equals(places, petriNet.places) &&
                Objects.equals(transitions, petriNet.transitions) &&
                Objects.equals(arcs, petriNet.arcs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(places, transitions, arcs);
    }
}
