CREATE OR REPLACE TEMPORARY VIEW wkp_template
AS
SELECT
    parentPageId        AS parent_page_id,
    parentRevisionId    AS parent_revision_id,
    parentHeaderId      AS parent_header_id,
    elementId           AS element_id,
    templateType        AS template_type,
    CASE 
        WHEN UPPER(TEMPLATETYPE) LIKE '%BOX%' 
        THEN 1
        ELSE 0 
        END             AS is_info_box
FROM 
    templates