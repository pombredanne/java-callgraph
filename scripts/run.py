import subprocess  # https://docs.python.org/3/library/subprocess.html
import os  # https://docs.python.org/3/library/os.html
import re  # https://docs.python.org/3/library/re.html
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


def get_cloned_directory(url, cwd):
    """
    Finds the directory which was created by `git clone`
    :param url: the url of the repository that was cloned
    :param cwd: the directory that the clone was executed in
    :return: the directory which was created by cloning the url
    """

    path = cwd.joinpath(Path(extract_project_name(url)))
    if path is None:
        panic("Unable to find the directory that {} was cloned to".format(url), -1)
    return path


def get_builder():
    """
    Determines which build system to use for the project
    :return: a string representing the build command (e.g., `gradle` or `mvn`)
    """

    print("Please specify the project's build tool")
    print("    1: Maven (Default)")
    print("    2: Gradle")
    user_input = input("Please specify the number representing your project's build tool: ")

    build_system = "mvn"
    builder_types = {
        "1": "mvn",
        "2": "gradle"
    }

    if user_input in builder_types:
        build_system = builder_types[user_input]
    else:
        print("Defaulting to Maven...")

    require_program(build_system)
    return build_system


def get_project_url():
    """
    Prompts the user for a url to the repository they'd like to clone
    :return: the url specified by the user
    """

    print("Please specify the repository to clone, e.g., git@github.com:wcygan/java-callgraph.git")
    url = input("The repository to clone: ")
    if url is None or url == "" or not url.endswith(".git"):
        panic("Please provide a valid git url!", -1)
    return url


def clone_url(url):
    """
    Clones a git url and returns the directory that it was cloned to
    :param url: a url to clone
    :return: the directory it was cloned to
    """

    cwd = Path(os.getcwd())
    subprocess.call(["git", "clone", url])
    return get_cloned_directory(url, cwd)


def execute_build_system(program):
    """
    Executes the build system
    :param program: the build system to execute
    """

    panic("Not yet implemented!", 0)
    # TODO: Make this work :)
    # require_program(program)
    # if program == "gradle":
    #     subprocess.call([program, "build"])
    # elif program == "mvn":
    #     subprocess.call([program, "install"])


if __name__ == '__main__':
    # TODO: What assumptions can we make about what directory this is called from?

    # Fetch directories
    current_directory = Path(os.getcwd())
    parent_directory = current_directory.parent

    # Prompt for a project's repository to clone
    repository_url = get_project_url()

    # Identify the build system
    builder = get_builder()

    # Clone the project & fetch the directory it resides in
    cloned_project_directory = clone_url(repository_url)

    # Enter the project's directory & execute the build system
    os.chdir(cloned_project_directory)
    execute_build_system(builder)
