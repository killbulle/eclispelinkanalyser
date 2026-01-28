import { memo, useState, useRef } from 'react';
import { Handle, Position } from 'reactflow';

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
    // Phase 2 Mappings
    transformationMapping?: boolean;
    serializedObjectConverter?: boolean;
    typeConversionConverter?: boolean;
    objectTypeConverter?: boolean;
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
    // Focus mode properties
    isCutPoint?: boolean;
    isInCycle?: boolean;
    hasEagerRisk?: boolean;
    isPotentialVO?: boolean;
    focusOpacity?: number; // 0-1, controlled by focus mode
}

interface EntityNodeProps {
    data: EntityNodeData;
    selected?: boolean;
}

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

const EntityNode = ({ data, selected }: EntityNodeProps) => {
    const [isHovered, setIsHovered] = useState(false);
    const hoverTimeout = useRef<any>(null);

    const handleMouseEnter = () => {
        hoverTimeout.current = setTimeout(() => {
            setIsHovered(true);
        }, 400); // 400ms delay
    };

    const handleMouseLeave = () => {
        if (hoverTimeout.current) {
            clearTimeout(hoverTimeout.current);
            hoverTimeout.current = null;
        }
        setIsHovered(false);
    };

    const errorCount = data.violations.filter(v => v.severity === 'ERROR').length;
    const isRoot = data.dddRole === 'AGGREGATE_ROOT';
    const aggColor = getAggregateColor(data.aggregateName);
    const focusOpacity = data.focusOpacity ?? 1;
    const hasBadges = data.isCutPoint || data.isInCycle || data.hasEagerRisk || data.isPotentialVO;

    // Glow effect when node is highlighted in focus mode
    const glowStyle = focusOpacity === 1 && hasBadges ? {
        boxShadow: data.isInCycle ? '0 0 12px rgba(255, 0, 64, 0.4)' :
            data.isCutPoint ? '0 0 12px rgba(0, 133, 255, 0.4)' :
                data.hasEagerRisk ? '0 0 12px rgba(255, 170, 0, 0.4)' :
                    data.isPotentialVO ? '0 0 12px rgba(168, 85, 247, 0.5)' : 'none'
    } : {};

    // Virtual Node Styles (MappedSuperclass, Embeddable)
    const isVirtual = data.type === 'MAPPED_SUPERCLASS' || data.type === 'EMBEDDABLE';
    const virtualStyle = isVirtual ? {
        borderStyle: 'dashed',
        backgroundColor: 'rgba(255, 230, 0, 0.08)', // Transparent yellow
        opacity: focusOpacity,
    } : {};

    // Dashed outline for potential VOs
    const voStyle = data.isPotentialVO ? {
        outline: '2px dashed #A855F7',
        outlineOffset: '2px'
    } : {};

    return (
        <div className={`relative min-w-[100px] max-w-[100px] min-h-[50px] rounded bg-node border transition-all duration-300 shadow-sm overflow-hidden 
            ${selected ? 'border-primary ring-1 ring-primary/30 z-10' : 'border-subtle'}
            ${isRoot ? 'ring-1 ring-primary/20' : ''}`}
            style={{
                backgroundColor: 'var(--bg-node)',
                borderColor: selected ? 'var(--primary)' : 'var(--border-subtle)',
                opacity: focusOpacity,
                transition: 'opacity 0.3s ease, box-shadow 0.3s ease',
                ...virtualStyle,
                ...glowStyle,
                ...voStyle
            }}>

            {/* Analysis Badges - Bottom Center */}
            {hasBadges && (
                <div className="absolute -bottom-3 left-1/2 -translate-x-1/2 flex gap-0.5 z-20">
                    {data.isInCycle && (
                        <span title="Part of Circular Dependency"
                            className="px-1 py-0.5 rounded text-[5px] font-bold text-white shadow-md"
                            style={{ backgroundColor: 'var(--score-low)' }}>
                            🔄
                        </span>
                    )}
                    {data.isCutPoint && (
                        <span title="Cut Point - Can be decoupled"
                            className="px-1 py-0.5 rounded text-[5px] font-bold text-white shadow-md"
                            style={{ backgroundColor: 'var(--primary)' }}>
                            ✂️
                        </span>
                    )}
                    {data.hasEagerRisk && (
                        <span title="Eager Fetch Risk"
                            className="px-1 py-0.5 rounded text-[5px] font-bold text-white shadow-md"
                            style={{ backgroundColor: 'var(--score-med)' }}>
                            ⚠️
                        </span>
                    )}
                    {data.isPotentialVO && (
                        <span title="Potential Value Object"
                            className="px-1 py-0.5 rounded text-[5px] font-bold text-white shadow-md"
                            style={{ backgroundColor: 'var(--accent-purple)' }}>
                            💎
                        </span>
                    )}
                </div>
            )}

            {/* Aggregate Indicator Bar - Thinner */}
            <div className="h-0.5 w-full" style={{ backgroundColor: aggColor }}></div>

            {/* Header Section - Stacked and tight */}
            <div className={`px-1.5 py-1 border-b ${selected ? 'bg-panel/50 border-primary/20' : 'bg-panel/10 border-subtle'}`}
                style={{ backgroundColor: selected ? 'var(--bg-panel)' : 'transparent', borderColor: 'var(--border-subtle)' }}>
                <div className="flex flex-col min-w-0 leading-tight">
                    <div className="flex items-center gap-0.5">
                        {isRoot && <span title="Aggregate Root" className="text-[7px] shrink-0">💠</span>}
                        <span className="font-extrabold text-[9px] text-main truncate tracking-tight" style={{ color: 'var(--text-main)' }}>{data.name}</span>
                    </div>
                </div>
            </div>

            {/* Content Section - Minimalist */}
            <div className="px-1.5 py-1 flex flex-col gap-0.5">
                <div className="flex items-center gap-1">
                    <div className={`w-1 h-1 rounded-full ${data.type === 'EMBEDDABLE' ? 'bg-accent-purple' : 'bg-primary'}`} style={{ backgroundColor: data.type === 'EMBEDDABLE' ? 'var(--accent-purple)' : 'var(--primary)' }}></div>
                    <span className="text-[6px] uppercase font-bold text-main" style={{ color: 'var(--text-main)' }}>
                        {data.dddRole === 'AGGREGATE_ROOT' ? 'ROOT' : (
                            data.type === 'EMBEDDABLE' ? 'EMBED' :
                                data.type === 'MAPPED_SUPERCLASS' ? 'SUPER' : 'ENTITY'
                        )}
                    </span>
                </div>
                {errorCount > 0 && (
                    <div
                        className="flex items-center gap-1 cursor-help relative group/badge"
                        onMouseEnter={handleMouseEnter}
                        onMouseLeave={handleMouseLeave}
                    >
                        <div className="w-1 h-1 rounded-full bg-score-low" style={{ backgroundColor: 'var(--score-low)' }}></div>
                        <span className="text-[6px] font-bold text-score-low" style={{ color: 'var(--score-low)' }}>{errorCount} ISSUES</span>

                        {isHovered && data.violations && data.violations.length > 0 && (
                            <div className="absolute left-0 top-full mt-1 z-[1000] w-48 bg-glass backdrop-blur-xl border border-score-low/30 rounded-lg p-2 shadow-2xl animate-in fade-in zoom-in duration-200">
                                <div className="text-[7px] font-bold text-score-low mb-1 uppercase tracking-wider flex items-center gap-1">
                                    <div className="w-1 h-1 rounded-full bg-score-low animate-pulse"></div>
                                    Anomalies Detected
                                </div>
                                <div className="space-y-1.5 max-h-32 overflow-y-auto custom-scrollbar pr-1">
                                    {data.violations.map((v, idx) => (
                                        <div key={idx} className="flex gap-1.5 items-start">
                                            <div className={`mt-0.5 w-1 h-1 rounded-full shrink-0 ${v.severity === 'ERROR' ? 'bg-score-low' : 'bg-score-med'}`} style={{ backgroundColor: v.severity === 'ERROR' ? 'var(--score-low)' : 'var(--score-med)' }}></div>
                                            <div className="flex flex-col gap-0.5">
                                                <p className="text-[8px] text-text-primary leading-tight font-medium">{v.message}</p>
                                                <p className="text-[6px] text-text-secondary opacity-70 font-mono">{v.ruleId}</p>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                                <div className="mt-2 pt-1.5 border-t border-subtle flex justify-between items-center text-[6px] text-text-secondary">
                                    <span>{data.violations.length} total warnings</span>
                                    <span>Click for full report</span>
                                </div>
                            </div>
                        )}
                    </div>
                )}
            </div>

            {data.showAttributes && (
                <div className="space-y-0.5 mt-1 max-h-[140px] overflow-y-auto custom-scrollbar pr-0.5">
                    {Object.entries<AttributeMetadata>(data.attributes || {}).map(([name, attr]) => {
                        const isId = name.toLowerCase().includes('id') || attr.id;
                        return (
                            <div key={name} className="flex items-center justify-between gap-1 group">
                                <div className="flex items-center gap-1 overflow-hidden min-w-0">
                                    {isId ? (
                                        <div className="w-1 h-1 rounded-full bg-score-med shrink-0 shadow-[0_0_3px_var(--score-med)]" style={{ backgroundColor: 'var(--score-med)' }} title="ID"></div>
                                    ) : attr.transformationMapping ? (
                                        <div className="w-1 h-1 rounded-full bg-orange-500 shrink-0" style={{ backgroundColor: '#f97316' }} title="Transformation Mapping"></div>
                                    ) : attr.serializedObjectConverter ? (
                                        <div className="w-1 h-1 rounded-full bg-red-500 shrink-0" style={{ backgroundColor: '#ef4444' }} title="Serialized Object"></div>
                                    ) : attr.typeConversionConverter ? (
                                        <div className="w-1 h-1 rounded-full bg-blue-500 shrink-0" style={{ backgroundColor: '#3b82f6' }} title="Type Conversion"></div>
                                    ) : attr.objectTypeConverter ? (
                                        <div className="w-1 h-1 rounded-full bg-cyan-500 shrink-0" style={{ backgroundColor: '#06b6d4' }} title="Object Type Converter"></div>
                                    ) : (
                                        <div className="w-0.5 h-0.5 rounded-full bg-border-active shrink-0" style={{ backgroundColor: 'var(--border-active)' }}></div>
                                    )}
                                    <span className={`text-[9px] truncate min-w-0 ${isId ? 'text-main font-semibold' : 'text-secondary'}`} style={{ color: isId ? 'var(--text-main)' : 'var(--text-secondary)' }}>
                                        {name}
                                    </span>
                                </div>
                                <span className="text-[8px] font-mono text-primary/50 shrink-0" style={{ color: 'var(--primary)', opacity: 0.5 }}>
                                    {String(attr.javaType).split('.').pop()}
                                </span>
                            </div>
                        );
                    })}
                </div>
            )}

            {/* Connection Handles - More visible */}
            <Handle
                type="target"
                position={Position.Top}
                className="react-flow__handle"
            />
            <Handle
                type="source"
                position={Position.Bottom}
                className="react-flow__handle"
            />
        </div>
    );
};

export default memo(EntityNode);
