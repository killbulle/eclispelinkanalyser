import React, { useMemo } from 'react';
import { GitGraph, Database } from 'lucide-react';

interface RelationshipRow {
  sourceEntity: string;
  targetEntity: string;
  attributeName: string;
  mappingType: string;
  lazy: boolean;
  cascadePersist: boolean;
  cascadeRemove: boolean;
  cascadeAll: boolean;
  owningSide: boolean;
  mappedBy?: string;
  optional?: boolean;
  batchFetchType?: string;
}

interface TableViewProps {
  relationships: RelationshipRow[];
  onRowClick?: (row: RelationshipRow) => void;
}

export const TableView: React.FC<TableViewProps> = ({ relationships, onRowClick }) => {
  const groupedRelationships = useMemo(() => {
    const groups = new Map<string, RelationshipRow[]>();
    relationships.forEach(rel => {
      if (!groups.has(rel.sourceEntity)) {
        groups.set(rel.sourceEntity, []);
      }
      groups.get(rel.sourceEntity)?.push(rel);
    });
    return Array.from(groups.entries()).sort(([a, b]) => {
      return String(a[0]).localeCompare(String(b[0]));
    });
  }, [relationships]);

  const getMappingTypeBadge = (mappingType: string) => {
    const colors: Record<string, string> = {
      'OneToOne': 'bg-blue-100 text-blue-700',
      'OneToMany': 'bg-green-100 text-green-700',
      'ManyToOne': 'bg-purple-100 text-purple-700',
      'ManyToMany': 'bg-orange-100 text-orange-700',
      'Embedded': 'bg-gray-100 text-gray-700',
      'ElementCollection': 'bg-pink-100 text-pink-700',
    };
    return colors[mappingType] || 'bg-gray-100 text-gray-700';
  };

  const getCascadeStatus = (rel: RelationshipRow) => {
    if (rel.cascadeAll) return 'ALL';
    if (rel.cascadePersist && rel.cascadeRemove) return 'P+R';
    if (rel.cascadePersist) return 'P';
    if (rel.cascadeRemove) return 'R';
    return '';
  };

  if (relationships.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center h-96 text-muted">
        <GitGraph className="w-12 h-12" />
        <p className="text-sm font-medium">No relationships found</p>
      </div>
    );
  }

  const stats = {
    oneToOne: relationships.filter(r => r.mappingType.includes('OneToOne')).length,
    oneToMany: relationships.filter(r => r.mappingType.includes('OneToMany')).length,
    manyToOne: relationships.filter(r => r.mappingType.includes('ManyToOne')).length,
    manyToMany: relationships.filter(r => r.mappingType.includes('ManyToMany')).length
  };

  return (
    <div className="p-4 h-full overflow-auto custom-scrollbar">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-bold text-main">Relationships Data View</h3>
        <div className="flex items-center gap-2 text-sm text-muted">
          <Database className="w-4 h-4" />
          <span>{relationships.length} relationships</span>
        </div>
      </div>

      <div className="grid grid-cols-4 gap-4 mb-4">
        <div className="bg-panel/50 border border-subtle rounded p-3">
          <div className="text-xs text-muted mb-1 uppercase tracking-wider">OneToOne</div>
          <div className="text-2xl font-bold text-primary">{stats.oneToOne}</div>
        </div>
        <div className="bg-panel/50 border border-subtle rounded p-3">
          <div className="text-xs text-muted mb-1 uppercase tracking-wider">OneToMany</div>
          <div className="text-2xl font-bold text-primary">{stats.oneToMany}</div>
        </div>
        <div className="bg-panel/50 border border-subtle rounded p-3">
          <div className="text-xs text-muted mb-1 uppercase tracking-wider">ManyToOne</div>
          <div className="text-2xl font-bold text-primary">{stats.manyToOne}</div>
        </div>
        <div className="bg-panel/50 border border-subtle rounded p-3">
          <div className="text-xs text-muted mb-1 uppercase tracking-wider">ManyToMany</div>
          <div className="text-2xl font-bold text-primary">{stats.manyToMany}</div>
        </div>
      </div>

      <div className="overflow-x-auto border border-subtle rounded-lg">
        <table className="min-w-full divide-y divide-subtle text-sm">
          <thead className="bg-panel/50">
            <tr>
              <th className="px-3 py-2 text-left text-xs text-muted uppercase tracking-wider font-semibold">Source</th>
              <th className="px-3 py-2 text-left text-xs text-muted uppercase tracking-wider font-semibold">Target</th>
              <th className="px-3 py-2 text-left text-xs text-muted uppercase tracking-wider font-semibold">Attribute</th>
              <th className="px-3 py-2 text-center text-xs text-muted uppercase tracking-wider font-semibold">Type</th>
              <th className="px-3 py-2 text-center text-xs text-muted uppercase tracking-wider font-semibold">Lazy</th>
              <th className="px-3 py-2 text-center text-xs text-muted uppercase tracking-wider font-semibold">Cascade</th>
              <th className="px-3 py-2 text-center text-xs text-muted uppercase tracking-wider font-semibold">Owns</th>
              <th className="px-3 py-2 text-left text-xs text-muted uppercase tracking-wider font-semibold">Mapped By</th>
            </tr>
          </thead>
          <tbody>
            {groupedRelationships.map(([sourceEntity, rels]) => (
              <React.Fragment key={sourceEntity}>
                <tr>
                  <td className="px-3 py-2 font-semibold text-primary border-b border-subtle/50" rowSpan={rels.length}>
                    {sourceEntity}
                  </td>
                </tr>
                {rels.map((rel, idx) => (
                  <tr
                    key={`${sourceEntity}-${idx}`}
                    onClick={() => onRowClick && onRowClick(rel)}
                    className="hover:bg-panel/50 cursor-pointer transition-colors"
                  >
                    <td className="px-3 py-2 text-secondary">{rel.targetEntity}</td>
                    <td className="px-3 py-2 text-secondary">{rel.attributeName}</td>
                    <td className="px-3 py-2">
                      <span className={`px-2 py-1 rounded text-[10px] font-medium ${getMappingTypeBadge(rel.mappingType)}`}>
                        {rel.mappingType}
                      </span>
                    </td>
                    <td className="px-3 py-2 text-center">
                      {rel.lazy ? (
                        <span className="text-red-600">No</span>
                      ) : (
                        <span className="text-green-600">Yes</span>
                      )}
                    </td>
                    <td className="px-3 py-2 text-center">
                      {getCascadeStatus(rel)}
                    </td>
                    <td className="px-3 py-2 text-center">
                      {rel.owningSide ? (
                        <span className="text-green-600">Yes</span>
                      ) : (
                        <span className="text-red-600">No</span>
                      )}
                    </td>
                    <td className="px-3 py-2 text-muted">
                      {rel.mappedBy || '-'}
                    </td>
                  </tr>
                ))}
              </React.Fragment>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};
