package com.eclipselink.analyzer.ddd;

import com.eclipselink.analyzer.DDDAnalyzer;
import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Calibration Test - Same model as genpy.py
 * 
 * This test replicates the exact model from genpy.py to compare
 * Python (Louvain) vs Java (DDDAnalyzer) heuristics.
 * 
 * Model from genpy.py:
 * - Cluster Trésorerie: CashAccount -> AccountEntry -> MoneyAmount -> Currency
 * - Cluster Tiers: Counterparty -> PostalAddress
 * - Dirty links: AccountEntry -> Counterparty (ASSOCIATION)
 * - Weak links: CashAccount -> AuditLog, Counterparty -> AuditLog
 * 
 * Python (Louvain) results:
 * - Cluster 0: [CashAccount, AccountEntry] - AccountEntry as root (WRONG)
 * - Cluster 1: [Counterparty, PostalAddress, AuditLog] - AuditLog as root
 * (WRONG)
 * - Cluster 2: [MoneyAmount, Currency] - MoneyAmount as root
 * 
 * Expected Java results (DDDAnalyzer):
 * - CashAccount should be AGGREGATE_ROOT (controls AccountEntry lifecycle)
 * - Currency should be VALUE_OBJECT or leaf
 * - Counterparty should be AGGREGATE_ROOT (owns PostalAddress)
 */
public class GenPyCalibrationTest {

        private List<EntityNode> treasuryModel;
        private DDDAnalyzer analyzer;

        @BeforeEach
        void setUp() {
                analyzer = new DDDAnalyzer();
                treasuryModel = createGenPyModel();
        }

        /**
         * Creates the exact model from genpy.py
         * 
         * RELATION_WEIGHTS in Python:
         * - COMPOSITION: 1.0 (Cascade ALL)
         * - AGGREGATION: 0.6 (Eager Loading)
         * - ASSOCIATION: 0.2 (Lazy Loading)
         * - WEAK: 0.1 (ManyToMany or Optional)
         */
        private List<EntityNode> createGenPyModel() {
                List<EntityNode> nodes = new ArrayList<>();

                // === Cluster Trésorerie ===

                // CashAccount (method_count=20 -> rich behavior)
                EntityNode cashAccount = new EntityNode("CashAccount", "com.treasury.account", "ENTITY");
                List<RelationshipMetadata> cashAccountRels = new ArrayList<>();

                // CashAccount -> AccountEntry (COMPOSITION - Cascade ALL)
                RelationshipMetadata caToEntry = new RelationshipMetadata(
                                "entries", "AccountEntry", "OneToMany");
                caToEntry.setCascadePersist(true);
                caToEntry.setCascadeRemove(true);
                caToEntry.setCascadeAll(true);
                caToEntry.setOrphanRemoval(true);
                caToEntry.setLazy(false); // Eager = COMPOSITION
                cashAccountRels.add(caToEntry);

                // CashAccount -> AuditLog (WEAK)
                RelationshipMetadata caToAudit = new RelationshipMetadata(
                                "auditLog", "AuditLog", "ManyToMany");
                caToAudit.setLazy(true);
                caToAudit.setOptional(true);
                // No cascade = WEAK
                cashAccountRels.add(caToAudit);

                cashAccount.setRelationships(cashAccountRels);
                nodes.add(cashAccount);

                // AccountEntry (method_count=5)
                EntityNode accountEntry = new EntityNode("AccountEntry", "com.treasury.account", "ENTITY");
                List<RelationshipMetadata> entryRels = new ArrayList<>();

                // AccountEntry -> MoneyAmount (AGGREGATION - Eager)
                RelationshipMetadata entryToAmount = new RelationshipMetadata(
                                "amount", "MoneyAmount", "OneToOne");
                entryToAmount.setCascadePersist(true);
                entryToAmount.setLazy(false); // Eager
                entryRels.add(entryToAmount);

                // AccountEntry -> Counterparty (ASSOCIATION - Lazy, this is the "dirty" link to
                // cut)
                RelationshipMetadata entryToCounterparty = new RelationshipMetadata(
                                "counterparty", "Counterparty", "ManyToOne");
                entryToCounterparty.setLazy(true);
                // No cascade = ASSOCIATION
                entryRels.add(entryToCounterparty);

                accountEntry.setRelationships(entryRels);
                nodes.add(accountEntry);

                // MoneyAmount (method_count=0 -> anemic, should be VO)
                EntityNode moneyAmount = new EntityNode("MoneyAmount", "com.treasury.value", "EMBEDDABLE");
                List<RelationshipMetadata> amountRels = new ArrayList<>();

                // MoneyAmount -> Currency (AGGREGATION)
                RelationshipMetadata amountToCurrency = new RelationshipMetadata(
                                "currency", "Currency", "ManyToOne");
                amountToCurrency.setLazy(false); // Eager
                amountRels.add(amountToCurrency);

                moneyAmount.setRelationships(amountRels);
                nodes.add(moneyAmount);

                // Currency (method_count=0 -> anemic, leaf node, should be VO)
                EntityNode currency = new EntityNode("Currency", "com.treasury.value", "EMBEDDABLE");
                currency.setRelationships(new ArrayList<>()); // No outgoing relationships
                nodes.add(currency);

                // === Cluster Tiers (Counterparty) ===

                // Counterparty (method_count=15)
                EntityNode counterparty = new EntityNode("Counterparty", "com.party.tiers", "ENTITY");
                List<RelationshipMetadata> partyRels = new ArrayList<>();

                // Counterparty -> PostalAddress (COMPOSITION)
                RelationshipMetadata partyToAddress = new RelationshipMetadata(
                                "address", "PostalAddress", "OneToOne");
                partyToAddress.setCascadePersist(true);
                partyToAddress.setCascadeRemove(true);
                partyToAddress.setOrphanRemoval(true);
                partyToAddress.setLazy(false);
                partyRels.add(partyToAddress);

                // Counterparty -> AuditLog (WEAK)
                RelationshipMetadata partyToAudit = new RelationshipMetadata(
                                "auditLog", "AuditLog", "ManyToMany");
                partyToAudit.setLazy(true);
                partyToAudit.setOptional(true);
                partyRels.add(partyToAudit);

                counterparty.setRelationships(partyRels);
                nodes.add(counterparty);

                // PostalAddress (method_count=1 -> almost anemic)
                EntityNode postalAddress = new EntityNode("PostalAddress", "com.party.tiers", "EMBEDDABLE");
                postalAddress.setRelationships(new ArrayList<>()); // Leaf
                nodes.add(postalAddress);

                // === Cross-cutting: AuditLog ===

                // AuditLog (method_count=2)
                EntityNode auditLog = new EntityNode("AuditLog", "com.common.audit", "ENTITY");
                auditLog.setRelationships(new ArrayList<>()); // Leaf - receives refs but doesn't own anything
                nodes.add(auditLog);

                return nodes;
        }

        @Test
        @DisplayName("CALIBRATION: CashAccount should be AGGREGATE_ROOT (not AccountEntry)")
        void cashAccountShouldBeAggregateRoot() {
                analyzer.analyze(treasuryModel);

                EntityNode cashAccount = findEntity("CashAccount");
                EntityNode accountEntry = findEntity("AccountEntry");

                assertNotNull(cashAccount);
                assertNotNull(accountEntry);

                // CashAccount has CASCADE ALL to AccountEntry -> CashAccount is the root
                assertEquals("AGGREGATE_ROOT", cashAccount.getDddRole(),
                                "CashAccount should be AGGREGATE_ROOT - it has CASCADE ALL to AccountEntry");

                // AccountEntry should NOT be root (it's owned by CashAccount)
                assertNotEquals("AGGREGATE_ROOT", accountEntry.getDddRole(),
                                "AccountEntry should NOT be AGGREGATE_ROOT - it's cascade-owned by CashAccount");
        }

        @Test
        @DisplayName("CALIBRATION: Counterparty should be AGGREGATE_ROOT")
        void counterpartyShouldBeAggregateRoot() {
                analyzer.analyze(treasuryModel);

                EntityNode counterparty = findEntity("Counterparty");
                assertNotNull(counterparty);

                assertEquals("AGGREGATE_ROOT", counterparty.getDddRole(),
                                "Counterparty should be AGGREGATE_ROOT - it has CASCADE to PostalAddress");
        }

        @Test
        @DisplayName("CALIBRATION: AuditLog should NOT be AGGREGATE_ROOT")
        void auditLogShouldNotBeAggregateRoot() {
                analyzer.analyze(treasuryModel);

                EntityNode auditLog = findEntity("AuditLog");
                assertNotNull(auditLog);

                // AuditLog is just a receiver of weak references, it owns nothing
                assertNotEquals("AGGREGATE_ROOT", auditLog.getDddRole(),
                                "AuditLog should NOT be AGGREGATE_ROOT - it's a cross-cutting logger with no ownership");
        }

        @Test
        @DisplayName("CALIBRATION: MoneyAmount and Currency should be VALUE_OBJECT")
        void valueObjectsShouldBeIdentified() {
                analyzer.analyze(treasuryModel);

                EntityNode moneyAmount = findEntity("MoneyAmount");
                EntityNode currency = findEntity("Currency");

                assertNotNull(moneyAmount);
                assertNotNull(currency);

                // Both are EMBEDDABLE -> should be VALUE_OBJECT
                assertEquals("VALUE_OBJECT", moneyAmount.getDddRole(),
                                "MoneyAmount should be VALUE_OBJECT (EMBEDDABLE)");
                assertEquals("VALUE_OBJECT", currency.getDddRole(),
                                "Currency should be VALUE_OBJECT (EMBEDDABLE)");
        }

        @Test
        @DisplayName("CALIBRATION: PostalAddress should be VALUE_OBJECT")
        void postalAddressShouldBeValueObject() {
                analyzer.analyze(treasuryModel);

                EntityNode postalAddress = findEntity("PostalAddress");
                assertNotNull(postalAddress);

                assertEquals("VALUE_OBJECT", postalAddress.getDddRole(),
                                "PostalAddress should be VALUE_OBJECT (EMBEDDABLE, leaf)");
        }

        @Test
        @DisplayName("CALIBRATION: CashAccount and Counterparty should be in DIFFERENT aggregates")
        void separateAggregates() {
                analyzer.analyze(treasuryModel);

                EntityNode cashAccount = findEntity("CashAccount");
                EntityNode counterparty = findEntity("Counterparty");

                assertNotNull(cashAccount);
                assertNotNull(counterparty);

                // Different packages, different lifecycle -> different aggregates
                assertNotEquals(cashAccount.getAggregateName(), counterparty.getAggregateName(),
                                "CashAccount and Counterparty should be in DIFFERENT aggregates (different bounded contexts)");
        }

        @Test
        @DisplayName("CALIBRATION: AccountEntry should be in CashAccount's aggregate")
        void accountEntrySameAggregateAsCashAccount() {
                analyzer.analyze(treasuryModel);

                EntityNode cashAccount = findEntity("CashAccount");
                EntityNode accountEntry = findEntity("AccountEntry");

                assertNotNull(cashAccount);
                assertNotNull(accountEntry);

                assertEquals(cashAccount.getAggregateName(), accountEntry.getAggregateName(),
                                "AccountEntry should be in CashAccount's aggregate (cascade ownership)");
        }

        @Test
        @DisplayName("CALIBRATION SUMMARY: Compare Java vs Python heuristics")
        void printCalibrationSummary() {
                analyzer.analyze(treasuryModel);

                System.out.println("\n=== CALIBRATION: Java DDDAnalyzer vs Python Louvain ===\n");

                System.out.println(
                                "| Entity         | Java Role        | Java Aggregate    | Python Cluster | Python Root |");
                System.out.println(
                                "|----------------|------------------|-------------------|----------------|-------------|");

                // Map of Python results for comparison
                Map<String, String> pythonRoots = new java.util.HashMap<>();
                pythonRoots.put("CashAccount", "Cluster 0");
                pythonRoots.put("AccountEntry", "Cluster 0 (ROOT)");
                pythonRoots.put("MoneyAmount", "Cluster 2 (ROOT)");
                pythonRoots.put("Currency", "Cluster 2");
                pythonRoots.put("Counterparty", "Cluster 1");
                pythonRoots.put("PostalAddress", "Cluster 1");
                pythonRoots.put("AuditLog", "Cluster 1 (ROOT)");

                for (EntityNode node : treasuryModel) {
                        String pythonInfo = pythonRoots.getOrDefault(node.getName(), "?");
                        System.out.printf("| %-14s | %-16s | %-17s | %-14s |%n",
                                        node.getName(),
                                        node.getDddRole(),
                                        node.getAggregateName(),
                                        pythonInfo);
                }

                System.out.println();

                // Summary stats
                long javaRoots = treasuryModel.stream()
                                .filter(e -> "AGGREGATE_ROOT".equals(e.getDddRole()))
                                .count();
                long javaVOs = treasuryModel.stream()
                                .filter(e -> "VALUE_OBJECT".equals(e.getDddRole()))
                                .count();
                long distinctAggregates = treasuryModel.stream()
                                .map(EntityNode::getAggregateName)
                                .distinct()
                                .count();

                System.out.println("=== JAVA RESULTS ===");
                System.out.println("Aggregate Roots: " + javaRoots);
                System.out.println("Value Objects: " + javaVOs);
                System.out.println("Distinct Aggregates: " + distinctAggregates);

                System.out.println("\n=== PYTHON (Louvain) RESULTS ===");
                System.out.println("Clusters: 3");
                System.out.println("Roots Identified: AccountEntry (wrong), AuditLog (wrong), MoneyAmount");
                System.out.println(
                                "Suggested Cuts: CashAccount->AuditLog, AccountEntry->MoneyAmount, AccountEntry->Counterparty");

                // Pass/Fail summary
                System.out.println("\n=== HEURISTIC COMPARISON ===");
                EntityNode ca = findEntity("CashAccount");
                EntityNode cp = findEntity("Counterparty");
                EntityNode al = findEntity("AuditLog");

                System.out.println("CashAccount as ROOT: Java=" +
                                ("AGGREGATE_ROOT".equals(ca.getDddRole()) ? "✅" : "❌") +
                                " | Python=❌");
                System.out.println("Counterparty as ROOT: Java=" +
                                ("AGGREGATE_ROOT".equals(cp.getDddRole()) ? "✅" : "❌") +
                                " | Python=❌");
                System.out.println("AuditLog NOT ROOT: Java=" +
                                (!"AGGREGATE_ROOT".equals(al.getDddRole()) ? "✅" : "❌") +
                                " | Python=❌");
        }

        private EntityNode findEntity(String name) {
                return treasuryModel.stream()
                                .filter(e -> name.equals(e.getName()))
                                .findFirst()
                                .orElse(null);
        }
}
