package edu.asu.irs13;

import java.io.*;
import java.text.NumberFormat;
import java.util.Map.Entry;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

public class PageRank {

	HashMap<Integer, Double> pagerank = new HashMap<Integer, Double>();
	ArrayList<Integer> sinknode = new ArrayList<Integer>();
	double dampingfactor = 0.85; /* damping factor (c) is taken from anlaysis as 8.5*/

	public static void main(String[] args) throws Exception {

		PageRank pr = new PageRank();
		Runtime runtime = Runtime.getRuntime();
		//long allocateMemory=runtime.totalMemory();
		long starttime=System.nanoTime();
		pr.intialrank();  /* method which intializes the values of all documents page-rank as 1/N where N=25054 */
		pr.pagerankalgo(); /* the algorithm which  implements the final page-rank of the documents */
		//long allocatedMemory = runtime.totalMemory();
		//NumberFormat format = NumberFormat.getInstance();
		//long totalmemory=allocateMemory+allocatedMemory;
		//System.out.println("printing the memory used " + format.format(totalmemory / (1024*1024)) );
		//int doc=pr.printmap();
		pr.userQuery(); /* takes input from the user and results the top documents of the query */
		//long endtime=System.nanoTime();
		//System.out.println("time taken for the query is " +(endtime-starttime));

	}
/**
 * The intialrank() method initial pagerank of documents to 1/N 
 * 
 */
	public void intialrank() {   /* initializes the documents with page-rank equal to 1/N */
		double val = (double)1.0 / 25054;
		LinkAnalysis.numDocs = 25054;   /* initiliazing LinkAnalysis class object **/
		LinkAnalysis l = new LinkAnalysis();

		for (int i = 0; i < 25054; i++) {
			int[] links = l.getLinks(i);  /* in case if the links length is zero it is considered as sink node */
			pagerank.put(i, val);
			if (links.length == 0)
				sinknode.add(i);

		}
	}
/**
 * The pagerankalgo() computes the final pagerank values of the documents in the corpus
 */
	public void pagerankalgo() {
		LinkAnalysis.numDocs = 25054;
		LinkAnalysis l = new LinkAnalysis();
		HashMap<Integer, Double> oldpagerank = new HashMap<Integer, Double>(); /* hashmap to generate key value pair for dicument ID and pagerabk value */
		double threshold = Math.pow(10, -9);  /* threshold is taken in order to stop the iterations */
		int itr=0; /* to calculate the total number of iterations taken by the algo to converge */
		for(int i=0; i<1000;i++) {
			itr++;
			double sinkrank = 0.0; /** calculating the sinkrank */
			for (Integer k: sinknode) {
				double rank = pagerank.get(k);
				sinkrank += rank;
			}
			/* copying the current itertaion pagerank values to atemp hashmap */
			for (Integer co: pagerank.keySet()) {
				oldpagerank.put(co, pagerank.get(co));
			}

			/** for here the computation starts
			 * 
			 */
			for (int k = 0; k < pagerank.size(); k++) {
				double newrank = (double)(1.0 - dampingfactor) / 25054;
				newrank += ((dampingfactor * sinkrank) / 25054);
				pagerank.put(k, newrank);  /* computing new pagerank with respect to the damping factor */
				
				int[] cite = l.getCitations(k);  /* getting the citations of the present document ID */
				for (int q = 0; q < cite.length; q++) {
					double temp = pagerank.get(k);
					int[] outlinks = l.getLinks(cite[q]);
					temp += (dampingfactor * pagerank.get(cite[q])) / outlinks.length; /* updating the pagerank of the docuID based on its citations and citations links */
					pagerank.put(k, temp);  /* putting the computed value in the hashmap */

				}

			}

			/** checking if each document pagerank at ith iteration and i-1 iteration differ by threshold 
			 * 
			 */
			boolean check = true;
			for (Integer ch: pagerank.keySet()) {
				
				double rank = pagerank.get(ch);
				double prerank = oldpagerank.get(ch);
				if (Math.abs(rank - prerank) > threshold)
					//System.out.println(Math.abs(rank-prerank));
					check = false;
			}	
			if(check)
				break;

		}
		//oldpagerank.clear();
		System.out.println("number of iterations " + itr);
	}

	/**
	 * printmap prints the key value pair of document ID and pagerank values 
	 * @return
	 */
	public int printmap() {
		Map<Integer,Double> sortList=new HashMap<Integer,Double>();//auths_score=sortByValues(auths_score);
		sortList=sortByValues(pagerank);
		int count=0;
		System.out.println("printing highest pagerank");
		for (Map.Entry<Integer, Double> e : sortList.entrySet()) {
			count++;
		    System.out.println(e.getKey());
		    System.out.println(e.getValue());
		    if(count==10)
		    {
		    	break;
		    }
		}
		return 1;
	}

	
	/**
	 * this method takes the input from the user and based on the top tf-IDf similarity computation along with pagerank metric top 10 documents 
	 * were given to the user
	 * @throws Exception
	 */
	public void userQuery() throws Exception
	{
		 
		HashMap<Integer, Double> results = new HashMap<Integer, Double>();
		HashMap<Integer,Double> finalResults=new HashMap<Integer,Double>();
		TfIDFIndex docs = new TfIDFIndex();
		docs.docWord(); /* starts docWord() method in the TfIDFIndex class */
		docs.docNorms(); /* starts docNorms() methdos in the TfIDFIndex class */
		Scanner scan=new Scanner(System.in);
		String str = "";
		double weight=0.4; /** assigning wegith as 0.4 **/
		System.out.print("query> ");
		while(!(str = scan.nextLine()).equals("quit"))
		{
			results = docs.idfInput(str);  /* gets the documents which are similar to the query */
			System.out.println("printing the top if-idf documents");
			Map<Integer,Double> sortList=new HashMap<Integer,Double>();
			
			/**
			 * calculating the pagerank metric with formula w*pagerankvale +(1-w) vectorsimilarity valies 
			 */
			Set<Integer> keys=results.keySet();
			for(Integer key :keys)
			{
				double idfvalue=results.get(key);
				double paRank=pagerank.get(key);
				double tempvalue=(weight*paRank)+(1-weight)*idfvalue;
				
				finalResults.put(key,tempvalue);
				
			}
		
			sortList=sortByValues(finalResults); /* sorting the documents based on the values **/
			int count=0;
			System.out.println("printing after pagerank algos");
			for (Map.Entry<Integer, Double> e : sortList.entrySet()) {
				count++;
			    System.out.println(e.getKey() + " " +e.getValue());
			    if(count==10)
			    	break;
			}
			/**count=0;
			System.out.println("printing values with pagerank");
			for (Map.Entry<Integer, Double> e : sortList.entrySet()) {
				count++;
			    System.out.println(e.getValue());
			    if(count==10)
			    	break;
			}**/
			print_docs(sortList);
			finalResults.clear();
			System.out.println("query>");
		}
		scan.close();
	}
	
	/**
	 * sort function which sorts the hashpmap entities in decreasing order
	 * @param map
	 * @return
	 */
		public <K, V extends Comparable<V>> Map<K, V> sortByValues(final Map<K, V> map) {
	    	Comparator<K> valueComparator =  new Comparator<K>() {
	    	    public int compare(K k1, K k2) {
	    	        int compare = map.get(k2).compareTo(map.get(k1));
	    	        if (compare == 0) return 1;
	    	        else return compare;
	    	    }
	    	};
	    	Map<K, V> sortedByValues = new TreeMap<K, V>(valueComparator);
	    	sortedByValues.putAll(map);
	    	
	    	return sortedByValues;
		
	}
		/**
		 * Method which gives the document URL's for the specific document ID 
		 * @param sort_set
		 * @throws CorruptIndexException
		 * @throws IOException
		 */
		public static void print_docs(Map<Integer,Double> sort_set) throws CorruptIndexException, IOException
		{
			IndexReader r = IndexReader.open(FSDirectory.open(new File("index")));
			Set<Integer> keys=sort_set.keySet();
			int count=0;
			for(Integer key :keys)
			{
				count++;
				Document d = r.document(key);
				String url = d.getFieldable("path").stringValue(); // the 'path' field of the Document object holds the URL
				System.out.println(url.replace("%%", "/"));
				if(count==10)
					break;
			}	
		}
}
