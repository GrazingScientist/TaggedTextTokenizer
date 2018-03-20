/** University Library of Frankfurt. 2018
* Specialised Information Service Biodiversity Research
*/

package de.unifrankfurt.taggedtexttokenizer;

import de.unifrankfurt.taggedtexttokenizer.TaggedTextTokenizer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.ArrayUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;

public class TaggedTextTokenizerFactory extends TokenizerFactory {
  //private final String tokenizerFactory;
  //private final String analyzerName;
  //private final Map<String, String> tokArgs = new HashMap<String, String>();
  //private final String searchedAttributesFiles;
  
  private HashMap<String, String[]> searchedAttributes = new HashMap<String, String[]>();
  
  public TaggedTextTokenizerFactory(Map<String, String> args) {
    super(args);
    
    String[] attributes = new String[1];
    attributes[0] = "uri";
    this.searchedAttributes.put("species", attributes);
    this.searchedAttributes.put("genus", attributes);
    this.searchedAttributes.put("taxon", attributes);
    this.searchedAttributes.put("location", attributes);
    
    //searchedAttributesFiles = require(args, "searchedAttributes");
    
    //analyzerName = get(args, "analyzer");
    //tokenizerFactory = get(args, "tokenizerFactory");
    
    /*if (analyzerName != null && tokenizerFactory != null) {
      throw new IllegalArgumentException("Analyzer and TokenizerFactory cannot be specified both: "+
                                          analyzerName + " and " + tokenizerFactory);
    }
    
    if (tokenizerFactory != null) {
      tokArgs.put("luceneMatchVersion", getLuceneMatchVersion().toString());
      for (Iterator<String> itr = args.keySet().iterator(); itr.hasNext();) {
        String key = itr.next();
        tokArgs.put(key.replaceAll("^tokenizerFactory\\.", ""), args.get(key));
        itr.remove();
      }
    }*/
    
    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
  }
  
  @Override
  public TaggedTextTokenizer create(AttributeFactory factory) {
    return new TaggedTextTokenizer(factory, searchedAttributes);
  }
  
  /*@Override
  public void inform(ResourceLoader loader) throws IOException {
    searchedAttributes = loadAttributes();
  }

  private HashMap<String, String[]> loadAttributes() throws IOException {
    HashMap<String, String[]> outputMap = new HashMap<String, String[]>();
    
    List<String> files = splitFileNames(searchedAttributesFiles);
    for (String file : files) {
      outputMap.putAll(parseAttributes(file));
    }
    
    return outputMap;
  }
  
  private HashMap<String, String[]> parseAttributes(String attributeFile) throws IOException {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(attributeFile));
    } catch (IOException e) {
      throw new IOException("Error parsing attribute file:", e);
    }
    
    HashMap<String, String[]> outputMap = new HashMap<String, String[]>();
    
    while (reader.ready()) {
      String line = reader.readLine();
      
      String[] lineArray = line.split(",");
      String tag = lineArray[0];
      lineArray = (String[]) ArrayUtils.removeElement(lineArray, tag);
      
      outputMap.put(tag, lineArray);
    }
    
    return outputMap;
  }*/
}
