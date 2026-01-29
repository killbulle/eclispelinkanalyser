import { useState, useCallback, useEffect, useMemo } from 'react';
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
} from 'reactflow';
import type { Node, Edge, Connection } from 'reactflow';

import 'reactflow/dist/style.css';
import dagre from 'dagre';
import * as d3Force from 'd3-force';
import ELK from 'elkjs/lib/elk.bundled.js';
import EntityNode from './components/EntityNode';
import GroupNode from './components/GroupNode';
import { jsPDF } from 'jspdf';
import { getAggregateForNode, heuristics, type HeuristicType } from './utils/aggregateHeuristics';
import { runAnalysis, type AnalysisConfig, type AnalysisReport as AdvancedAnalysisReport } from './analysis/engine';
import { SemanticProfile } from './analysis/semantic';
import { Layout, AlertCircle, CircleAlert, Info, Loader2, RefreshCw, Eye, EyeOff, GitGraph, Upload, ShieldAlert, Database, ChevronLeft, ChevronRight, Activity, Layers, FileDown, Brain, Box, GitMerge, ChevronDown, LayoutDashboard } from 'lucide-react';

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
  aggregateName?: string;
  showAttributes: boolean;
  hasAnomalies: boolean;
  violations: Violation[];
}

interface AnalysisReport {
  nodes: EntityNodeData[];
  anomalies: Anomaly[];
  violations: Violation[];
}

const nodeTypes = {
  entityNode: EntityNode,
  groupNode: GroupNode,
};

const AGGREGATE_COLORS = [
  '#00F0FF', // Cyan
  '#7000FF', // Purple
  '#FF00C8', // Magenta
  '#00FF8E', // Spring Green
  '#FF8A00', // Orange
  '#FFE600', // Yellow
  '#0085FF', // Blue
  '#FF0040', // Red-Pink
];

const getAggregateColor = (name?: string) => {
  if (!name || name === 'Default') return '#444';
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  const index = Math.abs(hash) % AGGREGATE_COLORS.length;
  return AGGREGATE_COLORS[index];
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
  const [selectedEdgeId, setSelectedEdgeId] = useState<string | null>(null);
  const [stats, setStats] = useState({ nodes: 0, anomalies: 0, violations: 0, eager: 0, errorCount: 0, warningCount: 0, infoCount: 0 });
  const [showAttributes, setShowAttributes] = useState(false);
  const [selectedReport, setSelectedReport] = useState('demo-scenarios.json');
  const [activeTab, setActiveTab] = useState<'mapping' | 'anomalies' | 'violations'>('mapping');
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [sidebarWidth, setSidebarWidth] = useState(480);
  const [isResizing, setIsResizing] = useState(false);
  const [theme, setTheme] = useState<'light' | 'dark'>('light');
  const [violationFilters, setViolationFilters] = useState<Record<string, boolean>>({ ERROR: true, WARNING: true, INFO: true });
  const [groupingMode, setGroupingMode] = useState(false);
  const [activeLayer, setActiveLayer] = useState<'aggregates' | 'cycles' | 'cuts' | 'perf' | 'vo' | 'anomalies'>('aggregates');
  const [selectedHeuristic, setSelectedHeuristic] = useState<HeuristicType>('shared');

  // Advanced Analysis State
  const [analysisConfig, setAnalysisConfig] = useState<AnalysisConfig>({
    semanticProfile: SemanticProfile.GENERIC,
    enableSemantic: true,
    enableTopology: true
  });
  const [advancedReport, setAdvancedReport] = useState<AdvancedAnalysisReport | null>(null);

  const triggerAdvancedAnalysis = useCallback(() => {
    if (nodes.length === 0) return;
    const report = runAnalysis(nodes, edges, analysisConfig);
    setAdvancedReport(report);
  }, [nodes, edges, analysisConfig]);

  // Auto-run analysis when config or data changes (debounced)
  useEffect(() => {
    const timer = setTimeout(triggerAdvancedAnalysis, 1000);
    return () => clearTimeout(timer);
  }, [triggerAdvancedAnalysis]);

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
    // === Level 1: Basic JPA ===
    { id: 'catalog/1-1-basic-entity.json', name: '1.1 📗 Basic Entity' },
    { id: 'catalog/1-2-basic-relationship.json', name: '1.2 📗 Basic Relationship' },
    { id: 'catalog/1-3-bidirectional.json', name: '1.3 📗 Bidirectional' },
    { id: 'catalog/1-4-many-to-many.json', name: '1.4 📗 Many-to-Many' },
    // === Level 2: JPA Intermediate ===
    { id: 'catalog/2-1-inheritance-single.json', name: '2.1 📘 Inheritance (Single)' },
    { id: 'catalog/2-2-inheritance-joined.json', name: '2.2 📘 Inheritance (Joined)' },
    { id: 'catalog/2-3-embedded.json', name: '2.3 📘 Embedded' },
    { id: 'catalog/2-4-element-collection.json', name: '2.4 📘 ElementCollection' },
    { id: 'catalog/2-5-mapped-superclass.json', name: '2.5 🏗️ MappedSuperclass' },
    // === Level 3: JPA Advanced ===
    { id: 'catalog/3-1-lazy-eager.json', name: '3.1 📙 Lazy vs Eager' },
    { id: 'catalog/3-2-cascade-operations.json', name: '3.2 📙 Cascade Operations' },
    { id: 'catalog/3-3-version-temporal.json', name: '3.3 📙 Version & Temporal' },
    { id: 'catalog/3-4-converters.json', name: '3.4 📙 Converters' },
    // === Level 4: EclipseLink Specific ===
    { id: 'catalog/4-1-batch-fetch.json', name: '4.1 🔷 Batch Fetch' },
    { id: 'catalog/4-2-cache-config.json', name: '4.2 🔷 Cache Config' },
    { id: 'catalog/4-3-indirection.json', name: '4.3 🔷 Indirection (ValueHolder)' },
    { id: 'catalog/4-4-private-owned.json', name: '4.4 🔷 Private Owned' },
    // === Level 5: EclipseLink Advanced ===
    { id: 'catalog/5-1-transformation.json', name: '5.1 🔶 Transformation Mapping' },
    { id: 'catalog/5-2-variable-onetoone.json', name: '5.2 🔶 Variable OneToOne' },
    { id: 'catalog/5-3-direct-collection.json', name: '5.3 🔶 DirectCollection & Map' },
    { id: 'catalog/5-4-aggregate-collection.json', name: '5.4 🔶 AggregateCollection' },
    { id: 'catalog/5-5-array-nested.json', name: '5.5 🔶 Array & NestedTable' },
    // === Level 6: Anti-Patterns ===
    { id: 'catalog/6-1-circular-refs.json', name: '6.1 ⚠️ Circular References' },
    { id: 'catalog/6-2-cartesian-product.json', name: '6.2 ⚠️ Cartesian Product' },
    { id: 'catalog/6-3-missing-optimizations.json', name: '6.3 ⚠️ Missing Optimizations' },
    { id: 'catalog/6-4-deep-cycle-verified.json', name: '6.4 ✅ Verified Deep Cycle (A-B-C-D)' },
    // === Level 7: Real-World ===
    { id: 'complex-scenario-report.json', name: '7.1 🏢 Complex Domain (E-commerce)' },
    { id: 'ofbiz-report.json', name: '7.2 🚀 OFBiz Stress Test (113 entities)' },
  ];
  const { fitView } = useReactFlow();

  const processReportData = useCallback((data: AnalysisReport) => {
    // Helper to count violations for a relationship
    const countViolationsForRelationship = (sourceEntity: string, rel: RelationshipMetadata): number => {
      let count = 0;
      // Check for violations in the global list that mention this relationship
      const violations = data.violations || [];

      for (const violation of violations) {
        const msg = violation.message.toLowerCase();
        if (msg.includes(sourceEntity.toLowerCase()) && msg.includes(rel.attributeName.toLowerCase())) {
          count++;
        }
      }
      return count;
    };

    const nodesList = data.nodes || [];
    const anomalies = data.anomalies || [];
    const violations = data.violations || [];

    // Pre-compute analysis data for badges
    // Detect nodes with EAGER relationships (performance risk)
    const nodesWithEagerRisk = new Set<string>();
    nodesList.forEach((n: EntityNodeData) => {
      if (n.relationships?.some((r: RelationshipMetadata) => !r.lazy)) {
        nodesWithEagerRisk.add(n.name);
      }
    });

    // Detect potential Value Objects (Embeddables or entities with no ID/relationships)
    const potentialVOs = new Set<string>();
    nodesList.forEach((n: EntityNodeData) => {
      const rels = Array.isArray(n.relationships) ? n.relationships : [];
      if (n.type === 'EMBEDDABLE' ||
        (rels.length === 0 && Object.keys(n.attributes || {}).length <= 3)) {
        potentialVOs.add(n.name);
      }
    });

    // Detect cut-points (entities that connect different aggregates)
    const cutPoints = new Set<string>();
    nodesList.forEach((n: EntityNodeData) => {
      if (n.relationships?.some((r: RelationshipMetadata) => {
        const target = nodesList.find((t: EntityNodeData) => t.name === r.targetEntity);
        return target && target.aggregateName !== n.aggregateName && n.aggregateName && target.aggregateName;
      })) {
        cutPoints.add(n.name);
      }
    });

    // Detect deep cycles (length > 2)
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
        if (!visitedForCycle.has(neighbor)) {
          findCycles(neighbor);
        } else if (recursionStack.has(neighbor)) {
          const startIndex = currentPath.indexOf(neighbor);
          if (startIndex !== -1) {
            const cycle = currentPath.slice(startIndex);
            if (cycle.length > 2) {
              cycle.forEach(name => nodesInCycle.add(name));
            }
          }
        }
      }

      currentPath.pop();
      recursionStack.delete(nodeName);
    };

    nodesList.forEach(n => {
      if (!visitedForCycle.has(n.name)) findCycles(n.name);
    });

    // Detect Aggregate Roots - the node with most outgoing owned relationships in each aggregate
    const aggregateRoots = new Set<string>();
    const aggregateGroups = new Map<string, EntityNodeData[]>();

    // Group nodes by aggregate
    nodesList.forEach((n: EntityNodeData) => {
      const agg = n.aggregateName || n.packageName?.split('.').pop() || 'General';
      if (!aggregateGroups.has(agg)) aggregateGroups.set(agg, []);
      aggregateGroups.get(agg)!.push(n);
    });

    // For each aggregate, find the root (most outgoing owned relationships or explicit dddRole)
    aggregateGroups.forEach((members, aggName) => {
      // Check if any member already has dddRole set
      const existingRoot = members.find(m => m.dddRole === 'AGGREGATE_ROOT');
      if (existingRoot) {
        aggregateRoots.add(existingRoot.name);
        return;
      }

      // If only one member, it's the root
      if (members.length === 1) {
        aggregateRoots.add(members[0].name);
        return;
      }

      // Otherwise, calculate: node with most outgoing owned relationships
      let bestRoot: EntityNodeData = members[0]; // Default to first member
      let bestScore = -Infinity;

      members.forEach(m => {
        // Skip potential VOs - they shouldn't be roots
        if (m.type === 'EMBEDDABLE') return;

        const ownedRelCount = (m.relationships || []).filter((r: RelationshipMetadata) => r.owningSide).length;
        const totalRelCount = m.relationships?.length || 0;
        const incomingCount = nodesList.filter((other: EntityNodeData) =>
          other.relationships?.some((r: RelationshipMetadata) => r.targetEntity === m.name)
        ).length || 0;

        // Score: owned relations + total relations - incoming (roots have more outgoing, less incoming)
        const score = ownedRelCount * 3 + totalRelCount - incomingCount * 2;

        if (score > bestScore) {
          bestScore = score;
          bestRoot = m;
        }
      });

      aggregateRoots.add(bestRoot.name);
      console.log(`[DDD] Aggregate "${aggName}" root: ${bestRoot.name} (score: ${bestScore})`);
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
        // Analysis badges
        isCutPoint: cutPoints.has(n.name),
        isInCycle: nodesInCycle.has(n.name),
        hasEagerRisk: nodesWithEagerRisk.has(n.name),
        isPotentialVO: potentialVOs.has(n.name),
        // Auto-detect aggregate root if not already set
        dddRole: n.dddRole || (aggregateRoots.has(n.name) ? 'AGGREGATE_ROOT' : undefined),
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

          // Detect cut-point edges (cross aggregate boundaries)
          const isCutPointEdge = n.aggregateName && targetNode.aggregateName &&
            n.aggregateName !== targetNode.aggregateName;

          // Determine base style based on mapping type
          let baseStrokeColor = '#10b981'; // green for normal relations
          let baseDashArray = rel.owningSide ? '' : '5 5';

          // Phase 2 Mapping Styles
          if (rel.nestedTable) {
            baseStrokeColor = '#f97316'; // orange for Oracle NestedTable
            baseDashArray = '3 3';
          } else if (rel.arrayMapping) {
            baseStrokeColor = '#64748b'; // slate for SQL Array
            baseDashArray = '1 1';
          } else if (rel.variableOneToOne) {
            baseStrokeColor = '#eab308'; // yellow for VariableOneToOne
            baseDashArray = '10 2';
          } else if (rel.mappingType === 'Embedded') {
            baseStrokeColor = '#a78bfa'; // purple for embedded
            baseDashArray = '8 4';
          } else if (rel.mappingType === 'ElementCollection' || rel.directCollection || rel.aggregateCollection || rel.directMapMapping) {
            baseStrokeColor = '#ec4899'; // pink for element collection
            baseDashArray = '4 2';
          }

          // Special styling for cut-point edges (cyan dashed - more visible)
          let strokeColor = baseStrokeColor;
          let strokeDasharray = baseDashArray;

          if (isCutPointEdge) {
            strokeColor = '#00F0FF'; // Cyan for cut-points
            strokeDasharray = '10 5'; // Longer dashes, more visible
          } else if (hasProblems) {
            strokeColor = '#f59e0b'; // Orange for problems
            strokeDasharray = '';
          }

          let fetchLabel = "";
          // Check for "OldLazy" (ValueHolder)
          if (rel.indirectionType === 'VALUEHOLDER') {
            fetchLabel = " (OldLazy)";
          } else if (isEager) {
            fetchLabel = " (E)";
          }

          let mappingLabel = rel.mappingType;
          if (rel.nestedTable) mappingLabel = "NestedTable";
          else if (rel.arrayMapping) mappingLabel = "Array";
          else if (rel.variableOneToOne) mappingLabel = "VarOneToOne";
          else if (rel.directCollection) mappingLabel = "DirectCol";
          else if (rel.aggregateCollection) mappingLabel = "AggCol";
          else if (rel.directMapMapping) mappingLabel = "DirectMap";

          transformedEdges.push({
            id: `e-${n.name}-${rel.targetEntity}-${rIdx}`,
            source: n.name,
            target: rel.targetEntity,
            label: (isCutPointEdge ? '✂️ ' : '') + mappingLabel + (rel.owningSide ? ' (O)' : '') + fetchLabel + (violationCount > 0 ? ` (${violationCount})` : ''),
            animated: hasProblems && !isCutPointEdge,
            type: 'smoothstep',
            style: {
              stroke: strokeColor,
              strokeWidth: isCutPointEdge ? 3.5 : (rel.owningSide ? 2.5 : 1.5),
              strokeDasharray: strokeDasharray
            },
            labelStyle: {
              fill: strokeColor,
              fontWeight: '600',
              fontSize: '10px',
              background: 'white',
              padding: '1px 4px',
              borderRadius: '3px',
              border: `1px solid ${strokeColor}`
            },
            markerEnd: rel.owningSide ? 'arrowclosed' : undefined,
            data: {
              ...rel,
              violationCount,
              hasProblems,
              isEager,
              isCutPointEdge
            }
          });
        }
      });
    });

    // Add inheritance edges
    (data.nodes || []).forEach((n: EntityNodeData) => {
      if (n.parentEntity) {
        const parentNode = data.nodes.find((tn: EntityNodeData) => tn.name === n.parentEntity);
        if (parentNode) {
          const inheritanceLabel = n.inheritanceStrategy ? `inherits (${n.inheritanceStrategy})` : 'inherits';
          transformedEdges.push({
            id: `inherit-${n.name}-${n.parentEntity}`,
            source: n.name,
            target: n.parentEntity,
            label: inheritanceLabel,
            type: 'smoothstep',
            style: {
              stroke: '#8b5cf6', // purple for inheritance
              strokeWidth: 2,
              strokeDasharray: '5 3',
            },
            labelStyle: {
              fill: '#8b5cf6',
              fontWeight: '600',
              fontSize: '10px',
              background: 'white',
              padding: '1px 4px',
              borderRadius: '3px',
              border: '1px solid #8b5cf6'
            },
            markerEnd: 'arrowclosed',
            data: {
              isInheritance: true,
              inheritanceStrategy: n.inheritanceStrategy
            }
          });
        }
      }
    });

    const { nodes: layoutedNodes, edges: layoutedEdges } = getLayoutedElements(transformedNodes, transformedEdges);
    setNodes(layoutedNodes);
    setEdges(layoutedEdges);
    const eagerCount = (data.nodes || []).reduce((count, n) => count + (n.relationships ? n.relationships.filter((r: RelationshipMetadata) => !r.lazy).length : 0), 0);
    const errorCount = (data.violations || []).filter((v: Violation) => v.severity === 'ERROR').length;
    const warningCount = (data.violations || []).filter((v: Violation) => v.severity === 'WARNING').length;
    const infoCount = (data.violations || []).filter((v: Violation) => v.severity === 'INFO').length;
    setStats({
      nodes: (data.nodes || []).length,
      anomalies: (data.anomalies || []).length,
      violations: (data.violations || []).length,
      eager: eagerCount,
      errorCount,
      warningCount,
      infoCount
    });
    setTimeout(() => fitView(), 100);
  }, [setNodes, setEdges, fitView]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setError(null);
    fetch(`/${selectedReport}`)
      .then(res => res.json())
      .then(data => {
        processReportData(data);
        setLoading(false);
        setTimeout(() => fitView(), 100);
      })
      .catch(err => {
        console.error("Failed to load analysis report:", err);
        setError(`Failed to load report: ${err.message}`);
        setLoading(false);
      });
  }, [selectedReport, processReportData, fitView]);

  const handleFileUpload = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const resultStr = e.target?.result as string;
        console.log("File content length:", resultStr.length);
        const data = JSON.parse(resultStr);
        console.log("Parsed JSON data:", data);

        if (!data || !data.nodes) {
          throw new Error("Invalid report format: 'nodes' array missing");
        }

        setError(null);
        setLoading(true);

        // Use requestAnimationFrame to let UI render loading state
        requestAnimationFrame(() => {
          try {
            processReportData(data);
            setLoading(false);
            setSelectedReport('uploaded');
          } catch (processErr) {
            console.error("Processing error:", processErr);
            setError(`Error processing report: ${(processErr as Error).message}`);
            setLoading(false);
          }
        });
      } catch (err) {
        console.error("Load error:", err);
        setError(`Invalid JSON file: ${(err as Error).message}`);
        setLoading(false);
      }
    };
    reader.readAsText(file);
  }, [processReportData]);

  const onLayout = useCallback(async (direction: 'TB' | 'LR' | 'ORGANIC' | 'GRID' | 'RADIAL' | 'CLUSTER') => {
    // First, filter out any existing groupNodes and remove parentId from entity nodes
    const entityNodesOnly = nodes
      .filter(n => n.type === 'entityNode')
      .map(n => ({ ...n, parentId: undefined }));

    let result;
    switch (direction) {
      case 'ORGANIC':
        result = await getElkLayout(entityNodesOnly, edges);
        break;
      case 'RADIAL': result = getRadialLayout(entityNodesOnly, edges); break;
      case 'GRID': result = getGridLayout(entityNodesOnly, edges); break;
      case 'CLUSTER':
        result = getClusterLayout(entityNodesOnly, edges, (n) => getAggregateForNode(n, entityNodesOnly, edges, heuristics[selectedHeuristic], analysisConfig));
        break;
      default: result = getLayoutedElements(entityNodesOnly, edges, direction);
    }

    const layoutedNodes = result.nodes;

    if (groupingMode) {
      // Use the selected heuristic (default is server-side truth)
      const heuristicFn = heuristics[selectedHeuristic];

      // Create aggregate groups using the selected heuristic
      const aggregates = new Map<string, Node[]>();
      layoutedNodes.filter(n => n.type === 'entityNode').forEach(n => {
        const agg = getAggregateForNode(n, layoutedNodes, edges, heuristicFn, analysisConfig);
        if (!aggregates.has(agg)) aggregates.set(agg, []);
        aggregates.get(agg)?.push(n);
      });

      const groupNodes: Node[] = [];
      const updatedEntityNodes = layoutedNodes.filter(n => n.type === 'entityNode').map(n => {
        const agg = getAggregateForNode(n, layoutedNodes, edges, heuristicFn, analysisConfig);
        return { ...n, parentId: `group-${agg}` };
      });

      aggregates.forEach((children, agg) => {
        // Calculate bounds with significantly more padding to avoid overlap and feel less cramped
        const padding = 150;
        const headerSpace = 70;
        const nodeWidth = 120;
        const nodeHeight = 70;
        const minX = Math.min(...children.map(c => c.position.x));
        const minY = Math.min(...children.map(c => c.position.y));
        const maxX = Math.max(...children.map(c => c.position.x + nodeWidth));
        const maxY = Math.max(...children.map(c => c.position.y + (c.data.showAttributes ? 200 : nodeHeight)));

        groupNodes.push({
          id: `group-${agg}`,
          type: 'groupNode',
          position: { x: minX - padding, y: minY - padding - headerSpace },
          style: {
            width: maxX - minX + padding * 2,
            height: maxY - minY + padding * 2 + headerSpace
          },
          data: {
            label: agg,
            aggregateName: agg,
            color: getAggregateColor(agg),
            memberCount: children.length
          },
          zIndex: -1,
        });

        // Adjust children positions to be relative to parent
        updatedEntityNodes.filter(n => n.parentId === `group-${agg}`).forEach(n => {
          n.position = {
            x: n.position.x - (minX - padding),
            y: n.position.y - (minY - padding - headerSpace)
          };
        });
      });

      setNodes([...groupNodes, ...updatedEntityNodes]);
    } else {
      // When grouping mode is OFF, return only entity nodes without parentId
      setNodes(layoutedNodes.map(n => ({ ...n, parentId: undefined })));
    }

    setTimeout(() => fitView({ padding: 0.2 }), 100);
  }, [nodes, edges, setNodes, fitView, groupingMode, selectedHeuristic, analysisConfig]);

  const toggleAttributes = useCallback(() => {
    setShowAttributes(prev => {
      const next = !prev;
      setNodes(nds => nds.map(node => ({
        ...node,
        data: { ...node.data, showAttributes: next }
      })));
      return next;
    });
    // Re-layout after state update has propagated
    setTimeout(() => onLayout('TB'), 100);
  }, [setNodes, onLayout]);

  const onConnect = useCallback(
    (params: Connection | Edge) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  const onNodeClick = (_: unknown, node: Node) => { setSelectedNodeId(node.id); setSelectedEdgeId(null); };
  const onEdgeClick = useCallback((_: unknown, edge: Edge) => { setSelectedEdgeId(edge.id); setSelectedNodeId(null); }, []);

  const selectedNode = nodes.find(n => n.id === selectedNodeId);
  const selectedEdge = edges.find(e => e.id === selectedEdgeId);

  // Generate PDF Report function
  const generatePDFReport = useCallback(() => {
    const doc = new jsPDF();
    const pageWidth = doc.internal.pageSize.getWidth();
    let y = 20;
    const lineHeight = 7;
    const margin = 20;

    // Helper function to add text with line wrapping
    const addText = (text: string, x: number, fontSize: number = 10, color: [number, number, number] = [0, 0, 0]) => {
      doc.setFontSize(fontSize);
      doc.setTextColor(...color);
      const lines = doc.splitTextToSize(text, pageWidth - margin * 2);
      doc.text(lines, x, y);
      y += lines.length * lineHeight;
    };

    const addSection = (title: string) => {
      if (y > 250) { doc.addPage(); y = 20; }
      y += 5;
      doc.setFillColor(112, 0, 255);
      doc.rect(margin, y - 5, pageWidth - margin * 2, 8, 'F');
      doc.setTextColor(255, 255, 255);
      doc.setFontSize(12);
      doc.text(title, margin + 3, y);
      doc.setTextColor(0, 0, 0);
      y += 12;
    };

    // Title
    doc.setFontSize(24);
    doc.setTextColor(112, 0, 255);
    doc.text('EclipseLink Analyzer Report', margin, y);
    y += 15;
    doc.setFontSize(10);
    doc.setTextColor(100, 100, 100);
    doc.text(`Generated: ${new Date().toLocaleString()}`, margin, y);
    y += 10;
    doc.text(`Report: ${selectedReport}`, margin, y);
    y += 15;

    // Statistics Section
    addSection('📊 Global Statistics');
    addText(`• Total Entities: ${stats.nodes}`, margin);
    addText(`• Total Violations: ${stats.violations}`, margin);
    addText(`• Errors: ${stats.errorCount} | Warnings: ${stats.warningCount} | Info: ${stats.infoCount}`, margin);
    addText(`• Eager Fetch Relations: ${stats.eager}`, margin);
    addText(`• Anomalies Detected: ${stats.anomalies}`, margin);

    // DDD Aggregates Section
    addSection('🎯 DDD Aggregates Analysis');
    const aggregates = new Map<string, { count: number; roots: string[] }>();
    nodes.forEach(n => {
      const agg = n.data.aggregateName || 'General';
      if (!aggregates.has(agg)) aggregates.set(agg, { count: 0, roots: [] });
      aggregates.get(agg)!.count++;
      if (n.data.dddRole === 'AGGREGATE_ROOT') aggregates.get(agg)!.roots.push(n.data.name);
    });
    aggregates.forEach((data, name) => {
      addText(`• ${name}: ${data.count} entities`, margin);
      if (data.roots.length > 0) {
        addText(`  → Aggregate Roots: ${data.roots.join(', ')}`, margin + 5, 9, [112, 0, 255]);
      }
    });

    // Violations Section
    addSection('⚠️ Violations & Issues');
    const allViolations = nodes.flatMap(n => n.data.violations || []);
    const errors = allViolations.filter(v => v.severity === 'ERROR');
    const warnings = allViolations.filter(v => v.severity === 'WARNING');

    if (errors.length > 0) {
      addText(`ERRORS (${errors.length}):`, margin, 11, [255, 0, 64]);
      errors.slice(0, 10).forEach(v => addText(`  • ${v.message}`, margin, 9));
      if (errors.length > 10) addText(`  ... and ${errors.length - 10} more`, margin, 9);
    }
    if (warnings.length > 0) {
      addText(`WARNINGS (${warnings.length}):`, margin, 11, [255, 138, 0]);
      warnings.slice(0, 10).forEach(v => addText(`  • ${v.message}`, margin, 9));
      if (warnings.length > 10) addText(`  ... and ${warnings.length - 10} more`, margin, 9);
    }

    // Performance Recommendations
    addSection('🚀 TODO & Recommendations');
    const eagerRelations = nodes.filter(n => n.data.relationships?.some((r: { lazy: boolean }) => !r.lazy));
    const highConnectivity = nodes.filter(n => (n.data.relationships?.length || 0) > 3);

    addText('Performance:', margin, 11, [0, 0, 0]);
    if (eagerRelations.length > 0) {
      addText(`  ⚡ ${eagerRelations.length} entities have EAGER fetch relations - consider switching to LAZY`, margin, 9, [255, 138, 0]);
      eagerRelations.slice(0, 5).forEach(n => addText(`    → ${n.data.name}`, margin, 8, [100, 100, 100]));
    }

    addText('Architecture:', margin, 11, [0, 0, 0]);
    if (highConnectivity.length > 0) {
      addText(`  🔗 ${highConnectivity.length} entities have high connectivity (>3 relations) - potential coupling`, margin, 9, [112, 0, 255]);
    }

    const noAggregate = nodes.filter(n => !n.data.aggregateName || n.data.aggregateName === 'Default');
    if (noAggregate.length > 0) {
      addText(`  📦 ${noAggregate.length} entities without clear aggregate - consider DDD refactoring`, margin, 9, [0, 136, 255]);
    }

    // Save PDF
    doc.save(`eclipselink-report-${new Date().toISOString().slice(0, 10)}.pdf`);
  }, [nodes, stats, selectedReport]);

  // When toggling expert options, ensure we are using the Shared or Combined engine to see results
  const handleConfigChange = useCallback((key: keyof AnalysisConfig, value: boolean | string) => {
    setAnalysisConfig(prev => ({ ...prev, [key]: value }));

    // Auto-switch to shared heuristic if not already selected, so the user sees the effect immediately
    if (selectedHeuristic === 'server' || selectedHeuristic === 'package') {
      setSelectedHeuristic('shared');
    }
  }, [selectedHeuristic]);

  // DDD Layers filtering logic - apply opacity and color based on active layer

  // AUTO-LAYOUT TRIGGER
  // When heuristics or config change, automatically re-run the layout
  useEffect(() => {
    if (nodes.length > 0) {
      // Debounce slightly to avoid flicker on rapid toggles
      const timer = setTimeout(() => {
        onLayout('CLUSTER');
      }, 50);
      return () => clearTimeout(timer);
    }
  }, [analysisConfig, selectedHeuristic]);
  const layerColors = {
    aggregates: '#7000FF', // Purple
    cycles: '#FF0040',     // Red
    cuts: '#00F0FF',       // Cyan
    perf: '#FF8A00',       // Orange
    vo: '#A855F7',         // Light Purple for VOs
    anomalies: '#FF0040',  // Red for Anomalies
  };

  const filteredNodes = useMemo(() => {
    if (activeLayer === 'aggregates') {
      // Reset all nodes to full opacity
      return nodes.map(node => ({
        ...node,
        data: { ...node.data, focusOpacity: 1 }
      }));
    }

    return nodes.map(node => {
      let isRelevant = false;

      switch (activeLayer) {
        case 'cycles':
          isRelevant = node.data.isInCycle === true;
          break;
        case 'cuts':
          isRelevant = node.data.isCutPoint === true;
          break;
        case 'perf':
          isRelevant = node.data.hasEagerRisk === true;
          break;
        case 'vo':
          isRelevant = node.data.isPotentialVO === true;
          break;
        case 'anomalies':
          isRelevant = node.data.hasAnomalies === true;
          break;
      }

      // Special handling for VO mode - hide others completely and expand properties
      if (activeLayer === 'vo') {
        return {
          ...node,
          data: {
            ...node.data,
            focusOpacity: isRelevant ? 1 : 0, // Hide completely
            showAttributes: isRelevant ? true : node.data.showAttributes // Expand VOs
          },
          hidden: !isRelevant // Actually hide non-VO nodes
        };
      }

      return {
        ...node,
        data: {
          ...node.data,
          focusOpacity: isRelevant ? 1 : 0.15
        }
      };
    });
  }, [nodes, activeLayer]);

  // AUTO-LAYOUT for specific layers (Bounded Contexts & Aggregates)
  useEffect(() => {
    if (activeLayer === 'cuts') {
      setGroupingMode(true);
      setSelectedHeuristic('package');
      setTimeout(() => onLayout('CLUSTER'), 50);
    } else if (activeLayer === 'aggregates') {
      setGroupingMode(true);
      setSelectedHeuristic('shared');
      setTimeout(() => onLayout('ORGANIC'), 50); // Using Organic instead of Cluster grid
    } else if (groupingMode) {
      setGroupingMode(false);
      setTimeout(() => onLayout('TB'), 50);
    }
  }, [activeLayer]);

  // Cycle Detection Logic (Client-side)
  // We identify "Complex Cycles" (Length >= 3) to distinguish from simple bidirectional (Length 2)
  const complexCycleEdges = useMemo(() => {
    if (activeLayer !== 'cycles' || !nodes.length) return new Set<string>();

    const adj = new Map<string, string[]>();
    edges.forEach(e => {
      if (!adj.has(e.source)) adj.set(e.source, []);
      if (e.source !== e.target) { // Ignore self-loops for this check
        adj.get(e.source)?.push(e.target);
      }
    });

    const visited = new Set<string>();
    const recursionStack = new Set<string>();
    const path: string[] = [];
    const detectedCycles: string[][] = [];

    function dfs(node: string) {
      visited.add(node);
      recursionStack.add(node);
      path.push(node);

      const neighbors = adj.get(node) || [];
      for (const neighbor of neighbors) {
        // Prevent trivial backtracking in undirected sense (though this is directed graph)
        // For directed cycles, we simply check if neighbor is in recursion stack
        if (!visited.has(neighbor)) {
          dfs(neighbor);
        } else if (recursionStack.has(neighbor)) {
          // Cycle found!
          const cycleStartIndex = path.indexOf(neighbor);
          if (cycleStartIndex !== -1) {
            const cycle = path.slice(cycleStartIndex);
            // Filter out simple bidirectional (length 2)
            // A->B, B->A is length 2. User focuses on "more than 1 level" (i.e., length > 2)
            if (cycle.length > 2) {
              detectedCycles.push([...cycle]);
            }
          }
        }
      }

      path.pop();
      recursionStack.delete(node);
    }

    nodes.forEach(node => {
      if (!visited.has(node.id)) {
        dfs(node.id);
      }
    });

    // Identify edges involved in these complex cycles
    const relevantEdges = new Set<string>();
    detectedCycles.forEach(cycle => {
      for (let i = 0; i < cycle.length; i++) {
        const source = cycle[i];
        const target = cycle[(i + 1) % cycle.length];
        const edge = edges.find(e => e.source === source && e.target === target);
        if (edge) relevantEdges.add(edge.id);
      }
    });

    return relevantEdges;
  }, [nodes, edges, activeLayer]);

  const filteredEdges = useMemo(() => {
    if (activeLayer === 'aggregates') return edges;

    const layerColor = layerColors[activeLayer as keyof typeof layerColors];

    return edges.map(edge => {
      let isRelevant = false;
      let overrideColor = undefined;
      let overrideOpacity = undefined;

      switch (activeLayer) {
        case 'cycles':
          // Highlight complex cycles (length > 2)
          if (complexCycleEdges.has(edge.id)) {
            isRelevant = true;
            overrideColor = '#FF0040';
            overrideOpacity = 1;
          }
          break;
        case 'cuts':
          isRelevant = edge.data?.mappingType === 'ManyToOne' || edge.data?.mappingType === 'OneToMany';
          break;
        case 'perf':
          isRelevant = edge.data?.isEager === true;
          break;
        case 'vo':
          isRelevant = false;
          break;
        case 'anomalies':
          isRelevant = edge.data?.hasProblems === true;
          break;
      }
      // For cycle edges, completely override the style (don't spread old style)
      const isCycleEdge = activeLayer === 'cycles' && isRelevant;

      const newStyle = isCycleEdge ? {
        stroke: '#FF0040',
        strokeWidth: 3,
        strokeDasharray: '0', // Solid line for cycle edges
        opacity: 1,
        transition: 'all 0.3s ease-in-out'
      } : {
        ...edge.style,
        stroke: isRelevant ? layerColor : edge.style?.stroke,
        opacity: isRelevant ? 1 : 0.05,
        strokeWidth: isRelevant ? 2.5 : 1,
        transition: 'all 0.3s ease-in-out'
      };

      return {
        ...edge,
        style: newStyle,
        labelStyle: isCycleEdge ? {
          fill: '#FF0040',
          fontWeight: '700',
          fontSize: '11px',
          background: 'white',
          padding: '2px 6px',
          borderRadius: '4px',
          border: '2px solid #FF0040'
        } : edge.labelStyle,
        animated: isCycleEdge
      };
    });
  }, [edges, activeLayer, nodes, complexCycleEdges, layerColors]);

  if (loading) {
    return (
      <div className="h-screen w-screen flex flex-col items-center justify-center bg-background-light text-text-primary">
        <Loader2 className="animate-spin text-primary mb-4" size={48} />
        <p className="text-text-secondary font-medium">Loading EclipseLink Analysis...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="h-screen w-screen flex flex-col items-center justify-center bg-background-light text-text-primary">
        <AlertCircle className="text-status-error mb-4" size={48} />
        <p className="text-text-primary font-medium mb-2">Failed to load model</p>
        <p className="text-text-muted text-sm mb-4">{error}</p>
        <button
          onClick={() => setError(null)}
          className="px-4 py-3 bg-background-card text-text-primary rounded border border-border hover:bg-background-header"
        >
          Dismiss
        </button>
      </div>
    );
  }

  return (
    <div className={`flex h-screen bg-body text-main font-sans selection:bg-primary/30 overflow-hidden ${theme === 'dark' ? 'dark-theme' : ''}`} style={{ backgroundColor: 'var(--bg-body)', color: 'var(--text-main)' }}>
      {/* SIDEBAR */}
      <aside className="w-[260px] bg-panel border-r border-[#E5E7EB] dark:border-[#222] flex flex-col z-20" style={{ backgroundColor: 'var(--bg-panel)', borderColor: 'var(--border-subtle)' }}>
        <div className="p-6 border-b border-[#E5E7EB] dark:border-[#222]" style={{ borderColor: 'var(--border-subtle)' }}>
          <div className="flex items-center gap-3 mb-6">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-[#7000FF] to-[#00F0FF] flex items-center justify-center shadow-lg shadow-primary/20">
              <GitGraph size={20} className="text-white" />
            </div>
            <h1 className="font-bold text-[15px] tracking-tight">EclipseLink Analyzer</h1>
          </div>

          <div className="space-y-4">
            <div>
              <label className="text-[10px] uppercase tracking-widest text-muted font-bold block mb-2" style={{ color: 'var(--text-muted)' }}>Analysis Target</label>
              <div className="relative group">
                <select
                  value={selectedReport}
                  onChange={(e) => setSelectedReport(e.target.value)}
                  className="w-full bg-body border border-[#E5E7EB] dark:border-[#222] rounded-md px-3 py-2.5 text-[12px] appearance-none focus:outline-none focus:ring-1 focus:ring-primary transition-all cursor-pointer group-hover:border-active"
                  style={{ backgroundColor: 'var(--bg-body)', borderColor: 'var(--border-subtle)', color: 'var(--text-main)' }}
                >
                  {reports.map(r => (
                    <option key={r.id} value={r.id}>{r.name}</option>
                  ))}
                  <option value="uploaded">Uploaded File</option>
                </select>
                <div className="absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none text-muted">
                  <Database size={14} />
                </div>
              </div>
            </div>

            <button
              onClick={() => document.getElementById('report-upload')?.click()}
              className="w-full py-2.5 px-4 bg-body border border-[#E5E7EB] dark:border-[#222] rounded-md text-[12px] font-medium flex items-center justify-center gap-2 hover:bg-neutral-50 dark:hover:bg-white/5 transition-all text-secondary"
              style={{ backgroundColor: 'var(--bg-body)', borderColor: 'var(--border-subtle)', color: 'var(--text-secondary)' }}
            >
              <Upload size={14} /> Import DDL Report
            </button>
            <input
              id="report-upload"
              type="file"
              accept=".json"
              onChange={handleFileUpload}
              className="hidden"
            />
          </div>
        </div>
        <nav className="flex-1 px-3 space-y-6">
          {/* Redundant Overview view option removed as requested */}




          <div className="space-y-4">
            <div>
              <h3 className="px-3 mb-2 text-[11px] uppercase tracking-wider text-muted font-bold flex items-center gap-2" style={{ color: 'var(--text-muted)' }}>
                <Database size={12} /> JPA Mapping Analysis
              </h3>

              <div className="mx-3 mb-3 p-2 bg-body/50 border border-subtle rounded flex items-center justify-between text-[11px]" style={{ backgroundColor: 'var(--bg-panel-hover)', borderColor: 'var(--border-subtle)' }}>
                <div className="flex flex-col items-center flex-1">
                  <span className="font-bold text-score-low" style={{ color: 'var(--score-low)' }}>{stats.errorCount}</span>
                  <span className="text-[9px] text-muted" style={{ color: 'var(--text-muted)' }}>ERR</span>
                </div>
                <div className="w-px h-6 bg-subtle" style={{ backgroundColor: 'var(--border-subtle)' }}></div>
                <div className="flex flex-col items-center flex-1">
                  <span className="font-bold text-score-med" style={{ color: 'var(--score-med)' }}>{stats.warningCount}</span>
                  <span className="text-[9px] text-muted" style={{ color: 'var(--text-muted)' }}>WARN</span>
                </div>
                <div className="w-px h-6 bg-subtle" style={{ backgroundColor: 'var(--border-subtle)' }}></div>
                <div className="flex flex-col items-center flex-1">
                  <span className="font-bold text-primary" style={{ color: 'var(--primary)' }}>{stats.infoCount}</span>
                  <span className="text-[9px] text-muted" style={{ color: 'var(--text-muted)' }}>INFO</span>
                </div>
              </div>

              {/* DOMAIN VIEW DROPDOWN */}
              <div className="relative mb-4">
                <label className="text-[10px] uppercase font-bold text-muted tracking-wider mb-1.5 block" style={{ color: 'var(--text-muted)' }}>
                  Domain View Layer
                </label>
                <div className="relative group">
                  <select
                    value={activeLayer}
                    onChange={(e) => setActiveLayer(e.target.value as any)}
                    className="w-full appearance-none bg-panel border-subtle border rounded-md px-3 py-2.5 text-[13px] font-medium outline-none focus:border-primary transition-all cursor-pointer"
                    style={{
                      backgroundColor: 'var(--bg-panel)',
                      borderColor: 'var(--border-subtle)',
                      color: 'var(--text-main)',
                      boxShadow: '0 2px 5px rgba(0,0,0,0.05)'
                    }}
                  >
                    <optgroup label="📊 Quality Analysis">
                      <option value="aggregates">📊 Overview</option>
                      <option value="anomalies">⚠️ Schema Anomalies ({stats.anomalies})</option>
                      <option value="perf">⚡ Performance Risks ({stats.eager})</option>
                      <option value="cycles">🔄 Dependency Cycles</option>
                    </optgroup>
                    <optgroup label="🧩 DDD Patterns">
                      <option value="aggregates">💎 Aggregates Viewer</option>
                      <option value="cuts">🔲 Bounded Contexts</option>
                      <option value="vo">📦 Value Objects</option>
                    </optgroup>
                  </select>
                  <ChevronDown size={14} className="absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none text-muted" style={{ color: 'var(--text-muted)' }} />

                  {/* Active Layer Indicator / Legend */}
                  <div className="mt-2 text-[11px] px-2 py-1.5 rounded flex items-center gap-2"
                    style={{
                      backgroundColor: activeLayer === 'aggregates' ? 'transparent' : 'var(--bg-panel-hover)',
                      border: activeLayer === 'aggregates' ? 'none' : '1px solid var(--border-subtle)'
                    }}>
                    {activeLayer === 'aggregates' && <LayoutDashboard size={12} className="text-secondary" />}
                    {activeLayer === 'anomalies' && <ShieldAlert size={12} className="text-score-low" />}
                    {activeLayer === 'perf' && <Activity size={12} className="text-score-med" />}
                    {activeLayer === 'cycles' && <GitMerge size={12} className="text-score-low" />}
                    {activeLayer === 'aggregates' && <Layers size={12} className="text-accent-purple" />}
                    {activeLayer === 'cuts' && <Box size={12} className="text-primary" />}
                    {activeLayer === 'vo' && <Info size={12} style={{ color: '#A855F7' }} />}

                    <span className="text-muted" style={{ color: 'var(--text-muted)' }}>
                      {activeLayer === 'aggregates' && "Showing Aggregate DDD analysis"}
                      {activeLayer === 'anomalies' && "Filtering consistency issues"}
                      {activeLayer === 'perf' && "Highlighting EAGER & N+1 risks"}
                      {activeLayer === 'cycles' && "Red edges indicate cycles"}
                      {activeLayer === 'aggregates' && "Grouped by Root Entity"}
                      {activeLayer === 'cuts' && "Context boundaries with frames"}
                      {activeLayer === 'vo' && "Highlighting Value Objects"}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div>
              {/* Expert Heuristics section follows */}

              {/* EXPERT DDD TUNING (Client-side only) */}
              <div className="mt-4 px-3 py-3 bg-panel/30 border border-dashed border-subtle rounded-lg" style={{ borderColor: 'var(--border-subtle)' }}>
                <div className="flex items-center justify-between mb-3">
                  <span className="text-[10px] font-bold text-muted uppercase tracking-tighter" style={{ color: 'var(--text-muted)' }}>Expert Heuristics (Local)</span>
                  <Brain size={12} className="text-muted" style={{ color: 'var(--text-muted)' }} />
                </div>
                <div className="space-y-3">
                  <div className="space-y-1">
                    <label className="text-[10px] text-muted font-medium" style={{ color: 'var(--text-muted)' }}>Analysis engine</label>
                    <div className="w-full bg-body/50 border border-subtle px-2 py-1.5 rounded text-[11px] font-bold text-primary flex items-center justify-between"
                      style={{ backgroundColor: 'var(--bg-panel-hover)', borderColor: 'var(--border-subtle)', color: 'var(--primary)' }}
                    >
                      <span>Shared Rules (Official)</span>
                      <ShieldAlert size={12} className="opacity-50" />
                    </div>
                    <p className="text-[9px] text-muted mt-1" style={{ color: 'var(--text-muted)' }}>Using standardized Java/TS hybrid engine</p>
                  </div>
                  <div className="space-y-1">
                    <label className="text-[10px] text-muted font-medium" style={{ color: 'var(--text-muted)' }}>Semantic Knowledge</label>
                    <select
                      value={analysisConfig.semanticProfile}
                      onChange={(e) => handleConfigChange('semanticProfile', e.target.value)}
                      className="w-full bg-body border border-subtle px-2 py-1.5 rounded text-[11px] focus:outline-none"
                      style={{ backgroundColor: 'var(--bg-body)', borderColor: 'var(--border-subtle)', color: 'var(--text-main)' }}
                    >
                      <option value={SemanticProfile.GENERIC}>Generic Domain</option>
                      <option value={SemanticProfile.ECOMMERCE_OFBIZ}>E-Commerce (OFBiz)</option>
                      <option value={SemanticProfile.TREASURY_ISO20022}>Treasury (ISO 20022)</option>
                    </select>
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleConfigChange('enableSemantic', !analysisConfig.enableSemantic)}
                      className={`flex-1 py-1 rounded text-[10px] font-bold border ${analysisConfig.enableSemantic ? 'bg-primary/20 border-primary text-primary' : 'bg-transparent border-subtle text-muted'}`}
                      style={{ borderColor: analysisConfig.enableSemantic ? 'var(--primary)' : 'var(--border-subtle)', color: analysisConfig.enableSemantic ? 'var(--primary)' : 'var(--text-muted)' }}
                    >
                      Name Analysis
                    </button>
                    <button
                      onClick={() => handleConfigChange('enableTopology', !analysisConfig.enableTopology)}
                      className={`flex-1 py-1 rounded text-[10px] font-bold border ${analysisConfig.enableTopology ? 'bg-accent-purple/20 border-accent-purple text-accent-purple' : 'bg-transparent border-subtle text-muted'}`}
                      style={{ borderColor: analysisConfig.enableTopology ? 'var(--accent-purple)' : 'var(--border-subtle)', color: analysisConfig.enableTopology ? 'var(--accent-purple)' : 'var(--text-muted)' }}
                    >
                      Graph Shape
                    </button>
                  </div>
                </div>
              </div>

              {advancedReport && (
                <div className="mt-3 mx-3 px-3 py-2 bg-panel-hover rounded-lg border border-subtle flex items-center justify-between" style={{ backgroundColor: 'var(--bg-panel-hover)', borderColor: 'var(--border-subtle)' }}>
                  <div className="flex gap-3 text-[10px] font-mono">
                    <div className="flex flex-col">
                      <span className="text-muted" style={{ color: 'var(--text-muted)' }}>ADDS</span>
                      <span style={{ color: 'var(--primary)' }}>{advancedReport.aggregates.length}</span>
                    </div>
                    <div className="flex flex-col">
                      <span className="text-muted" style={{ color: 'var(--text-muted)' }}>VOS</span>
                      <span style={{ color: 'var(--score-med)' }}>{advancedReport.valueObjects.length}</span>
                    </div>
                    <div className="flex flex-col">
                      <span className="text-muted" style={{ color: 'var(--text-muted)' }}>CUTS</span>
                      <span style={{ color: 'var(--score-low)' }}>{advancedReport.cuts.length}</span>
                    </div>
                  </div>
                  <Brain size={14} className="text-primary animate-pulse" style={{ color: 'var(--primary)' }} />
                </div>
              )}
            </div>
          </div>

          <div>
            <h3 className="px-3 mb-2 text-[11px] uppercase tracking-wider text-muted font-medium" style={{ color: 'var(--text-muted)' }}>Domain Aggregates</h3>
            <div className="space-y-1 max-h-[250px] overflow-y-auto custom-scrollbar px-1">
              {Array.from(new Set(nodes.map(n => n.data.aggregateName || 'General'))).sort().map(agg => (
                <div key={agg} className="flex items-center gap-3 px-3 py-1.5 rounded-md text-[12px] text-secondary hover:bg-panel-hover transition-all"
                  style={{ color: 'var(--text-secondary)', '--hover-bg': 'var(--bg-panel-hover)' } as React.CSSProperties}
                >
                  <div className="w-2 h-2 rounded-full shadow-sm" style={{ backgroundColor: getAggregateColor(agg) }}></div>
                  <span className="truncate flex-1">{agg}</span>
                  <span className="text-[10px] text-muted font-mono" style={{ color: 'var(--text-muted)' }}>{nodes.filter(n => n.data.aggregateName === agg || (!n.data.aggregateName && agg === 'General')).length}</span>
                </div>
              ))}
            </div>
          </div>
        </nav>

        <div className="p-4 mt-auto border-t border-subtle" style={{ borderColor: 'var(--border-subtle)' }}>
          <button
            onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}
            className="w-full flex items-center justify-between px-3 py-2 rounded-md text-[13px] text-secondary hover:bg-panel-hover hover:text-main transition-all"
            style={{ color: 'var(--text-secondary)', '--hover-bg': 'var(--bg-panel-hover)' } as React.CSSProperties}
          >
            <div className="flex items-center gap-3">
              <Info size={16} />
              <span>{theme === 'light' ? 'Dark' : 'Light'} Mode</span>
            </div>
            <div className={`w-8 h-4 rounded-full relative transition-colors ${theme === 'dark' ? 'bg-primary' : 'bg-subtle'}`} style={{ backgroundColor: theme === 'dark' ? 'var(--primary)' : 'var(--border-subtle)' }}>
              <div className={`absolute top-0.5 w-3 h-3 rounded-full bg-white shadow-sm transition-all ${theme === 'dark' ? 'left-4.5' : 'left-0.5'}`} />
            </div>
          </button>
        </div>
      </aside>

      {/* MAIN STAGE */}
      <main className="flex-1 relative overflow-hidden flex flex-col" style={{ backgroundColor: 'var(--bg-body)' }}>
        {/* Grid Background Overlay */}
        <div className="absolute inset-0 opacity-[0.4] pointer-events-none"
          style={{ backgroundImage: `linear-gradient(var(--border-subtle) 1px, transparent 1px), linear-gradient(90deg, var(--border-subtle) 1px, transparent 1px)`, backgroundSize: '40px 40px' }}></div>

        {/* Top Floating Control Bar */}
        <div className="absolute top-4 left-1/2 -translate-x-1/2 z-10 flex items-center gap-3 px-4 py-2 bg-glass backdrop-blur-md border border-subtle rounded-full shadow-lg"
          style={{ backgroundColor: 'var(--bg-glass)', borderColor: 'var(--border-subtle)' }}>
          <select
            value={selectedReport}
            onChange={(e) => setSelectedReport(e.target.value)}
            className="bg-transparent text-[13px] font-medium text-main border-none focus:ring-0 cursor-pointer pr-8 appearance-none"
            style={{ color: 'var(--text-main)' }}
          >
            {reports.map(r => (
              <option key={r.id} value={r.id} className="bg-panel" style={{ backgroundColor: 'var(--bg-panel)', color: 'var(--text-main)' }}>{r.name}</option>
            ))}
          </select>
          <div className="w-px h-4 bg-subtle" style={{ backgroundColor: 'var(--border-subtle)' }}></div>
          <button onClick={() => onLayout('TB')} className="p-1.5 text-muted hover:text-primary transition-colors" title="Hierarchical Layout" style={{ color: 'var(--text-muted)' }}>
            <Layout size={16} />
          </button>
          <button onClick={() => onLayout('LR')} className="p-1.5 text-muted hover:text-primary transition-colors" title="Horizontal Layout" style={{ color: 'var(--text-muted)' }}>
            <GitGraph size={16} />
          </button>
          <button onClick={() => onLayout('ORGANIC')} className="p-1.5 text-muted hover:text-primary transition-colors" title="Organic Layout" style={{ color: 'var(--text-muted)' }}>
            <RefreshCw size={16} />
          </button>
          <button onClick={() => onLayout('RADIAL')} className="p-1.5 text-muted hover:text-primary transition-colors" title="Radial Stress Layout" style={{ color: 'var(--text-muted)' }}>
            <Activity size={16} />
          </button>
          <button onClick={() => onLayout('GRID')} className="p-1.5 text-muted hover:text-primary transition-colors" title="Grid Layout" style={{ color: 'var(--text-muted)' }}>
            <Layout size={16} />
          </button>
          <button onClick={() => onLayout('CLUSTER')} className="p-1.5 text-muted hover:text-accent-purple transition-colors" title="Cluster by Aggregate - Groups entities by DDD aggregate" style={{ color: 'var(--text-muted)' }}>
            <Layers size={16} />
          </button>
          <div className="w-px h-4 bg-subtle" style={{ backgroundColor: 'var(--border-subtle)' }}></div>
          <button onClick={toggleAttributes} className="p-1.5 text-muted hover:text-primary transition-colors" title="Toggle Attributes" style={{ color: 'var(--text-muted)' }}>
            {showAttributes ? <EyeOff size={16} /> : <Eye size={16} />}
          </button>
          <button
            onClick={generatePDFReport}
            className="p-1.5 text-muted hover:text-primary transition-colors"
            title="Download PDF Report"
            style={{ color: 'var(--text-muted)' }}
          >
            <FileDown size={16} />
          </button>
          <div className="w-px h-4 bg-subtle" style={{ backgroundColor: 'var(--border-subtle)' }}></div>
          <div className="flex items-center gap-3 px-2 text-[11px] font-bold">
            <div className="flex items-center gap-1.5">
              <div className="w-2 h-2 rounded-full bg-score-low" style={{ backgroundColor: 'var(--score-low)' }}></div>
              <span className="text-score-low" style={{ color: 'var(--score-low)' }}>{stats.errorCount}</span>
            </div>
            <div className="flex items-center gap-1.5">
              <div className="w-2 h-2 rounded-full bg-score-med" style={{ backgroundColor: 'var(--score-med)' }}></div>
              <span className="text-score-med" style={{ color: 'var(--score-med)' }}>{stats.warningCount}</span>
            </div>
          </div>
        </div>

        <div className="flex-1 relative">
          <ReactFlow
            nodes={filteredNodes}
            edges={filteredEdges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            onNodeClick={onNodeClick}
            onEdgeClick={onEdgeClick}
            nodeTypes={nodeTypes}
            defaultEdgeOptions={{
              type: 'smoothstep',
              style: { strokeDasharray: '5,5' },
            }}
            fitView
            fitViewOptions={{ padding: 0.5 }}
            minZoom={0.1}
          >
            <Background color="var(--border-active)" gap={20} className="opacity-0" />
            <Controls className="!bg-panel !border-subtle !shadow-none [&_button]:!border-subtle [&_path]:!fill-muted" style={{ backgroundColor: 'var(--bg-panel)', borderColor: 'var(--border-subtle)' }} />
            <MiniMap
              className="!bg-glass !border-subtle !backdrop-blur-md"
              style={{ backgroundColor: 'var(--bg-glass)', borderColor: 'var(--border-subtle)' }}
              nodeColor={() => 'var(--primary)'}
              maskColor="var(--accent-glow)"
            />
          </ReactFlow>
        </div>
      </main>

      {/* INSPECTOR */}
      <aside
        style={{ width: sidebarCollapsed ? '48px' : `${sidebarWidth}px`, backgroundColor: 'var(--bg-panel)', borderColor: 'var(--border-subtle)' }}
        className="bg-panel border-l border-subtle flex flex-col z-20 transition-[width] duration-300 relative group"
      >
        {/* Resize Handle */}
        {!sidebarCollapsed && (
          <div
            onMouseDown={handleResizeStart}
            className="absolute left-0 top-0 bottom-0 w-1 cursor-col-resize hover:bg-[#00F0FF]/30 transition-colors z-30"
          />
        )}

        {/* Collapse Toggle */}
        <button
          onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
          className="absolute -left-3 top-1/2 -translate-y-1/2 w-6 h-6 bg-panel border border-subtle rounded-full flex items-center justify-center text-secondary hover:text-main hover:border-primary transition-all z-40"
          style={{ backgroundColor: 'var(--bg-panel)', borderColor: 'var(--border-subtle)', color: 'var(--text-secondary)' }}
        >
          {sidebarCollapsed ? <ChevronLeft size={14} /> : <ChevronRight size={14} />}
        </button>

        {!sidebarCollapsed ? (
          <div className="flex-1 flex flex-col overflow-hidden">
            {selectedNode || selectedEdge ? (
              <>
                <div className="p-6 border-b border-subtle" style={{ borderColor: 'var(--border-subtle)' }}>
                  <div className="flex items-center justify-between mb-2">
                    <h2 className="text-[18px] font-semibold text-main truncate mr-2" style={{ color: 'var(--text-main)' }}>
                      {selectedNode ? selectedNode.id : selectedEdge?.data?.attributeName}
                    </h2>
                    <div className="w-8 h-8 rounded-full border-2 border-subtle border-t-score-med flex items-center justify-center text-[10px] font-bold text-score-med"
                      style={{ borderColor: 'var(--border-subtle)', color: 'var(--score-med)', borderTopColor: 'var(--score-med)' }}>
                      {selectedNode ? Math.max(100 - ((selectedNode.data.violations?.length || 0) * 10), 0) : '65'}
                    </div>
                  </div>
                  <p className="text-[12px] font-bold text-accent-purple uppercase tracking-wider" style={{ color: 'var(--accent-purple)' }}>
                    {selectedNode ? selectedNode.data.type : 'Relationship'}
                  </p>
                </div>

                <div className="flex border-b border-subtle bg-panel" style={{ borderColor: 'var(--border-subtle)', backgroundColor: 'var(--bg-panel)' }}>
                  <button
                    onClick={() => setActiveTab('mapping')}
                    className={`flex-1 py-3 text-[12px] font-medium transition-all border-b-2 ${activeTab === 'mapping' ? 'text-main border-primary bg-gradient-to-t from-primary/5 to-transparent' : 'text-secondary border-transparent hover:text-main'}`}
                    style={{ color: activeTab === 'mapping' ? 'var(--text-main)' : 'var(--text-secondary)', borderColor: activeTab === 'mapping' ? 'var(--primary)' : 'transparent' }}
                  >
                    Bounded Context (AI)
                  </button>
                  <button
                    onClick={() => setActiveTab('anomalies')}
                    className={`flex-1 py-3 text-[12px] font-medium transition-all border-b-2 ${activeTab === 'anomalies' ? 'text-main border-primary bg-gradient-to-t from-primary/5 to-transparent' : 'text-secondary border-transparent hover:text-main'}`}
                    style={{ color: activeTab === 'anomalies' ? 'var(--text-main)' : 'var(--text-secondary)', borderColor: activeTab === 'anomalies' ? 'var(--primary)' : 'transparent' }}
                  >
                    JPA Mapping Details
                  </button>
                  <button
                    onClick={() => setActiveTab('violations')}
                    className={`flex-1 py-3 text-[12px] font-medium transition-all border-b-2 ${activeTab === 'violations' ? 'text-main border-primary bg-gradient-to-t from-primary/5 to-transparent' : 'text-secondary border-transparent hover:text-main'}`}
                    style={{ color: activeTab === 'violations' ? 'var(--text-main)' : 'var(--text-secondary)', borderColor: activeTab === 'violations' ? 'var(--primary)' : 'transparent' }}
                  >
                    Global Issues
                  </button>
                </div>

                <div className="flex-1 overflow-y-auto custom-scrollbar">
                  {activeTab === 'mapping' ? (
                    <div className="p-0">
                      <div className="px-5 py-4 text-[11px] uppercase tracking-wider font-bold text-[#7000FF]">DDD Retro-Analysis</div>
                      <div className={`mx-5 p-4 bg-white/3 border border-[#222] rounded-md border-l-2 mb-4`} style={{ borderLeftColor: getAggregateColor(selectedNode?.data.aggregateName) }}>
                        <div className="flex gap-3 mb-2">
                          <div style={{ color: getAggregateColor(selectedNode?.data.aggregateName) }}>
                            {selectedNode?.data.dddRole === 'AGGREGATE_ROOT' ? '💠' : '📦'}
                          </div>
                          <h4 className="text-[13px] font-semibold text-main" style={{ color: 'var(--text-main)' }}>
                            {selectedNode?.data.dddRole === 'AGGREGATE_ROOT' ? 'Aggregate Root' : 'Domain Entity'}
                          </h4>
                        </div>
                        <div className="text-[12px] font-bold text-main mb-2" style={{ color: 'var(--text-main)' }}>
                          Aggregate: <span style={{ color: getAggregateColor(selectedNode?.data.aggregateName) }}>{selectedNode?.data.aggregateName || 'General'}</span>
                        </div>
                        <p className="text-[11px] text-secondary leading-relaxed" style={{ color: 'var(--text-secondary)' }}>
                          {selectedNode?.data.dddRole === 'AGGREGATE_ROOT'
                            ? 'High centrality score. This entity acts as an entry point and controls the lifecycle of related entities.'
                            : 'Internal member of the domain cluster. Lifecycle managed by the aggregate root.'}
                        </p>
                      </div>

                      {/* Algorithm Explanation Section */}
                      <div className="px-5 py-2 text-[11px] uppercase tracking-wider font-bold text-primary" style={{ color: 'var(--primary)' }}>Algorithm Details</div>
                      <div className="mx-5 mb-4 space-y-2">
                        {/* Package-based grouping */}
                        <div className="p-3 bg-panel/50 border border-subtle rounded-md" style={{ backgroundColor: 'var(--bg-panel)', borderColor: 'var(--border-subtle)' }}>
                          <div className="flex items-center gap-2 mb-1">
                            <span className="text-[10px] px-1.5 py-0.5 rounded bg-primary/20 text-primary font-bold" style={{ color: 'var(--primary)' }}>PACKAGE</span>
                            <span className="text-[11px] font-bold text-main" style={{ color: 'var(--text-main)' }}>Grouping Rule</span>
                          </div>
                          <p className="text-[10px] text-secondary" style={{ color: 'var(--text-secondary)' }}>
                            Entities in <code className="text-primary">{selectedNode?.data.packageName?.split('.').slice(-1)[0] || 'root'}</code> package are clustered together.
                          </p>
                        </div>

                        {/* Relationship Analysis */}
                        <div className="p-3 bg-panel/50 border border-subtle rounded-md" style={{ backgroundColor: 'var(--bg-panel)', borderColor: 'var(--border-subtle)' }}>
                          <div className="flex items-center gap-2 mb-1">
                            <span className="text-[10px] px-1.5 py-0.5 rounded bg-accent-purple/20 text-accent-purple font-bold" style={{ color: 'var(--accent-purple)' }}>RELATIONS</span>
                            <span className="text-[11px] font-bold text-main" style={{ color: 'var(--text-main)' }}>Ownership Analysis</span>
                          </div>
                          <p className="text-[10px] text-secondary" style={{ color: 'var(--text-secondary)' }}>
                            {selectedNode?.data.relationships?.length || 0} relationships detected.
                            {selectedNode?.data.relationships?.filter((r: RelationshipMetadata) => r.owningSide).length || 0} owned (controlling lifecycle).
                          </p>
                        </div>

                        {/* Cycle/Cut Analysis */}
                        {selectedNode?.data.isInCycle && (
                          <div className="p-3 bg-score-low/10 border border-score-low/30 rounded-md">
                            <div className="flex items-center gap-2 mb-1">
                              <span className="text-[10px] px-1.5 py-0.5 rounded bg-score-low/30 text-score-low font-bold" style={{ color: 'var(--score-low)' }}>CYCLE</span>
                              <span className="text-[11px] font-bold text-main" style={{ color: 'var(--text-main)' }}>Deep Dependency Cycle</span>
                            </div>
                            <p className="text-[10px] text-secondary" style={{ color: 'var(--text-secondary)' }}>
                              This entity is part of a deep dependency cycle (length 3 or more). Consider reviewing domain boundaries and fetch strategies.
                            </p>
                          </div>
                        )}

                        {selectedNode?.data.isCutPoint && (
                          <div className="p-3 bg-primary/10 border border-primary/30 rounded-md">
                            <div className="flex items-center gap-2 mb-1">
                              <span className="text-[10px] px-1.5 py-0.5 rounded bg-primary/30 text-primary font-bold" style={{ color: 'var(--primary)' }}>✂️ CUT</span>
                              <span className="text-[11px] font-bold text-main" style={{ color: 'var(--text-main)' }}>Cross-Aggregate Bridge</span>
                            </div>
                            <p className="text-[10px] text-secondary" style={{ color: 'var(--text-secondary)' }}>
                              This entity connects different aggregates. Consider decoupling via ID reference instead of direct object reference.
                            </p>
                          </div>
                        )}

                        {selectedNode?.data.isPotentialVO && (
                          <div className="p-3 bg-accent-purple/10 border border-accent-purple/30 rounded-md">
                            <div className="flex items-center gap-2 mb-1">
                              <span className="text-[10px] px-1.5 py-0.5 rounded bg-accent-purple/30 text-accent-purple font-bold" style={{ color: 'var(--accent-purple)' }}>💎 VO</span>
                              <span className="text-[11px] font-bold text-main" style={{ color: 'var(--text-main)' }}>Potential Value Object</span>
                            </div>
                            <p className="text-[10px] text-secondary" style={{ color: 'var(--text-secondary)' }}>
                              Few attributes, no outgoing relationships. Could be refactored as an @Embeddable value object.
                            </p>
                          </div>
                        )}
                      </div>

                      <div className="px-5 py-2">
                        <div className="text-[11px] text-muted mb-2 uppercase tracking-tighter font-bold" style={{ color: 'var(--text-muted)' }}>Heuristic Confidence</div>
                        <div className="h-1.5 w-full bg-subtle rounded-full overflow-hidden" style={{ backgroundColor: 'var(--border-subtle)' }}>
                          <div className="h-full bg-gradient-to-r from-accent-purple to-primary" style={{ width: selectedNode?.data.dddRole === 'AGGREGATE_ROOT' ? '85%' : '60%', backgroundColor: 'var(--primary)' }}></div>
                        </div>
                      </div>

                      <div className="px-5 py-4 text-[11px] uppercase tracking-wider font-bold text-accent-purple" style={{ color: 'var(--accent-purple)' }}>Performance Rules</div>
                      {(selectedNode?.data?.violations || []).map((v: Violation, idx: number) => (
                        <div key={idx} className={`mx-5 p-4 bg-panel border-subtle border rounded-md mb-4 border-l-2 ${v.severity === 'ERROR' ? 'border-l-score-low' : 'border-l-score-med'}`}
                          style={{ backgroundColor: 'var(--bg-panel)', borderColor: 'var(--border-subtle)' }}>
                          <div className="flex gap-3 mb-2">
                            <div className={v.severity === 'ERROR' ? 'text-score-low' : 'text-score-med'} style={{ color: v.severity === 'ERROR' ? 'var(--score-low)' : 'var(--score-med)' }}>⚠</div>
                            <h4 className="text-[13px] font-semibold text-main" style={{ color: 'var(--text-main)' }}>{v.ruleId}</h4>
                          </div>
                          <p className="text-[12px] text-secondary leading-relaxed" style={{ color: 'var(--text-secondary)' }}>{v.message}</p>
                          <button className="w-full mt-3 py-2 bg-body border border-subtle rounded text-[12px] hover:border-primary hover:text-primary transition-all text-main"
                            style={{ backgroundColor: 'var(--bg-body)', borderColor: 'var(--border-subtle)', color: 'var(--text-main)' }}>
                            Apply Fix
                          </button>
                        </div>
                      ))}
                      {(!selectedNode?.data.violations || selectedNode.data.violations.length === 0) && (
                        <div className="px-5 py-4 text-[12px] text-[#888] italic">No performance violations detected.</div>
                      )}
                    </div>
                  ) : (
                    <div className="p-0 space-y-0 text-[12px]">
                      {selectedNode && (
                        <>
                          <div className="px-5 py-4 text-[11px] uppercase tracking-wider font-bold text-accent-purple" style={{ color: 'var(--accent-purple)' }}>Class Details</div>
                          <div className="flex justify-between px-5 py-2 border-b border-subtle" style={{ borderColor: 'var(--border-subtle)' }}>
                            <span className="text-secondary" style={{ color: 'var(--text-secondary)' }}>Table Name</span>
                            <span className="font-mono text-main uppercase font-bold" style={{ color: 'var(--text-main)' }}>{selectedNode.data.name || 'N/A'}</span>
                          </div>
                          <div className="flex justify-between px-5 py-2 border-b border-white/5" style={{ borderColor: 'var(--border-subtle)' }}>
                            <span className="text-secondary" style={{ color: 'var(--text-secondary)' }}>Package</span>
                            <span className="font-mono text-main truncate max-w-[180px] text-right" style={{ color: 'var(--text-main)' }}>{selectedNode.data.packageName || 'N/A'}</span>
                          </div>
                          <div className="flex justify-between px-5 py-2 border-b border-white/5" style={{ borderColor: 'var(--border-subtle)' }}>
                            <span className="text-secondary" style={{ color: 'var(--text-secondary)' }}>JPA Type</span>
                            <span className="font-mono text-primary uppercase font-bold" style={{ color: 'var(--primary)' }}>{selectedNode.data.type || 'ENTITY'}</span>
                          </div>

                          <div className="px-5 py-4 text-[11px] uppercase tracking-wider font-bold text-accent-purple" style={{ color: 'var(--accent-purple)' }}>Schema Anomalies</div>
                          {nodes.find(n => n.id === selectedNode.id)?.data.hasAnomalies ? (
                            <div className="px-5 space-y-2">
                              <div className="p-3 bg-score-med/10 border border-score-med/20 rounded-md" style={{ backgroundColor: 'var(--accent-glow)' }}>
                                <div className="flex items-center gap-2 mb-1 text-score-med font-bold" style={{ color: 'var(--score-med)' }}>
                                  <ShieldAlert size={14} />
                                  <span>DDL Mismatch Detected</span>
                                </div>
                                <p className="text-[11px] text-secondary" style={{ color: 'var(--text-secondary)' }}>The database schema does not perfectly align with the JPA mapping. Check column types and constraints.</p>
                              </div>
                            </div>
                          ) : (
                            <div className="px-5 py-2 text-secondary italic" style={{ color: 'var(--text-secondary)' }}>No schema anomalies found.</div>
                          )}

                          <div className="px-5 py-4 text-[11px] uppercase tracking-wider font-bold text-accent-purple" style={{ color: 'var(--accent-purple)' }}>Attributes</div>
                          <div className="max-h-[200px] overflow-y-auto custom-scrollbar">
                            {Object.entries(selectedNode.data.attributes || {}).map(([name, attr]: [string, any], i: number) => (
                              <div key={i} className="flex justify-between px-5 py-2 border-b border-subtle group hover:bg-panel-hover transition-colors" style={{ borderColor: 'var(--border-subtle)' }}>
                                <span className="text-secondary group-hover:text-main transition-colors" style={{ color: 'var(--text-secondary)' }}>{name}</span>
                                <span className="font-mono text-primary font-medium" style={{ color: 'var(--primary)' }}>{String(attr.javaType).split('.').pop()}</span>
                              </div>
                            ))}
                          </div>

                          <div className="px-5 py-4 text-[11px] uppercase tracking-wider font-bold text-accent-purple" style={{ color: 'var(--accent-purple)' }}>Bytecode Weaving Status</div>
                          <div className="flex justify-between px-5 py-2 border-b border-subtle" style={{ borderColor: 'var(--border-subtle)' }}>
                            <span className="text-secondary" style={{ color: 'var(--text-secondary)' }}>Lazy Loading</span>
                            <span className="text-score-high font-semibold" style={{ color: 'var(--score-high)' }}>WEAVED</span>
                          </div>
                          <div className="flex justify-between px-5 py-2 border-b border-subtle" style={{ borderColor: 'var(--border-subtle)' }}>
                            <span className="text-secondary" style={{ color: 'var(--text-secondary)' }}>Change Tracking</span>
                            <span className="text-score-high font-semibold" style={{ color: 'var(--score-high)' }}>WEAVED</span>
                          </div>
                        </>
                      )}

                      {selectedEdge && (
                        <>
                          <div className="px-5 py-4 text-[11px] uppercase tracking-wider font-bold text-[#7000FF]">Relationship Details</div>
                          <div className="flex justify-between px-5 py-2 border-b border-white/5">
                            <span className="text-[#888]">Mapping Type</span>
                            <span className="font-mono text-white uppercase">{selectedEdge.data?.mappingType || 'N/A'}</span>
                          </div>
                          <div className="flex justify-between px-5 py-2 border-b border-white/5">
                            <span className="text-[#888]">Fetch Strategy</span>
                            <span className={`font-mono ${selectedEdge.data?.lazy === false ? 'text-[#FF0040] font-bold' : 'text-[#00E050]'}`}>
                              {selectedEdge.data?.lazy === false ? 'EAGER' : 'LAZY'}
                            </span>
                          </div>
                          <div className="flex justify-between px-5 py-2 border-b border-subtle" style={{ borderColor: 'var(--border-subtle)' }}>
                            <span className="text-secondary" style={{ color: 'var(--text-secondary)' }}>Owning Side</span>
                            <span className="text-main font-medium" style={{ color: 'var(--text-main)' }}>{selectedEdge.data?.owningSide ? 'YES' : 'NO'}</span>
                          </div>

                          <div className="px-5 py-4 text-[11px] uppercase tracking-wider font-bold text-accent-purple" style={{ color: 'var(--accent-purple)' }}>Cascade configuration</div>
                          <div className="px-5 grid grid-cols-2 gap-2 pb-4">
                            {['Persist', 'Merge', 'Remove', 'Refresh', 'Detach'].map(c => {
                              const has = selectedEdge.data?.[`cascade${c}`];
                              return (
                                <div key={c} className={`px-2 py-1.5 rounded border text-[10px] text-center ${has ? 'bg-primary/10 border-primary text-primary font-bold' : 'bg-transparent border-subtle text-muted'}`}
                                  style={{ borderColor: has ? 'var(--primary)' : 'var(--border-subtle)', color: has ? 'var(--primary)' : 'var(--text-muted)' }}>
                                  {c}
                                </div>
                              );
                            })}
                          </div>
                        </>
                      )}
                    </div>
                  )}

                  {activeTab === 'violations' && (
                    <div className="p-0">
                      <div className="px-5 py-4 text-[11px] uppercase tracking-wider font-bold text-accent-red" style={{ color: 'var(--score-low)' }}>
                        Global Health ({stats.errorCount + stats.warningCount} Issues)
                      </div>

                      <div className="px-5 mb-4">
                        <div className="flex gap-2 mb-2">
                          <button
                            onClick={() => setViolationFilters(prev => ({ ...prev, ERROR: !prev.ERROR }))}
                            className={`flex-1 py-1.5 px-2 rounded text-[10px] border flex items-center justify-center gap-1 ${violationFilters.ERROR ? 'bg-score-low/10 border-score-low text-score-low' : 'bg-transparent border-subtle text-muted'}`}
                            style={{ color: violationFilters.ERROR ? 'var(--score-low)' : 'var(--text-muted)', borderColor: violationFilters.ERROR ? 'var(--score-low)' : 'var(--border-subtle)' }}
                          >
                            <ShieldAlert size={12} /> Errors ({stats.errorCount})
                          </button>
                          <button
                            onClick={() => setViolationFilters(prev => ({ ...prev, WARNING: !prev.WARNING }))}
                            className={`flex-1 py-1.5 px-2 rounded text-[10px] border flex items-center justify-center gap-1 ${violationFilters.WARNING ? 'bg-score-med/10 border-score-med text-score-med' : 'bg-transparent border-subtle text-muted'}`}
                            style={{ color: violationFilters.WARNING ? 'var(--score-med)' : 'var(--text-muted)', borderColor: violationFilters.WARNING ? 'var(--score-med)' : 'var(--border-subtle)' }}
                          >
                            <CircleAlert size={12} /> Warnings ({stats.warningCount})
                          </button>
                        </div>
                      </div>

                      <div className="space-y-0">
                        {Array.from(new Set(nodes.flatMap(n => (n.data.violations || []).map((v: Violation) => JSON.stringify({ ...v, source: n.data.name }))))).map(s => JSON.parse(s)).filter((v: any) => violationFilters[v.severity as string]).map((v: any, i: number) => (
                          <div key={i} className="px-5 py-3 border-b border-subtle hover:bg-panel-hover group cursor-pointer"
                            style={{ borderColor: 'var(--border-subtle)' }}
                            onClick={() => {
                              const node = nodes.find(n => n.data.name === v.source);
                              if (node) {
                                onNodeClick({} as any, node);
                                setActiveTab('anomalies');
                              }
                            }}
                          >
                            <div className="flex items-center gap-2 mb-1">
                              <div className={`text-[10px] font-bold px-1.5 py-0.5 rounded ${v.severity === 'ERROR' ? 'bg-score-low text-white' : 'bg-score-med text-white'}`}
                                style={{ backgroundColor: v.severity === 'ERROR' ? 'var(--score-low)' : 'var(--score-med)' }}
                              >
                                {v.severity}
                              </div>
                              <div className="text-[11px] font-mono text-primary truncate" style={{ color: 'var(--primary)' }}>{v.source}</div>
                            </div>
                            <p className="text-[11px] text-secondary leading-relaxed" style={{ color: 'var(--text-secondary)' }}>
                              {v.message}
                            </p>
                          </div>
                        ))}

                        {(stats.errorCount + stats.warningCount === 0) && (
                          <div className="px-5 py-8 text-center text-muted italic text-[12px]">
                            No global violations found. Great job!
                          </div>
                        )}
                      </div>
                    </div>
                  )}
                </div>
              </>
            ) : (
              <div className="flex-1 flex flex-col overflow-hidden">
                <div className="p-6 border-b border-subtle bg-panel" style={{ backgroundColor: 'var(--bg-panel)', borderColor: 'var(--border-subtle)' }}>
                  <h2 className="text-[18px] font-semibold text-main mb-1" style={{ color: 'var(--text-main)' }}>Global Analysis</h2>
                  <p className="text-[11px] text-muted font-mono" style={{ color: 'var(--text-muted)' }}>RETRO-ANALYSIS SESSION v1.0</p>
                </div>

                <div className="flex-1 overflow-y-auto custom-scrollbar p-6 space-y-6">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="p-4 bg-panel border border-subtle rounded-lg" style={{ backgroundColor: 'var(--bg-panel)', borderColor: 'var(--border-subtle)' }}>
                      <div className="text-[10px] text-muted uppercase font-bold mb-1" style={{ color: 'var(--text-muted)' }}>Total Entities</div>
                      <div className="text-2xl font-semibold text-main" style={{ color: 'var(--text-main)' }}>{stats.nodes}</div>
                    </div>
                    <div className="p-4 bg-panel border border-subtle rounded-lg" style={{ backgroundColor: 'var(--bg-panel)', borderColor: 'var(--border-subtle)' }}>
                      <div className="text-[10px] text-muted uppercase font-bold mb-1" style={{ color: 'var(--text-muted)' }}>Anomalies</div>
                      <div className="text-2xl font-semibold text-score-med" style={{ color: 'var(--score-med)' }}>{stats.anomalies}</div>
                    </div>
                  </div>

                  <div className="p-5 bg-panel border-subtle border rounded-xl relative overflow-hidden group" style={{ backgroundColor: 'var(--bg-panel)', borderColor: 'var(--border-subtle)' }}>
                    <div className="absolute top-0 right-0 p-3 opacity-5 group-hover:scale-110 transition-transform">
                      <ShieldAlert size={64} style={{ color: 'var(--primary)' }} />
                    </div>
                    <div className="relative z-10">
                      <div className="text-[11px] font-bold text-accent-purple uppercase mb-4 tracking-widest" style={{ color: 'var(--accent-purple)' }}>Health Score</div>
                      <div className="flex items-center gap-4">
                        <div className="text-4xl font-bold text-main" style={{ color: 'var(--text-main)' }}>
                          {Math.max(100 - (stats.errorCount * 5 + stats.warningCount * 2), 0)}%
                        </div>
                        <div className="flex-1 h-2 bg-subtle rounded-full overflow-hidden" style={{ backgroundColor: 'var(--border-subtle)' }}>
                          <div className="h-full bg-gradient-to-r from-accent-purple to-primary" style={{ width: `${Math.max(100 - (stats.errorCount * 5 + stats.warningCount * 2), 0)}%`, backgroundColor: 'var(--primary)' }}></div>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div>
                    <h3 className="text-[11px] uppercase tracking-wider text-muted font-bold mb-3 px-1" style={{ color: 'var(--text-muted)' }}>Recent Violations</h3>
                    <div className="space-y-3">
                      {stats.errorCount > 0 ? (
                        <div className="p-3 bg-score-low/5 border border-score-low/20 rounded-md flex gap-3" style={{ backgroundColor: 'var(--accent-glow)', borderColor: 'var(--score-low)' }}>
                          <div className="text-score-low" style={{ color: 'var(--score-low)' }}>⚠</div>
                          <div className="text-[12px]">
                            <div className="text-main font-semibold" style={{ color: 'var(--text-main)' }}>Critical Performance Risks</div>
                            <p className="text-secondary" style={{ color: 'var(--text-secondary)' }}>Multiple N+1 query patterns detected in the current unit.</p>
                          </div>
                        </div>
                      ) : (
                        <div className="p-4 border border-dashed border-subtle rounded-md text-center" style={{ borderColor: 'var(--border-subtle)' }}>
                          <p className="text-[11px] text-muted" style={{ color: 'var(--text-muted)' }}>No critical violations</p>
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="pt-4">
                    <div className="text-[11px] text-muted text-center border-t border-subtle pt-4" style={{ color: 'var(--text-muted)', borderColor: 'var(--border-subtle)' }}>
                      Select an entity or relationship to drill down into specific JPA mappings.
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        ) : (
          <div className="flex-1 flex flex-col items-center py-10 opacity-40 bg-panel" style={{ backgroundColor: 'var(--bg-panel)' }}>
            <ChevronRight className="mb-2 text-secondary" size={20} style={{ color: 'var(--text-secondary)' }} />
            <div className="[writing-mode:vertical-lr] text-[10px] uppercase font-bold tracking-widest text-secondary" style={{ color: 'var(--text-secondary)' }}>
              Inspector Collapsed
            </div>
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
