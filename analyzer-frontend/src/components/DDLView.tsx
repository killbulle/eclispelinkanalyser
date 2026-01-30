import React, { useState, useMemo } from 'react';
import { Copy, Download, Database } from 'lucide-react';

interface DDLViewProps {
    entities: any[];
    nativeDdl?: string | null;
}

export const DDLView: React.FC<DDLViewProps> = ({ entities, nativeDdl }) => {
    const [copySuccess, setCopySuccess] = useState(false);

    const sql = useMemo(() => {
        if (nativeDdl) return nativeDdl;

        if (!entities || entities.length === 0) return "-- No entities detected in this report";

        let ddl = `-- Generated SQL DDL for ${entities.length} entities\n`;
        ddl += `-- EclipseLink Analyzer Output\n\n`;

        // Pass 1: CREATE TABLE
        entities.forEach(node => {
            if (node.type === 'MAPPED_SUPERCLASS' || node.type === 'EMBEDDABLE') {
                ddl += `-- Skipping ${node.type}: ${node.name}\n`;
                return;
            }

            ddl += `CREATE TABLE ${(node.name || 'UNKNOWN').toUpperCase()} (\n`;
            const attrs = Object.values(node.attributes || {});
            attrs.forEach((attr: any, idx) => {
                const colName = (attr.columnName || attr.name || 'UNKNOWN').toUpperCase();
                const dbType = (attr.databaseType || 'VARCHAR').toUpperCase();
                ddl += `  ${colName} ${dbType}`;
                if (idx < attrs.length - 1) ddl += ',';
                ddl += '\n';
            });
            ddl += `);\n\n`;
        });

        // Pass 2: ALTER TABLE (Foreign Keys)
        entities.forEach(node => {
            if (!node.relationships) return;
            node.relationships.forEach((rel: any) => {
                if (rel.owningSide && rel.mappingType && rel.mappingType.includes('ToOne')) {
                    const tableName = (node.name || 'UNKNOWN').toUpperCase();
                    const targetTable = (rel.targetEntity || 'UNKNOWN').toUpperCase();
                    const fkName = `FK_${tableName}_${targetTable}`;
                    const colName = (rel.attributeName || 'UNKNOWN').toUpperCase();
                    ddl += `ALTER TABLE ${tableName} ADD CONSTRAINT ${fkName} `;
                    ddl += `FOREIGN KEY (${colName}_ID) REFERENCES ${targetTable}(ID);\n`;
                }
            });
        });

        return ddl;
    }, [entities, nativeDdl]);

    const copyToClipboard = () => {
        navigator.clipboard.writeText(sql);
        setCopySuccess(true);
        setTimeout(() => setCopySuccess(false), 2000);
    };

    const downloadSql = () => {
        const element = document.createElement("a");
        const file = new Blob([sql], { type: 'text/plain' });
        element.href = URL.createObjectURL(file);
        element.download = "schema.sql";
        document.body.appendChild(element);
        element.click();
    };

    return (
        <div className="flex flex-col h-full bg-[#0d1117] text-gray-300 overflow-hidden">
            <div className="flex items-center justify-between p-4 border-b border-gray-800 bg-[#161b22] shrink-0">
                <div className="flex items-center gap-3">
                    <Database className="w-5 h-5 text-blue-400" />
                    <h2 className="text-lg font-semibold text-white">SQL DDL Generation</h2>
                    {nativeDdl && (
                        <span className="px-2 py-0.5 text-xs font-medium bg-green-900/30 text-green-400 border border-green-800/50 rounded-full">
                            Native EclipseLink Output
                        </span>
                    )}
                </div>
                <div className="flex gap-2">
                    <button
                        onClick={copyToClipboard}
                        className="flex items-center gap-2 px-3 py-1.5 text-sm font-medium bg-gray-800 hover:bg-gray-700 text-white rounded-md transition-colors"
                    >
                        {copySuccess ? 'Copied!' : (
                            <>
                                <Copy className="w-4 h-4" />
                                Copy SQL
                            </>
                        )}
                    </button>
                    <button
                        onClick={downloadSql}
                        className="flex items-center gap-2 px-3 py-1.5 text-sm font-medium bg-blue-600 hover:bg-blue-500 text-white rounded-md transition-colors"
                    >
                        <Download className="w-4 h-4" />
                        Download .sql
                    </button>
                </div>
            </div>

            <div className="flex-1 overflow-auto p-6 font-mono text-sm leading-relaxed custom-scrollbar">
                <pre className="text-blue-300">
                    <code>{sql}</code>
                </pre>
            </div>
        </div>
    );
};
