package edu.asu.irs13;
/** author :Alekhya Cheruvu
 * ID: 1209209296
 * 
 */

import java.util.*;
import java.io.*;
import java.util.Map.Entry;

import org.apache.lucene.index.*;
import org.apache.lucene.store.*;

public class tfCalculation {
	static HashMap<Integer, HashMap<String, Integer>> doc_words = new HashMap<Integer, HashMap<String, Integer>>();
	static HashMap<Integer,Double> doc_norms=new HashMap<Integer,Double>();
	static int MAXDOC=30000;
	public static void main(String[] args) throws Exception {
		docWord();  /* this functions generates DOCID , <Word,frequency> hash map*/
		docNorms(); /* this functions generates DOCID , DOC norm */
		tfInput(); /* used to calculate TF weight for a query */
		idfInput(); /* used to calculate TF-IDF weight for a query */
	
		
	}
	
	public static void docWord() throws Exception{
		
		// long startTime=System.nanoTime();
		 
		/** creates a  index for all the documents present in the dictionary **/
		IndexReader r = IndexReader.open(FSDirectory.open(new File("index")));
		
		/* iterating over the terms for which index is generated **/
		TermEnum t=r.terms();
		while(t.next())
		{
			String word=t.term().text();        /* getting the word */
			Term term=new Term("contents",word);
			TermDocs td=r.termDocs(term);
			
			while(td.next())    /* iterating over the list of documents in which word is present*/
			{
				int docID=td.doc();
				
				
				int word_freq=td.freq(); /* getting the word frequency of the document */
				
				HashMap<String,Integer> values=doc_words.get(docID);
				/** storing the values in with key being document ID and values being an hashmap which has word 
				 * as key and value as its frequency
				 */
				if(values==null)
				{
					values=new HashMap<String,Integer>();
					values.put(word,word_freq);
					doc_words.put(docID, values);
				}
				else
				{
					values.put(word,word_freq);
				}
				
			}
		}
		
		
	 //long endTime=System.nanoTime();
	
	//System.out.println("Doc words time" );  /* printing the time  the algorithm took to get done */
	//System.out.println(endTime -startTime);
	
}
	

public static void docNorms() throws Exception{
	//final long startTime=System.nanoTime();
	/* getting the index of the file having all the document s**/
	IndexReader r = IndexReader.open(FSDirectory.open(new File("index")));
	/* iterating over the terms */
	TermEnum ter=r.terms(); 
	Set<Integer> keys = doc_words.keySet(); /* generating a key set for all the doc ID's which are present in doc_words hash map */
	for(int key:keys)
	{
		/** calculating document norm for each doc id and toring in the hash map with doc ID as key and doc norm as value **/
		HashMap<String,Integer> temp=doc_words.get(key);
		Set<String> str=temp.keySet();
		int num=0;
		for(String st:str)
		{
			 num=(int) (num+Math.pow(temp.get(st), 2));
			
		}
		
		double value=Math.sqrt(num);
		doc_norms.put(key, value);
	}
	//final long endTime=System.nanoTime();    /* printing the time taken for the algorithm to get implemented */
	//System.out.println(endTime -startTime);
}


/** calculates the TF weights for a query based on pre computed document norms
 * 
 * @throws Exception
 */
public static void tfInput() throws Exception{
	
	/* creating and index for all the documents present in the file */
	
	IndexReader r = IndexReader.open(FSDirectory.open(new File("index")));
	
	
	HashMap<String,Integer> queryNorm=new HashMap<String,Integer>();
	HashMap<Integer,Double> inter_temp=new HashMap<Integer,Double>();
	HashMap<Integer,Double> final_temp=new HashMap<Integer,Double>();
	
	System.out.println("Generating TF weights for the input queries ");
	
	/* getting query from the user **/
	Scanner scan=new Scanner(System.in);
	String str = "";
	System.out.print("query> ");
	while(!(str = scan.nextLine()).equals("quit"))
	{
		String[] terms = str.split("\\s+");
		/* splitting the query based on the white space and storing in the hash map with word as key and its frequency as velue */
		for(String word : terms)
		{
			if(!queryNorm.containsKey(word))
			{
				queryNorm.put(word,1);
			}
			else
			{
				queryNorm.put(word,queryNorm.get(word)+1);
			}
			
		}
		
		 //double startTime=System.nanoTime();
		/** the following liens of code implements query norm which is pre computed 
		 * 
		 */
		double query_norm=0;
		Set<String> query_keys=queryNorm.keySet();
		for (String key : query_keys)
		{
			query_norm=query_norm+Math.pow(queryNorm.get(key),2);
		}
		query_norm=Math.sqrt(query_norm);
		
		/** for each word in the query , the corresponding documents are retrieved and 
		 * word frequency in query is multiplied qith word frequency present in the document
		 * 
		 */
		for(String word:terms)
		{
			Term term = new Term("contents", word);
			TermDocs tdocs = r.termDocs(term);
			int temp=0;
			double temp2=0;
			while(tdocs.next())
			{
				
				temp=doc_words.get(tdocs.doc()).get(word);
				//System.out.println(word);
				temp2=queryNorm.get(word);
				
				Double temp3=inter_temp.get(tdocs.doc());
				if(temp3==null)
				{
					inter_temp.put(tdocs.doc(),temp*temp2);
				}
				else
				{
					inter_temp.put(tdocs.doc(),inter_temp.get(tdocs.doc())+ (temp*temp2));
				}
				
				
			}
		}
			
		/** key set is generated for the above hash map and the value is divided with query norm and document norm
		 * to generate TF weights
		 * 
		 * 
		 */
			Set<Integer> interKeys=inter_temp.keySet();
			double value,value2=0;
			for(Integer key : interKeys)
			{
				value=doc_norms.get(key);
				value2=inter_temp.get(key)/(value*query_norm);
				final_temp.put(key, value2);
			}
			
			//double endTime=System.nanoTime();
			//System.out.println(endTime-startTime);
			
			// double start_time=System.nanoTime();
			/**
			 * after generating the TF weights , they are stored in hashmap with DOC ID as key and TF weight as
			 * value.Sorting is done on the documents in order to print top 10 docuent list 
			 */
			Set<Entry<Integer, Double>> set =final_temp.entrySet();
	        List<Entry<Integer, Double>> list = new ArrayList<Entry<Integer,Double>>(
	                set);
	        Collections.sort(list, new Comparator<Map.Entry<Integer,Double>>() {
	            public int compare(Map.Entry<Integer, Double> o1,
	                    Map.Entry<Integer, Double> o2) {
	                return o2.getValue().compareTo(o1.getValue());
	            }
	        });
	        int i=0;
	        /* printing top 10 documents for a query */
	       for (Entry<Integer, Double> entry : list) {
	            System.out.println("Document ID" +" " +entry.getKey() + " TF weight value " +entry.getValue());
	            i++;
	            if(i==10)
	            	break;

	        }
	        //double end_Time=System.nanoTime();
	       // System.out.println("TF calculation");
			//System.out.println(end_Time-start_time);
			
	        System.out.print("query> ");
	        queryNorm.clear();
	        inter_temp.clear();
	        final_temp.clear();
			
			
		}
		
	}


/**
 * the following method idfInput() generates TF-IDF weights for the query based on pre computed document norms 
 * @throws Exception
 */
public static void idfInput() throws Exception{
	
	/** creating an index for the documents present */
	IndexReader r = IndexReader.open(FSDirectory.open(new File("index")));
	
	
	System.out.println("Generating TF-IDF weights for the given queries");
		

		HashMap<String,Integer> queryNorm=new HashMap<String,Integer>();
		HashMap<Integer,Double> inter_temp=new HashMap<Integer,Double>();
		HashMap<Integer,Double> final_temp=new HashMap<Integer,Double>();
		
		/** scanning the query given by the user */
		Scanner scan=new Scanner(System.in);
		String str = "";
		System.out.print("query> ");
		while(!(str = scan.nextLine()).equals("quit"))
		{
			/** splitting the keywords in the query and storing them in hashmap
			 * with key being the word and value being its count present in the  query
			 */
			String[] terms = str.split("\\s+");
			for(String word : terms)
			{
				if(!queryNorm.containsKey(word))
				{
					queryNorm.put(word,1);
				}
				else
				{
					queryNorm.put(word,queryNorm.get(word)+1);
				}
				
			}
			//double startTime=System.nanoTime();
			double query_norm=0;
			/** for the query set , query norm is computed 
			 * 
			 * 
			 */
			Set<String> query_keys=queryNorm.keySet();
			for (String key : query_keys)
			{
				query_norm=query_norm+Math.pow(queryNorm.get(key),2);
			}
			query_norm=Math.sqrt(query_norm);
			
			/** generating TF-IDF values for the query keywords from the document list
			 * 
			 */
			for(String word:terms)
			{
				/** getting the documents in which word is present*/
				Term term = new Term("contents", word);
				TermDocs tdocs = r.termDocs(term);
				int temp=0;
				double temp2=0;
				/* iterating over the documents to generate the TF-IDF weights */
				while(tdocs.next())
				{
		
					temp=doc_words.get(tdocs.doc()).get(word);
					temp2=queryNorm.get(word);
				
					/* putting the computed value in a hash map **/
					Double temp3=inter_temp.get(tdocs.doc());
					Double logValue=Math.log((double)r.maxDoc()/ r.docFreq(term));
					if(temp3==null)
					{
						inter_temp.put(tdocs.doc(),temp*temp2*logValue);
					}
					else
					{
						inter_temp.put(tdocs.doc(),inter_temp.get(tdocs.doc())+ (temp*temp2*logValue));
					}			
					
				}
			}
				
			/** generating a key set on the temperory hashmap in order to compute the TF-IDF weights 
			 * 
			 */
				Set<Integer> interKeys=inter_temp.keySet();
				double value,value2=0;
				for(Integer key : interKeys)
				{
					value=doc_norms.get(key);
					value2=inter_temp.get(key)/(value*query_norm);
					final_temp.put(key, value2);
				}
				
				// double endTime=System.nanoTime();
				//System.out.println(endTime-startTime);
				
				 //double start_time=System.nanoTime();
				/* sorting the list in order to generate top 10 documents for a query which has highest TF-IDF weights */
				Set<Entry<Integer, Double>> set =final_temp.entrySet();
		        List<Entry<Integer, Double>> list = new ArrayList<Entry<Integer,Double>>(
		                set);
		        Collections.sort(list, new Comparator<Map.Entry<Integer,Double>>() {
		            public int compare(Map.Entry<Integer, Double> o1,
		                    Map.Entry<Integer, Double> o2) {
		                return o2.getValue().compareTo(o1.getValue());
		            }
		        });
		        int i=0;
		        for (Entry<Integer, Double> entry : list) {
		            System.out.println("Document ID "+entry.getKey() +" TF-IDF weight value "+ entry.getValue());
		            i++;
		            if(i==10)
		            	break;

		        }
		       // double end_Time=System.nanoTime();
				//System.out.println(end_Time-start_time);
				
		        System.out.print("query> ");
		        queryNorm.clear();
		        inter_temp.clear();
		     
		        final_temp.clear();
				
				
			}
			
		}
	

}




	