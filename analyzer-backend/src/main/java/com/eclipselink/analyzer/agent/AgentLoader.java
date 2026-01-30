package com.eclipselink.analyzer.agent;

import net.bytebuddy.agent.ByteBuddyAgent;
import java.io.File;

public class AgentLoader {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println(
                    "Usage: java -cp analyzer-backend.jar com.eclipselink.analyzer.agent.AgentLoader <PID> [agentJarPath]");
            System.exit(1);
        }

        String pid = args[0];
        String agentJarPath = args.length > 1 ? args[1] : findAgentJar();

        if (agentJarPath == null || !new File(agentJarPath).exists()) {
            System.err.println("Could not find agent jar at: " + agentJarPath);
            System.exit(1);
        }

        System.out.println("Attaching agent " + agentJarPath + " to process " + pid + "...");
        try {
            ByteBuddyAgent.attach(new File(agentJarPath), pid);
            System.out.println("Agent attached successfully!");
        } catch (Exception e) {
            System.err.println("Failed to attach agent: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String findAgentJar() {
        // Try current directory or target directory
        String[] paths = {
                "analyzer-backend-1.0-SNAPSHOT.jar",
                "target/analyzer-backend-1.0-SNAPSHOT.jar",
                "analyzer-backend.jar"
        };
        for (String path : paths) {
            if (new File(path).exists()) {
                return new File(path).getAbsolutePath();
            }
        }
        return null;
    }
}
