package gr.gousiosg.javacg.stat.support.coloring;

public class ColoredNode {

    private static final String GREEN = "darkolivegreen1";
    public static String NO_COLOR = "ghostwhite";
    private final String label;
    private String color = NO_COLOR;

    public ColoredNode(String label) {
        this.label = label;
    }

    public String getColor() {
        return color;
    }

    public String getLabel() { return this.label; }

    public void mark() {
        this.color = GREEN;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof ColoredNode)) return false;
        ColoredNode node = (ColoredNode) obj;
        return node.label.equals(this.label);
    }

}
