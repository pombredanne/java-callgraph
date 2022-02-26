package gr.gousiosg.javacg.stat.support;

import gr.gousiosg.javacg.dyn.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.yaml.snakeyaml.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RepoTool {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepoTool.class);
    final private String name;
    final private String URL;
    final private String checkoutID;
    final private String patchName;
    final private String subProject;
    final private String mvnOptions;
    private List<Map<String, String>> properties;
    private Git git;

    private RepoTool(String name, String URL, String checkoutID, String patchName, String subProject, String mvnOptions){
        this.name = name;
        this.URL = URL;
        this.checkoutID = checkoutID;
        this.patchName = patchName;
        this.subProject = subProject;
        this.mvnOptions = mvnOptions;
    }

    public RepoTool(String name) throws FileNotFoundException {
        // @todo Perhaps using objects to store configuration data so we don't have to have unchecked casts e.g. https://www.baeldung.com/java-snake-yaml

        Yaml yaml = new Yaml();
        InputStream inputStream = new FileInputStream("artifacts/configs/" + name + "/" + name + ".yaml");
        Map<String, Object> data = yaml.load(inputStream);

        this.name = name;
        URL = (String) data.get("URL");
        checkoutID = (String) data.get("checkoutID");
        patchName = (String) data.get("patchName");
        subProject = (String) data.getOrDefault("subProject", "");
        mvnOptions = (String) data.getOrDefault("mvnOptions", "");
        properties = (List<Map<String,String>>) data.get("properties");
    }

    public void cloneRepo() throws GitAPIException, JGitInternalException {
        this.git = Git.cloneRepository()
                .setDirectory(new File(name))
                .setURI(URL)
                .call();
        this.git.checkout()
                .setName(checkoutID)
                .call();
    }

    public void applyPatch() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        if(isWindows())
            pb.command("cmd.exe", "/c", "git", "apply", patchName, "--directory", name);
        else
            pb.command("bash", "-c", "patch -p1 -d " + name + " < " + patchName);
        Process process = pb.start();
        process.waitFor();
    }

    public void buildJars() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        if(isWindows())
            pb.command("cmd.exe", "/c", "mvn", "install", "-DskipTests");
        else
            pb.command("bash", "-c", "mvn install -DskipTests");
        pb.directory(new File(this.name));
        Process process = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while((line = br.readLine()) != null)
            LOGGER.info(line);
        process.waitFor();
        moveJars();
    }

    public void testProperty(String property) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        if(isWindows())
            pb.command("cmd.exe", "/c", "mvn", "test", mvnOptions, "-Dtest=" + property);
        else
            pb.command("bash", "-c", "mvn test " + mvnOptions + " -Dtest=" + property);
        pb.directory(new File(this.name));
        long start = System.nanoTime();
        Process process = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while((line = br.readLine()) != null)
            LOGGER.info(line);
        process.waitFor();
        long end = System.nanoTime();
        moveJacoco(property, end - start);
    }

    public void cleanTarget() throws IOException, InterruptedException {
        LOGGER.info("-------Cleaning target---------");
        ProcessBuilder pb = new ProcessBuilder();
        if(isWindows())
            pb.command("cmd.exe", "/c", "mvn", "clean");
        else
            pb.command("bash", "-c", "mvn clean");
        pb.directory(new File(this.name));
        Process process = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while((line = br.readLine()) != null)
            LOGGER.info(line);
        process.waitFor();
    }

    public List<Pair<String,String>> obtainCoverageFilesAndEntryPoints(){
        List<Pair<String,String>> coverageFiles = new LinkedList<>();
        for(Map<String, String> m : properties)
            coverageFiles.add(new Pair<>("artifacts/results/" + getProjectDir() + "/" + m.get("name") + ".xml", m.get("entryPoint")));
        return coverageFiles;
    }

    public static Optional<RepoTool> obtainTool(String folderName){
        try {
            Yaml yaml = new Yaml();
            InputStream inputStream = new FileInputStream(new File("artifacts/configs/" + folderName + "/" + folderName + ".yaml"));
            Map<String, String> data = yaml.load(inputStream);
            return Optional.of(new RepoTool(data.get("name"), data.get("URL"), data.get("checkoutID"), data.get("patchName"), data.getOrDefault("subProject", ""), data.getOrDefault("mvnOptions", "")));
        }
        catch(IOException e){
            LOGGER.error("IOException: " + e.getMessage());
        }
        LOGGER.error("Could not obtain yaml file!");
        return Optional.empty();
    }

    private void moveJars() throws IOException{
        Path jar = Files.move(
                Paths.get(System.getProperty("user.dir") + "/" + this.name + "/target/" + this.name + "-1.0.6-SNAPSHOT.jar"),
                Paths.get(System.getProperty("user.dir") + "/artifacts/output/" + this.name + "-1.0.6-SNAPSHOT.jar"),
                StandardCopyOption.REPLACE_EXISTING);
        Path testJar = Files.move(
                Paths.get(System.getProperty("user.dir") + "/" + this.name + "/target/" + this.name + "-1.0.6-SNAPSHOT-tests.jar"),
                Paths.get(System.getProperty("user.dir") + "/artifacts/output/" + this.name + "-1.0.6-SNAPSHOT-tests.jar"),
                StandardCopyOption.REPLACE_EXISTING);
    }

    private void moveJacoco(String property, long timeElapsed) throws IOException{
        String projectName = getProjectDir();

        String jacocoPath = System.getProperty("user.dir") + "/" + projectName + "/target/site/jacoco/jacoco.xml";
        String jacocoTargetPath = System.getProperty("user.dir") + "/artifacts/results/"+ projectName + "/" + property + ".xml";
        String statisticsPath = System.getProperty("user.dir") + "/" + projectName + "/target/site/jacoco/index.html";
        String statisticsTargetPath = System.getProperty("user.dir") + "/artifacts/results/"+ projectName + "/" + property + ".html";
        Path jacoco = Files.move(
                Paths.get(jacocoPath),
                Paths.get(jacocoTargetPath),
                StandardCopyOption.REPLACE_EXISTING);
        Path statistics = Files.move(
                Paths.get(statisticsPath),
                Paths.get(statisticsTargetPath),
                StandardCopyOption.REPLACE_EXISTING);
        double timeElapsedInSeconds = (double) timeElapsed / 1_000_000_000;
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        try{
            fileWriter = new FileWriter(statisticsTargetPath, true);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.append("<html><section><h1> Total Time Elapsed: "+ timeElapsedInSeconds +" seconds</h1></section></html>");
            bufferedWriter.flush();
        } finally{
            fileWriter.close();
            bufferedWriter.close();
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name")
                .toLowerCase().startsWith("windows");
    }

    private String getProjectDir() {
        if (subProject.equals("")) {
            return name;
        }
        return this.name + "/" + this.subProject;
    }
}
