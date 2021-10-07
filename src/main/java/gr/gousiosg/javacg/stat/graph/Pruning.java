package gr.gousiosg.javacg.stat.graph;

import gr.gousiosg.javacg.stat.coverage.JacocoCoverage;
import gr.gousiosg.javacg.stat.support.JarMetadata;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class Pruning {
  private static final Logger LOGGER = LoggerFactory.getLogger(Pruning.class);

  /**
   * Remove all bridge / synthetic methods that were created during type erasure See
   * https://docs.oracle.com/javase/tutorial/java/generics/bridgeMethods.html for more information.
   *
   * @param graph the graph
   * @param metadata the metadata of the graph
   */
  public static void pruneBridgeMethods(Graph<String, DefaultEdge> graph, JarMetadata metadata) {
    metadata
        .getBridgeMethods()
        .forEach(
            bridgeNode -> {
              /* Fetch the bridge method and make sure it has exactly one outgoing edge */
              Optional<DefaultEdge> maybeEdge =
                  graph.outgoingEdgesOf(bridgeNode).stream().findFirst();

              if (graph.outDegreeOf(bridgeNode) != 1 || maybeEdge.isEmpty()) {
                /* announce the violator */
                LOGGER.error(
                    "Found a bridge method that doesn't have exactly 1 outgoing edge: "
                        + bridgeNode
                        + " : "
                        + graph.outDegreeOf(bridgeNode));
                /* announce the violator's connections */
                graph
                    .outgoingEdgesOf(bridgeNode)
                    .forEach(
                        e -> {
                          LOGGER.error(
                              "\t" + graph.getEdgeSource(e) + " -> " + graph.getEdgeTarget(e));
                        });
                System.exit(1);
              }

              /* Fetch the bridge method's target */
              String bridgeTarget = graph.getEdgeTarget(maybeEdge.get());

              /* Redirect all edges from the bridge method to its target */
              graph
                  .incomingEdgesOf(bridgeNode)
                  .forEach(
                      edge -> {
                        String sourceNode = graph.getEdgeSource(edge);
                        graph.addEdge(sourceNode, bridgeTarget);
                      });

              /* Remove the bridge method from the graph */
              graph.removeVertex(bridgeNode);
            });
  }

  /**
   * Remove all unused concrete method calls that are present in the graph
   *
   * <p>This technique helps us reduce the over-approximation incurred by method expansion.
   *
   * @param graph the graph
   * @param metadata the metadata of the graph
   */
  public static void pruneConcreteMethods(
      Graph<String, DefaultEdge> graph, JarMetadata metadata, JacocoCoverage coverage) {
    metadata.getConcreteMethods().stream()
        .filter(concreteMethod -> !coverage.hasNonzeroCoverage(concreteMethod))
        .forEach(graph::removeVertex);
  }
}
