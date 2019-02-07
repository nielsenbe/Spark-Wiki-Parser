CREATE OR REPLACE TEMPORARY VIEW wkp_text
AS
SELECT 
    parentPageId        AS parent_page_id,	
    parentRevisionId    AS parent_revision_id,	
    parentHeaderId      AS parent_header_id,
    TRIM(text)          AS text,
    LENGTH(text)        AS text_length
FROM texts
WHERE
    TRIM(text) != ''
AND LENGTH(text) > 20