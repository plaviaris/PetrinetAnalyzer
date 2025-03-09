package org.example.services;

import org.example.objects.Arc;
import org.example.objects.PetriNet;
import org.example.objects.Place;
import org.example.objects.Transition;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.util.*;

public class PetriNetUtils {

    private static ProjectionInheritanceChecker projectionInheritanceChecker = new ProjectionInheritanceChecker();

    public static PetriNet loadPetriNet(File file) throws JAXBException {
        return XMLParser.loadPetriNet(file);
    }

    // Generates the reachability graph for the given PetriNet
    public static Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> generateReachabilityGraph(PetriNet petriNet) {
        Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> reachabilityGraph = new HashMap<>();
        Set<Map<String, Integer>> visitedMarkings = new HashSet<>();
        Queue<Map<String, Integer>> workQueue = new LinkedList<>();

        Map<String, Integer> initialMarking = getInitialMarking(petriNet);
        workQueue.add(initialMarking);
        reachabilityGraph.put(initialMarking, new HashMap<>());

        while (!workQueue.isEmpty()) {
            Map<String, Integer> currentMarking = workQueue.poll();

            for (Transition transition : petriNet.getTransitions()) {
                if (canFire(currentMarking, transition, petriNet)) {
                    Map<String, Integer> newMarking = fireTransition(currentMarking, transition, petriNet);

                    if (!visitedMarkings.contains(newMarking)) {
                        visitedMarkings.add(newMarking);
                        workQueue.add(newMarking);
                        reachabilityGraph.putIfAbsent(newMarking, new HashMap<>());
                    }

                    reachabilityGraph.get(currentMarking).put(transition, newMarking);
                }
            }
        }

        return reachabilityGraph;
    }


    // Determines the inheritance type between the parent and child PetriNet
    public static String determineInheritanceType(PetriNet parentNet, PetriNet childNet) {
        Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> parentGraph = generateReachabilityGraph(parentNet);
        Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> childGraph = generateReachabilityGraph(childNet);

        Set<String> parentTransitions = new HashSet<>();
        for (Transition t : parentNet.getTransitions()) {
            parentTransitions.add(t.getId());
        }

        Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> protocolChildGraph = filterGraph(childGraph, parentTransitions);

        boolean isProtocolInheritance = compareReachabilityGraphs(parentGraph, protocolChildGraph);
        boolean isProjectionInheritance = projectionInheritanceChecker.checkProjectionInheritanceUsingReachabilityGraph(parentGraph, childGraph, parentTransitions);


        if (isProtocolInheritance) {
            return "Protocol Inheritance";
        } else if (isProjectionInheritance) {
            return "Projection Inheritance";
        } else {
            return "No Inheritance";
        }
    }

    // Compares the reachability graphs of parent and child
    private static boolean compareReachabilityGraphs(Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> parentGraph,
                                                     Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> childGraph) {
        for (Map.Entry<Map<String, Integer>, Map<Transition, Map<String, Integer>>> parentEntry : parentGraph.entrySet()) {
            Map<String, Integer> parentMarking = parentEntry.getKey();

            boolean matchingMarking = childGraph.keySet().stream().anyMatch(childMarking -> markingsMatch(parentMarking, childMarking));
            if (!matchingMarking) {
                return false;
            }

            Map<String, Integer> matchingChildMarking = childGraph.keySet().stream()
                    .filter(childMarking -> markingsMatch(parentMarking, childMarking)).findFirst().orElse(null);
            Map<Transition, Map<String, Integer>> childTransitions = childGraph.get(matchingChildMarking);
            Map<Transition, Map<String, Integer>> parentTransitions = parentEntry.getValue();

            for (Transition parentTransition : parentTransitions.keySet()) {
                if (!childTransitions.containsKey(parentTransition) ||
                        !childTransitions.get(parentTransition).equals(parentTransitions.get(parentTransition))) {
                    return false;
                }
            }
        }
        return true;
    }

    // Filters the graph to only include transitions in the allowed list
    private static Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> filterGraph(Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> graph, Set<String> allowedTransitions) {
        Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> filteredGraph = new HashMap<>();
        for (Map.Entry<Map<String, Integer>, Map<Transition, Map<String, Integer>>> entry : graph.entrySet()) {
            Map<Transition, Map<String, Integer>> filteredTransitions = new HashMap<>();
            for (Map.Entry<Transition, Map<String, Integer>> transitionEntry : entry.getValue().entrySet()) {
                if (allowedTransitions.contains(transitionEntry.getKey().getId())) {
                    filteredTransitions.put(transitionEntry.getKey(), transitionEntry.getValue());
                }
            }
            filteredGraph.put(entry.getKey(), filteredTransitions);
        }
        return filteredGraph;
    }

    // Helper method to compare markings
    private static boolean markingsMatch(Map<String, Integer> parentMarking, Map<String, Integer> childMarking) {
        for (String place : parentMarking.keySet()) {
            if (!childMarking.containsKey(place) || !childMarking.get(place).equals(parentMarking.get(place))) {
                return false;
            }
        }
        return true;
    }

    // Gets the initial marking from the PetriNet
    private static Map<String, Integer> getInitialMarking(PetriNet petriNet) {
        Map<String, Integer> marking = new HashMap<>();
        for (Place place : petriNet.getPlaces()) {
            marking.put(place.getId(), place.getTokens());
        }
        return marking;
    }

    // Checks if a transition can fire given the current marking
    private static boolean canFire(Map<String, Integer> marking, Transition transition, PetriNet petriNet) {
        for (Arc arc : petriNet.getArcs()) {
            if (arc.getDestinationId().equals(transition.getId())) {
                String sourcePlaceId = arc.getSourceId();
                if (marking.getOrDefault(sourcePlaceId, 0) < arc.getMultiplicity()) {
                    return false;
                }
            }
        }
        return true;
    }

    // Fires a transition and returns the new marking
    private static Map<String, Integer> fireTransition(Map<String, Integer> marking, Transition transition, PetriNet petriNet) {
        Map<String, Integer> newMarking = new HashMap<>(marking);

        for (Arc arc : petriNet.getArcs()) {
            if (arc.getDestinationId().equals(transition.getId())) {
                String sourcePlaceId = arc.getSourceId();
                newMarking.put(sourcePlaceId, newMarking.get(sourcePlaceId) - arc.getMultiplicity());
            }
            if (arc.getSourceId().equals(transition.getId())) {
                String destinationPlaceId = arc.getDestinationId();
                newMarking.put(destinationPlaceId, newMarking.getOrDefault(destinationPlaceId, 0) + arc.getMultiplicity());
            }
        }

        return newMarking;
    }


    // Checks if the new marking is covered by the previous marking
    private static boolean isCovered(Map<String, Integer> previousMarking, Map<String, Integer> newMarking) {
        for (String place : previousMarking.keySet()) {
            if (previousMarking.get(place) > newMarking.getOrDefault(place, 0)) {
                return false;
            }
        }
        return true;
    }
}
