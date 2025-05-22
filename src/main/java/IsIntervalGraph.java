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
        System.out.println("Максимальные клики до упорядочивания: " + maximalCliques);

        // Шаг 4: Упорядочить клики с помощью Lex-BFS
        maximalCliques = orderCliquesWithLexBFS(maximalCliques);
        System.out.println("Максимальные клики после упорядочивания: " + maximalCliques);

        // Шаг 5: Построим граф клик
        List<List<Integer>> cliqueGraph = new ArrayList<>();
        for (int i = 0; i < maximalCliques.size(); i++) {
            cliqueGraph.add(new ArrayList<>());
        }
        for (int i = 0; i < maximalCliques.size(); i++) {
            for (int j = i + 1; j < maximalCliques.size(); j++) {
                Set<Integer> intersection = new HashSet<>(maximalCliques.get(i));
                intersection.retainAll(maximalCliques.get(j));
                if (!intersection.isEmpty()) {
                    cliqueGraph.get(i).add(j);
                    cliqueGraph.get(j).add(i);
                }
            }
        }
        System.out.println("Граф клик: " + cliqueGraph);

        // Шаг 6: Проверить, образуют ли клики связное поддерево для каждой вершины
        for (Vertex v : vertices) {
            int vId = v.getId();
            List<Integer> cliques = new ArrayList<>();
            for (int i = 0; i < maximalCliques.size(); i++) {
                if (maximalCliques.get(i).contains(vId)) {
                    cliques.add(i);
                }
            }
            System.out.println("Вершина " + vId + " клики: " + cliques);
            // Проверяем, что клики образуют связное поддерево
            if (!cliques.isEmpty()) {
                // Создаём подграф клик, содержащий только клики с vId
                List<List<Integer>> subCliqueGraph = new ArrayList<>();
                for (int i = 0; i < maximalCliques.size(); i++) {
                    subCliqueGraph.add(new ArrayList<>());
                }
                for (int i : cliques) {
                    for (int j : cliques) {
                        if (i < j && cliqueGraph.get(i).contains(j)) {
                            subCliqueGraph.get(i).add(j);
                            subCliqueGraph.get(j).add(i);
                        }
                    }
                }
                // Проверяем связность подграфа
                boolean[] visited = new boolean[maximalCliques.size()];
                dfs(cliques.get(0), subCliqueGraph, visited);
                for (int cliqueIndex : cliques) {
                    if (!visited[cliqueIndex]) {
                        System.out.println("Клики для вершины " + vId + " не образуют связное поддерево");
                        return false;
                    }
                }
            }
        }

        return true; // Граф является интервальным
    }

    private void dfs(int start, List<List<Integer>> cliqueGraph, boolean[] visited) {
        visited[start] = true;
        for (int neighbor : cliqueGraph.get(start)) {
            if (!visited[neighbor]) {
                dfs(neighbor, cliqueGraph, visited);
            }
        }
    }

    List<Integer> performLexBFS(Graph graph, Map<Integer, Set<Integer>> adjacencyMap) {
        List<Integer> order = new ArrayList<>();
        List<Vertex> vertices = graph.getVertexList();
        if (vertices.isEmpty()) {
            return order; // Обработка пустого графа
        }

        // Инициализация меток и непосещённых вершин
        Map<Integer, String> labels = new HashMap<>();
        Set<Integer> unvisited = new HashSet<>();
        for (Vertex v : vertices) {
            int vId = v.getId();
            labels.put(vId, ""); // Пустая строка как начальная метка
            unvisited.add(vId);
        }

        // Обработка вершин
        for (int i = 0; i < vertices.size(); i++) {
            // Выбор вершины с лексикографически наибольшей меткой
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

            // Добавление в порядок и удаление из непосещённых
            order.add(maxVertex);
            unvisited.remove(maxVertex);
            System.out.println("Выбрана вершина: " + maxVertex + ", Порядок: " + order);

            // Обновление меток соседей
            for (int u : unvisited) {
                if (adjacencyMap.get(maxVertex).contains(u)) {
                    labels.put(u, (vertices.size() - i) + "," + labels.get(u));
                }
            }
        }

        System.out.println("Порядок Lex-BFS: " + order);
        return order;
    }

    boolean isPerfectEliminationOrder(Graph graph, List<Integer> order, Map<Integer, Set<Integer>> adjacencyMap) {
        int n = graph.getVertexList().size();
        for (int i = 0; i < n; i++) {
            int v = order.get(i);
            List<Integer> rightNeighbors = new ArrayList<>();
            for (int j = i + 1; j < n; j++) {
                int u = order.get(j);
                if (adjacencyMap.get(v).contains(u)) {
                    rightNeighbors.add(u);
                }
            }
            System.out.println("Вершина " + v + " правые соседи: " + rightNeighbors);
            if (rightNeighbors.size() <= 1) continue; // Одна или ноль вершин — тривиально клика
            // Проверяем, что правые соседи образуют клику
            for (int j = 0; j < rightNeighbors.size(); j++) {
                for (int k = j + 1; k < rightNeighbors.size(); k++) {
                    int u1 = rightNeighbors.get(j);
                    int u2 = rightNeighbors.get(k);
                    if (!adjacencyMap.get(u1).contains(u2)) {
                        // Проверяем наличие C4
                        if (checkForInducedC4(graph, v, u1, u2, adjacencyMap)) {
                            System.out.println("Найден C4 с вершинами " + v + ", " + u1 + ", " + u2);
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    boolean checkForInducedC4(Graph graph, int v, int u1, int u2, Map<Integer, Set<Integer>> adjacencyMap) {
        for (Vertex wVertex : graph.getVertexList()) {
            int w = wVertex.getId();
            if (w == v || w == u1 || w == u2) continue;
            Set<Integer> wNeighbors = adjacencyMap.get(w);
            // Проверяем, образует ли w цикл C4: v -> u1 -> w -> u2 -> v
            if (wNeighbors.contains(u1) && wNeighbors.contains(u2) && !wNeighbors.contains(v)) {
                // Проверяем, что нет дополнительных рёбер (например, v-w или u1-u2)
                if (!adjacencyMap.get(u1).contains(u2)) {
                    return true; // Найден C4
                }
            }
        }
        return false;
    }

    List<Set<Integer>> computeMaximalCliques(Graph graph, List<Integer> order, Map<Integer, Set<Integer>> adjacencyMap) {
        List<Set<Integer>> candidateCliques = new ArrayList<>();
        int n = graph.getVertexList().size();

        for (int i = 0; i < n; i++) {
            int v = order.get(i);
            Set<Integer> clique = new HashSet<>();
            clique.add(v);
            Set<Integer> candidates = new HashSet<>();
            for (int j = i + 1; j < n; j++) {
                int u = order.get(j);
                if (adjacencyMap.get(v).contains(u)) {
                    candidates.add(u);
                }
            }
            generateCliques(clique, candidates, candidateCliques, adjacencyMap);
        }

        List<Set<Integer>> maximalCliques = new ArrayList<>();
        for (Set<Integer> clique : candidateCliques) {
            boolean isMaximal = true;
            for (Vertex vertex : graph.getVertexList()) {
                int w = vertex.getId();
                if (!clique.contains(w)) {
                    boolean canExtend = true;
                    for (int u : clique) {
                        if (!adjacencyMap.get(w).contains(u)) {
                            canExtend = false;
                            break;
                        }
                    }
                    if (canExtend) {
                        isMaximal = false;
                        break;
                    }
                }
            }
            if (isMaximal && !maximalCliques.contains(clique)) {
                maximalCliques.add(clique);
            }
        }

        System.out.println("Кандидаты на клики: " + candidateCliques);
        System.out.println("Максимальные клики после фильтрации: " + maximalCliques);
        return maximalCliques;
    }

    void generateCliques(Set<Integer> clique, Set<Integer> candidates, List<Set<Integer>> candidateCliques, Map<Integer, Set<Integer>> adjacencyMap) {
        boolean isValidClique = true;
        for (int u : clique) {
            for (int v : clique) {
                if (u != v && !adjacencyMap.get(u).contains(v)) {
                    isValidClique = false;
                    break;
                }
            }
            if (!isValidClique) break;
        }
        if (isValidClique && !candidateCliques.contains(clique)) {
            candidateCliques.add(new HashSet<>(clique));
        } else {
            return;
        }

        for (int u : new HashSet<>(candidates)) {
            Set<Integer> newClique = new HashSet<>(clique);
            newClique.add(u);
            Set<Integer> newCandidates = new HashSet<>();
            for (int w : candidates) {
                if (w != u && adjacencyMap.get(u).contains(w)) {
                    newCandidates.add(w);
                }
            }
            generateCliques(newClique, newCandidates, candidateCliques, adjacencyMap);
        }
    }

    List<Set<Integer>> orderCliquesWithLexBFS(List<Set<Integer>> cliques) {
        if (cliques.isEmpty()) return new ArrayList<>();

        List<List<Integer>> cliqueGraph = new ArrayList<>();
        for (int i = 0; i < cliques.size(); i++) {
            cliqueGraph.add(new ArrayList<>());
        }
        for (int i = 0; i < cliques.size(); i++) {
            for (int j = i + 1; j < cliques.size(); j++) {
                Set<Integer> intersection = new HashSet<>(cliques.get(i));
                intersection.retainAll(cliques.get(j));
                if (!intersection.isEmpty()) {
                    cliqueGraph.get(i).add(j);
                    cliqueGraph.get(j).add(i);
                }
            }
        }
        System.out.println("Граф клик: " + cliqueGraph);

        int startClique = 0;
        int earliestVertex = Integer.MAX_VALUE;
        for (int i = 0; i < cliques.size(); i++) {
            for (int v : cliques.get(i)) {
                if (v < earliestVertex) {
                    earliestVertex = v;
                    startClique = i;
                }
            }
        }

        List<Set<Integer>> orderedCliques = new ArrayList<>();
        boolean[] visited = new boolean[cliques.size()];
        visited[startClique] = true;
        orderedCliques.add(cliques.get(startClique));

        Queue<Integer> queue = new LinkedList<>();
        queue.add(startClique);

        while (!queue.isEmpty()) {
            int currentCliqueIndex = queue.poll();
            Set<Integer> currentClique = cliques.get(currentCliqueIndex);

            List<Integer> neighbors = new ArrayList<>();
            for (int neighbor : cliqueGraph.get(currentCliqueIndex)) {
                if (!visited[neighbor]) {
                    neighbors.add(neighbor);
                }
            }

            neighbors.sort((a, b) -> {
                Set<Integer> intersectionA = new HashSet<>(cliques.get(a));
                intersectionA.retainAll(currentClique);
                Set<Integer> intersectionB = new HashSet<>(cliques.get(b));
                intersectionB.retainAll(currentClique);
                int minVertexA = intersectionA.stream().min(Integer::compare).orElse(Integer.MAX_VALUE);
                int minVertexB = intersectionB.stream().min(Integer::compare).orElse(Integer.MAX_VALUE);
                return Integer.compare(minVertexA, minVertexB);
            });

            for (int neighbor : neighbors) {
                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    orderedCliques.add(cliques.get(neighbor));
                    queue.add(neighbor);
                }
            }
        }

        System.out.println("Упорядоченные клики: " + orderedCliques);
        return orderedCliques;
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
}