import sys
import subprocess
import os
import datetime
import re
import pandas as pd

BASE_RESULT_DIR = "artifacts/results/"

def obtain_stats_directories(results_directory: str) -> list[str]:
    directory_tree = [x for x in os.walk(results_directory)] # os.walk returns a tuple with structure (directory, subdirectories, files)
    return directory_tree[0][1]

def filter_for_recent_results(project_name: str, stats_directories: list[str]) -> dict[str, str]:
    valid_directories = []
    time_stamps = [datetime.datetime.strptime(x.replace(project_name, "").replace("_", ":").replace("T", " "), "%Y-%m-%d %H:%M:%S.%f")
                   for x in stats_directories]
    time_stamps.sort()
    valid_runs = time_stamps[-10:]
    for directory in stats_directories:
        val = datetime.datetime.strptime(directory.replace(project_name, "").replace("_", ":").replace("T", " "), "%Y-%m-%d %H:%M:%S.%f")
        if val in valid_runs:
            valid_directories.append(directory)
    return valid_directories

def evaluate_directories(project_name: str, results_directory: str, directories: list[str])-> dict[str, dict]:
    final_stats = {}
    iteration = 1
    for directory in directories:
        directory_path = results_directory + directory + "/"
        directory_tree = [x[2] for x in os.walk(directory_path)]
        valid_htmls = [x for x in directory_tree[0] if 'html' in x]
        directory_stats = retrieve_time_elapsed(directory_path=directory_path, valid_htmls=valid_htmls)
        project_iteration = project_name + " - " + str(iteration)
        final_stats[project_iteration] = directory_stats
        iteration += 1
    return final_stats

def retrieve_time_elapsed(directory_path: str, valid_htmls: list[str]) -> dict[str, str]:
    times_elapsed_dict = {}
    for html_file in valid_htmls:
        property_name = "Property - " + html_file.replace(".html", "").replace("#", "")
        file_path = directory_path + html_file
        with open(file_path) as f:
            contents = f.read()
            time_elapsed_regrex = re.search('Total Time Elapsed: (.+?) seconds', contents)
            if time_elapsed_regrex:
                time_elapsed = time_elapsed_regrex.group(1)
                times_elapsed_dict[property_name] = round(float(time_elapsed), 2)
    return times_elapsed_dict

def generate_report(project_name: str, final_stats: dict[str, pd.Series], final_fixed_stats: dict[str, dict]):
    report_name = "artifacts/output/rq4_" + project_name + ".csv"
    tex_file = report_name.replace(".csv", ".tex")
    final_stats.update(final_fixed_stats)
    df = pd.DataFrame(final_stats)
    df.to_csv(path_or_buf=report_name)
    df.style.to_latex(buf=tex_file)

def main():
    if not sys.argv[1]:
        raise Exception("Must specify project name through command line param!")
    project_name = sys.argv[1]
    fixed_project_name = project_name + "-fixed"
    results_directory = BASE_RESULT_DIR + project_name + "/"
    fixed_results_directory = BASE_RESULT_DIR + fixed_project_name + "/"
    # vanilla
    stats_directories = obtain_stats_directories(results_directory=results_directory)
    evaluated_runs = filter_for_recent_results(project_name=project_name, stats_directories=stats_directories)
    final_stats = evaluate_directories(project_name=project_name, results_directory=results_directory, directories=evaluated_runs)

    # fixed
    fixed_stats_directories = obtain_stats_directories(results_directory=fixed_results_directory)
    evaluated_fixed_runs = filter_for_recent_results(project_name=project_name, stats_directories=fixed_stats_directories)
    final_fixed_stats = evaluate_directories(project_name=fixed_project_name, results_directory=fixed_results_directory, directories=evaluated_fixed_runs)

    # final report
    generate_report(project_name=project_name, final_stats=final_stats, final_fixed_stats=final_fixed_stats)


if __name__ == "__main__":
    main()
