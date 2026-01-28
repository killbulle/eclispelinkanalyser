package com.eclipselink.analyzer.ddd;

import com.eclipselink.analyzer.DDDAnalyzer;
import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DDD Sample Shipping Domain Test (Eric Evans "Control Group")
 * 
 * This is the academic standard for DDD. The ideal structure is known.
 * If the analyzer doesn't find the aggregates defined in this model,
 * the weightings are wrong.
 * 
 * Domain: Logistics / Cargo Transport
 * Source: DDDSample project (Eric Evans)
 * 
 * Expected Results:
 * - Cargo should be identified as AGGREGATE_ROOT
 * - DeliveryHistory should be an internal ENTITY within Cargo aggregate
 * - Leg should be an internal ENTITY within Cargo aggregate
 * - Location should be an external reference (VALUE_OBJECT or separate
 * aggregate)
 * - Itinerary should be a VALUE_OBJECT (immutable route)
 */
public class DDDSampleShippingTest {

    private List<EntityNode> shippingDomain;
    private DDDAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new DDDAnalyzer();
        shippingDomain = createShippingDomainModel();
    }

    /**
     * Creates the DDD Sample Shipping domain as per Eric Evans' book.
     * 
     * Structure:
     * - Cargo (Aggregate Root)
     * ├── DeliveryHistory (owned Entity)
     * ├── Itinerary (Value Object - immutable route)
     * └── Leg[] (Value Objects within Itinerary)
     * 
     * - Location (Separate Aggregate or Reference Data)
     * - Voyage (Reference to scheduling domain)
     */
    private List<EntityNode> createShippingDomainModel() {
        List<EntityNode> nodes = new ArrayList<>();

        // === CARGO AGGREGATE ===

        // Cargo - The main aggregate root
        EntityNode cargo = new EntityNode("Cargo", "se.citerus.dddsample.domain.model.cargo", "ENTITY");
        List<RelationshipMetadata> cargoRelations = new ArrayList<>();

        // Cargo -> DeliveryHistory (OneToOne, CASCADE ALL, orphanRemoval)
        RelationshipMetadata cargoToDeliveryHistory = new RelationshipMetadata(
                "deliveryHistory", "DeliveryHistory", "OneToOne");
        cargoToDeliveryHistory.setCascadePersist(true);
        cargoToDeliveryHistory.setCascadeRemove(true);
        cargoToDeliveryHistory.setOrphanRemoval(true);
        cargoToDeliveryHistory.setLazy(false); // Eager - always loaded with Cargo
        cargoRelations.add(cargoToDeliveryHistory);

        // Cargo -> Itinerary (Embedded/ValueObject)
        RelationshipMetadata cargoToItinerary = new RelationshipMetadata(
                "itinerary", "Itinerary", "OneToOne");
        cargoToItinerary.setCascadePersist(true);
        cargoToItinerary.setCascadeRemove(true);
        cargoToItinerary.setLazy(false);
        cargoRelations.add(cargoToItinerary);

        // Cargo -> RouteSpecification (Embedded VO)
        RelationshipMetadata cargoToRouteSpec = new RelationshipMetadata(
                "routeSpecification", "RouteSpecification", "OneToOne");
        cargoToRouteSpec.setCascadePersist(true);
        cargoToRouteSpec.setCascadeRemove(true);
        cargoToRouteSpec.setLazy(false);
        cargoRelations.add(cargoToRouteSpec);

        // Cargo -> Origin Location (Reference - NOT cascade)
        RelationshipMetadata cargoToOrigin = new RelationshipMetadata(
                "origin", "Location", "ManyToOne");
        cargoToOrigin.setLazy(true); // Lazy reference
        cargoToOrigin.setCascadePersist(false); // NO cascade - just a reference
        cargoRelations.add(cargoToOrigin);

        cargo.setRelationships(cargoRelations);
        nodes.add(cargo);

        // DeliveryHistory - Internal entity within Cargo aggregate
        EntityNode deliveryHistory = new EntityNode("DeliveryHistory", "se.citerus.dddsample.domain.model.cargo",
                "ENTITY");
        List<RelationshipMetadata> dhRelations = new ArrayList<>();

        // DeliveryHistory -> HandlingEvent (OneToMany - past events)
        RelationshipMetadata dhToEvents = new RelationshipMetadata(
                "eventsOrderedByCompletionTime", "HandlingEvent", "OneToMany");
        dhToEvents.setCascadePersist(true);
        dhToEvents.setCascadeRemove(true);
        dhToEvents.setLazy(true);
        dhRelations.add(dhToEvents);

        deliveryHistory.setRelationships(dhRelations);
        nodes.add(deliveryHistory);

        // Itinerary - Value Object (immutable route)
        EntityNode itinerary = new EntityNode("Itinerary", "se.citerus.dddsample.domain.model.cargo", "EMBEDDABLE");
        List<RelationshipMetadata> itineraryRelations = new ArrayList<>();

        // Itinerary -> Leg (OneToMany - ordered list of legs)
        RelationshipMetadata itineraryToLegs = new RelationshipMetadata(
                "legs", "Leg", "OneToMany");
        itineraryToLegs.setCascadePersist(true);
        itineraryToLegs.setCascadeRemove(true);
        itineraryToLegs.setOrphanRemoval(true);
        itineraryToLegs.setLazy(false);
        itineraryRelations.add(itineraryToLegs);

        itinerary.setRelationships(itineraryRelations);
        nodes.add(itinerary);

        // Leg - Value Object (one segment of the route)
        EntityNode leg = new EntityNode("Leg", "se.citerus.dddsample.domain.model.cargo", "EMBEDDABLE");
        List<RelationshipMetadata> legRelations = new ArrayList<>();

        // Leg -> Voyage (reference)
        RelationshipMetadata legToVoyage = new RelationshipMetadata(
                "voyage", "Voyage", "ManyToOne");
        legToVoyage.setLazy(true);
        legRelations.add(legToVoyage);

        // Leg -> LoadLocation (reference)
        RelationshipMetadata legToLoadLocation = new RelationshipMetadata(
                "loadLocation", "Location", "ManyToOne");
        legToLoadLocation.setLazy(true);
        legRelations.add(legToLoadLocation);

        // Leg -> UnloadLocation (reference)
        RelationshipMetadata legToUnloadLocation = new RelationshipMetadata(
                "unloadLocation", "Location", "ManyToOne");
        legToUnloadLocation.setLazy(true);
        legRelations.add(legToUnloadLocation);

        leg.setRelationships(legRelations);
        nodes.add(leg);

        // RouteSpecification - Value Object (criteria for route)
        EntityNode routeSpec = new EntityNode("RouteSpecification", "se.citerus.dddsample.domain.model.cargo",
                "EMBEDDABLE");
        List<RelationshipMetadata> routeSpecRelations = new ArrayList<>();

        // References to locations
        RelationshipMetadata rsToOrigin = new RelationshipMetadata(
                "origin", "Location", "ManyToOne");
        rsToOrigin.setLazy(true);
        routeSpecRelations.add(rsToOrigin);

        RelationshipMetadata rsToDestination = new RelationshipMetadata(
                "destination", "Location", "ManyToOne");
        rsToDestination.setLazy(true);
        routeSpecRelations.add(rsToDestination);

        routeSpec.setRelationships(routeSpecRelations);
        nodes.add(routeSpec);

        // HandlingEvent - Entity within DeliveryHistory
        EntityNode handlingEvent = new EntityNode("HandlingEvent", "se.citerus.dddsample.domain.model.handling",
                "ENTITY");
        List<RelationshipMetadata> heRelations = new ArrayList<>();

        // HandlingEvent -> Location (reference)
        RelationshipMetadata heToLocation = new RelationshipMetadata(
                "location", "Location", "ManyToOne");
        heToLocation.setLazy(true);
        heRelations.add(heToLocation);

        // HandlingEvent -> Voyage (optional reference)
        RelationshipMetadata heToVoyage = new RelationshipMetadata(
                "voyage", "Voyage", "ManyToOne");
        heToVoyage.setLazy(true);
        heToVoyage.setOptional(true);
        heRelations.add(heToVoyage);

        handlingEvent.setRelationships(heRelations);
        nodes.add(handlingEvent);

        // === LOCATION (Reference Data / Separate Bounded Context) ===

        // Location - Should NOT be part of Cargo aggregate
        EntityNode location = new EntityNode("Location", "se.citerus.dddsample.domain.model.location", "ENTITY");
        // Location has no outgoing cascade relationships (it's referenced, not owner)
        location.setRelationships(new ArrayList<>());
        nodes.add(location);

        // === VOYAGE (Scheduling Domain - Separate Bounded Context) ===

        // Voyage - Should be its own aggregate root or external reference
        EntityNode voyage = new EntityNode("Voyage", "se.citerus.dddsample.domain.model.voyage", "ENTITY");
        List<RelationshipMetadata> voyageRelations = new ArrayList<>();

        // Voyage -> CarrierMovement (internal entities)
        RelationshipMetadata voyageToMovements = new RelationshipMetadata(
                "carrierMovements", "CarrierMovement", "OneToMany");
        voyageToMovements.setCascadePersist(true);
        voyageToMovements.setCascadeRemove(true);
        voyageToMovements.setLazy(false);
        voyageRelations.add(voyageToMovements);

        voyage.setRelationships(voyageRelations);
        nodes.add(voyage);

        // CarrierMovement - Internal to Voyage
        EntityNode carrierMovement = new EntityNode("CarrierMovement", "se.citerus.dddsample.domain.model.voyage",
                "ENTITY");
        List<RelationshipMetadata> cmRelations = new ArrayList<>();

        RelationshipMetadata cmToDeparture = new RelationshipMetadata(
                "departureLocation", "Location", "ManyToOne");
        cmToDeparture.setLazy(true);
        cmRelations.add(cmToDeparture);

        RelationshipMetadata cmToArrival = new RelationshipMetadata(
                "arrivalLocation", "Location", "ManyToOne");
        cmToArrival.setLazy(true);
        cmRelations.add(cmToArrival);

        carrierMovement.setRelationships(cmRelations);
        nodes.add(carrierMovement);

        return nodes;
    }

    @Test
    @DisplayName("Cargo should be identified as AGGREGATE_ROOT")
    void cargoShouldBeAggregateRoot() {
        analyzer.analyze(shippingDomain);

        EntityNode cargo = findEntity("Cargo");
        assertNotNull(cargo, "Cargo entity should exist");
        assertEquals("AGGREGATE_ROOT", cargo.getDddRole(),
                "Cargo must be identified as AGGREGATE_ROOT - it controls DeliveryHistory and Itinerary lifecycle");
    }

    @Test
    @DisplayName("DeliveryHistory should be in Cargo's aggregate")
    void deliveryHistoryShouldBeInCargoAggregate() {
        analyzer.analyze(shippingDomain);

        EntityNode cargo = findEntity("Cargo");
        EntityNode deliveryHistory = findEntity("DeliveryHistory");

        assertNotNull(cargo);
        assertNotNull(deliveryHistory);

        assertEquals(cargo.getAggregateName(), deliveryHistory.getAggregateName(),
                "DeliveryHistory should be in the same aggregate as Cargo (cascade ownership)");
    }

    @Test
    @DisplayName("Location should NOT be in Cargo's aggregate")
    void locationShouldNotBeInCargoAggregate() {
        analyzer.analyze(shippingDomain);

        EntityNode cargo = findEntity("Cargo");
        EntityNode location = findEntity("Location");

        assertNotNull(cargo);
        assertNotNull(location);

        // Location is in a different package AND is not cascade-owned
        assertNotEquals(cargo.getAggregateName(), location.getAggregateName(),
                "Location should NOT be in Cargo's aggregate - it's reference data");
    }

    @Test
    @DisplayName("Voyage should be its own aggregate (separate bounded context)")
    void voyageShouldBeOwnAggregate() {
        analyzer.analyze(shippingDomain);

        EntityNode cargo = findEntity("Cargo");
        EntityNode voyage = findEntity("Voyage");

        assertNotNull(cargo);
        assertNotNull(voyage);

        // Voyage owns CarrierMovements, so it should be a root
        assertEquals("AGGREGATE_ROOT", voyage.getDddRole(),
                "Voyage should be AGGREGATE_ROOT - it controls CarrierMovement lifecycle");

        assertNotEquals(cargo.getAggregateName(), voyage.getAggregateName(),
                "Voyage should NOT be in Cargo's aggregate - it's a separate bounded context");
    }

    @Test
    @DisplayName("Itinerary should be VALUE_OBJECT (Embeddable)")
    void itineraryShouldBeValueObject() {
        analyzer.analyze(shippingDomain);

        EntityNode itinerary = findEntity("Itinerary");
        assertNotNull(itinerary);

        assertEquals("VALUE_OBJECT", itinerary.getDddRole(),
                "Itinerary should be VALUE_OBJECT - it's marked as EMBEDDABLE");
    }

    @Test
    @DisplayName("CarrierMovement should be in Voyage's aggregate")
    void carrierMovementShouldBeInVoyageAggregate() {
        analyzer.analyze(shippingDomain);

        EntityNode voyage = findEntity("Voyage");
        EntityNode carrierMovement = findEntity("CarrierMovement");

        assertNotNull(voyage);
        assertNotNull(carrierMovement);

        assertEquals(voyage.getAggregateName(), carrierMovement.getAggregateName(),
                "CarrierMovement should be in Voyage's aggregate (cascade ownership)");
    }

    @Test
    @DisplayName("HEURISTIC VALIDATION: Correct number of aggregates detected")
    void shouldDetectCorrectNumberOfAggregates() {
        analyzer.analyze(shippingDomain);

        // Count distinct aggregates
        long distinctAggregates = shippingDomain.stream()
                .map(EntityNode::getAggregateName)
                .distinct()
                .count();

        // Expected: Cargo, Location, Voyage (3 main bounded contexts)
        // Note: Some entities might have "Default" aggregate if heuristics fail
        assertTrue(distinctAggregates >= 3,
                "Should detect at least 3 distinct aggregates (Cargo, Location, Voyage), found: " + distinctAggregates);
        assertTrue(distinctAggregates <= 5,
                "Should not over-fragment into too many aggregates, found: " + distinctAggregates);
    }

    @Test
    @DisplayName("HEURISTIC VALIDATION: Summary of DDD Analysis")
    void printDDDAnalysisSummary() {
        analyzer.analyze(shippingDomain);

        System.out.println("\n=== DDD SAMPLE SHIPPING DOMAIN ANALYSIS ===\n");

        // Group by aggregate
        shippingDomain.stream()
                .collect(java.util.stream.Collectors.groupingBy(EntityNode::getAggregateName))
                .forEach((aggName, entities) -> {
                    System.out.println("Aggregate: " + aggName);
                    entities.forEach(e -> System.out.println("  - " + e.getName() + " [" + e.getDddRole() + "]"));
                    System.out.println();
                });

        // Count aggregates
        long aggregateRoots = shippingDomain.stream()
                .filter(e -> "AGGREGATE_ROOT".equals(e.getDddRole()))
                .count();

        long valueObjects = shippingDomain.stream()
                .filter(e -> "VALUE_OBJECT".equals(e.getDddRole()))
                .count();

        System.out.println("Summary:");
        System.out.println("  - Aggregate Roots: " + aggregateRoots);
        System.out.println("  - Value Objects: " + valueObjects);
        System.out.println("  - Regular Entities: " + (shippingDomain.size() - aggregateRoots - valueObjects));
    }

    private EntityNode findEntity(String name) {
        return shippingDomain.stream()
                .filter(e -> name.equals(e.getName()))
                .findFirst()
                .orElse(null);
    }
}
