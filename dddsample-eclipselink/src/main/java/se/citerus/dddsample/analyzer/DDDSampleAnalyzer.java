package se.citerus.dddsample.analyzer;

import com.eclipselink.analyzer.agent.AgentAnalysisTrigger;
import com.eclipselink.analyzer.model.EntityNode;
import org.eclipse.persistence.jpa.JpaHelper;
import org.eclipse.persistence.sessions.Session;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.List;

public class DDDSampleAnalyzer {
    
    public static void main(String[] args) {
        System.out.println("Starting DDDSample EclipseLink analysis...");
        
        EntityManagerFactory emf = null;
        EntityManager em = null;
        
        try {
            // Create EntityManagerFactory
            emf = Persistence.createEntityManagerFactory("dddsample");
            System.out.println("EntityManagerFactory created.");
            
            // Create EntityManager to trigger schema generation
            em = emf.createEntityManager();
            System.out.println("EntityManager created.");
            
            // Get EclipseLink Session
            Session session = JpaHelper.getEntityManager(em).getDatabaseSession();
            System.out.println("EclipseLink Session obtained.");
            
            // Run analysis via AgentAnalysisTrigger
            System.out.println("Running analysis...");
            List<EntityNode> nodes = AgentAnalysisTrigger.run(session);
            
            System.out.println("Analysis complete. Extracted " + nodes.size() + " entities.");
            
            // Print summary
            for (EntityNode node : nodes) {
                System.out.println("  - " + node.getName() + " (" + node.getPackageName() + ")");
                System.out.println("    Type: " + node.getType() + ", DDD Role: " + node.getDddRole());
                System.out.println("    Aggregate: " + node.getAggregateName());
                if (node.getRelationships() != null && !node.getRelationships().isEmpty()) {
                    System.out.println("    Relationships: " + node.getRelationships().size());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error during analysis:");
            e.printStackTrace();
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
            if (emf != null && emf.isOpen()) {
                emf.close();
            }
        }
    }
}