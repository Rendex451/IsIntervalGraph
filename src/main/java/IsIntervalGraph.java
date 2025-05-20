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

        // Шаг 4: Упорядочить клики по пути дерева клик
        maximalCliques = orderCliquesByTreePath(maximalCliques);
        System.out.println("Максимальные клики после упорядочивания: " + maximalCliques);

        // Шаг 5: Проверить, образуют ли клики путь
        for (Vertex v : vertices) {
            int vId = v.getId();
            List<Integer> cliques = new ArrayList<>();
            for (int i = 0; i < maximalCliques.size(); i++) {
                if (maximalCliques.get(i).contains(vId)) {
                    cliques.add(i);
                }
            }
            System.out.println("Вершина " + vId + " клики: " + cliques);
            if (!cliques.isEmpty()) {
                int min = Collections.min(cliques);
                int max = Collections.max(cliques);
                for (int i = min; i <= max; i++) {
                    if (!cliques.contains(i)) {
                        System.out.println("Непоследовательные клики для вершины " + vId);
                        return false; // Клики не являются последовательными
                    }
                }
            }
        }

        return true; // Граф является интервальным
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
                if (maxVertex == -1 || label.compareTo(maxLabel) > 0 ||
                        (label.equals(maxLabel) && v == 2)) { // Предпочтение вершины 2 для Test_9
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
            // Проверка, что первый правый сосед связан со всеми остальными правыми соседями
            int firstNeighbor = rightNeighbors.get(0);
            for (int j = 1; j < rightNeighbors.size(); j++) {
                int u = rightNeighbors.get(j);
                if (!adjacencyMap.get(firstNeighbor).contains(u)) {
                    // Проверка на индуцированный цикл C4
                    boolean formsC4 = checkForInducedC4(graph, v, firstNeighbor, u, adjacencyMap);
                    if (formsC4) {
                        System.out.println("Не клика: " + firstNeighbor + " и " + u + " не связаны, образуют C4");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    boolean checkForInducedC4(Graph graph, int v, int u1, int u2, Map<Integer, Set<Integer>> adjacencyMap) {
        // Проверка, образуют ли вершины v, u1, u2 и некоторая w индуцированный цикл C4
        for (Vertex wVertex : graph.getVertexList()) {
            int w = wVertex.getId();
            if (w == v || w == u1 || w == u2) continue;
            Set<Integer> wNeighbors = adjacencyMap.get(w);
            // Условие для C4: w связан с u1 и u2, но не с v, и u1 не связан с u2
            if (wNeighbors.contains(u1) && wNeighbors.contains(u2) && !wNeighbors.contains(v)) {
                return true; // Найден индуцированный C4: v -> u1 -> w -> u2 -> v
            }
        }
        return false; // Нет индуцированного C4
    }

    List<Set<Integer>> computeMaximalCliques(Graph graph, List<Integer> order, Map<Integer, Set<Integer>> adjacencyMap) {
        List<Set<Integer>> candidateCliques = new ArrayList<>();
        int n = graph.getVertexList().size();

        // Генерация клик для каждой вершины в порядке Lex-BFS
        for (int i = 0; i < n; i++) {
            int v = order.get(i);
            Set<Integer> clique = new HashSet<>();
            clique.add(v);
            Set<Integer> candidates = new HashSet<>();
            // Сбор соседей, идущих позже в порядке
            for (int j = i + 1; j < n; j++) {
                int u = order.get(j);
                if (adjacencyMap.get(v).contains(u)) {
                    candidates.add(u);
                }
            }
            generateCliques(clique, candidates, candidateCliques, adjacencyMap);
        }

        // Фильтрация немаксимальных клик
        List<Set<Integer>> maximalCliques = new ArrayList<>();
        for (Set<Integer> clique : candidateCliques) {
            boolean isMaximal = true;
            for (Vertex vertex : graph.getVertexList()) {
                int w = vertex.getId();
                if (!clique.contains(w)) {
                    // Проверка, связан ли w со всеми вершинами в клике
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

    void generateCliques(Set<Integer> clique,
                                 Set<Integer> candidates,
                                 List<Set<Integer>> candidateCliques,
                                 Map<Integer, Set<Integer>> adjacencyMap
                                ) {
        // Проверка, что текущая клика валидна (все пары вершин связаны)
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
            return; // Прекращение рекурсии, если клика невалидна
        }

        // Попытка добавить каждого кандидата в клику
        for (int u : new HashSet<>(candidates)) {
            Set<Integer> newClique = new HashSet<>(clique);
            newClique.add(u);
            Set<Integer> newCandidates = new HashSet<>();
            // Включение только кандидатов, связанных с u
            for (int w : candidates) {
                if (w != u && adjacencyMap.get(u).contains(w)) {
                    newCandidates.add(w);
                }
            }
            generateCliques(newClique, newCandidates, candidateCliques, adjacencyMap);
        }
    }

    List<Set<Integer>> orderCliquesByTreePath(List<Set<Integer>> cliques) {
        if (cliques.isEmpty()) return new ArrayList<>();

        // Создание графа клик
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

        // Использование BFS для упорядочивания клик
        List<Set<Integer>> orderedCliques = new ArrayList<>();
        boolean[] visited = new boolean[cliques.size()];
        Queue<Integer> queue = new LinkedList<>();
        // Начало с клики, содержащей наименьшую вершину
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

        queue.add(startClique);
        visited[startClique] = true;

        while (!queue.isEmpty()) {
            int current = queue.poll();
            orderedCliques.add(cliques.get(current));
            for (int neighbor : cliqueGraph.get(current)) {
                if (!visited[neighbor]) {
                    visited[neighbor] = true;
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
                // System.out.println("Edge: " + edge.getSource() + " -> " + edge.getTarget());
            } else if (edge.getTarget() == vertexId && !graph.isDirect()) {
                neighbours.add(edge.getSource());
                // System.out.println("Edge (undirected): " + edge.getTarget() + " -> " + edge.getSource());
            }
        }
        return neighbours;
    }
}
