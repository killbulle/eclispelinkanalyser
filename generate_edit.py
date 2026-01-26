#!/usr/bin/env python3
import json

with open('/tmp/old.txt', 'r') as f:
    old_content = f.read()
    
with open('/tmp/new.txt', 'r') as f:
    new_content = f.read()

# Create the edit tool call JSON
edit_call = {
    "filePath": "/home/marco/dev/eclispelinkanalyser/analyzer-frontend/src/App.tsx",
    "oldString": old_content,
    "newString": new_content,
    "replaceAll": False
}

print(json.dumps(edit_call, indent=2))