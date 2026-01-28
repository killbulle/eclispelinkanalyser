import { memo } from 'react';

interface GroupNodeData {
    label: string;
    aggregateName: string;
    color?: string;
    memberCount?: number;
}

const GroupNode = ({ data }: { data: GroupNodeData }) => {
    const baseColor = data.color || '#7000FF';

    return (
        <div
            className="group-node w-full h-full rounded-3xl pointer-events-none transition-all duration-500 relative"
            style={{
                background: `linear-gradient(135deg, ${baseColor}15 0%, ${baseColor}08 100%)`,
                border: `2px dashed ${baseColor}40`,
                boxShadow: `0 4px 24px ${baseColor}10, inset 0 0 40px ${baseColor}05`
            }}
        >
            {/* Aggregate Label Header */}
            <div
                className="absolute -top-4 left-4 px-4 py-1.5 rounded-full text-[11px] font-bold uppercase tracking-wider backdrop-blur-md flex items-center gap-2 shadow-lg"
                style={{
                    background: `linear-gradient(135deg, ${baseColor}30 0%, ${baseColor}20 100%)`,
                    color: baseColor,
                    border: `1px solid ${baseColor}40`
                }}
            >
                <span className="w-2 h-2 rounded-full" style={{ backgroundColor: baseColor }} />
                {data.label}
                {data.memberCount !== undefined && (
                    <span
                        className="ml-1 px-1.5 py-0.5 rounded-full text-[9px] font-mono"
                        style={{
                            backgroundColor: `${baseColor}20`,
                            color: baseColor
                        }}
                    >
                        {data.memberCount}
                    </span>
                )}
            </div>

            {/* Decorative Corner Elements */}
            <div
                className="absolute top-2 right-2 w-6 h-6 rounded-br-xl opacity-30"
                style={{
                    borderRight: `2px solid ${baseColor}`,
                    borderBottom: `2px solid ${baseColor}`
                }}
            />
            <div
                className="absolute bottom-2 left-2 w-6 h-6 rounded-tl-xl opacity-30"
                style={{
                    borderLeft: `2px solid ${baseColor}`,
                    borderTop: `2px solid ${baseColor}`
                }}
            />
        </div>
    );
};

export default memo(GroupNode);
