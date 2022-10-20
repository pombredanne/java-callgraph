import sys
import subprocess

JAR_FILE = "target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar"

def test_properties():
    subprocess.run(["java", "-jar", JAR_FILE, "test", "-c",
                     "jflex", "-f", "jflex_graph"])

def test_fixed_properties():
    subprocess.run(["java", "-jar", JAR_FILE, "test", "-c",
                     "jflex-fixed", "-f", "jflex_fixed_graph"])


def main():
    # for index in range(10):
    #     test_properties()
    for index in range(10):
        test_fixed_properties()

if __name__ == "__main__":
    main()