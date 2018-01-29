package de.unifrankfurt.taggedtexttokenizer;

import java.util.HashMap;

public class BufferedOutputTag {
  String term;
  String tagName;
  int startNode;
  int endNode;
  HashMap<String, String> attributes;

  public BufferedOutputTag(String tagName, String term, int startNode, int endNode) {
    this.term = term;
    this.startNode = startNode;
    this.endNode = endNode;
    this.tagName = tagName;
    this.attributes = null;
  }

  public void addText(String text) {
    this.term += text;
  }

  public void addAttributes(String idName, String idValue) {
    if (this.attributes == null) {
      this.attributes = new HashMap<String, String>();
    }

    this.attributes.put(idName, idValue);
  }

  public boolean hasAttributes() {
    if (this.attributes == null) {
      return false;
    }
    
    return this.attributes.size() > 0;
  }

  public String getAttributeValue(String attName) {
    return attributes.get(attName);
  }

  public int getStartNode() {
    return this.startNode;
  }
  
  public int getEndNode() {
    return this.endNode;
  }
}
