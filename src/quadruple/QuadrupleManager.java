package quadruple;

import java.util.*;

public class QuadrupleManager {
    private List<Quadruple> quads = new ArrayList<>();
    private int tempCount = 0;
    private int labelCount = 0;
    private Map<String, List<Integer>> pending = new HashMap<>();

    public int emit(String op, String a1, String a2, String r) {
        quads.add(new Quadruple(op, a1, a2, r));
        return quads.size();
    }

    public int emit(String op) {
        return emit(op, "_", "_", "_");
    }

    public int emit(String op, String a1) {
        return emit(op, a1, "_", "_");
    }

    public int emit(String op, String a1, String a2) {
        return emit(op, a1, a2, "_");
    }

    public String newTemp() {
        tempCount++;
        return "T" + tempCount;
    }

    public String newLabel() {
        labelCount++;
        return "$" + labelCount;
    }

    public int emitWP(String op, String a1, String a2, String label) {
        int idx = emit(op, a1, a2, label);
        pending.computeIfAbsent(label, k -> new ArrayList<>()).add(idx);
        return idx;
    }

    public int patch(String label) {
        int pos = quads.size() + 1;
        List<Integer> idxs = pending.get(label);
        if (idxs != null) {
            for (int i : idxs) {
                quads.get(i - 1).result = "" + pos;
            }
            pending.remove(label);
        }
        return pos;
    }

    public String toStr() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < quads.size(); i++) {
            sb.append("(").append(i + 1).append(")").append(quads.get(i)).append("\n");
        }
        return sb.toString().trim();
    }
}
