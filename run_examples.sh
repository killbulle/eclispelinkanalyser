#!/bin/bash
set -e

AGENT_JAR="/home/marco/dev/eclispelinkanalyser/analyzer-backend/target/analyzer-backend-1.0-SNAPSHOT.jar"
EXAMPLES_ROOT="/home/marco/dev/eclispelinkanalyser/eclipselink-examples-repo"
TODO_FILE="/home/marco/dev/eclispelinkanalyser/TODO_MAPPINGS.md"
REPORTS_DIR="/home/marco/dev/eclispelinkanalyser/analyzer-backend/reports"

mkdir -p "$REPORTS_DIR"
echo "# Unhandled Mappings" > "$TODO_FILE"

run_example() {
    local dir=$1
    local main_class=$2
    local name=$3

    echo "----------------------------------------------------"
    echo "Running Example: $name in $dir"
    cd "$dir"
    
    # Ensure classpath is available
    if [ ! -f "classpath.txt" ]; then
        mvn dependency:build-classpath -Dmdep.outputFile=classpath.txt > /dev/null 2>&1
    fi
    CP=$(cat classpath.txt):target/classes:target/test-classes

    # Run with agent
    # We use a timeout to handle examples that might hang or wait for input
    # We redirect stderr to capture [TODO-MAPPING]
    # The agent now writes to reports/agent-report.json relative to CWD
    timeout 30s java -javaagent:"$AGENT_JAR" -cp "$CP" "$main_class" > run.log 2> run.err || true
    
    # Give it a moment to flush the report if it was in a background thread
    sleep 2

    # Check for report
    if [ -f "reports/agent-report.json" ]; then
        echo "[SUCCESS] Report generated for $name"
        mv "reports/agent-report.json" "$REPORTS_DIR/${name}_report.json"
    else
        echo "[FAILURE] No report for $name. Check run.log"
    fi

    # Check for unhandled mappings
    grep "\[TODO-MAPPING\]" run.err | sort -u >> "$TODO_FILE" || true
}

# Example 1: Employee Model JavaSE
run_example "$EXAMPLES_ROOT/jpa/employee/employee.model" "example.JavaSEExample" "EmployeeModel"


echo "----------------------------------------------------"
echo "Done. Reports in $REPORTS_DIR, mappings in $TODO_FILE"
