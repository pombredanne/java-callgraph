GEN_STATS="artifacts/experiments/RQ4/generateStats.py"
GEN_RESULTS="artifacts/experiments/RQ4/generateResults.py"
for PROJECT in mph-table convex jflex
do
  echo Generating statistics for $PROJECT
  python3 $GEN_STATS $PROJECT
done
echo Generating results for projects
python3 $GEN_RESULTS