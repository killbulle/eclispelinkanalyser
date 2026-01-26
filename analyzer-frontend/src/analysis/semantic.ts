/**
 * Semantic Analysis Module
 * Matches entity names and structure against Domain Ontologies (OFBiz, Treasury/ISO20022).
 */

import { get } from 'fast-levenshtein';

export enum SemanticProfile {
    GENERIC = 'GENERIC',
    ECOMMERCE_OFBIZ = 'ECOMMERCE_OFBIZ',
    TREASURY_ISO20022 = 'TREASURY_ISO20022',
}

export enum Stereotype {
    ENTITY = 'ENTITY',
    VALUE_OBJECT = 'VALUE_OBJECT',
    EVENT = 'EVENT',
    ROLE = 'ROLE',
    RESOURCE = 'RESOURCE',
    UNKNOWN = 'UNKNOWN'
}

interface ConceptDefinition {
    terms: string[];
    stereotype: Stereotype;
    ontologyRef?: string; // e.g., "schema:Order", "fibo:MonetaryAmount"
}

// --- Dictionaries ---

const GENERIC_CONCEPTS: Record<string, ConceptDefinition> = {
    // Value Objects
    'Money': { terms: ['Money', 'Amount', 'Currency', 'Price', 'Cost'], stereotype: Stereotype.VALUE_OBJECT },
    'Date': { terms: ['Date', 'Time', 'Period', 'Duration', 'Timestamp'], stereotype: Stereotype.VALUE_OBJECT },
    'Address': { terms: ['Address', 'Location', 'City', 'Country', 'Zip'], stereotype: Stereotype.VALUE_OBJECT },
    'Status': { terms: ['Status', 'State', 'Type', 'Category', 'Enum'], stereotype: Stereotype.VALUE_OBJECT },
    'Identifier': { terms: ['Id', 'Identifier', 'Key', 'Code', 'Reference'], stereotype: Stereotype.VALUE_OBJECT },

    // Entities
    'User': { terms: ['User', 'Person', 'Customer', 'Account', 'Profile'], stereotype: Stereotype.ENTITY },

    // Events
    'Log': { terms: ['Log', 'History', 'Audit', 'Trace'], stereotype: Stereotype.EVENT },
};

const OFBIZ_CONCEPTS: Record<string, ConceptDefinition> = {
    // GoodRelations / Schema.org
    'Product': { terms: ['Product', 'Good', 'Service', 'Item'], stereotype: Stereotype.RESOURCE, ontologyRef: 'gr:ProductOrService' },
    'Order': { terms: ['Order', 'Quote', 'Request'], stereotype: Stereotype.ENTITY, ontologyRef: 'schema:Order' },
    'Party': { terms: ['Party', 'Person', 'Group', 'Organization', 'Company'], stereotype: Stereotype.ENTITY, ontologyRef: 'foaf:Person / org:Organization' },
    'Inventory': { terms: ['Inventory', 'Stock', 'Facility', 'Warehouse'], stereotype: Stereotype.RESOURCE, ontologyRef: 'prov:Entity' },
    'Accounting': { terms: ['GlAccount', 'Journal', 'Invoice', 'Payment'], stereotype: Stereotype.EVENT, ontologyRef: 'fibo-fnd-acc-cur' },
};

const TREASURY_CONCEPTS: Record<string, ConceptDefinition> = {
    // FIBO / ISO 20022
    'Account': { terms: ['Account', 'CashAccount', 'Nostro', 'Vostro'], stereotype: Stereotype.ENTITY, ontologyRef: 'iso:CashAccount' },
    'Transaction': { terms: ['Transaction', 'Entry', 'Booking', 'Movement', 'Flow'], stereotype: Stereotype.EVENT, ontologyRef: 'iso:Entry' },
    'Balance': { terms: ['Balance', 'Position', 'Limit', 'Exposure'], stereotype: Stereotype.VALUE_OBJECT, ontologyRef: 'iso:Balance' },
    'Party': { terms: ['Debtor', 'Creditor', 'Agent', 'Bank', 'Counterparty'], stereotype: Stereotype.ROLE, ontologyRef: 'iso:Party' },
    'Mandate': { terms: ['Mandate', 'Authorization', 'Contract', 'Agreement'], stereotype: Stereotype.ENTITY, ontologyRef: 'iso:Mandate' },
    'Message': { terms: ['Pain001', 'Camt053', 'Pacs008', 'File', 'Report'], stereotype: Stereotype.EVENT, ontologyRef: 'iso:Message' },
};

// --- Analysis Logic ---

export interface SemanticResult {
    concept: string;
    stereotype: Stereotype;
    confidence: number;
    ontologySource?: string;
    matchedTerm?: string;
}

export const analyzeSemantics = (
    nodeName: string,
    profile: SemanticProfile = SemanticProfile.GENERIC
): SemanticResult => {
    let dictionary = { ...GENERIC_CONCEPTS };

    if (profile === SemanticProfile.ECOMMERCE_OFBIZ) {
        dictionary = { ...dictionary, ...OFBIZ_CONCEPTS };
    } else if (profile === SemanticProfile.TREASURY_ISO20022) {
        dictionary = { ...dictionary, ...TREASURY_CONCEPTS };
    }

    let bestMatch: SemanticResult = {
        concept: 'Unknown',
        stereotype: Stereotype.UNKNOWN,
        confidence: 0
    };

    const normalizedName = nodeName.toLowerCase();

    for (const [conceptKey, def] of Object.entries(dictionary)) {
        for (const term of def.terms) {
            const normalizedTerm = term.toLowerCase();

            // 1. Exact Match (or contained)
            if (normalizedName.includes(normalizedTerm)) {
                // High confidence if explicit match
                const confidence = normalizedName === normalizedTerm ? 1.0 : 0.8;
                if (confidence > bestMatch.confidence) {
                    bestMatch = {
                        concept: conceptKey,
                        stereotype: def.stereotype,
                        confidence,
                        ontologySource: def.ontologyRef,
                        matchedTerm: term
                    };
                }
            }
            // 2. Levenshtein Distance (Fuzzy Match)
            else {
                const distance = get(normalizedName, normalizedTerm);
                const maxLen = Math.max(normalizedName.length, normalizedTerm.length);
                const similarity = 1 - (distance / maxLen);

                if (similarity > 0.8 && similarity > bestMatch.confidence) {
                    bestMatch = {
                        concept: conceptKey,
                        stereotype: def.stereotype,
                        confidence: similarity,
                        ontologySource: def.ontologyRef,
                        matchedTerm: term
                    };
                }
            }
        }
    }

    // Fallback: Structural/Naming Conventions
    if (bestMatch.stereotype === Stereotype.UNKNOWN) {
        if (nodeName.endsWith('Type') || nodeName.endsWith('Status') || nodeName.endsWith('Code')) {
            bestMatch = { concept: 'Type', stereotype: Stereotype.VALUE_OBJECT, confidence: 0.6, matchedTerm: 'Suffix' };
        }
        else if (nodeName.endsWith('Hist') || nodeName.endsWith('Log')) {
            bestMatch = { concept: 'History', stereotype: Stereotype.EVENT, confidence: 0.6, matchedTerm: 'Suffix' };
        }
    }

    return bestMatch;
};
