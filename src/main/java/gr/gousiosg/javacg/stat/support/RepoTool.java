package gr.gousiosg.javacg.stat.support;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.yaml.snakeyaml.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.util.Map;
import java.util.Optional;

public class RepoTool {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepoTool.class);
    private String name;
    private String URL;
    private String checkoutID;
    private String patchName;
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
        if(isWindows()){
            pb.command("cmd.exe", "/c", "git", "apply", patchName, "--directory", name);
        }
        else{
            pb.command("sh -c patch -p1 -d", name, "<", patchName);
        }
        pb.directory(new File(System.getProperty("user.dir")));
        Process process = pb.start();
        process.waitFor();
    }

    public void buildJars() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        if(isWindows()){
            pb.command("cmd.exe", "/c", "mvn", "install", "-Dmaven.test.failure.ignore=true");
        }
        else{
            pb.command("sh -c mvn install --Dmaven.test.failure.ignore=true");
        }
        pb.directory(new File(System.getProperty("user.dir") + "/" + name));
        Process process = pb.start();
        process.waitFor();
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
