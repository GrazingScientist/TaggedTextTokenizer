/** University Library of Frankfurt. 2018
* Specialised Information Service Biodiversity Research
*/

package de.unifrankfurt.taggedtexttokenizer;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import de.unifrankfurt.taggedtexttokenizer.TaggedTextTokenizer;

public class TestTaggedTextTokenizer extends BaseTokenStreamTestCase {
  
  HashMap<String, String[]> testSearchedAttributes = new HashMap<String, String[]>();
  
  String simpleXmlString = "<doc>The tree <species uri='720243'><genus uri='fag394'>Fagus</genus> "
      + "<taxon uri='spec9574'>sylvatica</taxon></species> could not be found in <location "
      + "uri='loc67567'>London</location>, but in <location uri='loc7g68'>Frankfurt</location>."
      + "</doc>";
  String incompleteSimpleXmlString = "The tree <species uri='720243'><genus uri='fag394'>Fagus"
      + "</genus> <taxon uri='spec9574'>sylvatica</taxon></species> could not be found in "
      + "<location uri='loc67567'>London</location>, but in <location uri='loc7g68'>Frankfurt"
      + "</location>.";
  String missingAttributeXmlString = "The tree <species uri='720243'><genus uri='fag394'>Fagus"
      + "</genus> <taxon uri=''>sylvatica</taxon></species> could not be found in <location "
      + "uri=''>London</location>, but in <location uri='loc7g68'>Frankfurt</location>.";
    
  
  /** Test TaggedTextTokenizer. */
  public void testTaggedTextTokenizer() throws Exception {
    Tokenizer stream = getTaggedTextTokenizer(simpleXmlString, false);
    
    assertTokenStreamContents(stream,
        new String[]{"The", "tree", "Fagus", "sylvatica", "could", "not", "be", "found", "in",
                     "London", "but", "in", "Frankfurt"},
        new int[] {0, 4, 9, 15, 25, 31, 35, 38, 44, 47, 55, 59, 62}, //start offset
        new int[] {3, 8, 14, 24, 30, 34, 37, 43, 46, 53, 58, 61, 71} // end offset
    );
  }

  /** Test the insertion of attributes into the token stream. */
  public void testAttributeInserting() throws Exception {
    Tokenizer stream = getTaggedTextTokenizer(simpleXmlString, true);
    
    assertTokenStreamContents(stream,
        new String[]{"The", "tree", "fag394", "720243", "Fagus", "spec9574", "sylvatica", "could",
            "not", "be", "found", "in", "loc67567", "London", "but", "in", "loc7g68", "Frankfurt"},
        //start offset
        new int[] {0, 4, 9, 9, 9, 15, 15, 25, 31, 35, 38, 44, 47, 47, 55, 59, 62, 62},
        // end offset
        new int[] {3, 8, 14, 24, 14, 24, 24, 30, 34, 37, 43, 46, 53, 53, 58, 61, 71, 71},
        //type
        new String[] {"word", "word", "URI", "URI", "word", "URI", "word", "word", "word", "word",
            "word", "word", "URI", "word", "word", "word", "URI", "word"},
        //posIncr
        new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
    );
  }
  
  /** Test for the input of an XML without a root tag. */
  public void testIncompleteXmlInput() throws Exception {
    Tokenizer stream = getTaggedTextTokenizer(incompleteSimpleXmlString, true);
    
    assertTokenStreamContents(stream,
        new String[]{"The", "tree", "fag394", "720243", "Fagus", "spec9574", "sylvatica", "could",
            "not", "be", "found", "in", "loc67567", "London", "but", "in", "loc7g68", "Frankfurt"},
        //start offset
        new int[] {0, 4, 9, 9, 9, 15, 15, 25, 31, 35, 38, 44, 47, 47, 55, 59, 62, 62},
        // end offset
        new int[] {3, 8, 14, 24, 14, 24, 24, 30, 34, 37, 43, 46, 53, 53, 58, 61, 71, 71},
        //type
        new String[] {"word", "word", "URI", "URI", "word", "URI", "word", "word", "word", "word",
            "word", "word", "URI", "word", "word", "word", "URI", "word"},
        //posIncr
        new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
    );
  }
  
  /** Store some test attributes in a map. */
  private void fillAttributeMap() {
    String[] attributes = new String[1];
    attributes[0] = "uri";
    this.testSearchedAttributes.put("species", attributes);
    this.testSearchedAttributes.put("genus", attributes);
    this.testSearchedAttributes.put("taxon", attributes);
    this.testSearchedAttributes.put("location", attributes);
  }
  
  /** Setup the TaggedTextTokenizer stream. */
  private Tokenizer getTaggedTextTokenizer(String str, boolean insertAttributes) {
    Reader reader = new StringReader(str);
    Tokenizer stream;
    
    fillAttributeMap();
    if (insertAttributes) {
      stream = new TaggedTextTokenizer(testSearchedAttributes);
    } else {
      stream = new TaggedTextTokenizer();
    }
    
    stream.setReader(reader);
    
    return stream;
  }
}

