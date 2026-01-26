#!/usr/bin/env python3

with open('/home/marco/dev/eclispelinkanalyser/analyzer-frontend/src/App.tsx', 'r') as f:
    content = f.read()

old_block = '''                {activeTab === 'violations' && (
                  <div className="space-y-0">
                    <div className="px-4 py-2 bg-background-header/30 text-[10px] font-bold text-text-muted uppercase tracking-widest border-b border-border">
                      Rule Violations ({selectedNode.data.violations.length})
                    </div>
                    {selectedNode.data.violations.length > 0 ? (
                      <div className="jpa-grid">
                        {selectedNode.data.violations.map((v: Violation, idx: number) => (
                          <div key={idx} className={`jpa-row border-l-2 ${v.severity === 'ERROR' ? 'border-l-status-error bg-status-error/5' : v.severity === 'WARNING' ? 'border-l-status-warning bg-status-warning/5' : 'border-l-status-info bg-status-info/5'}`}>
                            <div className="jpa-label">{v.ruleId}</div>
                            <div className="jpa-value text-text-primary text-xs">{v.message}</div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="p-4 flex flex-col items-center gap-2 opacity-60">
                        <CheckCircle2 size={32} className="text-status-success/50" />
                        <span className="text-[11px] text-status-success">No rule violations detected</span>
                      </div>
                    )}
                  </div>
                )}'''

new_block = '''                {activeTab === 'violations' && (
                  <div className="space-y-0">
                    <div className="px-4 py-2 bg-background-header/30 text-[10px] font-bold text-text-muted uppercase tracking-widest border-b border-border">
                      Rule Violations ({selectedNode.data.violations.length})
                    </div>
                    {selectedNode.data.violations.length > 0 ? (
                      <>
                        <div className="flex items-center gap-1 px-4 py-2 border-b border-border bg-background-header/20">
                          <span className="text-[10px] text-text-muted font-medium mr-2">Filter:</span>
                          {['ERROR', 'WARNING', 'INFO'].map(severity => {
                            const isActive = violationFilters[severity];
                            return (
                              <button
                                key={severity}
                                onClick={() => setViolationFilters(prev => ({ ...prev, [severity]: !prev[severity] }))}
                                className={`text-[9px] px-2 py-0.5 rounded border ${isActive ? 
                                  (severity === 'ERROR' ? 'bg-status-error text-white border-status-error' :
                                   severity === 'WARNING' ? 'bg-status-warning text-white border-status-warning' :
                                   'bg-status-info text-white border-status-info') : 
                                  'bg-background-card text-text-muted border-border'}`}
                              >
                                {severity}
                              </button>
                            );
                          })}
                        </div>
                        <div className="jpa-grid">
                          {selectedNode.data.violations
                            .filter((v: Violation) => violationFilters[v.severity])
                            .map((v: Violation, idx: number) => (
                              <div key={idx} className={`jpa-row border-l-2 ${v.severity === 'ERROR' ? 'border-l-status-error bg-status-error/5' : v.severity === 'WARNING' ? 'border-l-status-warning bg-status-warning/5' : 'border-l-status-info bg-status-info/5'}`}>
                                <div className="jpa-label">{v.ruleId}</div>
                                <div className="jpa-value text-text-primary text-xs">{v.message}</div>
                              </div>
                            ))}
                        </div>
                      </>
                    ) : (
                      <div className="p-4 flex flex-col items-center gap-2 opacity-60">
                        <CheckCircle2 size={32} className="text-status-success/50" />
                        <span className="text-[11px] text-status-success">No rule violations detected</span>
                      </div>
                    )}
                  </div>
                )}'''

if old_block in content:
    content = content.replace(old_block, new_block)
    with open('/home/marco/dev/eclispelinkanalyser/analyzer-frontend/src/App.tsx', 'w') as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Old block not found")
    # Try with slightly different indentation
    # Read from file lines 654-675
    with open('/home/marco/dev/eclispelinkanalyser/analyzer-frontend/src/App.tsx', 'r') as f:
        lines = f.readlines()
    block_from_file = ''.join(lines[653:675])  # 0-indexed
    print("Actual block from file:")
    print(repr(block_from_file[:200]))