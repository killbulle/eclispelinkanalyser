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
                new IndexRule());

        List<GlobalMappingRule> globalRules = Arrays.asList(
                new GraphAnalysisRule());

        runner.runAnalysis(nodes, schema, rules, globalRules, outputPath);
    }
}
