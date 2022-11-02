import sys
import subprocess

JAR_FILE = "target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar"

def test_properties(project_name: str):
    project_graph = project_name + "_graph" if project_name != "convex" else project_name + "-core_graph"
    subprocess.run(["java", "-jar", JAR_FILE, "test", "-c",
                     project_name, "-f", project_graph])

def test_fixed_properties(project_name: str):
    project_fixed_name = project_name + "-fixed"
    project_graph = project_fixed_name + "_graph" if project_name != "convex" else project_name + "-core-fixed_graph"
    subprocess.run(["java", "-jar", JAR_FILE, "test", "-c",
                     project_fixed_name, "-f", project_graph])


def main():
    if not sys.argv[1]:
        raise Exception("Must specify project name through command line param!")
    project_name = sys.argv[1]
    for index in range(10):
        test_properties(project_name)
    for index in range(10):
        test_fixed_properties(project_name)

if __name__ == "__main__":
    main()