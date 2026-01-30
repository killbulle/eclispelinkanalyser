package com.eclipselink.analyzer.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.eclipse.persistence.sessions.Session;

import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.Set;
import java.util.IdentityHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AnalyzerAgent {

    private static final Set<Session> analyzedSessions = Collections.newSetFromMap(new IdentityHashMap<>());

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[AnalyzerAgent] Premain started. Instrumenting EclipseLink...");
        install(inst);
        startBackgroundScanner();
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("[AnalyzerAgent] Agentmain started. Attaching to running JVM...");
        install(inst);

        // Scan for existing sessions for "a posteriori" extraction
        scanForExistingSessions();
    }

    private static void install(Instrumentation inst) {
        new AgentBuilder.Default()
                .with(AgentBuilder.Listener.StreamWriting.toSystemOut())
                .type(ElementMatchers.named("org.eclipse.persistence.internal.sessions.DatabaseSessionImpl"))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> builder
                        .visit(net.bytebuddy.asm.Advice.to(SessionAdvice.class).on(ElementMatchers.named("logout"))))
                .installOn(inst);
    }

    private static void scanForExistingSessions() {
        System.out.println("[AnalyzerAgent] Scanning for existing EclipseLink sessions...");
        try {
            // Attempt to find sessions via SessionManager
            Class<?> managerClass = Class.forName("org.eclipse.persistence.sessions.factories.SessionManager");
            Object manager = managerClass.getMethod("getManager").invoke(null);
            java.util.Map<?, ?> sessions = (java.util.Map<?, ?>) managerClass.getMethod("getSessions").invoke(manager);

            if (sessions != null && !sessions.isEmpty()) {
                System.out.println("[AnalyzerAgent] Found " + sessions.size() + " existing sessions.");
                for (Object sessionObj : sessions.values()) {
                    if (sessionObj instanceof Session) {
                        triggerAnalysis((Session) sessionObj);
                    }
                }
            } else {
                System.out.println("[AnalyzerAgent] No existing sessions found in SessionManager.");
            }
        } catch (Throwable t) {
            System.err.println("[AnalyzerAgent] Failed to scan existing sessions: " + t.getMessage());
        }
    }

    public static void triggerAnalysis(Session session) {
        synchronized (analyzedSessions) {
            if (analyzedSessions.contains(session)) {
                return;
            }
            analyzedSessions.add(session);
        }

        System.out.println("[AnalyzerAgent] Triggering analysis for session: " + session.getName());
        try {
            com.eclipselink.analyzer.agent.AgentAnalysisTrigger.run(session);
        } catch (Exception e) {
            System.err.println("[AnalyzerAgent] Analysis failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void startBackgroundScanner() {
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AnalyzerAgent-Scanner");
            t.setDaemon(true);
            return t;
        }).scheduleAtFixedRate(AnalyzerAgent::scanForExistingSessions, 1, 1, TimeUnit.SECONDS);
    }

    public static class SessionAdvice {
        @net.bytebuddy.asm.Advice.OnMethodEnter
        public static void onEnter(@net.bytebuddy.asm.Advice.This Object sessionObj) {
            System.out.println(
                    "[AnalyzerAgent] SessionAdvice.onEnter (logout) CALLED with " + sessionObj.getClass().getName());
            try {
                if (sessionObj instanceof Session) {
                    Session session = (Session) sessionObj;
                    System.out.println("[AnalyzerAgent] EclipseLink Session detected: " + session.getName());
                    triggerAnalysis(session);
                }
            } catch (Throwable t) {
                System.err.println("[AnalyzerAgent] Error in advice: " + t.getMessage());
            }
        }
    }
}
