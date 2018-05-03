/** University Library of Frankfurt. 2018
* Specialised Information Service Biodiversity Research
*/

package de.unifrankfurt.taggedtexttokenizer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import javax.xml.stream.XMLStreamException;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeFactory;
import de.unifrankfurt.taggedtexttokenizer.BufferedOutputTag;

public final class TaggedTextTokenizer extends Tokenizer {

  public static final String TYPE_URI = "URI";
  private static final boolean DEBUGGING = false;

  /* The attributes of the currently processed token */
  // The text of the token
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class); 
  
  // Position of the token relative to the previous token
  private final PositionIncrementAttribute posIncrAtt = 
      addAttribute(PositionIncrementAttribute.class);
  
  // Length of the token (i.e. number of characters)
  private final PositionLengthAttribute posLenAtt = addAttribute(PositionLengthAttribute.class);
  
  // The standard type of a token is "word". Would be replaced by "URI" if it was a URI (surprise!).
  private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
  
  // Gives the start and end position of the token
  private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

  /** A private instance of the JFlex-constructed scanner. */
  private TaggedTextTokenizerImpl scanner;
  
  private HashMap<String, String[]> searchedAttributes = new HashMap<String, String[]>();
  private LinkedList<BufferedOutputToken> bufferedOutputTokens = 
      new LinkedList<BufferedOutputToken>();
  
  boolean isParsed;
  
  static class BufferedOutputToken {
    final String term;
    final String type;
    final int startNode;
    final int endNode;
    
    public BufferedOutputToken(String type, String term, int startNode, int endNode) {
      this.type = type;
      this.term = term;
      this.startNode = startNode;
      this.endNode = endNode;
    }
  }
  
  /** Create a new TaggedTextTokenizer with a given HashMap containing
   * searched tags and within these the searched attributes.
   * @param searchedAttributes HashMap with the key:value = tag:[attributes]
   */
  public TaggedTextTokenizer(HashMap<String, String[]> searchedAttributes) {
    init();
    if (searchedAttributes != null) {
      this.searchedAttributes.putAll(searchedAttributes);
    }
  }
  
  public TaggedTextTokenizer() {
    init();
  }
  
  /** Creates a new TaggedTextTokenizer with a given 
   * {@link org.apache.lucene.util.AttributeFactory}.
   */
  public TaggedTextTokenizer(AttributeFactory factory, HashMap<String, 
                             String[]> searchedAttributes) {
    super(factory);
    this.searchedAttributes.putAll(searchedAttributes);
    init();
  }
  
  /** Initializes the reader. */
  private void init() {
    this.scanner = new TaggedTextTokenizerImpl();
    isParsed = false;
  }
  
  /** Insert the found attributes into the list of tokens as tokens on their own. 
   * The attributes always come before the original word. */
  private LinkedList<BufferedOutputToken> insertAttributesInPosition(
      LinkedList<BufferedOutputTag> inputList) {
    LinkedList<BufferedOutputToken> outputList = new LinkedList<BufferedOutputToken>();
    
    Iterator<BufferedOutputTag> it = inputList.iterator();
    while (it.hasNext()) {
      BufferedOutputTag tag = it.next();
      
      if (tag.hasAttributes()) {
        for (String att : tag.attributes.keySet()) {
          outputList.add(createNewBufferedOutputToken(tag, tag.getAttributeValue(att), TYPE_URI));
        }
      } else {
        outputList.add(createNewBufferedOutputToken(tag, tag.term, "word"));
      }
    }
    
    // Debugging output
    if (DEBUGGING) {
      for (BufferedOutputToken token : outputList) {
        System.out.println(token.term + " start: " + token.startNode + " / end: " + token.endNode);
      }
    }
    
    return outputList;
  }

  @Override
  /** Go over the given text stream and parse the next token in the stream. */
  public boolean incrementToken() throws IOException {
    clearAttributes();
    
    if (!isParsed) {
      scanner.setSearchedAttributes(searchedAttributes);
      LinkedList<BufferedOutputTag> outputList = scanner.parse();
      bufferedOutputTokens = insertAttributesInPosition(outputList);
      isParsed = true;
    }
    
    return setNextToken();
  }
  
  /** Insert the next token in the buffer to the token stream.
   * Returns true as long as there are tokens in the buffer.  */
  private boolean setNextToken() {
    if (!bufferedOutputTokens.isEmpty()) {
      BufferedOutputToken token = bufferedOutputTokens.poll();
      
      // Here the token attributes are filled into the stream
      termAtt.append(token.term);
      posIncrAtt.setPositionIncrement(1);
      posLenAtt.setPositionLength(token.term.length());
      typeAtt.setType(token.type);
      offsetAtt.setOffset(token.startNode, token.endNode);
      
      return true;
    }
    
    return true;
  }
  
  /** Takes a BufferedOutputTag created from the TaggedTextTokenizerImpl and
   * returns BufferedOutputToken.
   * TODO: Get rid of this function! No need to transfer between formats!
   * @param tag
   * @param term
   * @param type
   * @return
   */
  private BufferedOutputToken createNewBufferedOutputToken(BufferedOutputTag tag, 
                                                          String term, String type) {
    int startNode = tag.getStartNode();
    int endNode = tag.getEndNode();
    
    return new BufferedOutputToken(type, term, startNode, endNode);
  }
  
  /** Resets the reader of the TaggedTextTokenizer. */
  @Override
  public void reset() throws IOException {
    super.reset();
    
    try {
      // Reset the XML-reading instance
      scanner.setup(this.input);
    } catch (XMLStreamException e) {
      // Has to rethrow the IOException, since otherwise there
      // is no override
      throw new IOException(e);
    }
  }
  
  @Override
  public void close() throws IOException {
    super.close();
    
    try {
      scanner.close();
    } catch (XMLStreamException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
