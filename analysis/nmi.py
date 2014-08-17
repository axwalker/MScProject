#import operator
from igraph import *
import sys

def communities_from(ground_truth_file):
	assigned_nodes = set([])
	all_communities = (line.split() for line in open(ground_truth_file))
	nodes_dict = {}
	for index, community in enumerate(all_communities):
		if len([node for node in community if node not in assigned_nodes]) < 3:
			continue
		for node in community:
			if node not in assigned_nodes:
				nodes_dict[int(node)] = index
				assigned_nodes.add(node)
	return nodes_dict

def nodes_dict(graph, id_label, community_label):
	node_tuples = ((node[id_label], node[community_label]) for node in graph.vs)
	return {int(id): int(community) for (id, community) in node_tuples}

def nodes_dict2(graph, id_label, community_label):
	node_tuples = ((node[id_label], node[community_label]) for node in graph.vs)
	return {int(id): ord(community) for (id, community) in node_tuples}

def sorted_communities(nodes_dict):
	return [node[1] for node in sorted(nodes_dict.items(), key=lambda x: x[0])]

def main1(file1, file2):
	graph1 = Graph.Read_GML(file1)
	graph2 = Graph.Read_GML(file2)
	communities1 = sorted_communities(nodes_dict2(graph1, 'id', 'value'))
	communities2 = sorted_communities(nodes_dict(graph2, 'id', 'community'))
	nmi = compare_communities(communities1, communities2, method = "nmi")
	print('nmi: {:.3f}'.format(nmi))

def main2(ground_truth_file, found_file):
	ground_truth = communities_from(ground_truth_file)
	found = nodes_dict(Graph.Read_GML(found_file), 'label', 'community')
	found_filtered = {id: community for (id, community) in found.items() if id in ground_truth}

	if (len(found) / len(ground_truth) > 10):
		print('not enough overlap')
	else:   
		ground_truth_communities = sorted_communities(ground_truth)
		found_communities = sorted_communities(found_filtered)

		nmi = compare_communities(ground_truth_communities, found_communities, method = "nmi")
		print('nmi: {:.3f}'.format(nmi))



ground_truth_file = sys.argv[1]
found_file = sys.argv[2]
main1(ground_truth_file, found_file)
#main2(ground_truth_file, found_file)