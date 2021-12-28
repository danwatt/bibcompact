package org.danwatt.bibcompact.huffman.canon2;

import java.io.IOException;
import java.util.*;

public class Canonical_Huffman {

    // Treemap to store the
    // code lengths(sorted) as keys
    // and corresponding(sorted)
    // set of characters as values
    static TreeMap<Integer, TreeSet<String>> data;

    // Constructor to initialize the Treemap
    public Canonical_Huffman() {
        data = new TreeMap<>();
    }

    // Recursive function
    // to generate code lengths
    // from regular Huffman codes
    static void code_gen(Node root, int code_length) {
        if (root == null)
            return;

        // base case; if the left and right are null
        // then its a leaf node.
        if (root.left == null && root.right == null) {

            // check if key is present or not.
            // If not present add a new treeset
            // as value along with the key
            if (!data.containsKey(code_length)) {
                data.put(code_length, new TreeSet<String>());
            }

            // c is the character in the node
            data.get(code_length).add(root.c);
            return;
        }

        // Add 1 when on going left or right.
        code_gen(root.left, code_length + 1);
        code_gen(root.right, code_length + 1);
    }

    public static List<Integer> testCanonicalHC(int n, String chararr[], int freq[]) {
        List<Integer> codeLengths = new ArrayList<Integer>();



        // min-priority queue(min-heap).
        PriorityQueue<Node> q
                = new PriorityQueue<Node>(n, new Pq_compare());

        for (int i = 0; i < n; i++) {

            // creating a node object
            // and adding it to the priority-queue.
            Node node = new Node();

            node.c = chararr[i];
            node.data = freq[i];

            node.left = null;
            node.right = null;

            // add functions adds
            // the node to the queue.
            q.add(node);
        }

        // create a root node
        Node root = null;

        // extract the two minimum value
        // from the heap each time until
        // its size reduces to 1, extract until
        // all the nodes are extracted.
        while (q.size() > 1) {

            // first min extract.
            Node x = q.peek();
            q.poll();

            // second min extract.
            Node y = q.peek();
            q.poll();

            // new node f which is equal
            Node nodeobj = new Node();

            // to the sum of the frequency of the two nodes
            // assigning values to the f node.
            nodeobj.data = x.data + y.data;
            nodeobj.c = "-";

            // first extracted node as left child.
            nodeobj.left = x;

            // second extracted node as the right child.
            nodeobj.right = y;

            // marking the f node as the root node.
            root = nodeobj;

            // add this node to the priority-queue.
            q.add(nodeobj);
        }

        // Creating a canonical Huffman object
        Canonical_Huffman obj = new Canonical_Huffman();

        // generate code lengths by traversing the tree
        code_gen(root, 0);

        // Object array to store the keys
        Object[] arr = data.keySet().toArray();

        // Set initial canonical code=0
        int c_code = 0, curr_len = 0, next_len = 0;

        for (int i = 0; i < arr.length; i++) {
            Iterator it = data.get(arr[i]).iterator();

            // code length of current character
            curr_len = (int) arr[i];

            while (it.hasNext()) {

                // Display the canonical codes
                System.out.println(it.next() + "\t"
                        + Integer.toBinaryString(c_code) + "\t" + Integer.toBinaryString(c_code).length());
                codeLengths.add(Integer.toBinaryString(c_code).length());

                // if values set is not
                // completed or if it is
                // the last set set code length
                // of next character as current
                // code length
                if (it.hasNext() || i == arr.length - 1)
                    next_len = curr_len;
                else
                    next_len = (int) arr[i + 1];

                // Generate canonical code
                // for next character using
                // regular code length of next
                // character
                c_code = (c_code + 1) << (next_len - curr_len);
            }
        }

        return codeLengths;
    }

    // Driver code
    public static void main(String args[]) throws IOException {
        int n = 4;
        String[] chararr = {"a", "b", "c", "d"};
        int[] freq = {10, 1, 15, 7};
        testCanonicalHC(n, chararr, freq);
    }
}