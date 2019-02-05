/*************************************
### Overview
This notebook will walk you through doing more advanced analysis with of the parsed Wikipedia data.  If you want to run the code then it assumes you have already parsed the Wikipedia dump and created your Parquet files.

###Configure the Zeppelin Interpreter
This notebook does not require any external dependencies to be loaded.

***************************************/

val folder = "[Folder]"

spark.read.parquet(folder + "wkp_page").createOrReplaceTempView("wkp_page")
spark.read.parquet(folder + "wkp_redirect").createOrReplaceTempView("wkp_redirect")
spark.read.parquet(folder + "wkp_header").createOrReplaceTempView("wkp_header")
spark.read.parquet(folder + "wkp_text").createOrReplaceTempView("wkp_text")
spark.read.parquet(folder + "wkp_template").createOrReplaceTempView("wkp_template")
spark.read.parquet(folder + "wkp_tag").createOrReplaceTempView("wkp_tag")
spark.read.parquet(folder + "wkp_table").createOrReplaceTempView("wkp_table")
spark.read.parquet(folder + "wkp_link_external").createOrReplaceTempView("wkp_link_external")
spark.read.parquet(folder + "wkp_link_wiki").createOrReplaceTempView("wkp_link_wiki")

/*************************************
### Dump Contents
Most people assume the dump consists entirely of Wikipedia articles.  While articles do make up the bulk of the dump, there are numerous other page types.

* *ARTICLE* What most people think of when they envision Wikipedia
* *CATEGORY* Pages that are combination indexes, ontology, and summaries.
* *WIKIPEDIA* Admin and meta pages that deal with creating and maintaining Wikipedia articles.
* *FILE* Media content
* *TEMPLATE* Templates are described in more detail in a below section

Other namespace definitions can be found here: [Namespaces](https://en.wikipedia.org/wiki/Wikipedia:Namespace)

***************************************/

SELECT 
    name_space_text, 
    COUNT(*) AS page_count 
FROM 
    wkp_page 
GROUP BY 
    name_space_text 
ORDER BY 
    page_count DESC
	

/*************************************
###Analyze pages
We'll start with doing some basic analysis on the pages.

* *id* Unique wikipedia ID from the dump file.
* *title* Wikipedia page's title.
* *nameSpace* Text name of a wiki's name space. Ex: https://en.wikipedia.org/wiki/*Wikipedia*:title
* *pageType* Similar to namespace, but Articles are split into ARTICLE, REDIRECT, DISAMBIGUATION, and LIST. Otherwise defaults to namespace.
* *lastRevisionId* identifier for the last revision.
* *lastRevisionDate* Date for when the page was last updated.

***************************************/

SELECT * 
FROM 
    wkp_page
LIMIT 25


/*************************************
### Redirects

In the Wikipedia dump many of the page elements are simple redirects to the primary article.  In Wikipedia they act as HTML redirects.  Researchers can use this information to form disambiguations for article titles.

***************************************/

-- Get pages with most redirects
SELECT 
    art.id, 
    art.title,
    COUNT(*) AS total_redirects
FROM
    wkp_page art 
JOIN 
    wkp_redirect rdr 
    ON art.id = rdr.target_page_id
GROUP BY
    art.id, 
    art.title
ORDER BY
    total_redirects DESC
LIMIT 25


/*************************************
### Headers

A wikipedia article is divided into sections by headers.  In the web view they appear as standard HTML headers (H1, H2, H3, etc).  These can provide valuable semantic information.  For example, links that appear in the LEAD (part right below title also known as the abstract) are probably more important than links that appear deeper in the article.

 * *parentPageId* Wikimedia Id for the page
 * *headerId* Unique (to the page) identifier for a header.
 * *title* Header text
 * *level* Header depth. 1 is Lead H2 = 2, H3 = 3, etc.
 * *mainPage*  A section's main_page.  This is derived from the main template. The main template contains important semantic information.
 * *isAncillary* If the header is a Reference, External Links, See more, etc type.  Generally these should be excluded when doing natural language research.
 
***************************************/
SELECT * 
FROM wkp_header 
LIMIT 25

/*************************************
Most common headers in Wikipedia.  Note that LEAD is a pseudo header and contains the data between the title and the first actual header.

***************************************/

SELECT
    title,
    COUNT(*) AS header_count
FROM
    wkp_header
GROUP BY
    title
ORDER BY
    header_count DESC
LIMIT 25



/*************************************
### Text

Generally this is the part of Wikipedia that most people are interested in.  It is the natural language part of the wiki text.  Generally this is reflective of what you would see if you were to visit the article's web page.

***************************************/
-- Longest Pages
SELECT
    art.id,
    art.title,
    sub.text_length
FROM
    wkp_page art
JOIN (
    SELECT
        parent_page_id,
        SUM(LENGTH(text)) AS text_length
    FROM
        wkp_text
    GROUP BY
        parent_page_id
    ) SUB
    ON  sub.parent_page_id = art.id
WHERE
    art.page_type = 'ARTICLE'
ORDER BY
    text_length DESC
LIMIT 25

/*************************************
### Links

Broadly speaking there are two kinds of links in Wikipedia: Internal and External.  Internal links are denoted with double bracketrs[[ ]] and point to a page within the Wikimedia ecosystem.  External links are your run of the mill HTTP link.

* *elementId* Unique (to the page) integer for an element.
* *destination* URL.  For internal links, the wikipedia title, otherwise the domain. Internal domains may (and often do) point to redirects.  This needs to be taken into account when analysing links.
* *text* The textual overlay for a link.  If empty the destination will be used.
* *subType* Namespace for WIKIMEDIA links or the domain for external links
* *pageBookmark* We separate the page book mark from the domain for analytic purposes.  www.test.com#page_bookmark becomes www.test.com  and page_bookmark.

***************************************/
SELECT * 
FROM wkp_link_wiki
LIMIT 25

SELECT *
FROM wkp_link_external
LIMIT 25

/*************************************
### Templates

Templates are a special MediaWiki construct that allows code to be shared among pages.  For example the template {{Global warming}} will create a table with links that are common to all GW related pages.  If a link is added to the template then all the pages that reference the template will see the new link.  Templates are an integral part of Wikipedia and much information can be gleaned from them.  Templates are explored more in depth in the advanced analysis notebook.

***************************************/
SELECT
    template_type,
    is_info_box,
    COUNT(*) AS template_count
FROM 
    wkp_template
GROUP BY
    template_type,
    is_info_box
ORDER BY 
    template_count DESC
LIMIT 25


-- Get Pages with most citation needed templates
SELECT
    art.id,
    art.title,
    COUNT(*) AS citation_needed_count
FROM
    wkp_page art
JOIN
    wkp_template tmp
    ON tmp.parent_page_id = art.id
WHERE
    UPPER(tmp.template_type) = 'CITATION NEEDED'
GROUP BY
    art.id,
    art.title
ORDER BY
    citation_needed_count DESC
LIMIT 25


/*************************************
### Tags
All the Wikipedia parsers handle HTML tags differently.  This parser uses the Sweble parser.  The tags that end up here are mostly ones that the parser does not understand.  Unfortunately it is unable to parse the <ref> tag which is very common.  This can be worked around by re-parsing the text inside the tag.  This is an expensive operation and is turned off by default.  Generally if you are interested in references then you should look into DBPedia's curated reference lists.

***************************************/
SELECT
    tag,
    COUNT(*) AS tagCount
FROM
    wkp_tag
GROUP BY
    tag
    
    
/*************************************
### Tables

Wikipedia has a special syntax for creating HTML tables.  Generally these tables are designed for human viewing and not computer consumption.  This generally means few tables are in a nice flat n X n square format.  The parser converts the wiki syntax to an HTML table.  It extracts the table caption (if available) and any further parsing is left to the user.

This query shows the articles that contain the largest tables.

Occasionally users will miss-format and the entire article ends up as a table

***************************************/
-- Get longest tables
SELECT
    art.id,
    art.title,
    tbl.caption,
    LENGTH(html) AS tableLength
FROM
    wkpPage art
JOIN
    wkpTable tbl
    ON tbl.parentPageId = art.id
WHERE
    art.pageType = 'ARTICLE'
ORDER BY
    tableLength DESC
LIMIT 25