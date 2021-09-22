package gr.gousiosg.javacg.stat.graph;

import gr.gousiosg.javacg.stat.coverage.ColoredNode;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class Reachability {

  private static final Logger LOGGER = LoggerFactory.getLogger(Reachability.class);

  /**
   * Computes the reachability subgraph from a parent graph, entrypoint, and an optional depth to search
   * @param graph the parent {@link Graph}
   * @param entrypoint the root node of the reachability subgraph
   * @param maybeMaximumDepth the depth to traverse (e.g., all nodes reachable within N steps from the root)
   * @return a subgraph containing all reachable nodes as described
   */
  public static Graph<ColoredNode, DefaultEdge> compute(Graph<String, DefaultEdge> graph, String entrypoint, Optional<Integer> maybeMaximumDepth) {

    if (!graph.containsVertex(entrypoint)) {
      LOGGER.error("---> " + entrypoint + "<---");
      LOGGER.error("The graph doesn't contain the vertex specified as the entry point!");
      throw new InputMismatchException("graph doesn't contain vertex " + entrypoint);
    }

    if (maybeMaximumDepth.isPresent() && (maybeMaximumDepth.get() < 0)) {
      LOGGER.error("Depth " + maybeMaximumDepth.get() + " must be greater than 0!");
      System.exit(1);
    }

    LOGGER.info("Starting reachability at entry point: " + entrypoint);
    maybeMaximumDepth.ifPresent(d -> LOGGER.info("Traversing to depth " + d));

    Graph<ColoredNode, DefaultEdge> subgraph = new DefaultDirectedGraph<>(DefaultEdge.class);
    int currentDepth = 0;

    Deque<String> reachable = new ArrayDeque<>();
    reachable.push(entrypoint);

    Map<String, ColoredNode> subgraphNodes = new HashMap<>();
    Set<String> seenBefore = new HashSet<>();
    Set<String> nextLevel = new HashSet<>();

    while (!reachable.isEmpty()) {

      /* Stop once we've surpassed maximum depth */
      if (maybeMaximumDepth.isPresent() && (maybeMaximumDepth.get() < currentDepth)) {
        break;
      }

      while (!reachable.isEmpty()) {
        /* Visit reachable node */
        String source = reachable.pop();
        ColoredNode sourceNode = subgraphNodes.containsKey(source) ? subgraphNodes.get(source) : new ColoredNode(source);

        /* Keep track of who we've visited */
        seenBefore.add(source);
        if (!subgraphNodes.containsKey(source)) {
          subgraph.addVertex(sourceNode);
          subgraphNodes.put(source, sourceNode);
        }

        /* Check if we can add deeper edges or not */
        if (maybeMaximumDepth.isPresent() && (maybeMaximumDepth.get() == currentDepth)) {
          break;
        }

        graph.edgesOf(source).forEach(edge -> {
          String target = graph.getEdgeTarget(edge);
          ColoredNode targetNode = subgraphNodes.containsKey(target) ? subgraphNodes.get(target) : new ColoredNode(target);

          if (!subgraphNodes.containsKey(target)) {
            subgraphNodes.put(target, targetNode);
            subgraph.addVertex(targetNode);
          }

          if (graph.containsEdge(source, target) && !subgraph.containsEdge(sourceNode, targetNode)) {
            subgraph.addEdge(sourceNode, targetNode);
          }

          /* Have we visited this vertex before? */
          if (!seenBefore.contains(target)) {
            nextLevel.add(target);
            seenBefore.add(target);
          }

        });
      }

      currentDepth++;
      reachable.addAll(nextLevel);
      nextLevel.clear();
    }

    subgraphNodes.get(entrypoint).markEntryPoint();
    return subgraph;
  }
}
