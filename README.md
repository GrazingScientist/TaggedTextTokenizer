[![Build Status](https://travis-ci.org/GrazingScientist/TaggedTextTokenizer.svg?branch=master)](https://travis-ci.org/GrazingScientist/TaggedTextTokenizer)

# TaggedTextTokenizer
-- Under Development --

The TaggedTextTokenizer was invented to improve the search results in Apache Lucene, by indexing XML files that contain semantic information of the tagged words. While the SynonymGraphFilter may be applicable in some cases to improve the search result, it will not work with ambiguous words (for example, try SynonymGraphFilter with the word 'break' ;) ). When you have tagged words and their respective attribute-value pairs in an XML, you can be precise for the meaning of every single word.

The TaggedTextTokenizer is able to index text like this:

```xml
<sec sec-type="Introduction" id="SECID0E4F">
<title>Introduction</title>
<p>The cyclocephaline scarabs (
<tp:taxon-name><tp:taxon-name-part taxon-name-part-type="order">Coleoptera</tp:taxon-name-part></tp:taxon-name>:
<tp:taxon-name><tp:taxon-name-part taxon-name-part-type="family">Scarabaeidae</tp:taxon-name-part></tp:taxon-name>:
<tp:taxon-name><tp:taxon-name-part taxon-name-part-type="subfamily">Dynastinae</tp:taxon-name-part></tp:taxon-name>
) are remarkable among rhinoceros beetles for the group’s immense species richness and ecological importance.  
<tp:taxon-name><tp:taxon-name-part taxon-name-part-type="tribe">Cyclocephalini</tp:taxon-name-part></tp:taxon-name> 
is a pan-tropical tribe with several genera considered to be keystone pollinators in New and Old World tropical 
ecosystems. By one estimate, pollination<!--PageBreak-->mutualisms between cyclocephalines and early-diverging 
angiosperms suggest that nearly 900 species of Neotropical plants rely upon these scarab beetles for sexual 
reproduction (<xref ref-type="bibr" rid="B403">Schatz 1990</xref>). Beyond tropical forests, cyclocephaline scarab 
beetle species are important to human industry as pests in tropical and temperate agroecosystems and turfgrass in 
North America. Due to these factors, the group has received considerable alpha-taxonomic attention as species
identity (and identification) is crucial for understanding the fascinating biology of these scarabs. However, almost
nothing is known about the evolution of the group into their incredible ecological roles.</p><p>This paper synthesizes
all available information on cyclocephaline scarab beetles into these broad categories: 1) taxonomic and nomenclatural
history of the group organized by major worker, including an exegesis of Endrődi’s German-language revision of the 
tribe; 2) state of knowledge surrounding diagnosis and identification of immature life-stages; 3) economic importance 
in agroecosystems; 4) natural enemies of these beetles; 5) use as food by humans; 6) importance of adults as pollination
mutualists; 7) knowledge of the fossil record and evolution; and 8) an overview of each genus, including expanded 
diagnoses and a key to world genera of <tp:taxon-name><tp:taxon-name-part taxon-name-part-type="tribe">Cyclocephalini
</tp:taxon-name-part></tp:taxon-name>.</p></sec>
```
(quoted from Moore et al., 2018)

The Tokenizer works a little like the SynonymGraphFilter in Apache Lucene. The XML text is read and at least all words of the plain text are given to the index. And that would be it, given a simple configuration that you only want to index
the plain text.

However, given a configuration file, the tokenizer also indexes the attributes of the respective word FOR THE SAME POSITION as the tagged word. For example:

`<tp:taxon-name-part taxon-name-part-type="order">Coleoptera</tp:taxon-name-part></tp:taxon-name>`

In the above given text, the word 'Coleoptera' starts at the character position 41 (if I counted correctly) and ends at character position 51. Lucene remembers the position of single words. Now, the value 'order' of the given attribute 'taxon-name-part-type' is also indexed (if configured) for the start position 41 and the end position 51, because this attribute is tagged to the word 'Coleoptera'. So, when you search in your index for 'order' the index will hand you back 'Coleoptera' at this exact position.

## Installation
You have to compile a .jar file from the source code by using [Apache Maven](https://maven.apache.org/).

To compile the whole code, just type (when in the repository folder):

`mvn package`

When run successfully, this will create a .jar file in the **target** folder. This .jar file you may put into a **solr-7.X.X/lib** folder (that you have to create), if you like (but any other folder is appropriate too). 

Additionally, you have to give the path (either the path to the folder or the exact path) to the .jar file. This you can do in the **server/solr/[your core name]/conf/solrconfig.xml**. In this file, you should find entries looking like this:

`<lib dir="${solr.install.dir:../../../..}/contrib/extraction/lib" regex=".*\.jar" />`

Somewhere close to these entries, you may type (if following the above given example):

`<lib dir="${solr.install.dir:../../../..}/lib" regex=".*\.jar" />`, when you want to add the whole folder, or

`<lib path="/home/me/solr-X.X.X/lib/tagged-text-tokenizer-X.X.jar" />`, for reading only this .jar specifically, where X.X is the version number (please note the differing syntax to above statement).

Additionally, you have to give a process sequence to Solr for your document. Hence, you need to modify **server/solr/[your core name]/conf/managed-schema** to include:

`<fieldType name="tagged_text" class="solr.TextField">  
<analyzer>  
<tokenizer class="de.unifrankfurt.taggedtexttokenizer.TaggedTextTokenizerFactory" />
    </analyzer>
 </fieldType>`
 
 You can also add any filter after the tokenizer, which you like.
 
 In your input document, you have to name all fields that need to be included with this process line as
 
 `<field name="tagged_text" class="solr.TextField" />`.
 
 For more details on how to configure your input file, please see [the official documentation](https://lucene.apache.org/solr/guide/7_3/uploading-data-with-index-handlers.html).
 
 Finally, reload your core in the Solr Administration Menu and index your file.

For running only the tests (there are not so many yet):

`mvn test`


## References

Moore MR, Cave RD, Branham MA (2018) Synopsis of the cyclocephaline scarab beetles (Coleoptera, Scarabaeidae, Dynastinae). 
ZooKeys 745: 1-99. https://doi.org/10.3897/zookeys.745.23683
