package com.eclipselink.analyzer;

import com.eclipselink.analyzer.model.AttributeMetadata;
import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import com.eclipselink.analyzer.rules.*;
import com.eclipselink.analyzer.rules.EagerFetchRule;
import com.eclipselink.analyzer.rules.MappingRule;
import com.eclipselink.analyzer.rules.RelationshipOwnerRule;
import com.eclipselink.analyzer.rules.RedundantUpdateRule;
import com.eclipselink.analyzer.rules.OptimisticLockingRule;
import com.eclipselink.analyzer.rules.ForeignKeyIndexRule;
import com.eclipselink.analyzer.rules.LargeCollectionRule;
import com.eclipselink.analyzer.rules.SelfReferencingRule;
import com.eclipselink.analyzer.rules.InheritanceRule;
import com.eclipselink.analyzer.rules.GraphAnalysisRule;
import com.eclipselink.analyzer.rules.LobRule;
import com.eclipselink.analyzer.rules.TemporalRule;
import com.eclipselink.analyzer.rules.VersionRule;
import com.eclipselink.analyzer.rules.BatchFetchRule;
import com.eclipselink.analyzer.rules.InheritanceStrategyRule;
import com.eclipselink.analyzer.rules.DiscriminatorRule;
import com.eclipselink.analyzer.rules.NPlusOneQueryRule;
import com.eclipselink.analyzer.rules.CartesianProductRule;
import com.eclipselink.analyzer.rules.IndexRule;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting EclipseLink Mapping Analyzer...");

        generateEmployeeReport();
        generatePhoneReport();
        generateProjectReport();
        generateOrderReport();
        generateAdvancedReport();
        generateStudentReport();
        generateCyclicReport();
        generatePerformanceReport();
        generateComplexInheritanceReport();
        generateInvalidMappingReport();
        generateAnnotationReport();

        // Check for OFBiz stress test data
        java.io.File ofbizDir = new java.io.File("ofbiz-stress-test");
        System.out.println("Checking for OFBiz at: " + ofbizDir.getAbsolutePath());
        if (!ofbizDir.exists()) {
            ofbizDir = new java.io.File("../ofbiz-stress-test");
            System.out.println("Checking for OFBiz at: " + ofbizDir.getAbsolutePath());
        }

        if (ofbizDir.exists()) {
            System.out.println("OFBiz directory detected. Starting stress-test conversion...");
            generateOfbizReport();
        }

        // Generate Progressive Scenario Catalog
        System.out.println("\n=== Generating Progressive Scenario Catalog ===");
        generateProgressiveCatalog();

        System.out.println("All example reports generated successfully!");
    }

    private static void generateOfbizReport() throws Exception {
        com.eclipselink.analyzer.stress.OFBizConverter converter = new com.eclipselink.analyzer.stress.OFBizConverter();
        String path = "ofbiz-stress-test";
        if (!new java.io.File(path).exists()) {
            path = "../ofbiz-stress-test";
        }
        List<EntityNode> nodes = converter.convertFolder(path);
        System.out.println("Extracted " + nodes.size() + " entities from OFBiz.");
        runAnalysis(nodes, "OFBiz_Stress_Test", "ofbiz-report.json");
    }

    private static void generateEmployeeReport() throws Exception {
        EntityNode employee = new EntityNode("Employee", "eclipselink.example.jpa.employee.model", "ENTITY");
        Map<String, AttributeMetadata> empAttrs = new HashMap<>();
        empAttrs.put("id", new AttributeMetadata("id", "long", "BIGINT", "ID"));
        empAttrs.put("firstName", new AttributeMetadata("firstName", "String", "VARCHAR", "FIRSTNAME"));
        empAttrs.put("lastName", new AttributeMetadata("lastName", "String", "VARCHAR", "LASTNAME"));
        empAttrs.put("salary", new AttributeMetadata("salary", "double", "DOUBLE", "SALARY"));
        employee.setAttributes(empAttrs);

        List<RelationshipMetadata> empRels = new ArrayList<>();
        RelationshipMetadata rel = new RelationshipMetadata("address", "Address", "OneToOne");
        rel.setLazy(false);
        rel.setOwningSide(true);
        empRels.add(rel);

        RelationshipMetadata relPhones = new RelationshipMetadata("phones", "Phone", "OneToMany");
        relPhones.setLazy(true);
        relPhones.setMappedBy("owner");
        relPhones.setOwningSide(false);
        empRels.add(relPhones);

        employee.setRelationships(empRels);

        EntityNode address = new EntityNode("Address", "eclipselink.example.jpa.employee.model", "ENTITY");
        Map<String, AttributeMetadata> addrAttrs = new HashMap<>();
        addrAttrs.put("id", new AttributeMetadata("id", "long", "BIGINT", "ID"));
        addrAttrs.put("city", new AttributeMetadata("city", "String", "VARCHAR", "CITY"));
        addrAttrs.put("street", new AttributeMetadata("street", "String", "VARCHAR", "STREET"));
        address.setAttributes(addrAttrs);

        List<EntityNode> nodes = Arrays.asList(employee, address);
        runAnalysis(nodes, "Employee_Model", "employee-report.json");
    }

    private static void generatePhoneReport() throws Exception {
        EntityNode phone = new EntityNode("Phone", "eclipselink.example.jpa.phone.model", "ENTITY");
        Map<String, AttributeMetadata> attrs = new HashMap<>();
        attrs.put("id", new AttributeMetadata("id", "long", "BIGINT", "ID"));
        attrs.put("number", new AttributeMetadata("number", "String", "VARCHAR", "PHONE_NUMBER"));
        attrs.put("type", new AttributeMetadata("type", "String", "VARCHAR", "TYPE"));
        // Simulating a missing table error for demonstration
        phone.setAttributes(attrs);

        List<EntityNode> nodes = Arrays.asList(phone);
        runAnalysis(nodes, "Phone_Model", "phone-report.json");
    }

    private static void generateProjectReport() throws Exception {
        EntityNode project = new EntityNode("Project", "eclipselink.example.jpa.project.model", "ENTITY");
        Map<String, AttributeMetadata> projAttrs = new HashMap<>();
        projAttrs.put("id", new AttributeMetadata("id", "long", "BIGINT", "PROJ_ID"));
        projAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "PROJ_NAME"));
        projAttrs.put("description", new AttributeMetadata("description", "String", "VARCHAR", "DESC"));
        project.setAttributes(projAttrs);

        List<RelationshipMetadata> projRels = new ArrayList<>();
        RelationshipMetadata relTeam = new RelationshipMetadata("teamMembers", "Employee", "ManyToMany");
        relTeam.setLazy(true);
        relTeam.setOwningSide(true);
        projRels.add(relTeam);
        project.setRelationships(projRels);

        EntityNode emp = new EntityNode("Employee", "eclipselink.example.jpa.project.model", "ENTITY");
        Map<String, AttributeMetadata> empAttrs = new HashMap<>();
        empAttrs.put("id", new AttributeMetadata("id", "long", "BIGINT", "EMP_ID"));
        empAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "EMP_NAME"));
        emp.setAttributes(empAttrs);

        List<RelationshipMetadata> empRels = new ArrayList<>();
        RelationshipMetadata relProjs = new RelationshipMetadata("projects", "Project", "ManyToMany");
        relProjs.setLazy(true);
        relProjs.setMappedBy("teamMembers");
        relProjs.setOwningSide(false);
        empRels.add(relProjs);
        emp.setRelationships(empRels);

        List<EntityNode> nodes = Arrays.asList(project, emp);
        runAnalysis(nodes, "Project_Model", "project-report.json");
    }

    private static void generateOrderReport() throws Exception {
        // Renamed to PurchaseOrder to avoid SQL keyword ORDER
        EntityNode order = new EntityNode("PurchaseOrder", "eclipselink.example.jpa.order.model", "ENTITY");
        Map<String, AttributeMetadata> orderAttrs = new HashMap<>();
        orderAttrs.put("id", new AttributeMetadata("id", "long", "BIGINT", "ORDER_ID"));
        orderAttrs.put("total", new AttributeMetadata("total", "BigDecimal", "DECIMAL", "TOTAL_AMT"));
        order.setAttributes(orderAttrs);

        List<RelationshipMetadata> orderRels = new ArrayList<>();
        RelationshipMetadata relLines = new RelationshipMetadata("orderLines", "OrderLine", "OneToMany");
        relLines.setLazy(true);
        relLines.setCascadePersist(true);
        relLines.setMappedBy("order");
        relLines.setOwningSide(false);
        orderRels.add(relLines);
        order.setRelationships(orderRels);

        EntityNode line = new EntityNode("OrderLine", "eclipselink.example.jpa.order.model", "ENTITY");
        Map<String, AttributeMetadata> lineAttrs = new HashMap<>();
        lineAttrs.put("id", new AttributeMetadata("id", "long", "BIGINT", "LINE_ID"));
        lineAttrs.put("quantity", new AttributeMetadata("quantity", "int", "INTEGER", "QUANTITY"));
        line.setAttributes(lineAttrs);

        List<RelationshipMetadata> lineRels = new ArrayList<>();
        RelationshipMetadata relOrder = new RelationshipMetadata("order", "PurchaseOrder", "ManyToOne");
        relOrder.setLazy(false); // EAGER Issue
        relOrder.setOwningSide(true);
        relOrder.setMappedBy("orderLines");
        lineRels.add(relOrder);
        line.setRelationships(lineRels);

        List<EntityNode> nodes = Arrays.asList(order, line);
        runAnalysis(nodes, "Order_Model", "order-report.json");
    }

    private static void generateAdvancedReport() throws Exception {
        // Abstract Project entity (inheritance)
        EntityNode project = new EntityNode("Project", "org.eclipse.persistence.testing.perf.jpa.model.basic",
                "ABSTRACT_ENTITY");
        Map<String, AttributeMetadata> projAttrs = new HashMap<>();
        projAttrs.put("id", new AttributeMetadata("id", "int", "INTEGER", "PROJ_ID"));
        projAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "PROJ_NAME"));
        projAttrs.put("description", new AttributeMetadata("description", "String", "VARCHAR", "DESCRIP"));
        projAttrs.put("version", new AttributeMetadata("version", "long", "BIGINT", "VERSION"));
        project.setAttributes(projAttrs);

        List<RelationshipMetadata> projRels = new ArrayList<>();
        RelationshipMetadata relTeamLeader = new RelationshipMetadata("teamLeader", "Employee", "ManyToOne");
        relTeamLeader.setLazy(true);
        relTeamLeader.setOwningSide(true);
        relTeamLeader.setMappedBy(null);
        projRels.add(relTeamLeader);
        project.setRelationships(projRels);

        // LargeProject extends Project
        EntityNode largeProject = new EntityNode("LargeProject", "org.eclipse.persistence.testing.perf.jpa.model.basic",
                "ENTITY");
        largeProject.setParentEntity("Project");
        Map<String, AttributeMetadata> lpAttrs = new HashMap<>();
        lpAttrs.put("budget", new AttributeMetadata("budget", "double", "DOUBLE", "BUDGET"));
        lpAttrs.put("milestone", new AttributeMetadata("milestone", "Calendar", "TIMESTAMP", "MILESTONE"));
        largeProject.setAttributes(lpAttrs);
        // Inherits attributes from Project (not represented in this simple model)
        largeProject.setRelationships(new ArrayList<>());

        // SmallProject extends Project
        EntityNode smallProject = new EntityNode("SmallProject", "org.eclipse.persistence.testing.perf.jpa.model.basic",
                "ENTITY");
        smallProject.setParentEntity("Project");
        smallProject.setAttributes(new HashMap<>()); // No additional attributes
        smallProject.setRelationships(new ArrayList<>());

        // Employee entity (complex)
        EntityNode employee = new EntityNode("Employee", "org.eclipse.persistence.testing.perf.jpa.model.basic",
                "ENTITY");
        Map<String, AttributeMetadata> empAttrs = new HashMap<>();
        empAttrs.put("id", new AttributeMetadata("id", "long", "BIGINT", "EMP_ID"));
        empAttrs.put("firstName", new AttributeMetadata("firstName", "String", "VARCHAR", "F_NAME"));
        empAttrs.put("lastName", new AttributeMetadata("lastName", "String", "VARCHAR", "L_NAME"));
        empAttrs.put("gender", new AttributeMetadata("gender", "Gender", "VARCHAR", "GENDER"));
        empAttrs.put("salary", new AttributeMetadata("salary", "double", "DOUBLE", "SALARY"));
        empAttrs.put("version", new AttributeMetadata("version", "long", "BIGINT", "VERSION"));
        employee.setAttributes(empAttrs);

        List<RelationshipMetadata> empRels = new ArrayList<>();
        // OneToOne address
        RelationshipMetadata relAddress = new RelationshipMetadata("address", "Address", "OneToOne");
        relAddress.setLazy(true);
        relAddress.setOwningSide(true);
        relAddress.setCascadePersist(true);
        empRels.add(relAddress);
        // ManyToOne jobTitle (via join table)
        RelationshipMetadata relJobTitle = new RelationshipMetadata("jobTitle", "JobTitle", "ManyToOne");
        relJobTitle.setLazy(true);
        relJobTitle.setOwningSide(true);
        relJobTitle.setCascadePersist(true);
        empRels.add(relJobTitle);
        // ManyToOne manager
        RelationshipMetadata relManager = new RelationshipMetadata("manager", "Employee", "ManyToOne");
        relManager.setLazy(true);
        relManager.setOwningSide(true);
        empRels.add(relManager);
        // OneToMany managedEmployees (mappedBy manager)
        RelationshipMetadata relManagedEmployees = new RelationshipMetadata("managedEmployees", "Employee",
                "OneToMany");
        relManagedEmployees.setLazy(true);
        relManagedEmployees.setOwningSide(false);
        relManagedEmployees.setMappedBy("manager");
        empRels.add(relManagedEmployees);
        // OneToMany phoneNumbers (mappedBy owner)
        RelationshipMetadata relPhoneNumbers = new RelationshipMetadata("phoneNumbers", "PhoneNumber", "OneToMany");
        relPhoneNumbers.setLazy(true);
        relPhoneNumbers.setOwningSide(false);
        relPhoneNumbers.setMappedBy("owner");
        relPhoneNumbers.setCascadePersist(true);
        empRels.add(relPhoneNumbers);
        // OneToMany degrees (owning side)
        RelationshipMetadata relDegrees = new RelationshipMetadata("degrees", "Degree", "OneToMany");
        relDegrees.setLazy(true);
        relDegrees.setOwningSide(true);
        relDegrees.setCascadePersist(true);
        empRels.add(relDegrees);
        // ManyToMany projects
        RelationshipMetadata relProjects = new RelationshipMetadata("projects", "Project", "ManyToMany");
        relProjects.setLazy(true);
        relProjects.setOwningSide(true);
        empRels.add(relProjects);
        // ElementCollection responsibilities (simulated as OneToMany to a separate
        // table)
        RelationshipMetadata relResponsibilities = new RelationshipMetadata("responsibilities", "String",
                "ElementCollection");
        relResponsibilities.setLazy(true);
        relResponsibilities.setOwningSide(true);
        empRels.add(relResponsibilities);
        // ElementCollection emailAddresses (simulated)
        RelationshipMetadata relEmailAddresses = new RelationshipMetadata("emailAddresses", "EmailAddress",
                "ElementCollection");
        relEmailAddresses.setLazy(true);
        relEmailAddresses.setOwningSide(true);
        empRels.add(relEmailAddresses);

        employee.setRelationships(empRels);

        // Address entity
        EntityNode address = new EntityNode("Address", "org.eclipse.persistence.testing.perf.jpa.model.basic",
                "ENTITY");
        Map<String, AttributeMetadata> addrAttrs = new HashMap<>();
        addrAttrs.put("id", new AttributeMetadata("id", "long", "BIGINT", "ADDR_ID"));
        addrAttrs.put("street", new AttributeMetadata("street", "String", "VARCHAR", "STREET"));
        addrAttrs.put("city", new AttributeMetadata("city", "String", "VARCHAR", "CITY"));
        addrAttrs.put("postalCode", new AttributeMetadata("postalCode", "String", "VARCHAR", "POSTAL_CODE"));
        address.setAttributes(addrAttrs);
        address.setRelationships(new ArrayList<>());

        // PhoneNumber entity
        EntityNode phoneNumber = new EntityNode("PhoneNumber", "org.eclipse.persistence.testing.perf.jpa.model.basic",
                "ENTITY");
        Map<String, AttributeMetadata> phoneAttrs = new HashMap<>();
        phoneAttrs.put("id", new AttributeMetadata("id", "long", "BIGINT", "PHONE_ID"));
        phoneAttrs.put("type", new AttributeMetadata("type", "String", "VARCHAR", "TYPE"));
        phoneAttrs.put("areaCode", new AttributeMetadata("areaCode", "String", "VARCHAR", "AREA_CODE"));
        phoneAttrs.put("number", new AttributeMetadata("number", "String", "VARCHAR", "PHONE_NUMBER"));
        phoneNumber.setAttributes(phoneAttrs);
        List<RelationshipMetadata> phoneRels = new ArrayList<>();
        RelationshipMetadata relOwner = new RelationshipMetadata("owner", "Employee", "ManyToOne");
        relOwner.setLazy(false);
        relOwner.setOwningSide(true);
        relOwner.setMappedBy("phoneNumbers");
        phoneRels.add(relOwner);
        phoneNumber.setRelationships(phoneRels);

        // Degree entity
        EntityNode degree = new EntityNode("Degree", "org.eclipse.persistence.testing.perf.jpa.model.basic", "ENTITY");
        Map<String, AttributeMetadata> degAttrs = new HashMap<>();
        degAttrs.put("id", new AttributeMetadata("id", "long", "BIGINT", "DEGREE_ID"));
        degAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "DEGREE_NAME"));
        degree.setAttributes(degAttrs);
        degree.setRelationships(new ArrayList<>());

        // JobTitle entity
        EntityNode jobTitle = new EntityNode("JobTitle", "org.eclipse.persistence.testing.perf.jpa.model.basic",
                "ENTITY");
        Map<String, AttributeMetadata> jobAttrs = new HashMap<>();
        jobAttrs.put("id", new AttributeMetadata("id", "long", "BIGINT", "TITLE_ID"));
        jobAttrs.put("title", new AttributeMetadata("title", "String", "VARCHAR", "TITLE"));
        jobTitle.setAttributes(jobAttrs);
        jobTitle.setRelationships(new ArrayList<>());

        // EmailAddress embeddable (simulated as entity for simplicity)
        EntityNode emailAddr = new EntityNode("EmailAddress", "org.eclipse.persistence.testing.perf.jpa.model.basic",
                "EMBEDDABLE");
        Map<String, AttributeMetadata> emailAttrs = new HashMap<>();
        emailAttrs.put("address", new AttributeMetadata("address", "String", "VARCHAR", "EMAIL_ADDRESS"));
        emailAddr.setAttributes(emailAttrs);
        emailAddr.setRelationships(new ArrayList<>());

        List<EntityNode> nodes = Arrays.asList(project, largeProject, smallProject, employee, address, phoneNumber,
                degree, jobTitle, emailAddr);
        runAnalysis(nodes, "Advanced_Model", "advanced-report.json");
    }

    private static void generateStudentReport() throws Exception {
        EntityNode student = new EntityNode("Student", "eclipselink.example.jpars.student.model", "ENTITY");
        Map<String, AttributeMetadata> stuAttrs = new HashMap<>();
        stuAttrs.put("id", new AttributeMetadata("id", "Long", "BIGINT", "ID"));
        stuAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "NAME"));
        student.setAttributes(stuAttrs);

        List<RelationshipMetadata> stuRels = new ArrayList<>();
        RelationshipMetadata relCourses = new RelationshipMetadata("courses", "Course", "ManyToMany");
        relCourses.setLazy(true);
        relCourses.setOwningSide(true);
        stuRels.add(relCourses);
        student.setRelationships(stuRels);

        EntityNode course = new EntityNode("Course", "eclipselink.example.jpars.student.model", "ENTITY");
        Map<String, AttributeMetadata> courseAttrs = new HashMap<>();
        courseAttrs.put("id", new AttributeMetadata("id", "Long", "BIGINT", "ID"));
        courseAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "NAME"));
        course.setAttributes(courseAttrs);

        List<RelationshipMetadata> courseRels = new ArrayList<>();
        RelationshipMetadata relStudents = new RelationshipMetadata("students", "Student", "ManyToMany");
        relStudents.setLazy(true);
        relStudents.setMappedBy("courses");
        relStudents.setOwningSide(false);
        courseRels.add(relStudents);
        course.setRelationships(courseRels);

        List<EntityNode> nodes = Arrays.asList(student, course);
        runAnalysis(nodes, "Student_Model", "student-report.json");
    }

    private static void generateCyclicReport() throws Exception {
        // Create entities with circular references
        EntityNode a = new EntityNode("EntityA", "cyclic.model", "ENTITY");
        Map<String, AttributeMetadata> aAttrs = new HashMap<>();
        aAttrs.put("id", new AttributeMetadata("id", "Long", "BIGINT", "ID"));
        aAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "NAME"));
        a.setAttributes(aAttrs);

        EntityNode b = new EntityNode("EntityB", "cyclic.model", "ENTITY");
        Map<String, AttributeMetadata> bAttrs = new HashMap<>();
        bAttrs.put("id", new AttributeMetadata("id", "Long", "BIGINT", "ID"));
        bAttrs.put("value", new AttributeMetadata("value", "int", "INTEGER", "VAL"));
        b.setAttributes(bAttrs);

        EntityNode c = new EntityNode("EntityC", "cyclic.model", "ENTITY");
        Map<String, AttributeMetadata> cAttrs = new HashMap<>();
        cAttrs.put("id", new AttributeMetadata("id", "Long", "BIGINT", "ID"));
        cAttrs.put("data", new AttributeMetadata("data", "String", "VARCHAR", "DATA"));
        c.setAttributes(cAttrs);

        // Circular relationships: A -> B -> C -> A
        List<RelationshipMetadata> aRels = new ArrayList<>();
        RelationshipMetadata relAB = new RelationshipMetadata("toB", "EntityB", "OneToOne");
        relAB.setLazy(false); // Eager for detection
        relAB.setOwningSide(true);
        aRels.add(relAB);
        a.setRelationships(aRels);

        List<RelationshipMetadata> bRels = new ArrayList<>();
        RelationshipMetadata relBC = new RelationshipMetadata("toC", "EntityC", "OneToOne");
        relBC.setLazy(true);
        relBC.setOwningSide(true);
        bRels.add(relBC);
        b.setRelationships(bRels);

        List<RelationshipMetadata> cRels = new ArrayList<>();
        RelationshipMetadata relCA = new RelationshipMetadata("toA", "EntityA", "OneToOne");
        relCA.setLazy(true);
        relCA.setOwningSide(true);
        cRels.add(relCA);
        c.setRelationships(cRels);

        List<EntityNode> nodes = Arrays.asList(a, b, c);
        runAnalysis(nodes, "Cyclic_Model", "cyclic-report.json");
    }

    private static void generatePerformanceReport() throws Exception {
        // Model with common performance antipatterns
        EntityNode customer = new EntityNode("Customer", "performance.model", "ENTITY");
        Map<String, AttributeMetadata> custAttrs = new HashMap<>();
        custAttrs.put("id", new AttributeMetadata("id", "Long", "BIGINT", "ID"));
        custAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "NAME"));
        customer.setAttributes(custAttrs);

        List<RelationshipMetadata> custRels = new ArrayList<>();
        // Eager fetching chain
        RelationshipMetadata relOrders = new RelationshipMetadata("orders", "SalesOrder", "OneToMany");
        relOrders.setLazy(false); // Eager fetching antipattern
        relOrders.setOwningSide(false);
        relOrders.setMappedBy("customer");
        custRels.add(relOrders);
        // ManyToMany without join table index (simulated)
        RelationshipMetadata relTags = new RelationshipMetadata("tags", "Tag", "ManyToMany");
        relTags.setLazy(true);
        relTags.setOwningSide(true);
        custRels.add(relTags);
        customer.setRelationships(custRels);

        EntityNode order = new EntityNode("SalesOrder", "performance.model", "ENTITY");
        Map<String, AttributeMetadata> orderAttrs = new HashMap<>();
        orderAttrs.put("id", new AttributeMetadata("id", "Long", "BIGINT", "ID"));
        orderAttrs.put("total", new AttributeMetadata("total", "BigDecimal", "DECIMAL", "TOTAL"));
        order.setAttributes(orderAttrs);

        List<RelationshipMetadata> orderRels = new ArrayList<>();
        RelationshipMetadata relCustomer = new RelationshipMetadata("customer", "Customer", "ManyToOne");
        relCustomer.setLazy(false); // Eager
        relCustomer.setOwningSide(true);
        orderRels.add(relCustomer);
        // OneToMany with large collection
        RelationshipMetadata relItems = new RelationshipMetadata("items", "OrderItem", "OneToMany");
        relItems.setLazy(true);
        relItems.setOwningSide(false);
        relItems.setMappedBy("order");
        orderRels.add(relItems);
        order.setRelationships(orderRels);

        EntityNode orderItem = new EntityNode("OrderItem", "performance.model", "ENTITY");
        Map<String, AttributeMetadata> itemAttrs = new HashMap<>();
        itemAttrs.put("id", new AttributeMetadata("id", "Long", "BIGINT", "ID"));
        itemAttrs.put("quantity", new AttributeMetadata("quantity", "int", "INTEGER", "QTY"));
        orderItem.setAttributes(itemAttrs);

        List<RelationshipMetadata> itemRels = new ArrayList<>();
        RelationshipMetadata relOrder = new RelationshipMetadata("order", "SalesOrder", "ManyToOne");
        relOrder.setLazy(true);
        relOrder.setOwningSide(true);
        itemRels.add(relOrder);
        orderItem.setRelationships(itemRels);

        EntityNode tag = new EntityNode("Tag", "performance.model", "ENTITY");
        Map<String, AttributeMetadata> tagAttrs = new HashMap<>();
        tagAttrs.put("id", new AttributeMetadata("id", "Long", "BIGINT", "ID"));
        tagAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "NAME"));
        tag.setAttributes(tagAttrs);

        List<RelationshipMetadata> tagRels = new ArrayList<>();
        RelationshipMetadata relCustomers = new RelationshipMetadata("customers", "Customer", "ManyToMany");
        relCustomers.setLazy(true);
        relCustomers.setMappedBy("tags");
        relCustomers.setOwningSide(false);
        tagRels.add(relCustomers);
        tag.setRelationships(tagRels);

        List<EntityNode> nodes = Arrays.asList(customer, order, orderItem, tag);
        runAnalysis(nodes, "Performance_Model", "performance-report.json");
    }

    private static void generateComplexInheritanceReport() throws Exception {
        // MappedSuperclass example (simulated as abstract entity)
        EntityNode baseEntity = new EntityNode("BaseEntity", "complex.model", "ABSTRACT_ENTITY");
        Map<String, AttributeMetadata> baseAttrs = new HashMap<>();
        baseAttrs.put("id", new AttributeMetadata("id", "Long", "BIGINT", "ID"));
        baseAttrs.put("version", new AttributeMetadata("version", "Long", "BIGINT", "VERSION"));
        baseAttrs.put("createdDate", new AttributeMetadata("createdDate", "Date", "TIMESTAMP", "CREATED"));
        baseEntity.setAttributes(baseAttrs);
        baseEntity.setRelationships(new ArrayList<>());

        // Person extends BaseEntity (JOINED inheritance)
        EntityNode person = new EntityNode("Person", "complex.model", "ENTITY");
        person.setParentEntity("BaseEntity");
        Map<String, AttributeMetadata> personAttrs = new HashMap<>();
        personAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "NAME"));
        personAttrs.put("email", new AttributeMetadata("email", "String", "VARCHAR", "EMAIL"));
        person.setAttributes(personAttrs);

        List<RelationshipMetadata> personRels = new ArrayList<>();
        // Embedded address
        RelationshipMetadata relAddress = new RelationshipMetadata("address", "Address", "Embedded");
        relAddress.setOwningSide(true);
        personRels.add(relAddress);
        // OneToMany phones
        RelationshipMetadata relPhones = new RelationshipMetadata("phones", "Phone", "OneToMany");
        relPhones.setLazy(true);
        relPhones.setOwningSide(false);
        relPhones.setMappedBy("owner");
        relPhones.setCascadePersist(true);
        personRels.add(relPhones);
        person.setRelationships(personRels);

        // Employee extends Person (JOINED with discriminator)
        EntityNode employee = new EntityNode("Employee", "complex.model", "ENTITY");
        employee.setParentEntity("Person");
        Map<String, AttributeMetadata> empAttrs = new HashMap<>();
        empAttrs.put("employeeId", new AttributeMetadata("employeeId", "String", "VARCHAR", "EMP_ID"));
        empAttrs.put("salary", new AttributeMetadata("salary", "BigDecimal", "DECIMAL", "SALARY"));
        empAttrs.put("startDate", new AttributeMetadata("startDate", "Date", "DATE", "START_DATE"));
        employee.setAttributes(empAttrs);

        List<RelationshipMetadata> empRels = new ArrayList<>();
        // ManyToOne department
        RelationshipMetadata relDept = new RelationshipMetadata("department", "Department", "ManyToOne");
        relDept.setLazy(false); // EAGER - problematic
        relDept.setOwningSide(true);
        empRels.add(relDept);
        // OneToMany projects (join table)
        RelationshipMetadata relProjects = new RelationshipMetadata("projects", "Project", "ManyToMany");
        relProjects.setLazy(true);
        relProjects.setOwningSide(true);
        empRels.add(relProjects);
        // Self-referential manager
        RelationshipMetadata relManager = new RelationshipMetadata("manager", "Employee", "ManyToOne");
        relManager.setLazy(true);
        relManager.setOwningSide(true);
        empRels.add(relManager);
        employee.setRelationships(empRels);

        // Manager extends Employee (SINGLE_TABLE inheritance)
        EntityNode manager = new EntityNode("Manager", "complex.model", "ENTITY");
        manager.setParentEntity("Employee");
        Map<String, AttributeMetadata> mgrAttrs = new HashMap<>();
        mgrAttrs.put("bonus", new AttributeMetadata("bonus", "BigDecimal", "DECIMAL", "BONUS"));
        mgrAttrs.put("teamSize", new AttributeMetadata("teamSize", "int", "INTEGER", "TEAM_SIZE"));
        manager.setAttributes(mgrAttrs);
        manager.setRelationships(new ArrayList<>());

        // Contractor extends Person (TABLE_PER_CLASS inheritance)
        EntityNode contractor = new EntityNode("Contractor", "complex.model", "ENTITY");
        contractor.setParentEntity("Person");
        Map<String, AttributeMetadata> contrAttrs = new HashMap<>();
        contrAttrs.put("hourlyRate", new AttributeMetadata("hourlyRate", "BigDecimal", "DECIMAL", "RATE"));
        contrAttrs.put("contractEnd", new AttributeMetadata("contractEnd", "Date", "DATE", "END_DATE"));
        contractor.setAttributes(contrAttrs);
        contractor.setRelationships(new ArrayList<>());

        // Address Embeddable
        EntityNode address = new EntityNode("Address", "complex.model", "EMBEDDABLE");
        Map<String, AttributeMetadata> addrAttrs = new HashMap<>();
        addrAttrs.put("street", new AttributeMetadata("street", "String", "VARCHAR", "STREET"));
        addrAttrs.put("city", new AttributeMetadata("city", "String", "VARCHAR", "CITY"));
        addrAttrs.put("zipCode", new AttributeMetadata("zipCode", "String", "VARCHAR", "ZIP"));
        address.setAttributes(addrAttrs);
        address.setRelationships(new ArrayList<>());

        // Phone entity
        EntityNode phone = new EntityNode("Phone", "complex.model", "ENTITY");
        Map<String, AttributeMetadata> phoneAttrs = new HashMap<>();
        phoneAttrs.put("id", new AttributeMetadata("id", "Long", "BIGINT", "ID"));
        phoneAttrs.put("number", new AttributeMetadata("number", "String", "VARCHAR", "PHONE_NUM"));
        phoneAttrs.put("type", new AttributeMetadata("type", "String", "VARCHAR", "TYPE"));
        phone.setAttributes(phoneAttrs);

        List<RelationshipMetadata> phoneRels = new ArrayList<>();
        RelationshipMetadata relOwner = new RelationshipMetadata("owner", "Person", "ManyToOne");
        relOwner.setLazy(true);
        relOwner.setOwningSide(true);
        relOwner.setMappedBy("phones");
        phoneRels.add(relOwner);
        phone.setRelationships(phoneRels);

        // Department with SecondaryTable
        EntityNode department = new EntityNode("Department", "complex.model", "ENTITY");
        Map<String, AttributeMetadata> deptAttrs = new HashMap<>();
        deptAttrs.put("id", new AttributeMetadata("id", "Long", "BIGINT", "DEPT_ID"));
        deptAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "DEPT_NAME"));
        deptAttrs.put("budget", new AttributeMetadata("budget", "BigDecimal", "DECIMAL", "BUDGET")); // in secondary
                                                                                                     // table
        department.setAttributes(deptAttrs);

        List<RelationshipMetadata> deptRels = new ArrayList<>();
        // OneToMany employees (mappedBy department)
        RelationshipMetadata relEmployees = new RelationshipMetadata("employees", "Employee", "OneToMany");
        relEmployees.setLazy(true);
        relEmployees.setOwningSide(false);
        relEmployees.setMappedBy("department");
        deptRels.add(relEmployees);
        // ElementCollection locations
        RelationshipMetadata relLocations = new RelationshipMetadata("locations", "String", "ElementCollection");
        relLocations.setLazy(true);
        relLocations.setOwningSide(true);
        deptRels.add(relLocations);
        department.setRelationships(deptRels);

        // Project with complex relationships
        EntityNode project = new EntityNode("Project", "complex.model", "ENTITY");
        Map<String, AttributeMetadata> projAttrs = new HashMap<>();
        projAttrs.put("id", new AttributeMetadata("id", "Long", "BIGINT", "PROJ_ID"));
        projAttrs.put("code", new AttributeMetadata("code", "String", "VARCHAR", "PROJ_CODE"));
        projAttrs.put("description", new AttributeMetadata("description", "String", "VARCHAR", "DESCRIPTION"));
        projAttrs.put("budget", new AttributeMetadata("budget", "BigDecimal", "DECIMAL", "BUDGET")); // secondary
        project.setAttributes(projAttrs);

        List<RelationshipMetadata> projRels = new ArrayList<>();
        // ManyToMany employees (owning side with join table)
        RelationshipMetadata relProjectEmployees = new RelationshipMetadata("team", "Employee", "ManyToMany");
        relProjectEmployees.setLazy(true);
        relProjectEmployees.setOwningSide(true);
        projRels.add(relProjectEmployees);
        // ManyToOne department
        RelationshipMetadata relProjectDept = new RelationshipMetadata("department", "Department", "ManyToOne");
        relProjectDept.setLazy(false); // EAGER
        relProjectDept.setOwningSide(true);
        projRels.add(relProjectDept);
        // OneToMany tasks
        RelationshipMetadata relTasks = new RelationshipMetadata("tasks", "Task", "OneToMany");
        relTasks.setLazy(true);
        relTasks.setOwningSide(false);
        relTasks.setMappedBy("project");
        relTasks.setCascadePersist(true);
        projRels.add(relTasks);
        project.setRelationships(projRels);

        // Task entity
        EntityNode task = new EntityNode("Task", "complex.model", "ENTITY");
        Map<String, AttributeMetadata> taskAttrs = new HashMap<>();
        taskAttrs.put("id", new AttributeMetadata("id", "Long", "BIGINT", "TASK_ID"));
        taskAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "TASK_NAME"));
        taskAttrs.put("status", new AttributeMetadata("status", "String", "VARCHAR", "STATUS"));
        task.setAttributes(taskAttrs);

        List<RelationshipMetadata> taskRels = new ArrayList<>();
        RelationshipMetadata relProject = new RelationshipMetadata("project", "Project", "ManyToOne");
        relProject.setLazy(true);
        relProject.setOwningSide(true);
        relProject.setMappedBy("tasks");
        taskRels.add(relProject);
        // ManyToMany tags
        RelationshipMetadata relTags = new RelationshipMetadata("tags", "Tag", "ManyToMany");
        relTags.setLazy(true);
        relTags.setOwningSide(true);
        taskRels.add(relTags);
        task.setRelationships(taskRels);

        // Tag entity
        EntityNode tag = new EntityNode("Tag", "complex.model", "ENTITY");
        Map<String, AttributeMetadata> tagAttrs = new HashMap<>();
        tagAttrs.put("id", new AttributeMetadata("id", "Long", "BIGINT", "TAG_ID"));
        tagAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "TAG_NAME"));
        tag.setAttributes(tagAttrs);
        tag.setRelationships(new ArrayList<>());

        List<EntityNode> nodes = Arrays.asList(baseEntity, person, employee, manager, contractor, address, phone,
                department, project, task, tag);
        runAnalysis(nodes, "Complex_Inheritance_Model", "complex-inheritance-report.json");
    }

    private static void generateInvalidMappingReport() throws Exception {
        // Entity without ID
        EntityNode badEntity = new EntityNode("BadEntity", "invalid.model", "ENTITY");
        Map<String, AttributeMetadata> badAttrs = new HashMap<>();
        badAttrs.put("data", new AttributeMetadata("data", "String", "VARCHAR", "DATA"));
        badEntity.setAttributes(badAttrs);

        // Parent with eager collection
        EntityNode parent = new EntityNode("Parent", "invalid.model", "ENTITY");
        Map<String, AttributeMetadata> parentAttrs = new HashMap<>();
        parentAttrs.put("id", new AttributeMetadata("id", "Long", "BIGINT", "ID"));
        parent.setAttributes(parentAttrs);

        List<RelationshipMetadata> parentRels = new ArrayList<>();
        RelationshipMetadata relChildren = new RelationshipMetadata("children", "Child", "OneToMany");
        relChildren.setLazy(false); // EAGER collection
        relChildren.setOwningSide(false);
        relChildren.setMappedBy("parent");
        parentRels.add(relChildren);
        parent.setRelationships(parentRels);

        // Child with eager ManyToOne and circular reference
        EntityNode child = new EntityNode("Child", "invalid.model", "ENTITY");
        Map<String, AttributeMetadata> childAttrs = new HashMap<>();
        childAttrs.put("id", new AttributeMetadata("id", "Long", "BIGINT", "ID"));
        child.setAttributes(childAttrs);

        List<RelationshipMetadata> childRels = new ArrayList<>();
        RelationshipMetadata relParent = new RelationshipMetadata("parent", "Parent", "ManyToOne");
        relParent.setLazy(false); // EAGER
        relParent.setOwningSide(true);
        childRels.add(relParent);
        RelationshipMetadata relSibling = new RelationshipMetadata("sibling", "Child", "OneToOne");
        relSibling.setLazy(true);
        relSibling.setOwningSide(true);
        childRels.add(relSibling);
        child.setRelationships(childRels);

        // Entity with long table name
        EntityNode longTable = new EntityNode("VeryLongTableNameThatExceedsTypicalDatabaseLimits", "invalid.model",
                "ENTITY");
        Map<String, AttributeMetadata> tableAttrs = new HashMap<>();
        tableAttrs.put("id", new AttributeMetadata("id", "Long", "BIGINT", "ID"));
        longTable.setAttributes(tableAttrs);
        longTable.setRelationships(new ArrayList<>());

        List<EntityNode> nodes = Arrays.asList(badEntity, parent, child, longTable);
        runAnalysis(nodes, "Invalid_Mapping_Model", "invalid-mapping-report.json");
    }

    private static void generateAnnotationReport() throws Exception {
        // Document entity with @Lob
        EntityNode document = new EntityNode("Document", "annotation.model", "ENTITY");
        Map<String, AttributeMetadata> docAttrs = new HashMap<>();
        AttributeMetadata idAttr = new AttributeMetadata("id", "Long", "BIGINT", "ID");
        idAttr.setId(true);
        docAttrs.put("id", idAttr);
        AttributeMetadata contentAttr = new AttributeMetadata("content", "String", "CLOB", "CONTENT");
        contentAttr.setLob(true);
        docAttrs.put("content", contentAttr);
        AttributeMetadata pdfAttr = new AttributeMetadata("pdfData", "byte[]", "BLOB", "PDF_DATA");
        pdfAttr.setLob(true);
        docAttrs.put("pdfData", pdfAttr);
        AttributeMetadata versionAttr = new AttributeMetadata("version", "Long", "BIGINT", "VERSION");
        versionAttr.setVersion(true);
        docAttrs.put("version", versionAttr);
        document.setAttributes(docAttrs);

        List<RelationshipMetadata> docRels = new ArrayList<>();
        RelationshipMetadata relTags = new RelationshipMetadata("tags", "Tag", "OneToMany");
        relTags.setLazy(true);
        relTags.setOwningSide(false);
        relTags.setMappedBy("document");
        relTags.setCascadePersist(true);
        relTags.setCascadeRemove(true);
        relTags.setBatchFetchType("JOIN");
        docRels.add(relTags);
        document.setRelationships(docRels);

        // Tag entity
        EntityNode tag = new EntityNode("Tag", "annotation.model", "ENTITY");
        Map<String, AttributeMetadata> tagAttrs = new HashMap<>();
        AttributeMetadata tagIdAttr = new AttributeMetadata("id", "Long", "BIGINT", "ID");
        tagIdAttr.setId(true);
        tagAttrs.put("id", tagIdAttr);
        AttributeMetadata nameAttr = new AttributeMetadata("name", "String", "VARCHAR", "NAME");
        tagAttrs.put("name", nameAttr);
        tag.setAttributes(tagAttrs);

        List<RelationshipMetadata> tagRels = new ArrayList<>();
        RelationshipMetadata relDocument = new RelationshipMetadata("document", "Document", "ManyToOne");
        relDocument.setLazy(true);
        relDocument.setOwningSide(true);
        relDocument.setMappedBy("tags");
        relDocument.setBatchFetchType("IN");
        tagRels.add(relDocument);
        tag.setRelationships(tagRels);

        // Event entity with @Temporal
        EntityNode event = new EntityNode("Event", "annotation.model", "ENTITY");
        Map<String, AttributeMetadata> eventAttrs = new HashMap<>();
        AttributeMetadata eventIdAttr = new AttributeMetadata("id", "Long", "BIGINT", "ID");
        eventIdAttr.setId(true);
        eventAttrs.put("id", eventIdAttr);
        AttributeMetadata dateAttr = new AttributeMetadata("eventDate", "java.util.Date", "DATE", "EVENT_DATE");
        dateAttr.setTemporal(true);
        dateAttr.setTemporalType("DATE");
        eventAttrs.put("eventDate", dateAttr);
        AttributeMetadata timeAttr = new AttributeMetadata("eventTime", "java.util.Date", "TIME", "EVENT_TIME");
        timeAttr.setTemporal(true);
        timeAttr.setTemporalType("TIME");
        eventAttrs.put("eventTime", timeAttr);
        AttributeMetadata timestampAttr = new AttributeMetadata("created", "java.util.Date", "TIMESTAMP", "CREATED");
        timestampAttr.setTemporal(true);
        timestampAttr.setTemporalType("TIMESTAMP");
        eventAttrs.put("created", timestampAttr);
        event.setAttributes(eventAttrs);
        event.setRelationships(new ArrayList<>());

        // VersionedEntity with @Version and @Enumerated
        EntityNode versioned = new EntityNode("VersionedEntity", "annotation.model", "ENTITY");
        Map<String, AttributeMetadata> verAttrs = new HashMap<>();
        AttributeMetadata verIdAttr = new AttributeMetadata("id", "Long", "BIGINT", "ID");
        verIdAttr.setId(true);
        verAttrs.put("id", verIdAttr);
        AttributeMetadata verAttr = new AttributeMetadata("version", "Integer", "INTEGER", "VERSION");
        verAttr.setVersion(true);
        verAttrs.put("version", verAttr);
        AttributeMetadata statusAttr = new AttributeMetadata("status", "Status", "VARCHAR", "STATUS");
        statusAttr.setEnumerated(true);
        verAttrs.put("status", statusAttr);
        versioned.setAttributes(verAttrs);
        versioned.setRelationships(new ArrayList<>());

        // Inheritance example with discriminator
        EntityNode vehicle = new EntityNode("Vehicle", "annotation.model", "ABSTRACT_ENTITY");
        Map<String, AttributeMetadata> vehicleAttrs = new HashMap<>();
        AttributeMetadata vehicleIdAttr = new AttributeMetadata("id", "Long", "BIGINT", "ID");
        vehicleIdAttr.setId(true);
        vehicleAttrs.put("id", vehicleIdAttr);
        AttributeMetadata regAttr = new AttributeMetadata("registration", "String", "VARCHAR", "REG_NUM");
        vehicleAttrs.put("registration", regAttr);
        vehicle.setAttributes(vehicleAttrs);
        vehicle.setInheritanceStrategy("SINGLE_TABLE");
        vehicle.setDiscriminatorColumn("VEHICLE_TYPE");
        vehicle.setDiscriminatorValue("VEHICLE");
        vehicle.setRelationships(new ArrayList<>());

        EntityNode car = new EntityNode("Car", "annotation.model", "ENTITY");
        car.setParentEntity("Vehicle");
        car.setInheritanceStrategy("SINGLE_TABLE");
        car.setDiscriminatorValue("CAR");
        Map<String, AttributeMetadata> carAttrs = new HashMap<>();
        AttributeMetadata seatsAttr = new AttributeMetadata("seats", "int", "INTEGER", "SEATS");
        carAttrs.put("seats", seatsAttr);
        car.setAttributes(carAttrs);
        car.setRelationships(new ArrayList<>());

        EntityNode truck = new EntityNode("Truck", "annotation.model", "ENTITY");
        truck.setParentEntity("Vehicle");
        truck.setInheritanceStrategy("SINGLE_TABLE");
        truck.setDiscriminatorValue("TRUCK");
        Map<String, AttributeMetadata> truckAttrs = new HashMap<>();
        AttributeMetadata capacityAttr = new AttributeMetadata("capacity", "double", "DOUBLE", "CAPACITY");
        truckAttrs.put("capacity", capacityAttr);
        truck.setAttributes(truckAttrs);
        truck.setRelationships(new ArrayList<>());

        // Relationship with multiple cascade types and batch fetch
        EntityNode department = new EntityNode("Department", "annotation.model", "ENTITY");
        Map<String, AttributeMetadata> deptAttrs = new HashMap<>();
        AttributeMetadata deptIdAttr = new AttributeMetadata("id", "Long", "BIGINT", "ID");
        deptIdAttr.setId(true);
        deptAttrs.put("id", deptIdAttr);
        AttributeMetadata deptNameAttr = new AttributeMetadata("name", "String", "VARCHAR", "NAME");
        deptAttrs.put("name", deptNameAttr);
        department.setAttributes(deptAttrs);

        List<RelationshipMetadata> deptRels = new ArrayList<>();
        RelationshipMetadata relEmployees = new RelationshipMetadata("employees", "Employee", "OneToMany");
        relEmployees.setLazy(true);
        relEmployees.setOwningSide(false);
        relEmployees.setMappedBy("department");
        relEmployees.setCascadePersist(true);
        relEmployees.setCascadeMerge(true);
        relEmployees.setCascadeRemove(true);
        relEmployees.setBatchFetchType("EXISTS");
        relEmployees.setOrphanRemoval(true);
        deptRels.add(relEmployees);
        department.setRelationships(deptRels);

        EntityNode employee = new EntityNode("Employee", "annotation.model", "ENTITY");
        Map<String, AttributeMetadata> empAttrs = new HashMap<>();
        AttributeMetadata empIdAttr = new AttributeMetadata("id", "Long", "BIGINT", "ID");
        empIdAttr.setId(true);
        empAttrs.put("id", empIdAttr);
        AttributeMetadata empNameAttr = new AttributeMetadata("name", "String", "VARCHAR", "NAME");
        empAttrs.put("name", empNameAttr);
        employee.setAttributes(empAttrs);

        List<RelationshipMetadata> empRels = new ArrayList<>();
        RelationshipMetadata relDepartment = new RelationshipMetadata("department", "Department", "ManyToOne");
        relDepartment.setLazy(true);
        relDepartment.setOwningSide(true);
        relDepartment.setMappedBy("employees");
        relDepartment.setBatchFetchType("JOIN");
        relDepartment.setJoinFetch(true);
        empRels.add(relDepartment);
        employee.setRelationships(empRels);

        List<EntityNode> nodes = Arrays.asList(document, tag, event, versioned, vehicle, car, truck, department,
                employee);
        runAnalysis(nodes, "Annotation_Model", "annotation-report.json");
    }

    private static void generatePhase1Report() throws Exception {
        // Entity with ObjectTypeConverter
        EntityNode enumEntity = new EntityNode("EnumEntity", "phase1.model", "ENTITY");
        Map<String, AttributeMetadata> enumAttrs = new HashMap<>();
        AttributeMetadata idAttr = new AttributeMetadata("id", "Long", "BIGINT", "ID");
        idAttr.setId(true);
        enumAttrs.put("id", idAttr);

        AttributeMetadata statusAttr = new AttributeMetadata("status", "String", "VARCHAR", "STATUS");
        statusAttr.setObjectTypeConverter(true);
        statusAttr.setObjectTypeDataType("java.lang.String");
        statusAttr.setObjectTypeObjectType("com.mycompany.StatusEnum");
        enumAttrs.put("status", statusAttr);
        enumEntity.setAttributes(enumAttrs);
        enumEntity.setRelationships(new ArrayList<>());

        // Entity with DirectMapMapping and AggregateCollection
        EntityNode advancedEntity = new EntityNode("AdvancedEntity", "phase1.model", "ENTITY");
        Map<String, AttributeMetadata> advAttrs = new HashMap<>();
        AttributeMetadata advId = new AttributeMetadata("id", "Long", "BIGINT", "ID");
        advId.setId(true);
        advAttrs.put("id", advId);
        advancedEntity.setAttributes(advAttrs);

        List<RelationshipMetadata> advRels = new ArrayList<>();

        // DirectMapMapping (Map<String, Integer>)
        RelationshipMetadata relDirectMap = new RelationshipMetadata("configMap", "java.lang.Integer", "DirectMap");
        relDirectMap.setDirectMapMapping(true);
        relDirectMap.setMapKeyType("java.lang.String");
        relDirectMap.setMapValueType("java.lang.Integer");
        advRels.add(relDirectMap);

        // AggregateCollection (List<Embeddable>)
        RelationshipMetadata relAggCol = new RelationshipMetadata("historyEntries", "HistoryEntry",
                "AggregateCollection");
        relAggCol.setAggregateCollection(true);
        relAggCol.setLazy(false); // EAGER warning
        advRels.add(relAggCol);

        advancedEntity.setRelationships(advRels);

        List<EntityNode> nodes = Arrays.asList(enumEntity, advancedEntity);
        runAnalysis(nodes, "Phase1_Model", "phase1-report.json");
    }

    private static void generatePhase2Report() throws Exception {
        // TransformationMapping Entity
        EntityNode transformEntity = new EntityNode("TransformEntity", "phase2.model", "ENTITY");
        Map<String, AttributeMetadata> trAttrs = new HashMap<>();
        AttributeMetadata id = new AttributeMetadata("id", "Long", "BIGINT", "ID");
        id.setId(true);
        trAttrs.put("id", id);

        AttributeMetadata fullAddress = new AttributeMetadata("fullAddress", "String", "VARCHAR", "FULL_ADDR");
        fullAddress.setTransformationMapping(true);
        fullAddress.setTransformationMethodName("buildFullAddress");
        trAttrs.put("fullAddress", fullAddress);
        transformEntity.setAttributes(trAttrs);
        transformEntity.setRelationships(new ArrayList<>());

        // VariableOneToOne Entity
        EntityNode varEntity = new EntityNode("PolyEntity", "phase2.model", "ENTITY");
        Map<String, AttributeMetadata> varAttrs = new HashMap<>();
        AttributeMetadata varId = new AttributeMetadata("id", "Long", "BIGINT", "ID");
        varId.setId(true);
        varAttrs.put("id", varId);
        varEntity.setAttributes(varAttrs);

        List<RelationshipMetadata> varRels = new ArrayList<>();
        RelationshipMetadata relVar = new RelationshipMetadata("contact", "ContactInterface", "VariableOneToOne");
        relVar.setVariableOneToOne(true);
        relVar.setVariableDiscriminatorColumn("CONTACT_TYPE");
        varRels.add(relVar);
        varEntity.setRelationships(varRels);

        // DirectCollection (Primitive Array) & ArrayMapping (DB Array)
        EntityNode arrayEntity = new EntityNode("ArrayEntity", "phase2.model", "ENTITY");
        Map<String, AttributeMetadata> arrAttrs = new HashMap<>();
        AttributeMetadata arrId = new AttributeMetadata("id", "Long", "BIGINT", "ID");
        arrId.setId(true);
        arrAttrs.put("id", arrId);
        arrayEntity.setAttributes(arrAttrs);

        List<RelationshipMetadata> arrRels = new ArrayList<>();

        // DirectCollection (List<String>)
        RelationshipMetadata relDirectCol = new RelationshipMetadata("tags", "String", "DirectCollection");
        relDirectCol.setDirectCollection(true);
        arrRels.add(relDirectCol);

        // ArrayMapping (String[])
        RelationshipMetadata relArray = new RelationshipMetadata("scores", "Integer", "Array");
        relArray.setArrayMapping(true);
        relArray.setArrayStructureName("SCORES_VARRAY");
        arrRels.add(relArray);

        arrayEntity.setRelationships(arrRels);

        // Converter Entity
        EntityNode convEntity = new EntityNode("ConvEntity", "phase2.model", "ENTITY");
        Map<String, AttributeMetadata> convAttrs = new HashMap<>();
        AttributeMetadata convId = new AttributeMetadata("id", "Long", "BIGINT", "ID");
        convId.setId(true);
        convAttrs.put("id", convId);

        AttributeMetadata serAttr = new AttributeMetadata("config", "ConfigObj", "BLOB", "CONFIG");
        serAttr.setSerializedObjectConverter(true);
        convAttrs.put("config", serAttr);

        AttributeMetadata typeAttr = new AttributeMetadata("legacyStatus", "String", "CHAR", "STATUS");
        typeAttr.setTypeConversionConverter(true);
        convAttrs.put("legacyStatus", typeAttr);

        convEntity.setAttributes(convAttrs);
        convEntity.setRelationships(new ArrayList<>());

        // NestedTable Entity
        EntityNode nestedEntity = new EntityNode("NestedEntity", "phase2.model", "ENTITY");
        Map<String, AttributeMetadata> nestedAttrs = new HashMap<>();
        nestedAttrs.put("id", createIdAttribute());
        nestedEntity.setAttributes(nestedAttrs);
        RelationshipMetadata nestedRel = new RelationshipMetadata();
        nestedRel.setAttributeName("nestedItems");
        nestedRel.setTargetEntity("NestedItem");
        nestedRel.setMappingType("NestedTable");
        nestedRel.setNestedTable(true);
        nestedEntity.setRelationships(Collections.singletonList(nestedRel));

        List<EntityNode> nodes = Arrays.asList(transformEntity, varEntity, arrayEntity, convEntity, nestedEntity);
        runAnalysis(nodes, "Phase2_Model", "phase2-report.json");
    }

    private static void runAnalysis(List<EntityNode> nodes, String dbName, String outputPath) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1", "sa", "");
        // Simplified DDL generation
        for (EntityNode node : nodes) {
            // Skip non-table types: MAPPED_SUPERCLASS, EMBEDDABLE, ABSTRACT_ENTITY
            if ("MAPPED_SUPERCLASS".equals(node.getType()) ||
                    "EMBEDDABLE".equals(node.getType()) ||
                    "ABSTRACT_ENTITY".equals(node.getType())) {
                continue;
            }
            StringBuilder ddl = new StringBuilder("CREATE TABLE " + node.getName().toUpperCase() + " (");
            boolean first = true;
            for (AttributeMetadata attr : node.getAttributes().values()) {
                if (!first)
                    ddl.append(", ");
                ddl.append(attr.getColumnName()).append(" ").append(attr.getDatabaseType());
                first = false;
            }
            ddl.append(")");
            conn.createStatement().execute(ddl.toString());
        }

        DDLInspector inspector = new DDLInspector();
        Map<String, DDLInspector.TableMetadata> schema = inspector.inspectSchema(conn);

        AnalysisRunner runner = new AnalysisRunner();
        List<MappingRule> rules = Arrays.asList(
                new EagerFetchRule(),
                new RelationshipOwnerRule(),
                new RedundantUpdateRule(),
                new OptimisticLockingRule(),
                new ForeignKeyIndexRule(),
                new LargeCollectionRule(),
                new SelfReferencingRule(),
                new CacheRule(),
                new InheritanceRule(),
                new LobRule(),
                new TemporalRule(),
                new VersionRule(),
                new BatchFetchRule(),
                new InheritanceStrategyRule(),
                new DiscriminatorRule(),
                new NPlusOneQueryRule(),
                new CartesianProductRule(),
                new MappedSuperclassRule(), // New
                new IndirectionPolicyRule(), // New
                new ObjectTypeConverterRule(), // Phase 1
                new DirectMapRule(), // Phase 1
                new AggregateCollectionRule(), // Phase 1
                new IndexRule(),
                // Phase 2 Mappings
                new TransformationMappingRule(),
                new VariableOneToOneRule(),
                new DirectCollectionRule(),
                new ArrayMappingRule(),
                new NestedTableRule(),
                new ConverterRule());

        List<GlobalMappingRule> globalRules = Arrays.asList(
                new GraphAnalysisRule());

        runner.runAnalysis(nodes, schema, rules, globalRules, outputPath);
    }

    private static void generateComprehensiveReport() throws Exception {
        System.out.println("Generating Comprehensive Reference Report...");
        List<EntityNode> allNodes = new ArrayList<>();

        // 1. Standard Mappings & Indirection (ValueHolder)
        EntityNode stdEntity = new EntityNode("StandardEntity", "ref.model", "ENTITY");
        stdEntity.setAttributes(Collections.singletonMap("id", createIdAttribute()));
        List<RelationshipMetadata> stdRels = new ArrayList<>();
        stdRels.add(createRel("eagerOneToOne", "TargetA", "OneToOne", false)); // Eager Risk
        stdRels.add(createRel("lazyOneToMany", "TargetB", "OneToMany", true));

        // ValueHolder (OldLazy)
        RelationshipMetadata vhRel = new RelationshipMetadata("legacyIndirection", "TargetA", "OneToOne");
        vhRel.setIndirectionType("VALUEHOLDER");
        stdRels.add(vhRel);
        stdEntity.setRelationships(stdRels);
        allNodes.add(stdEntity);
        allNodes.add(new EntityNode("TargetA", "ref.model", "ENTITY"));
        allNodes.add(new EntityNode("TargetB", "ref.model", "ENTITY"));

        // 2. Phase 1 Extras (ObjectTypeConverter, DirectMap, AggCol)
        EntityNode extra1 = new EntityNode("Phase1Extra", "ref.model", "ENTITY");
        Map<String, AttributeMetadata> p1Attrs = new HashMap<>();
        p1Attrs.put("id", createIdAttribute());
        AttributeMetadata statusAttr = new AttributeMetadata("status", "String", "VARCHAR", "STATUS");
        statusAttr.setObjectTypeConverter(true);
        p1Attrs.put("status", statusAttr);
        extra1.setAttributes(p1Attrs);

        List<RelationshipMetadata> p1Rels = new ArrayList<>();
        RelationshipMetadata relDirectMap = new RelationshipMetadata("configMap", "Integer", "DirectMap");
        relDirectMap.setDirectMapMapping(true);
        p1Rels.add(relDirectMap);

        RelationshipMetadata relAggCol = new RelationshipMetadata("history", "TargetA", "AggregateCollection");
        relAggCol.setAggregateCollection(true);
        p1Rels.add(relAggCol);
        extra1.setRelationships(p1Rels);
        allNodes.add(extra1);

        // 3. Phase 2 Mappings (Transformation, VarOneToOne, Array, Nested, Converters)
        // Transformation
        EntityNode transEntity = new EntityNode("TransEntity", "ref.model", "ENTITY");
        Map<String, AttributeMetadata> transAttrs = new HashMap<>();
        transAttrs.put("id", createIdAttribute());
        AttributeMetadata trAttr = new AttributeMetadata("fullAddr", "String", "VARCHAR", "ADDR");
        trAttr.setTransformationMapping(true);
        transAttrs.put("fullAddr", trAttr);
        transEntity.setAttributes(transAttrs);
        transEntity.setRelationships(new ArrayList<>());
        allNodes.add(transEntity);

        // VariableOneToOne
        EntityNode varEntity = new EntityNode("VarOneToOneEntity", "ref.model", "ENTITY");
        varEntity.setAttributes(Collections.singletonMap("id", createIdAttribute()));
        RelationshipMetadata varRel = new RelationshipMetadata("contact", "ContactInterface", "VariableOneToOne");
        varRel.setVariableOneToOne(true);
        varEntity.setRelationships(Collections.singletonList(varRel));
        allNodes.add(varEntity);

        // Arrays & DirectCollection
        EntityNode arrayEntity = new EntityNode("ArrayEntity", "ref.model", "ENTITY");
        arrayEntity.setAttributes(Collections.singletonMap("id", createIdAttribute()));
        List<RelationshipMetadata> arrRels = new ArrayList<>();
        RelationshipMetadata relArr = new RelationshipMetadata("scores", "Integer", "Array");
        relArr.setArrayMapping(true);
        arrRels.add(relArr);
        RelationshipMetadata relDirCol = new RelationshipMetadata("tags", "String", "DirectCollection");
        relDirCol.setDirectCollection(true);
        arrRels.add(relDirCol);
        arrayEntity.setRelationships(arrRels);
        allNodes.add(arrayEntity);

        // Converters & NestedTable
        EntityNode advEntity = new EntityNode("AdvancedEntity", "ref.model", "ENTITY");
        Map<String, AttributeMetadata> advAttrs = new HashMap<>();
        advAttrs.put("id", createIdAttribute());
        AttributeMetadata serAttr = new AttributeMetadata("blob", "Object", "BLOB", "BLOB");
        serAttr.setSerializedObjectConverter(true);
        advAttrs.put("blob", serAttr);
        AttributeMetadata typeAttr = new AttributeMetadata("type", "String", "CHAR", "TYPE");
        typeAttr.setTypeConversionConverter(true);
        advAttrs.put("type", typeAttr);
        advEntity.setAttributes(advAttrs);

        RelationshipMetadata nestedRel = new RelationshipMetadata("nestedData", "NestedType", "NestedTable");
        nestedRel.setNestedTable(true);
        advEntity.setRelationships(Collections.singletonList(nestedRel));
        allNodes.add(advEntity);

        runAnalysis(allNodes, "Comprehensive_Model", "comprehensive-report.json");
    }

    private static RelationshipMetadata createRel(String name, String target, String type, boolean lazy) {
        RelationshipMetadata r = new RelationshipMetadata(name, target, type);
        r.setLazy(lazy);
        return r;
    }

    /**
     * Generates a complex scenario report with varied and realistic mapping
     * patterns
     * to comprehensively test all frontend visualization features.
     */
    private static void generateComplexScenarioReport() throws Exception {
        System.out.println("Generating Complex Scenario Report...");
        List<EntityNode> allNodes = new ArrayList<>();

        // ==== 1. E-COMMERCE DOMAIN WITH INHERITANCE ====
        // Abstract Product (SINGLE_TABLE inheritance)
        EntityNode product = new EntityNode("Product", "ecommerce", "ABSTRACT_ENTITY");
        product.setInheritanceStrategy("SINGLE_TABLE");
        product.setDiscriminatorColumn("PRODUCT_TYPE");
        product.setDiscriminatorValue("PRODUCT");
        Map<String, AttributeMetadata> productAttrs = new HashMap<>();
        productAttrs.put("id", createIdAttribute());
        AttributeMetadata nameAttr = new AttributeMetadata("name", "String", "VARCHAR", "NAME");
        productAttrs.put("name", nameAttr);
        AttributeMetadata priceAttr = new AttributeMetadata("price", "BigDecimal", "DECIMAL", "PRICE");
        productAttrs.put("price", priceAttr);
        AttributeMetadata versionAttr = new AttributeMetadata("version", "Integer", "INTEGER", "VERSION");
        versionAttr.setVersion(true);
        productAttrs.put("version", versionAttr);
        product.setAttributes(productAttrs);
        product.setRelationships(new ArrayList<>());
        allNodes.add(product);

        // PhysicalProduct (inherits from Product)
        EntityNode physicalProduct = new EntityNode("PhysicalProduct", "ecommerce", "ENTITY");
        physicalProduct.setParentEntity("Product");
        physicalProduct.setInheritanceStrategy("SINGLE_TABLE");
        physicalProduct.setDiscriminatorValue("PHYSICAL");
        Map<String, AttributeMetadata> physAttrs = new HashMap<>();
        AttributeMetadata weightAttr = new AttributeMetadata("weight", "Double", "DOUBLE", "WEIGHT");
        physAttrs.put("weight", weightAttr);
        physicalProduct.setAttributes(physAttrs);

        List<RelationshipMetadata> physRels = new ArrayList<>();
        // Eager relationship (performance risk)
        RelationshipMetadata warehouseRel = createRel("warehouse", "Warehouse", "ManyToOne", false);
        warehouseRel.setOwningSide(true);
        physRels.add(warehouseRel);
        physicalProduct.setRelationships(physRels);
        allNodes.add(physicalProduct);

        // DigitalProduct (inherits from Product) with Converters
        EntityNode digitalProduct = new EntityNode("DigitalProduct", "ecommerce", "ENTITY");
        digitalProduct.setParentEntity("Product");
        digitalProduct.setInheritanceStrategy("SINGLE_TABLE");
        digitalProduct.setDiscriminatorValue("DIGITAL");
        Map<String, AttributeMetadata> digAttrs = new HashMap<>();
        AttributeMetadata downloadUrlAttr = new AttributeMetadata("downloadUrl", "String", "VARCHAR", "DOWNLOAD_URL");
        digAttrs.put("downloadUrl", downloadUrlAttr);
        AttributeMetadata licenseAttr = new AttributeMetadata("licenseType", "String", "VARCHAR", "LICENSE");
        licenseAttr.setObjectTypeConverter(true);
        licenseAttr.setObjectTypeDataType("java.lang.String");
        licenseAttr.setObjectTypeObjectType("com.ecommerce.LicenseType");
        digAttrs.put("licenseType", licenseAttr);
        AttributeMetadata metadataAttr = new AttributeMetadata("metadata", "Object", "BLOB", "METADATA");
        metadataAttr.setSerializedObjectConverter(true);
        digAttrs.put("metadata", metadataAttr);
        digitalProduct.setAttributes(digAttrs);
        digitalProduct.setRelationships(new ArrayList<>());
        allNodes.add(digitalProduct);

        // ==== 2. SALES ORDER AGGREGATE WITH COMPLEX RELATIONSHIPS ====
        EntityNode order = new EntityNode("SalesOrder", "ecommerce", "ENTITY");
        order.setDddRole("AGGREGATE_ROOT");
        order.setAggregateName("SalesOrder");
        Map<String, AttributeMetadata> orderAttrs = new HashMap<>();
        orderAttrs.put("id", createIdAttribute());
        AttributeMetadata orderNumAttr = new AttributeMetadata("orderNumber", "String", "VARCHAR", "ORDER_NUM");
        orderAttrs.put("orderNumber", orderNumAttr);
        AttributeMetadata statusAttr = new AttributeMetadata("status", "String", "VARCHAR", "STATUS");
        statusAttr.setEnumerated(true);
        orderAttrs.put("status", statusAttr);
        AttributeMetadata createdAttr = new AttributeMetadata("createdAt", "java.util.Date", "TIMESTAMP", "CREATED_AT");
        createdAttr.setTemporal(true);
        createdAttr.setTemporalType("TIMESTAMP");
        orderAttrs.put("createdAt", createdAttr);
        order.setAttributes(orderAttrs);

        List<RelationshipMetadata> orderRels = new ArrayList<>();
        // Bidirectional OneToMany (lazy, with batch fetch needed)
        RelationshipMetadata itemsRel = createRel("items", "OrderItem", "OneToMany", true);
        itemsRel.setOwningSide(false);
        itemsRel.setMappedBy("order");
        itemsRel.setCascadePersist(true);
        itemsRel.setCascadeRemove(true);
        itemsRel.setOrphanRemoval(true);
        orderRels.add(itemsRel);

        // Cross-aggregate reference (should show cut-point)
        RelationshipMetadata customerRel = createRel("customer", "Customer", "ManyToOne", true);
        customerRel.setOwningSide(true);
        customerRel.setIndirectionType("VALUEHOLDER"); // Legacy indirection
        orderRels.add(customerRel);

        // Eager relationship (Cartesian product risk with items)
        RelationshipMetadata paymentRel = createRel("payment", "Payment", "OneToOne", false);
        paymentRel.setOwningSide(true);
        orderRels.add(paymentRel);

        order.setRelationships(orderRels);
        allNodes.add(order);

        // OrderItem (part of SalesOrder aggregate)
        EntityNode orderItem = new EntityNode("OrderItem", "ecommerce", "ENTITY");
        orderItem.setDddRole("ENTITY");
        orderItem.setAggregateName("SalesOrder");
        Map<String, AttributeMetadata> itemAttrs = new HashMap<>();
        itemAttrs.put("id", createIdAttribute());
        AttributeMetadata qtyAttr = new AttributeMetadata("quantity", "Integer", "INTEGER", "QTY");
        itemAttrs.put("quantity", qtyAttr);
        AttributeMetadata unitPriceAttr = new AttributeMetadata("unitPrice", "BigDecimal", "DECIMAL", "UNIT_PRICE");
        itemAttrs.put("unitPrice", unitPriceAttr);
        orderItem.setAttributes(itemAttrs);

        List<RelationshipMetadata> itemRels = new ArrayList<>();
        RelationshipMetadata orderBackRel = createRel("salesOrder", "SalesOrder", "ManyToOne", true);
        orderBackRel.setOwningSide(true);
        orderBackRel.setMappedBy("items");
        itemRels.add(orderBackRel);

        // VariableOneToOne for polymorphic product reference
        RelationshipMetadata productRel = new RelationshipMetadata("product", "Product", "VariableOneToOne");
        productRel.setVariableOneToOne(true);
        productRel.setVariableDiscriminatorColumn("PRODUCT_TYPE");
        productRel.setLazy(true);
        itemRels.add(productRel);

        orderItem.setRelationships(itemRels);
        allNodes.add(orderItem);

        // ==== 3. CUSTOMER AGGREGATE WITH ADVANCED MAPPINGS ====
        EntityNode customer = new EntityNode("Customer", "ecommerce", "ENTITY");
        customer.setDddRole("AGGREGATE_ROOT");
        customer.setAggregateName("Customer");
        Map<String, AttributeMetadata> custAttrs = new HashMap<>();
        custAttrs.put("id", createIdAttribute());
        AttributeMetadata emailAttr = new AttributeMetadata("email", "String", "VARCHAR", "EMAIL");
        emailAttr.setUnique(true);
        custAttrs.put("email", emailAttr);

        // TransformationMapping for full name
        AttributeMetadata fullNameAttr = new AttributeMetadata("fullName", "String", "VARCHAR", "FULL_NAME");
        fullNameAttr.setTransformationMapping(true);
        fullNameAttr.setTransformationMethodName("buildFullName");
        custAttrs.put("fullName", fullNameAttr);

        // TypeConversionConverter for legacy status
        AttributeMetadata legacyStatusAttr = new AttributeMetadata("legacyStatus", "String", "CHAR", "LEGACY_STATUS");
        legacyStatusAttr.setTypeConversionConverter(true);
        custAttrs.put("legacyStatus", legacyStatusAttr);

        customer.setAttributes(custAttrs);

        List<RelationshipMetadata> custRels = new ArrayList<>();
        // DirectCollection for tags
        RelationshipMetadata tagsRel = new RelationshipMetadata("tags", "String", "DirectCollection");
        tagsRel.setDirectCollection(true);
        tagsRel.setLazy(true);
        custRels.add(tagsRel);

        // DirectMapMapping for preferences
        RelationshipMetadata prefsRel = new RelationshipMetadata("preferences", "String", "DirectMap");
        prefsRel.setDirectMapMapping(true);
        prefsRel.setMapKeyType("java.lang.String");
        prefsRel.setMapValueType("java.lang.String");
        prefsRel.setLazy(true);
        custRels.add(prefsRel);

        // AggregateCollection for addresses
        RelationshipMetadata addressesRel = new RelationshipMetadata("addresses", "Address", "AggregateCollection");
        addressesRel.setAggregateCollection(true);
        addressesRel.setLazy(false); // EAGER - will trigger warning
        custRels.add(addressesRel);

        customer.setRelationships(custRels);
        allNodes.add(customer);

        // ==== 4. WAREHOUSE WITH ARRAY MAPPINGS ====
        EntityNode warehouse = new EntityNode("Warehouse", "ecommerce", "ENTITY");
        warehouse.setDddRole("AGGREGATE_ROOT");
        warehouse.setAggregateName("Warehouse");
        Map<String, AttributeMetadata> whAttrs = new HashMap<>();
        whAttrs.put("id", createIdAttribute());
        AttributeMetadata whNameAttr = new AttributeMetadata("name", "String", "VARCHAR", "NAME");
        whAttrs.put("name", whNameAttr);
        warehouse.setAttributes(whAttrs);

        List<RelationshipMetadata> whRels = new ArrayList<>();
        // ArrayMapping for capacity zones
        RelationshipMetadata zonesRel = new RelationshipMetadata("capacityZones", "Integer", "Array");
        zonesRel.setArrayMapping(true);
        zonesRel.setArrayStructureName("ZONES_VARRAY");
        zonesRel.setLazy(true);
        whRels.add(zonesRel);

        // NestedTable for inventory items
        RelationshipMetadata inventoryRel = new RelationshipMetadata("inventory", "InventoryItem", "NestedTable");
        inventoryRel.setNestedTable(true);
        inventoryRel.setLazy(true);
        whRels.add(inventoryRel);

        warehouse.setRelationships(whRels);
        allNodes.add(warehouse);

        // ==== 5. PAYMENT WITH SELF-REFERENCING ====
        EntityNode payment = new EntityNode("Payment", "ecommerce", "ENTITY");
        payment.setDddRole("ENTITY");
        payment.setAggregateName("SalesOrder");
        Map<String, AttributeMetadata> payAttrs = new HashMap<>();
        payAttrs.put("id", createIdAttribute());
        AttributeMetadata amountAttr = new AttributeMetadata("amount", "BigDecimal", "DECIMAL", "AMOUNT");
        payAttrs.put("amount", amountAttr);
        payment.setAttributes(payAttrs);

        List<RelationshipMetadata> payRels = new ArrayList<>();
        // Self-referencing for refund chain
        RelationshipMetadata refundRel = createRel("refundedPayment", "Payment", "ManyToOne", true);
        refundRel.setOwningSide(true);
        payRels.add(refundRel);

        RelationshipMetadata refundsRel = createRel("refunds", "Payment", "OneToMany", true);
        refundsRel.setOwningSide(false);
        refundsRel.setMappedBy("refundedPayment");
        payRels.add(refundsRel);

        payment.setRelationships(payRels);
        allNodes.add(payment);

        // ==== 6. SUPPORT ENTITIES ====
        // Address (Embeddable - no table)
        EntityNode address = new EntityNode("Address", "ecommerce", "EMBEDDABLE");
        Map<String, AttributeMetadata> addrAttrs = new HashMap<>();
        AttributeMetadata streetAttr = new AttributeMetadata("street", "String", "VARCHAR", "STREET");
        addrAttrs.put("street", streetAttr);
        AttributeMetadata cityAttr = new AttributeMetadata("city", "String", "VARCHAR", "CITY");
        addrAttrs.put("city", cityAttr);
        address.setAttributes(addrAttrs);
        address.setRelationships(new ArrayList<>());
        allNodes.add(address);

        // InventoryItem (for NestedTable)
        EntityNode inventoryItem = new EntityNode("InventoryItem", "ecommerce", "ENTITY");
        Map<String, AttributeMetadata> invAttrs = new HashMap<>();
        invAttrs.put("id", createIdAttribute());
        AttributeMetadata skuAttr = new AttributeMetadata("sku", "String", "VARCHAR", "SKU");
        invAttrs.put("sku", skuAttr);
        inventoryItem.setAttributes(invAttrs);
        inventoryItem.setRelationships(new ArrayList<>());
        allNodes.add(inventoryItem);

        runAnalysis(allNodes, "Complex_Scenario", "complex-scenario-report.json");
    }

    /**
     * Generates the complete progressive catalog of scenarios.
     */
    private static void generateProgressiveCatalog() throws Exception {
        // Level 1: Basic JPA Foundations
        generateLevel1Basic();
        // Level 2: JPA Intermediate
        generateLevel2Intermediate();
        // Level 3: JPA Advanced
        generateLevel3Advanced();
        // Level 4: EclipseLink Specific
        generateLevel4EclipseLink();
        // Level 5: EclipseLink Advanced Mappings
        generateLevel5AdvancedMappings();
        // Level 6: Anti-Patterns & Issues
        generateLevel6AntiPatterns();
        // Level 7: Real-World (keep existing complex-scenario as 7.2)
        generateLevel7RealWorld();
    }

    // ==================== LEVEL 1: BASIC JPA ====================
    private static void generateLevel1Basic() throws Exception {
        System.out.println("Level 1: Basic JPA Foundations...");

        // 1.1 Basic Entity
        List<EntityNode> nodes = new ArrayList<>();
        EntityNode person = new EntityNode("Person", "demo.basic", "ENTITY");
        Map<String, AttributeMetadata> attrs = new HashMap<>();
        attrs.put("id", createIdAttribute());
        AttributeMetadata nameAttr = new AttributeMetadata("name", "String", "VARCHAR", "NAME");
        attrs.put("name", nameAttr);
        AttributeMetadata emailAttr = new AttributeMetadata("email", "String", "VARCHAR", "EMAIL");
        emailAttr.setUnique(true);
        attrs.put("email", emailAttr);
        AttributeMetadata ageAttr = new AttributeMetadata("age", "Integer", "INTEGER", "AGE");
        attrs.put("age", ageAttr);
        person.setAttributes(attrs);
        person.setRelationships(new ArrayList<>());
        nodes.add(person);
        runAnalysis(nodes, "L1_Basic_Entity", "catalog/1-1-basic-entity.json");

        // 1.2 Basic Relationship (Unidirectional)
        nodes = new ArrayList<>();
        EntityNode author = new EntityNode("Author", "demo.basic", "ENTITY");
        Map<String, AttributeMetadata> authAttrs = new HashMap<>();
        authAttrs.put("id", createIdAttribute());
        authAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "NAME"));
        author.setAttributes(authAttrs);
        List<RelationshipMetadata> authRels = new ArrayList<>();
        RelationshipMetadata bioRel = createRel("biography", "Biography", "OneToOne", true);
        bioRel.setOwningSide(true);
        authRels.add(bioRel);
        author.setRelationships(authRels);
        nodes.add(author);

        EntityNode bio = new EntityNode("Biography", "demo.basic", "ENTITY");
        Map<String, AttributeMetadata> bioAttrs = new HashMap<>();
        bioAttrs.put("id", createIdAttribute());
        bioAttrs.put("content", new AttributeMetadata("content", "String", "CLOB", "CONTENT"));
        bio.setAttributes(bioAttrs);
        bio.setRelationships(new ArrayList<>());
        nodes.add(bio);
        runAnalysis(nodes, "L1_Basic_Relationship", "catalog/1-2-basic-relationship.json");

        // 1.3 Bidirectional
        nodes = new ArrayList<>();
        EntityNode dept = new EntityNode("Department", "demo.basic", "ENTITY");
        Map<String, AttributeMetadata> deptAttrs = new HashMap<>();
        deptAttrs.put("id", createIdAttribute());
        deptAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "NAME"));
        dept.setAttributes(deptAttrs);
        List<RelationshipMetadata> deptRels = new ArrayList<>();
        RelationshipMetadata empsRel = createRel("employees", "Employee", "OneToMany", true);
        empsRel.setOwningSide(false);
        empsRel.setMappedBy("department");
        deptRels.add(empsRel);
        dept.setRelationships(deptRels);
        nodes.add(dept);

        EntityNode emp = new EntityNode("Employee", "demo.basic", "ENTITY");
        Map<String, AttributeMetadata> empAttrs = new HashMap<>();
        empAttrs.put("id", createIdAttribute());
        empAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "NAME"));
        emp.setAttributes(empAttrs);
        List<RelationshipMetadata> empRels = new ArrayList<>();
        RelationshipMetadata deptRef = createRel("department", "Department", "ManyToOne", true);
        deptRef.setOwningSide(true);
        empRels.add(deptRef);
        emp.setRelationships(empRels);
        nodes.add(emp);
        runAnalysis(nodes, "L1_Bidirectional", "catalog/1-3-bidirectional.json");

        // 1.4 Many-to-Many
        nodes = new ArrayList<>();
        EntityNode student = new EntityNode("Student", "demo.basic", "ENTITY");
        Map<String, AttributeMetadata> stuAttrs = new HashMap<>();
        stuAttrs.put("id", createIdAttribute());
        stuAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "NAME"));
        student.setAttributes(stuAttrs);
        List<RelationshipMetadata> stuRels = new ArrayList<>();
        RelationshipMetadata coursesRel = createRel("courses", "Course", "ManyToMany", true);
        coursesRel.setOwningSide(true);
        stuRels.add(coursesRel);
        student.setRelationships(stuRels);
        nodes.add(student);

        EntityNode course = new EntityNode("Course", "demo.basic", "ENTITY");
        Map<String, AttributeMetadata> crsAttrs = new HashMap<>();
        crsAttrs.put("id", createIdAttribute());
        crsAttrs.put("title", new AttributeMetadata("title", "String", "VARCHAR", "TITLE"));
        course.setAttributes(crsAttrs);
        List<RelationshipMetadata> crsRels = new ArrayList<>();
        RelationshipMetadata studentsRel = createRel("students", "Student", "ManyToMany", true);
        studentsRel.setOwningSide(false);
        studentsRel.setMappedBy("courses");
        crsRels.add(studentsRel);
        course.setRelationships(crsRels);
        nodes.add(course);
        runAnalysis(nodes, "L1_ManyToMany", "catalog/1-4-many-to-many.json");
    }

    // ==================== LEVEL 2: JPA INTERMEDIATE ====================
    private static void generateLevel2Intermediate() throws Exception {
        System.out.println("Level 2: JPA Intermediate...");

        // 2.1 Inheritance Single Table
        List<EntityNode> nodes = new ArrayList<>();
        EntityNode vehicle = new EntityNode("Vehicle", "demo.inheritance", "ENTITY");
        vehicle.setInheritanceStrategy("SINGLE_TABLE");
        vehicle.setDiscriminatorColumn("VEHICLE_TYPE");
        vehicle.setDiscriminatorValue("VEHICLE");
        Map<String, AttributeMetadata> vehAttrs = new HashMap<>();
        vehAttrs.put("id", createIdAttribute());
        vehAttrs.put("brand", new AttributeMetadata("brand", "String", "VARCHAR", "BRAND"));
        vehicle.setAttributes(vehAttrs);
        vehicle.setRelationships(new ArrayList<>());
        nodes.add(vehicle);

        EntityNode car = new EntityNode("Car", "demo.inheritance", "ENTITY");
        car.setParentEntity("Vehicle");
        car.setInheritanceStrategy("SINGLE_TABLE");
        car.setDiscriminatorValue("CAR");
        Map<String, AttributeMetadata> carAttrs = new HashMap<>();
        carAttrs.put("numDoors", new AttributeMetadata("numDoors", "Integer", "INTEGER", "NUM_DOORS"));
        car.setAttributes(carAttrs);
        car.setRelationships(new ArrayList<>());
        nodes.add(car);

        EntityNode motorcycle = new EntityNode("Motorcycle", "demo.inheritance", "ENTITY");
        motorcycle.setParentEntity("Vehicle");
        motorcycle.setInheritanceStrategy("SINGLE_TABLE");
        motorcycle.setDiscriminatorValue("MOTO");
        Map<String, AttributeMetadata> motoAttrs = new HashMap<>();
        motoAttrs.put("engineCC", new AttributeMetadata("engineCC", "Integer", "INTEGER", "ENGINE_CC"));
        motorcycle.setAttributes(motoAttrs);
        motorcycle.setRelationships(new ArrayList<>());
        nodes.add(motorcycle);
        runAnalysis(nodes, "L2_Inheritance_Single", "catalog/2-1-inheritance-single.json");

        // 2.2 Inheritance Joined
        nodes = new ArrayList<>();
        EntityNode payment = new EntityNode("PaymentMethod", "demo.inheritance", "ENTITY");
        payment.setInheritanceStrategy("JOINED");
        Map<String, AttributeMetadata> payAttrs = new HashMap<>();
        payAttrs.put("id", createIdAttribute());
        payAttrs.put("ownerName", new AttributeMetadata("ownerName", "String", "VARCHAR", "OWNER_NAME"));
        payment.setAttributes(payAttrs);
        payment.setRelationships(new ArrayList<>());
        nodes.add(payment);

        EntityNode creditCard = new EntityNode("CreditCard", "demo.inheritance", "ENTITY");
        creditCard.setParentEntity("PaymentMethod");
        creditCard.setInheritanceStrategy("JOINED");
        Map<String, AttributeMetadata> ccAttrs = new HashMap<>();
        ccAttrs.put("cardNumber", new AttributeMetadata("cardNumber", "String", "VARCHAR", "CARD_NUM"));
        ccAttrs.put("expiryDate", new AttributeMetadata("expiryDate", "String", "VARCHAR", "EXPIRY"));
        creditCard.setAttributes(ccAttrs);
        creditCard.setRelationships(new ArrayList<>());
        nodes.add(creditCard);

        EntityNode bankAccount = new EntityNode("BankAccount", "demo.inheritance", "ENTITY");
        bankAccount.setParentEntity("PaymentMethod");
        bankAccount.setInheritanceStrategy("JOINED");
        Map<String, AttributeMetadata> baAttrs = new HashMap<>();
        baAttrs.put("iban", new AttributeMetadata("iban", "String", "VARCHAR", "IBAN"));
        baAttrs.put("bic", new AttributeMetadata("bic", "String", "VARCHAR", "BIC"));
        bankAccount.setAttributes(baAttrs);
        bankAccount.setRelationships(new ArrayList<>());
        nodes.add(bankAccount);
        runAnalysis(nodes, "L2_Inheritance_Joined", "catalog/2-2-inheritance-joined.json");

        // 2.3 Embedded
        nodes = new ArrayList<>();
        EntityNode company = new EntityNode("Company", "demo.embedded", "ENTITY");
        Map<String, AttributeMetadata> compAttrs = new HashMap<>();
        compAttrs.put("id", createIdAttribute());
        compAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "NAME"));
        company.setAttributes(compAttrs);
        List<RelationshipMetadata> compRels = new ArrayList<>();
        RelationshipMetadata addrRel = new RelationshipMetadata("address", "CompanyAddress", "Embedded");
        addrRel.setLazy(false);
        compRels.add(addrRel);
        company.setRelationships(compRels);
        nodes.add(company);

        EntityNode compAddr = new EntityNode("CompanyAddress", "demo.embedded", "EMBEDDABLE");
        Map<String, AttributeMetadata> addrAttrs = new HashMap<>();
        addrAttrs.put("street", new AttributeMetadata("street", "String", "VARCHAR", "STREET"));
        addrAttrs.put("city", new AttributeMetadata("city", "String", "VARCHAR", "CITY"));
        addrAttrs.put("zipCode", new AttributeMetadata("zipCode", "String", "VARCHAR", "ZIP"));
        compAddr.setAttributes(addrAttrs);
        compAddr.setRelationships(new ArrayList<>());
        nodes.add(compAddr);
        runAnalysis(nodes, "L2_Embedded", "catalog/2-3-embedded.json");

        // 2.4 Element Collection
        nodes = new ArrayList<>();
        EntityNode user = new EntityNode("UserProfile", "demo.collection", "ENTITY");
        Map<String, AttributeMetadata> userAttrs = new HashMap<>();
        userAttrs.put("id", createIdAttribute());
        userAttrs.put("username", new AttributeMetadata("username", "String", "VARCHAR", "USERNAME"));
        user.setAttributes(userAttrs);
        List<RelationshipMetadata> userRels = new ArrayList<>();
        RelationshipMetadata phonesRel = new RelationshipMetadata("phoneNumbers", "String", "ElementCollection");
        phonesRel.setLazy(true);
        userRels.add(phonesRel);
        RelationshipMetadata tagsRel = new RelationshipMetadata("tags", "String", "ElementCollection");
        tagsRel.setLazy(true);
        userRels.add(tagsRel);
        user.setRelationships(userRels);
        nodes.add(user);
        runAnalysis(nodes, "L2_ElementCollection", "catalog/2-4-element-collection.json");
    }

    // ==================== LEVEL 3: JPA ADVANCED ====================
    private static void generateLevel3Advanced() throws Exception {
        System.out.println("Level 3: JPA Advanced...");

        // 3.1 Lazy vs Eager
        List<EntityNode> nodes = new ArrayList<>();
        EntityNode blog = new EntityNode("Blog", "demo.fetch", "ENTITY");
        Map<String, AttributeMetadata> blogAttrs = new HashMap<>();
        blogAttrs.put("id", createIdAttribute());
        blogAttrs.put("title", new AttributeMetadata("title", "String", "VARCHAR", "TITLE"));
        blog.setAttributes(blogAttrs);
        List<RelationshipMetadata> blogRels = new ArrayList<>();
        RelationshipMetadata postsRel = createRel("posts", "BlogPost", "OneToMany", true);
        postsRel.setOwningSide(false);
        postsRel.setMappedBy("blog");
        blogRels.add(postsRel);
        RelationshipMetadata ownerRel = createRel("owner", "BlogOwner", "ManyToOne", false); // EAGER!
        ownerRel.setOwningSide(true);
        blogRels.add(ownerRel);
        blog.setRelationships(blogRels);
        nodes.add(blog);

        EntityNode post = new EntityNode("BlogPost", "demo.fetch", "ENTITY");
        Map<String, AttributeMetadata> postAttrs = new HashMap<>();
        postAttrs.put("id", createIdAttribute());
        postAttrs.put("content", new AttributeMetadata("content", "String", "CLOB", "CONTENT"));
        post.setAttributes(postAttrs);
        List<RelationshipMetadata> postRels = new ArrayList<>();
        RelationshipMetadata blogRef = createRel("blog", "Blog", "ManyToOne", true);
        blogRef.setOwningSide(true);
        postRels.add(blogRef);
        RelationshipMetadata commentsRel = createRel("comments", "Comment", "OneToMany", false); // EAGER!
        commentsRel.setOwningSide(false);
        postRels.add(commentsRel);
        post.setRelationships(postRels);
        nodes.add(post);

        EntityNode owner = new EntityNode("BlogOwner", "demo.fetch", "ENTITY");
        Map<String, AttributeMetadata> ownerAttrs = new HashMap<>();
        ownerAttrs.put("id", createIdAttribute());
        ownerAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "NAME"));
        owner.setAttributes(ownerAttrs);
        owner.setRelationships(new ArrayList<>());
        nodes.add(owner);

        EntityNode comment = new EntityNode("Comment", "demo.fetch", "ENTITY");
        Map<String, AttributeMetadata> comAttrs = new HashMap<>();
        comAttrs.put("id", createIdAttribute());
        comAttrs.put("text", new AttributeMetadata("text", "String", "VARCHAR", "TEXT"));
        comment.setAttributes(comAttrs);
        comment.setRelationships(new ArrayList<>());
        nodes.add(comment);
        runAnalysis(nodes, "L3_Lazy_Eager", "catalog/3-1-lazy-eager.json");

        // 3.2 Cascade Operations
        nodes = new ArrayList<>();
        EntityNode invoice = new EntityNode("Invoice", "demo.cascade", "ENTITY");
        Map<String, AttributeMetadata> invAttrs = new HashMap<>();
        invAttrs.put("id", createIdAttribute());
        invAttrs.put("invoiceNumber", new AttributeMetadata("invoiceNumber", "String", "VARCHAR", "INV_NUM"));
        invoice.setAttributes(invAttrs);
        List<RelationshipMetadata> invRels = new ArrayList<>();
        RelationshipMetadata linesRel = createRel("lines", "InvoiceLine", "OneToMany", true);
        linesRel.setOwningSide(false);
        linesRel.setMappedBy("invoice");
        linesRel.setCascadePersist(true);
        linesRel.setCascadeMerge(true);
        linesRel.setCascadeRemove(true);
        linesRel.setOrphanRemoval(true);
        invRels.add(linesRel);
        invoice.setRelationships(invRels);
        nodes.add(invoice);

        EntityNode line = new EntityNode("InvoiceLine", "demo.cascade", "ENTITY");
        Map<String, AttributeMetadata> lineAttrs = new HashMap<>();
        lineAttrs.put("id", createIdAttribute());
        lineAttrs.put("quantity", new AttributeMetadata("quantity", "Integer", "INTEGER", "QTY"));
        lineAttrs.put("amount", new AttributeMetadata("amount", "BigDecimal", "DECIMAL", "AMOUNT"));
        line.setAttributes(lineAttrs);
        List<RelationshipMetadata> lineRels = new ArrayList<>();
        RelationshipMetadata invRef = createRel("invoice", "Invoice", "ManyToOne", true);
        invRef.setOwningSide(true);
        lineRels.add(invRef);
        line.setRelationships(lineRels);
        nodes.add(line);
        runAnalysis(nodes, "L3_Cascade", "catalog/3-2-cascade-operations.json");

        // 3.3 Version & Temporal
        nodes = new ArrayList<>();
        EntityNode audit = new EntityNode("AuditedEntity", "demo.temporal", "ENTITY");
        Map<String, AttributeMetadata> audAttrs = new HashMap<>();
        audAttrs.put("id", createIdAttribute());
        AttributeMetadata verAttr = new AttributeMetadata("version", "Integer", "INTEGER", "VERSION");
        verAttr.setVersion(true);
        audAttrs.put("version", verAttr);
        AttributeMetadata createdAttr = new AttributeMetadata("createdAt", "java.util.Date", "TIMESTAMP", "CREATED_AT");
        createdAttr.setTemporal(true);
        createdAttr.setTemporalType("TIMESTAMP");
        audAttrs.put("createdAt", createdAttr);
        AttributeMetadata modifiedAttr = new AttributeMetadata("modifiedAt", "java.util.Date", "TIMESTAMP",
                "MODIFIED_AT");
        modifiedAttr.setTemporal(true);
        modifiedAttr.setTemporalType("TIMESTAMP");
        audAttrs.put("modifiedAt", modifiedAttr);
        AttributeMetadata birthDateAttr = new AttributeMetadata("birthDate", "java.util.Date", "DATE", "BIRTH_DATE");
        birthDateAttr.setTemporal(true);
        birthDateAttr.setTemporalType("DATE");
        audAttrs.put("birthDate", birthDateAttr);
        audit.setAttributes(audAttrs);
        audit.setRelationships(new ArrayList<>());
        nodes.add(audit);
        runAnalysis(nodes, "L3_Version_Temporal", "catalog/3-3-version-temporal.json");

        // 3.4 Converters
        nodes = new ArrayList<>();
        EntityNode config = new EntityNode("Configuration", "demo.converter", "ENTITY");
        Map<String, AttributeMetadata> cfgAttrs = new HashMap<>();
        cfgAttrs.put("id", createIdAttribute());
        AttributeMetadata statusAttr = new AttributeMetadata("status", "String", "VARCHAR", "STATUS");
        statusAttr.setEnumerated(true);
        cfgAttrs.put("status", statusAttr);
        AttributeMetadata jsonAttr = new AttributeMetadata("jsonData", "Object", "CLOB", "JSON_DATA");
        jsonAttr.setConvert(true);
        jsonAttr.setConverterName("JsonConverter");
        cfgAttrs.put("jsonData", jsonAttr);
        config.setAttributes(cfgAttrs);
        config.setRelationships(new ArrayList<>());
        nodes.add(config);
        runAnalysis(nodes, "L3_Converters", "catalog/3-4-converters.json");
    }

    // ==================== LEVEL 4: ECLIPSELINK SPECIFIC ====================
    private static void generateLevel4EclipseLink() throws Exception {
        System.out.println("Level 4: EclipseLink Specific...");

        // 4.1 Batch Fetch
        List<EntityNode> nodes = new ArrayList<>();
        EntityNode catalog = new EntityNode("ProductCatalog", "demo.batch", "ENTITY");
        Map<String, AttributeMetadata> catAttrs = new HashMap<>();
        catAttrs.put("id", createIdAttribute());
        catAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "NAME"));
        catalog.setAttributes(catAttrs);
        List<RelationshipMetadata> catRels = new ArrayList<>();
        RelationshipMetadata prodsRel = createRel("products", "CatalogProduct", "OneToMany", true);
        prodsRel.setOwningSide(false);
        prodsRel.setBatchFetchType("JOIN");
        catRels.add(prodsRel);
        catalog.setRelationships(catRels);
        nodes.add(catalog);

        EntityNode prod = new EntityNode("CatalogProduct", "demo.batch", "ENTITY");
        Map<String, AttributeMetadata> prodAttrs = new HashMap<>();
        prodAttrs.put("id", createIdAttribute());
        prodAttrs.put("sku", new AttributeMetadata("sku", "String", "VARCHAR", "SKU"));
        prod.setAttributes(prodAttrs);
        List<RelationshipMetadata> prodRels = new ArrayList<>();
        RelationshipMetadata catRef = createRel("catalog", "ProductCatalog", "ManyToOne", true);
        catRef.setOwningSide(true);
        prodRels.add(catRef);
        prod.setRelationships(prodRels);
        nodes.add(prod);
        runAnalysis(nodes, "L4_BatchFetch", "catalog/4-1-batch-fetch.json");

        // 4.2 Cache Config
        nodes = new ArrayList<>();
        EntityNode cached = new EntityNode("CachedReference", "demo.cache", "ENTITY");
        cached.setCacheType("SOFT_WEAK");
        cached.setCacheExpiry(3600);
        cached.setCacheCoordinationType("SEND_NEW_OBJECTS_WITH_CHANGES");
        Map<String, AttributeMetadata> cacheAttrs = new HashMap<>();
        cacheAttrs.put("id", createIdAttribute());
        cacheAttrs.put("code", new AttributeMetadata("code", "String", "VARCHAR", "CODE"));
        cacheAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "NAME"));
        cached.setAttributes(cacheAttrs);
        cached.setRelationships(new ArrayList<>());
        nodes.add(cached);
        runAnalysis(nodes, "L4_Cache", "catalog/4-2-cache-config.json");

        // 4.3 Indirection (ValueHolder)
        nodes = new ArrayList<>();
        EntityNode master = new EntityNode("MasterRecord", "demo.indirection", "ENTITY");
        Map<String, AttributeMetadata> mastAttrs = new HashMap<>();
        mastAttrs.put("id", createIdAttribute());
        mastAttrs.put("code", new AttributeMetadata("code", "String", "VARCHAR", "CODE"));
        master.setAttributes(mastAttrs);
        List<RelationshipMetadata> mastRels = new ArrayList<>();
        RelationshipMetadata detailRel = createRel("detail", "DetailRecord", "OneToOne", true);
        detailRel.setOwningSide(true);
        detailRel.setIndirectionType("VALUEHOLDER");
        mastRels.add(detailRel);
        RelationshipMetadata historyRel = createRel("history", "HistoryRecord", "OneToMany", true);
        historyRel.setOwningSide(false);
        historyRel.setIndirectionType("VALUEHOLDER");
        mastRels.add(historyRel);
        master.setRelationships(mastRels);
        nodes.add(master);

        EntityNode detail = new EntityNode("DetailRecord", "demo.indirection", "ENTITY");
        Map<String, AttributeMetadata> detAttrs = new HashMap<>();
        detAttrs.put("id", createIdAttribute());
        detAttrs.put("info", new AttributeMetadata("info", "String", "VARCHAR", "INFO"));
        detail.setAttributes(detAttrs);
        detail.setRelationships(new ArrayList<>());
        nodes.add(detail);

        EntityNode history = new EntityNode("HistoryRecord", "demo.indirection", "ENTITY");
        Map<String, AttributeMetadata> histAttrs = new HashMap<>();
        histAttrs.put("id", createIdAttribute());
        histAttrs.put("timestamp", new AttributeMetadata("timestamp", "java.util.Date", "TIMESTAMP", "TS"));
        history.setAttributes(histAttrs);
        history.setRelationships(new ArrayList<>());
        nodes.add(history);
        runAnalysis(nodes, "L4_Indirection", "catalog/4-3-indirection.json");

        // 4.4 Private Owned
        nodes = new ArrayList<>();
        EntityNode parent = new EntityNode("ParentAggregate", "demo.owned", "ENTITY");
        parent.setDddRole("AGGREGATE_ROOT");
        Map<String, AttributeMetadata> parAttrs = new HashMap<>();
        parAttrs.put("id", createIdAttribute());
        parAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "NAME"));
        parent.setAttributes(parAttrs);
        List<RelationshipMetadata> parRels = new ArrayList<>();
        RelationshipMetadata childRel = createRel("children", "OwnedChild", "OneToMany", true);
        childRel.setOwningSide(false);
        childRel.setPrivateOwned(true);
        childRel.setCascadeAll(true);
        parRels.add(childRel);
        parent.setRelationships(parRels);
        nodes.add(parent);

        EntityNode child = new EntityNode("OwnedChild", "demo.owned", "ENTITY");
        Map<String, AttributeMetadata> chAttrs = new HashMap<>();
        chAttrs.put("id", createIdAttribute());
        chAttrs.put("childValue", new AttributeMetadata("childValue", "String", "VARCHAR", "CHILD_VALUE"));
        child.setAttributes(chAttrs);
        child.setRelationships(new ArrayList<>());
        nodes.add(child);
        runAnalysis(nodes, "L4_PrivateOwned", "catalog/4-4-private-owned.json");
    }

    // ==================== LEVEL 5: ECLIPSELINK ADVANCED MAPPINGS
    // ====================
    private static void generateLevel5AdvancedMappings() throws Exception {
        System.out.println("Level 5: EclipseLink Advanced Mappings...");

        // 5.1 Transformation
        List<EntityNode> nodes = new ArrayList<>();
        EntityNode coord = new EntityNode("GeoCoordinate", "demo.transform", "ENTITY");
        Map<String, AttributeMetadata> coordAttrs = new HashMap<>();
        coordAttrs.put("id", createIdAttribute());
        AttributeMetadata latAttr = new AttributeMetadata("latitude", "Double", "DOUBLE", "LAT");
        latAttr.setTransformationMapping(true);
        latAttr.setTransformationMethodName("readLatitude");
        coordAttrs.put("latitude", latAttr);
        AttributeMetadata lonAttr = new AttributeMetadata("longitude", "Double", "DOUBLE", "LON");
        lonAttr.setTransformationMapping(true);
        lonAttr.setTransformationMethodName("readLongitude");
        coordAttrs.put("longitude", lonAttr);
        coord.setAttributes(coordAttrs);
        coord.setRelationships(new ArrayList<>());
        nodes.add(coord);
        runAnalysis(nodes, "L5_Transformation", "catalog/5-1-transformation.json");

        // 5.2 Variable OneToOne
        nodes = new ArrayList<>();
        EntityNode container = new EntityNode("MediaContainer", "demo.variable", "ENTITY");
        Map<String, AttributeMetadata> contAttrs = new HashMap<>();
        contAttrs.put("id", createIdAttribute());
        contAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "NAME"));
        container.setAttributes(contAttrs);
        List<RelationshipMetadata> contRels = new ArrayList<>();
        RelationshipMetadata mediaRel = new RelationshipMetadata("media", "MediaContent", "VariableOneToOne");
        mediaRel.setVariableOneToOne(true);
        mediaRel.setVariableDiscriminatorColumn("MEDIA_TYPE");
        mediaRel.setLazy(true);
        contRels.add(mediaRel);
        container.setRelationships(contRels);
        nodes.add(container);

        EntityNode image = new EntityNode("ImageContent", "demo.variable", "ENTITY");
        Map<String, AttributeMetadata> imgAttrs = new HashMap<>();
        imgAttrs.put("id", createIdAttribute());
        imgAttrs.put("width", new AttributeMetadata("width", "Integer", "INTEGER", "WIDTH"));
        imgAttrs.put("height", new AttributeMetadata("height", "Integer", "INTEGER", "HEIGHT"));
        image.setAttributes(imgAttrs);
        image.setRelationships(new ArrayList<>());
        nodes.add(image);

        EntityNode video = new EntityNode("VideoContent", "demo.variable", "ENTITY");
        Map<String, AttributeMetadata> vidAttrs = new HashMap<>();
        vidAttrs.put("id", createIdAttribute());
        vidAttrs.put("duration", new AttributeMetadata("duration", "Integer", "INTEGER", "DURATION"));
        video.setAttributes(vidAttrs);
        video.setRelationships(new ArrayList<>());
        nodes.add(video);
        runAnalysis(nodes, "L5_VariableOneToOne", "catalog/5-2-variable-onetoone.json");

        // 5.3 Direct Collection & Map
        nodes = new ArrayList<>();
        EntityNode profile = new EntityNode("UserSettings", "demo.direct", "ENTITY");
        Map<String, AttributeMetadata> profAttrs = new HashMap<>();
        profAttrs.put("id", createIdAttribute());
        profAttrs.put("username", new AttributeMetadata("username", "String", "VARCHAR", "USERNAME"));
        profile.setAttributes(profAttrs);
        List<RelationshipMetadata> profRels = new ArrayList<>();
        RelationshipMetadata tagsRel = new RelationshipMetadata("tags", "String", "DirectCollection");
        tagsRel.setDirectCollection(true);
        tagsRel.setLazy(true);
        profRels.add(tagsRel);
        RelationshipMetadata prefsRel = new RelationshipMetadata("preferences", "String", "DirectMap");
        prefsRel.setDirectMapMapping(true);
        prefsRel.setMapKeyType("java.lang.String");
        prefsRel.setMapValueType("java.lang.String");
        prefsRel.setLazy(true);
        profRels.add(prefsRel);
        profile.setRelationships(profRels);
        nodes.add(profile);
        runAnalysis(nodes, "L5_DirectCollectionMap", "catalog/5-3-direct-collection.json");

        // 5.4 Aggregate Collection
        nodes = new ArrayList<>();
        EntityNode portfolio = new EntityNode("Portfolio", "demo.aggregate", "ENTITY");
        Map<String, AttributeMetadata> portAttrs = new HashMap<>();
        portAttrs.put("id", createIdAttribute());
        portAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "NAME"));
        portfolio.setAttributes(portAttrs);
        List<RelationshipMetadata> portRels = new ArrayList<>();
        RelationshipMetadata holdingsRel = new RelationshipMetadata("holdings", "Holding", "AggregateCollection");
        holdingsRel.setAggregateCollection(true);
        holdingsRel.setLazy(true);
        portRels.add(holdingsRel);
        portfolio.setRelationships(portRels);
        nodes.add(portfolio);

        EntityNode holding = new EntityNode("Holding", "demo.aggregate", "EMBEDDABLE");
        Map<String, AttributeMetadata> holdAttrs = new HashMap<>();
        holdAttrs.put("symbol", new AttributeMetadata("symbol", "String", "VARCHAR", "SYMBOL"));
        holdAttrs.put("quantity", new AttributeMetadata("quantity", "Integer", "INTEGER", "QTY"));
        holding.setAttributes(holdAttrs);
        holding.setRelationships(new ArrayList<>());
        nodes.add(holding);
        runAnalysis(nodes, "L5_AggregateCollection", "catalog/5-4-aggregate-collection.json");

        // 5.5 Array & Nested Table
        nodes = new ArrayList<>();
        EntityNode dataStore = new EntityNode("DataStore", "demo.oracle", "ENTITY");
        Map<String, AttributeMetadata> dsAttrs = new HashMap<>();
        dsAttrs.put("id", createIdAttribute());
        dsAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "NAME"));
        dataStore.setAttributes(dsAttrs);
        List<RelationshipMetadata> dsRels = new ArrayList<>();
        RelationshipMetadata valuesRel = new RelationshipMetadata("values", "Integer", "Array");
        valuesRel.setArrayMapping(true);
        valuesRel.setArrayStructureName("INT_VARRAY");
        valuesRel.setLazy(true);
        dsRels.add(valuesRel);
        RelationshipMetadata recordsRel = new RelationshipMetadata("records", "DataRecord", "NestedTable");
        recordsRel.setNestedTable(true);
        recordsRel.setLazy(true);
        dsRels.add(recordsRel);
        dataStore.setRelationships(dsRels);
        nodes.add(dataStore);

        EntityNode record = new EntityNode("DataRecord", "demo.oracle", "ENTITY");
        Map<String, AttributeMetadata> recAttrs = new HashMap<>();
        recAttrs.put("id", createIdAttribute());
        recAttrs.put("dataValue", new AttributeMetadata("dataValue", "String", "VARCHAR", "DATA_VALUE"));
        record.setAttributes(recAttrs);
        record.setRelationships(new ArrayList<>());
        nodes.add(record);
        runAnalysis(nodes, "L5_ArrayNestedTable", "catalog/5-5-array-nested.json");
    }

    // ==================== LEVEL 6: ANTI-PATTERNS ====================
    private static void generateLevel6AntiPatterns() throws Exception {
        System.out.println("Level 6: Anti-Patterns & Issues...");

        // 6.1 Circular References
        List<EntityNode> nodes = new ArrayList<>();
        EntityNode nodeA = new EntityNode("NodeA", "demo.cycle", "ENTITY");
        Map<String, AttributeMetadata> aAttrs = new HashMap<>();
        aAttrs.put("id", createIdAttribute());
        nodeA.setAttributes(aAttrs);
        List<RelationshipMetadata> aRels = new ArrayList<>();
        aRels.add(createRel("toB", "NodeB", "ManyToOne", true));
        nodeA.setRelationships(aRels);
        nodes.add(nodeA);

        EntityNode nodeB = new EntityNode("NodeB", "demo.cycle", "ENTITY");
        Map<String, AttributeMetadata> bAttrs = new HashMap<>();
        bAttrs.put("id", createIdAttribute());
        nodeB.setAttributes(bAttrs);
        List<RelationshipMetadata> bRels = new ArrayList<>();
        bRels.add(createRel("toC", "NodeC", "ManyToOne", true));
        nodeB.setRelationships(bRels);
        nodes.add(nodeB);

        EntityNode nodeC = new EntityNode("NodeC", "demo.cycle", "ENTITY");
        Map<String, AttributeMetadata> cAttrs = new HashMap<>();
        cAttrs.put("id", createIdAttribute());
        nodeC.setAttributes(cAttrs);
        List<RelationshipMetadata> cRels = new ArrayList<>();
        cRels.add(createRel("toA", "NodeA", "ManyToOne", true)); // Cycle!
        nodeC.setRelationships(cRels);
        nodes.add(nodeC);
        runAnalysis(nodes, "L6_Circular", "catalog/6-1-circular-refs.json");

        // 6.2 Cartesian Product Risk
        nodes = new ArrayList<>();
        EntityNode report = new EntityNode("SalesReport", "demo.cartesian", "ENTITY");
        Map<String, AttributeMetadata> repAttrs = new HashMap<>();
        repAttrs.put("id", createIdAttribute());
        repAttrs.put("title", new AttributeMetadata("title", "String", "VARCHAR", "TITLE"));
        report.setAttributes(repAttrs);
        List<RelationshipMetadata> repRels = new ArrayList<>();
        // Multiple EAGER collections = Cartesian product!
        RelationshipMetadata reg1 = createRel("regions", "Region", "OneToMany", false);
        reg1.setOwningSide(false);
        repRels.add(reg1);
        RelationshipMetadata prod1 = createRel("products", "ReportProduct", "OneToMany", false);
        prod1.setOwningSide(false);
        repRels.add(prod1);
        RelationshipMetadata cust1 = createRel("customers", "ReportCustomer", "OneToMany", false);
        cust1.setOwningSide(false);
        repRels.add(cust1);
        report.setRelationships(repRels);
        nodes.add(report);

        EntityNode region = new EntityNode("Region", "demo.cartesian", "ENTITY");
        region.setAttributes(Map.of("id", createIdAttribute()));
        region.setRelationships(new ArrayList<>());
        nodes.add(region);

        EntityNode rprod = new EntityNode("ReportProduct", "demo.cartesian", "ENTITY");
        rprod.setAttributes(Map.of("id", createIdAttribute()));
        rprod.setRelationships(new ArrayList<>());
        nodes.add(rprod);

        EntityNode rcust = new EntityNode("ReportCustomer", "demo.cartesian", "ENTITY");
        rcust.setAttributes(Map.of("id", createIdAttribute()));
        rcust.setRelationships(new ArrayList<>());
        nodes.add(rcust);
        runAnalysis(nodes, "L6_Cartesian", "catalog/6-2-cartesian-product.json");

        // 6.3 Missing Optimizations
        nodes = new ArrayList<>();
        EntityNode largeEntity = new EntityNode("LargeEntity", "demo.missing", "ENTITY");
        Map<String, AttributeMetadata> lgAttrs = new HashMap<>();
        lgAttrs.put("id", createIdAttribute());
        lgAttrs.put("name", new AttributeMetadata("name", "String", "VARCHAR", "NAME"));
        largeEntity.setAttributes(lgAttrs);
        List<RelationshipMetadata> lgRels = new ArrayList<>();
        // No batch fetch, no join fetch
        RelationshipMetadata items = createRel("items", "LargeItem", "OneToMany", true);
        items.setOwningSide(false);
        // No batchFetch set -> N+1 risk
        lgRels.add(items);
        largeEntity.setRelationships(lgRels);
        nodes.add(largeEntity);

        EntityNode lItem = new EntityNode("LargeItem", "demo.missing", "ENTITY");
        Map<String, AttributeMetadata> liAttrs = new HashMap<>();
        liAttrs.put("id", createIdAttribute());
        lItem.setAttributes(liAttrs);
        lItem.setRelationships(new ArrayList<>());
        nodes.add(lItem);
        runAnalysis(nodes, "L6_MissingOpt", "catalog/6-3-missing-optimizations.json");
    }

    // ==================== LEVEL 7: REAL-WORLD ====================
    private static void generateLevel7RealWorld() throws Exception {
        System.out.println("Level 7: Real-World Integration...");

        // 7.1 DDD Aggregates (reuse complex scenario patterns)
        // Just reference to complex-scenario-report.json in the catalog

        // 7.2 is handled by ofbiz-report.json

        System.out.println("Level 7 uses existing reports: complex-scenario-report.json and ofbiz-report.json");
    }

    private static AttributeMetadata createIdAttribute() {
        AttributeMetadata id = new AttributeMetadata("id", "Long", "BIGINT", "ID");
        id.setId(true);
        return id;
    }
}
