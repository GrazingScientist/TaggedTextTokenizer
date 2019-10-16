/** University Library of Frankfurt. 2018
* Specialised Information Service Biodiversity Research
*/

package de.unifrankfurt.taggedtexttokenizer;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import de.unifrankfurt.taggedtexttokenizer.TaggedTextTokenizer;

public class TestTaggedTextTokenizer extends BaseTokenStreamTestCase {
  
  HashMap<String, String[]> testSearchedAttributes = new HashMap<String, String[]>();
  
  String referenceString = "Reynolds & Simons";
  
  String simpleXmlString = "<doc>The tree <species uri='720243'><genus uri='fag394'>Fagus</genus> "
      + "<taxon uri='spec9574'>sylvatica</taxon></species> could not be found in <location "
      + "uri='loc67567'>London</location>, but in <location uri='loc7g68' complete='true'>Frankfurt</location>."
      + "</doc>";
  
  String simpleXmlStringWithSpecialCharacters = "The tree* <species uri='720243'><genus uri='fag394'>&amp;Fagus</genus> "
      + "<taxon uri='spec9574'>sylvatica</taxon></species> ^#could^$ not be found in <location "
      + "uri='loc67567'>London</location>, but in <location uri='loc7g68'>Frankfurt</location>.$";
  
  String incompleteSimpleXmlString = "The tree <species uri='720243'><genus uri='fag394'>Fagus"
      + "</genus> <taxon uri='spec9574'>sylvatica</taxon></species> could not be found in "
      + "<location uri='loc67567'>London</location>, but in <location uri='loc7g68'>Frankfurt"
      + "</location>.";
  
  String missingAttributeXmlString = "The tree <species uri='720243'><genus uri='fag394'>Fagus"
      + "</genus> <taxon uri=''>sylvatica</taxon></species> could not be found in <location "
      + "uri=''>London</location>, but in <location uri='loc7g68'>Frankfurt</location>.";
  
  String simpleXmlStringWithWhitespaces = "<doc>The tree   <species uri='720243'>"
      + "<genus uri='fag394'>"
      + "Fagus</genus><taxon uri='spec9574'>sylvatica</taxon></species> could    not be "
      + "found in <location uri='loc67567'>London</location>, but    in    <location "
      + "uri='loc7g68'>Frankfurt</location>.   </doc>";
  
  // taken from Arteaga et al. (2018)
  String emptyText = "“<italic><tp:taxon-name><tp:taxon-name-part taxon-name-part-type=\"genus\" "
      + "reg=\"Tropidodipsas\">T.</tp:taxon-name-part></tp:taxon-name></italic>” <italic><tp:taxon-name>"
      + "<tp:taxon-name-part taxon-name-part-type=\"genus\" reg=\"Tropidodipsas\"/>"
      + "<tp:taxon-name-part taxon-name-part-type=\"species\" reg=\"sartorii\">"
      + "sartorii</tp:taxon-name-part></tp:taxon-name></italic> + "
      + "<italic><tp:taxon-name><tp:taxon-name-part taxon-name-part-type=\"genus\" reg=\"Geophis\">"
      + "Geophis</tp:taxon-name-part></tp:taxon-name></italic> +";
  
  
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
  
  /** Test the removing of multiple white spaces within the text. */
  public void testRemovingEmptyWords() throws Exception {
    Tokenizer stream = getTaggedTextTokenizer(simpleXmlStringWithWhitespaces, false);
    
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
        new int[] {1, 1, 1, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0}
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
        new int[] {1, 1, 1, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0}
    );
  }
  
  /** Test for an input with an unescaped ampersand. */
  public void testReferenceInput() throws Exception {
    Tokenizer stream = getTaggedTextTokenizer(referenceString, true);
    
    assertTokenStreamContents(stream,
        new String[] {"Reynolds", "Simons"},
        new int[] {0, 10},
        new int[] {8, 16},
        new String[] {"word", "word"},
        new int[] {1, 1}
    );
  }
  
  /** Test for empty elements. */
  public void testEmptyText() throws Exception {
    
    this.testSearchedAttributes.put("tp:taxon-name-part", 
        new String[] {"taxon-name-part-type", "reg"});
    
    
    Tokenizer stream = getTaggedTextTokenizer(emptyText, true);
    
    assertTokenStreamContents(stream,
        new String[] {"genus", "Tropidodipsas", "T", "genus", "Tropidodipsas", "species", 
            "sartorii", "sartorii", "genus", "Geophis", "Geophis"},
        new int[] {1, 1, 1, 5, 5, 5, 5, 5, 15, 15, 15},
        new int[] {3, 3, 2, 5, 5, 13, 13, 13,22, 22, 22},
        new String[] {"URI", "URI", "word", "URI", "URI", "URI", "URI", "word", "URI", 
            "URI", "word"},
        new int[] {1, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0});
  }
  
  /** Test to index all tags and attributes. */
  public void testIndexingAll() throws Exception {
    
    Tokenizer stream = getTaggedTextTokenizer(simpleXmlString, false, true);
    
    assertTokenStreamContents(stream,
        new String[]{"The", "tree", "fag394", "720243", "Fagus", "spec9574", "sylvatica", "could",
            "not", "be", "found", "in", "loc67567", "London", "but", "in", "true", "loc7g68", 
            "Frankfurt"},
        //start offset
        new int[] {0, 4, 9, 9, 9, 15, 15, 25, 31, 35, 38, 44, 47, 47, 55, 59, 62, 62, 62},
        // end offset
        new int[] {3, 8, 14, 24, 14, 24, 24, 30, 34, 37, 43, 46, 53, 53, 58, 61, 71, 71, 71},
        //type
        new String[] {"word", "word", "URI", "URI", "word", "URI", "word", "word", "word", "word",
            "word", "word", "URI", "word", "word", "word", "URI", "URI", "word"},
        //posIncr
        new int[] {1, 1, 1, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 0}
    );
  }
  
  /** Test for potential zero token length. */
  public void testTokenWithLengthZero() throws Exception {
    String zeroTokenString = "<sec sec-type=\"Introduction\" id=\"SECID0EDKAC\">"
        + "<title>Introduction</title></sec>";
    
    Tokenizer stream = getTaggedTextTokenizer(zeroTokenString, false, true);
    
    assertTokenStreamContents(stream,
        new String[] {"Introduction", "SECID0EDKAC", "Introduction"});
  }
  
  /** Test that the tokens are returned in appropriate order. */
  public void testTokenOrder() throws Exception {
    // taken from Arteaga et al. (2018)
    String referenceString = "With 70 currently recognized species (Table <xref "
        + "ref-type=\"table\" rid=\"T1\">1</xref>), the snail-eaters (tribe <tp:taxon-name>"
        + "<tp:taxon-name-part taxon-name-part-type=\"tribe\">Dipsadini</tp:taxon-name-part>"
        + "</tp:taxon-name>) are among the most diverse groups of arboreal snakes.";
    
    Tokenizer stream = getTaggedTextTokenizer(referenceString, false, true);
    
    assertTokenStreamContents(stream,
        new String[] {"With", "70", "currently",
            "recognized", "species", "Table", "T1", "table",  "1", "the", "snail-eaters", "tribe", 
            "tribe", "Dipsadini", "are", "among", "the", "most", "diverse", "groups", "of", 
            "arboreal", "snakes"},
        new int[] {0, 5, 8, 18, 29, 38, 44, 44, 44, 48, 52, 66, 72, 72, 83, 87, 93, 97, 102,
            110, 117, 120, 129},
        new int[] {4, 7, 17, 28, 36, 43, 45, 45, 45, 51, 64, 71, 81, 81, 86, 92, 96, 101, 
            109, 116, 119, 128, 135}
    );
  }
  
  /*public void testMinusRemoval() throws Exception {
    String minimalString = "- 9 - Verbands-, Ordnung";
    
    Tokenizer stream = getTaggedTextTokenizer(minimalString, false, false);
    assertTokenStreamContents(stream,
        new String[] {"9", "Verbands-", "Ordnung"}
    );
  }*/
  
  
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
    return getTaggedTextTokenizer(str, insertAttributes, false);
  }
  
  private Tokenizer getTaggedTextTokenizer(String str, boolean insertAttributes, boolean indexAll) {
    Reader reader = new StringReader(str);
    Tokenizer stream;
    
    fillAttributeMap();
    List<String> excludedAttributes = new ArrayList<String>();
    if (insertAttributes || indexAll) {
      stream = new TaggedTextTokenizer(testSearchedAttributes, excludedAttributes, indexAll);
    } else {
      stream = new TaggedTextTokenizer();
    }
    
    stream.setReader(reader);
    
    return stream;
  }

  /* References
   * Arteaga A, Salazar-Valenzuela D, Mebert K, Peñafiel N, Aguiar G, Sánchez-Nivicela JC, Pyron RA, 
   * Colston TJ, Cisneros-Heredia DF, Yánez-Muñoz MH, Venegas PJ, Guayasamin JM, Torres-Carvajal O (2018) 
   * Systematics of South American snail-eating snakes (Serpentes, Dipsadini), with the description of 
   * five new species from Ecuador and Peru. ZooKeys 766: 79-147. https://doi.org/10.3897/zookeys.766.24523
   */

}


