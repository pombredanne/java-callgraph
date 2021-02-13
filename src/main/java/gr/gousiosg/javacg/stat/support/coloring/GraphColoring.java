package gr.gousiosg.javacg.stat.support.coloring;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GraphColoring {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphColoring.class);

    public static void applyCoverage(Graph<ColoredNode, DefaultEdge> graph, Set<String> coverage) {
        Map<String, ColoredNode> nodeMap = nodeMap(graph.vertexSet());
        coverage.forEach(cover -> {
                    if (nodeMap.containsKey(cover)) {
                        nodeMap.get(cover).mark();
                    }
                });
    }

    private static Map<String, ColoredNode> nodeMap(Set<ColoredNode> nodes) {
        return nodes.stream()
                .collect(Collectors.toMap(ColoredNode::getLabel, node -> node));
    }

}
