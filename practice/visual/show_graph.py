import sys
import networkx as nx
import matplotlib.pyplot as plt
import struct

def make_graph(filename):
    G = nx.Graph()
    fh = open(filename, 'rb')
    G = nx.read_edgelist(fh)
    fh.close()
    return G

def get_node_colour_map_binary(filename):
    label_map = {}
    with open(filename, "rb") as f:
        index = 0
        bytes = f.read(4)
        while bytes:
            label_map[str(index)] = int(struct.unpack('i', bytes)[0])
            bytes = f.read(4)
            index += 1
    return label_map

def get_node_colour_map(filename):
    label_map = {}
    with open(filename, 'r') as f:
        for line in f:
            elements = line.rstrip().split(" ")
            label_map[elements[0]] = int(elements[1])
    return label_map

G = make_graph(sys.argv[1])
val_map = get_node_colour_map_binary(sys.argv[1] + ".4Bj.vout")

values = [val_map.get(node, 0.25) for node in G.nodes()]
nx.draw(G, with_labels=True, node_color = values)
plt.show()