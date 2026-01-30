import os
import shutil
import re

def convert_imports(source_dir, target_dir):
    # Map jakarta.persistence -> javax.persistence
    import_map = {
        'jakarta.persistence': 'javax.persistence',
        'jakarta.validation': 'javax.validation',
        'jakarta.annotation': 'javax.annotation',
        # Add other jakarta packages if needed
    }
    
    # Find all Java files
    for root, dirs, files in os.walk(source_dir):
        for file in files:
            if file.endswith('.java'):
                src_path = os.path.join(root, file)
                rel_path = os.path.relpath(src_path, source_dir)
                target_path = os.path.join(target_dir, rel_path)
                
                # Create target directory
                os.makedirs(os.path.dirname(target_path), exist_ok=True)
                
                # Read and convert imports
                with open(src_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Convert imports
                for old, new in import_map.items():
                    content = re.sub(r'import\s+' + re.escape(old) + r'\.', f'import {new}.', content)
                    # Also handle fully qualified names in annotations
                    content = content.replace(f'@{old}.', f'@{new}.')
                
                # Write converted file
                with open(target_path, 'w', encoding='utf-8') as f:
                    f.write(content)
                
                print(f"Converted: {rel_path}")

def copy_entities():
    source_dir = '/home/marco/dev/eclispelinkanalyser/dddsample-core/src/main/java'
    target_dir = '/home/marco/dev/eclispelinkanalyser/dddsample-eclipselink/src/main/java'
    
    # First, copy all Java files
    convert_imports(source_dir, target_dir)
    
    # Also copy any resources (optional)
    source_resources = '/home/marco/dev/eclispelinkanalyser/dddsample-core/src/main/resources'
    target_resources = '/home/marco/dev/eclispelinkanalyser/dddsample-eclipselink/src/main/resources'
    
    if os.path.exists(source_resources):
        shutil.copytree(source_resources, target_resources, dirs_exist_ok=True)

if __name__ == '__main__':
    copy_entities()