CREATE OR REPLACE TEMPORARY VIEW wkp_page_simple
AS
SELECT 
    id, 
    title, 
    nameSpace       AS name_space, 
    revisionId      AS revision_id, 
    revisionDate    AS revision_date
FROM 
    items
WHERE 
    redirect = ''
AND parserMessage = 'SUCCESS'