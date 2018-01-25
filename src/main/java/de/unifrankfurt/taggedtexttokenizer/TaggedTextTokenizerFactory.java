package uni_frankfurt;

import java.util.Map;

import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;

public class TaggedTextTokenizerFactory extends TokenizerFactory {

	  /** Creates a new TaggedTextTokenizerFactory */
	  public TaggedTextTokenizerFactory(Map<String,String> args) {
	    super(args);
	    if (!args.isEmpty()) {
	      throw new IllegalArgumentException("Unknown parameters: " + args);
	    }
	  }

	  @Override
	  public TaggedTextTokenizer create(AttributeFactory factory) {
		TaggedTextTokenizer tokenizer = new TaggedTextTokenizer(factory);
	    return tokenizer;
	  }
}
