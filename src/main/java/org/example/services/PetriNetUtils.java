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

        // 4. Projekčné dedenie (použije svoj checker)
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
            Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> g1,
            Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> g2,
            Set<String> placesToCheck) {

        for (Map.Entry<Map<String, Integer>, Map<Transition, Map<String, Integer>>> entry1 : g1.entrySet()) {
            Map<String, Integer> m1 = entry1.getKey();

            // 1) nájdi zodpovedajúce marking v g2 (len na rodičovských miestach)
            Optional<Map<String, Integer>> optM2 = g2.keySet().stream()
                    .filter(m2 -> markingsMatchOn(m1, m2, placesToCheck))
                    .findFirst();
            if (!optM2.isPresent()) {
                System.out.println("[PROTOCOL DEBUG] Chýba stav v detskom grafe pre marking rodiča: " + m1);
                return false;
            }

            Map<String, Integer> m2 = optM2.get();
            Map<Transition, Map<String, Integer>> trans1 = entry1.getValue();
            Map<Transition, Map<String, Integer>> trans2 = g2.get(m2);

            // 2) pre každý rodičovský prechod hľadaj zodpovedajúci v trans2
            for (Transition t1 : trans1.keySet()) {
                Transition t2 = trans2.keySet().stream()
                        .filter(c -> c.getId().equals(t1.getId()))
                        .findFirst().orElse(null);
                if (t2 == null) {
                    System.out.println("[PROTOCOL DEBUG] V detskom grafe chýba prechod '" +
                            t1.getId() + "' v stave: " + m2);
                    return false;
                }
                // 3) skontroluj ďalšie markingy na rodičovských miestach
                if (!markingsMatchOn(trans1.get(t1), trans2.get(t2), placesToCheck)) {
                    System.out.println("[PROTOCOL DEBUG] Nesúlad cieľových markingov pre prechod '"
                            + t1.getId() + "'. rodič očakáva: " + trans1.get(t1)
                            + ", dieťa má: " + trans2.get(t2));
                    return false;
                }
            }
        }
        return true;
    }

    private static Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> filterGraph(
            Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> graph,
            Set<String> allowedTransitions) {

        Map<Map<String, Integer>, Map<Transition, Map<String, Integer>>> filtered = new HashMap<>();
        for (Map.Entry<Map<String, Integer>, Map<Transition, Map<String, Integer>>> e : graph.entrySet()) {
            Map<Transition, Map<String, Integer>> m = new HashMap<>();
            for (Map.Entry<Transition, Map<String, Integer>> te : e.getValue().entrySet()) {
                if (allowedTransitions.contains(te.getKey().getId())) {
                    m.put(te.getKey(), te.getValue());
                }
            }
            filtered.put(e.getKey(), m);
        }
        return filtered;
    }

    /** Kontroluje označenia len na vybraných miestach */
    private static boolean markingsMatchOn(
            Map<String, Integer> p,
            Map<String, Integer> c,
            Set<String> placesToCheck) {

        for (String place : placesToCheck) {
            if (!Objects.equals(p.get(place), c.get(place))) {
                return false;
            }
        }
        // navyše v c nesmú byť tokeny na inom mieste (ak to chceme prísnejšie)
        for (String place : c.keySet()) {
            if (!placesToCheck.contains(place) && c.get(place) > 0) {
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
            Map<String, Integer> marking, Transition t, PetriNet net) {

        Map<String, Integer> nm = new HashMap<>(marking);
        for (Arc a : net.getArcs()) {
            if (a.getDestinationId().equals(t.getId())) {
                nm.put(a.getSourceId(), nm.get(a.getSourceId()) - a.getMultiplicity());
            }
            if (a.getSourceId().equals(t.getId())) {
                nm.put(a.getDestinationId(),
                        nm.getOrDefault(a.getDestinationId(), 0) + a.getMultiplicity());
            }
        }
        return nm;
    }

    @SuppressWarnings("unused")
    private static boolean isCovered(Map<String, Integer> prev, Map<String, Integer> next) {
        for (String place : prev.keySet()) {
            if (prev.get(place) > next.getOrDefault(place, 0)) {
                return false;
            }
        }
        return true;
    }
}
