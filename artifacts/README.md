## How to use the run configuration:

Look through the configuration file `config.yaml` and replace each attribute with the attributes corresponding to your
project.

Let's use [java-callgraph-driver](https://github.com/wcygan/java-callgraph-driver) as an example.

We specify that we'd like to download the project by using:

```yaml
repository-url: "git@github.com:wcygan/java-callgraph-driver.git"
```

We need to give java-callgraph a hint for how to build the project:

```yaml
build-system: "maven"
build-command: "mvn install"
```

We need to specify where to find certain files once the project is built:

```yaml
target-jar-location: "/target/java-callgraph-driver-1.0-SNAPSHOT.jar"
coverage-location: "/target/site/jacoco/jacoco.xml"
```

Additionally, we can specify the arguments for java-callgraph like so:

```yaml
entrypoint: "\"edu.uic.cs398.Main.main([Ljava/lang/String;)V\""
depth: "5"
output-name: "example"
ancestry: "2"
```

Once we have the required fields specified, we can run execute the run script like so:

```shell
python3 run.py
```

Please note that this script depends on [PyYAML](https://pypi.org/project/pyaml/) which can be installed with
pip: `pip install pyaml`.