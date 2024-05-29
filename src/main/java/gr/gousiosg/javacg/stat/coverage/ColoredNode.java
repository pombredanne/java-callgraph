package gr.gousiosg.javacg.stat.coverage;

import java.io.Serializable;

public class ColoredNode implements Serializable {

    /* Colors */
    private static final String IMPLIED_COVERAGE_COLOR = "skyblue";
    private static final String LIGHT_GREEN = "greenyellow";
    private static final String MEDIUM_GREEN = "green1";
    private static final String MEDIUM_DARK_GREEN = "green3";
    private static final String DARK_GREEN = "green4";
    private static final String FIREBRICK = "lightpink";
    private static final String ENTRYPOINT_COLOR = "lightgoldenrod";
    private static final String NO_COLOR = "ghostwhite";
    private static final String TEST_NODE_COLOR = "plum";

    private final String label;
    private String color = NO_COLOR;
    private boolean excluded = false;
    private boolean covered = false;
    private int linesCovered = 0;
    private int linesMissed = 0;
    private int branchesCovered = 0;
    private int branchesMissed = 0;

    public ColoredNode(String label) {
        this.label = label;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getLabel() {
        return this.label;
    }

    public void mark(Report.Package.Class.Method method) {

        /* Apply coverage to node */
        for (Report.Package.Class.Method.Counter counter : method.getCounter()) {
            switch (counter.getType()) {
                case JacocoCoverage.METHOD_TYPE: {
                    this.covered = counter.getCovered() > 0;
                    break;
                }
                case JacocoCoverage.LINE_TYPE: {
                    this.linesMissed = counter.getMissed();
                    this.linesCovered = counter.getCovered();
                    break;
                }
                case JacocoCoverage.BRANCH_TYPE: {
                    this.branchesMissed += counter.getMissed();
                    this.branchesCovered += counter.getCovered();
                    break;
                }
                default:
            }
        }

        chooseColor();
    }

    private void chooseColor() {
        if (!this.color.equals(ENTRYPOINT_COLOR)) {
            float lineRatio = lineRatio();
            if (lineRatio > 0.75) {
                this.color = DARK_GREEN;
            } else if (lineRatio > 0.5) {
                this.color = MEDIUM_DARK_GREEN;
            } else if (lineRatio > 0.25) {
                this.color = MEDIUM_GREEN;
            } else if (lineRatio > 0.02) {
                this.color = LIGHT_GREEN;
            } else {
                this.color = FIREBRICK;
            }
        }
    }

    public void markImpliedCoverage() {
        this.covered = true;
        this.color = IMPLIED_COVERAGE_COLOR;
    }

    public void markMissing() {
        if (!covered && !(this.color.equals(ENTRYPOINT_COLOR))) this.color = FIREBRICK;
    }

    public boolean covered() {
        return this.covered;
    }

    public void markEntryPoint() {
        this.color = ENTRYPOINT_COLOR;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof ColoredNode)) return false;
        ColoredNode node = (ColoredNode) obj;
        return node.label.equals(this.label);
    }

    public boolean isExcluded() {
        return excluded;
    }

    public void setExcluded(boolean excluded) {
        this.excluded = excluded;
        if (excluded) {
            this.color = TEST_NODE_COLOR;
        } else {
            chooseColor();
        }
    }

    public int getLinesCovered() {
        return linesCovered;
    }

    public int getLinesMissed() {
        return linesMissed;
    }

    public int getBranchesCovered() {
        return branchesCovered;
    }

    public int getBranchesMissed() {
        return branchesMissed;
    }

    public float lineRatio() {
        return (float) linesCovered / ((float) linesCovered + (float) linesMissed);
    }

    public String toString() {
        return getLabel();
    }
}
