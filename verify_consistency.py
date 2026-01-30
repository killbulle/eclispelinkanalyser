import json
import sys

def load_json(filepath):
    try:
        with open(filepath, 'r') as f:
            return json.load(f)
    except Exception as e:
        print(f"Error loading {filepath}: {e}")
        sys.exit(1)

def get_entities(report):
    # Normalize entity list to a dictionary keyed by name for easy lookup
    entities = {}
    if 'nodes' in report:
        for node in report['nodes']:
            entities[node['name']] = node
    return entities

def compare_entities(static_report_path, agent_report_path):
    print(f"Comparing Static Report: {static_report_path}")
    print(f"vs Agent Report: {agent_report_path}\n")

    static_data = load_json(static_report_path)
    agent_data = load_json(agent_report_path)

    static_entities = get_entities(static_data)
    agent_entities = get_entities(agent_data)

    all_entity_names = sorted(set(static_entities.keys()) | set(agent_entities.keys()))

    print(f"{'ENTITY':<30} | {'STATIC':<10} | {'AGENT':<10} | {'STATUS'}")
    print("-" * 70)

    discrepancies = []

    for name in all_entity_names:
        in_static = name in static_entities
        in_agent = name in agent_entities
        
        status = "MATCH"
        if in_static and not in_agent:
            status = "STATIC_ONLY"
        elif not in_static and in_agent:
            status = "AGENT_ONLY"
        
        print(f"{name:<30} | {'YES' if in_static else 'NO':<10} | {'YES' if in_agent else 'NO':<10} | {status}")

        if status == "MATCH":
            # Deep dive into attributes if both exist
            s_attrs = set(static_entities[name].get('attributes', {}).keys())
            a_attrs = set(agent_entities[name].get('attributes', {}).keys())
            
            missing_in_static = a_attrs - s_attrs
            missing_in_agent = s_attrs - a_attrs
            
            if missing_in_static:
                discrepancies.append(f"  - [{name}] Attributes in Agent but not Static: {missing_in_static}")
            if missing_in_agent:
                discrepancies.append(f"  - [{name}] Attributes in Static but not Agent: {missing_in_agent}")

            # Check relationships
            s_rels = {r.get('attributeName') for r in static_entities[name].get('relationships', [])}
            a_rels = {r.get('attributeName') for r in agent_entities[name].get('relationships', [])}

            missing_rels_in_static = a_rels - s_rels
            missing_rels_in_agent = s_rels - a_rels

            if missing_rels_in_static:
                discrepancies.append(f"  - [{name}] Relationships in Agent but not Static: {missing_rels_in_static}")
            if missing_rels_in_agent:
                discrepancies.append(f"  - [{name}] Relationships in Static but not Agent: {missing_rels_in_agent}")

    print("\n--- Detailed Discrepancies ---")
    if discrepancies:
        for disc in discrepancies:
            print(disc)
    else:
        print("No attribute/relationship discrepancies found in matching entities.")

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python3 verify_consistency.py <static_report.json> <agent_report.json>")
        sys.exit(1)
    
    compare_entities(sys.argv[1], sys.argv[2])
