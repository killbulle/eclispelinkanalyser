import os
import re

def convert_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Replace jakarta.persistence with javax.persistence
    content = content.replace('jakarta.persistence', 'javax.persistence')
    # Also replace jakarta.validation with javax.validation
    content = content.replace('jakarta.validation', 'javax.validation')
    # Also replace jakarta.annotation with javax.annotation
    content = content.replace('jakarta.annotation', 'javax.annotation')
    # Replace jakarta.servlet with javax.servlet
    content = content.replace('jakarta.servlet', 'javax.servlet')
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

def convert_directory(root_dir):
    for root, dirs, files in os.walk(root_dir):
        for file in files:
            if file.endswith('.java'):
                filepath = os.path.join(root, file)
                convert_file(filepath)
                print(f"Converted: {filepath}")

if __name__ == '__main__':
    convert_directory('/home/marco/dev/eclispelinkanalyser/dddsample-eclipselink-mod/src/main/java')