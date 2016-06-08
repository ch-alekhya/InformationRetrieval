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

public class TfIDFIndex {
	static HashMap<Integer, HashMap<String, Integer>> doc_words = new HashMap<Integer, HashMap<String, Integer>>();
	static HashMap<Integer,Double> doc_norms=new HashMap<Integer,Double>();
	public static void main(String[] args) throws Exception {
		docWord();  /* this functions generates DOCID , <Word,frequency> hash map*/
		docNorms(); /* this functions generates DOCID , DOC norm */
		
		HashMap<Integer,Double> results=new HashMap<Integer,Double>();
		results=idfInput(""); /* used to calculate TF-IDF weight for a query */
	
		
	}
	
	public static HashMap<Integer,HashMap<String,Integer>> docWord() throws Exception{
		 
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
		return doc_words;
	
}
	

public static HashMap<Integer, Double> docNorms() throws Exception{
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
	
	return doc_norms;
}



/**
 * the following method idfInput() generates TF-IDF weights for the query based on pre computed document norms 
 * @throws Exception
 */
public static HashMap<Integer,Double> idfInput(String str) throws Exception{
	
	/** creating an index for the documents present */
	IndexReader r = IndexReader.open(FSDirectory.open(new File("index")));
	
	
	//System.out.println("Generating TF-IDF weights for the given queries");
		

		HashMap<String,Integer> queryNorm=new HashMap<String,Integer>();
		HashMap<Integer,Double> inter_temp=new HashMap<Integer,Double>();
		HashMap<Integer,Double> final_temp=new HashMap<Integer,Double>();
		HashMap<Integer,Double> return_temp=new HashMap<Integer,Double>();
		
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
				double value,value2,normalvalue=0;
				for(Integer key : interKeys)
				{
					value=doc_norms.get(key);
					value2=inter_temp.get(key)/(value*query_norm);
					inter_temp.put(key,value2);
					normalvalue+=value2;
				}
				for(Integer key:interKeys)
				{
					value=inter_temp.get(key);
					value=value/normalvalue;
					final_temp.put(key, value);
				}
				System.out.println("printing the vector similarity size  "+final_temp.size());
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
		        
		        for (Entry<Integer, Double> entry : list) {
		        	 return_temp.put(entry.getKey(), entry.getValue());
		        	 
		        }
		        queryNorm.clear();
		        inter_temp.clear();
		        final_temp.clear();
		        return return_temp;
		}
	
public static HashMap<Integer,Double> idfInputK(String str) throws Exception{
	
	/** creating an index for the documents present */
	IndexReader r = IndexReader.open(FSDirectory.open(new File("index")));
	
	
	//System.out.println("Generating TF-IDF weights for the given queries");
		

		HashMap<String,Integer> queryNorm=new HashMap<String,Integer>();
		HashMap<Integer,Double> inter_temp=new HashMap<Integer,Double>();
		HashMap<Integer,Double> final_temp=new HashMap<Integer,Double>();
		HashMap<Integer,Double> return_temp=new HashMap<Integer,Double>();
		
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
				double value,value2,normalvalue=0;
				for(Integer key : interKeys)
				{
					value=doc_norms.get(key);
					value2=inter_temp.get(key)/(value*query_norm);
					inter_temp.put(key,value2);
					normalvalue+=value2;
				}
				for(Integer key:interKeys)
				{
					value=inter_temp.get(key);
					value=value/normalvalue;
					final_temp.put(key, value);
				}
				System.out.println("printing the vector similarity size  "+final_temp.size());
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
		        	i++; 
		        	return_temp.put(entry.getKey(), entry.getValue());
		        	 if(i==10)
		        		 break;
		        }
		        queryNorm.clear();
		        inter_temp.clear();
		        final_temp.clear();
		        return return_temp;
		}
	


}




	