package org.example.services;

import org.example.objects.Arc;
import org.example.objects.PetriNet;
import org.example.objects.Place;
import org.example.objects.Transition;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class ProjectionInheritanceChecker {

    // Sada ID prechodov, ktoré sú v parent sieti (viditeľné).
    private final Set<String> parentTransitionIds = new HashSet<>();

    public ProjectionInheritanceChecker() {
        // prázdny konštruktor
    }


    public void initParent(PetriNet parentNet) {
        parentTransitionIds.clear();
        for (Transition t : parentNet.getTransitions()) {
            parentTransitionIds.add(t.getId());
        }
    }

    public boolean checkProjectionInheritance(PetriNet parentNet, PetriNet childNet, boolean debugPrint) {
        // 1. Najprv vygenerujeme LTS z parent aj child siete
        initParent(parentNet); // zapamätáme si ID prechodov parenta

        // LTS pre parent
        Map<Map<String, Integer>, List<LTSEdge>> ltsParent = buildLTS(parentNet, false);

        // LTS pre child
        Map<Map<String, Integer>, List<LTSEdge>> ltsChild = buildLTS(childNet, true);

        // Voliteľne vytlačíme textovú reprezentáciu LTS
        if (debugPrint) {
            System.out.println("=== PARENT LTS ===");
            printLTS(ltsParent);

            System.out.println("=== CHILD LTS ===");
            printLTS(ltsChild);

            // Alebo si vygenerujeme DOT reťazce:
            String dotParent = ltsToDot("ParentLTS", ltsParent);
            String dotChild = ltsToDot("ChildLTS", ltsChild);

            // Môžeš si ich vypísať alebo uložiť do súborov:
            System.out.println("DOT for Parent:\n" + dotParent);
            System.out.println("DOT for Child:\n" + dotChild);
        }

        // 2. Začiatočné stavy
        Map<String,Integer> initParent = getInitialMarking(parentNet);
        Map<String,Integer> initChild  = getInitialMarking(childNet);

        // 3. Spustíme weak-bisimulation check
        boolean result = isWeakBisimilar(ltsParent, initParent, ltsChild, initChild);

        if (debugPrint) {
            System.out.println("Projection Inheritance result = " + result);
        }
        return result;
    }

    private Map<Map<String,Integer>, List<LTSEdge>> buildLTS(PetriNet net, boolean applyTauLogic) {
        Map<Map<String,Integer>, List<LTSEdge>> adjacency = new HashMap<>();

        Queue<Map<String,Integer>> queue = new LinkedList<>();
        Set<Map<String,Integer>> visited = new HashSet<>();

        Map<String,Integer> init = getInitialMarking(net);
        queue.add(init);
        visited.add(init);
        adjacency.put(init, new ArrayList<>());

        while (!queue.isEmpty()) {
            Map<String,Integer> currentMarking = queue.poll();

            for (Transition t : net.getTransitions()) {
                if (canFire(currentMarking, t, net)) {
                    Map<String,Integer> newMarking = fireTransition(currentMarking, t, net);

                    // "tau" ak nie je v parent sieti
                    String label;
                    if (applyTauLogic) {
                        boolean isTau = !parentTransitionIds.contains(t.getId());
                        label = isTau ? "tau" : t.getId();
                    } else {
                        // V parent nete (predpokladáme, že nič nie je tiché),
                        // ale ak by si chcel, mohol by si sem dať aj svoju logiku.
                        label = t.getId();
                    }

                    adjacency.get(currentMarking).add(new LTSEdge(label, newMarking));

                    if (!visited.contains(newMarking)) {
                        visited.add(newMarking);
                        adjacency.put(newMarking, new ArrayList<>());
                        queue.add(newMarking);
                    }
                }
            }
        }

        return adjacency;
    }

    private Map<String,Integer> getInitialMarking(PetriNet net) {
        Map<String,Integer> marking = new HashMap<>();
        for (Place p : net.getPlaces()) {
            marking.put(p.getId(), p.getTokens());
        }
        return marking;
    }

    private boolean canFire(Map<String,Integer> marking, Transition t, PetriNet net) {
        for (Arc arc : net.getArcs()) {
            if (arc.getDestinationId().equals(t.getId())) {
                int available = marking.getOrDefault(arc.getSourceId(), 0);
                if (available < arc.getMultiplicity()) {
                    return false;
                }
            }
        }
        return true;
    }

    private Map<String,Integer> fireTransition(Map<String,Integer> state, Transition t, PetriNet net) {
        Map<String,Integer> newState = new HashMap<>(state);

        // Odober tokeny z miest, ktoré vedú do t
        for (Arc arc : net.getArcs()) {
            if (arc.getDestinationId().equals(t.getId())) {
                String placeId = arc.getSourceId();
                newState.put(placeId, newState.get(placeId) - arc.getMultiplicity());
            }
        }
        // Pridaj tokeny do miest, kam t vedie
        for (Arc arc : net.getArcs()) {
            if (arc.getSourceId().equals(t.getId())) {
                String placeId = arc.getDestinationId();
                int oldVal = newState.getOrDefault(placeId, 0);
                newState.put(placeId, oldVal + arc.getMultiplicity());
            }
        }
        return newState;
    }

    //--------------------------------------------------------------------------
    // 2) Pomocné triedy pre LTS + páry stavov
    //--------------------------------------------------------------------------

    private static class LTSEdge {
        final String label;                     // "tau" alebo transitionId
        final Map<String,Integer> targetState;

        LTSEdge(String label, Map<String,Integer> targetState) {
            this.label = label;
            this.targetState = targetState;
        }
    }

    private static class Pair {
        final Map<String,Integer> sA;
        final Map<String,Integer> sB;

        Pair(Map<String,Integer> sA, Map<String,Integer> sB) {
            this.sA = sA;
            this.sB = sB;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pair)) return false;
            Pair other = (Pair) o;
            return Objects.equals(sA, other.sA) && Objects.equals(sB, other.sB);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sA, sB);
        }
    }

    private static class Move {
        final String label;
        final Map<String,Integer> target;

        Move(String label, Map<String,Integer> target) {
            this.label = label;
            this.target = target;
        }
    }

    //--------------------------------------------------------------------------
    // 3) Weak-bisimulation check
    //--------------------------------------------------------------------------

    private boolean isWeakBisimilar(
            Map<Map<String,Integer>, List<LTSEdge>> ltsA, Map<String,Integer> initA,
            Map<Map<String,Integer>, List<LTSEdge>> ltsB, Map<String,Integer> initB
    ) {
        Set<Pair> R = new HashSet<>();
        Queue<Pair> queue = new LinkedList<>();

        Pair start = new Pair(initA, initB);
        R.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            Pair current = queue.poll();
            Map<String,Integer> sA = current.sA;
            Map<String,Integer> sB = current.sB;

            // 1. Pre všetky viditeľné akcie z sA
            for (Move moveA : getAllVisibleMoves(ltsA, sA)) {
                String alpha = moveA.label;
                Map<String,Integer> sAPrime = moveA.target;

                // Z sB hľadáme stavy (τ*)-alpha-(τ*) => sB'
                List<Map<String,Integer>> matchesB = getMatchingStates(ltsB, sB, alpha);
                if (matchesB.isEmpty()) {
                    return false;
                }
                for (Map<String,Integer> sBPrime : matchesB) {
                    Pair candidate = new Pair(sAPrime, sBPrime);
                    if (!R.contains(candidate)) {
                        R.add(candidate);
                        queue.add(candidate);
                    }
                }
            }

            // 2. Symetricky pre sB
            for (Move moveB : getAllVisibleMoves(ltsB, sB)) {
                String alpha = moveB.label;
                Map<String,Integer> sBPrime = moveB.target;

                List<Map<String,Integer>> matchesA = getMatchingStates(ltsA, sA, alpha);
                if (matchesA.isEmpty()) {
                    return false;
                }
                for (Map<String,Integer> sAPrime : matchesA) {
                    Pair candidate = new Pair(sAPrime, sBPrime);
                    if (!R.contains(candidate)) {
                        R.add(candidate);
                        queue.add(candidate);
                    }
                }
            }
        }
        return true; // nenašiel sa rozpor
    }

    //--------------------------------------------------------------------------
    // 4) Pomocné metódy na τ + viditeľné prechody
    //--------------------------------------------------------------------------

    private List<Move> getAllVisibleMoves(Map<Map<String,Integer>, List<LTSEdge>> lts,
                                          Map<String,Integer> s) {
        List<Move> result = new ArrayList<>();
        Set<Map<String,Integer>> tauClosure = getTauClosure(lts, s);

        for (Map<String,Integer> intermediate : tauClosure) {
            List<LTSEdge> edges = lts.getOrDefault(intermediate, Collections.emptyList());
            for (LTSEdge edge : edges) {
                if (!"tau".equals(edge.label)) {
                    Set<Map<String,Integer>> afterAlpha = getTauClosure(lts, edge.targetState);
                    for (Map<String,Integer> stFinal : afterAlpha) {
                        result.add(new Move(edge.label, stFinal));
                    }
                }
            }
        }
        return result;
    }

    private Set<Map<String,Integer>> getTauClosure(Map<Map<String,Integer>, List<LTSEdge>> lts,
                                                   Map<String,Integer> s) {
        Set<Map<String,Integer>> closure = new HashSet<>();
        Stack<Map<String,Integer>> stack = new Stack<>();
        closure.add(s);
        stack.push(s);

        while (!stack.isEmpty()) {
            Map<String,Integer> top = stack.pop();
            List<LTSEdge> edges = lts.getOrDefault(top, Collections.emptyList());
            for (LTSEdge e : edges) {
                if ("tau".equals(e.label) && !closure.contains(e.targetState)) {
                    closure.add(e.targetState);
                    stack.push(e.targetState);
                }
            }
        }
        return closure;
    }

    private List<Map<String,Integer>> getMatchingStates(Map<Map<String,Integer>, List<LTSEdge>> lts,
                                                        Map<String,Integer> s,
                                                        String alpha) {
        List<Map<String,Integer>> result = new ArrayList<>();
        Set<Map<String,Integer>> initialClosure = getTauClosure(lts, s);

        for (Map<String,Integer> st : initialClosure) {
            List<LTSEdge> edges = lts.getOrDefault(st, Collections.emptyList());
            for (LTSEdge e : edges) {
                if (alpha.equals(e.label)) {
                    Set<Map<String,Integer>> afterAlpha = getTauClosure(lts, e.targetState);
                    result.addAll(afterAlpha);
                }
            }
        }
        return result;
    }

    //--------------------------------------------------------------------------
    // 5) Metódy na zobrazenie / debug výpis LTS
    //--------------------------------------------------------------------------

    /**
     * Jednoduchý textový výpis LTS do konzoly.
     */
    public void printLTS(Map<Map<String,Integer>, List<LTSEdge>> lts) {
        System.out.println("----- LTS START -----");
        for (Map.Entry<Map<String,Integer>, List<LTSEdge>> entry : lts.entrySet()) {
            Map<String,Integer> state = entry.getKey();
            List<LTSEdge> edges = entry.getValue();

            System.out.println("State: " + state);
            for (LTSEdge e : edges) {
                System.out.println("   --" + e.label + "--> " + e.targetState);
            }
        }
        System.out.println("----- LTS END -----");
    }

    /**
     * Vygeneruje jednoduchý DOT (GraphViz) reťazec pre daný LTS.
     * Ten si môžeš uložiť do súboru napr. "mygraph.dot" a potom spustiť:
     *   dot -Tpdf mygraph.dot -o mygraph.pdf
     * alebo použiť akýkoľvek online GraphViz editor.
     */
    public String ltsToDot(String graphName, Map<Map<String,Integer>, List<LTSEdge>> lts) {
        // Najprv si zmapujeme "marking" -> ID (napr. S0, S1, ...)
        Map<Map<String,Integer>, String> stateIds = new HashMap<>();
        List<Map<String,Integer>> allStates = new ArrayList<>(lts.keySet());

        // Pomenovanie stavov
        for (int i = 0; i < allStates.size(); i++) {
            Map<String,Integer> st = allStates.get(i);
            stateIds.put(st, "S" + i);
        }

        // Vygenerujeme DOT
        StringBuilder sb = new StringBuilder();
        sb.append("digraph ").append(graphName).append(" {\n");
        sb.append("  rankdir=LR;\n");
        sb.append("  node [shape=box];\n\n");

        // Definícia uzlov
        for (Map<String,Integer> st : allStates) {
            String stId = stateIds.get(st);
            sb.append("  ").append(stId)
                    .append(" [label=\"").append(stId).append("\\n")
                    .append(st.toString().replace("{","").replace("}",""))
                    .append("\"];\n");
        }

        sb.append("\n");
        // Definícia hrán
        for (Map.Entry<Map<String,Integer>, List<LTSEdge>> entry : lts.entrySet()) {
            Map<String,Integer> sourceState = entry.getKey();
            String sourceId = stateIds.get(sourceState);

            for (LTSEdge edge : entry.getValue()) {
                String targetId = stateIds.get(edge.targetState);
                sb.append("  ").append(sourceId)
                        .append(" -> ").append(targetId)
                        .append(" [label=\"").append(edge.label).append("\"];\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }
}
