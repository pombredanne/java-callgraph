import subprocess  # https://docs.python.org/3/library/subprocess.html
import os  # https://docs.python.org/3/library/os.html
import re  # https://docs.python.org/3/library/re.html
import yaml  # https://pyyaml.org/wiki/PyYAMLDocumentation
from shutil import which  # https://docs.python.org/3/library/shutil.html#shutil.which
from pathlib import Path  # https://docs.python.org/3/library/pathlib.html

'''
Checks a project out and builds it
@author: Will Cygan <wcygan3232@gmail.com>
'''


def panic(msg, code):
    """
    Invoked when the program enters an invalid state
    :param msg: the message to display
    :param code: the exit status
    """

    print(msg)
    exit(code)


def require_program(program):
    """
    Verify that the program we're about to call exists
    :param program: the program to verify that it is on the PATH
    """

    if which(program) is None:
        panic("{} is not installed! Please install it and try again...".format(program), -1)


def extract_project_name(url):
    """
    Extracts the name of the project from a git url

    Example:
        given    "git@github.com:wcygan/java-callgraph.git"
        returns                        "java-callgraph"

    :param url: the url to search
    :return: the name of the project from the git url
    """

    result = re.search('/(.+?).git', url)
    if result is None:
        panic("Didn't find a project name inside of {}".format(url), -1)
    return result.group(1)


def verify_build_system_install(cfg):
    """
    Determines which build system to use for the project
    :return: a string representing the build command (e.g., `gradle` or `mvn`)
    """

    system = cfg["build-system"]

    # overwrite with the maven command `mvn`
    if system == "maven":
        system = "mvn"

    require_program(system)


def get_project_url(cfg):
    """
    Fetches the repository url from the config
    :return: the url specified by the user
    """

    url = cfg["repository-url"]
    if url is None or url == "" or not url.endswith(".git"):
        panic("Please provide a valid git url!", -1)
    return url


def clone_url(url):
    """
    Clones a git url and returns the directory that it was cloned to
    :param url: a url to clone
    :return: the directory it was cloned to
    """

    target_dir = Path(os.getcwd() + "/" + extract_project_name(url))

    if not target_dir.exists():
        require_program("git")
        subprocess.call(["git", "clone", url])

    return target_dir


def build_java_callgraph(root_dir):
    """
    Builds java-callgraph
    :param root_dir: The root directory of java-callgraph
    :return:
    """

    cwd = os.getcwd()
    os.chdir(root_dir)
    subprocess.call(["mvn", "install"])
    os.chdir(cwd)


def build_target_project(cfg, directory):
    """
    Executes the build system
    :param directory: the directory to execute the build command in
    :param cfg: the run configuration
    """

    cwd = os.getcwd()
    cmd = cfg["build-command"]
    os.chdir(directory)
    subprocess.call([x for x in cmd.split()])
    os.chdir(cwd)


def run_java_cg(root_dir, target_dir, cfg):
    """
    Executes java-callgraph against the target project
    :param root_dir: the directory that java-callgraph resides in
    :param target_dir: the directory that the target project resides in
    :param cfg: the run configuration
    """

    javacg_jar = Path("{}{}".format(root_dir, "/target/javacg-0.1-SNAPSHOT-jar-with-dependencies.jar"))
    target_jar = Path("{}{}".format(target_dir, cfg["target-jar-location"]))
    cmd = "java -jar {} -j {}".format(javacg_jar, target_jar)

    if "coverage-location" in cfg:
        cmd += " -c {}".format(Path("{}{}".format(target_dir, cfg["coverage-location"])))

    if "entrypoint" in cfg:
        cmd += " -e {}".format(cfg["entrypoint"])

    if "depth" in cfg:
        cmd += " -d {}".format(cfg["depth"])

    if "output-name" in cfg:
        cmd += " -o {}".format(cfg["output-name"])

    if "ancestry" in cfg:
        cmd += " -a {}".format(cfg["ancestry"])

    os.chdir(Path("{}{}".format(root_dir, "/artifacts")))
    print("Running `{}`".format(cmd))
    subprocess.call([x for x in cmd.split()])


if __name__ == '__main__':
    """
    1. Fetch the url of a project to clone (e.g., git@github.com:wcygan/java-callgraph.git)
    2. Ask for the project's build system (e.g., maven)
    3. Clone the project (e.g., `git clone <project_url>`
    4. Build / Install the project (e.g., `mvn install`)
    """

    with open('config.yaml') as f:
        config = yaml.load(f, Loader=yaml.FullLoader)

    javacg_directory = Path(os.getcwd()).parent
    if not Path("{}{}".format(javacg_directory, "/target")).exists():
        build_java_callgraph(javacg_directory)

    # 1. Fetch the project's repository url
    repository_url = get_project_url(config)

    # 2. Verify the build system is installed
    verify_build_system_install(config)

    # 3. Clone the project & fetch the directory it resides in
    target_directory = clone_url(repository_url)

    # 4. Enter the project's directory & execute the build system
    build_target_project(config, target_directory)

    # 5. Execute java-callgraph against the target project
    run_java_cg(javacg_directory, target_directory, config)
