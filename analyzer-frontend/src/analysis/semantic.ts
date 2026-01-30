/**
 * Semantic Analysis Module
 * 
 * Uses naming conventions and contextual rules to identify DDD Stereotypes.
 */

export const SemanticProfile = {
    GENERIC: 'GENERIC',
    SPRING_BOOT: 'SPRING_BOOT',
    QUARKUS: 'QUARKUS',
    MICRONAUT: 'MICRONAUT',
    ECOMMERCE_OFBIZ: 'ECOMMERCE_OFBIZ',
    TREASURY_ISO20022: 'TREASURY_ISO20022'
} as const;

export type SemanticProfileType = typeof SemanticProfile[keyof typeof SemanticProfile];

export const Stereotype = {
    ENTITY: 'ENTITY',
    VALUE_OBJECT: 'VALUE_OBJECT',
    AGGREGATE_ROOT: 'AGGREGATE_ROOT',
    REPOSITORY: 'REPOSITORY',
    SERVICE: 'SERVICE',
    EVENT: 'EVENT',
    ROLE: 'ROLE',
    RESOURCE: 'RESOURCE',
    UNKNOWN: 'UNKNOWN'
} as const;

export type StereotypeType = typeof Stereotype[keyof typeof Stereotype];

export interface SemanticMatch {
    concept: string;
    stereotype: StereotypeType;
    confidence: number;
    ontologySource?: string;
}

/**
 * Basic semantic analyzer using keywords
 */
export const analyzeSemantics = (name: string, _profile: SemanticProfileType = "GENERIC"): SemanticMatch => {
    // profile is reserved for future ontology-specific rules
    const _p = _profile; // Use it to satisfy lint if it's configured strictly
    const n = name.toLowerCase();

    // Value Object Keywords
    const voKeywords = ['value', 'embed', 'type', 'money', 'address', 'period', 'range', 'amount', 'metadata', 'description', 'status', 'info'];
    if (voKeywords.some(k => n.includes(k))) {
        return { concept: 'VO Candidate', stereotype: Stereotype.VALUE_OBJECT, confidence: 0.8, ontologySource: 'Keyword' };
    }

    // Aggregate Root Keywords
    const rootKeywords = ['header', 'parent', 'master', 'root', 'order', 'invoice', 'customer', 'product', 'facility', 'account', 'contract', 'agreement', 'case', 'session'];
    if (rootKeywords.some(k => n.includes(k))) {
        return { concept: 'Root Candidate', stereotype: Stereotype.AGGREGATE_ROOT, confidence: 0.7, ontologySource: 'Keyword' };
    }

    // Service/Repository Keywords (In JPA world, these shouldn't be here, but just in case)
    if (n.endsWith('service')) return { concept: 'Service', stereotype: Stereotype.SERVICE, confidence: 1, ontologySource: 'Suffix' };
    if (n.endsWith('repository') || n.endsWith('dao')) return { concept: 'Repo', stereotype: Stereotype.REPOSITORY, confidence: 1, ontologySource: 'Suffix' };

    return { concept: 'Generic Entity', stereotype: Stereotype.ENTITY, confidence: 0.5 };
};

export const getAggregateForNode = (_name: string) => {
    const _n = _name;
    // Simple mock logic for node matching
    return "Default";
};
