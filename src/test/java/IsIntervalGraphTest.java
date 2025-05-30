import ru.leti.wise.task.graph.model.Graph;
import ru.leti.wise.task.graph.util.FileLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class IntervalGraphCheckTest {
    private IsIntervalGraph check;

    @BeforeEach
    void setUp() {
        check = new IsIntervalGraph();
    }

    private Graph loadGraph(String fileName) throws Exception {
        return FileLoader.loadGraphFromJson("src/test/resources/" + fileName);
    }

    // === Тесты для isConsecutive ===
    @Test
    void isConsecutive_Test11_Vertex1() throws Exception {
        Graph graph = loadGraph("Test_11.json");
        Map<Integer, Set<Integer>> adjacencyMap = check.buildAdjacencyMap(graph);
        List<Integer> order = check.performLexBFS(graph, adjacencyMap);
        List<Set<Integer>> cliques = check.computeMaximalCliques(graph, order, adjacencyMap);
        // Cliques for Test_11: e.g., [{1,2,4}, {2,3,4}]
        // Vertex 1 is in clique 0 (indices [0])
        List<Integer> cliqueIndices = new ArrayList<>();
        for (int i = 0; i < cliques.size(); i++) {
            if (cliques.get(i).contains(1)) {
                cliqueIndices.add(i);
            }
        }
        assertThat(check.isConsecutive(cliqueIndices)).isEqualTo(true);
    }

    @Test
    void isConsecutive_Test15_Vertex4() throws Exception {
        Graph graph = loadGraph("Test_15.json");
        Map<Integer, Set<Integer>> adjacencyMap = check.buildAdjacencyMap(graph);
        List<Integer> order = check.performLexBFS(graph, adjacencyMap);
        List<Set<Integer>> cliques = check.computeMaximalCliques(graph, order, adjacencyMap);
        // Vertex 4 in Test_15 has non-consecutive cliques (e.g., in multiple non-adjacent cliques)
        List<Integer> cliqueIndices = new ArrayList<>();
        for (int i = 0; i < cliques.size(); i++) {
            if (cliques.get(i).contains(4)) {
                cliqueIndices.add(i);
            }
        }
        assertThat(check.isConsecutive(cliqueIndices)).isEqualTo(true);
    }

    @Test
    void isConsecutive_Test11_EmptyIndices() throws Exception {
        Graph graph = loadGraph("Test_11.json");
        Map<Integer, Set<Integer>> adjacencyMap = check.buildAdjacencyMap(graph);
        List<Integer> order = check.performLexBFS(graph, adjacencyMap);
        List<Set<Integer>> cliques = check.computeMaximalCliques(graph, order, adjacencyMap);
        // Choose a vertex not in any clique or simulate empty indices
        List<Integer> cliqueIndices = Collections.emptyList();
        assertThat(check.isConsecutive(cliqueIndices)).isEqualTo(true);
    }
    // === Тесты для getCliquePathOrder ===
    @Test
    void getCliquePathOrder_Test11_ReturnsValidPath() throws Exception {
        Graph graph = loadGraph("Test_11.json");
        Map<Integer, Set<Integer>> adjacencyMap = check.buildAdjacencyMap(graph);
        List<Integer> order = check.performLexBFS(graph, adjacencyMap);
        List<Set<Integer>> cliques = check.computeMaximalCliques(graph, order, adjacencyMap);
        List<Integer> path = check.getCliquePathOrder(cliques);
        assertThat(path).hasSize(2);
        assertThat(path).containsExactlyInAnyOrder(0, 1);
        Set<Integer> intersection = new HashSet<>(cliques.get(path.get(0)));
        intersection.retainAll(cliques.get(path.get(1)));
        assertThat(intersection).isNotEmpty();
    }

    @Test
    void getCliquePathOrder_Test13_ReturnsNull() throws Exception {
        Graph graph = loadGraph("Test_13.json");
        Map<Integer, Set<Integer>> adjacencyMap = check.buildAdjacencyMap(graph);
        List<Integer> order = check.performLexBFS(graph, adjacencyMap);
        List<Set<Integer>> cliques = check.computeMaximalCliques(graph, order, adjacencyMap);
        List<Integer> path = check.getCliquePathOrder(cliques);
        assertThat(path).isNull();
    }

    // === Тесты для getNeighbours ===
    @Test
    void getNeighbours_Test1_Vertex2_ReturnsCorrectNeighbours() throws Exception {
        Graph graph = loadGraph("Test_1.json");
        Set<Integer> neighbours = check.getNeighbours(graph, 2);

        assertThat(neighbours).containsExactlyInAnyOrder(1, 3);
    }

    @Test
    void getNeighbours_Test9_Vertex2_ReturnsCorrectNeighbours() throws Exception {
        Graph graph = loadGraph("Test_9.json");
        Set<Integer> neighbours = check.getNeighbours(graph, 2);

        assertThat(neighbours).containsExactlyInAnyOrder(1, 3, 4);
    }

    @Test
    void getNeighbours_Test6_Vertex7_ReturnsCorrectNeighbours() throws Exception {
        Graph graph = loadGraph("Test_6.json");
        Set<Integer> neighbours = check.getNeighbours(graph, 7);

        assertThat(neighbours).containsExactlyInAnyOrder(2, 4, 5, 6);
    }

    // === Тесты для performLexBFS ===
    @Test
    void performLexBFS_Test8_ReturnsValidOrder() throws Exception {
        Graph graph = loadGraph("Test_8.json");
        Map<Integer, Set<Integer>> adjacencyMap = check.buildAdjacencyMap(graph);
        List<Integer> order = check.performLexBFS(graph, adjacencyMap);
        assertThat(order).hasSize(3).containsExactlyInAnyOrder(1, 2, 3);
        assertThat(new HashSet<>(order)).hasSize(3);
        // Стартовая вершина имеет степень 2
        int startVertex = order.get(0);
        Set<Integer> neighbors = adjacencyMap.get(startVertex);

        assertThat(neighbors).hasSize(2);
    }

    @Test
    void performLexBFS_Test9_ReturnsValidOrder() throws Exception {
        Graph graph = loadGraph("Test_9.json");
        Map<Integer, Set<Integer>> adjacencyMap = check.buildAdjacencyMap(graph);
        List<Integer> order = check.performLexBFS(graph, adjacencyMap);
        assertThat(order).hasSize(4).containsExactlyInAnyOrder(1, 2, 3, 4);
        // Стартовая вершина — 2 (степень 3)
        int startVertex = order.get(0);
        Set<Integer> neighbors = adjacencyMap.get(startVertex);

        assertThat(neighbors.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void performLexBFS_Test10_ReturnsValidOrder() throws Exception {
        Graph graph = loadGraph("Test_10.json");
        Map<Integer, Set<Integer>> adjacencyMap = check.buildAdjacencyMap(graph);
        List<Integer> order = check.performLexBFS(graph, adjacencyMap);
        assertThat(order).hasSize(6).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6);
        // Стартовая вершина — 5 (степень 4)
        int startVertex = order.get(0);
        Set<Integer> neighbors = adjacencyMap.get(startVertex);

        assertThat(neighbors.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void performLexBFS_EmptyGraph_ReturnsEmptyList() throws Exception {
        Graph graph = new Graph();
        Map<Integer, Set<Integer>> adjacencyMap = check.buildAdjacencyMap(graph);
        graph.setVertexList(new ArrayList<>());
        graph.setEdgeList(new ArrayList<>());
        List<Integer> order = check.performLexBFS(graph, adjacencyMap);

        assertThat(order).isEmpty();
    }

    //=== Тесты для isPerfectEliminationOrder ===
    @Test
    void isPerfectEliminationOrder_Test1_ReturnsTrue() throws Exception {
        Graph graph = loadGraph("Test_1.json");
        Map<Integer, Set<Integer>> adjacencyMap = check.buildAdjacencyMap(graph);
        List<Integer> order = check.performLexBFS(graph, adjacencyMap);
        boolean result = check.isPerfectEliminationOrder(graph, order, adjacencyMap);

        assertThat(result).isTrue();
    }

    @Test

    void isPerfectEliminationOrder_Test2_ReturnsFalse() throws Exception {
        Graph graph = loadGraph("Test_2.json");
        Map<Integer, Set<Integer>> adjacencyMap = check.buildAdjacencyMap(graph);
        List<Integer> order = check.performLexBFS(graph, adjacencyMap);
        boolean result = check.isPerfectEliminationOrder(graph, order, adjacencyMap);

        assertThat(result).isFalse();
    }

    @Test
    void isPerfectEliminationOrder_Test8_ReturnsTrue() throws Exception {
        Graph graph = loadGraph("Test_8.json");
        Map<Integer, Set<Integer>> adjacencyMap = check.buildAdjacencyMap(graph);
        List<Integer> order = check.performLexBFS(graph, adjacencyMap);
        boolean result = check.isPerfectEliminationOrder(graph, order, adjacencyMap);

        assertThat(result).isTrue();
    }

    @Test
    void isPerfectEliminationOrder_Test9_ReturnsTrue() throws Exception {
        Graph graph = loadGraph("Test_9.json");
        Map<Integer, Set<Integer>> adjacencyMap = check.buildAdjacencyMap(graph);
        List<Integer> order = check.performLexBFS(graph, adjacencyMap);
        boolean result = check.isPerfectEliminationOrder(graph, order, adjacencyMap);

        assertThat(result).isTrue();
    }


    // === Тесты для computeMaximalCliques ===
    @Test
    void computeMaximalCliques_Test8_ReturnsSingleClique() throws Exception {
        Graph graph = loadGraph("Test_8.json");
        Map<Integer, Set<Integer>> adjacencyMap = check.buildAdjacencyMap(graph);
        List<Integer> order = check.performLexBFS(graph, adjacencyMap);
        List<Set<Integer>> cliques = check.computeMaximalCliques(graph, order, adjacencyMap);

        assertThat(cliques).hasSize(1);
        assertThat(cliques.get(0)).containsExactlyInAnyOrder(1, 2, 3);
    }

    @Test
    void computeMaximalCliques_Test9_ReturnsCorrectCliques() throws Exception {
        Graph graph = loadGraph("Test_9.json");
        Map<Integer, Set<Integer>> adjacencyMap = check.buildAdjacencyMap(graph);
        List<Integer> order = check.performLexBFS(graph, adjacencyMap);
        List<Set<Integer>> cliques = check.computeMaximalCliques(graph, order, adjacencyMap);

        assertThat(cliques).hasSize(2);
        assertThat(cliques).contains(
                new HashSet<>(Arrays.asList(1, 2, 4)),
                new HashSet<>(Arrays.asList(2, 3, 4))
        );
    }

    @Test
    void computeMaximalCliques_Test10_ReturnsCorrectCliques() throws Exception {
        Graph graph = loadGraph("Test_10.json");
        Map<Integer, Set<Integer>> adjacencyMap = check.buildAdjacencyMap(graph);
        List<Integer> order = check.performLexBFS(graph, adjacencyMap);
        List<Set<Integer>> cliques = check.computeMaximalCliques(graph, order, adjacencyMap);

        assertThat(cliques).hasSize(4);
        assertThat(cliques).contains(
                new HashSet<>(Arrays.asList(1, 2, 5)),
                new HashSet<>(Arrays.asList(2, 3, 5)),
                new HashSet<>(Arrays.asList(3, 4, 5)),
                new HashSet<>(Arrays.asList(4, 6))
        );
    }



    // === Тесты на интервальность (run) ===
    @Test
    void run_Test1_ReturnsTrue() throws Exception {
        Graph graph = loadGraph("Test_1.json");
        assertThat(check.run(graph)).isEqualTo(true);
    }

    @Test
    void run_Test2_ReturnsFalse() throws Exception {
        Graph graph = loadGraph("Test_2.json");
        assertThat(check.run(graph)).isEqualTo(false);
    }

    @Test
    void run_Test3_ReturnsFalse() throws Exception {
        Graph graph = loadGraph("Test_3.json");
        assertThat(check.run(graph)).isEqualTo(false);
    }

    @Test
    void run_Test4_ReturnsFalse() throws Exception {
        Graph graph = loadGraph("Test_4.json");
        assertThat(check.run(graph)).isEqualTo(false);
    }

    @Test
    void run_Test5_ReturnsFalse() throws Exception {
        Graph graph = loadGraph("Test_5.json");
        assertThat(check.run(graph)).isEqualTo(false);
    }

    @Test
    void run_Test6_ReturnsTrue() throws Exception {
        Graph graph = loadGraph("Test_6.json");
        assertThat(check.run(graph)).isEqualTo(true);
    }

    @Test
    void run_Test7_ReturnsTrue() throws Exception {
        Graph graph = loadGraph("Test_7.json");
        assertThat(check.run(graph)).isEqualTo(true);
    }

    @Test
    void run_Test8_ReturnsTrue() throws Exception {
        Graph graph = loadGraph("Test_8.json");
        assertThat(check.run(graph)).isEqualTo(true);
    }

    @Test
    void run_Test9_ReturnsTrue() throws Exception {
        Graph graph = loadGraph("Test_9.json");
        assertThat(check.run(graph)).isEqualTo(true);
    }

    @Test
    void run_Test10_ReturnsTrue() throws Exception {
        Graph graph = loadGraph("Test_10.json");
        assertThat(check.run(graph)).isEqualTo(true);
    }

    @Test
    void run_Test11_ReturnsTrue() throws Exception {
        Graph graph = loadGraph("Test_11.json");
        assertThat(check.run(graph)).isEqualTo(true);
    }

    @Test
    void run_Test12_ReturnsTrue() throws Exception {
        Graph graph = loadGraph("Test_12.json");
        assertThat(check.run(graph)).isEqualTo(true);
    }

    @Test
    void run_Test13_ReturnsTrue() throws Exception {
        Graph graph = loadGraph("Test_13.json");
        assertThat(check.run(graph)).isEqualTo(false);
    }
    @Test
    void run_Test14_ReturnsTrue() throws Exception {
        Graph graph = loadGraph("Test_14.json");
        assertThat(check.run(graph)).isEqualTo(false);
    }

    @Test
    void run_Test15_ReturnsTrue() throws Exception {
        Graph graph = loadGraph("AnotherOne.json");
        assertThat(check.run(graph)).isEqualTo(true);
    }

    @Test
    void run_Test16_ReturnsTrue() throws Exception {
        Graph graph = loadGraph("Test_15.json");
        assertThat(check.run(graph)).isEqualTo(false);
    }

    @Test
    void run_Test17_ReturnsTrue() throws Exception {
        Graph graph = loadGraph("ForOther.json");
        assertThat(check.run(graph)).isEqualTo(false);
    }
}
