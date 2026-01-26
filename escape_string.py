#!/usr/bin/env python3
import json
import sys

with open('/tmp/old.txt', 'r') as f:
    old_content = f.read()
    
with open('/tmp/new.txt', 'r') as f:
    new_content = f.read()

# Escape for JSON string
old_escaped = json.dumps(old_content)[1:-1]  # Remove surrounding quotes
new_escaped = json.dumps(new_content)[1:-1]

print("OLD (first 500 chars):")
print(old_escaped[:500])
print("\nNEW (first 500 chars):")
print(new_escaped[:500])