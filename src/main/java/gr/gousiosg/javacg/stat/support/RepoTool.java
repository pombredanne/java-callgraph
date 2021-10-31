package gr.gousiosg.javacg.stat.support;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class RepoTool {
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
        pb.directory(new File(System.getProperty("user.dir")));
        Process process = pb.start();
        process.waitFor();
    }

    public void buildJars() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        if(isWindows()){
            pb.command("cmd.exe", "/c", "mvn", "install", "-Dmaven.test.failure.ignore=true");
        }
        pb.directory(new File(System.getProperty("user.dir") + "/" + name));
        Process process = pb.start();
        process.waitFor();
    }

    public static RepoTool obtainTool(String folderName){ //Not implemented yet
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command("cmd.exe", "/c", "DIR");
            pb.directory(new File(System.getProperty("user.dir")));
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null)
                System.out.println(line);
            int exitCode = process.waitFor();
            System.out.println(exitCode);
        }
        catch(InterruptedException | IOException e){
            e.printStackTrace();
        }
        return null;
    }

    private boolean isWindows() {
        return System.getProperty("os.name")
                .toLowerCase().startsWith("windows");
    }
}
