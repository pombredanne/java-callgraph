import os
import datetime
import re

import numpy as np
import pandas as pd

BASE_RESULT_DIR = "artifacts/results/"
PROJECTS = ["jflex", "convex", "mph-table"]
REPORT_NAME = "artifacts/output/rq4.csv"
TEX_REPORT_NAME = "artifacts/output/rq4.tex"

def obtain_stats_directories(results_directory: str) -> list[str]:
    directory_tree = [x for x in os.walk(results_directory)] # os.walk returns a tuple with structure (directory, subdirectories, files)
    return directory_tree[0][1]

def filter_for_recent_results(project_name: str, stats_directories: list[str]) -> dict[str, str]:
    valid_directories = []
    project_string = project_name if project_name != "convex" else project_name + "-core"  # edge case
    if "mph-table-fixed" in stats_directories[0]:  # edge case
        project_string = "mph-table-fixed"
    time_stamps = [datetime.datetime.strptime(x.replace(project_string, "").replace("_", ":").replace("T", " "), "%Y-%m-%d %H:%M:%S.%f")
                   for x in stats_directories]
    time_stamps.sort()
    valid_runs = time_stamps[-10:]
    for directory in stats_directories:
        val = datetime.datetime.strptime(directory.replace(project_string, "").replace("_", ":").replace("T", " "), "%Y-%m-%d %H:%M:%S.%f")
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
        property_name = html_file.replace(".html", "").replace("#", "-")
        file_path = directory_path + html_file
        with open(file_path) as f:
            contents = f.read()
            time_elapsed_regrex = re.search('Total Time Elapsed: (.+?) seconds', contents)
            if time_elapsed_regrex:
                time_elapsed = time_elapsed_regrex.group(1)
                times_elapsed_dict[property_name] = round(float(time_elapsed), 2)
    return times_elapsed_dict

def generate_report_stats(stat_values: dict[str, dict]) -> dict[str, str]:
    first_iteration = stat_values[next(iter(stat_values))]
    # stage a dictionary to contain an array of times for ea property
    property_dict = {}
    for key in first_iteration:
        property_dict[key] = []

    # populate the dictionary with our results
    for key, val in stat_values.items():
        for prop, time in val.items():
            property_array = property_dict.get(prop)
            property_array.append(time)

    # generate mean, standard deviation and populate our final object
    property_stats_dict = {}
    for key, val in property_dict.items():
        np_array = np.array(val)
        mean = round(np_array.mean(), 2)
        standard_dev = round(np_array.std(), 2)
        property_stats_dict[key] = str(mean) + " \u00B1 " + str(standard_dev)
    return property_stats_dict


def generate_project_report(project_name: str, final_stats: dict[str, str], final_fixed_stats: dict[str, str]) -> dict[str, dict]:
    final_report_dict = {project_name: final_stats, project_name + "-fixed": final_fixed_stats}
    return final_report_dict


def main():
    final_report = {}
    df_dict = {}
    for project_name in PROJECTS:
        print("Starting " + project_name)
        fixed_project_name = project_name + "-fixed"
        results_directory = BASE_RESULT_DIR + project_name + "/"
        fixed_results_directory = BASE_RESULT_DIR + fixed_project_name + "/"
        # vanilla
        stats_directories = obtain_stats_directories(results_directory=results_directory)
        evaluated_runs = filter_for_recent_results(project_name=project_name, stats_directories=stats_directories)
        raw_stats = evaluate_directories(project_name=project_name, results_directory=results_directory, directories=evaluated_runs)

        # fixed
        fixed_stats_directories = obtain_stats_directories(results_directory=fixed_results_directory)
        evaluated_fixed_runs = filter_for_recent_results(project_name=project_name, stats_directories=fixed_stats_directories)
        fixed_raw_stats = evaluate_directories(project_name=fixed_project_name, results_directory=fixed_results_directory, directories=evaluated_fixed_runs)

        # obtain mean/st dev
        final_stats = generate_report_stats(stat_values=raw_stats)
        final_fixed_stats = generate_report_stats(stat_values=fixed_raw_stats)
        report = generate_project_report(project_name=project_name, final_stats=final_stats, final_fixed_stats=final_fixed_stats)
        df_dict[project_name] = report
        final_report.update(report)
        print("Completed " + project_name)

    for key, val in df_dict.items():
        df = pd.DataFrame(val).reset_index()
        df.to_csv(path_or_buf="artifacts/output/" + key + "_rq4.csv")
        df.style.to_latex(buf="artifacts/output/" + key + "_rq4.tex")


if __name__ == "__main__":
    main()
