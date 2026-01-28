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
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ERP Stress Test - General Ledger Domain (Apache OFBiz style)
 * 
 * ERPs open-source (like Apache OFBiz or Broadleaf Commerce) often have:
 * - Huge, highly interconnected data models
 * - God Classes with many relationships
 * - Heavy ORM usage (Hibernate)
 * - Bidirectional relationships everywhere
 * 
 * Domain: General Accounting / General Ledger
 * Why: Adjacent to treasury, contains Journal, Entry, Account, Period concepts.
 * 
 * SUCCESS CRITERIA:
 * - Tool should suggest cutting bidirectional links between Invoice and
 * GeneralLedgerEntry
 * - Tool should NOT propose putting everything in a single giant cluster
 * - If heuristic is too weak, everything will be ONE aggregate (BAD!)
 */
public class ERPStressTestGeneralLedgerTest {

    private List<EntityNode> erpDomain;
    private DDDAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new DDDAnalyzer();
        erpDomain = createChaoticERPModel();
    }

    /**
     * Creates a "dirty" ERP model typical of legacy systems.
     * 
     * Problems simulated:
     * 1. God Class (GeneralLedgerAccount has too many relationships)
     * 2. Bidirectional relationships (Invoice <-> GeneralLedgerEntry)
     * 3. Weak/optional relationships everywhere
     * 4. Cross-module dependencies (Sales -> Accounting)
     */
    private List<EntityNode> createChaoticERPModel() {
        List<EntityNode> nodes = new ArrayList<>();

        // === GENERAL LEDGER MODULE (Accounting Core) ===

        // GeneralLedgerAccount - Potential God Class
        EntityNode glAccount = new EntityNode("GeneralLedgerAccount", "org.ofbiz.accounting.ledger", "ENTITY");
        List<RelationshipMetadata> glAccountRels = new ArrayList<>();

        // Many relationships making it a "hub"
        RelationshipMetadata glToEntries = new RelationshipMetadata(
                "entries", "GeneralLedgerEntry", "OneToMany");
        glToEntries.setCascadePersist(false); // Not owned
        glToEntries.setLazy(true);
        glAccountRels.add(glToEntries);

        RelationshipMetadata glToParent = new RelationshipMetadata(
                "parentAccount", "GeneralLedgerAccount", "ManyToOne");
        glToParent.setLazy(true);
        glAccountRels.add(glToParent);

        RelationshipMetadata glToChildren = new RelationshipMetadata(
                "childAccounts", "GeneralLedgerAccount", "OneToMany");
        glToChildren.setLazy(true);
        glAccountRels.add(glToChildren);

        RelationshipMetadata glToOrg = new RelationshipMetadata(
                "organization", "Organization", "ManyToOne");
        glToOrg.setLazy(true);
        glAccountRels.add(glToOrg);

        glAccount.setRelationships(glAccountRels);
        nodes.add(glAccount);

        // GeneralLedgerEntry - Core journal entry
        EntityNode glEntry = new EntityNode("GeneralLedgerEntry", "org.ofbiz.accounting.ledger", "ENTITY");
        List<RelationshipMetadata> glEntryRels = new ArrayList<>();

        RelationshipMetadata entryToAccount = new RelationshipMetadata(
                "account", "GeneralLedgerAccount", "ManyToOne");
        entryToAccount.setLazy(false); // Eager - always need account
        glEntryRels.add(entryToAccount);

        RelationshipMetadata entryToJournal = new RelationshipMetadata(
                "journal", "Journal", "ManyToOne");
        entryToJournal.setLazy(true);
        glEntryRels.add(entryToJournal);

        RelationshipMetadata entryToPeriod = new RelationshipMetadata(
                "accountingPeriod", "AccountingPeriod", "ManyToOne");
        entryToPeriod.setLazy(true);
        glEntryRels.add(entryToPeriod);

        // DIRTY: Bidirectional reference to Invoice
        RelationshipMetadata entryToInvoice = new RelationshipMetadata(
                "sourceInvoice", "Invoice", "ManyToOne");
        entryToInvoice.setLazy(true);
        entryToInvoice.setOptional(true);
        glEntryRels.add(entryToInvoice);

        glEntry.setRelationships(glEntryRels);
        nodes.add(glEntry);

        // Journal - Groups entries
        EntityNode journal = new EntityNode("Journal", "org.ofbiz.accounting.ledger", "ENTITY");
        List<RelationshipMetadata> journalRels = new ArrayList<>();

        RelationshipMetadata journalToEntries = new RelationshipMetadata(
                "entries", "GeneralLedgerEntry", "OneToMany");
        journalToEntries.setCascadePersist(true);
        journalToEntries.setCascadeRemove(true);
        journalToEntries.setLazy(true);
        journalRels.add(journalToEntries);

        journal.setRelationships(journalRels);
        nodes.add(journal);

        // AccountingPeriod - Reference data
        EntityNode period = new EntityNode("AccountingPeriod", "org.ofbiz.accounting.period", "ENTITY");
        period.setRelationships(new ArrayList<>());
        nodes.add(period);

        // === INVOICING MODULE ===

        // Invoice - Should be its own aggregate
        EntityNode invoice = new EntityNode("Invoice", "org.ofbiz.accounting.invoice", "ENTITY");
        List<RelationshipMetadata> invoiceRels = new ArrayList<>();

        RelationshipMetadata invToItems = new RelationshipMetadata(
                "items", "InvoiceItem", "OneToMany");
        invToItems.setCascadePersist(true);
        invToItems.setCascadeRemove(true);
        invToItems.setOrphanRemoval(true);
        invToItems.setLazy(false);
        invoiceRels.add(invToItems);

        RelationshipMetadata invToParty = new RelationshipMetadata(
                "party", "Party", "ManyToOne");
        invToParty.setLazy(true);
        invoiceRels.add(invToParty);

        // DIRTY: Bidirectional to GL (should be cut!)
        RelationshipMetadata invToGlEntries = new RelationshipMetadata(
                "glEntries", "GeneralLedgerEntry", "OneToMany");
        invToGlEntries.setCascadePersist(false); // At least not cascade
        invToGlEntries.setLazy(true);
        invoiceRels.add(invToGlEntries);

        invoice.setRelationships(invoiceRels);
        nodes.add(invoice);

        // InvoiceItem - Owned by Invoice
        EntityNode invoiceItem = new EntityNode("InvoiceItem", "org.ofbiz.accounting.invoice", "ENTITY");
        List<RelationshipMetadata> itemRels = new ArrayList<>();

        RelationshipMetadata itemToProduct = new RelationshipMetadata(
                "product", "Product", "ManyToOne");
        itemToProduct.setLazy(true);
        itemRels.add(itemToProduct);

        invoiceItem.setRelationships(itemRels);
        nodes.add(invoiceItem);

        // === PARTY MODULE (Cross-cutting) ===

        EntityNode party = new EntityNode("Party", "org.ofbiz.party.party", "ENTITY");
        List<RelationshipMetadata> partyRels = new ArrayList<>();

        RelationshipMetadata partyToRoles = new RelationshipMetadata(
                "roles", "PartyRole", "OneToMany");
        partyToRoles.setCascadePersist(true);
        partyToRoles.setLazy(true);
        partyRels.add(partyToRoles);

        RelationshipMetadata partyToContacts = new RelationshipMetadata(
                "contactMechanisms", "ContactMechanism", "OneToMany");
        partyToContacts.setCascadePersist(true);
        partyToContacts.setLazy(true);
        partyRels.add(partyToContacts);

        party.setRelationships(partyRels);
        nodes.add(party);

        EntityNode partyRole = new EntityNode("PartyRole", "org.ofbiz.party.party", "ENTITY");
        partyRole.setRelationships(new ArrayList<>());
        nodes.add(partyRole);

        EntityNode contactMech = new EntityNode("ContactMechanism", "org.ofbiz.party.contact", "ENTITY");
        contactMech.setRelationships(new ArrayList<>());
        nodes.add(contactMech);

        // === PRODUCT MODULE ===

        EntityNode product = new EntityNode("Product", "org.ofbiz.product.product", "ENTITY");
        List<RelationshipMetadata> productRels = new ArrayList<>();

        RelationshipMetadata prodToCategory = new RelationshipMetadata(
                "categories", "ProductCategory", "ManyToMany");
        prodToCategory.setLazy(true);
        productRels.add(prodToCategory);

        product.setRelationships(productRels);
        nodes.add(product);

        EntityNode productCategory = new EntityNode("ProductCategory", "org.ofbiz.product.category", "ENTITY");
        productCategory.setRelationships(new ArrayList<>());
        nodes.add(productCategory);

        // === ORGANIZATION MODULE ===

        EntityNode organization = new EntityNode("Organization", "org.ofbiz.party.organization", "ENTITY");
        organization.setRelationships(new ArrayList<>());
        nodes.add(organization);

        // === MORE DIRTY RELATIONSHIPS (Audit, Cross-references) ===

        EntityNode auditLog = new EntityNode("AuditLog", "org.ofbiz.common.audit", "ENTITY");
        List<RelationshipMetadata> auditRels = new ArrayList<>();

        // AuditLog has weak refs to many entities (typical ERP pattern)
        RelationshipMetadata auditToInvoice = new RelationshipMetadata(
                "relatedInvoice", "Invoice", "ManyToOne");
        auditToInvoice.setLazy(true);
        auditToInvoice.setOptional(true);
        auditRels.add(auditToInvoice);

        RelationshipMetadata auditToGlEntry = new RelationshipMetadata(
                "relatedGlEntry", "GeneralLedgerEntry", "ManyToOne");
        auditToGlEntry.setLazy(true);
        auditToGlEntry.setOptional(true);
        auditRels.add(auditToGlEntry);

        auditLog.setRelationships(auditRels);
        nodes.add(auditLog);

        return nodes;
    }

    @Test
    @DisplayName("STRESS: Should NOT create single giant aggregate")
    void shouldNotCreateSingleGiantAggregate() {
        analyzer.analyze(erpDomain);

        // Count distinct aggregates
        Set<String> distinctAggregates = erpDomain.stream()
                .map(EntityNode::getAggregateName)
                .collect(Collectors.toSet());

        System.out.println("Aggregates detected: " + distinctAggregates);

        // If everything is in ONE aggregate, the heuristic failed!
        assertTrue(distinctAggregates.size() > 1,
                "ERP model should NOT collapse into a single giant aggregate! Found: " + distinctAggregates);

        // We expect at least 3-4 distinct bounded contexts:
        // Ledger, Invoice, Party, Product
        assertTrue(distinctAggregates.size() >= 3,
                "Should detect at least 3 distinct aggregates (Ledger, Invoice, Party...), found: "
                        + distinctAggregates.size());
    }

    @Test
    @DisplayName("STRESS: Invoice and GeneralLedgerEntry should be in DIFFERENT aggregates")
    void invoiceAndGlEntryShouldBeInDifferentAggregates() {
        analyzer.analyze(erpDomain);

        EntityNode invoice = findEntity("Invoice");
        EntityNode glEntry = findEntity("GeneralLedgerEntry");

        assertNotNull(invoice);
        assertNotNull(glEntry);

        // These are in different modules with different lifecycles
        // The bidirectional link should be identified as a "cut" candidate
        assertNotEquals(invoice.getAggregateName(), glEntry.getAggregateName(),
                "Invoice and GeneralLedgerEntry should be in DIFFERENT aggregates - " +
                        "their bidirectional link should be cut based on module boundaries");
    }

    @Test
    @DisplayName("STRESS: Invoice should own InvoiceItem (cascade relationship)")
    void invoiceShouldOwnInvoiceItem() {
        analyzer.analyze(erpDomain);

        EntityNode invoice = findEntity("Invoice");
        EntityNode invoiceItem = findEntity("InvoiceItem");

        assertNotNull(invoice);
        assertNotNull(invoiceItem);

        assertEquals(invoice.getAggregateName(), invoiceItem.getAggregateName(),
                "InvoiceItem should be in Invoice's aggregate (cascade ownership)");
    }

    @Test
    @DisplayName("STRESS: Journal should be its own aggregate root")
    void journalShouldBeAggregateRoot() {
        analyzer.analyze(erpDomain);

        EntityNode journal = findEntity("Journal");
        assertNotNull(journal);

        assertEquals("AGGREGATE_ROOT", journal.getDddRole(),
                "Journal should be AGGREGATE_ROOT - it owns GeneralLedgerEntry");
    }

    @Test
    @DisplayName("STRESS: Party should be separate from accounting")
    void partyShouldBeSeparateFromAccounting() {
        analyzer.analyze(erpDomain);

        EntityNode party = findEntity("Party");
        EntityNode invoice = findEntity("Invoice");
        EntityNode glEntry = findEntity("GeneralLedgerEntry");

        assertNotNull(party);
        assertNotNull(invoice);
        assertNotNull(glEntry);

        // Party is a cross-cutting concern - should be its own aggregate
        assertNotEquals(party.getAggregateName(), invoice.getAggregateName(),
                "Party should NOT be in Invoice's aggregate - it's a cross-cutting concern");
    }

    @Test
    @DisplayName("STRESS: AuditLog should NOT pull entities into its aggregate")
    void auditLogShouldNotBeGravitationalCenter() {
        analyzer.analyze(erpDomain);

        EntityNode auditLog = findEntity("AuditLog");
        assertNotNull(auditLog);

        // AuditLog has weak refs but shouldn't pull anything
        // It should NOT be an aggregate root
        assertNotEquals("AGGREGATE_ROOT", auditLog.getDddRole(),
                "AuditLog should NOT be AGGREGATE_ROOT - it's a cross-cutting logger with weak references");
    }

    @Test
    @DisplayName("STRESS: Count God Classes (entities with > 3 relationships)")
    void identifyGodClasses() {
        // This is a diagnostic test, not pass/fail
        System.out.println("\n=== GOD CLASS DETECTION ===\n");

        for (EntityNode node : erpDomain) {
            int relCount = node.getRelationships() != null ? node.getRelationships().size() : 0;
            if (relCount > 3) {
                System.out.println("⚠️  Potential God Class: " + node.getName() +
                        " (" + relCount + " relationships)");
            }
        }
    }

    @Test
    @DisplayName("HEURISTIC VALIDATION: Summary of ERP Analysis")
    void printERPAnalysisSummary() {
        analyzer.analyze(erpDomain);

        System.out.println("\n=== ERP STRESS TEST ANALYSIS ===\n");

        // Group by aggregate
        Map<String, List<EntityNode>> aggregates = erpDomain.stream()
                .collect(Collectors.groupingBy(EntityNode::getAggregateName));

        aggregates.forEach((aggName, entities) -> {
            System.out.println("Aggregate: " + aggName + " (" + entities.size() + " entities)");
            entities.forEach(e -> {
                int rels = e.getRelationships() != null ? e.getRelationships().size() : 0;
                System.out.println("  - " + e.getName() + " [" + e.getDddRole() + "] (" + rels + " rels)");
            });
            System.out.println();
        });

        // Metrics
        System.out.println("=== METRICS ===");
        System.out.println("Total entities: " + erpDomain.size());
        System.out.println("Total aggregates: " + aggregates.size());

        long roots = erpDomain.stream().filter(e -> "AGGREGATE_ROOT".equals(e.getDddRole())).count();
        System.out.println("Aggregate roots: " + roots);

        // Largest aggregate (potential "God Aggregate")
        int largest = aggregates.values().stream().mapToInt(List::size).max().orElse(0);
        System.out.println("Largest aggregate size: " + largest);

        if (largest > erpDomain.size() / 2) {
            System.out.println("⚠️  WARNING: Largest aggregate contains >50% of entities - heuristic may be too weak!");
        }
    }

    private EntityNode findEntity(String name) {
        return erpDomain.stream()
                .filter(e -> name.equals(e.getName()))
                .findFirst()
                .orElse(null);
    }
}
