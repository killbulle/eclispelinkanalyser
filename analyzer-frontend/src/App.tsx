import { useState, useCallback, useEffect } from 'react';
import ReactFlow, {
  Background,
  Controls,
  MiniMap,
  useNodesState,
  useEdgesState,
  addEdge,
  Position,
  useReactFlow,
  ReactFlowProvider,
  BackgroundVariant
} from 'reactflow';
import type { Node, Edge, Connection } from 'reactflow';

import 'reactflow/dist/style.css';
import dagre from 'dagre';
import * as d3Force from 'd3-force';
import ELK from 'elkjs/lib/elk.bundled.js';
import EntityNode from './components/EntityNode';
import GroupNode from './components/GroupNode';
import { JPAView } from './components/JPAView';
import { TableView } from './components/TableView';
import { DDLView } from './components/DDLView';
import { jsPDF } from 'jspdf';
import { getAggregateForNode, heuristics } from './utils/aggregateHeuristics';
// @ts-ignore
import DDDRules from './analysis/shared-rules';
import { runAnalysis, type AnalysisReport as DDDReport } from './analysis/engine';
import { Box, AlertCircle, Loader2, Eye, GitGraph, Upload, CheckCircle2, ShieldAlert, Database, ChevronLeft, ChevronRight, FileDown, ChevronDown, Sun, Moon, Scissors, Layers, Cpu, Info, Languages } from 'lucide-react';
import { translations, type Language } from './translations';

interface AttributeMetadata {
  name: string;
  javaType: string;
  databaseType: string;
  columnName: string;
  lob?: boolean;
  temporal?: boolean;
  temporalType?: string;
  version?: boolean;
  enumerated?: boolean;
  convert?: boolean;
  converterName?: string;
  generatedValue?: boolean;
  generationStrategy?: string;
  nullable?: boolean;
  unique?: boolean;
  id?: boolean;
  transient?: boolean;
  basic?: boolean;
  elementCollection?: boolean;
  multitenantPrimaryKey?: boolean;
}


interface RelationshipMetadata {
  attributeName: string;
  targetEntity: string;
  mappingType: string;
  lazy: boolean;
  owningSide: boolean;
  mappedBy?: string;
  cascadePersist?: boolean;
  cascadeMerge?: boolean;
  cascadeRemove?: boolean;
  cascadeRefresh?: boolean;
  cascadeDetach?: boolean;
  cascadeAll?: boolean;
  orphanRemoval?: boolean;
  optional?: boolean;
  batchFetchType?: string;
  joinFetch?: boolean;
  privateOwned?: boolean;
  mutable?: boolean;
  readOnly?: boolean;
  indirectionType?: string;
  directCollection?: boolean;
  variableOneToOne?: boolean;
  arrayMapping?: boolean;
  nestedTable?: boolean;
  aggregateCollection?: boolean;
  directMapMapping?: boolean;
  structureMapping?: boolean;
  referenceMapping?: boolean;
  directToXMLTypeMapping?: boolean;
  multitenantPrimaryKey?: boolean;
  unidirectionalOneToMany?: boolean;
  objectArrayMapping?: boolean;
  structureName?: string;
}

interface Anomaly {
  entityName: string;
  propertyName?: string;
  severity: string;
  message: string;
}

interface Violation {
  ruleId: string;
  severity: string;
  message: string;
}

interface EntityNodeData {
  name: string;
  packageName: string;
  type: string;
  parentEntity?: string;
  inheritanceStrategy?: string;
  discriminatorColumn?: string;
  discriminatorValue?: string;
  attributes: Record<string, AttributeMetadata>;
  relationships?: RelationshipMetadata[];
  dddRole?: string;
  dddRoleConfidence?: number;
  aggregateName?: string;
  aggregateNameConfidence?: number;
  cutPointScore?: number;
  cutPointNormalized?: number;
  showAttributes: boolean;
  hasAnomalies: boolean;
  violations: Violation[];
  cacheType?: string;
  cacheSize?: number;
  cacheExpiry?: number;
  cacheCoordinationType?: string;
  cacheIsolation?: string;
  cacheAlwaysRefresh?: boolean;
  cacheRefreshOnlyIfNewer?: boolean;
  cacheDisableHits?: boolean;
}

interface AnalysisReport {
  nodes: EntityNodeData[];
  anomalies: Anomaly[];
  violations: Violation[];
  nativeDdl?: string;
}

const nodeTypes = {
  entityNode: EntityNode,
  groupNode: GroupNode,
};


const getLayoutedElements = (nodes: Node[], edges: Edge[], direction = 'TB') => {
  const dagreGraph = new dagre.graphlib.Graph();
  dagreGraph.setDefaultEdgeLabel(() => ({}));

  const isLarge = nodes.length > 50;
  dagreGraph.setGraph({
    rankdir: direction,
    ranksep: isLarge ? 150 : 100,
    nodesep: isLarge ? 80 : 100
  });

  nodes.forEach((node: Node) => {
    dagreGraph.setNode(node.id, { width: 180, height: node.data.showAttributes ? 200 : 40 });
  });

  edges.forEach((edge) => {
    dagreGraph.setEdge(edge.source, edge.target);
  });

  dagre.layout(dagreGraph);

  const newNodes = nodes.map((node) => {
    const nodeWithPosition = dagreGraph.node(node.id);
    return {
      ...node,
      targetPosition: Position.Top,
      sourcePosition: Position.Bottom,
      position: {
        x: nodeWithPosition.x - 110,
        y: nodeWithPosition.y - (node.data.showAttributes ? 100 : 25),
      },
    };
  });

  return { nodes: newNodes, edges };
};

const elk = new ELK();

const getElkLayout = async (nodes: Node[], edges: Edge[], options = {}) => {
  const elkNodes = nodes.map((node) => ({
    id: node.id,
    width: 250,
    height: node.data.showAttributes ? 300 : 80,
  }));

  const elkEdges = edges.map((edge) => ({
    id: edge.id,
    sources: [edge.source],
    targets: [edge.target],
  }));

  const layoutOptions = {
    'elk.algorithm': 'org.eclipse.elk.stress', // High-quality organic
    'elk.edgeRouting': 'SPLINES',
    'elk.spacing.nodeNode': '100',
    'org.eclipse.elk.stress.desiredEdgeLength': '200',
    ...options
  };

  const graph = {
    id: 'root',
    children: elkNodes,
    edges: elkEdges,
  };

  const layoutedGraph = await elk.layout(graph, { layoutOptions });

  const newNodes = nodes.map((node) => {
    const elkNode = layoutedGraph.children?.find((n) => n.id === node.id);
    return {
      ...node,
      position: {
        x: elkNode?.x || 0,
        y: elkNode?.y || 0,
      },
    };
  });

  return { nodes: newNodes, edges };
};

const getGridLayout = (nodes: Node[], edges: Edge[]) => {
  const columns = Math.ceil(Math.sqrt(nodes.length));
  const newNodes = nodes.map((node, index) => {
    return {
      ...node,
      position: {
        x: (index % columns) * 250,
        y: Math.floor(index / columns) * 150,
      },
    };
  });
  return { nodes: newNodes, edges };
};

const getRadialLayout = (nodes: Node[], edges: Edge[]) => {
  const newNodes = [...nodes];
  const centerX = 0;
  const centerY = 0;
  const layerSize = 15; // Nodes per circle layer

  newNodes.forEach((node, index) => {
    const layer = Math.floor(index / layerSize);
    const indexInLayer = index % layerSize;
    const radius = (layer + 1) * 300;
    const angle = (indexInLayer / layerSize) * 2 * Math.PI;

    node.position = {
      x: centerX + Math.cos(angle) * radius,
      y: centerY + Math.sin(angle) * radius,
    };
  });

  return { nodes: newNodes, edges };
};

// Cluster Layout: Groups nodes by aggregate with gravity towards cluster centers
const getClusterLayout = (nodes: Node[], edges: Edge[], getAggFn: (node: Node) => string) => {
  interface SimNode extends d3Force.SimulationNodeDatum {
    id: string;
    originalNode: Node;
    cluster: string;
  }

  interface SimLink extends d3Force.SimulationLinkDatum<SimNode> {
    source: string | SimNode;
    target: string | SimNode;
  }

  // Group nodes by aggregate
  const clusters = new Map<string, Node[]>();
  nodes.forEach(node => {
    const agg = getAggFn(node);
    if (!clusters.has(agg)) clusters.set(agg, []);
    clusters.get(agg)!.push(node);
  });

  // Calculate cluster center positions (arrange in a grid with more spacing)
  const clusterNames = Array.from(clusters.keys());
  const cols = Math.ceil(Math.sqrt(clusterNames.length));
  // Increase spacing based on cluster size for better separation
  const baseSpacing = 500;
  const clusterCenters = new Map<string, { x: number; y: number }>();

  clusterNames.forEach((name, idx) => {
    const row = Math.floor(idx / cols);
    const col = idx % cols;
    // Add extra spacing based on position
    const clusterSize = clusters.get(name)?.length || 1;
    const sizeFactor = Math.min(clusterSize * 40, 200);
    clusterCenters.set(name, {
      x: col * (baseSpacing + sizeFactor),
      y: row * (baseSpacing + sizeFactor)
    });
  });

  // Create simulation nodes
  const simNodes: SimNode[] = nodes.map((node) => {
    const cluster = getAggFn(node);
    const center = clusterCenters.get(cluster) || { x: 0, y: 0 };
    return {
      id: node.id,
      originalNode: node,
      cluster,
      x: center.x + (Math.random() - 0.5) * 150,
      y: center.y + (Math.random() - 0.5) * 150,
    };
  });

  // Create simulation links
  const simLinks: SimLink[] = edges.map(edge => ({
    source: edge.source,
    target: edge.target,
  }));

  // Create force simulation with stronger cluster gravity and collision
  const simulation = d3Force.forceSimulation(simNodes)
    .force('link', d3Force.forceLink<SimNode, SimLink>(simLinks)
      .id(d => d.id)
      .distance(150)
      .strength(0.3))
    .force('charge', d3Force.forceManyBody()
      .strength(-1200)
      .distanceMax(600))
    .force('collision', d3Force.forceCollide<SimNode>().radius(d => {
      const isExpanded = d.originalNode.data?.showAttributes;
      const w = 200;
      const h = isExpanded ? 260 : 60;
      return Math.sqrt(w * w + h * h) / 2 + 80; // More padding for clusters
    }))
    // Stronger pull to cluster center
    .force('clusterX', d3Force.forceX<SimNode>(d => clusterCenters.get(d.cluster)?.x || 0).strength(0.8))
    .force('clusterY', d3Force.forceY<SimNode>(d => clusterCenters.get(d.cluster)?.y || 0).strength(0.8))
    .stop();

  // Run simulation
  for (let i = 0; i < 300; i++) {
    simulation.tick();
  }

  // Apply positions
  const newNodes = simNodes.map(simNode => ({
    ...simNode.originalNode,
    targetPosition: Position.Top,
    sourcePosition: Position.Bottom,
    position: {
      x: simNode.x || 0,
      y: simNode.y || 0,
    },
  }));

  return { nodes: newNodes, edges };
};

function AnalyzerApp() {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [inspectorJpaTab, setInspectorJpaTab] = useState<'general' | 'cache'>('general');
  const [selectedEdgeId, setSelectedEdgeId] = useState<string | null>(null);
  const [isSystemOpen, setIsSystemOpen] = useState(true);
  const [isEntityOpen, setIsEntityOpen] = useState(false);
  const [isTuningOpen, setIsTuningOpen] = useState(false);
  const [expandedAlgo, setExpandedAlgo] = useState<'weak' | 'stability' | null>(null);
  const [stats, setStats] = useState({ nodes: 0, anomalies: 0, violations: 0, eager: 0, errorCount: 0, warningCount: 0, infoCount: 0 });
  const [showAttributes, setShowAttributes] = useState(false);
  const [selectedReport, setSelectedReport] = useState('demo-scenarios.json');
  const [activeTab, setActiveTab] = useState<'mapping' | 'performance' | 'ddl' | 'tuning' | 'overview'>('overview');
  const [analysisConfig, setAnalysisConfig] = useState({
    semanticProfile: 'GENERIC' as const,
    enableSemantic: true,
    enableTopology: true,
    voConfidenceThreshold: 0.5,
    instabilityStableThreshold: 0.3,
    instabilityUnstableThreshold: 0.7,
    weakLinkThreshold: 0.3,
  });
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [sidebarWidth, setSidebarWidth] = useState(480);
  const [isResizing, setIsResizing] = useState(false);
  const [theme, setTheme] = useState<'light' | 'dark'>('light');
  const [violationFilters, setViolationFilters] = useState<Record<string, boolean>>({ ERROR: true, WARNING: true, INFO: true });
  const [viewMode, setViewMode] = useState<'graph' | 'table' | 'ddl' | 'jpa'>('graph');
  const [nativeDdl, setNativeDdl] = useState<string | null>(null);
  const [dddReport, setDddReport] = useState<DDDReport | null>(null);
  const [focusedAggregate, setFocusedAggregate] = useState<string | null>(null);
  const [language, setLanguage] = useState<Language>('fr');
  const { fitView, getNodes, getEdges } = useReactFlow();

  const t = useCallback((key: keyof typeof translations['en']) => {
    return (translations[language] as any)[key] || (translations['en'] as any)[key] || key;
  }, [language]);

  const centerOnAggregate = useCallback((aggregateName: string | null) => {
    setFocusedAggregate(aggregateName);
    if (!aggregateName) {
      fitView({ duration: 800 });
      return;
    }

    const clusterNodes = nodes.filter(n => n.data.aggregateName === aggregateName);
    if (clusterNodes.length > 0) {
      fitView({
        nodes: clusterNodes,
        duration: 800,
        padding: 0.2
      });
    }
  }, [nodes, fitView]);

  // Sidebar resize handlers
  const handleResizeStart = useCallback((e: React.MouseEvent | React.TouchEvent) => {
    e.preventDefault();
    setIsResizing(true);
  }, []);

  const handleResize = useCallback((clientX: number) => {
    if (!isResizing) return;

    const newWidth = window.innerWidth - clientX;
    // Constrain width between 320px and 800px
    const constrainedWidth = Math.max(320, Math.min(800, newWidth));
    setSidebarWidth(constrainedWidth);
  }, [isResizing]);

  const handleResizeEnd = useCallback(() => {
    setIsResizing(false);
  }, []);

  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      handleResize(e.clientX);
    };

    const handleTouchMove = (e: TouchEvent) => {
      if (e.touches.length > 0) {
        handleResize(e.touches[0].clientX);
      }
    };

    const handleMouseUp = () => {
      handleResizeEnd();
    };

    const handleTouchEnd = () => {
      handleResizeEnd();
    };

    if (isResizing) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('touchmove', handleTouchMove);
      document.addEventListener('mouseup', handleMouseUp);
      document.addEventListener('touchend', handleTouchEnd);

      return () => {
        document.removeEventListener('mousemove', handleMouseMove);
        document.removeEventListener('touchmove', handleTouchMove);
        document.removeEventListener('mouseup', handleMouseUp);
        document.removeEventListener('touchend', handleTouchEnd);
      };
    }
  }, [isResizing, handleResize, handleResizeEnd]);

  const reports = [
    { id: 'catalog/1-1-basic-entity-real.json', name: '1.1 📗 Basic Entity (Real)' },
    { id: 'catalog/2-1-onetoone-real.json', name: '2.1 📘 OneToOne' },
    { id: 'catalog/2-2-onetomany-real.json', name: '2.2 📘 OneToMany' },
    { id: 'catalog/2-3-manytomany-real.json', name: '2.3 📘 ManyToMany' },
    { id: 'catalog/2-4-element-collection-real.json', name: '2.4 📘 ElementCollection' },
    { id: 'catalog/2-5-embedded-real.json', name: '2.5 📘 Embedded' },
    { id: 'catalog/2-5-mapped-superclass.json', name: '2.6 📘 Mapped Superclass' },
    { id: 'catalog/3-1-basic-converter-real.json', name: '3.1 📙 Basic Converter' },
    { id: 'catalog/3-2-object-type-real.json', name: '3.2 📙 ObjectType Converter' },
    { id: 'catalog/3-3-serialized-object-real.json', name: '3.3 📙 Serialized Object' },
    { id: 'catalog/4-1-batch-fetch-real.json', name: '4.1 🔷 Batch Fetch' },
    { id: 'catalog/4-2-cache-config-real.json', name: '4.2 🔷 Cache Config' },
    { id: 'catalog/4-3-indirection-real.json', name: '4.3 🔷 Indirection (ValueHolder)' },
    { id: 'catalog/4-4-private-owned-real.json', name: '4.4 🔷 Private Owned' },
    { id: 'catalog/5-1-transformation-real.json', name: '5.1 🔶 Transformation Mapping' },
    { id: 'catalog/5-2-variable-onetoone-real.json', name: '5.2 🔶 Variable OneToOne' },
    { id: 'catalog/5-3-direct-collection-real.json', name: '5.3 🔶 DirectCollection & Map' },
    { id: 'catalog/5-4-aggregate-collection-real.json', name: '5.4 🔶 AggregateCollection' },
    { id: 'catalog/5-5-array-real.json', name: '5.5 🔶 Array' },
    { id: 'catalog/6-1-circular-refs-real.json', name: '6.1 ⚠️ Circular References' },
    { id: 'catalog/6-2-cartesian-product-real.json', name: '6.2 ⚠️ Cartesian Product' },
    { id: 'catalog/6-3-missing-optimizations-real.json', name: '6.3 ⚠️ Missing Optimizations (Eager)' },
    { id: 'catalog/6-4-deep-cycle-verified.json', name: '6.4 ⚠️ Deep Cycle Verified' },
    { id: 'complex-scenario-report.json', name: '7.1 🏢 Complex Domain (E-commerce)' },
    { id: 'ofbiz-report.json', name: '7.2 🚀 OFBiz Stress Test (113 entities)' },
    { id: 'agent-report.json', name: 'DDDSample Analysis' },
  ];

  const processReportData = useCallback((data: AnalysisReport) => {
    const countViolationsForRelationship = (sourceEntity: string, rel: RelationshipMetadata): number => {
      let count = 0;
      const violations = data.violations || [];
      for (const violation of violations) {
        const msg = violation.message.toLowerCase();
        if (msg.includes(sourceEntity.toLowerCase()) && msg.includes(rel.attributeName.toLowerCase())) {
          count++;
        }
      }
      return count;
    };

    const getEdgeMarkers = (rel: RelationshipMetadata) => {
      const markers: { markerStart?: string; markerEnd?: string } = {};
      const isRelationship = rel.mappingType && ['OneToOne', 'OneToMany', 'ManyToOne', 'ManyToMany'].some(t => rel.mappingType.includes(t));
      if (!isRelationship) return markers;

      const isComposition = rel.cascadeAll || (rel.cascadePersist && rel.cascadeRemove);
      if (isComposition) markers.markerStart = 'url(#marker-composition)';
      else if (rel.owningSide) markers.markerStart = 'url(#marker-ownership-bar)';

      if (rel.mappingType) {
        const isOneToOne = rel.mappingType.includes('OneToOne');
        const isOneToMany = rel.mappingType.includes('OneToMany');
        const isManyToOne = rel.mappingType.includes('ManyToOne');
        const isManyToMany = rel.mappingType.includes('ManyToMany');

        if (isOneToOne) markers.markerEnd = 'url(#marker-standard-arrow)';
        else if (isOneToMany) {
          markers.markerEnd = 'url(#marker-crows-foot)';
          if (!markers.markerStart) markers.markerStart = 'url(#marker-standard-arrow)';
        } else if (isManyToOne) {
          markers.markerEnd = 'url(#marker-standard-arrow)';
          if (!markers.markerStart) markers.markerStart = 'url(#marker-crows-foot-start)';
        } else if (isManyToMany) {
          markers.markerEnd = 'url(#marker-crows-foot)';
          if (!markers.markerStart) markers.markerStart = 'url(#marker-crows-foot-start)';
        } else markers.markerEnd = 'url(#marker-standard-arrow)';
      } else markers.markerEnd = 'url(#marker-standard-arrow)';

      return markers;
    };

    const nodesList = data.nodes || [];
    const anomalies = data.anomalies || [];
    const violations = data.violations || [];
    setNativeDdl(data.nativeDdl || null);

    const nodesWithEagerRisk = new Set<string>();
    nodesList.forEach((n: EntityNodeData) => {
      if (n.relationships?.some((r: RelationshipMetadata) => !r.lazy)) nodesWithEagerRisk.add(n.name);
    });

    const potentialVOs = new Set<string>();
    nodesList.forEach((n: EntityNodeData) => {
      const rels = Array.isArray(n.relationships) ? n.relationships : [];
      if (n.type === 'EMBEDDABLE' || (rels.length === 0 && Object.keys(n.attributes || {}).length <= 3)) potentialVOs.add(n.name);
    });

    const embeddedEntities = new Set<string>();
    nodesList.forEach((n: EntityNodeData) => {
      if (n.relationships) {
        n.relationships.forEach((rel: RelationshipMetadata) => {
          if (rel.mappingType && (rel.mappingType === "Embedded" || rel.mappingType.includes("Embedded"))) embeddedEntities.add(rel.targetEntity);
        });
      }
    });

    const cutPoints = new Set<string>();
    const allNodesForRules = nodesList.map(n => ({ data: n }));
    nodesList.forEach((n: EntityNodeData) => {
      if (n.relationships?.some((r: RelationshipMetadata) => {
        const target = nodesList.find((t: EntityNodeData) => t.name === r.targetEntity);
        if (!target) return false;
        return DDDRules.isCutPointEdge(r, { data: n }, { data: target }, allNodesForRules);
      })) cutPoints.add(n.name);
    });

    const nodesInCycle = new Set<string>();
    const adj = new Map<string, string[]>();
    nodesList.forEach((n: EntityNodeData) => {
      const rels = Array.isArray(n.relationships) ? n.relationships : [];
      adj.set(n.name, rels.map(r => r.targetEntity));
    });

    const visitedForCycle = new Set<string>();
    const recursionStack = new Set<string>();
    const currentPath: string[] = [];

    const findCycles = (nodeName: string) => {
      visitedForCycle.add(nodeName);
      recursionStack.add(nodeName);
      currentPath.push(nodeName);
      const neighbors = adj.get(nodeName) || [];
      for (const neighbor of neighbors) {
        if (!visitedForCycle.has(neighbor)) findCycles(neighbor);
        else if (recursionStack.has(neighbor)) {
          const startIndex = currentPath.indexOf(neighbor);
          if (startIndex !== -1) {
            const cycle = currentPath.slice(startIndex);
            if (new Set(cycle).size > 2) cycle.forEach(name => nodesInCycle.add(name));
          }
        }
      }
      currentPath.pop();
      recursionStack.delete(nodeName);
    };

    nodesList.forEach(n => { if (!visitedForCycle.has(n.name)) findCycles(n.name); });

    const aggregateRoots = new Set<string>();
    const aggregateGroups = new Map<string, EntityNodeData[]>();
    nodesList.forEach((n: EntityNodeData) => {
      const agg = n.aggregateName || n.packageName?.split('.').pop() || 'General';
      if (!aggregateGroups.has(agg)) aggregateGroups.set(agg, []);
      aggregateGroups.get(agg)!.push(n);
    });

    aggregateGroups.forEach((members) => {
      const existingRoot = members.find(m => m.dddRole === 'AGGREGATE_ROOT' && !embeddedEntities.has(m.name));
      if (existingRoot) { aggregateRoots.add(existingRoot.name); return; }
      if (members.length === 1) { aggregateRoots.add(members[0].name); return; }
      let bestRoot: EntityNodeData = members[0];
      let bestScore = -Infinity;
      members.forEach(m => {
        if (m.type === 'EMBEDDABLE' || embeddedEntities.has(m.name)) return;
        const ownedRelCount = (m.relationships || []).filter((r: RelationshipMetadata) => r.owningSide).length;
        const totalRelCount = m.relationships?.length || 0;
        const incomingCount = nodesList.filter((other: EntityNodeData) => other.relationships?.some((r: RelationshipMetadata) => r.targetEntity === m.name)).length || 0;
        const score = ownedRelCount * 3 + totalRelCount - incomingCount * 2;
        if (score > bestScore) { bestScore = score; bestRoot = m; }
      });
      aggregateRoots.add(bestRoot.name);
    });

    const transformedNodes: Node[] = nodesList.map((n: EntityNodeData) => ({
      id: n.name,
      type: 'entityNode',
      position: { x: 0, y: 0 },
      data: {
        ...n,
        showAttributes: false,
        hasAnomalies: anomalies.some((a: Anomaly) => a.entityName === n.name),
        violations: violations.filter((v: Violation) => v.message.includes(n.name)),
        isCutPoint: cutPoints.has(n.name),
        isInCycle: nodesInCycle.has(n.name),
        hasEagerRisk: nodesWithEagerRisk.has(n.name),
        isPotentialVO: potentialVOs.has(n.name),
        dddRole: (!embeddedEntities.has(n.name) && n.dddRole) || (aggregateRoots.has(n.name) ? 'AGGREGATE_ROOT' : undefined),
        focusOpacity: 1
      },
    }));

    const transformedEdges: Edge[] = [];
    nodesList.forEach((n: EntityNodeData) => {
      const rels = Array.isArray(n.relationships) ? n.relationships : [];
      rels.forEach((rel: RelationshipMetadata, rIdx: number) => {
        const targetNode = nodesList.find((tn: EntityNodeData) => tn.name === rel.targetEntity);
        if (targetNode) {
          const violationCount = countViolationsForRelationship(n.name, rel);
          const hasViolations = violationCount > 0;
          const isEager = !rel.lazy;
          const hasProblems = isEager || hasViolations;
          const isCutPointEdge = DDDRules.isCutPointEdge(rel, { data: n }, { data: targetNode }, allNodesForRules);

          let strokeColor = '#10b981';
          let strokeDasharray = rel.owningSide ? '' : '5 5';

          if (rel.nestedTable) { strokeColor = '#f97316'; strokeDasharray = '3 3'; }
          else if (rel.arrayMapping) { strokeColor = '#64748b'; strokeDasharray = '1 1'; }
          else if (rel.variableOneToOne) { strokeColor = '#eab308'; strokeDasharray = '10 2'; }
          else if (rel.mappingType === 'Embedded') { strokeColor = '#a78bfa'; strokeDasharray = '8 4'; }
          else if (rel.mappingType === 'ElementCollection' || rel.directCollection || rel.aggregateCollection || rel.directMapMapping) { strokeColor = '#ec4899'; strokeDasharray = '4 2'; }

          if (isCutPointEdge) { strokeColor = '#00F0FF'; strokeDasharray = '10 5'; }
          else if (hasProblems) { strokeColor = '#f59e0b'; strokeDasharray = ''; }

          const fetchLabel = rel.indirectionType === 'VALUEHOLDER' ? " (OldLazy)" : (isEager ? " (E)" : "");
          const mappingLabel = rel.nestedTable ? "NestedTable" : rel.arrayMapping ? "Array" : rel.variableOneToOne ? "VarOneToOne" : rel.directCollection ? "DirectCol" : rel.aggregateCollection ? "AggCol" : rel.directMapMapping ? "DirectMap" : rel.mappingType;

          const edgeMarkers = getEdgeMarkers(rel);
          transformedEdges.push({
            id: `e-${n.name}-${rel.targetEntity}-${rIdx}`,
            source: n.name,
            target: rel.targetEntity,
            label: (isCutPointEdge ? '✂️ ' : '') + mappingLabel + (rel.owningSide ? ' 🔑' : '') + fetchLabel + (violationCount > 0 ? ` (${violationCount})` : ''),
            animated: hasProblems && !isCutPointEdge,
            type: 'smoothstep',
            style: { stroke: strokeColor, strokeWidth: isCutPointEdge ? 3.5 : (rel.owningSide ? 2.5 : 1.5), strokeDasharray: strokeDasharray },
            labelStyle: { fill: strokeColor, fontWeight: '600', fontSize: '10px', background: 'white', padding: '1px 4px', borderRadius: '3px', border: `1px solid ${strokeColor}` },
            ...edgeMarkers,
            data: { ...rel, violationCount, hasProblems, isEager, isCutPointEdge }
          });
        }
      });
      if (n.parentEntity && nodesList.some(p => p.name === n.parentEntity)) {
        transformedEdges.push({
          id: `inherit-${n.name}-${n.parentEntity}`,
          source: n.name,
          target: n.parentEntity,
          label: n.inheritanceStrategy ? `inherits (${n.inheritanceStrategy})` : 'inherits',
          type: 'smoothstep',
          style: { stroke: '#8b5cf6', strokeWidth: 2, strokeDasharray: '5 3' },
          markerEnd: 'url(#marker-inheritance)',
          data: { isInheritance: true, inheritanceStrategy: n.inheritanceStrategy }
        });
      }
    });

    const { nodes: layoutedNodes, edges: layoutedEdges } = getLayoutedElements(transformedNodes, transformedEdges);
    setNodes(layoutedNodes);
    setEdges(layoutedEdges);

    // Run Advanced DDD Analysis
    const analysisResult = runAnalysis(layoutedNodes, layoutedEdges, analysisConfig);
    setDddReport(analysisResult);

    setStats({
      nodes: nodesList.length,
      anomalies: anomalies.length,
      violations: violations.length,
      eager: nodesList.reduce((acc, n) => acc + (n.relationships?.filter(r => !r.lazy).length || 0), 0),
      errorCount: violations.filter(v => v.severity === 'ERROR').length,
      warningCount: violations.filter(v => v.severity === 'WARNING').length,
      infoCount: violations.filter(v => v.severity === 'INFO').length
    });
    setTimeout(() => fitView(), 100);
  }, [setNodes, setEdges, fitView, analysisConfig]);

  useEffect(() => {
    if (selectedReport === 'uploaded' || !selectedReport) { setLoading(false); return; }
    setError(null);
    fetch(`/${selectedReport}`)
      .then(res => res.json())
      .then(data => { processReportData(data); setLoading(false); setTimeout(() => fitView(), 100); })
      .catch(err => { console.error(err); setError(`Failed to load report: ${err.message}`); setLoading(false); });
  }, [selectedReport, processReportData, fitView]);

  const handleFileUpload = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const data = JSON.parse(e.target?.result as string);
        if (!data || !data.nodes) throw new Error("Invalid report format");
        setError(null);
        setLoading(true);
        requestAnimationFrame(() => {
          try { processReportData(data); setLoading(false); setSelectedReport('uploaded'); }
          catch (processErr) { console.error(processErr); setError(`Error processing report: ${(processErr as Error).message}`); setLoading(false); }
        });
      } catch (err) { console.error(err); setError(`Invalid JSON file: ${(err as Error).message}`); setLoading(false); }
    };
    reader.readAsText(file);
  }, [processReportData]);

  const onLayout = useCallback(async (direction: 'TB' | 'LR' | 'ORGANIC' | 'GRID' | 'RADIAL' | 'CLUSTER') => {
    const currentNodes = getNodes().filter(n => n.type === 'entityNode');
    const currentEdges = getEdges();
    const nodeIds = new Set(currentNodes.map(n => n.id));
    const validEdges = currentEdges.filter(e => nodeIds.has(e.source) && nodeIds.has(e.target));

    let result;
    if (direction === 'ORGANIC') result = await getElkLayout(currentNodes, validEdges);
    else if (direction === 'RADIAL') result = getRadialLayout(currentNodes, validEdges);
    else if (direction === 'GRID') result = getGridLayout(currentNodes, validEdges);
    else if (direction === 'CLUSTER') result = getClusterLayout(currentNodes, validEdges, (n) => getAggregateForNode(n, currentNodes, validEdges, heuristics['shared']));
    else result = getLayoutedElements(currentNodes, validEdges, direction);

    setNodes(result.nodes);
    setTimeout(() => fitView({ padding: 0.2 }), 100);
  }, [getNodes, getEdges, setNodes, fitView]);

  const toggleAttributes = useCallback(() => {
    setShowAttributes(prev => {
      const next = !prev;
      setNodes(nds => nds.map(node => ({ ...node, data: { ...node.data, showAttributes: next } })));
      return next;
    });
    setTimeout(() => onLayout('TB'), 100);
  }, [setNodes, onLayout]);

  const generatePDFReport = useCallback(() => {
    const doc = new jsPDF();
    const pageWidth = doc.internal.pageSize.getWidth();
    let y = 20;
    const addText = (text: string, x: number, fontSize = 10, color: [number, number, number] = [0, 0, 0]) => {
      doc.setFontSize(fontSize); doc.setTextColor(...color);
      const lines = doc.splitTextToSize(text, pageWidth - 40);
      doc.text(lines, x, y); y += lines.length * 7;
    };
    const addSection = (title: string) => {
      if (y > 250) { doc.addPage(); y = 20; }
      y += 5; doc.setFillColor(112, 0, 255); doc.rect(20, y - 5, pageWidth - 40, 8, 'F');
      doc.setTextColor(255, 255, 255); doc.setFontSize(12); doc.text(title, 23, y);
      doc.setTextColor(0, 0, 0); y += 12;
    };

    doc.setFontSize(24); doc.setTextColor(112, 0, 255); doc.text('EclipseLink Analyzer Report', 20, y);
    y += 15; addText(`Generated: ${new Date().toLocaleString()}`, 20, 10, [100, 100, 100]);
    addSection('📊 Global Statistics');
    addText(`• Total Entities: ${stats.nodes}`, 20);
    addText(`• Total Violations: ${stats.violations} (Errors: ${stats.errorCount}, Warnings: ${stats.warningCount})`, 20);
    doc.save(`eclipselink-report-${new Date().toISOString().slice(0, 10)}.pdf`);
  }, [stats]);

  const onConnect = useCallback((params: Connection | Edge) => setEdges((eds) => addEdge(params, eds)), [setEdges]);
  const onNodeClick = (_: unknown, node: Node) => {
    setSelectedNodeId(node.id);
    setSelectedEdgeId(null);
    setIsEntityOpen(true);
  };
  const onEdgeClick = useCallback((_: unknown, edge: Edge) => { setSelectedEdgeId(edge.id); setSelectedNodeId(null); }, []);

  const selectedNode = nodes.find(n => n.id === selectedNodeId);
  const selectedEdge = edges.find(e => e.id === selectedEdgeId);

  if (loading) {
    return (
      <div className="h-screen w-screen flex flex-col items-center justify-center bg-body text-main">
        <Loader2 className="animate-spin text-primary mb-4" size={48} />
        <p className="font-medium">Loading EclipseLink Analysis...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="h-screen w-screen flex flex-col items-center justify-center bg-body text-main">
        <AlertCircle className="text-score-low mb-4" size={48} />
        <p className="font-medium mb-2">Failed to load model</p>
        <p className="text-muted text-sm mb-4">{error}</p>
        <button onClick={() => setError(null)} className="px-4 py-2 bg-panel border border-subtle rounded hover:bg-input transition-colors">Dismiss</button>
      </div>
    );
  }

  return (
    <div className={`flex h-screen bg-body text-main font-sans selection:bg-primary/30 overflow-hidden ${theme === 'dark' ? 'dark-theme' : ''}`} style={{ backgroundColor: 'var(--bg-body)', color: 'var(--text-main)' }}>
      {/* SIDEBAR */}
      <aside className="w-[280px] bg-panel border-r border-subtle flex flex-col z-20 tool-sidebar" style={{ backgroundColor: 'var(--bg-panel)', borderColor: 'var(--border-subtle)' }}>
        <div className="h-[48px] px-4 border-b border-subtle flex items-center gap-2" style={{ borderColor: 'var(--border-subtle)' }}>
          <GitGraph size={16} className="text-primary" />
          <h1 className="text-[11px] font-black uppercase tracking-[0.2em] text-main">Eclipselink Analyzer</h1>
        </div>

        <div className="p-4 border-b border-subtle">
          <div className="space-y-3">
            <div>
              <label className="text-[9px] uppercase tracking-widest text-muted font-black block mb-1.5">Project / Source</label>
              <div className="relative group">
                <select
                  value={selectedReport}
                  onChange={(e) => setSelectedReport(e.target.value)}
                  className="w-full bg-input border border-subtle rounded-[2px] px-3 py-1.5 text-[11px] appearance-none focus:outline-none focus:border-primary transition-all cursor-pointer"
                  style={{ backgroundColor: 'var(--bg-input)', borderColor: 'var(--border-subtle)', color: 'var(--text-main)' }}
                >
                  {reports.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
                  <option value="uploaded">Uploaded File</option>
                </select>
                <ChevronDown size={12} className="absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none text-muted opacity-50" />
              </div>
            </div>

            <button
              onClick={() => document.getElementById('report-upload')?.click()}
              className="w-full py-1.5 px-3 bg-panel hover:bg-input border border-subtle rounded-[2px] text-[10px] font-bold flex items-center justify-center gap-2 transition-all text-secondary"
            >
              <Upload size={12} /> Import Metamodel
            </button>
            <input id="report-upload" type="file" accept=".json" onChange={handleFileUpload} className="hidden" />
          </div>
        </div>

        <nav className="flex-1 px-3 space-y-6 overflow-y-auto custom-scrollbar py-4">
          <div>
            <h3 className="px-2 mb-2 text-[10px] uppercase font-black tracking-widest text-muted flex items-center gap-2">
              <Database size={11} /> {t('stats')}
            </h3>
            <div className="mx-2 mb-3 p-2 bg-input border border-subtle rounded-[2px] grid grid-cols-3 text-center">
              <div><div className="text-[10px] font-bold text-score-low">{stats.errorCount}</div><div className="text-[8px] text-muted">ERR</div></div>
              <div className="border-x border-subtle"><div className="text-[10px] font-bold text-score-med">{stats.warningCount}</div><div className="text-[8px] text-muted">WARN</div></div>
              <div><div className="text-[10px] font-bold text-primary">{stats.infoCount}</div><div className="text-[8px] text-muted">INFO</div></div>
            </div>
          </div>


          <div>
            {/* 1. SYSTEM SECTION */}
            <div className="border-b border-subtle">
              <button
                onClick={() => setIsSystemOpen(!isSystemOpen)}
                className="w-full flex items-center justify-between px-3 py-2 bg-black/5 hover:bg-black/10 transition-colors"
              >
                <div className="flex items-center gap-2">
                  <Database size={11} className="text-muted" />
                  <span className="text-[10px] uppercase font-black tracking-widest text-muted">{t('tabSystem')}</span>
                </div>
                <ChevronDown size={10} className={`text-muted transition-transform duration-200 ${isSystemOpen ? '' : '-rotate-90'}`} />
              </button>

              {isSystemOpen && (
                <div className="px-3 py-3 space-y-4 bg-panel/50">
                  {/* Stats Grid */}
                  <div className="p-2 bg-input border border-subtle rounded-[2px] grid grid-cols-3 text-center">
                    <div><div className="text-[10px] font-bold text-score-low">{stats.errorCount}</div><div className="text-[8px] text-muted">ERR</div></div>
                    <div className="border-x border-subtle"><div className="text-[10px] font-bold text-score-med">{stats.warningCount}</div><div className="text-[8px] text-muted">WARN</div></div>
                    <div><div className="text-[10px] font-bold text-primary">{stats.infoCount}</div><div className="text-[8px] text-muted">INFO</div></div>
                  </div>

                  {/* Algorithm Tuning Section */}
                  <div className="border-t border-subtle/50 pt-3 mt-3">
                    <button
                      onClick={() => setIsTuningOpen(!isTuningOpen)}
                      className="w-full flex items-center justify-between mb-3 text-[9px] uppercase font-black tracking-widest text-primary hover:text-main transition-colors"
                    >
                      <div className="flex items-center gap-2">
                        <Layers size={10} /> {t('algorithmTuning')}
                      </div>
                      <ChevronDown size={10} className={`transition-transform duration-200 ${isTuningOpen ? '' : '-rotate-90'}`} />
                    </button>

                    {isTuningOpen && (
                      <div className="space-y-5 px-1 animate-in slide-in-from-top-2 duration-200">
                        {/* VO Threshold */}
                        <div className="space-y-1.5">
                          <div className="flex justify-between items-center" title={t('voConfidenceTooltip')}>
                            <span className="text-[9px] font-bold text-main">{t('voConfidenceThreshold')}</span>
                            <span className="text-[9px] font-mono text-primary bg-primary/10 px-1.5 py-0.5 rounded">{analysisConfig.voConfidenceThreshold.toFixed(2)}</span>
                          </div>
                          <input
                            type="range" min="0" max="1" step="0.05"
                            value={analysisConfig.voConfidenceThreshold}
                            onChange={(e) => setAnalysisConfig(prev => ({ ...prev, voConfidenceThreshold: parseFloat(e.target.value) }))}
                            className="w-full accent-primary h-1 bg-subtle rounded-lg appearance-none cursor-pointer"
                          />
                        </div>

                        {/* Weak Link Threshold */}
                        <div className="space-y-1.5">
                          <div className="flex justify-between items-center" title={t('weakLinkTooltip')}>
                            <div className="flex items-center gap-1.5">
                              <span className="text-[9px] font-bold text-main">{t('weakLinkSensitivity')}</span>
                              <button
                                onClick={() => setExpandedAlgo(expandedAlgo === 'weak' ? null : 'weak')}
                                className="text-primary hover:text-white transition-colors"
                              >
                                <Info size={9} />
                              </button>
                            </div>
                            <span className="text-[9px] font-mono text-primary bg-primary/10 px-1.5 py-0.5 rounded">{analysisConfig.weakLinkThreshold.toFixed(2)}</span>
                          </div>

                          {expandedAlgo === 'weak' && (
                            <div className="p-2 mb-2 bg-blue-500/10 border-l-2 border-blue-500 rounded-r text-[9px] text-muted leading-relaxed">
                              <h4 className="font-bold text-blue-400 mb-1 uppercase tracking-wider">Heuristic: Point Cuts</h4>
                              {t('weakLinkAlgoDetail')}
                            </div>
                          )}

                          <input
                            type="range" min="0" max="1" step="0.05"
                            value={analysisConfig.weakLinkThreshold}
                            onChange={(e) => setAnalysisConfig(prev => ({ ...prev, weakLinkThreshold: parseFloat(e.target.value) }))}
                            className="w-full accent-primary h-1 bg-subtle rounded-lg appearance-none cursor-pointer"
                          />
                        </div>

                        {/* Stability Thresholds */}
                        <div className="space-y-3 pt-2 border-t border-subtle/30">
                          <div className="flex items-center justify-between">
                            <div className="text-[8px] font-black text-muted uppercase tracking-tight flex items-center gap-2">
                              {t('stabilityBoundaries')}
                              <button
                                onClick={() => setExpandedAlgo(expandedAlgo === 'stability' ? null : 'stability')}
                                className="text-muted hover:text-white transition-colors"
                              >
                                <Info size={9} />
                              </button>
                            </div>
                          </div>

                          {expandedAlgo === 'stability' && (
                            <div className="p-2 mb-2 bg-purple-500/10 border-l-2 border-purple-500 rounded-r text-[9px] text-muted leading-relaxed">
                              <h4 className="font-bold text-purple-400 mb-1 uppercase tracking-wider">Metric: Robert C. Martin's Instability</h4>
                              {t('stabilityAlgoDetail')}
                            </div>
                          )}

                          <div className="space-y-1.5">
                            <div className="flex justify-between items-center" title={t('stableZoneTooltip')}>
                              <span className="text-[9px] font-bold text-main italic">{t('stableZone')}</span>
                              <span className="text-[9px] font-mono text-score-high bg-score-high/10 px-1.5 py-0.5 rounded">{analysisConfig.instabilityStableThreshold.toFixed(2)}</span>
                            </div>
                            <input
                              type="range" min="0" max="0.5" step="0.05"
                              value={analysisConfig.instabilityStableThreshold}
                              onChange={(e) => setAnalysisConfig(prev => ({ ...prev, instabilityStableThreshold: parseFloat(e.target.value) }))}
                              className="w-full accent-score-high h-1 bg-subtle rounded-lg appearance-none cursor-pointer"
                            />
                          </div>
                          <div className="space-y-1.5">
                            <div className="flex justify-between items-center" title={t('unstableZoneTooltip')}>
                              <span className="text-[9px] font-bold text-main italic">{t('unstableZone')}</span>
                              <span className="text-[9px] font-mono text-score-low bg-score-low/10 px-1.5 py-0.5 rounded">{analysisConfig.instabilityUnstableThreshold.toFixed(2)}</span>
                            </div>
                            <input
                              type="range" min="0.5" max="1" step="0.05"
                              value={analysisConfig.instabilityUnstableThreshold}
                              onChange={(e) => setAnalysisConfig(prev => ({ ...prev, instabilityUnstableThreshold: parseFloat(e.target.value) }))}
                              className="w-full accent-score-low h-1 bg-subtle rounded-lg appearance-none cursor-pointer"
                            />
                          </div>
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Aggregate Explorer */}
                  <div>
                    <h3 className="mb-2 text-[9px] uppercase font-black tracking-widest text-muted flex items-center gap-2 opacity-70">
                      <Cpu size={10} /> {t('domainArchitecture')}
                    </h3>
                    <div className="relative group">
                      <select
                        value={focusedAggregate || ''}
                        onChange={(e) => centerOnAggregate(e.target.value || null)}
                        className="w-full bg-input border border-subtle rounded-[2px] px-2.5 py-1.5 text-[10px] appearance-none focus:outline-none focus:border-primary transition-all cursor-pointer font-bold pr-8"
                        style={{ backgroundColor: 'var(--bg-input)', borderColor: 'var(--border-subtle)', color: 'var(--text-main)' }}
                      >
                        <option value="">{t('allAggregates')}</option>
                        {dddReport?.aggregates.map((agg: any) => (
                          <option key={agg.root} value={agg.root}>{agg.root}</option>
                        ))}
                      </select>
                      <ChevronDown size={10} className="absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none text-muted opacity-50" />
                    </div>
                  </div>

                  {/* Quick Health Summary */}
                  {!dddReport && <div className="py-2 text-center text-[9px] text-muted italic border border-dashed border-subtle rounded">{t('systemHealthy')}</div>}
                  {dddReport && dddReport.cuts.length > 0 && (
                    <div className="p-2 bg-accent-rose/5 border border-accent-rose/20 rounded-[2px] flex items-center justify-between">
                      <span className="text-[9px] font-black text-accent-rose uppercase tracking-widest flex items-center gap-1.5">
                        <Scissors size={9} /> {t('anomalies')}
                      </span>
                      <span className="px-1.5 py-0.5 rounded-[1px] bg-accent-rose text-white text-[9px] font-bold">{dddReport.cuts.length}</span>
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* 2. ENTITY SECTION */}
            <div className="border-b border-subtle">
              <button
                onClick={() => setIsEntityOpen(!isEntityOpen)}
                className={`w-full flex items-center justify-between px-3 py-2 transition-colors ${selectedNode ? 'bg-primary/5 hover:bg-primary/10' : 'bg-transparent hover:bg-black/5'}`}
              >
                <div className="flex items-center gap-2">
                  <Box size={11} className={selectedNode ? 'text-primary' : 'text-muted'} />
                  <span className={`text-[10px] uppercase font-black tracking-widest ${selectedNode ? 'text-primary' : 'text-muted opacity-70'}`}>{t('tabEntity')}</span>
                </div>
                <ChevronDown size={10} className={`transition-transform duration-200 ${isEntityOpen ? '' : '-rotate-90'} ${selectedNode ? 'text-primary' : 'text-muted'}`} />
              </button>

              {isEntityOpen && (
                <div className="px-3 py-3">
                  {selectedNode ? (
                    <div className="space-y-3">
                      <div className="bg-input border border-subtle rounded-[2px] p-3 space-y-2.5">
                        <div className="flex justify-between items-center">
                          <span className="text-[9px] text-muted font-bold uppercase tracking-tight">Role</span>
                          <span className={`px-1.5 py-0.5 rounded-[1px] text-[9px] font-black ${selectedNode.data.dddRole === 'AGGREGATE_ROOT' ? 'bg-primary/20 text-primary border border-primary/30' : 'bg-subtle text-muted'}`}>
                            {selectedNode.data.dddRole || 'ENTITY'}
                          </span>
                        </div>
                        <div className="flex justify-between items-center border-t border-subtle/30 pt-2">
                          <span className="text-[9px] text-muted font-bold uppercase tracking-tight">Cluster</span>
                          <span className="text-[10px] font-mono text-main truncate max-w-[120px]">{selectedNode.data.aggregateName || 'General'}</span>
                        </div>
                      </div>
                      <div className="p-2.5 bg-primary/5 border border-primary/10 rounded-[2px]">
                        <div className="text-[8px] font-black text-primary uppercase mb-1.5 opacity-70">Architecture Guidance</div>
                        <p className="text-[10px] text-secondary leading-tight">
                          {selectedNode.data.dddRole === 'AGGREGATE_ROOT'
                            ? `Transactional consistency boundary. Reference by ID only from other aggregates.`
                            : `Child entity. Must be accessed through its Aggregate Root to maintain invariants.`}
                        </p>
                      </div>
                    </div>
                  ) : (
                    <div className="py-6 text-center text-[9px] text-muted italic opacity-50 border border-dashed border-subtle rounded-[2px]">
                      Select an entity to view details
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </nav>

        <div className="p-4 mt-auto border-t border-subtle">
          <button
            onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}
            className="w-full flex items-center justify-between px-3 py-1.5 rounded-[2px] text-[11px] font-bold text-secondary hover:bg-input transition-all"
          >
            <div className="flex items-center gap-2">
              {theme === 'light' ? <Moon size={14} /> : <Sun size={14} />}
              <span>{theme === 'light' ? 'Dark' : 'Light'} UI</span>
            </div>
          </button>
        </div>
      </aside>

      {/* MAIN STAGE */}
      <main className="flex-1 relative overflow-hidden flex flex-col" style={{ backgroundColor: 'var(--bg-body)' }}>
        <div className="h-[48px] border-b border-subtle px-4 flex items-center justify-between z-10 bg-panel shadow-sm">
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2">
              <span className="text-[9px] uppercase font-black text-muted tracking-widest">Projection</span>
              <div className="flex bg-input rounded-[2px] p-0.5 border border-subtle">
                {(['graph', 'table', 'jpa', 'ddl'] as const).map(mode => (
                  <button key={mode} onClick={() => setViewMode(mode)} className={`px-3 py-1 text-[10px] font-bold rounded-[1px] transition-all capitalize ${viewMode === mode ? 'bg-primary text-white' : 'text-muted hover:text-main'}`}>
                    {mode}
                  </button>
                ))}
              </div>
            </div>
            <div className="w-px h-4 bg-subtle"></div>
            <div className="flex items-center gap-2">
              <span className="text-[9px] uppercase font-black text-muted tracking-widest">{t('layout')}</span>
              <select
                onChange={(e) => onLayout(e.target.value as any)}
                className="bg-input border border-subtle rounded-[2px] px-2 py-0.5 text-[10px] font-bold focus:outline-none cursor-pointer hover:border-primary transition-colors appearance-none pr-6 relative"
                style={{ backgroundColor: 'var(--bg-input)', color: 'var(--text-main)', borderColor: 'var(--border-subtle)' }}
              >
                <option value="TB" className="bg-panel">{t('layouts.TB' as any)}</option>
                <option value="LR" className="bg-panel">{t('layouts.LR' as any)}</option>
                <option value="ORGANIC" className="bg-panel">{t('layouts.ORGANIC' as any)}</option>
                <option value="RADIAL" className="bg-panel">{t('layouts.RADIAL' as any)}</option>
                <option value="GRID" className="bg-panel">{t('layouts.GRID' as any)}</option>
                <option value="CLUSTER" className="bg-panel">{t('layouts.CLUSTER' as any)}</option>
              </select>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="relative group">
              <Languages size={14} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-muted pointer-events-none" />
              <select
                value={language}
                onChange={(e) => setLanguage(e.target.value as Language)}
                className="bg-input border border-subtle rounded-[2px] pl-8 pr-6 py-1.5 text-[10px] font-bold focus:outline-none cursor-pointer hover:border-primary transition-colors appearance-none"
                style={{ backgroundColor: 'var(--bg-input)', color: 'var(--text-main)', borderColor: 'var(--border-subtle)' }}
              >
                <option value="en">🇬🇧 EN</option>
                <option value="fr">🇫🇷 FR</option>
                <option value="pl">🇵🇱 PL</option>
                <option value="ru">🇷🇺 RU</option>
              </select>
              <ChevronDown size={10} className="absolute right-2 top-1/2 -translate-y-1/2 pointer-events-none text-muted opacity-50" />
            </div>

            <button onClick={generatePDFReport} className="flex items-center gap-2 px-3 py-1.5 bg-input border border-subtle rounded-[2px] text-[10px] font-bold text-secondary hover:text-main transition-all">
              <FileDown size={14} /> {t('exportReport')}
            </button>
            <button onClick={toggleAttributes} className={`p-1.5 rounded-[2px] border transition-all ${showAttributes ? 'bg-primary/20 border-primary text-primary' : 'bg-input border-subtle text-muted'}`}>
              <Eye size={16} />
            </button>
          </div>
        </div>

        <div className="flex-1 relative flex flex-col overflow-hidden">
          {viewMode === 'graph' && (
            <ReactFlow nodes={nodes} edges={edges} onNodesChange={onNodesChange} onEdgesChange={onEdgesChange} onConnect={onConnect} onNodeClick={onNodeClick} onEdgeClick={onEdgeClick} nodeTypes={nodeTypes} fitView minZoom={0.1}>
              <svg style={{ position: 'absolute', width: 0, height: 0 }}>
                <defs>
                  <marker id="marker-inheritance" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="8" markerHeight="8" orient="auto"><path d="M 0 0 L 10 5 L 0 10 z" fill="white" stroke="currentColor" strokeWidth="1.5" /></marker>
                  <marker id="marker-composition" viewBox="0 0 10 10" refX="0" refY="5" markerWidth="8" markerHeight="8" orient="auto"><path d="M 0 5 L 10 0 L 20 5 L 10 10 z" fill="currentColor" /></marker>
                  <marker id="marker-crows-foot" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="12" markerHeight="12" orient="auto"><path d="M 0 0 L 10 5 L 0 10 M 10 5 L 0 5" fill="none" stroke="currentColor" strokeWidth="1.5" /></marker>
                  <marker id="marker-standard-arrow" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="8" markerHeight="8" orient="auto"><path d="M 0 0 L 10 5 L 0 10 z" fill="currentColor" /></marker>
                </defs>
              </svg>
              <Background color="var(--border-subtle)" variant={BackgroundVariant.Dots} gap={24} size={1} />
              <Controls />
              <MiniMap nodeStrokeWidth={3} zoomable pannable />
            </ReactFlow>
          )}

          {viewMode === 'table' && <TableView relationships={edges.map(e => ({ sourceEntity: e.source, targetEntity: e.target, attributeName: e.data?.attributeName || 'unknown', mappingType: e.data?.mappingType || 'unknown', lazy: e.data?.lazy !== false, cascadePersist: !!e.data?.cascadePersist, cascadeRemove: !!e.data?.cascadeRemove, cascadeAll: !!e.data?.cascadeAll, owningSide: !!e.data?.owningSide, mappedBy: e.data?.mappedBy, optional: !!e.data?.optional, batchFetchType: e.data?.batchFetchType }))} onRowClick={(row) => { const node = nodes.find(n => n.id === row.sourceEntity); if (node) setSelectedNodeId(node.id); setViewMode('graph'); }} />}
          {viewMode === 'jpa' && <JPAView entities={nodes.map(n => n.data)} onRowClick={(row) => { const node = nodes.find(n => n.id === row.sourceEntity); if (node) setSelectedNodeId(node.id); setViewMode('graph'); }} />}
          {viewMode === 'ddl' && <DDLView entities={nodes.map(n => n.data)} nativeDdl={nativeDdl} />}
        </div>
      </main>

      {/* INSPECTOR */}
      <aside
        style={{ width: sidebarCollapsed ? '48px' : `${sidebarWidth}px`, backgroundColor: 'var(--bg-panel)', borderColor: 'var(--border-subtle)' }}
        className="bg-panel border-l border-subtle flex flex-col z-20 transition-[width] duration-300 relative group inspector-sidebar"
      >
        {!sidebarCollapsed && <div onMouseDown={handleResizeStart} className="absolute left-0 top-0 bottom-0 w-1 cursor-col-resize hover:bg-primary/30 transition-colors z-30" />}
        <button onClick={() => setSidebarCollapsed(!sidebarCollapsed)} className="absolute -left-3 top-1/2 -translate-y-1/2 w-6 h-6 bg-panel border border-subtle rounded-full flex items-center justify-center text-secondary hover:text-main z-40">
          {sidebarCollapsed ? <ChevronLeft size={14} /> : <ChevronRight size={14} />}
        </button>

        {!sidebarCollapsed ? (
          <div className="flex-1 flex flex-col overflow-hidden">
            {selectedNode || selectedEdge ? (
              <>
                <div className="p-4 border-b border-subtle bg-black/10">
                  <div className="flex items-center justify-between mb-1">
                    <h2 className="text-[14px] font-black text-main truncate mr-2">{selectedNode ? selectedNode.id : selectedEdge?.data?.attributeName}</h2>
                    <div className="px-1.5 py-0.5 bg-primary/10 border border-primary/20 rounded-[2px] text-[9px] font-bold text-primary">{selectedNode ? selectedNode.data.type : 'MAP'}</div>
                  </div>
                  <div className="text-[9px] font-mono text-muted tracking-tight">{selectedNode && selectedNode.data.packageName}</div>
                </div>

                {selectedNode && (
                  <div className="px-4 py-2 flex bg-panel border-b border-subtle">
                    <button
                      onClick={() => setInspectorJpaTab('general')}
                      className={`flex-1 py-1 text-[10px] font-bold uppercase tracking-wider rounded transition-all ${inspectorJpaTab === 'general' ? 'bg-primary/10 text-primary' : 'text-muted hover:text-secondary'}`}
                    >
                      General
                    </button>
                    <button
                      onClick={() => setInspectorJpaTab('cache')}
                      className={`flex-1 py-1 text-[10px] font-bold uppercase tracking-wider rounded transition-all ${inspectorJpaTab === 'cache' ? 'bg-primary/10 text-primary' : 'text-muted hover:text-secondary'}`}
                    >
                      Cache
                    </button>
                  </div>
                )}

                <div className="flex border-b border-subtle bg-black/5">
                  {(['mapping', 'performance', 'ddl', 'tuning'] as const).map((tab) => (
                    <button key={tab} onClick={() => setActiveTab(tab)} className={`ide-tab ${activeTab === tab ? 'active' : ''}`}>{tab}</button>
                  ))}
                </div>

                <div className="flex-1 overflow-y-auto custom-scrollbar">
                  {activeTab === 'mapping' && (
                    <div className="p-4 space-y-4">
                      {selectedNode && (
                        <>
                          <div>
                            <div className="text-[9px] font-black text-primary uppercase tracking-widest mb-2">Technical Meta</div>
                            <div className="space-y-1.5">
                              <div className="flex justify-between items-center text-[11px] py-1 border-b border-subtle border-dashed"><span className="text-muted">Table</span><span className="font-mono text-main">{selectedNode.data.name}</span></div>
                              <div className="flex justify-between items-center text-[11px] py-1 border-b border-subtle border-dashed"><span className="text-muted">Entities</span><span className="font-mono text-main">{selectedNode.data.type}</span></div>
                            </div>
                          </div>

                          {selectedNode.data.relationships && selectedNode.data.relationships.length > 0 && (
                            <div>
                              <div className="text-[9px] font-black text-accent-purple uppercase tracking-widest mb-2">Structure</div>
                              <div className="space-y-2">
                                {selectedNode.data.relationships.map((rel: any, i: number) => (
                                  <div key={i} className="p-2 bg-input border border-subtle rounded-[2px]">
                                    <div className="flex justify-between text-[10px] mb-1"><span className="font-bold text-main">{rel.attributeName}</span><span className="text-primary">{rel.mappingType}</span></div>
                                    <div className="text-[9px] text-muted">target: {rel.targetEntity}</div>
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}
                        </>
                      )}
                      {selectedEdge && (
                        <div>
                          <div className="text-[9px] font-black text-primary uppercase tracking-widest mb-2">Relation Meta</div>
                          <div className="space-y-1.5">
                            <div className="flex justify-between items-center text-[11px] py-1 border-b border-subtle border-dashed"><span className="text-muted">Type</span><span className="font-mono text-main">{selectedEdge.data?.mappingType}</span></div>
                            <div className="flex justify-between items-center text-[11px] py-1 border-b border-subtle border-dashed"><span className="text-muted">Fetch</span><span className={`font-bold ${selectedEdge.data?.lazy === false ? 'text-score-low' : 'text-score-high'}`}>{selectedEdge.data?.lazy === false ? 'EAGER' : 'LAZY'}</span></div>
                          </div>
                        </div>
                      )}
                    </div>
                  )}


                  {activeTab === 'performance' && (
                    <div className="p-4 space-y-4">
                      {selectedNode && selectedNode.data.violations && selectedNode.data.violations.length > 0 ? (
                        <div>
                          <div className="text-[9px] font-black text-score-low uppercase tracking-widest mb-3">Performance Audit</div>
                          <div className="space-y-3">
                            {selectedNode.data.violations.map((v: any, i: number) => (
                              <div key={i} className="p-3 bg-score-low/5 border-l-2 border-score-low border border-subtle rounded-[2px]">
                                <div className="text-[11px] font-bold text-main mb-1">{v.ruleId}</div>
                                <p className="text-[10px] text-secondary leading-normal">{v.message}</p>
                              </div>
                            ))}
                          </div>
                        </div>
                      ) : (
                        <div className="py-10 text-center">
                          <CheckCircle2 size={16} className="mx-auto mb-2 text-score-high opacity-30" />
                          <div className="text-[11px] font-bold text-muted">No Performance Risks</div>
                        </div>
                      )}
                    </div>
                  )}

                  {activeTab === 'ddl' && (
                    <div className="p-4">
                      <div className="text-[9px] font-black text-muted uppercase tracking-widest mb-2">Native DDL Projection</div>
                      <div className="bg-input border border-subtle p-3 rounded-[2px] font-mono text-[10px] leading-relaxed break-all">
                        <span className="text-primary">CREATE TABLE</span> {selectedNode?.id?.toUpperCase()} (
                        <div className="pl-4">
                          ID <span className="text-accent-purple">BIGINT</span> PRIMARY KEY,<br />
                          {selectedNode?.data.attributes && Object.keys(selectedNode.data.attributes).slice(0, 3).map(k => (
                            <span key={k}>{k.toUpperCase()} <span className="text-accent-purple">VARCHAR</span>(255),<br /></span>
                          ))}
                          ...
                        </div>
                        );
                      </div>
                    </div>
                  )}
                </div>
              </>
            ) : (
              <div className="flex-1 flex flex-col overflow-hidden">
                <div className="p-4 border-b border-subtle bg-black/10">
                  <h2 className="text-[12px] font-black text-main uppercase tracking-widest">{t('globalInsights')}</h2>
                </div>
                <div className="flex border-b border-subtle bg-black/5">
                  {(['overview'] as const).map((tab) => (
                    <button
                      key={tab}
                      onClick={() => setActiveTab(tab as any)}
                      className={`ide-tab ${activeTab === tab || (tab === 'overview' && !['tuning', 'overview'].includes(activeTab)) ? 'active' : ''}`}
                    >
                      {t(tab as any)}
                    </button>
                  ))}
                </div>
                <div className="flex-1 overflow-y-auto custom-scrollbar">
                  {(activeTab === 'overview' || !['tuning'].includes(activeTab)) && (
                    <div className="p-4 space-y-6">
                      <div className="px-1 mb-2">
                        <div className="flex gap-2">
                          {(['ERROR', 'WARNING'] as const).map(sev => (
                            <button
                              key={sev}
                              onClick={() => setViolationFilters(prev => ({ ...prev, [sev]: !prev[sev] }))}
                              className={`flex-1 py-1 px-2 rounded text-[9px] border transition-all ${violationFilters[sev] ? 'bg-primary/10 border-primary text-primary' : 'bg-transparent border-subtle text-muted'}`}
                            >
                              {sev}
                            </button>
                          ))}
                        </div>
                      </div>
                      <div className="grid grid-cols-2 gap-3">
                        <div className="p-3 bg-input border border-subtle rounded-[2px] text-center">
                          <div className="text-[8px] text-muted font-black uppercase mb-1">{t('entities')}</div>
                          <div className="text-lg font-bold text-main">{stats.nodes}</div>
                        </div>
                        <div className="p-3 bg-input border border-subtle rounded-[2px] text-center">
                          <div className="text-[8px] text-muted font-black uppercase mb-1">{t('health')}</div>
                          <div className="text-lg font-bold text-score-high">{Math.max(100 - (stats.errorCount * 5 + stats.warningCount * 2), 0)}%</div>
                        </div>
                      </div>

                      <div>
                        <div className="text-[9px] font-black text-muted uppercase tracking-widest mb-3">{t('systemHealthRisks')}</div>
                        <div className="space-y-2">
                          {stats.errorCount > 0 ? (
                            <div className="p-3 bg-score-low/10 border border-score-low/30 rounded-[2px] flex gap-2">
                              <ShieldAlert size={14} className="text-score-low shrink-0 h-4" />
                              <div>
                                <div className="text-[11px] font-bold text-score-low mb-1">{t('actionRequired')}</div>
                                <p className="text-[10px] text-secondary leading-relaxed">{t('archRisksDetected')}</p>
                              </div>
                            </div>
                          ) : (
                            <div className="p-4 border border-dashed border-subtle rounded-[2px] text-center italic text-[10px] text-muted">{t('systemHealthy')}</div>
                          )}
                        </div>
                      </div>
                    </div>
                  )}

                  {activeTab === 'tuning' && null}
                </div>
              </div>
            )}
          </div>
        ) : (
          <div className="flex-1 flex flex-col items-center py-6 opacity-40">
            <span className="[writing-mode:vertical-lr] text-[9px] font-black uppercase tracking-widest text-muted">{t('inspectorHidden')}</span>
          </div>
        )}
      </aside>
    </div >
  );
}

export default function App() {
  return (
    <ReactFlowProvider>
      <AnalyzerApp />
    </ReactFlowProvider>
  );
}
