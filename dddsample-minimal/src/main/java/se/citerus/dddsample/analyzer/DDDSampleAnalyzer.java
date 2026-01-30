package se.citerus.dddsample.analyzer;

import com.eclipselink.analyzer.agent.AgentAnalysisTrigger;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.eclipse.persistence.jpa.JpaHelper;
import org.eclipse.persistence.sessions.Session;

public class DDDSampleAnalyzer {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting DDDSample EclipseLink Analyzer...");
        
        // Create EntityManagerFactory using persistence.xml
        EntityManagerFactory emf = null;
        try {
            emf = Persistence.createEntityManagerFactory("dddsample");
            System.out.println("EntityManagerFactory created successfully.");
            
            // Get EclipseLink Session
            Session session = JpaHelper.getEntityManagerFactory(emf).unwrap(Session.class);
            System.out.println("EclipseLink Session obtained.");
            
            // Trigger analyzer agent
            System.out.println("Starting agent analysis...");
            AgentAnalysisTrigger.run(session);
            System.out.println("Agent analysis completed.");
            
        } catch (Exception e) {
            System.err.println("Error during analysis: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (emf != null && emf.isOpen()) {
                emf.close();
                System.out.println("EntityManagerFactory closed.");
            }
        }
        
        System.out.println("Analysis report generated in reports/agent-report.json");
    }
}