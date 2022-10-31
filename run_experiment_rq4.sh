GEN_STATS="artifacts/experiments/RQ4/generateStats.py"
for PROJECT in mph-table convex jflex
do
  echo Generating statistics for $PROJECT
  python3 $GEN_STATS $PROJECT
done