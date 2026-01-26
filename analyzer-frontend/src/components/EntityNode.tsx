import { memo } from 'react';
import { Handle, Position } from 'reactflow';
import { ShieldAlert } from 'lucide-react';

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
    const errorCount = data.violations.filter(v => v.severity === 'ERROR').length;
    const isRoot = data.dddRole === 'AGGREGATE_ROOT';
    const aggColor = getAggregateColor(data.aggregateName);

    return (
        <div className={`relative min-w-[100px] max-w-[100px] min-h-[50px] rounded bg-node border transition-all duration-300 shadow-sm overflow-hidden 
            ${selected ? 'border-primary ring-1 ring-primary/30 z-10' : 'border-subtle'}
            ${isRoot ? 'ring-1 ring-primary/20' : ''}`}
            style={{ backgroundColor: 'var(--bg-node)', borderColor: selected ? 'var(--primary)' : 'var(--border-subtle)' }}>

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
                        {data.dddRole === 'AGGREGATE_ROOT' ? 'ROOT' : (data.type === 'EMBEDDABLE' ? 'VALUE' : 'ENTITY')}
                    </span>
                </div>
                {errorCount > 0 && (
                    <div className="flex items-center gap-1">
                        <div className="w-1 h-1 rounded-full bg-score-low" style={{ backgroundColor: 'var(--score-low)' }}></div>
                        <span className="text-[6px] font-bold text-score-low" style={{ color: 'var(--score-low)' }}>{errorCount} ISSUES</span>
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
                                        <div className="w-1 h-1 rounded-full bg-score-med shrink-0 shadow-[0_0_3px_var(--score-med)]" style={{ backgroundColor: 'var(--score-med)' }}></div>
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
