/** University Library of Frankfurt. 2018
* Specialised Information Service Biodiversity Research
*/

package de.unifrankfurt.taggedtexttokenizer;

import de.unifrankfurt.taggedtexttokenizer.BufferedOutputTag;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import javax.xml.stream.XMLStreamException;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class TaggedTextTokenizer extends Tokenizer {
  
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
  
  private boolean isParsed;
  
  String typeOfPreviouslyStreamedToken = "word";
  
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
    super();
    init();
    if (searchedAttributes != null) {
      this.searchedAttributes.putAll(searchedAttributes);
    }
  }
  
  public TaggedTextTokenizer() {
    super();
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
    
    log.debug("Inserting Attributes to Stream...");
    
    Iterator<BufferedOutputTag> it = inputList.iterator();
    while (it.hasNext()) {
      BufferedOutputTag tag = it.next();
      
      log.debug(tag + " has attributes: " + tag.hasAttributes());
      
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
        log.debug(token.term + " start: " + token.startNode + " / end: " + token.endNode);
      }
    }
    
    return outputList;
  }

  @Override
  /** Go over the given text stream and parse the next token in the stream. */
  public boolean incrementToken() throws IOException {
    clearAttributes();
    
    // Parse the whole XML in one go...
    if (!isParsed) {
      scanner.setSearchedAttributes(searchedAttributes);
      LinkedList<BufferedOutputTag> outputList = scanner.parse();
      bufferedOutputTokens = insertAttributesInPosition(outputList);
      isParsed = true;
    }
    
    // ... then return the tokens separately
    return setNextToken();
  }
  
  /** Insert the next token in the buffer to the token stream.
   * Returns true as long as there are tokens in the buffer.  */
  private boolean setNextToken() throws IOException {
    if (!bufferedOutputTokens.isEmpty()) {
      BufferedOutputToken token = bufferedOutputTokens.poll();
      
      // Here the token attributes are filled into the stream.
      
      // The token itself (i.e. its string representation)
      termAtt.append(token.term);
      
      // The position increment for words on the same position should be 0.
      if (typeOfPreviouslyStreamedToken == TYPE_URI) {
        posIncrAtt.setPositionIncrement(0);
      } else {
        posIncrAtt.setPositionIncrement(1);
      }
      
      if (token.term.length() == 0) {
        throw new IOException("Token with length 0!");
      }
      
      // This attribute will be ignored by the indexing process, but still may
      // be useful.
      posLenAtt.setPositionLength(token.term.length());
      
      // The token type (if it is a "word" or a "URI")
      typeAtt.setType(token.type);
      
      // Start and end position of the token
      offsetAtt.setOffset(token.startNode, token.endNode);
      
      typeOfPreviouslyStreamedToken = typeAtt.type();
      
      // Return true, to indicate that there is a token ready to be streamed.
      return true;
    }
    
    // No token left to stream
    return false;
  }
  
  /** Takes a BufferedOutputTag created from the TaggedTextTokenizerImpl and
   * returns BufferedOutputToken.
   * TODO: Get rid of this function! No need to transfer between formats!
   */
  private BufferedOutputToken createNewBufferedOutputToken(BufferedOutputTag tag, 
                                                          String term, String type) {
    int startNode = tag.getStartNode();
    int endNode = tag.getEndNode();
    
    log.debug("Creating new OutputToken: " + term);
    
    return new BufferedOutputToken(type, term, startNode, endNode);
  }
  
  /** Resets the reader of the TaggedTextTokenizer. */
  @Override
  public void reset() throws IOException {
    super.reset();
    
    try {
      // Reset the XML-reading instance
      scanner.reset();
      scanner.setup(this.input);
      
      this.isParsed = false;
      this.typeOfPreviouslyStreamedToken = "word";
      
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
