# java-callgraph
Static Call Graph Generator for Java Projects

![alt text](ExampleData/example-reachability-4.png)

## Installation  
You must have [Java](https://docs.oracle.com/en/java/javase/11/install/overview-jdk-installation.html#GUID-8677A77F-231A-40F7-98B9-1FD0B48C346A) and [Maven](https://maven.apache.org/install.html) installed

```console
$ git clone git@github.com:wcygan/java-callgraph.git
$ cd java-callgraph
$ mvn install
```

This will produce a `target` directory with the following jar:
- `javacg-0.1-SNAPSHOT-jar-with-dependencies.jar`: This is an executable jar which includes the static call graph generator and all dependencies needed to run this program

## Usage

This program will generate a graph and save it to a file `<output-name>.dot` which you can use [Graphviz](https://www.graphviz.org/download/) to visualize.

A directed edge in the graph is denoted with two method signatures:

```
  class1:<method1>(arg_types) -> class2:<method2>(arg_types)
```

### Example 
You can test this program by running the following code in the root directory:

```
java -jar ./target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar -j ./ExampleData/java-callgraph-driver-1.0-SNAPSHOT.jar -c ./ExampleData/jacoco.xml -o example  -e "edu.uic.cs398.Main:main(java.lang.String[])" -d 4
```

### Options

There are command line options that can be used:

| Option      | Usage                                                           | Example            |
| :---------- | :-------------------------------------------------------------- | :----------------- |
| `-j`        | The path to a jar file to inspect                               | `-j ../../path`    |
| `-c`        | The path to the coverage file to use                            | `-c ../../path`    |
| `-e`        | The name of the fuzzer's entrypoint                             | `-e "com.foo.bar.Baz:main(java.lang.String[])"`    |
| `-d`        | The depth to run breadth first search                           | `-d 10`            |
| `-a`        | Report the ancestry of the entrypoint                           | `-a`               |
| `-o`        | The name of the output file                                     | `-o foo`           |

### Known Restrictions

* The static call graph generator does not account for methods invoked via
  reflection.



### Authors

Georgios Gousios <gousiosg@gmail.com>  
Will Cygan <wcygan3232@gmail.com>

### License

[2-clause BSD](http://www.opensource.org/licenses/bsd-license.php)
