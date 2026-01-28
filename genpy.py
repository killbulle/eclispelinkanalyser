import networkx as nx
import community.community_louvain as louvain # pip install python-louvain
import matplotlib.pyplot as plt
from difflib import SequenceMatcher

#Configuration de la simulation
RELATION_WEIGHTS = {
    'COMPOSITION': 1.0,   # Cascade ALL
    'AGGREGATION': 0.6,   # Eager Loading
    'ASSOCIATION': 0.2,   # Lazy Loading
    'WEAK': 0.1           # ManyToMany ou Optional
}

# Dictionnaire sémantique simplifié (ISO 20022 / Trésorerie)
SEMANTIC_DICT = {
    'ROOT': ['Account', 'Bank', 'LiquidityPosition', 'CashPool'],
    'VO': ['Amount', 'Currency', 'Date', 'Status', 'Iban', 'Bic'],
    'ENTITY': ['Entry', 'Transaction', 'Flow', 'AuditLog']
}

class ORMGraphAnalyzer:
    def __init__(self):
        self.G = nx.DiGraph()

    def add_class(self, name, method_count=5):
        # method_count simule la "richesse" comportementale (peu de méthodes = data structure)
        self.G.add_node(name, method_count=method_count, type="Unknown")

    def add_relation(self, source, target, rel_type):
        weight = RELATION_WEIGHTS.get(rel_type, 0.1)
        self.G.add_edge(source, target, weight=weight, relation=rel_type)

    def _semantic_score(self, class_name):
        """Retourne un type probable basé sur le nom"""
        for k, keywords in SEMANTIC_DICT.items():
            for kw in keywords:
                if kw in class_name:
                    return k
        return "UNKNOWN"

    def _is_sink_node(self, node):
        """Un noeud feuille ne pointe vers rien (ou que des scalaires non modélisés ici)"""
        return self.G.out_degree(node) == 0

    def analyze(self):
        print(f"--- Analyse du Graphe ({self.G.number_of_nodes()} noeuds) ---")
        
        # 1. Detection de communautés (Clusters / Agrégats Potentiels)
        # On convertit en non-orienté pour Louvain
        undirected_G = self.G.to_undirected()
        partition = louvain.best_partition(undirected_G, weight='weight')
        
        clusters = {}
        for node, cluster_id in partition.items():
            clusters.setdefault(cluster_id, []).append(node)

        # 2. Analyse par Cluster
        for cid, members in clusters.items():
            print(f"\n[Cluster {cid}] Membres : {members}")
            
            # Identifier la Racine (plus haut degré entrant interne + score sémantique)
            candidates = []
            for member in members:
                sem_type = self._semantic_score(member)
                in_degree = self.G.in_degree(member)
                
                # Heuristique simple de scoring pour la racine
                score = in_degree
                if sem_type == 'ROOT': score += 5
                if sem_type == 'VO': score -= 5 # Un VO ne peut pas être racine
                
                candidates.append((member, score, sem_type))
            
            candidates.sort(key=lambda x: x[1], reverse=True)
            root = candidates[0][0]
            print(f"  -> Racine Suggerée : {root} (Score: {candidates[0][1]})")

            # Identifier les Value Objects
            for member in members:
                if member == root: continue
                
                sem_type = self._semantic_score(member)
                is_leaf = self._is_sink_node(member)
                
                # Règle : Si sémantique VO ou Feuille sans comportement -> VO
                if sem_type == 'VO' or (is_leaf and self.G.nodes[member]['method_count'] < 2):
                    print(f"  -> Suggestion VO : {member} (IsLeaf: {is_leaf}, Sem: {sem_type})")
                else:
                    print(f"  -> Entité Interne : {member}")

        # 3. Analyse des coupures (Liens inter-clusters)
        print("\n[Analyse des Coupures]")
        for u, v in self.G.edges():
            if partition[u] != partition[v]:
                weight = self.G[u][v]['weight']
                print(f"  -> COUPER lien {u} -> {v} (Poids: {weight}) car traverse la frontière des agrégats.")

# --- Simulation d'un cas réel "Sale" ---

analyzer = ORMGraphAnalyzer()

# Cluster Trésorerie (devrait être détecté ensemble)
analyzer.add_class("CashAccount", method_count=20)
analyzer.add_class("AccountEntry", method_count=5)
analyzer.add_class("MoneyAmount", method_count=0) # Anémique
analyzer.add_class("Currency", method_count=0)

analyzer.add_relation("CashAccount", "AccountEntry", "COMPOSITION") # OneToMany Cascade
analyzer.add_relation("AccountEntry", "MoneyAmount", "AGGREGATION") 
analyzer.add_relation("MoneyAmount", "Currency", "AGGREGATION")

# Cluster Tiers (Counterparty)
analyzer.add_class("Counterparty", method_count=15)
analyzer.add_class("PostalAddress", method_count=1)

analyzer.add_relation("Counterparty", "PostalAddress", "COMPOSITION")

# Le lien "Sale" à couper (Relation directe entre Entrée comptable et Tiers)
# Souvent fait en ORM Lazy, devrait être un ID
analyzer.add_relation("AccountEntry", "Counterparty", "ASSOCIATION") 

# Un autre lien sale (Cycle ou dépendance faible)
analyzer.add_class("AuditLog", method_count=2)
analyzer.add_relation("CashAccount", "AuditLog", "WEAK")
analyzer.add_relation("Counterparty", "AuditLog", "WEAK")

# Lancer l'analyse
analyzer.analyze()