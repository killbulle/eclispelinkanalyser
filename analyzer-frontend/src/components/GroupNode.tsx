import { memo } from 'react';

interface GroupNodeData {
    label: string;
    aggregateName: string;
}

const GroupNode = ({ data }: { data: GroupNodeData }) => {
    return (
        <div className="group-node w-full h-full rounded-2xl border-2 border-dashed border-primary/20 bg-primary/5 pointer-events-none transition-all duration-500">
            <div className="absolute -top-6 left-2 px-3 py-0.5 rounded-full bg-primary/10 border border-primary/20 text-[10px] font-bold text-primary uppercase tracking-widest backdrop-blur-md">
                {data.label}
            </div>
        </div>
    );
};

export default memo(GroupNode);
