import React, { useMemo } from 'react';
import { Zap } from 'lucide-react';

interface CacheViewProps {
    entities: any[];
}

const CACHE_CONFIG_COLORS: Record<string, string> = {
    FULL: '#ef4444',
    WEAK: '#3b82f6',
    SOFT: '#10b981',
    SOFT_WEAK: '#8b5cf6',
    HARD_WEAK: '#f59e0b',
    NONE: '#6b7280',
};

const getCacheTypeColor = (type?: string) => {
    if (!type) return '#6b7280';
    return CACHE_CONFIG_COLORS[type] || '#6b7280';
};

const formatExpiry = (expiryMs?: number) => {
    if (!expiryMs) return 'No expiry';
    if (expiryMs < 60000) return `${Math.floor(expiryMs / 1000)}s`;
    if (expiryMs < 3600000) return `${Math.floor(expiryMs / 60000)}m`;
    return `${Math.floor(expiryMs / 3600000)}h`;
};

export const CacheView: React.FC<CacheViewProps> = ({ entities }) => {
    const cacheEntities = useMemo(() => {
        return entities.filter(e => e.cacheType || e.cacheSize || e.cacheExpiry);
    }, [entities]);

    if (cacheEntities.length === 0) {
        return (
            <div className="flex flex-col items-center justify-center h-96 text-muted">
                <Zap className="w-12 h-12 mb-4 opacity-20" />
                <p className="text-sm font-medium">No cache configurations detected in this model</p>
                <p className="text-xs mt-2 opacity-50 text-center max-w-xs">
                    Verify that your entities are annotated with @Cache or @Cacheable and that the analyzer backend has processed them.
                </p>
            </div>
        );
    }

    const stats = {
        total: cacheEntities.length,
        shared: cacheEntities.filter(e => e.cacheIsolation === 'SHARED' || !e.cacheIsolation).length,
        isolated: cacheEntities.filter(e => e.cacheIsolation === 'ISOLATED').length,
        coordinated: cacheEntities.filter(e => e.cacheCoordinationType && e.cacheCoordinationType !== 'NONE').length,
        alwaysRefresh: cacheEntities.filter(e => e.cacheAlwaysRefresh).length,
        disableHits: cacheEntities.filter(e => e.cacheDisableHits).length,
    };

    return (
        <div className="p-6 h-full overflow-auto custom-scrollbar bg-body">
            <div className="flex items-center justify-between mb-6">
                <div>
                    <h3 className="text-xl font-bold text-main flex items-center gap-2">
                        <Zap className="text-primary" size={20} />
                        EclipseLink Cache Analysis
                    </h3>
                    <p className="text-sm text-secondary mt-1">Detailed view of L2 cache configurations across all entities</p>
                </div>
                <div className="flex gap-4">
                    <div className="flex flex-col items-end">
                        <span className="text-[10px] uppercase font-bold text-muted">Tracked Entities</span>
                        <span className="text-lg font-bold text-primary">{stats.total}</span>
                    </div>
                    <div className="w-px h-8 bg-subtle" />
                    <div className="flex flex-col items-end">
                        <span className="text-[10px] uppercase font-bold text-muted">Coordinated Caches</span>
                        <span className="text-lg font-bold text-primary">{stats.coordinated}</span>
                    </div>
                </div>
            </div>

            <div className="grid grid-cols-4 gap-4 mb-8">
                <div className="bg-panel p-4 rounded-xl border border-subtle shadow-sm">
                    <div className="text-[10px] font-bold text-muted uppercase tracking-wider mb-2">SHARED (Global)</div>
                    <div className="flex items-baseline gap-2">
                        <div className="text-2xl font-bold text-main">{stats.shared}</div>
                        <div className="text-[10px] text-green-500 font-bold">Recommended</div>
                    </div>
                </div>
                <div className="bg-panel p-4 rounded-xl border border-subtle shadow-sm">
                    <div className="text-[10px] font-bold text-muted uppercase tracking-wider mb-2">ISOLATED</div>
                    <div className="flex items-baseline gap-2">
                        <div className="text-2xl font-bold text-main">{stats.isolated}</div>
                        <div className="text-[10px] text-score-low font-bold">L1 Only</div>
                    </div>
                </div>
                <div className="bg-panel p-4 rounded-xl border border-subtle shadow-sm col-span-2">
                    <div className="text-[10px] font-bold text-muted uppercase tracking-wider mb-2">Cache Strategy Advice</div>
                    <p className="text-[11px] text-secondary">
                        Entities with high read/write ratios should use <span className="text-green-500 font-bold">SOFT</span> or <span className="text-primary font-bold">WEAK</span> cache types.
                        Cluster environments <span className="text-score-med font-bold">must</span> enable Cache Coordination.
                    </p>
                </div>
            </div>

            <div className="bg-panel p-4 rounded-xl border border-subtle shadow-sm mb-8">
                <div className="text-[10px] font-bold text-muted uppercase tracking-wider mb-3">Cache Behavior Flags</div>
                <div className="flex gap-8">
                    <div className="flex items-center gap-3">
                        <div className={`text-xl font-bold ${stats.alwaysRefresh > 0 ? 'text-score-med' : 'text-secondary'}`}>{stats.alwaysRefresh}</div>
                        <div className="flex flex-col">
                            <span className="text-[11px] font-bold text-main">Always Refresh</span>
                            <span className="text-[10px] text-secondary">Force DB Refetch</span>
                        </div>
                    </div>
                    <div className="w-px h-8 bg-subtle"></div>
                    <div className="flex items-center gap-3">
                        <div className={`text-xl font-bold ${stats.disableHits > 0 ? 'text-score-low' : 'text-secondary'}`}>{stats.disableHits}</div>
                        <div className="flex flex-col">
                            <span className="text-[11px] font-bold text-main">Disable Hits</span>
                            <span className="text-[10px] text-secondary">Ignore Cache Hits</span>
                        </div>
                    </div>
                </div>
            </div>

            <div className="bg-panel rounded-xl border border-subtle shadow-sm overflow-hidden">
                <table className="w-full text-left border-collapse">
                    <thead>
                        <tr className="bg-panel-hover border-b border-subtle">
                            <th className="px-4 py-3 text-[10px] font-bold text-muted uppercase tracking-wider">Entity Name</th>
                            <th className="px-4 py-3 text-[10px] font-bold text-muted uppercase tracking-wider">Cache Type</th>
                            <th className="px-4 py-3 text-[10px] font-bold text-muted uppercase tracking-wider">Size</th>
                            <th className="px-4 py-3 text-[10px] font-bold text-muted uppercase tracking-wider">Expiry</th>
                            <th className="px-4 py-3 text-[10px] font-bold text-muted uppercase tracking-wider">Isolation</th>
                            <th className="px-4 py-3 text-[10px] font-bold text-muted uppercase tracking-wider">Coordination</th>
                            <th className="px-4 py-3 text-[10px] font-bold text-muted uppercase tracking-wider">Always Refresh</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-subtle">
                        {cacheEntities.map((entity, idx) => (
                            <tr key={idx} className="hover:bg-panel-hover transition-colors group">
                                <td className="px-4 py-3 text-sm font-semibold text-main">{entity.name}</td>
                                <td className="px-4 py-3 text-sm">
                                    <span
                                        className="px-2 py-0.5 rounded-full text-[10px] font-bold text-white shadow-sm"
                                        style={{ backgroundColor: getCacheTypeColor(entity.cacheType) }}
                                    >
                                        {entity.cacheType || 'N/A'}
                                    </span>
                                </td>
                                <td className="px-4 py-3 text-sm font-mono text-secondary">{entity.cacheSize || '-'}</td>
                                <td className="px-4 py-3 text-sm font-mono text-secondary">{formatExpiry(entity.cacheExpiry)}</td>
                                <td className="px-4 py-3 text-sm">
                                    <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${entity.cacheIsolation === 'ISOLATED' ? 'bg-red-500/10 text-red-500' :
                                        entity.cacheIsolation === 'PROTECTED' ? 'bg-yellow-500/10 text-yellow-500' :
                                            'bg-green-500/10 text-green-500'
                                        }`}>
                                        {entity.cacheIsolation || 'SHARED'}
                                    </span>
                                </td>
                                <td className="px-4 py-3 text-xs text-secondary italic">
                                    {entity.cacheCoordinationType || 'NONE'}
                                </td>
                                <td className="px-4 py-3 text-sm">
                                    {entity.cacheAlwaysRefresh ? (
                                        <span className="text-score-low font-bold">Yes</span>
                                    ) : (
                                        <span className="text-green-500 font-bold">No</span>
                                    )}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};
