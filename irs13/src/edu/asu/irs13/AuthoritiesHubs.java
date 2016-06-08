package edu.asu.irs13;

import java.io.*;
import java.util.Map.Entry;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import java.util.ArrayList;
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

public class AuthoritiesHubs {
	static HashMap<Integer,Double> hubs_score=new HashMap<Integer,Double>();  /* hashmap to have docid -husb values as key value pair */
	static HashMap<Integer,Double> auths_score=new HashMap<Integer,Double>(); /* hashmap to have docid and auth score as key-value pair */
	static HashMap<Integer,Double> doc_norms=new HashMap<Integer,Double>();   /* doc norms which are returned form project 1 **/
	static HashMap<Integer, HashMap<String, Integer>> doc_words=new HashMap<Integer,HashMap<String,Integer>>(); /* doc words which are returned from project 1 **/
	
	public static void main(String[] args) throws Exception
	{
		//long startTime=System.nanoTime();
		intial(); /* intial method which gets the doc_words and doc_norms from project 1 **/
		//long endTime=System.nanoTime(); 
		//System.out.println(endTime-startTime);
		//System.out.println("intial analysis to create doc_words and doc_norms of tf-idf function");
		//startTime=System.nanoTime();
		queryDocs(); /* takes user input from the user and computes top 10 authorities and hubs for a given query **/
		//endTime=System.nanoTime();
		//System.out.println(endTime-startTime);
		//System.out.println("the total time taken for the 6 queries");
		
	}
	
	/**
	 * intial() method gets the odc_words and doc_norms form the project 1
	 * @throws Exception
	 */
	public static void intial() throws Exception {
		doc_words=TfIDFIndex.docWord();
		doc_norms=TfIDFIndex.docNorms();
	}
	
	/**
	 * takes input from the user and based on tf-IDf similarity hub_auth_Cal methos is called 
	 * @throws Exception
	 */
	public static void queryDocs() throws Exception
	{
		
		HashMap<Integer, Double> results = new HashMap<Integer, Double>();
		Scanner scan=new Scanner(System.in);
		String str = "";
		System.out.print("query> "); /* takes input from the user **/
		while(!(str = scan.nextLine()).equals("quit"))
		{
		//	long st=System.nanoTime();
		results = TfIDFIndex.idfInputK(str); /* takes top 10 documents of tf-idf vector similarity for a given query **/
		//long en=System.nanoTime();
		//System.out.println("time taken for root set " + (en-st));
		//System.out.println(results.size());
		Set<Integer> keys2=results.keySet();
		//System.out.println(keys2);
		
		//long startTime=System.nanoTime();
		auth_hub_calcu(keys2); /** calclutaes the hub and authority score for the documents in the root set **/
		//long endtime=System.nanoTime();
		//System.out.print("time taken for entire hub-auth algo");
		//System.out.println((str)+" "+(endtime-startTime));
		hubs_score.clear();
		auths_score.clear();
		System.out.print("query> ");
		
		}
		scan.close();

	}
/**
 * base set is created and authorities and hibs values are computed for the base
	 * set documents and sorting is performed on them and top 10 documents are retrived  
 * @param keyset
 * @throws Exception
 */
	public static void auth_hub_calcu(Set<Integer> keyset) throws Exception{
		
		double threshhold=Math.pow(10,-9);
		LinkAnalysis.numDocs = 25054;
		LinkAnalysis l = new LinkAnalysis();
		Set<Integer> baseset=new HashSet<Integer>();

		/** generating base set for top 10 documents of the query **/
		//long startTime=System.nanoTime();
		int itr=0;
		for (Integer key : keyset) {
			itr++;
			baseset.add(key);
			int[] link1 = l.getLinks(key); /* stores all links for a given document in the base set */
			for (int i = 0; i < link1.length; i++) {
				baseset.add(link1[i]);
			}
			int[] link2 = l.getCitations(key); /* stores all citations ffor a given document in the base set */
			for (int i = 0; i < link2.length; i++) {
					baseset.add(link2[i]);
			}
		}
		//long endTime=System.nanoTime();
		//System.out.print(endTime-startTime);
		//System.out.println(" time take for creating base set");
		
		//System.out.println(baseset);
		System.out.println("baseset size is  "+baseset.size());
	
		int leng = baseset.size();

		//startTime=System.nanoTime();
		int[][] admatrix = new int[leng + 1][leng + 1];  /* creating adjacency matrix */
		int i = 1;
		
		/* storing the values of matrix as 1 in case the document in a particular row ahs links to other documensts */
		for (Integer key : baseset) {
			admatrix[i][0] = key;
			admatrix[0][i] = key;
			i++;
		}
		
		for(int j=1;j<leng+1;j++)
		{
			int[] rows = l.getLinks(admatrix[j][0]);
			for(int k=0;k<rows.length;k++)
			{
				for(int b=1;b<=leng;b++)
				if(rows[k]==admatrix[0][b])
				{
					admatrix[j][b]=1;
				}
			}
			
		}
		//endTime=System.nanoTime();
		//System.out.print("time taken to generate adjacency matrix");
		//System.out.println(endTime-startTime);
		/** genrating adjacency matrix transpose
		 * 
		 */
		int [][] adjmatrixtrnas=new int[leng][leng]; 
		for(int m=1;m<=leng;m++)
		{
			for(int k=1;k<=leng;k++)
			{
				adjmatrixtrnas[k-1][m-1]=admatrix[m][k];
			}
		}
		
		//long inStat=System.nanoTime();
		/**
		 * creating hub_matrix and auth_matrix 
		 */
		double[][] hub_matrix=new double[leng][1];
		double[][] auth_matrix=new double[leng][1];
		
		double[][] pre_hub_matrix=new double[leng][1];
		double[][] pre_auth_matrix=new double[leng][1];
		//long inEnd=System.nanoTime();
		//System.out.println("Initialization phase "+ (inEnd-inStat));
		/**
		 * iniializing hubs and authorities matrix to value 1
		 */
		for(int k=0;k<leng;k++)
		{
			hub_matrix[k][0]=1.0;
			auth_matrix[k][0]=1.0;
		}
		
		int iter=0;
		//startTime=System.nanoTime();
		//long iTime = 0,oTime=0,nTime=0;
		
		for(int k=0; k<1000;k++)
		{
			double hub_norm=0.0;
			double auth_norm=0.0;
			
			//long var1=System.nanoTime();
			/**
			 * I-operation
			 * computing authority value and hib value
			 * with respect to a=A(t)*h(i-1)
			 * 
			 */
			for(int a=0; a<leng;a++)
			{
				for(int b=0;b<1;b++)
				{
					for (int c=0;c<leng;c++)
					{
					
						auth_matrix[a][b]+=adjmatrixtrnas[a][c]*hub_matrix[c][b];
						auth_norm+=auth_matrix[a][b]*auth_matrix[a][b];
					}
				}
			}
			//long var2=System.nanoTime();
			//iTime+=Math.abs(var2-var1);
			
			//var1=System.nanoTime();
			/**
			 * O-operation
			 * computing hub value with respect 
			 * to h=A*a(i)
			 */
			for(int a=0; a<leng;a++)
			{
				for(int b=0;b<1;b++)	
				{
					for (int c=0;c<leng;c++)
					{
					
						hub_matrix[a][b]+=admatrix[a+1][c+1]*auth_matrix[c][b];
						hub_norm+=hub_matrix[a][b]*hub_matrix[a][b];
					}
				}
			}
			//var2=System.nanoTime();
			//oTime+=Math.abs(var2-var1);
			
			//var1=System.nanoTime();
			/**
			 * normalization operation which 
			 * divides each authority and hub value with auth and hub norm  
			 */
			for(int a=0; a<leng;a++)
			{
				for(int b=0;b<1;b++)
				{
					auth_matrix[a][b]=(double) (auth_matrix[a][b]/Math.sqrt(auth_norm));
					hub_matrix[a][b]=(double)(hub_matrix[a][b]/Math.sqrt(hub_norm));
				}
			}
			
			//var2=System.nanoTime();
			//nTime+=Math.abs(var2-var1);
			double max1=0.0;
			double max2=0.0;
		/* check if each of the auth and hub value is comparable with threshold (calculating L-infinity norm ))*/	
			for(int a=0; a<leng;a++)
			{
				
				for(int b=0;b<1;b++)
				{
					double temp=(hub_matrix[a][b]-pre_hub_matrix[a][b]);
					if(temp < 0)
						temp=temp*(-1);
					double temp2=(auth_matrix[a][b]-pre_auth_matrix[a][b]);
					if(temp2 <0)
						temp2=temp2*(-1);
					if(temp >max1)
					{
						max1=temp;
					}
					if(temp2 >max2)
					{
						max2=temp2;
					}
				}
			}
			
			if(max1 <threshhold && max2 < threshhold)
				break;
					
			iter=k;
			for(int a=0;a<leng;a++)
			{
				for(int b=0; b<1;b++)
				{
					pre_hub_matrix[a][b]=hub_matrix[a][b];
					pre_auth_matrix[a][b]=auth_matrix[a][b];
				}
			}
			
			
		}
		/***endTime=System.nanoTime();
		System.out.println("the count of iterations is " +iter);
		System.out.println("I phase " + (iTime/iter));
		System.out.println("O-Phase "+ oTime/iter );
		System.out.println("N-phase"+ nTime/iter);
		
		
		System.out.print("time take for calcultaing matrices");
		System.out.println((endTime-startTime)/iter); ***/
		List<Integer> baseset_list=new ArrayList<Integer>();
		Iterator<Integer> iterator = baseset.iterator();
		
		while(iterator.hasNext())
		{
			baseset_list.add(iterator.next());
		}
		
		
		for(int a=0;a<leng;a++)
		{
			for(int b=0; b<1;b++)
			{
				hubs_score.put(baseset_list.get(a),hub_matrix[a][b]);
				auths_score.put(baseset_list.get(a),auth_matrix[a][b]);
			}
		}
		
		//startTime=System.nanoTime();
		/** sorting the hubs and authority values **/
		Map<Integer,Double> sort_hub=new HashMap<Integer,Double>();//auths_score=sortByValues(auths_score);
		sort_hub=sortByValues(hubs_score);
				
		int count=0;
		System.out.println("Printing Hubs score");
		//System.out.println("size of the hubs is " +sort_hub.size());
		for (Map.Entry<Integer, Double> e : sort_hub.entrySet()) {
			count++;
		    System.out.println(e.getKey()+ " " +e.getValue());
		    if(count==10)
		    	break;
		}
		
		//endTime=System.nanoTime();
		//System.out.print("sorting time for hubs ");
		//System.out.println(endTime-startTime);
		print_docs(sort_hub);
		 count=0;
		 //startTime=System.nanoTime();
		 sort_hub=sortByValues(auths_score);
		System.out.println("Printing authorities score");
		//System.out.println("size of the authorities is " +sort_hub.size());
		for (Map.Entry<Integer, Double> e : sort_hub.entrySet()) {
			count++;
		    System.out.println(e.getKey() + " " +e.getValue());
		    if(count==10)
		    	break;
		}
		print_docs(sort_hub);
		//endTime=System.nanoTime();
		//System.out.print("sorting time for auths ");
		//System.out.println(endTime-startTime);
		
		
	}
	

	public static <K, V extends Comparable<V>> Map<K, V> sortByValues(final Map<K, V> map) {
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
	/** prints the document url
	 * for a given document ID 
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
