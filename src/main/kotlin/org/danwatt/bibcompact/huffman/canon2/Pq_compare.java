package org.danwatt.bibcompact.huffman.canon2;

import java.util.Comparator;

// comparator class helps to compare the node
// on the basis of one of its attribute.
// Here we will be compared
// on the basis of data values of the nodes.
class Pq_compare implements Comparator<Node> {
    public int compare(Node a, Node b)
    {

        return a.data - b.data;
    }
}