import ru.leti.wise.task.graph.model.Graph;
import ru.leti.wise.task.graph.model.Vertex;
import ru.leti.wise.task.graph.model.Edge;
import ru.leti.wise.task.plugin.graph.GraphProperty;

import java.util.*;

public class IsIntervalGraph implements GraphProperty {
    @Override
    public boolean run(Graph graph) {
        List<Vertex> vertices = graph.getVertexList();
        System.out.println("Вершины: " + vertices.stream().map(Vertex::getId).toList());
        System.out.println("Рёбра: " + graph.getEdgeList());
        int n = vertices.size();
        if (n == 0) return true; // Пустой граф является интервальным

        // Шаг 0: строим матрицу смежности
        Map<Integer, Set<Integer>> adjacencyMap = buildAdjacencyMap(graph);

        // Шаг 1: Выполнить лексикографический поиск в ширину (Lex-BFS)
        List<Integer> lexBFSOrder = performLexBFS(graph, adjacencyMap);
        System.out.println("Порядок Lex-BFS: " + lexBFSOrder);

        // Шаг 2: Проверить, является ли порядок совершенным порядком исключения
        if (!isPerfectEliminationOrder(graph, lexBFSOrder, adjacencyMap)) {
            System.out.println("Граф не является хордальным");
            return false;
        }

        // Шаг 3: Вычислить максимальные клики
        List<Set<Integer>> maximalCliques = computeMaximalCliques(graph, lexBFSOrder, adjacencyMap);
        System.out.println("Максимальные клики: " + maximalCliques);

        // Шаг 4: Проверить последовательность клик
        if (!checkConsecutiveCliques(maximalCliques, vertices)) {
            System.out.println("Клики не образуют последовательный порядок");
            return false;
        }

        return true; // Граф является интервальным
    }

    private boolean checkConsecutiveCliques(List<Set<Integer>> cliques, List<Vertex> vertices) {
        // Получаем порядок клик, образующий путь
        List<Integer> path = getCliquePathOrder(cliques);
        if (path == null) {
            System.out.println("Дерево клик не является путём");
            return false;
        }

        // Проверка последовательности индексов клик для каждой вершины в порядке пути
        for (Vertex vertex : vertices) {
            int vId = vertex.getId();
            List<Integer> cliqueIndices = new ArrayList<>();
            for (int i = 0; i < path.size(); i++) {
                if (cliques.get(path.get(i)).contains(vId)) {
                    cliqueIndices.add(i);
                }
            }
            if (!isConsecutive(cliqueIndices)) {
                System.out.println("Вершина " + vId + " имеет непоследовательные клики: " + cliqueIndices);
                return false;
            }
        }

        return true;
    }

    boolean isConsecutive(List<Integer> list) {
        if (list.isEmpty()) return true;
        int min = Collections.min(list);
        int max = Collections.max(list);
        return max - min + 1 == list.size(); // Проверка, что нет пропусков
    }

    List<Integer> getCliquePathOrder(List<Set<Integer>> cliques) {
        if (cliques.size() <= 2) {
            List<Integer> path = new ArrayList<>();
            for (int i = 0; i < cliques.size(); i++) path.add(i);
            return path;
        }

        // Строим граф пересечений клик
        List<List<Integer>> intersectionGraph = new ArrayList<>();
        for (int i = 0; i < cliques.size(); i++) {
            intersectionGraph.add(new ArrayList<>());
        }
        for (int i = 0; i < cliques.size(); i++) {
            for (int j = i + 1; j < cliques.size(); j++) {
                Set<Integer> intersection = new HashSet<>(cliques.get(i));
                intersection.retainAll(cliques.get(j));
                if (!intersection.isEmpty()) {
                    intersectionGraph.get(i).add(j);
                    intersectionGraph.get(j).add(i);
                }
            }
        }

        // Проверяем связность графа с помощью BFS
        boolean[] visited = new boolean[cliques.size()];
        bfs(0, intersectionGraph, visited);
        for (boolean v : visited) {
            if (!v) return null; // Граф не связный
        }

        // Проверяем количество концов
        int endpoints = 0;
        for (List<Integer> neighbors : intersectionGraph) {
            if (neighbors.size() == 1) endpoints++;
        }
        // Если нет концов и ≥ 3 клики, это цикл (например, треугольник)
        if (endpoints == 0 && cliques.size() >= 3) return null;

        // Пробуем построить путь с каждого возможного старта
        for (int start = 0; start < cliques.size(); start++) {
            List<Integer> path = new ArrayList<>();
            visited = new boolean[cliques.size()];
            path.add(start);
            visited[start] = true;
            int current = start;

            while (path.size() < cliques.size()) {
                boolean extended = false;
                for (int next : intersectionGraph.get(current)) {
                    if (!visited[next]) {
                        path.add(next);
                        visited[next] = true;
                        current = next;
                        extended = true;
                        break;
                    }
                }
                if (!extended) break;
            }

            if (path.size() == cliques.size()) {
                boolean valid = true;
                for (int i = 0; i < path.size() - 1; i++) {
                    Set<Integer> intersection = new HashSet<>(cliques.get(path.get(i)));
                    intersection.retainAll(cliques.get(path.get(i + 1)));
                    if (intersection.isEmpty()) {
                        valid = false;
                        break;
                    }
                }
                if (valid) return path;
            }
        }

        return null; // Нет подходящего пути
    }
    private void bfs(int start, List<List<Integer>> graph, boolean[] visited) {
        Queue<Integer> queue = new LinkedList<>();
        queue.add(start);
        visited[start] = true;
        while (!queue.isEmpty()) {
            int v = queue.poll();
            for (int u : graph.get(v)) {
                if (!visited[u]) {
                    visited[u] = true;
                    queue.add(u);
                }
            }
        }
    }

    List<Set<Integer>> computeMaximalCliques(Graph graph, List<Integer> order, Map<Integer, Set<Integer>> adjacencyMap) {
        Map<Integer, List<Integer>> rightNeighbors = new HashMap<>();
        Map<Integer, Integer> parent = new HashMap<>();
        Map<Integer, List<Integer>> children = new HashMap<>();
        Map<Integer, List<Integer>> rightNeighborsNoParent = new HashMap<>();

        for (Integer v : order) {
            children.put(v, new ArrayList<>());
        }

        for (int i = 0; i < order.size() - 1; i++) {
            int vertex = order.get(i);
            List<Integer> rn = new ArrayList<>();
            for (int j = i + 1; j < order.size(); j++) {
                int u = order.get(j);
                if (adjacencyMap.get(vertex).contains(u)) {
                    rn.add(u);
                }
            }
            rightNeighbors.put(vertex, rn);
            rightNeighborsNoParent.put(vertex, new ArrayList<>());
            if (!rn.isEmpty()) {
                parent.put(vertex, rn.get(0));
                children.get(rn.get(0)).add(vertex);
                for (int k = 1; k < rn.size(); k++) {
                    rightNeighborsNoParent.get(vertex).add(rn.get(k));
                }
            }
        }
        rightNeighbors.put(order.get(order.size() - 1), new ArrayList<>());

        List<Set<Integer>> cliques = new ArrayList<>();
        Map<Integer, Set<Integer>> cliqueMap = new HashMap<>();
        cliqueMap.put(order.get(order.size() - 1), new HashSet<>(Collections.singletonList(order.get(order.size() - 1))));
        generateCliques(order.get(order.size() - 1), children, cliqueMap, rightNeighbors, cliques);

        return cliques;
    }

    private void generateCliques(Integer node, Map<Integer, List<Integer>> children, Map<Integer, Set<Integer>> cliqueMap,
                                 Map<Integer, List<Integer>> rightNeighbors, List<Set<Integer>> cliques) {
        for (Integer child : children.get(node)) {
            Set<Integer> clique = new HashSet<>(rightNeighbors.get(child));
            clique.add(child);
            cliqueMap.put(child, clique);

            Integer parent = rightNeighbors.get(child).isEmpty() ? null : rightNeighbors.get(child).get(0);
            if (parent != null && cliqueMap.get(parent) != null && clique.containsAll(cliqueMap.get(parent))) {
                cliques.remove(cliqueMap.get(parent));
            }

            if (!cliques.contains(clique)) {
                cliques.add(clique);
            }

            if (!children.get(child).isEmpty()) {
                generateCliques(child, children, cliqueMap, rightNeighbors, cliques);
            }
        }
    }

    boolean isPerfectEliminationOrder(Graph graph, List<Integer> order, Map<Integer, Set<Integer>> adjacencyMap) {
        for (int i = 0; i < order.size(); i++) {
            int vertex = order.get(i);
            List<Integer> rightNeighbors = new ArrayList<>();
            for (int j = i + 1; j < order.size(); j++) {
                int u = order.get(j);
                if (adjacencyMap.get(vertex).contains(u)) {
                    rightNeighbors.add(u);
                }
            }
            System.out.println("Вершина " + vertex + " правые соседи: " + rightNeighbors);
            if (rightNeighbors.size() <= 1) continue;
            for (int j = 0; j < rightNeighbors.size(); j++) {
                int u1 = rightNeighbors.get(j);
                for (int k = j + 1; k < rightNeighbors.size(); k++) {
                    int u2 = rightNeighbors.get(k);
                    if (!adjacencyMap.get(u1).contains(u2)) {
                        System.out.println("Правые соседи вершины " + vertex + " (" + u1 + ", " + u2 + ") не связаны");
                        if (checkForInducedC4(graph, vertex, u1, u2, adjacencyMap)) {
                            System.out.println("Найден индуцированный C4 с вершинами " + vertex + ", " + u1 + ", " + u2);
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean checkForInducedC4(Graph graph, int v, int u1, int u2, Map<Integer, Set<Integer>> adjacencyMap) {
        Set<Integer> vertices = new HashSet<>(Arrays.asList(v, u1, u2));
        for (Vertex wVertex : graph.getVertexList()) {
            int w = wVertex.getId();
            if (vertices.contains(w)) continue;
            Set<Integer> wNeighbors = adjacencyMap.get(w);
            if (wNeighbors.contains(u1) && wNeighbors.contains(u2) && !wNeighbors.contains(v)) {
                // Проверяем цикл v-u1-w-u2-v
                Set<Integer> cycleVertices = new HashSet<>(Arrays.asList(v, u1, w, u2));
                boolean hasChord = false;
                // Проверяем прямые хорды
                if (adjacencyMap.get(v).contains(w) || adjacencyMap.get(u1).contains(u2)) {
                    hasChord = true;
                }
                // Проверяем треугольники, создающие хорды
                for (Vertex xVertex : graph.getVertexList()) {
                    int x = xVertex.getId();
                    if (cycleVertices.contains(x)) continue;
                    Set<Integer> xNeighbors = adjacencyMap.get(x);
                    if (xNeighbors.contains(v) && xNeighbors.contains(u1) && xNeighbors.contains(w)) {
                        hasChord = true; // Хорда в треугольнике v-u1-w
                    } else if (xNeighbors.contains(v) && xNeighbors.contains(u2) && xNeighbors.contains(w)) {
                        hasChord = true; // Хорда в треугольнике v-u2-w
                    } else if (xNeighbors.contains(u1) && xNeighbors.contains(w) && xNeighbors.contains(u2)) {
                        hasChord = true; // Хорда в треугольнике u1-w-u2
                    }
                }
                if (!hasChord) {
                    return true; // Найден индуцированный C4
                }
            }
        }
        return false;
    }

    Map<Integer, Set<Integer>> buildAdjacencyMap(Graph graph) {
        Map<Integer, Set<Integer>> adjacencyMap = new HashMap<>();
        if (graph.getVertexList() == null || graph.getVertexList().isEmpty()) {
            return adjacencyMap;
        }
        for (Vertex v : graph.getVertexList()) {
            Set<Integer> neighbours = getNeighbours(graph, v.getId());
            adjacencyMap.put(v.getId(), neighbours);
            System.out.println("Neighbours of " + v.getId() + ": " + neighbours);
        }
        return adjacencyMap;
    }

    Set<Integer> getNeighbours(Graph graph, int vertexId) {
        Set<Integer> neighbours = new HashSet<>();
        for (Edge edge : graph.getEdgeList()) {
            if (edge.getSource() == vertexId) {
                neighbours.add(edge.getTarget());
            } else if (edge.getTarget() == vertexId && !graph.isDirect()) {
                neighbours.add(edge.getSource());
            }
        }
        return neighbours;
    }

    List<Integer> performLexBFS(Graph graph, Map<Integer, Set<Integer>> adjacencyMap) {
        List<Integer> order = new ArrayList<>();
        List<Vertex> vertices = graph.getVertexList();
        if (vertices.isEmpty()) {
            return order;
        }

        Map<Integer, String> labels = new HashMap<>();
        Set<Integer> unvisited = new HashSet<>();
        for (Vertex v : vertices) {
            int vId = v.getId();
            labels.put(vId, "");
            unvisited.add(vId);
        }

        for (int i = 0; i < vertices.size(); i++) {
            int maxVertex = -1;
            String maxLabel = "";
            for (int v : unvisited) {
                String label = labels.get(v);
                if (maxVertex == -1 || label.compareTo(maxLabel) > 0) {
                    maxVertex = v;
                    maxLabel = label;
                }
            }

            if (maxVertex == -1) break;

            order.add(maxVertex);
            unvisited.remove(maxVertex);
            System.out.println("Выбрана вершина: " + maxVertex + ", Порядок: " + order);

            for (int u : unvisited) {
                if (adjacencyMap.get(maxVertex).contains(u)) {
                    labels.put(u, (vertices.size() - i) + "," + labels.get(u));
                }
            }
        }

        System.out.println("Порядок Lex-BFS: " + order);
        return order;
    }
}