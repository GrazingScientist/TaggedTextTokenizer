/** University Library of Frankfurt. 2018
* Specialised Information Service Biodiversity Research
*/

package de.unifrankfurt.taggedtexttokenizer;

import de.unifrankfurt.taggedtexttokenizer.TaggedTextTokenizerFactory;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ja.StringMockResourceLoader;
import org.apache.lucene.analysis.util.BaseTokenStreamFactoryTestCase;

public class TestTaggedTextTokenizerFactory extends BaseTokenStreamFactoryTestCase {
  
  // Test string taken from:
  // Moore MR, Cave RD, Branham MA (2018) Annotated catalog and bibliography of the 
  // cyclocephaline scarab beetles (Coleoptera, Scarabaeidae, Dynastinae, Cyclocephalini).
  // ZooKeys 745: 101-378. https://doi.org/10.3897/zookeys.745.23685
  String complexXmlString = "For example, the Costa "
      + "Rican <italic><tp:taxon-name><tp:taxon-name-part taxon-name-part-type=\"genus\" reg=\"Cyclocephala\">"
      + "C.</tp:taxon-name-part><tp:taxon-name-part taxon-name-part-type=\"species\" reg=\"unamas\">unamas"
      + "</tp:taxon-name-part></tp:taxon-name></italic> Ratcliffe (Spanish \"una mas\") was named after the "
      + "overwhelming feeling one gets after the discovery of <italic>yet another</italic> new <italic>"
      + "<tp:taxon-name><tp:taxon-name-part taxon-name-part-type=\"genus\" reg=\"Cyclocephala\">Cyclocephala"
      + "</tp:taxon-name-part></tp:taxon-name></italic> species, epitomized by the species name <italic>"
      + "<tp:taxon-name><tp:taxon-name-part taxon-name-part-type=\"genus\" reg=\"Cyclocephala\">C."
      + "</tp:taxon-name-part><tp:taxon-name-part taxon-name-part-type=\"species\" reg=\"nodanotherwon\">"
      + "nodanotherwon</tp:taxon-name-part></tp:taxon-name></italic> Ratcliffe. ";
    
  String searchAttributes = "{"
      + "  \"tp:taxon-name-part\": ["
      + "    \"taxon-name-part-type\","
      + "    \"reg\""
      + "  ],"
      + "  \"ext-link\": ["
      + "      \"ext-link-type\","
      + "      \"xlink:href\""
      + "  ],"
      + "  \"taxon\": ["
      + "    \"uri\""
      + "  ],"
      + "  \"location\": ["
      + "    \"uri\""
      + "  ]"
      + "}";
  
  /** Test TaggedTextTokenizerFactory. */
  public void testTaggedTextTokenizerFactory() throws Exception {
    
    Reader reader = new StringReader(complexXmlString);
    
    HashMap<String, String> args = new HashMap<String, String>();
    // It's a trap! Only necessary to fulfill an IF-clause.
    args.put("searchAttributesFile", "test_attributes.json");

    TaggedTextTokenizerFactory factory = new TaggedTextTokenizerFactory(args);
    factory.inform(new StringMockResourceLoader(searchAttributes));
    
    Tokenizer stream = factory.create(newAttributeFactory());
    stream.setReader(reader);
    
    assertTokenStreamContents(stream,
        new String[]{"For", "example", "the", "Costa", "Rican", "genus", "Cyclocephala", "C", "species", "unamas", "unamas", 
                     "Ratcliffe", "Spanish", "una", "mas", "was", "named", "after", "the", "overwhelming", "feeling", "one", "gets",
                     "after", "the", "discovery", "of", "yet", "another", "new", "genus", "Cyclocephala", "Cyclocephala", "species",
                     "epitomized", "by", "the", "species", "name", "genus", "Cyclocephala", "C", "species", "nodanotherwon",
                     "nodanotherwon", "Ratcliffe"},
        new int[] {0, 4, 13, 17, 23, 29, 29, 29, 32, 32, 32, 39, 50, 59, 63, 69, 73, 79, 85, 89, 102, 110, 114, 119, 125, 129, 139, 142, 
            146, 154, 158, 158, 158, 171, 180, 191, 194, 198, 206, 211, 211, 211, 214, 214, 214, 228}, //start offset
        new int[] {3, 11, 16, 22, 28, 31, 31, 30, 38, 38, 38, 48, 57, 62, 66, 72, 78, 84, 88, 101, 109, 113, 118, 124, 128, 138, 141, 145,
            153, 157, 170, 170, 170, 178, 190, 193, 197, 205, 210, 213, 213, 212, 227, 227, 227, 237} // end offset
    );
  }
  
}
