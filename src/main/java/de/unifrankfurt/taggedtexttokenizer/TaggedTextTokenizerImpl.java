/** University Library of Frankfurt. 2018
* Specialised Information Service Biodiversity Research
*/

package de.unifrankfurt.taggedtexttokenizer;

import de.unifrankfurt.taggedtexttokenizer.BufferedOutputTag;

import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The code is partially (especially the basic input pattern) copied from the ClassicTokenizerImpl. 
 * This implementation of the TaggedTextTokenizer will read tagged text (surprise!), filter out the
 * tags, and insert any demanded attributes into the text. The attributes are inserted BEFORE the
 * actual word!
 */
public class TaggedTextTokenizerImpl {
  
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  
  // Constants
  private static boolean DEBUGGING = false;
  private static boolean XML_READER_NAMESPACE_AWARE = false;
  private static final String DUMMY_ROOT = "doc";
  private static final String SPECIAL_CHARACTERS = "[\\p{Punct}“”\"]";
  private static final String ALPHANUMERIC_CHARACTERS = "[\\p{Alnum}]";
  private static final String WHITESPACES = "\\p{Space}";
  
  /** The reader to store the given xml data. */
  private XMLStreamReader xmlStreamReader;
  
  /** Stores a list of currently open tags in document order. */
  private LinkedList<BufferedOutputTag> openTagList = new LinkedList<BufferedOutputTag>();

  /** Stores the tokens (all all their respective data) for output. NOT necessarily in document
   *  order until calling sortOutputListByStartingPosition(). */
  private LinkedList<BufferedOutputTag> outputList = new LinkedList<BufferedOutputTag>();

  /** Stores the demanded tags and their attributes. */
  private HashMap<String, String[]> searchedAttributes = new HashMap<String, String[]>(); 
  
  /** Whether to index all found attributes. */
  private boolean indexAll = false;
  
  private XMLInputFactory xmlInputFactory;
  
  /** Stores the current character position in the text. */
  private int currentTextOffset = 0;
  
  /** Constructor for the TaggedTextTokenizerImpl. */
  public TaggedTextTokenizerImpl() {
    setXmlReaderProperties();
  }
  
  /** Create a new instance of the XML Reader and set its properties. */
  private void setXmlReaderProperties() {
    xmlInputFactory = XMLInputFactory.newInstance();
    // Configures if the XML Reader is sensitive for the given namespaces
    xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, XML_READER_NAMESPACE_AWARE);
  }
  
  /** Parse the text to into the xml reader. This is all done in one go, because we need
   * to know the whole text with all tags to process the XML. 
   * @throws IOException Thrown when no attributes are given in the input text. */
  public LinkedList<BufferedOutputTag> parse() throws IOException {

    try {
      // A cursor moves successively over the text and triggers at 'events'.
      // Here, an event is an opening tag, text character, or a closing tag.
      int event = xmlStreamReader.getEventType();
      
      // Read XML stream
      while (true) {
        
        // XML event handling
        switch (event) {
              
          // Found an opening tag
          case XMLStreamConstants.START_ELEMENT: {
            String tagName = xmlStreamReader.getLocalName();
            printMessage("Current tag: " + tagName);
            printMessage("Is tag demanded? " + isTagDemanded(tagName));
            if (tagName != DUMMY_ROOT && isTagDemanded(tagName)) {
              BufferedOutputTag newTag = createNewOpenTag(tagName);
              openTagList.add(newTag);
              addAttributesToTag(newTag);
            }
            
            break;
          }

          // Found some characters (actually a string)
          case XMLStreamConstants.CHARACTERS: {
            String text = xmlStreamReader.getText();
            if (!text.isEmpty()) {
              addTextTokensToOutputListSuccessively(text);
            }
            
            break;
          }
          
          // Found a closing tag
          case XMLStreamConstants.END_ELEMENT: {
            closeTag(xmlStreamReader.getLocalName());
            
            break;
          }
          
          default: {
            // Do nothing
          }
        }
        
        if (xmlStreamReader.hasNext()) {
          event = xmlStreamReader.next();
        } else {
          break;
        }
      }
    } catch (XMLStreamException e) {
      e.printStackTrace();
    }
    
    // Sort the list of tokens by their respective starting position.
    // There are no URIs included yet.
    sortOutputListByStartingPosition();
    
    if (outputList.isEmpty()) {
      throw new IOException("Empty String!");
    }

    return outputList;
  }

  public void setSearchedAttributes(HashMap<String, String[]> searchedAttributes) {
    this.searchedAttributes.putAll(searchedAttributes);
  }
  
  /** Read the input text and hand it to the XML Reader. */
  public void setup(java.io.Reader input) throws XMLStreamException, IOException {
    StringBuilder bldr = new StringBuilder();
    int ch;    
    ch = input.read();
    while (ch != -1) {
      bldr.append((char)ch);
      ch = input.read();
    }
    
    // Insert a dummy root at the start and the end
    // This will be ignored by the reader, but is necessary for the XML Reader
    // to accept some input.
    bldr.insert(0, "<" + DUMMY_ROOT + ">");
    bldr.insert(bldr.length(), "</" + DUMMY_ROOT + ">");
    
    String xmlText = bldr.toString();
    xmlText = xmlText.replaceAll(" & ", " &amp; ");
    
    // Parse the text to the XML Reader
    this.xmlStreamReader =
             xmlInputFactory.createXMLStreamReader(new StringReader(xmlText));
  }
  
  public void close() throws IOException, XMLStreamException {
    xmlStreamReader.close();
  }
  
  /** Store all demanded attributes of a given opened tag. 
   * @throws IOException If there are attributes with an empty value. */
  private void addAttributesToTag(BufferedOutputTag openTag) throws IOException {  
    // Get the tag's name
    String tag = xmlStreamReader.getLocalName();
      
    // Only if the tag is demanded
    if (isTagDemanded(tag)) {
      printMessage("Searched Attributes of tag \"" + tag + "\" :" + searchedAttributes.get(tag));
      
      // Get all demanded attributes of the processed tag and
      // read their value in the currently opened tag
      String[] attributes = {};
      if (indexAll) {
        attributes = getAttributes();
      } else {
        attributes = searchedAttributes.get(tag);
      }
      
      for (String attName : attributes) {
        printMessage("Searching Attribute Name: " + attName);
        String attValue = xmlStreamReader.getAttributeValue("", attName);
        
        printMessage("Found: " + attValue);
        
        if (attValue != null) {
          
          // If the attribute value is empty, throw an exception
          // If you waste bytes with empty values, you deserve an exception.
          if (attValue.isEmpty()) {
            throw new IOException("The tag '" + tag + "' at position " + getCurrentTextOffset()
                                  + "misses the value for attribute " + attName);
          }
          
          // Add the attribute value to the tag
          openTag.addAttributes(attName, attValue);
          printMessage("Added Attribute " + attName + " with the value " + attValue 
              + " to " + tag);
        }
      }
    }
  }
  
  private String[] getAttributes() {
    
    ArrayList<String> attributeNames = new ArrayList<String>();
    for (int i = 0; i < xmlStreamReader.getAttributeCount(); ++i) {
      attributeNames.add(xmlStreamReader.getAttributeName(i).toString());
    }
    
    return attributeNames.toArray(new String[0]);
  }
  
  /** Returns true, if the the given tag name is searched for. */
  private boolean isTagDemanded(String tag) {
    // Do not process tag, when it has no attributes and ignore the corresponding end tag
    if (xmlStreamReader.isStartElement() && xmlStreamReader.getAttributeCount() == 0
        || xmlStreamReader.isEndElement() && getLatestOpenTagByName(tag) == null) {
      return false;
    }
    
    // If all tags should be indexed, ignore any given list
    if (!indexAll) {
      return searchedAttributes.keySet().contains(tag);
    } else if (tag == DUMMY_ROOT) {
      // ignore the dummy tag
      return false;
    } else {
      return true;
    }
  }
  
  /** Create a new buffered tag.
   * Returns a BufferedOpenTag with the given name. */
  private BufferedOutputTag createNewOpenTag(String name) {
    // Get the current position in the text
    int start = getCurrentTextOffset();
    
    printMessage("Open Position: [Text] " + start + " - [Reader] "
                         + xmlStreamReader.getLocation().getCharacterOffset());
    printMessage("Open Tag: " + name);
    
    // Create the new BufferedOpenTag
    // Sets the end offset to "-1". This will be updated when the tag is closed.
    return new BufferedOutputTag(name, "", start, -1);
  }
  
  /** Split the given text by white spaces, remove any special character and 
   * create a new BufferedOutputTag for every token. */
  private void addTextTokensToOutputListSuccessively(String text) throws IOException {
    
    // For every token...
    for (String token : text.split(WHITESPACES)) {
      
      // Separate the token from the special characters
      // Be aware that the regex searches the characters that should NOT be included,
      // since here applies a replace-function
      String alphaNumericOnly = token.replaceAll(SPECIAL_CHARACTERS, "");
      
      // Jump over (multiple) special characters and only increment the offset
      if (alphaNumericOnly.isEmpty()) {
        incrementTextOffset(token);
        continue;
      }
      
      printMessage("Creating single node with text : \"" + alphaNumericOnly + "\"");
      
      // Also separate the special characters coming before the token
      String specialCharactersBeforeToken = token.replaceAll(ALPHANUMERIC_CHARACTERS + ".*", "");
      
      // Increase the offset counter, if there are special characters in front of the token
      incrementTextOffset(specialCharactersBeforeToken);
      
      // Create a new BufferedOutputTag
      BufferedOutputTag tag = createNewOpenTag(alphaNumericOnly);
      tag.startNode = getCurrentTextOffset();
      tag.endNode = incrementTextOffset(alphaNumericOnly);
      tag.addText(alphaNumericOnly);
      
      printMessage("Add token: \"" + alphaNumericOnly + "\"");
      
      // Store the BufferedOutputTag directly in the output list
      outputList.add(tag);
      
      // Also separate the special characters coming before the token
      String specialCharactersAfterToken = token.replaceAll(".*" 
          + ALPHANUMERIC_CHARACTERS + "+?", "");
      
      printMessage("Token: " + token);
      printMessage("Special Characters BEFORE Token: " + specialCharactersBeforeToken);
      printMessage("Special Characters AFTER Token: " + specialCharactersAfterToken);
      
      // Increment the offset counter by 1 to set it to the next token's starting position
      incrementTextOffset(specialCharactersAfterToken.length() + 1);
    }
  }
  
  /** Close an open tag. */
  private void closeTag(String tagName) throws XMLStreamException {
    printMessage("Close Tag: " + tagName);
    
    if (isTagDemanded(tagName)) {
      BufferedOutputTag openTag = getLatestOpenTagByName(tagName);
      
      /* If the tag contains no text or the tag is an empty element (e.g. <test\>), start
        and end offset are the same
      */
      if (openTag.startNode == getCurrentTextOffset()) {
        openTag.endNode = openTag.startNode;
      } else {
        openTag.endNode = getCurrentTextOffset() - 1;
      }
      
      // Remove the tag for the open tag list and put it into the output list
      // Throw an exception, if there is an inconsistency.
      openTagList.removeLastOccurrence(openTag);
      outputList.add(openTag);
    }
  }
  
  /** Return the last opened tag with the given name 
   * Return null, if no match was found. */
  private BufferedOutputTag getLatestOpenTagByName(String tagName) {
    // Iterate reversely over the open tag list
    Iterator<BufferedOutputTag> li = openTagList.descendingIterator();
    while (li.hasNext()) {
      BufferedOutputTag openTag = li.next();
      
      if (openTag.tagName == tagName) {
        return openTag;
      }
    }
    return null;
  }
  
  private void sortOutputListByStartingPosition() {
    outputList.sort(new StartingPositionComparator());
  }

  /** Sorts the given BufferedOutputTags <br>
    *  1.) by starting position<br>
    *  2.) by type ("URI" before "word") */
  private class StartingPositionComparator implements Comparator<BufferedOutputTag> {
    @Override
    public int compare(BufferedOutputTag a, BufferedOutputTag b) {
      // Sort by starting position...
      if (a.startNode < b.startNode) {
        return -1;
      } else if (a.startNode == b.startNode) {
        // Sort by token type...
        // i.e. if A has attributes and B does not, A goes first...
        if (a.hasAttributes() && !b.hasAttributes()) {
          return -1;
        } else if (!a.hasAttributes() && b.hasAttributes()) {
          // ... if B has attributes and A has not, B goes first
          return 1;
        } else {
          // ... else both are equal
          return 0;
        }
      } else {
        return 1;
      }
    }
  }
  
  /** Increase the text offset counter by the length of the given token. 
    * @return The new text offset */
  private int incrementTextOffset(String token) {
    printMessage("Increment offset from token " + token + " by " + token.length());
    
    this.currentTextOffset += token.length();
    
    return this.currentTextOffset;
  }
  
  /** Increase the text offset counter by the given number. Does NOT increase, 
   * if the text is on position "0" (i.e. the very first position).
    * @return The new text offset  */
  private int incrementTextOffset(int inc) {
    printMessage("Increment offset by " + inc);
    if (this.currentTextOffset != 0) {
      this.currentTextOffset += inc;
    }
    
    return this.currentTextOffset;
  }
  
  private int getCurrentTextOffset() {
    return this.currentTextOffset;
  }
  
  public void setIndexAll(boolean b) {
    this.indexAll = b;
  }
  
  /** Reset this instance. */
  public void reset() {
    setXmlReaderProperties();
    this.openTagList.clear();
    this.outputList.clear();
    this.currentTextOffset = 0;
  }
  
  private void printMessage(String str) {
    if (DEBUGGING) {
      System.out.println(str);
    } else {
      log.debug(str);
    }
  }
}
