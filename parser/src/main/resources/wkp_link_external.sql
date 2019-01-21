CREATE OR REPLACE TEMPORARY VIEW wkp_link_external
AS

SELECT
    lnk.parentPageId    AS parent_page_id,
    lnk.parentRevisionId AS parent_revision_id,
    lnk.parentHeaderId  AS parent_header_id,
    lnk.elementId       AS element_id,
    lnk.destination     AS destination,
    lnk.text            AS link_text,
    lnk.subType         AS domain,
    lnk.pageBookmark    AS page_bookmark
FROM
    links lnk
WHERE
    lnk.linkType = 'EXTERNAL'