package com.eclipselink.analyzer.stress;

import com.eclipselink.analyzer.model.AttributeMetadata;
import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OFBizConverter {

    public List<EntityNode> convertFolder(String folderPath) {
        List<EntityNode> allNodes = new ArrayList<>();
        File folder = new File(folderPath);
        if (!folder.exists())
            return allNodes;

        scanAndConvert(folder, allNodes);
        return allNodes;
    }

    private void scanAndConvert(File file, List<EntityNode> allNodes) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    scanAndConvert(child, allNodes);
                }
            }
        } else if (file.getName().equals("entitymodel.xml")) {
            allNodes.addAll(convertFile(file));
        }
    }

    public List<EntityNode> convertFile(File file) {
        List<EntityNode> nodes = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            NodeList entityList = doc.getElementsByTagName("entity");
            for (int i = 0; i < entityList.getLength(); i++) {
                Element entityElem = (Element) entityList.item(i);
                nodes.add(parseEntity(entityElem));
            }

            NodeList viewEntityList = doc.getElementsByTagName("view-entity");
            for (int i = 0; i < viewEntityList.getLength(); i++) {
                Element viewElem = (Element) viewEntityList.item(i);
                EntityNode node = parseEntity(viewElem);
                node.setType("VIEW_ENTITY");
                nodes.add(node);
            }

        } catch (Exception e) {
            System.err.println("Error parsing OFBiz file " + file.getAbsolutePath() + ": " + e.getMessage());
        }
        return nodes;
    }

    private EntityNode parseEntity(Element elem) {
        EntityNode node = new EntityNode();
        String name = elem.getAttribute("entity-name");
        node.setName(name);
        node.setPackageName(elem.getAttribute("package-name"));
        node.setType("ENTITY");

        Map<String, AttributeMetadata> attributes = new HashMap<>();
        NodeList fields = elem.getElementsByTagName("field");
        for (int i = 0; i < fields.getLength(); i++) {
            Element fieldElem = (Element) fields.item(i);
            AttributeMetadata attr = new AttributeMetadata();
            attr.setName(fieldElem.getAttribute("name"));
            attr.setJavaType(fieldElem.getAttribute("type")); // OFBiz type as java type proxy
            attr.setColumnName(fieldElem.getAttribute("name"));
            attributes.put(attr.getName(), attr);
        }
        node.setAttributes(attributes);

        List<RelationshipMetadata> relationships = new ArrayList<>();
        NodeList relations = elem.getElementsByTagName("relation");
        for (int i = 0; i < relations.getLength(); i++) {
            Element relElem = (Element) relations.item(i);
            RelationshipMetadata rel = new RelationshipMetadata();
            rel.setAttributeName(relElem.getAttribute("title") + relElem.getAttribute("rel-entity-name"));
            rel.setTargetEntity(relElem.getAttribute("rel-entity-name"));
            rel.setMappingType(relElem.getAttribute("type").equals("one") ? "OneToOne" : "OneToMany");
            rel.setLazy(true);
            rel.setOwningSide(true);
            relationships.add(rel);
        }
        node.setRelationships(relationships);
        node.setViolations(new ArrayList<>());

        return node;
    }
}
