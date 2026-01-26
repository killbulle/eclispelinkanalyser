package com.eclipselink.analyzer.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.eclipse.persistence.sessions.Session;

import java.lang.instrument.Instrumentation;

public class AnalyzerAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[AnalyzerAgent] Premain started. Instrumenting EclipseLink...");

        new AgentBuilder.Default()
                .type(ElementMatchers.named("org.eclipse.persistence.internal.sessions.DatabaseSessionImpl"))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> builder
                        .method(ElementMatchers.named("postLogin"))
                        .intercept(MethodDelegation.to(SessionInterceptor.class)))
                .installOn(inst);
    }

    public static class SessionInterceptor {
        public static void postLogin(@net.bytebuddy.implementation.bind.annotation.This Object sessionObj) {
            try {
                if (sessionObj instanceof Session) {
                    Session session = (Session) sessionObj;
                    System.out.println("[AnalyzerAgent] EclipseLink Session detected: " + session.getName());

                    // Trigger analysis in a separate thread to avoid blocking application startup
                    new Thread(() -> {
                        try {
                            System.out.println("[AnalyzerAgent] Starting analysis for session: " + session.getName());
                            com.eclipselink.analyzer.agent.AgentAnalysisTrigger.run(session);
                        } catch (Exception e) {
                            System.err.println("[AnalyzerAgent] Analysis failed: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }).start();
                }
            } catch (Throwable t) {
                System.err.println("[AnalyzerAgent] Error in interceptor: " + t.getMessage());
            }
        }
    }
}
