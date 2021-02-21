package gr.gousiosg.javacg.stat.support.coloring;

import gr.gousiosg.javacg.stat.JCallGraph;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

public class CoverageStatistics {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoverageStatistics.class);
    private static final String SEPARATOR = "######################";

    @Writeable private final long nodeCount;
    @Writeable private final long coloredNodeCount;
    @Writeable private final long edgeCount;
    @Writeable private final float percentCoverage;

    private CoverageStatistics(Graph<ColoredNode, DefaultEdge> graph) {
        this.nodeCount = graph.vertexSet().size();
        this.coloredNodeCount = graph.vertexSet().stream().filter(ColoredNode::marked).count();
        this.edgeCount = graph.edgeSet().size();
        this.percentCoverage = ((float) coloredNodeCount / nodeCount);
    }

    public static void analyze(Graph<ColoredNode, DefaultEdge> graph, Optional<String> outputName) {
        CoverageStatistics statistics = new CoverageStatistics(graph);
        statistics.announce();
        if (outputName.isPresent()) {
            try {
                toCsv(statistics, outputName.get());
            } catch (Exception e) {
                LOGGER.error("Error writing " + outputName.get() + " to CSV!");
            }
        }
    }

    private void announce() {
        LOGGER.info(SEPARATOR);
        LOGGER.info("Coverage Statistics:");
        LOGGER.info("Node Count:     " + this.nodeCount);
        LOGGER.info("Nodes Covered:  " + this.coloredNodeCount);
        LOGGER.info("Edge Count:     " + this.edgeCount);
        LOGGER.info("Coverage:       " + String.format("%.2f", this.percentCoverage * 100) + "%");
        LOGGER.info(SEPARATOR);
    }

    private static void toCsv(CoverageStatistics statistics, String fileName) throws Exception {
        if (statistics == null) return;
        FileWriter writer = new FileWriter(JCallGraph.OUTPUT_DIRECTORY + fileName);
        Arrays.stream(CoverageStatistics.class.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Writeable.class))
                .forEach(f -> {
                    try {
                        writer.write(f.getName() + "," + f.get(statistics) + "\n");
                    } catch (IllegalAccessException | IOException e) {
                        e.printStackTrace();
                        LOGGER.error("Unable to write statistics to " + fileName);
                    }
                });
        writer.close();
    }
}