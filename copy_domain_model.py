import os
import shutil

def copy_and_convert(source_dir, target_dir):
    # List of domain model files to copy (relative to source_dir)
    files_to_copy = [
        # cargo package
        'se/citerus/dddsample/domain/model/cargo/Cargo.java',
        'se/citerus/dddsample/domain/model/cargo/Leg.java',
        'se/citerus/dddsample/domain/model/cargo/RouteSpecification.java',
        'se/citerus/dddsample/domain/model/cargo/Delivery.java',
        'se/citerus/dddsample/domain/model/cargo/HandlingActivity.java',
        'se/citerus/dddsample/domain/model/cargo/TrackingId.java',
        'se/citerus/dddsample/domain/model/cargo/Itinerary.java',
        'se/citerus/dddsample/domain/model/cargo/RoutingStatus.java',
        'se/citerus/dddsample/domain/model/cargo/TransportStatus.java',
        
        # location package
        'se/citerus/dddsample/domain/model/location/Location.java',
        'se/citerus/dddsample/domain/model/location/UnLocode.java',
        
        # voyage package
        'se/citerus/dddsample/domain/model/voyage/Voyage.java',
        'se/citerus/dddsample/domain/model/voyage/CarrierMovement.java',
        'se/citerus/dddsample/domain/model/voyage/VoyageNumber.java',
        'se/citerus/dddsample/domain/model/voyage/Schedule.java',
        
        # handling package
        'se/citerus/dddsample/domain/model/handling/HandlingEvent.java',
        'se/citerus/dddsample/domain/model/handling/HandlingHistory.java',
        
        # shared package
        'se/citerus/dddsample/domain/shared/DomainEntity.java',
        'se/citerus/dddsample/domain/shared/ValueObject.java',
        'se/citerus/dddsample/domain/shared/DomainEvent.java',
        'se/citerus/dddsample/domain/shared/AbstractSpecification.java',
        'se/citerus/dddsample/domain/shared/Specification.java',
        'se/citerus/dddsample/domain/shared/AndSpecification.java',
        'se/citerus/dddsample/domain/shared/OrSpecification.java',
        'se/citerus/dddsample/domain/shared/NotSpecification.java',
    ]
    
    # Create shared directory
    shared_target = os.path.join(target_dir, 'se/citerus/dddsample/domain/shared')
    os.makedirs(shared_target, exist_ok=True)
    
    for file_path in files_to_copy:
        src_path = os.path.join(source_dir, file_path)
        dst_path = os.path.join(target_dir, file_path)
        
        # Create target directory
        os.makedirs(os.path.dirname(dst_path), exist_ok=True)
        
        if os.path.exists(src_path):
            # Read and convert jakarta.persistence to javax.persistence
            with open(src_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Convert imports
            content = content.replace('jakarta.persistence', 'javax.persistence')
            content = content.replace('jakarta.validation', 'javax.validation')
            
            # Fix instanceof pattern matching for Java 11
            if 'instanceof Location other' in content:
                content = content.replace('if (!(object instanceof Location other)) {', 'if (!(object instanceof Location)) {')
                # Need to add cast after the instanceof check - this is a simple fix
                # We'll need a more sophisticated replacement
                # For now, let's just copy and we'll fix manually
            
            # Write converted file
            with open(dst_path, 'w', encoding='utf-8') as f:
                f.write(content)
            
            print(f"Copied and converted: {file_path}")
        else:
            print(f"Warning: Source file not found: {src_path}")

if __name__ == '__main__':
    source_dir = '/home/marco/dev/eclispelinkanalyser/dddsample-core/src/main/java'
    target_dir = '/home/marco/dev/eclispelinkanalyser/dddsample-minimal/src/main/java'
    
    copy_and_convert(source_dir, target_dir)