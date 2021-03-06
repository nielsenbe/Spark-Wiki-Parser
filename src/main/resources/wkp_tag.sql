CREATE OR REPLACE TEMPORARY VIEW wkp_tag
AS
SELECT 
    parentPageId        AS parent_page_id,	
    parentRevisionId    AS parent_revision_id,	
    parentHeaderId      AS parent_header_id,
    elementId           AS element_id,	
    TRIM(tag)           AS tag,
    TRIM(tagValue)      AS tag_value
FROM 
    tags