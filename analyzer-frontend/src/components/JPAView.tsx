import React, { useMemo } from 'react';
import { Database } from 'lucide-react';

interface TableMetadata {
  tableName: string;
  columns: ColumnMetadata[];
}

interface ColumnMetadata {
  columnName: string;
  typeName: string;
  size: number;
  isNullable: boolean;
}

interface EntityTableRelation {
  sourceEntity: string;
  targetEntity: string;
  attributeName: string;
  sourceColumn?: string;
  targetColumn?: string;
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

interface JPAViewProps {
  entities: any[];
  onRowClick?: (row: EntityTableRelation) => void;
}

export const JPAView: React.FC<JPAViewProps> = ({ entities, onRowClick }) => {
  const relationships = useMemo(() => {
    const rels: EntityTableRelation[] = [];
    entities.forEach(entity => {
      const entityName = entity.name || '';
      if (entity.relationships && Array.isArray(entity.relationships)) {
        entity.relationships.forEach((rel: any) => {
          if (!rel.targetEntity) return;

          rels.push({
            sourceEntity: entityName,
            targetEntity: rel.targetEntity,
            attributeName: rel.attributeName,
            mappingType: rel.mappingType || rel.attributeName,
            lazy: !!rel.lazy,
            cascadePersist: !!rel.cascadePersist,
            cascadeRemove: !!rel.cascadeRemove,
            cascadeAll: !!rel.cascadeAll,
            owningSide: !!rel.owningSide,
            mappedBy: rel.mappedBy,
            optional: !!rel.optional,
            batchFetchType: rel.batchFetchType,
          });
        });
      }
    });
    return rels;
  }, [entities]);

  if (relationships.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center h-96 text-muted">
        <Database className="w-12 h-12" />
        <p className="text-sm font-medium">No JPA relationships found</p>
      </div>
    );
  }


  const stats = {
    oneToOne: relationships.filter(r => r.mappingType.includes('OneToOne')).length,
    oneToMany: relationships.filter(r => r.mappingType.includes('OneToMany')).length,
    manyToOne: relationships.filter(r => r.mappingType.includes('ManyToOne')).length,
    manyToMany: relationships.filter(r => r.mappingType.includes('ManyToMany')).length,
    embedded: relationships.filter(r => r.mappingType === 'Embedded' || r.mappingType === 'ElementCollection').length
  };

  return (
    <div className="p-4 h-full overflow-auto custom-scrollbar">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-bold text-main">JPA Schema Analysis</h3>
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
        <div className="bg-panel/50 border border-subtle rounded p-3">
          <div className="text-xs text-muted mb-1 uppercase tracking-wider">Embedded</div>
          <div className="text-2xl font-bold text-primary">{stats.embedded}</div>
        </div>
      </div>

      <div className="overflow-x-auto border border-subtle rounded-lg">
        <table className="min-w-full divide-y divide-subtle text-sm">
          <thead className="bg-panel/50">
            <tr>
              <th className="px-3 py-2 text-left text-xs text-muted uppercase tracking-wider">Entity</th>
              <th className="px-3 py-2 text-left text-xs text-muted uppercase tracking-wider">Related Entity</th>
              <th className="px-3 py-2 text-left text-xs text-muted uppercase tracking-wider">Relationship</th>
              <th className="px-3 py-2 text-center text-xs text-muted uppercase tracking-wider">Type</th>
              <th className="px-3 py-2 text-center text-xs text-muted uppercase tracking-wider">Lazy</th>
              <th className="px-3 py-2 text-center text-xs text-muted uppercase tracking-wider">Cascade</th>
              <th className="px-3 py-2 text-center text-xs text-muted uppercase tracking-wider">Owner</th>
            </tr>
          </thead>
          <tbody>
            {relationships.map((rel, idx) => (
              <tr
                key={idx}
                className="hover:bg-panel/50 cursor-pointer transition-colors"
                onClick={() => onRowClick && onRowClick(rel)}
              >
                <td className="px-3 py-2 text-secondary">{rel.sourceEntity}</td>
                <td className="px-3 py-2 text-secondary">{rel.targetEntity}</td>
                <td className="px-3 py-2 text-secondary">{rel.attributeName}</td>
                <td className="px-3 py-2">
                  <span className="text-xs font-medium">{rel.mappingType}</span>
                </td>
                <td className="px-3 py-2 text-center">
                  {rel.lazy ? (
                    <span className="text-red-600">No</span>
                  ) : (
                    <span className="text-green-600">Yes</span>
                  )}
                </td>
                <td className="px-3 py-2 text-center">
                  {rel.cascadeAll ? 'ALL' : rel.cascadePersist && rel.cascadeRemove ? 'P+R' : rel.cascadePersist ? 'P' : rel.cascadeRemove ? 'R' : ''}
                </td>
                <td className="px-3 py-2 text-center">
                  {rel.owningSide ? (
                    <span className="text-green-600">Yes</span>
                  ) : (
                    <span className="text-red-600">No</span>
                  )}
                </td>
                <td className="px-3 py-2 text-center text-muted">
                  {rel.mappedBy || '-'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};
