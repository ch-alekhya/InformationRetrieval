package edu.asu.irs13;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.FSDirectory;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JTextField;
import java.awt.ScrollPane;
import javax.swing.JLabel;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.Font;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.awt.event.ActionEvent;
import javax.swing.JTextArea;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
public class UI extends JFrame {

	private JPanel contentPane;
	private JTextField userText;
	private JLabel jl;
	static HashMap<Integer, HashMap<String, Integer>> doc_words = new HashMap<Integer, HashMap<String, Integer>>();
	static HashMap<Integer,Double> doc_norms=new HashMap<Integer,Double>();
	private JTextArea textArea;
	private JTextArea textArea_1;
    
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UI frame = new UI();
					frame.setVisible(true);
					docWord();
					docNorms();
					
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	//gets the doc words and doc terms for the given query 
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
	 * Create the frame.
	 */
	public UI() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(0, 0, 1500, 900);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		
		userText = new JTextField("query",30);
		jl=new JLabel();
		textArea_1 = new JTextArea();
		userText.setFont(new Font("Tahoma", Font.PLAIN, 13));
		userText.setColumns(10);
		
		userText.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						String input=userText.getText();
						jl.setText(input);
					}
				});
		contentPane.add(jl);
		//getContentPane().add(contentPane);
		
		
		JButton btnNewButton = new JButton("Search");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				long starttime=System.nanoTime();
				String input=userText.getText();
				try {
					HashMap<Integer,String> results=idfInputK(input,10);
					String output="";
					String output2="";
					for(Integer key:results.keySet())
					{
						String urlresult=getUrl(results.get(key).replace("%%", "/"));
						output+=results.get(key).replace("%%", "/")+"\n";
						if(urlresult.equals("page"))
						{
						  output="page not found+\n+printing top tf-idf words in document\n";
							//output+=results.get(key)+"\n";
						HashMap<String,Integer>terms=doc_words.get(key);
						Set<Entry<String, Integer>> set =terms.entrySet();
						 List<Entry<String, Integer>> list = new ArrayList<Entry<String,Integer>>(set);
					        Collections.sort(list, new Comparator<Map.Entry<String,Integer>>() {
					            public int compare(Map.Entry<String, Integer> o1,
					                    Map.Entry<String, Integer> o2) {
					                return o2.getValue().compareTo(o1.getValue());
					            }
					        });
					        int j=0;
					        for (Entry<String,Integer> entry : list) {
					        	if(entry.getKey().length()>3)
					        	{
					        	j++; 
					        	output+=entry.getKey()+" ";
					        	//System.out.println(entry.getKey()+ " " +entry.getValue());
					        	if(j==10)
					        		 break;
					        	}
					        }
					       output2+=results.get(key).replace("%%", "/")+"\n"+output+"\n\n\n";
						}
						else
						{
							int j=0;
							String ans="";
							for(char out:urlresult.toCharArray())
							{
								ans+=out;
								j++;
								if(j==99)
								{
									ans+="\n";
									j++;
								}
								else if(j==197)
									break;
							}
							ans+="...";
						//String mainout=urlresult+"\n\n\n";
						output2+=results.get(key).replace("%%", "/")+"\n"+ans+"\n\n\n";
						}
					
					}
					
					textArea_1.setText(output2);
					long endtime=System.nanoTime();
					System.out.println("time taken is " +(endtime-starttime));
					
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
			}
		});
		
		
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
						.addGroup(Alignment.TRAILING, gl_contentPane.createSequentialGroup()
							.addComponent(userText, GroupLayout.PREFERRED_SIZE, 291, GroupLayout.PREFERRED_SIZE)
							.addGap(33)
							.addComponent(btnNewButton)
							.addGap(410))
						.addComponent(textArea_1, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 960, Short.MAX_VALUE)))
		);
		gl_contentPane.setVerticalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addGap(61)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(userText, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(btnNewButton))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(textArea_1, GroupLayout.DEFAULT_SIZE, 544, Short.MAX_VALUE))
		);
		contentPane.setLayout(gl_contentPane);
	}
	public JTextField getTextField() {
		return userText;
	}
	public void setTextField(JTextField userText)
	{
		//textField.setText(userText);
	}
	public static HashMap<Integer, String> idfInputK(String str, int K) throws Exception{
		
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
			        	//System.out.println(entry.getKey()+ " " +entry.getValue());
			        	if(i==K)
			        		 break;
			        }
			        queryNorm.clear();
			        inter_temp.clear();
			        final_temp.clear();
			        HashMap<Integer,String>docUrl=new HashMap<Integer,String>();
			        for (Integer docid : return_temp.keySet()){
						Document d = r.document(docid);
						String url = d.getFieldable("path").stringValue();
						docUrl.put(docid,url);
			        }
			        return docUrl;
			}
	
	@SuppressWarnings("finally")
	//get the url form the given index file and generates snippets 
	public static String getUrl(String url)
	{
		boolean ex=false;
		StringBuffer sb=new StringBuffer();
		try{
			org.jsoup.nodes.Document doc=Jsoup.connect("http://"+url).get();
			Elements ele=doc.body().select("p");
			int i=0;
			for (Element element : ele) {
				//System.out.println(ele.text());
				sb.append(element.ownText());
				if(i++>3)
					break;
			}
			
		}
		catch (Exception e)
		{
			ex=true;
		}
		finally{
			if(ex)
				return "page";
			return sb.toString();
		}
	}
}
