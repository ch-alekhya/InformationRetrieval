package edu.asu.irs13;

import java.io.*;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
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

public class ClusterMeans {

	static HashMap<Integer, Double> doc_norms = new HashMap<Integer, Double>();
	static HashMap<Integer, HashMap<String, Integer>> doc_words = new HashMap<Integer, HashMap<String, Integer>>();
	static int targetdocid;  //target docid for similarity analysis

	public static void main(String[] args) throws Exception {
		initial();
		userQuery();
	

	}

	public static void initial() throws Exception {
		doc_words = TfIDF3.docWord(); //doc_Words to store words in each dcoument 
		doc_norms = TfIDF3.docNorms(); //doc _norms to store all the doc_norms 
		
	}

	public static void userQuery() throws Exception {
		
		HashMap<Integer, Double> results = new HashMap<Integer, Double>(); //takes the inpuyt from the user 
		Scanner scan = new Scanner(System.in);
		System.out.println("Enter the value of document set ");
		int K = scan.nextInt();
		String str = "";
		str = scan.nextLine();
		System.out.print("query> "); /* takes input from the user **/
		long starttime=System.nanoTime();
		while (!(str = scan.nextLine()).equals("quit")) {
			results = TfIDF3.idfInputK(str,
					K); /*
						 * takes top 10 documents of tf-idf vector similarity
						 * for a given query
						 **/

			Set<Entry<Integer, Double>> set = results.entrySet(); //gets entire list of documents having tf-idf values 
			List<Entry<Integer, Double>> list = new ArrayList<Entry<Integer, Double>>(set);
			Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
				public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
					return o2.getValue().compareTo(o1.getValue());
				}
			}); // sort the documents based on the value of tf-idf
			HashMap<Integer, Double> results_temp = new HashMap<Integer, Double>();

			int i = 0;
			//store them in a hahsmap as key value pair and if count is K break;
			for (Entry<Integer, Double> entry : list) {
				i++;
				results_temp.put(entry.getKey(), entry.getValue());
				// System.out.println(entry.getKey() + " " + entry.getValue());
				if (i == K)
					break;
			}
			// kmeans(results_temp);
			// clusterComputation(3,results);
			HashSet<String> allTerms = getTermSet(results_temp);
			updateDocTerms(allTerms, results_temp);
			long endtime=System.nanoTime();
			
			System.out.println("time taken for query is "+ (endtime-starttime)+"\n\n\n");
			System.out.print("query> ");

		}
		scan.close();

	}

	//gets all the terms in top 50 document  and returns as hahsset 
	public static HashSet<String> getTermSet(HashMap<Integer, Double> results) {
		//System.out.println("Document set size is " + results.size());
		Set<Integer> keys = results.keySet();
		HashSet<String> allTerms = new HashSet<String>();
		for (Integer key : keys) {
			HashMap<String, Integer> term_freq = doc_words.get(key);
			Set<String> terms = term_freq.keySet();
			for (String term : terms) {
				if (!allTerms.contains(term))
					allTerms.add(term);
			}

		}

		return allTerms;
	}
// update all the documents with all terms present in the top 50 document set
	public static void updateDocTerms(HashSet<String> allTerms, HashMap<Integer, Double> results)
			throws CorruptIndexException, IOException {
		for (Integer key : results.keySet()) {
			HashMap<String, Integer> doc_terms = doc_words.get(key);
			for (String term : allTerms) {
				if (!doc_terms.containsKey(term)) {
					doc_terms.put(term, 0);
					doc_words.put(key, doc_terms);
				}
			}
		}
	
		HashMap<Integer, HashMap<String, Double>> updatedDoc_words = tfIdfUpatedDocValues(results);
		//for(int i=3;i<=10;i++)
		//{
			//System.out.println(i+" " +targetdocid);
			initialCluster(updatedDoc_words, results, 3);
		//}
			//System.out.println("printing documents belonging to various algo");
		//initialCluster2(updatedDoc_words, results, 10);
		
	}

	//gets the updated docs with value tf-idf values 
	public static HashMap<Integer, HashMap<String, Double>> tfIdfUpatedDocValues(HashMap<Integer, Double> results)
			throws CorruptIndexException, IOException {
		IndexReader r = IndexReader.open(FSDirectory.open(new File("index")));
		HashMap<Integer, HashMap<String, Double>> updatedDoc_words = new HashMap<Integer, HashMap<String, Double>>();
		int id=0;
		for (Integer key : results.keySet()) {
			id++;
			if(id==10)
				targetdocid=key;
			HashMap<String, Integer> temp_values = doc_words.get(key);
			HashMap<String, Double> updated_values = new HashMap<String, Double>();
			int max = findMaxValue(temp_values);
			//System.out.println(key+"  "+max);
			for (String str : temp_values.keySet()) {
				Term term = new Term("contents", str);

				int value = temp_values.get(str);
				double tf = ((double) value / max) * Math.log((double) r.maxDoc() / r.docFreq(term));
				// System.out.println(tf);;

				updated_values.put(str, tf);
			}
			updatedDoc_words.put(key, updated_values);
		}
		return updatedDoc_words;

	}
//find the max value of term frequency of word in a given document 
	public static int findMaxValue(HashMap<String, Integer> temp_values) {
		int max = Integer.MIN_VALUE;

		for (String str : temp_values.keySet()) {
			int temp = temp_values.get(str);
			if (temp >= max)
				max = temp;
		}
		return max;

	}

	public static void printUpdatedDocs(HashMap<Integer, HashMap<String, Double>> results) {

		/***for (Integer key : results.keySet()) {
			System.out.println(results.get(key));

		}
		 System.out.println(results.size());***/
	}

	public static void initialCluster2(HashMap<Integer, HashMap<String, Double>> updatedDocs,
			HashMap<Integer, Double> results, int count) throws CorruptIndexException, IOException {

		HashMap<Integer, ArrayList<Integer>> clusters = new HashMap<Integer, ArrayList<Integer>>();
		ArrayList<Integer> doc_list = new ArrayList<Integer>();

		int clustcount=0;
		for(Integer key:results.keySet())
		{
			doc_list.add(key);
			if(clusters.get(clustcount)==null)
			{
				ArrayList<Integer> doclists = new ArrayList<Integer>(doc_list);
			    clusters.put(clustcount, doclists);
			}
			else
			{
				ArrayList<Integer>doclists=clusters.get(clustcount);
				doclists.add(key);
				clusters.put(clustcount, doclists);
			}
			clustcount++;
			if(clustcount==count-1)
				clustcount=0;
			doc_list.clear();
			
		}		
	/***	for(Integer key:clusters.keySet())
		{
			System.out.println(key+"  "+clusters.get(key));
		}***/

		centroidComputation2(clusters, updatedDocs,results);
	}

	public static void centroidComputation2(HashMap<Integer, ArrayList<Integer>> clusters,
			HashMap<Integer, HashMap<String, Double>> updatedDocs, HashMap<Integer,Double> results) throws CorruptIndexException, IOException {
		HashMap<Integer, HashMap<String, Double>> centroids = new HashMap<Integer, HashMap<String, Double>>();
		HashMap<String, Double> temp_points = new HashMap<String, Double>();
			
		int docCount=0;
		for(Integer key:results.keySet())
		{
			HashMap<String,Double>temp_cent=new HashMap<String,Double>(updatedDocs.get(key));
			centroids.put(docCount, temp_cent);
			if(docCount==clusters.keySet().size())
				break;
			docCount++;
			
		}
		
		/***for(Integer key:centroids.keySet())
		{
		System.out.println(centroids.get(key));
		}***/
		int count = 0;
		while (true) {
			count++;
			if(count==100)
				break;
			HashMap<Integer, HashMap<String, Double>> pre_centroids = new HashMap<Integer, HashMap<String, Double>>(
					centroids);			
			HashMap<Integer,ArrayList<Integer>> pre_clusters=new HashMap<Integer,ArrayList<Integer>>(clusters);
				clusters.clear();

			
			for (Integer key : updatedDocs.keySet()) {

				ArrayList<Double> distances = new ArrayList<Double>();
				for (Integer cent : centroids.keySet()) {
						double dist = cosineSimilarity(updatedDocs.get(key), centroids.get(cent));
						distances.add(dist);

				}

				int maxIndex = distances.indexOf(Collections.max(distances));

				if (clusters.get(maxIndex)==null) {
					ArrayList<Integer> newlist = new ArrayList<Integer>();
					newlist.add(key);
					clusters.put(maxIndex, newlist);
				} else {
					ArrayList<Integer> newlist = clusters.get(maxIndex);
					newlist.add(key);
					clusters.put(maxIndex, newlist);

				}

			}
			
			centroids.clear();
			centroids = newCentroidComputation(clusters, updatedDocs);
			
			//System.out.println(pre_clusters+"\n\n\n");
	
			boolean convergance=false;
			int clustercount=0;
			for(Integer key: clusters.keySet())
			{
				if (clusters.get(key) != null && pre_clusters.get(key) != null) {
					Collections.sort(clusters.get(key));
					Collections.sort(pre_clusters.get(key));
					if (clusters.get(key).equals(pre_clusters.get(key))) {

						clustercount++;
						convergance = true;
					} else {
						convergance = false;
						break;
					}
				} else
					clustercount++;
			}
			//System.out.println("printing cluster count"+clustercount);
			if(clustercount==clusters.keySet().size())
				break;
					}
		System.out.println("printing the count"+ " "+ count);

		printclusters(clusters,results);
		//System.out.println("printing the intracluster similarity -roundrobin");
		//intraClusterSimilarity(updatedDocs,clusters,centroids);
		//System.out.println("printing the intercluster disimilarity -roundrobin");
		//interClusterDisSimilarity(clusters,centroids);
		//topKeyWords(updatedDocs,clusters);
	}
	//this method computes the similarity between the centroid and the documents 
	public static void initialCluster(HashMap<Integer, HashMap<String, Double>> updatedDocs,
			HashMap<Integer, Double> results, int count) throws CorruptIndexException, IOException {

		int division = (results.size() / count) -1 ;
		//System.out.println(division);
		HashMap<Integer, ArrayList<Integer>> clusters = new HashMap<Integer, ArrayList<Integer>>();
		ArrayList<Integer> doc_list = new ArrayList<Integer>();

		int cluster_count = 0;
		int var = 0;
		int keysize = results.size();
		int size = 0;
		for (Integer key : results.keySet()) {
			size++;
			doc_list.add(key);
			var++;
			if (var == division && cluster_count < (count - 1)) {
				ArrayList<Integer> doclists = new ArrayList<Integer>(doc_list);
				clusters.put(cluster_count, doclists);
				// System.out.println(clusters.get(cluster_count));
				var = 0;
				doc_list.clear();
				cluster_count++;
			} else if (size == keysize) {
				ArrayList<Integer> doclists = new ArrayList<Integer>(doc_list);
				clusters.put(cluster_count, doclists);
				// System.out.println(clusters.get(cluster_count));
				var = 0;
				doc_list.clear();
				cluster_count++;

			}
		}
		
		//System.out.println(clusters);
		

		centroidComputation(clusters, updatedDocs, results);
	}

	//compute the centroid for the each set of clusters 
	public static void centroidComputation(HashMap<Integer, ArrayList<Integer>> clusters,
			HashMap<Integer, HashMap<String, Double>> updatedDocs, HashMap<Integer,Double>results) throws CorruptIndexException, IOException {
		HashMap<Integer, HashMap<String, Double>> centroids = new HashMap<Integer, HashMap<String, Double>>();
		HashMap<String, Double> temp_points = new HashMap<String, Double>();
		int size = 0;

		for (Integer key : updatedDocs.keySet()) {
			HashMap<String, Double> termssize = updatedDocs.get(key);
			Set<String> terms = termssize.keySet();
			size = terms.size();
			break;

		}
		
		
		for (Integer key : clusters.keySet()) {
			ArrayList<Integer> docslist = clusters.get(key);
			for (Integer docId : docslist) {
				HashMap<String, Double> terms = updatedDocs.get(docId);
				for (String str : terms.keySet()) {
					if (!temp_points.containsKey(str)) {
						temp_points.put(str, terms.get(str));
					} else {
						temp_points.put(str, terms.get(str) + temp_points.get(str));
					}
				}

			}
			for (String str : temp_points.keySet()) // the values are not in
													// integers they are tf-idf
													// values hence the division
													// in sizes results order of
													// 10 -4
			{
				double value = (double) temp_points.get(str) / size;
				temp_points.put(str, value);

			}
			HashMap<String, Double> clusterPoints = new HashMap<String, Double>(temp_points);
			centroids.put(key, clusterPoints);
			temp_points.clear();
		}

		
		int count = 0;
		while (true) {
			count++;
			if(count==100)
				break;
			HashMap<Integer, HashMap<String, Double>> pre_centroids = new HashMap<Integer, HashMap<String, Double>>(
					centroids);			
			HashMap<Integer,ArrayList<Integer>> pre_clusters=new HashMap<Integer,ArrayList<Integer>>(clusters);
			//System.out.println("printing the old values of clusters");
			//System.out.println(pre_clusters);

			// System.out.println(clusters);
			clusters.clear();

			
			for (Integer key : updatedDocs.keySet()) {

				ArrayList<Double> distances = new ArrayList<Double>();
				for (Integer cent : centroids.keySet()) {
					//if (!centroids.get(cent).isEmpty()) {
						double dist = cosineSimilarity(updatedDocs.get(key), centroids.get(cent));
						distances.add(dist);
					//}

				}

				int maxIndex = distances.indexOf(Collections.max(distances));

				if (clusters.get(maxIndex)==null) {
					ArrayList<Integer> newlist = new ArrayList<Integer>();
					newlist.add(key);
					clusters.put(maxIndex, newlist);
				} else {
					ArrayList<Integer> newlist = clusters.get(maxIndex);
					newlist.add(key);
					clusters.put(maxIndex, newlist);

				}

			}
			
			centroids.clear();
			centroids = newCentroidComputation(clusters, updatedDocs);
			
			boolean convergance=false;
			int clustercount=0;
			// check for convergence for previous list is matching with current lkist or not 
			for(Integer key: clusters.keySet())
			{
				if(clusters.get(key)!=null && pre_clusters.get(key)!=null)
				{
				Collections.sort(clusters.get(key));
				Collections.sort(pre_clusters.get(key));
				//System.out.println(clusters.get(key));
				//System.out.println(pre_clusters.get(key));
				if(clusters.get(key).equals(pre_clusters.get(key)))
				{
						
						clustercount++;
						convergance=true;
				}
				else 
				{	convergance=false;
					break;
				}
				}
				else
					clustercount++;
			}
			if(clustercount==clusters.keySet().size())
				break;
					}
		
		System.out.println("printing the count"+ " "+ count);

		printclusters(clusters,results);
		//singleDocSimilarity(updatedDocs,clusters,centroids);
		//System.out.println("printing intra cluster similarity-with top 15");
		//intraClusterSimilarity(updatedDocs,clusters,centroids);
		//System.out.println("printing inter cluster dissimilarity-with top 15");
		//interClusterDisSimilarity(clusters,centroids);
		System.out.println("printing top words of all clusters");
		//topKeyWords(updatedDocs,clusters);

	}
//computes new centroid for ecah iteration formed clusters 
	public static HashMap<Integer, HashMap<String, Double>> newCentroidComputation(
			HashMap<Integer, ArrayList<Integer>> clusters, HashMap<Integer, HashMap<String, Double>> updatedDocs) {
		HashMap<Integer, HashMap<String, Double>> centroids = new HashMap<Integer, HashMap<String, Double>>();
		HashMap<String, Double> temp_points = new HashMap<String, Double>();
		int size = 0;

		for (Integer key : updatedDocs.keySet()) {
			HashMap<String, Double> termssize = updatedDocs.get(key);
			Set<String> terms = termssize.keySet();
			size = terms.size();
			break;

		}

		for (Integer key : clusters.keySet()) {
			ArrayList<Integer> docslist = clusters.get(key);
			for (Integer docId : docslist) {
				HashMap<String, Double> terms = updatedDocs.get(docId);
				for (String str : terms.keySet()) {
					if (!temp_points.containsKey(str)) {
						temp_points.put(str, terms.get(str));
					} else {
						temp_points.put(str, terms.get(str) + temp_points.get(str));
					}
				}

			}
			for (String str : temp_points.keySet()) // the values are not in
													// integers they are tf-idf
													// values hence the division
													// in sizes results order of
													// 10 -4
			{
				double value = (double) temp_points.get(str) / size;
				temp_points.put(str, value);

			}
			HashMap<String, Double> clusterPoints = new HashMap<String, Double>(temp_points);
			centroids.put(key, clusterPoints);
			temp_points.clear();
		}
		return centroids;

	}
	
	// compares two arraylist in order to check for convergence
	public static boolean compareTwoArryList(ArrayList<Integer>cluster,ArrayList<Integer>pre_cluster)
	{
		List<Integer> sourceList = new ArrayList<Integer>(cluster);
	    List<Integer> destinationList = new ArrayList<Integer>(pre_cluster);

        if(cluster.isEmpty()|| pre_cluster.isEmpty())
        	return false;
	    sourceList.removeAll( pre_cluster );
	    System.out.println("printing the sourceList");
	    System.out.println(sourceList);
	    destinationList.removeAll( cluster );
		
	    System.out.println("printing the destinationList");
	    System.out.println(destinationList);
	   
	    if(sourceList.isEmpty() && destinationList.isEmpty())
			return true;
		return false;
	}

	//similarity using eculideaindistance
	public static double eculideanDistance(HashMap<String, Double> docId, HashMap<String, Double> cent) {
		double dist = 0.0;

		if (!cent.isEmpty() && !docId.isEmpty()) {
			for (String str : docId.keySet()) {
			  dist += Math.pow(docId.get(str) - cent.get(str), 2);
			}
		}

		dist = Math.sqrt(dist);
		return dist;

	}
	
	//similarity using cosine similarity
	public static double cosineSimilarity(HashMap<String, Double> docId, HashMap<String, Double> cent) {
		double dist = 0.0;
        double docIdnorm=0.0;
        double centnorm=0.0;
		if (!cent.isEmpty() && !docId.isEmpty()) {
			for (String str : docId.keySet()) {
			  dist += docId.get(str)*cent.get(str);
			  docIdnorm+=Math.pow(docId.get(str), 2);
			  centnorm+=Math.pow(cent.get(str), 2);
			}
		}

		docIdnorm = Math.sqrt(docIdnorm);
		centnorm=Math.sqrt(centnorm);
		if(docIdnorm !=0 && centnorm !=0)
			dist=(double)dist/(docIdnorm*centnorm);
		return dist;

	}
	
	//printing clusters along with urls's

	public static void printclusters(HashMap<Integer, ArrayList<Integer>> clusters, HashMap<Integer, Double> results)
			throws CorruptIndexException, IOException {
		IndexReader r = IndexReader.open(FSDirectory.open(new File("index")));

		/***for (Integer key : clusters.keySet()) {
			System.out.println("printing documents in cliuster "+ key);
			HashMap<Integer, Double> docid_values = new HashMap<Integer, Double>();
			ArrayList<Integer> docs = clusters.get(key);
			for (Integer id : docs) {
				Double tfvalue = results.get(id);
				docid_values.put(id, tfvalue);
			}
			Set<Entry<Integer, Double>> set = docid_values.entrySet();
			List<Entry<Integer, Double>> list = new ArrayList<Entry<Integer, Double>>(set);
			Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
				public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
					return o2.getValue().compareTo(o1.getValue());
				}
			});
			for (Entry<Integer, Double> entry : list) {
				Document d = r.document(entry.getKey());
				String url = d.getFieldable("path").stringValue();
				System.out.println(entry.getKey() + "  " + url.replace("%%", "/") +" "+entry.getValue());

			}
			
			System.out.println("---------------------------------------------\n\n");

		}***/
		
		for(Integer key:clusters.keySet()){
			System.out.println("printing documents in cluster "+ key);
			ArrayList<Integer> docs=clusters.get(key);
			
			for(Integer id:docs)
			{
				Document d = r.document(id);
				String url = d.getFieldable("path").stringValue();
				System.out.println(id + "  " + url.replace("%%", "/"));
				
			}
			
			System.out.println("---------------------------------------------\n\n");

		}

	}
//computer intra cluster similarity between centroid and the document list
	public static void intraClusterSimilarity(HashMap<Integer,HashMap<String,Double>> updatedDocs, HashMap<Integer,ArrayList<Integer>> clusters,HashMap<Integer,HashMap<String,Double>> centroids)
	{
		HashMap<Integer,Double>simvalues=new HashMap<Integer,Double>();
		for(Integer key:clusters.keySet())
		{
			int clustSize=clusters.get(key).size();
			double clustsim=0.0;
			HashMap<String,Double> cent=centroids.get(key);
			for(Integer id:clusters.get(key))
			{
				double docsim=0.0;
				double docnorm=0.0;
				double centnorm=0.0;
				
				for(String str:updatedDocs.get(id).keySet())
				{
					docsim+=updatedDocs.get(id).get(str)*cent.get(str);
					docnorm+=Math.pow(updatedDocs.get(id).get(str), 2);
					centnorm+=Math.pow(cent.get(str), 2);
					
				}
		     docnorm=Math.sqrt(docnorm);
		     centnorm=Math.sqrt(centnorm);
		     docsim=(double)docsim/(docnorm*centnorm);
		     clustsim+=docsim;
		     
				
			}
			clustsim=(double)clustsim/clustSize;
			simvalues.put(key, clustsim);
		}
		
		double overallsim=0.0;
		System.out.println("printing the similarity between the clusters documents");
		for(Integer key: simvalues.keySet())
		{
			System.out.println(simvalues.get(key));
			overallsim+=simvalues.get(key);
		}
		System.out.println("printing average similarity values of the clusters ");
		overallsim=(double)overallsim/(simvalues.keySet().size());
		System.out.println(overallsim);
		
	}
	//computers similarity between centroids of various clusters 
	public static void interClusterDisSimilarity(HashMap<Integer,ArrayList<Integer>> clusters,HashMap<Integer,HashMap<String,Double>>centroids )
	{
		HashMap<Integer,Double>clustSim=new HashMap<Integer,Double>();
		
		int clustsize=clusters.keySet().size();
		double centsim=0.0;
		for(int i=0 ; i<clustsize;i++)
		{
			double clustsim=0.0;
			for(int j=0;j<clustsize ;j++)
			{
			
				double clust1norm=0.0;
				double clust2norm=0.0;
				double tempsim=0.0;
				if(i!=j)
				{
					for(String str:centroids.get(j).keySet())
					{
						
						tempsim+=centroids.get(j).get(str)*centroids.get(i).get(str);
						clust1norm=Math.pow(centroids.get(i).get(str),2);
						clust2norm=Math.pow(centroids.get(j).get(str),2);
						
					}
					
				}
				if(clust1norm!=0.0 && clust2norm!=0.0)
					tempsim=(double)clustsim/Math.sqrt(clust1norm*clust2norm);
				clustsim+=tempsim;
				
			}
			clustSim.put(i,clustsim);
			
			centsim+=clustsim;
		}
		System.out.println("pinting dissimilarity among clusters");
		centsim=(double)(2*centsim)/(clustsize*(clustsize-1));
		for(Integer key:clustSim.keySet())
		{
			System.out.println(clustSim.get(key));
		}
		System.out.println("printing the average of cluster disimilarity");
		System.out.println(centsim);
	}
	public static void topKeyWords(HashMap<Integer,HashMap<String,Double>>updatedDocs,HashMap<Integer,ArrayList<Integer>> clusters )
	{
		for(Integer key:clusters.keySet())
		{
			ArrayList<Integer> docid=clusters.get(key);
			HashMap<String,Double>termsValues=new HashMap<String,Double>();
			for(Integer id:docid)
			{
				for(String str:updatedDocs.get(id).keySet())
				{
					if(updatedDocs.get(id).get(str)!=0.0)
					{
						double temp=updatedDocs.get(id).get(str);
						if(termsValues.containsKey(str))
						{
							double value=termsValues.get(str);
							if(value<temp)
								termsValues.put(str, temp);
						}
						else
						{
							termsValues.put(str,temp);
						}
							
					}
				}
			}
			
			Set<Entry<String, Double>> set= termsValues.entrySet();
	        List<Entry<String, Double>> list = new ArrayList<Entry<String,Double>>(
	                set);
	        Collections.sort(list, new Comparator<Map.Entry<String,Double>>() {
	            public int compare(Map.Entry<String, Double> o1,
	                    Map.Entry<String, Double> o2) {
	                return o2.getValue().compareTo(o1.getValue());
	            }
	        });
	        System.out.println("printing top 50 words of each cluster");
	        int i=0;
	        for (Entry<String, Double> entry : list) {
	        	i++; 
	        	System.out.println(entry.getKey());
	        	if(i==50)
	        		break;
	        }
		}
		
	}

	public static void singleDocSimilarity(HashMap<Integer,HashMap<String,Double>> updatedDocs,HashMap<Integer,ArrayList<Integer>> clusters, HashMap<Integer,HashMap<String,Double>> centroids )
	{
		Integer clusterid = 0;
		for(Integer key : clusters.keySet())
			if(clusters.get(key).contains(targetdocid))
			{
				clusterid=key;
				break;
			}
		
		HashMap<String,Double> centid=centroids.get(clusterid);
		HashMap<String,Double> docterms=updatedDocs.get(targetdocid);
		double sim=0.0;
		double docnorm=0.0;
		double centnorm=0.0;
		for(String str: centid.keySet())
		{
			sim+=centid.get(str)*docterms.get(str);
			docnorm+=Math.pow(docterms.get(str), 2);
			centnorm+=Math.pow(centid.get(str), 2);
		}
		docnorm=Math.sqrt(docnorm);
		centnorm=Math.sqrt(centnorm);
		sim=sim/(docnorm*centnorm);
		System.out.println("printing doc and cluster similarity");
		System.out.println(sim);
	}
}
