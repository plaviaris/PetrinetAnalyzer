package org.example.services;

import org.example.objects.Arc;
import org.example.objects.PetriNet;
import org.example.objects.Place;
import org.example.objects.Transition;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class PetriNetUtils {

    private static ProjectionInheritanceChecker projectionInheritanceChecker = new ProjectionInheritanceChecker();

    public static PetriNet loadPetriNet(File file) throws JAXBException {
        return XMLParser.loadPetriNet(file);
    }

    public static Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> generateReachabilityGraph(PetriNet petriNet) {
        Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> reachabilityGraph = new HashMap<>();
        Set<Map<String, Integer>> visitedMarkings = new HashSet<>();
        Queue<Map<String, Integer>> workQueue = new LinkedList<>();

        Map<String, Integer> initialMarking = getInitialMarking(petriNet);
        visitedMarkings.add(initialMarking);
        workQueue.add(initialMarking);
        reachabilityGraph.put(initialMarking, new HashMap<>());

        int stepCount = 0;
        final int MAX_STEPS = 10_000;

        while (!workQueue.isEmpty()) {
            if (++stepCount > MAX_STEPS) {
                throw new IllegalStateException(
                        "Reachability graph is too large – more than " + MAX_STEPS + " states.");
            }
            Map<String, Integer> currentMarking = workQueue.poll();
            for (Transition transition : petriNet.getTransitions()) {
                if (canFire(currentMarking, transition, petriNet)) {
                    Map<String, Integer> newMarking = fireTransition(currentMarking, transition, petriNet);

                    if (visitedMarkings.add(newMarking)) {
                        workQueue.add(newMarking);
                        reachabilityGraph.putIfAbsent(newMarking, new HashMap<>());
                    }

                    reachabilityGraph.get(currentMarking).put(transition, newMarking);
                }
            }
        }
        return reachabilityGraph;
    }

    public static String determineInheritanceType(PetriNet parentNet, PetriNet childNet) {
        Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> parentGraph =
                generateReachabilityGraph(parentNet);
        Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> childGraph =
                generateReachabilityGraph(childNet);

        Set<String> parentTransitions = parentNet.getTransitions().stream()
                .map(Transition::getId).collect(Collectors.toSet());

        Set<String> parentPlaces = parentNet.getPlaces().stream()
                .map(Place::getId).collect(Collectors.toSet());

        Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> protocolChildGraph =
                filterGraph(childGraph, parentTransitions);

        boolean isProtocolInheritance =
                compareReachabilityGraphs(parentGraph, protocolChildGraph, parentPlaces);

        boolean isProjectionInheritance =
                projectionInheritanceChecker.checkProjectionInheritanceUsingReachabilityGraph(
                        parentGraph, childGraph, parentTransitions);

        if (isProtocolInheritance) {
            return "Protocol Inheritance";
        } else if (isProjectionInheritance) {
            return "Projection Inheritance";
        } else {
            return "No Inheritance";
        }
    }

    private static boolean compareReachabilityGraphs(
            Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> parentGraph,
            Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> childGraph,
            Set<String> parentPlaces) {

        for (var parentEntry : parentGraph.entrySet()) {
            Map<String, Integer> parentMarking = parentEntry.getKey();

            Optional<Map<String, Integer>> childMarkingOpt = childGraph.keySet().stream()
                    .filter(childMarking -> markingsMatchOn(parentMarking, childMarking, parentPlaces))
                    .findFirst();
            if (childMarkingOpt.isEmpty()) {
                System.out.println("[PROTOCOL DEBUG] Chýba stav v dieťati pre rodičovské značkovanie: "
                        + parentMarking);
                return false;
            }
            Map<String, Integer> childMarking = childMarkingOpt.get();

            Map<Transition, Map<String, Integer>> parentTransitions = parentEntry.getValue();
            Map<Transition, Map<String, Integer>> childTransitions  = childGraph.get(childMarking);

            for (Transition parentT : parentTransitions.keySet()) {
                Optional<Transition> childTopt = childTransitions.keySet().stream()
                        .filter(ct -> ct.getId().equals(parentT.getId()))
                        .findFirst();
                if (childTopt.isEmpty()) {
                    System.out.println("[PROTOCOL DEBUG] Dieťa neobsahuje prechod '"
                            + parentT.getId() + "' v stave: " + childMarking);
                    return false;
                }
                Transition childT = childTopt.get();

                Map<String, Integer> parentNextMarking = parentTransitions.get(parentT);
                Map<String, Integer> childNextMarking  = childTransitions.get(childT);
                if (!markingsMatchOn(parentNextMarking, childNextMarking, parentPlaces)) {
                    System.out.println("[PROTOCOL DEBUG] Nesúlad cieľových prechodov pre prechod '"
                            + parentT.getId() + "'. rodič očakáva: " + parentNextMarking
                            + ", dieťa má: " + childNextMarking);
                    return false;
                }
            }
        }

        return true;
    }

    private static Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>>
    filterGraph(
            Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> reachGraph,
            Set<String> allowedTransitionIds
    ) {
        Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> filteredGraph = new HashMap<>();

        for (Map.Entry<Map<String, Integer>, Map<Transition, Map<String, Integer>>> entry : reachGraph.entrySet()) {
            Map<Transition, Map<String, Integer>> outgoing = entry.getValue();
            Map<Transition, Map<String, Integer>> pruned = new HashMap<>();

            for (Map.Entry<Transition, Map<String, Integer>> transEntry : outgoing.entrySet()) {
                String id = transEntry.getKey().getId();
                if (allowedTransitionIds.contains(id)) {
                    pruned.put(transEntry.getKey(), transEntry.getValue());
                }
            }

            filteredGraph.put(entry.getKey(), pruned);
        }

        return filteredGraph;
    }

    private static boolean markingsMatchOn(
            Map<String, Integer> parentMarking,
            Map<String, Integer> childMarking,
            Set<String> commonPlaces) {

        for (String placeId : commonPlaces) {
            Integer parentTokens = parentMarking.getOrDefault(placeId, 0);
            Integer childTokens  = childMarking.getOrDefault(placeId, 0);
            if (!Objects.equals(parentTokens, childTokens)) {
                return false;
            }
        }

        for (Map.Entry<String, Integer> entry : childMarking.entrySet()) {
            String placeId = entry.getKey();
            Integer tokens = entry.getValue();
            if (!commonPlaces.contains(placeId) && tokens > 0) {
                return false;
            }
        }

        return true;
    }

    private static Map<String, Integer> getInitialMarking(PetriNet net) {
        Map<String, Integer> m = new HashMap<>();
        for (Place p : net.getPlaces()) {
            m.put(p.getId(), p.getTokens());
        }
        return m;
    }

    private static boolean canFire(Map<String, Integer> marking, Transition t, PetriNet net) {
        for (Arc a : net.getArcs()) {
            if (a.getDestinationId().equals(t.getId())) {
                if (marking.getOrDefault(a.getSourceId(), 0) < a.getMultiplicity()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Map<String, Integer> fireTransition(
            Map<String, Integer> currentMarking,
            Transition transition,
            PetriNet petriNet) {

        Map<String, Integer> newMarking = new HashMap<>(currentMarking);

        for (Arc arc : petriNet.getArcs()) {
            if (arc.getDestinationId().equals(transition.getId())) {
                String placeId = arc.getSourceId();
                int oldTokens = newMarking.getOrDefault(placeId, 0);
                newMarking.put(placeId, oldTokens - arc.getMultiplicity());
            }
        }

        for (Arc arc : petriNet.getArcs()) {
            if (arc.getSourceId().equals(transition.getId())) {
                String placeId = arc.getDestinationId();
                int oldTokens = newMarking.getOrDefault(placeId, 0);
                newMarking.put(placeId, oldTokens + arc.getMultiplicity());
            }
        }

        return newMarking;
    }
}
