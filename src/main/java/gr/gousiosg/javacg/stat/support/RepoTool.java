package gr.gousiosg.javacg.stat.support;

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
import java.util.Map;
import java.util.Optional;

public class RepoTool {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepoTool.class);
    private String name;
    private String URL;
    private String checkoutID;
    private String patchName;
    private String output = "artifacts/output/";
    private Git git;

    private RepoTool(String name, String URL, String checkoutID, String patchName){
        this.name = name;
        this.URL = URL;
        this.checkoutID = checkoutID;
        this.patchName = patchName;
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
            pb.command("bash", "-c", "patch -d " + name + " < " + patchName);
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
    }

    public void moveFiles() throws IOException{
//        Path jacoco = Files.move(
//                Paths.get(System.getProperty("user.dir") + "/" + this.name + "/target/site/jacoco/jacoco.xml"),
//                Paths.get(System.getProperty("user.dir") + "/artifacts/output/jacoco.xml"),
//                StandardCopyOption.REPLACE_EXISTING);
//        if(jacoco == null)
//            throw new IOException("Jacoco file not moved properly!");
        Path jar = Files.move(
                Paths.get(System.getProperty("user.dir") + "/" + this.name + "/target/" + this.name + "-1.0.6-SNAPSHOT.jar"),
                Paths.get(System.getProperty("user.dir") + "/artifacts/output/" + this.name + "-1.0.6-SNAPSHOT.jar"),
                StandardCopyOption.REPLACE_EXISTING);
        if(jar == null)
            throw new IOException("Jar not moved properly!");
        Path testJar = Files.move(
                Paths.get(System.getProperty("user.dir") + "/" + this.name + "/target/" + this.name + "-1.0.6-SNAPSHOT-tests.jar"),
                Paths.get(System.getProperty("user.dir") + "/artifacts/output/" + this.name + "-1.0.6-SNAPSHOT-tests.jar"),
                StandardCopyOption.REPLACE_EXISTING);
        if(testJar == null)
            throw new IOException("TestJar not moved properly!");
    }

    public static Optional<RepoTool> obtainTool(String folderName){ //Not implemented yet
        try {
            Yaml yaml = new Yaml();
            InputStream inputStream = new FileInputStream(new File("artifacts/configs/" + folderName + "/" + folderName + ".yaml"));
            Map<String, String> data = yaml.load(inputStream);
            return Optional.of(new RepoTool(data.get("name"), data.get("URL"), data.get("checkoutID"), data.get("patchName")));
        }
        catch(IOException e){
            e.printStackTrace();
        }
        LOGGER.error("Could not obtain yaml file!");
        return Optional.empty();
    }

    private boolean isWindows() {
        return System.getProperty("os.name")
                .toLowerCase().startsWith("windows");
    }
}
