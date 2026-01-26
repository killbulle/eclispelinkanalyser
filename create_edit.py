#!/usr/bin/env python3
import re

with open('/home/marco/dev/eclispelinkanalyser/analyzer-frontend/src/App.tsx', 'r') as f:
    lines = f.readlines()

# Lines are 1-indexed in file, but 0-indexed in list
start_line = 451  # line 452 in file (0-indexed)
end_line = 684    # line 685 in file (0-indexed)

old_content = ''.join(lines[start_line:end_line+1])
new_content = '        {!sidebarCollapsed && (\n' + old_content + '        )}\n'

print("Old content (first 200 chars):")
print(old_content[:200])
print("\nNew content (first 200 chars):")
print(new_content[:200])

# Write to temp files for verification
with open('/tmp/old.txt', 'w') as f:
    f.write(old_content)
    
with open('/tmp/new.txt', 'w') as f:
    f.write(new_content)