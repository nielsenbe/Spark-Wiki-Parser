CREATE OR REPLACE TEMPORARY VIEW wkp_link_wiki
AS
SELECT
    lnk.parentPageId                        AS parent_page_id,
    lnk.parentRevisionId                    AS parent_revision_id,
    lnk.parentHeaderId                      AS parent_header_id,
    lnk.elementId                           AS element_id,
    COALESCE(
        pge1.title, 
        pge2.title, 
        lnk.destination)   AS destination,
    COALESCE(
        pge1.id, 
        pge2.id)                            AS destination_page_id,
    lnk.subType                             AS wiki_name_space,
    lnk.text                                AS link_text,
    lnk.pageBookmark                        AS page_bookmark,
    CASE 
        WHEN COALESCE(pge1.id, pge2.id) IS NULL 
        THEN 0
        ELSE 1 
        END                                 AS page_exists
FROM(
    SELECT
      parentPageId,
      parentRevisionId,
      parentHeaderId,
      elementId,
      destination  AS destination,
      TRIM(UPPER(REPLACE(destination, '_', ' '))) AS destination_clean,
      text,
      linkType,
      subType,
      pageBookmark
    FROM links
    WHERE linkType = 'WIKIMEDIA'
    UNION ALL
    SELECT
        parentPageId,	
        parentRevisionId,
        parentHeaderId,
        elementId,
        replace(split(split(destination, '/wiki/')[1], '\\?')[0], '_', ' ') AS destination,
        replace(split(split(destination, '/wiki/')[1], '\\?')[0], '_', ' ') AS destination_clean,
        text,
        linkType,
        subType,
        pageBookmark
    FROM links
    WHERE linkType = 'EXTERNAL'
    AND subType = 'en.wikipedia.org'
    AND destination LIKE '%en.wikipedia.org/wiki/%'
   ) lnk
LEFT JOIN
    wkp_redirect rdr
    ON lnk.destination_clean = TRIM(UPPER(rdr.redirect_title))
LEFT JOIN
    wkp_page_simple pge1
    ON  rdr.target_page_id = pge1.id
LEFT JOIN
    wkp_page_simple pge2
    ON  lnk.destination_clean = TRIM(UPPER(pge2.title))