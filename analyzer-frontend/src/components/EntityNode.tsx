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
    dddRoleConfidence?: number;
    aggregateName?: string;
    aggregateNameConfidence?: number;
    cutPointScore?: number;
    cutPointNormalized?: number;
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
        }, 300);
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

    // Entity type labels with specific colors
    const typeLabel = data.type === 'EMBEDDABLE' ? 'EMBED' :
        data.type === 'MAPPED_SUPERCLASS' ? 'SUPER' :
            data.dddRole === 'AGGREGATE_ROOT' ? 'ROOT' : 'ENTITY';

    const typeColor = data.type === 'EMBEDDABLE' ? 'var(--accent-purple)' :
        data.type === 'MAPPED_SUPERCLASS' ? 'var(--score-med)' : 'var(--primary)';

    return (
        <div className={`flex flex-col bg-node border transition-opacity duration-300 overflow-hidden
            ${selected ? 'border-primary ring-[0.5px] ring-primary shadow-[0_0_10px_rgba(82,102,255,0.2)]' : 'border-subtle shadow-surgical'}`}
            style={{
                width: '180px',
                borderRadius: 'var(--border-radius-sm)',
                opacity: focusOpacity,
                backgroundColor: 'var(--bg-node)',
                borderColor: selected ? 'var(--primary)' : 'var(--border-subtle)',
            }}>

            {/* Top Indicator Line (Aggregate) */}
            <div className="h-[2px] w-full" style={{ backgroundColor: aggColor }}></div>

            {/* Header Section */}
            <div className="px-2 py-1.5 border-b border-subtle bg-panel/30 flex items-center justify-between gap-2"
                style={{ borderColor: 'var(--border-subtle)' }}>
                <div className="flex items-center gap-1.5 min-w-0">
                    {isRoot && <span className="text-[8px] leading-none shrink-0" title="Aggregate Root">💠</span>}
                    <span className="font-bold text-[11px] text-main truncate tracking-tight">{data.name}</span>
                </div>
                <span className="text-[7px] font-black px-1 py-0.5 rounded-[1px] shrink-0 border border-current opacity-80"
                    style={{ color: typeColor }}>
                    {typeLabel}
                </span>
            </div>

            {/* Package Label */}
            <div className="px-2 py-0.5 bg-black/20 border-b border-subtle" style={{ borderColor: 'var(--border-subtle)' }}>
                <span className="text-[8px] font-mono text-muted truncate block">
                    {data.packageName || 'default.package'}
                </span>
            </div>

            {/* Attributes List */}
            <div className={`p-2 space-y-0.5 ${data.showAttributes ? 'max-h-[200px]' : 'max-h-[60px]'} overflow-hidden`}>
                {Object.entries<AttributeMetadata>(data.attributes || {}).map(([name, attr]) => {
                    const isId = name.toLowerCase().includes('id') || attr.id;
                    return (
                        <div key={name} className="flex items-center justify-between gap-2 group">
                            <div className="flex items-center gap-1.5 min-w-0">
                                <div className={`w-1 h-1 shrink-0 ${isId ? 'bg-primary' : 'bg-muted/40'}`}
                                    style={{
                                        backgroundColor: isId ? 'var(--primary)' : 'var(--text-muted)',
                                        opacity: isId ? 1 : 0.4,
                                        borderRadius: '1px'
                                    }}></div>
                                <span className={`text-[10px] truncate ${isId ? 'text-main font-semibold' : 'text-secondary'}`}>
                                    {name}
                                </span>
                            </div>
                            <span className="text-[8px] font-mono text-muted/60 shrink-0">
                                {String(attr.javaType).split('.').pop()}
                            </span>
                        </div>
                    );
                })}
                {!data.showAttributes && Object.keys(data.attributes || {}).length > 3 && (
                    <div className="text-[8px] text-muted italic pt-0.5 opacity-50">
                        + {Object.keys(data.attributes).length - 3} more...
                    </div>
                )}
            </div>

            {/* Issues Badge (If any) */}
            {errorCount > 0 && (
                <div className="mt-auto px-2 py-1 bg-score-low/10 border-t border-score-low/20 flex items-center justify-between cursor-help"
                    onMouseEnter={handleMouseEnter}
                    onMouseLeave={handleMouseLeave}>
                    <div className="flex items-center gap-1.5">
                        <div className="w-1 h-1 rounded-full bg-score-low animate-pulse"></div>
                        <span className="text-[8px] font-bold text-score-low uppercase tracking-wider">{errorCount} Anomalies</span>
                    </div>

                    {isHovered && (
                        <div className="absolute left-full top-0 ml-2 w-56 bg-panel border border-score-low/30 shadow-xl z-50 p-2 pointer-events-none">
                            <div className="text-[8px] font-black text-score-low mb-1.5 uppercase tracking-widest border-b border-score-low/10 pb-1">
                                Critical Validation Failures
                            </div>
                            <div className="space-y-1.5">
                                {data.violations.map((v, i) => (
                                    <div key={i} className="flex gap-2">
                                        <div className="text-[8px] leading-tight text-main font-mono">
                                            <span className="text-score-low font-bold">[{v.severity}]</span> {v.message}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}
                </div>
            )}

            {/* Connection Handles */}
            <Handle type="target" position={Position.Top} className="!w-2 !h-2 !bg-body !border-[1.5px] !border-primary" style={{ top: '-1px' }} />
            <Handle type="source" position={Position.Bottom} className="!w-2 !h-2 !bg-body !border-[1.5px] !border-primary" style={{ bottom: '-1px' }} />
        </div>
    );
};

export default memo(EntityNode);
